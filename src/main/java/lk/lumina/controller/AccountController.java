package lk.lumina.controller;

import static lk.lumina.security.AccessControl.branch;
import static lk.lumina.security.AccessControl.staffOnly;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Objects;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.repository.UserRepository;
import lk.lumina.security.Security;
import lk.lumina.util.BusinessRules;

/** Handles account requests using the existing URLs and form contracts. */
public final class AccountController extends BaseController {
  public static void get(
      HttpServletRequest q, HttpServletResponse r, String path, Map<String, Object> u)
      throws Exception {

    switch (path) {
      case "/profile":
        {
          view(q, r, "profile");
          break;
        }

      case "/member":
        {
          staffOnly(u);
          var member = UserRepository.findMemberDetails(num(q, "id"));
          BusinessRules.require(member != null, "Member not found.");
          if (branch(u) != null)
            BusinessRules.require(
                Objects.equals(member.get("branch_id"), branch(u))
                    || UserRepository.findMemberBranchLoan(num(q, "id"), branch(u)) != null,
                "Member is outside your branch.");
          q.setAttribute("member", member);
          String memberScope =
              branch(u) == null
                  ? ""
                  : " AND l.copy_id IN (SELECT id FROM copies WHERE branch_id=" + branch(u) + ")";
          q.setAttribute(
              "rows", UserRepository.listMemberLoans(String.valueOf(memberScope), num(q, "id")));
          q.setAttribute(
              "fineRows",
              UserRepository.listMemberFines(String.valueOf(memberScope), num(q, "id")));
          view(q, r, "member");
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
      case "/profile":
        {
          if (p(q, "operation").equals("password")) {
            var row = UserRepository.findPasswordHash(uid);
            BusinessRules.require(
                Security.verify(p(q, "current_password"), row.get("password_hash").toString()),
                "Current password is incorrect.");
            Security.password(p(q, "password"));
            BusinessRules.require(
                p(q, "password").equals(p(q, "confirm_password")), "Passwords do not match.");
            UserRepository.updatePassword(Security.hash(p(q, "password")), uid);
          } else
            UserRepository.updateProfile(
                required(q, "name", 180),
                required(q, "phone", 40),
                required(q, "address", 500),
                uid);
          go(q, r, "/profile", "Your account has been updated.");
          return;
        }
      default:
        throw new IllegalArgumentException("Unknown post.");
    }
  }
}
