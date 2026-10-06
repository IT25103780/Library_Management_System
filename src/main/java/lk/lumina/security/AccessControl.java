package lk.lumina.security;

import static lk.lumina.util.BusinessRules.require;

import java.util.Map;
import lk.lumina.model.UserAccount;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.util.BusinessRules;

/** AccessControl operations, preserving existing validation and transaction boundaries. */
public final class AccessControl {
  public static void scope(Map<String, Object> row, Long branch) {
    require(row != null, "Record not found.");
    if (branch != null)
      require(
          JdbcRepository.id(row, "branch_id") == branch, "This record belongs to another branch.");
  }

  public static boolean staff(Map<String, Object> row) {
    UserAccount user = UserAccount.from(row);
    return user != null && user.isStaff();
  }

  public static boolean manager(Map<String, Object> row) {
    UserAccount user = UserAccount.from(row);
    return user != null && user.isManager();
  }

  public static Long branch(Map<String, Object> row) {
    UserAccount user = UserAccount.from(row);
    return user.role().equals("BRANCH_MANAGER") ? user.branchId() : null;
  }

  public static void staffOnly(Map<String, Object> u) {
    BusinessRules.require(staff(u), "Staff access required.");
  }

  public static void adminOnly(Map<String, Object> u) {
    BusinessRules.require(u.get("role").equals("ADMIN"), "Administrator access required.");
  }
}
