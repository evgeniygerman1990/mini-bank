package german.dev.service;
import java.math.BigDecimal;

public interface TransferService {
    void transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount, String comment);
}
