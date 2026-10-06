package lk.lumina.service;

import static lk.lumina.security.AccessControl.scope;
import static lk.lumina.service.AuditService.audit;
import static lk.lumina.service.ReservationService.fulfill;
import static lk.lumina.util.BusinessRules.require;

import java.util.Set;
import lk.lumina.repository.InventoryRepository;
import lk.lumina.repository.JdbcRepository;

/** Inventory operations, preserving existing validation and transaction boundaries. */
public final class InventoryService {
  public static void editCopy(
      long actor, long id, long destination, String shelf, Long branch, boolean delete)
      throws Exception {
    JdbcRepository.tx(
        c -> {
          var cp = InventoryRepository.findCopy(c, id);
          scope(cp, branch);
          require(
              !Set.of("BORROWED", "RESERVED").contains(cp.get("status")),
              "Return or release the copy first.");
          if (delete) {
            require(
                InventoryRepository.findLoanHistory(c, id) == null
                    && InventoryRepository.findReservationHistory(c, id) == null,
                "This copy has history; mark it WITHDRAWN instead.");
            InventoryRepository.deleteCopy(c, id);
          } else {
            if (branch != null) require(destination == branch, "Choose your assigned branch.");
            require(
                InventoryRepository.findActiveBranch(c, destination) != null,
                "Select an active branch.");
            InventoryRepository.updateLocation(c, destination, shelf, id);
            fulfill(c);
          }
          audit(c, actor, delete ? "Delete unused copy" : "Edit copy", "Copy " + id);
          return null;
        });
  }
}
