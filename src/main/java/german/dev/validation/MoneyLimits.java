package german.dev.validation;

import java.math.BigDecimal;

public final class MoneyLimits {

    /** Exclusive upper bound for amounts stored as NUMERIC(19, 2). */
    public static final BigDecimal UPPER_BOUND_EXCLUSIVE = new BigDecimal("100000000000000000");

    private MoneyLimits() {
    }
}
