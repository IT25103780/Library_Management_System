package lk.lumina.service;

import static lk.lumina.util.BusinessRules.now;

import java.sql.Connection;
import lk.lumina.repository.AuditRepository;

/** Audit operations, preserving existing validation and transaction boundaries. */
public final class AuditService {
  public static void audit(Connection c, long uid, String action, String details) throws Exception {
    AuditRepository.insertAuditEvent(c, uid, action, details, now());
  }
}
