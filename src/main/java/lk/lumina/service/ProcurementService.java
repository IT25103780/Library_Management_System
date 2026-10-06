package lk.lumina.service;

import static lk.lumina.security.AccessControl.scope;
import static lk.lumina.service.AuditService.audit;
import static lk.lumina.service.ReservationService.fulfill;
import static lk.lumina.util.BusinessRules.require;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lk.lumina.dto.PurchaseItem;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.repository.PurchaseRepository;
import lk.lumina.util.BusinessRules;

/** Multiple purchase lines share an invoice reference and commit together. */
public final class ProcurementService {

  public static void create(
      long actor,
      long supplier,
      long branch,
      String invoice,
      String notes,
      List<PurchaseItem> items,
      Long scope)
      throws Exception {
    BusinessRules.require(scope == null || scope == branch, "Select your assigned branch.");
    BusinessRules.require(
        !items.isEmpty() && items.size() <= 20, "Enter at least one purchase line, up to 20.");
    BusinessRules.require(
        invoice != null && invoice.length() <= 100 && notes.length() <= 500,
        "Invoice or notes are too long.");
    String reference =
        invoice.isBlank() ? "PO-" + UUID.randomUUID().toString().substring(0, 12) : invoice;
    JdbcRepository.tx(
        c -> {
          BusinessRules.require(
              PurchaseRepository.findActiveSupplier(c, supplier) != null,
              "Choose an active supplier.");
          BusinessRules.require(
              PurchaseRepository.findActiveBranch(c, branch) != null, "Choose an active branch.");
          BusinessRules.require(
              PurchaseRepository.findSupplierInvoice(c, supplier, reference) == null,
              "This supplier invoice is already recorded.");
          Set<Long> seen = new HashSet<>();
          for (PurchaseItem item : items) {
            BusinessRules.require(
                item.quantity() > 0
                    && item.quantity() <= 500
                    && item.unitCost().signum() >= 0
                    && item.unitCost().compareTo(new BigDecimal("10000000")) < 0,
                "Invalid quantity or unit cost.");
            BusinessRules.require(
                seen.add(item.book()), "Combine duplicate books into one purchase line.");
            BusinessRules.require(
                PurchaseRepository.findActivePhysicalBook(c, item.book()) != null,
                "Choose an active physical book.");
            PurchaseRepository.insertPurchase(
                c,
                supplier,
                item.book(),
                branch,
                item.quantity(),
                item.unitCost(),
                BusinessRules.now(),
                reference,
                notes);
          }
          AuditService.audit(
              c,
              actor,
              "Create purchase",
              "Invoice " + reference + " / " + items.size() + " lines");
          return null;
        });
  }

  public static void receive(long actor, long id, int quantity, Long branch) throws Exception {
    JdbcRepository.tx(
        c -> {
          var p = PurchaseRepository.findById(c, id);
          scope(p, branch);
          require(
              Set.of("ORDERED", "PARTIALLY_RECEIVED").contains(p.get("status")),
              "Purchase is already closed.");
          int received = ((Number) p.get("received_quantity")).intValue(),
              ordered = ((Number) p.get("quantity")).intValue();
          require(
              quantity > 0 && quantity <= ordered - received,
              "Enter a quantity no greater than the remaining order.");
          for (int i = 0; i < quantity; i++)
            PurchaseRepository.insertReceivedCopy(c, p.get("book_id"), p.get("branch_id"));
          received += quantity;
          PurchaseRepository.updateReceivedQuantity(
              c, received, received == ordered ? "RECEIVED" : "PARTIALLY_RECEIVED", id);
          audit(c, actor, "Receive stock", "Purchase " + id + ": " + quantity + " copies");
          fulfill(c);
          return null;
        });
  }
}
