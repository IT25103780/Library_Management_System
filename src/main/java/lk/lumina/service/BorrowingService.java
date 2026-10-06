package lk.lumina.service;

import static lk.lumina.util.BusinessRules.now;
import static lk.lumina.util.BusinessRules.require;

import java.util.Map;
import lk.lumina.repository.LoanRepository;

/** Borrowing operations, preserving existing validation and transaction boundaries. */
public final class BorrowingService {
  public static Map<String, Object> reading(long uid, long loan) throws Exception {
    var r = LoanRepository.findAuthorizedDigitalLoan(loan, uid, now());
    require(r != null, "Reading access has expired or is unavailable for this account.");
    return r;
  }
}
