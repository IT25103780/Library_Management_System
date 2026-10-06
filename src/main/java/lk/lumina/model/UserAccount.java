package lk.lumina.model;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Typed view of the existing session account; JSPs continue to receive the original row. */
public record UserAccount(String role, Long branchId) {
  public static UserAccount from(Map<String, Object> row) {
    if (row == null) return null;
    return new UserAccount(
        Objects.toString(row.get("role"), ""),
        row.get("branch_id") == null ? null : ((Number) row.get("branch_id")).longValue());
  }

  public boolean isStaff() {
    return Set.of("ADMIN", "LIBRARIAN", "BRANCH_MANAGER").contains(role);
  }

  public boolean isManager() {
    return isStaff() || role.equals("MANAGEMENT");
  }
}
