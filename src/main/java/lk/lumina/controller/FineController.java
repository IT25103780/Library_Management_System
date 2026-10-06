package lk.lumina.controller;

import static lk.lumina.security.AccessControl.branch;
import static lk.lumina.security.AccessControl.staff;
import static lk.lumina.security.AccessControl.staffOnly;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import lk.lumina.repository.FineRepository;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.security.AccessControl;
import lk.lumina.service.AuditService;
import lk.lumina.service.FineService;
import lk.lumina.service.PaymentService;
import lk.lumina.util.BusinessRules;

/** Handles fine requests using the existing URLs and form contracts. */
public final class FineController extends BaseController {
  public static void get(
      HttpServletRequest q, HttpServletResponse r, String path, Map<String, Object> u)
      throws Exception {

    switch (path) {
      case "/fine":
        {
          var fineDetail = FineRepository.findFineDetails(num(q, "id"));
          BusinessRules.require(
              fineDetail != null
                  && (staff(u)
                      || JdbcRepository.id(fineDetail, "user_id") == JdbcRepository.id(u, "id")),
              "Fine unavailable.");
          if (staff(u)) AccessControl.scope(fineDetail, branch(u));
          q.setAttribute("fine", fineDetail);
          q.setAttribute("rows", FineRepository.listPaymentHistory(num(q, "id")));
          view(q, r, "fine");
          break;
        }

      case "/fines":
        {
          q.setAttribute(
              "rows",
              FineRepository.listFineBalances(
                  String.valueOf(
                      (staff(u)
                          ? (branch(u) == null
                              ? "1=1"
                              : "l.copy_id IN (SELECT id FROM copies WHERE branch_id="
                                  + branch(u)
                                  + ")")
                          : "f.user_id=" + u.get("id")))));
          view(q, r, "fines");
          break;
        }
      default:
        throw new IllegalArgumentException("Unknown get.");
    }
  }

  public static void action(
      HttpServletRequest q, HttpServletResponse r, String action, Map<String, Object> u)
      throws Exception {
    long uid = JdbcRepository.id(u, "id");
    switch (action) {
      case "pay-fine":
        {
          var fineOrder = PaymentService.createOrder(uid, 0, num(q, "id"), 7);
          go(q, r, "/checkout?id=" + fineOrder.get("id"), "Review your fine payment.");
          return;
        }

      case "payment-correct":
      case "payment-void":
        {
          staffOnly(u);
          long correctionId = num(q, "id");
          String correctionReason = required(q, "reason", 500);
          long fineId =
              JdbcRepository.tx(
                  c -> {
                    var pay = FineRepository.findSuccessfulOfflinePayment(c, correctionId);
                    AccessControl.scope(pay, branch(u));
                    if (action.equals("payment-void")) {
                      FineRepository.markPaymentRefunded(
                          c,
                          correctionReason.substring(0, Math.min(120, correctionReason.length())),
                          correctionId);
                      FineService.fineStatus(c, JdbcRepository.id(pay, "fine_id"));
                    } else {
                      String method = p(q, "method");
                      BusinessRules.require(
                          Set.of("CASH", "BANK_TRANSFER", "OTHER_OFFLINE").contains(method),
                          "Invalid offline method.");
                      FineRepository.correctPaymentMethod(
                          c, method, p(q, "reference"), correctionId);
                    }
                    AuditService.audit(
                        c, uid, action, "Payment " + correctionId + ": " + correctionReason);
                    return JdbcRepository.id(pay, "fine_id");
                  });
          go(q, r, "/fine?id=" + fineId, "Payment record corrected. No money was transferred.");
          return;
        }

      case "offline-fine":
        {
          staffOnly(u);
          FineService.offlinePayment(
              uid, num(q, "id"), price(q, "amount"), p(q, "method"), p(q, "reference"), branch(u));
          go(q, r, "/fines", "Payment recorded and balance updated.");
          return;
        }

      case "assess-fines":
        {
          staffOnly(u);
          FineService.assessOverdue(uid, branch(u));
          go(q, r, "/fines", "Overdue fines assessed without duplicate records.");
          return;
        }

      case "adjust-fine":
      case "void-fine":
        {
          staffOnly(u);
          FineService.adjustFine(
              uid,
              num(q, "id"),
              action.equals("void-fine") ? BigDecimal.ZERO : price(q, "amount"),
              required(q, "reason", 500),
              branch(u),
              action.equals("void-fine"));
          go(q, r, "/fines", "Fine correction recorded.");
          return;
        }
      default:
        throw new IllegalArgumentException("Unknown action.");
    }
  }
}
