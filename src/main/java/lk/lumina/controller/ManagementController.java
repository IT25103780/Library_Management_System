package lk.lumina.controller;

import static lk.lumina.config.EntityDefinitions.ENTITIES;
import static lk.lumina.security.AccessControl.adminOnly;
import static lk.lumina.security.AccessControl.branch;
import static lk.lumina.security.AccessControl.staffOnly;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.repository.WorkspaceRepository;
import lk.lumina.security.Security;
import lk.lumina.service.AuditService;
import lk.lumina.service.ReservationService;
import lk.lumina.util.BusinessRules;

/** Handles management requests using the existing URLs and form contracts. */
public final class ManagementController extends BaseController {
  public static void get(
      HttpServletRequest q, HttpServletResponse r, String path, Map<String, Object> u)
      throws Exception {

    switch (path) {
      case "/manage":
        {
          staffOnly(u);
          manage(q, u);
          view(q, r, "manage");
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
      case "/manage":
        {
          staffOnly(u);
          saveEntity(q, u);
          go(q, r, "/manage?entity=" + p(q, "entity"), "Record saved.");
          return;
        }
      default:
        throw new IllegalArgumentException("Unknown post.");
    }
  }

  public static void action(
      HttpServletRequest q, HttpServletResponse r, String action, Map<String, Object> u)
      throws Exception {
    long uid = JdbcRepository.id(u, "id");
    switch (action) {
      case "toggle":
        {
          staffOnly(u);
          String entity = p(q, "entity");
          BusinessRules.require(
              ENTITIES.containsKey(entity) || Set.of("books", "users").contains(entity),
              "Invalid record.");
          if (entity.equals("users") || entity.equals("branches")) adminOnly(u);
          if (entity.equals("users"))
            BusinessRules.require(num(q, "id") != uid, "You cannot deactivate your own account.");
          JdbcRepository.tx(
              c -> {
                WorkspaceRepository.toggleActive(c, String.valueOf(entity), num(q, "id"));
                AuditService.audit(c, uid, "Toggle " + entity, "Record " + num(q, "id"));
                return null;
              });
          go(q, r, "/manage?entity=" + entity, "Record status updated.");
          return;
        }

      case "delete-record":
        {
          staffOnly(u);
          String deleteEntity = p(q, "entity");
          BusinessRules.require(
              ENTITIES.containsKey(deleteEntity) || Set.of("books", "users").contains(deleteEntity),
              "Invalid record.");
          if (deleteEntity.equals("users") || deleteEntity.equals("branches")) adminOnly(u);
          BusinessRules.require(
              !deleteEntity.equals("users") || num(q, "id") != uid,
              "You cannot remove your own account.");
          try {
            JdbcRepository.tx(
                c -> {
                  WorkspaceRepository.deleteUnusedRecord(
                      c, String.valueOf(deleteEntity), num(q, "id"));
                  AuditService.audit(
                      c, uid, "Delete unused " + deleteEntity, "Record " + num(q, "id"));
                  return null;
                });
          } catch (java.sql.SQLException ex) {
            if ("23000".equals(ex.getSQLState()) || ex.getSQLState().startsWith("23"))
              throw new IllegalArgumentException(
                  "This record is linked to library history. Deactivate it instead.");
            throw ex;
          }
          go(q, r, "/manage?entity=" + deleteEntity, "Unused record removed.");
          return;
        }
      default:
        throw new IllegalArgumentException("Unknown action.");
    }
  }

  private static void manage(HttpServletRequest q, Map<String, Object> u) throws Exception {
    String entity = p(q, "entity");
    if (entity.isBlank()) entity = "books";
    BusinessRules.require(
        ENTITIES.containsKey(entity)
            || Set.of("books", "users", "purchases", "copies").contains(entity),
        "Unknown workspace.");
    if (entity.equals("users") || entity.equals("branches")) adminOnly(u);
    q.setAttribute("entity", entity);
    String sql =
        entity.equals("books")
            ? "SELECT b.id,b.title,b.isbn,b.format,b.fee,b.duration_days,b.active FROM books b"
            : entity.equals("users")
                ? "SELECT id,name,email,username,phone,address,member_type,role,branch_id,active"
                    + " FROM users"
                : entity.equals("copies")
                    ? "SELECT c.id,b.title,c.book_id,c.branch_id,c.shelf,c.status FROM copies c"
                        + " JOIN books b ON b.id=c.book_id"
                        + (branch(u) == null ? "" : " WHERE c.branch_id=" + branch(u))
                    : entity.equals("purchases")
                        ? "SELECT p.id,s.name AS"
                              + " supplier,b.title,p.supplier_id,p.book_id,p.branch_id,p.quantity,p.received_quantity,p.unit_cost,p.invoice_ref,p.notes,p.status,p.created_at"
                              + " FROM purchases p JOIN suppliers s ON s.id=p.supplier_id JOIN"
                              + " books b ON b.id=p.book_id"
                            + (branch(u) == null ? "" : " WHERE p.branch_id=" + branch(u))
                        : "SELECT * FROM " + entity;
    q.setAttribute("rows", WorkspaceRepository.listWorkspace(String.valueOf(sql)));
    if (num(q, "edit") > 0 && ENTITIES.containsKey(entity))
      q.setAttribute(
          "editing",
          WorkspaceRepository.findDirectoryRecord(String.valueOf(entity), num(q, "edit")));
    if (num(q, "edit") > 0 && entity.equals("users"))
      q.setAttribute("editing", WorkspaceRepository.findEditableAccount(num(q, "edit")));
    if (Set.of("purchases", "copies", "users").contains(entity)) {
      q.setAttribute("books", WorkspaceRepository.listPhysicalBooks());
      q.setAttribute(
          "branches",
          WorkspaceRepository.listActiveBranches(
              String.valueOf((branch(u) == null ? "" : " AND id=" + branch(u)))));
      q.setAttribute("suppliers", WorkspaceRepository.listActiveSuppliers());
    }
  }

  private static void saveEntity(HttpServletRequest q, Map<String, Object> u) throws Exception {
    String entity = p(q, "entity");
    long id = num(q, "id");
    JdbcRepository.tx(
        c -> {
          if (ENTITIES.containsKey(entity)) {
            if (entity.equals("branches")) adminOnly(u);
            BusinessRules.require(
                WorkspaceRepository.findDuplicateName(c, String.valueOf(entity), p(q, "name"), id)
                    == null,
                "A record with this name already exists. Edit the existing record.");
            if (entity.equals("suppliers") && !p(q, "email").isBlank()) email(p(q, "email"));
            String[] fields = ENTITIES.get(entity);
            List<Object> values = new ArrayList<>();
            for (String field : fields) {
              String value = p(q, field);
              if (field.equals("name")) value = required(q, field, 180);
              BusinessRules.require(
                  value.length()
                      <= (field.equals("description")
                          ? 1000
                          : field.equals("address") ? 500 : field.equals("phone") ? 40 : 200),
                  "Field is too long: " + field);
              values.add(value);
            }
            if (id == 0)
              WorkspaceRepository.insertDirectoryRecord(
                  c,
                  String.valueOf(entity),
                  String.valueOf(String.join(",", fields)),
                  String.valueOf(String.join(",", Collections.nCopies(fields.length, "?"))),
                  values.toArray());
            else {
              values.add(id);
              WorkspaceRepository.updateDirectoryRecord(
                  c,
                  String.valueOf(entity),
                  String.valueOf(
                      String.join(",", Arrays.stream(fields).map(f -> f + "=?").toList())),
                  values.toArray());
            }
          } else if (entity.equals("users")) {
            adminOnly(u);
            String role = p(q, "role");
            BusinessRules.require(
                Set.of("ADMIN", "LIBRARIAN", "BRANCH_MANAGER", "MANAGEMENT", "READER")
                    .contains(role),
                "Choose a valid role.");
            BusinessRules.require(
                id != JdbcRepository.id(u, "id") || role.equals("ADMIN"),
                "You cannot remove your own administrator role.");
            String name = required(q, "name", 180),
                mail = required(q, "email", 180).toLowerCase(Locale.ROOT),
                username = required(q, "username", 100).toLowerCase(Locale.ROOT);
            email(mail);
            BusinessRules.require(
                WorkspaceRepository.findDuplicateAccount(c, mail, username, id) == null,
                "Email or username already exists.");
            BusinessRules.require(
                username.matches("[a-z0-9_.-]{3,40}"),
                "Username must use 3-40 letters, numbers, dots, hyphens or underscores.");
            String mt = p(q, "member_type");
            if (mt.isBlank()) mt = "STUDENT";
            BusinessRules.require(
                Set.of("STUDENT", "STAFF").contains(mt), "Choose student or staff membership.");
            Long br = num(q, "branch_id") == 0 ? null : num(q, "branch_id");
            BusinessRules.require(
                !role.equals("BRANCH_MANAGER") || br != null,
                "Assign a branch to the branch manager.");
            if (br != null)
              BusinessRules.require(
                  WorkspaceRepository.findActiveBranch(c, br) != null, "Choose an active branch.");
            if (id == 0) {
              Security.password(p(q, "password"));
              WorkspaceRepository.insertAccount(
                  c,
                  name,
                  mail,
                  username,
                  Security.hash(p(q, "password")),
                  role,
                  br,
                  BusinessRules.now(),
                  p(q, "phone"),
                  p(q, "address"),
                  mt);
            } else
              WorkspaceRepository.updateAccount(
                  c, name, mail, username, role, br, p(q, "phone"), p(q, "address"), mt, id);
          } else if (entity.equals("purchases") || entity.equals("copies")) {
            long br = num(q, "branch_id");
            if (branch(u) != null)
              BusinessRules.require(br == branch(u), "Select your assigned branch.");
            int qty = positive(q, "quantity", 500);
            BusinessRules.require(
                WorkspaceRepository.findActivePhysicalBook(c, num(q, "book_id")) != null,
                "Choose an active physical book.");
            BusinessRules.require(
                WorkspaceRepository.findActiveBranch(c, br) != null, "Choose an active branch.");
            if (entity.equals("purchases")) {
              BusinessRules.require(
                  WorkspaceRepository.findActiveSupplier(c, num(q, "supplier_id")) != null,
                  "Choose an active supplier.");
              WorkspaceRepository.insertPurchase(
                  c,
                  num(q, "supplier_id"),
                  num(q, "book_id"),
                  br,
                  qty,
                  price(q, "unit_cost"),
                  BusinessRules.now(),
                  p(q, "invoice_ref"),
                  p(q, "notes"));
            } else
              for (int i = 0; i < qty; i++)
                WorkspaceRepository.insertCopy(c, num(q, "book_id"), br);
            ReservationService.fulfill(c);
          } else throw new IllegalArgumentException("Invalid workspace.");
          AuditService.audit(c, JdbcRepository.id(u, "id"), "Save " + entity, "Record " + id);
          return null;
        });
  }
}
