package lk.lumina.controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Objects;
import lk.lumina.util.BusinessRules;

/**
 * Shared request parsing and JSP/redirect helpers. Only MainController is registered as a servlet.
 */
public abstract class BaseController {
  public static String p(HttpServletRequest q, String key) {
    String value = Objects.toString(q.getParameter(key), "");
    return key.contains("password") ? value : value.trim();
  }

  public static long num(HttpServletRequest q, String key) {
    String v = p(q, key);
    return v.isBlank() ? 0 : Long.parseLong(v);
  }

  public static String required(HttpServletRequest q, String key, int max) {
    String v = p(q, key);
    BusinessRules.require(
        !v.isBlank() && v.length() <= max,
        "Please enter a valid " + key.replace('_', ' ') + " (maximum " + max + " characters).");
    return v;
  }

  public static int positive(HttpServletRequest q, String key, int max) {
    long v = num(q, key);
    BusinessRules.require(v > 0 && v <= max, "Invalid " + key.replace('_', ' ') + ".");
    return (int) v;
  }

  public static BigDecimal price(HttpServletRequest q, String key) {
    BigDecimal d = new BigDecimal(p(q, key)).setScale(2, java.math.RoundingMode.HALF_UP);
    BusinessRules.require(
        d.signum() >= 0 && d.compareTo(new BigDecimal("10000000")) < 0, "Enter a valid amount.");
    return d;
  }

  public static void email(String s) {
    BusinessRules.require(
        s.length() <= 180 && s.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"),
        "Enter a valid email address.");
  }

  public static void view(HttpServletRequest q, HttpServletResponse r, String name)
      throws ServletException, IOException {
    r.setContentType("text/html;charset=UTF-8");
    q.getRequestDispatcher("/WEB-INF/views/" + name + ".jsp").forward(q, r);
  }

  public static void go(HttpServletRequest q, HttpServletResponse r, String path, String message)
      throws IOException {
    q.getSession().setAttribute("flash", message);
    r.sendRedirect(q.getContextPath() + path);
  }
}
