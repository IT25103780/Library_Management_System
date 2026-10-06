package lk.lumina.service;

import static lk.lumina.util.BusinessRules.now;

import java.sql.Connection;
import lk.lumina.repository.NotificationRepository;

/** Notification operations, preserving existing validation and transaction boundaries. */
public final class NotificationService {
  public static void notify(
      Connection c, long uid, String title, String message, String link, String key)
      throws Exception {
    if (NotificationRepository.findByDedupeKey(c, key) == null)
      NotificationRepository.insertNotification(c, uid, title, message, link, key, now());
  }
}
