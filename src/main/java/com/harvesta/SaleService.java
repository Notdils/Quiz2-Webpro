package com.harvesta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/** The remaining-quantity rule for harvest sales (PRD F-09). */
public final class SaleService {

    private SaleService() {
    }

    /** total_amount is always computed on the server, never taken from input. */
    public static BigDecimal total(BigDecimal quantity, BigDecimal unitPrice) {
        return quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Quantity of the harvest not yet sold. When editing, pass the sale's id so its current
     * quantity is not counted twice. Call inside {@link Db#tx} so the row lock holds until commit.
     */
    public static BigDecimal remaining(long harvestId, Long exceptSaleId) {
        Map<String, Object> harvest = Db.one("select quantity from harvests where id = ? for update", harvestId);
        BigDecimal sold = (BigDecimal) Db.value("select coalesce(sum(quantity), 0) from sales where harvest_id = ? "
                + "and id <> ?", harvestId, exceptSaleId == null ? -1L : exceptSaleId);
        return ((BigDecimal) harvest.get("quantity")).subtract(sold);
    }

    /** Null when the quantity fits, otherwise the English error message. */
    public static String check(long harvestId, BigDecimal quantity, Long exceptSaleId) {
        BigDecimal remaining = remaining(harvestId, exceptSaleId);
        if (quantity.compareTo(remaining) <= 0) {
            return null;
        }
        Object unit = Db.value("select unit from harvests where id = ?", harvestId);
        return "The quantity exceeds the remaining unsold amount of this harvest ("
                + com.harvesta.web.Fn.qty(remaining.max(BigDecimal.ZERO)) + " " + Options.label(unit) + ").";
    }
}
