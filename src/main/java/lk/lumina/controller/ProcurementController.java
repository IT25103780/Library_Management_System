package lk.lumina.controller;

import static lk.lumina.security.AccessControl.branch;
import static lk.lumina.security.AccessControl.staffOnly;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lk.lumina.dto.PurchaseItem;
import lk.lumina.repository.JdbcRepository;
import lk.lumina.repository.PurchaseRepository;
import lk.lumina.service.AuditService;
import lk.lumina.service.ProcurementService;
import lk.lumina.util.BusinessRules;

/** Handles procurement requests using the existing URLs and form contracts. */
public final class ProcurementController extends BaseController {
  public static void action(
      HttpServletRequest q, HttpServletResponse r, String action, Map<String, Object> u)
      throws Exception {
    long uid = JdbcRepository.id(u, "id");
    switch (action) {
      case "purchase-batch":
        {
          staffOnly(u);
          List<PurchaseItem> items = new ArrayList<>();
          for (int i = 0; i < 5; i++) {
            if (num(q, "quantity" + i) > 0)
              items.add(
                  new PurchaseItem(
                      num(q, "book" + i), positive(q, "quantity" + i, 500), price(q, "cost" + i)));
          }
          ProcurementService.create(
              uid,
              num(q, "supplier_id"),
              num(q, "branch_id"),
              p(q, "invoice_ref"),
              p(q, "notes"),
              items,
              branch(u));
          go(
              q,
              r,
              "/manage?entity=purchases",
              "Purchase saved. Receive each line as copies arrive.");
          return;
        }

      case "receive":
        {
          staffOnly(u);
          ProcurementService.receive(uid, num(q, "id"), positive(q, "quantity", 500), branch(u));
          go(q, r, "/manage?entity=purchases", "Received copies added to stock.");
          return;
        }

      case "purchase-update":
      case "purchase-cancel":
        {
          staffOnly(u);
          JdbcRepository.tx(
              c -> {
                var purchase = PurchaseRepository.findOpenPurchase(c, num(q, "id"));
                BusinessRules.require(
                    purchase != null, "Only an unreceived purchase can be changed.");
                if (branch(u) != null)
                  BusinessRules.require(
                      JdbcRepository.id(purchase, "branch_id") == branch(u),
                      "This purchase belongs to another branch.");
                if (action.equals("purchase-cancel"))
                  PurchaseRepository.cancelPurchase(c, purchase.get("id"));
                else {
                  int qty = positive(q, "quantity", 500);
                  BusinessRules.require(
                      qty > JdbcRepository.id(purchase, "received_quantity"),
                      "Quantity must exceed copies already received.");
                  PurchaseRepository.updatePurchase(
                      c,
                      qty,
                      price(q, "unit_cost"),
                      p(q, "invoice_ref"),
                      p(q, "notes"),
                      purchase.get("id"));
                }
                AuditService.audit(c, uid, action, "Purchase " + purchase.get("id"));
                return null;
              });
          go(q, r, "/manage?entity=purchases", "Purchase updated.");
          return;
        }
      default:
        throw new IllegalArgumentException("Unknown action.");
    }
  }
}
