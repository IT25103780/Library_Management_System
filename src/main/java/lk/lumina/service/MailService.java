package lk.lumina.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import lk.lumina.config.AppConfig;
import lk.lumina.repository.NotificationRepository;

public class MailService {
  public static boolean enabled() {
    return !AppConfig.get("smtp.host", "").isBlank();
  }

  public static void send(String to, String subject, String body) throws Exception {
    if (!enabled()) return;
    Properties p = new Properties();
    p.put("mail.smtp.host", AppConfig.get("smtp.host", ""));
    p.put("mail.smtp.port", AppConfig.get("smtp.port", "587"));
    p.put("mail.smtp.auth", "true");
    p.put("mail.smtp.starttls.enable", "true");
    p.put("mail.smtp.starttls.required", "true");
    p.put("mail.smtp.connectiontimeout", "5000");
    p.put("mail.smtp.timeout", "5000");
    p.put("mail.smtp.writetimeout", "5000");
    Session s =
        Session.getInstance(
            p,
            new Authenticator() {
              protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                    AppConfig.get("smtp.user", ""), AppConfig.get("smtp.password", ""));
              }
            });
    MimeMessage m = new MimeMessage(s);
    m.setFrom(new InternetAddress(AppConfig.get("smtp.from", AppConfig.get("smtp.user", ""))));
    m.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
    m.setSubject(subject, "UTF-8");
    m.setText(body, "UTF-8");
    Transport.send(m);
  }

  public static void deliver() throws Exception {
    if (!enabled()) return;
    for (var n : NotificationRepository.listPendingEmails()) {
      send(
          n.get("email").toString(),
          n.get("title").toString(),
          n.get("message")
              + "\n\n"
              + AppConfig.get("app.url", "http://localhost:8080/lumina")
              + n.get("link"));
      NotificationRepository.markEmailSent(n.get("id"));
    }
  }
}
