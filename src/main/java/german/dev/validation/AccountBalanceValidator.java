package german.dev.validation;

import java.math.BigDecimal;

public final class AccountBalanceValidator {

    private AccountBalanceValidator() {
    }

    public static void validateForUpdate(Long accountId, BigDecimal newBalance) {
        if (accountId == null) {
            throw new IllegalArgumentException("ID счёта не может быть null");
        }
        if (newBalance == null) {
            throw new IllegalArgumentException("Новый баланс не может быть null");
        }
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Баланс не может быть отрицательным");
        }
        if (newBalance.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Баланс должен иметь не более двух знаков после запятой");
        }
        if (newBalance.compareTo(MoneyLimits.UPPER_BOUND_EXCLUSIVE) >= 0) {
            throw new IllegalArgumentException("Баланс превышает максимально допустимое значение");
        }
    }
}
