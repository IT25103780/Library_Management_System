package lk.lumina.controller;

import static lk.lumina.security.AccessControl.manager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import lk.lumina.config.AppConfig;
import lk.lumina.model.LoginAttempt;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.repository.UserRepository;
import lk.lumina.security.Security;
import lk.lumina.service.AuditService;
import lk.lumina.service.MailService;
import lk.lumina.service.NotificationService;
import lk.lumina.util.BusinessRules;

/** Handles auth requests using the existing URLs and form contracts. */
public final class AuthController extends BaseController {
  private static final Map<String, LoginAttempt> attempts =
      new java.util.concurrent.ConcurrentHashMap<>();

  public static void get(
      HttpServletRequest q, HttpServletResponse r, String path, Map<String, Object> u)
      throws Exception {

    switch (path) {
      case "/login":
      case "/signup":
      case "/forgot":
      case "/reset":
        {
          view(q, r, "auth");
          break;
        }
      default:
        throw new IllegalArgumentException("Unknown get.");
    }
  }

  public static void post(
      HttpServletRequest q, HttpServletResponse r, String path, Map<String, Object> u)
      throws Exception {

    switch (path) {
      case "/login":
        {
          String key = q.getRemoteAddr() + ":" + p(q, "username").toLowerCase(Locale.ROOT);
          long now = System.currentTimeMillis();
          attempts.entrySet().removeIf(x -> now - x.getValue().start() > 900000);
          LoginAttempt a = attempts.getOrDefault(key, new LoginAttempt(0, now));
          BusinessRules.require(a.count() < 8, "Too many attempts. Try again after 15 minutes.");
          attempts.put(key, new LoginAttempt(a.count() + 1, a.start()));
          var account =
              UserRepository.findActiveLogin(
                  p(q, "username").toLowerCase(Locale.ROOT),
                  p(q, "username").toLowerCase(Locale.ROOT));
          BusinessRules.require(
              account != null
                  && Security.verify(p(q, "password"), account.get("password_hash").toString()),
              "The username or password is incorrect.");
          attempts.remove(key);
          JdbcRepository.tx(
              c -> {
                AuditService.audit(
                    c, JdbcRepository.id(account, "id"), "Login", "Successful sign in");
                return null;
              });
          q.changeSessionId();
          q.getSession().setAttribute("csrf", Security.token());
          q.getSession().setAttribute("uid", account.get("id"));
          go(q, r, manager(account) ? "/dashboard" : "/my-books", "Welcome back to Lumina.");
          return;
        }

      case "/signup":
        {
          String name = required(q, "name", 180),
              mail = required(q, "email", 180).toLowerCase(Locale.ROOT),
              username = required(q, "username", 100).toLowerCase(Locale.ROOT);
          email(mail);
          BusinessRules.require(
              username.matches("[a-z0-9_.-]{3,40}"),
              "Username must use 3â€“40 letters, numbers, dots, hyphens or underscores.");
          String password = p(q, "password");
          Security.password(password);
          BusinessRules.require(
              password.equals(p(q, "confirm_password")), "Passwords do not match.");
          BusinessRules.require(
              UserRepository.findDuplicateAccount(mail, username) == null,
              "That email or username is already registered.");
          String phone = required(q, "phone", 40), address = required(q, "address", 500);
          long id =
              JdbcRepository.tx(
                  c -> {
                    long uid =
                        UserRepository.insertReader(
                            c,
                            name,
                            mail,
                            username,
                            Security.hash(password),
                            phone,
                            address,
                            BusinessRules.now());
                    NotificationService.notify(
                        c,
                        uid,
                        "Welcome to Lumina",
                        "Your account is ready. Explore the collection and begin your next"
                            + " chapter.",
                        "/catalog",
                        "welcome-" + uid);
                    return uid;
                  });
          q.changeSessionId();
          q.getSession().setAttribute("uid", id);
          q.getSession().setAttribute("csrf", Security.token());
          go(q, r, "/my-books", "Your account is ready.");
          return;
        }

      case "/forgot":
        {
          String mail = p(q, "email").toLowerCase(Locale.ROOT);
          email(mail);
          var account = UserRepository.findActiveEmail(mail);
          if (account != null) {
            String token = Security.token();
            UserRepository.insertResetToken(
                account.get("id"),
                Security.digest(token, "SHA-256"),
                Timestamp.from(Instant.now().plusSeconds(1800)));
            String link =
                AppConfig.get("app.url", "http://localhost:8080/lumina") + "/reset?token=" + token;
            if (MailService.enabled())
              MailService.send(
                  mail, "Reset your Lumina password", link + "\nThis link expires in 30 minutes.");
            else if (AppConfig.get("db.mode", "sqlserver").equals("demo"))
              q.getSession().setAttribute("devReset", link);
          }
          go(
              q,
              r,
              "/forgot",
              "If this account exists, a reset link has been sent. Links expire after 30 minutes.");
          return;
        }

      case "/reset":
        {
          String password = p(q, "password");
          Security.password(password);
          BusinessRules.require(
              password.equals(p(q, "confirm_password")), "Passwords do not match.");
          JdbcRepository.tx(
              c -> {
                var token =
                    UserRepository.findValidResetToken(
                        c, Security.digest(p(q, "token"), "SHA-256"), BusinessRules.now());
                BusinessRules.require(token != null, "This reset link is invalid or expired.");
                UserRepository.updatePassword(c, Security.hash(password), token.get("user_id"));
                UserRepository.invalidateResetTokens(c, token.get("user_id"));
                return null;
              });
          go(q, r, "/login", "Password reset. Sign in with your new password.");
          return;
        }

      case "/logout":
        {
          q.getSession().invalidate();
          r.sendRedirect(q.getContextPath() + "/home");
          return;
        }
      default:
        throw new IllegalArgumentException("Unknown post.");
    }
  }
}
