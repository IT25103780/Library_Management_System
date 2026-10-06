package lk.lumina.dto;

import java.math.BigDecimal;

/** A single purchase line submitted by the acquisitions form. */
public record PurchaseItem(long book, int quantity, BigDecimal unitCost) {}
