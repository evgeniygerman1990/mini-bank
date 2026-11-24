package german.dev.repository;

import german.dev.domain.Account;
import german.dev.domain.AccountStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import german.dev.repository.dto.ClientBalanceSummary;

public interface AccountRepository {
    Optional<Account> findById(Long id);
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findAllByClientId(Long clientId);
    List<Account> findActiveAccounts();
    List<Account> findAccountsWithBalanceGreaterThan(BigDecimal amount);
    List<Account> findAccountsByCurrency(String currency);
    List<Account> findBlockedAccounts();
    void updateBalance(Long accountId, BigDecimal newBalance);
    void updateStatus(Long accountId, AccountStatus status);
    Optional<Account> findByAccountNumberForUpdate(String accountNumber);
    List<ClientBalanceSummary> getClientBalanceSummary();
    List<ClientBalanceSummary> findRichClients(BigDecimal minTotalBalance);
    List<Account> findAccountsWithTransfers();
}
