package german.dev.repository;

import german.dev.domain.Transfer;
import java.math.BigDecimal;
import java.util.List;

public interface TransferRepository {
    Transfer save(Transfer transfer);
    List<Transfer> findByAccountId(Long accountId);
    List<Transfer> findOutgoingTransfers(Long accountId);
    List<Transfer> findIncomingTransfers(Long accountId);
    List<Transfer> findSuccessfulTransfers();
    BigDecimal getTotalOutgoingAmount(Long accountId);
    BigDecimal getTotalIncomingAmount(Long accountId);
    List<Transfer> findTransfersWithoutComment();
    List<Transfer> findLargeTransfers(BigDecimal minAmount);
}
