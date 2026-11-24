package german.dev.validation;

import german.dev.domain.Transfer;
import java.math.BigDecimal;

public final class TransferValidator {

    private TransferValidator() {
    }

    public static void validateForSave(Transfer transfer) {
        if (transfer == null) {
            throw new IllegalArgumentException("Перевод не может быть null");
        }
        if (transfer.getId() != null) {
            throw new IllegalStateException("ID перевода уже присвоен");
        }
        if (transfer.getFromAccount() == null ||
                transfer.getFromAccount().getId() == null) {
            throw new IllegalArgumentException("Счёт отправителя отсутствует");
        }
        if (transfer.getToAccount() == null ||
                transfer.getToAccount().getId() == null) {
            throw new IllegalArgumentException("Счёт получателя отсутствует");
        }
        if (transfer.getFromAccount().getId().equals(transfer.getToAccount().getId())) {
            throw new IllegalArgumentException("Счета отправителя и получателя должны быть разными");
        }
        if (transfer.getAmount() == null) {
            throw new IllegalArgumentException("Сумма перевода не может быть null");
        }
        if (transfer.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма перевода должна быть положительной");
        }
        if (transfer.getAmount().stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Сумма перевода должна иметь не более двух знаков после запятой");
        }
        if (transfer.getAmount().compareTo(MoneyLimits.UPPER_BOUND_EXCLUSIVE) >= 0) {
            throw new IllegalArgumentException("Сумма перевода превышает максимально допустимое значение");
        }
        if (transfer.getStatus() == null) {
            throw new IllegalArgumentException("Статус перевода не может быть null");
        }
    }
}
