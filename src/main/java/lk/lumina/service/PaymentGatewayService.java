package lk.lumina.service;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lk.lumina.config.AppConfig;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.repository.PaymentRepository;
import lk.lumina.security.Security;
import lk.lumina.util.BusinessRules;

public final class PaymentGatewayService {

  public static void reverse(long paymentId, String reference) throws Exception {
    JdbcRepository.tx(
        c -> {
          var p = PaymentRepository.findById(c, paymentId);
          BusinessRules.require(p != null, "Payment not found.");
          if (p.get("status").equals("REFUNDED")) return null;
          BusinessRules.require(
              p.get("status").equals("SUCCESSFUL"),
              "Only successful payments can be marked refunded.");
          PaymentRepository.recordRefund(c, reference, paymentId);
          if (p.get("loan_id") != null) {
            var loan = PaymentRepository.findLoan(c, p.get("loan_id"));
            java.time.Instant due =
                ((java.sql.Timestamp) loan.get("due_at"))
                    .toInstant()
                    .minus(java.time.Duration.ofDays(((Number) p.get("duration_days")).intValue()));
            boolean hasOther =
                PaymentRepository.findSuccessfulLoanPayment(c, p.get("loan_id")) != null;
            PaymentRepository.updateRefundedLoan(
                c,
                java.sql.Timestamp.from(due),
                hasOther && due.isAfter(java.time.Instant.now()) ? "ACTIVE" : "CANCELLED",
                p.get("loan_id"));
          }
          if (p.get("fine_id") != null) FineService.fineStatus(c, JdbcRepository.id(p, "fine_id"));
          NotificationService.notify(
              c,
              JdbcRepository.id(p, "user_id"),
              "Refund recorded",
              "A refund was recorded for your payment. Check PaymentGatewayService and My Books for"
                  + " the updated status.",
              "/payments",
              "refund-" + paymentId);
          return null;
        });
  }

  public static String md5(String s) throws Exception {
    return Security.digest(s, "MD5");
  }

  public static Map<String, String> checkout(
      Map<String, Object> p, Map<String, Object> u, String city) throws Exception {
    String merchant = AppConfig.get("payhere.merchant.id", "");
    String secret = AppConfig.get("payhere.merchant.secret", "");
    String base = AppConfig.get("app.url", "http://localhost:8080/lumina");
    BusinessRules.require(
        !merchant.isBlank() && !secret.isBlank(),
        "Card checkout is not configured. Ask the administrator to configure PayHere.");
    Map<String, String> f = new LinkedHashMap<>();
    f.put("merchant_id", merchant);
    f.put("return_url", base + "/payment-status?id=" + p.get("id"));
    f.put("cancel_url", base + "/payment-status?id=" + p.get("id") + "&cancelled=1");
    f.put("notify_url", base + "/payment-notify");
    f.put("order_id", p.get("order_ref").toString());
    f.put("items", "Lumina Library order " + p.get("id"));
    f.put("currency", "LKR");
    f.put("amount", ((BigDecimal) p.get("amount")).setScale(2).toPlainString());
    String[] name = u.get("name").toString().split(" ", 2);
    f.put("first_name", name[0]);
    f.put("last_name", name.length > 1 ? name[1] : name[0]);
    f.put("email", u.get("email").toString());
    f.put("phone", Objects.toString(u.get("phone"), ""));
    f.put("address", Objects.toString(u.get("address"), ""));
    f.put("city", city);
    f.put("country", "Sri Lanka");
    f.put("hash", md5(merchant + f.get("order_id") + f.get("amount") + "LKR" + md5(secret)));
    return f;
  }

  public static void callback(Map<String, String> f) throws Exception {
    BusinessRules.require(!AppConfig.demo(), "Payment notifications disabled in demo mode.");
    String secret = AppConfig.get("payhere.merchant.secret", "");
    String merchant = AppConfig.get("payhere.merchant.id", "");
    BusinessRules.require(
        !secret.isBlank() && merchant.equals(f.get("merchant_id")), "Invalid merchant.");
    String expected =
        md5(
            merchant
                + f.get("order_id")
                + f.get("payhere_amount")
                + f.get("payhere_currency")
                + f.get("status_code")
                + md5(secret));
    BusinessRules.require(
        MessageDigest.isEqual(
            expected.getBytes(), f.getOrDefault("md5sig", "").toUpperCase(Locale.ROOT).getBytes()),
        "Invalid payment signature.");
    var p = PaymentRepository.findGatewayOrder(f.get("order_id"));
    BusinessRules.require(
        p != null
            && p.get("currency").equals(f.get("payhere_currency"))
            && ((BigDecimal) p.get("amount")).compareTo(new BigDecimal(f.get("payhere_amount")))
                == 0,
        "Payment order or amount mismatch.");
    String status = f.get("status_code");
    if (status.equals("2")) {
      PaymentService.settle(
          f.get("order_id"), f.get("payment_id"), f.getOrDefault("method", "CARD"), true);
      String masked = f.getOrDefault("card_no", "");
      if (masked.matches(".*[0-9]{4}$"))
        PaymentRepository.captureLastFour(masked.substring(masked.length() - 4), p.get("id"));
    } else if (status.equals("-1") || status.equals("-2"))
      PaymentService.settle(
          f.get("order_id"), f.get("payment_id"), f.getOrDefault("method", "CARD"), false);
    else if (status.equals("-3")) reverse(JdbcRepository.id(p, "id"), f.get("payment_id"));
  }
}
