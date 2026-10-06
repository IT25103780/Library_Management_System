package lk.lumina.controller;

import static lk.lumina.security.AccessControl.adminOnly;
import static lk.lumina.security.AccessControl.branch;
import static lk.lumina.security.AccessControl.staff;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Set;
import lk.lumina.config.AppConfig;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.repository.PaymentRepository;
import lk.lumina.security.Security;
import lk.lumina.service.AuditService;
import lk.lumina.service.NotificationService;
import lk.lumina.service.PaymentGatewayService;
import lk.lumina.service.PaymentService;
import lk.lumina.service.ReceiptService;
import lk.lumina.util.BusinessRules;

/** Handles payment requests using the existing URLs and form contracts. */
public final class PaymentController extends BaseController {
  public static void get(
      HttpServletRequest q, HttpServletResponse r, String path, Map<String, Object> u)
      throws Exception {

    switch (path) {
      case "/checkout":
      case "/payment-status":
        {
          var payment = PaymentRepository.findMemberPayment(num(q, "id"), u.get("id"));
          BusinessRules.require(payment != null, "Payment not found.");
          if (path.equals("/payment-status") && p(q, "poll").equals("1")) {
            r.setContentType("application/json");
            r.getWriter().write("{\"status\":\"" + payment.get("status") + "\"}");
            return;
          }
          if (path.equals("/payment-status") && payment.get("status").equals("SUCCESSFUL")) {
            payment = ReceiptService.forOwner(num(q, "id"), JdbcRepository.id(u, "id"));
            String marker = "celebrated-" + payment.get("id");
            if (q.getSession().getAttribute(marker) == null) {
              q.setAttribute("celebrate", true);
              q.getSession().setAttribute(marker, true);
            }
            q.setAttribute("payment", payment);
            view(q, r, "receipt");
          } else {
            q.setAttribute("payment", payment);
            view(q, r, path.equals("/checkout") ? "checkout" : "payment-status");
          }
          break;
        }

      case "/payment-admin":
        {
          adminOnly(u);
          q.setAttribute("rows", PaymentRepository.listAllPayments());
          view(q, r, "payment-admin");
          break;
        }

      case "/payments":
        {
          q.setAttribute("rows", PaymentRepository.listMemberPayments(u.get("id")));
          view(q, r, "payments");
          break;
        }

      case "/receipt":
        {
          var receipt =
              PaymentRepository.findAccessibleReceipt(
                  String.valueOf(
                      (staff(u)
                          ? (u.get("role").equals("ADMIN")
                              ? "1=1"
                              : "EXISTS (SELECT 1 FROM fines f JOIN loans l ON l.id=f.loan_id JOIN"
                                  + " copies cp ON cp.id=l.copy_id WHERE f.id=p.fine_id"
                                  + (branch(u) == null ? "" : " AND cp.branch_id=" + branch(u))
                                  + ")")
                          : "p.user_id=" + u.get("id"))),
                  num(q, "id"));
          BusinessRules.require(receipt != null, "Receipt unavailable.");
          q.setAttribute("payment", receipt);
          if (p(q, "format").equals("pdf")) {
            r.setContentType("application/pdf");
            r.setHeader(
                "Content-Disposition",
                "attachment; filename=\"Lumina-Receipt-" + receipt.get("id") + ".pdf\"");
            ReceiptService.pdf(receipt, r.getOutputStream());
          } else view(q, r, "receipt");
          break;
        }
      default:
        throw new IllegalArgumentException("Unknown get.");
    }
  }

  public static void post(
      HttpServletRequest q, HttpServletResponse r, String path, Map<String, Object> u)
      throws Exception {
    long uid = JdbcRepository.id(u, "id");
    switch (path) {
      case "/checkout":
        {
          var payment = PaymentRepository.findPendingMemberPayment(num(q, "id"), uid);
          BusinessRules.require(
              payment != null, "This payment is already processed or unavailable.");
          if (AppConfig.demo()) {
            BusinessRules.require(payment.get("provider").equals("DEMO"), "Provider mismatch.");
            PaymentService.settle(
                payment.get("order_ref").toString(),
                "DEMO-" + Security.token().substring(0, 12),
                "DEMO",
                !p(q, "outcome").equals("fail"));
            go(
                q,
                r,
                "/payment-status?id=" + payment.get("id"),
                "Demonstration checkout completed. No money was charged.");
          } else {
            BusinessRules.require(
                payment.get("provider").equals("PAYHERE"),
                "Create a new order for the current payment provider.");
            String phone = required(q, "phone", 40),
                address = required(q, "address", 500),
                city = required(q, "city", 120);
            PaymentRepository.updateContactDetails(phone, address, uid);
            u.put("phone", phone);
            u.put("address", address);
            var fields = PaymentGatewayService.checkout(payment, u, city);
            String card = p(q, "card_method");
            BusinessRules.require(
                Set.of("VISA", "MASTER").contains(card), "Choose Visa or Mastercard.");
            fields.put("payment_method", card);
            q.setAttribute("fields", fields);
            view(q, r, "gateway");
          }
          return;
        }
      default:
        throw new IllegalArgumentException("Unknown post.");
    }
  }

  public static void action(
      HttpServletRequest q, HttpServletResponse r, String action, Map<String, Object> u)
      throws Exception {
    long uid = JdbcRepository.id(u, "id");
    switch (action) {
      case "retry":
        {
          var old = PaymentRepository.findRetryablePayment(num(q, "id"), uid);
          BusinessRules.require(old != null, "This payment cannot be retried.");
          var retry =
              PaymentService.createOrder(
                  uid,
                  old.get("book_id") == null ? 0 : JdbcRepository.id(old, "book_id"),
                  old.get("fine_id") == null ? 0 : JdbcRepository.id(old, "fine_id"),
                  ((Number) old.get("duration_days")).intValue());
          go(q, r, "/checkout?id=" + retry.get("id"), "New payment attempt ready.");
          return;
        }

      case "cancel-payment":
        {
          JdbcRepository.tx(
              c -> {
                var payment = PaymentRepository.findPendingMemberPayment(c, num(q, "id"), uid);
                BusinessRules.require(
                    payment != null, "Only your pending orders can be cancelled.");
                PaymentRepository.cancelPayment(c, payment.get("id"));
                NotificationService.notify(
                    c,
                    uid,
                    "Payment order cancelled",
                    "The pending order has been cancelled. A later verified bank payment will still"
                        + " be reconciled.",
                    "/payments",
                    "payment-cancel-" + payment.get("id"));
                return null;
              });
          go(q, r, "/payments", "Pending order cancelled.");
          return;
        }

      case "record-refund":
        {
          adminOnly(u);
          long refundId = num(q, "id");
          String refundReference = required(q, "reference", 120);
          PaymentGatewayService.reverse(refundId, refundReference);
          JdbcRepository.tx(
              c -> {
                AuditService.audit(
                    c,
                    uid,
                    "Record externally completed refund",
                    "Payment " + refundId + " / " + refundReference);
                return null;
              });
          go(
              q,
              r,
              "/payment-admin",
              "Refund recorded. This action records a refund already completed with the provider;"
                  + " it does not send money.");
          return;
        }
      default:
        throw new IllegalArgumentException("Unknown action.");
    }
  }
}
