package german.dev.dao.jdbc;

import german.dev.config.JdbcConnectionFactory;
import german.dev.domain.Account;
import german.dev.domain.AccountStatus;
import german.dev.domain.Client;
import german.dev.domain.Currency;
import german.dev.exception.AccountNotFoundException;
import german.dev.repository.AccountRepository;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import german.dev.repository.dto.ClientBalanceSummary;
import german.dev.validation.AccountBalanceValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;

public class JdbcAccountRepository implements AccountRepository {

    private final JdbcConnectionFactory connectionFactory;

    private static final Logger log =
            LoggerFactory.getLogger(JdbcAccountRepository.class);

    public JdbcAccountRepository(JdbcConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public Optional<Account> findById(Long id) {
        String sql = """
                SELECT
                a.*,
                c.id AS client_id_result,
                c.full_name,
                c.phone,
                c.created_at AS client_created_at
                FROM accounts a
                JOIN clients c
                ON a.client_id = c.id
                WHERE a.id = ?;
                """;
        try (JdbcConnectionScope scope = JdbcConnectionScope.open(connectionFactory);
             PreparedStatement statement = scope.getConnection().prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapAccount(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Ошибка при поиске счета по ID", e);
            throw new IllegalStateException("Ошибка при работе с базой данных", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        String sql = "SELECT a.*, c.id AS client_id_result, c.full_name, c.phone, c.created_at AS client_created_at " +
                "FROM accounts a JOIN clients c ON a.client_id = c.id  WHERE a.account_number = ?";
        try (JdbcConnectionScope scope = JdbcConnectionScope.open(connectionFactory);
             PreparedStatement statement = scope.getConnection().prepareStatement(sql)) {
            statement.setString(1, accountNumber);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapAccount(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Ошибка при поиске счета по Account_number", e);
            throw new IllegalStateException("Ошибка при работе с базой данных", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Account> findAllByClientId(Long clientId) {
        String sql = "SELECT a.*, c.id AS client_id_result, c.full_name, c.phone, c.created_at AS client_created_at " +
                "FROM accounts a JOIN clients c ON a.client_id = c.id  WHERE a.client_id = ?";
        ArrayList<Account> accounts = new ArrayList<>();
        try (JdbcConnectionScope scope = JdbcConnectionScope.open(connectionFactory);
             PreparedStatement statement = scope.getConnection().prepareStatement(sql)) {
            statement.setLong(1, clientId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    accounts.add(mapAccount(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Ошибка при поиске счетов клиента", e);
            throw new IllegalStateException("Ошибка при работе с базой данных", e);
        }
        return accounts;
    }

    @Override
    public List<Account> findActiveAccounts() {
        String sql = "SELECT a.*, c.id AS client_id_result, c.full_name, c.phone, c.created_at AS client_created_at " +
                "FROM accounts a JOIN clients c ON a.client_id = c.id  WHERE a.status = 'ACTIVE'";
        ArrayList<Account> accounts = new ArrayList<>();
        try (JdbcConnectionScope scope = JdbcConnectionScope.open(connectionFactory);
             PreparedStatement statement = scope.getConnection().prepareStatement(sql)) {
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    accounts.add(mapAccount(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Ошибка при поиске активных счетов клиента", e);
            throw new IllegalStateException("Ошибка при работе с базой данных", e);
        }
        return accounts;
    }

    @Override
    public List<Account> findAccountsWithBalanceGreaterThan(BigDecimal amount) {
        String sql = "SELECT a.*, c.id AS client_id_result, c.full_name, c.phone, c.created_at AS client_created_at " +
                "FROM accounts a JOIN clients c ON a.client_id = c.id  WHERE a.balance > ?";
        ArrayList<Account> accounts = new ArrayList<>();
        try (JdbcConnectionScope scope = JdbcConnectionScope.open(connectionFactory);
             PreparedStatement statement = scope.getConnection().prepareStatement(sql)) {
            statement.setBigDecimal(1, amount);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    accounts.add(mapAccount(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Ошибка при поиске счетов c балансом > запрошенного", e);
            throw new IllegalStateException("Ошибка при работе с базой данных", e);
        }
        return accounts;
    }

    @Override
    public List<Account> findAccountsByCurrency(String currency) {
        String sql = "SELECT a.*, c.id AS client_id_result, c.full_name, c.phone, c.created_at AS client_created_at " +
                "FROM accounts a JOIN clients c ON a.client_id = c.id WHERE a.currency = ?";
        ArrayList<Account> accounts = new ArrayList<>();
        try (JdbcConnectionScope scope = JdbcConnectionScope.open(connectionFactory);
             PreparedStatement statement = scope.getConnection().prepareStatement(sql)) {
            statement.setString(1, currency);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    accounts.add(mapAccount(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Ошибка при поиске счетов в запрошенной валюте", e);
            throw new IllegalStateException("Ошибка при работе с базой данных", e);
        }
        return accounts;
    }

    @Override
    public List<Account> findBlockedAccounts() {
        String sql = "SELECT a.*, c.id AS client_id_result, c.full_name, c.phone, c.created_at AS client_created_at " +
                "FROM accounts a JOIN clients c ON a.client_id = c.id WHERE a.status = 'BLOCKED'";
        List<Account> accounts = new ArrayList<>();
        try (JdbcConnectionScope scope = JdbcConnectionScope.open(connectionFactory);
             PreparedStatement statement = scope.getConnection().prepareStatement(sql)) {
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    accounts.add(mapAccount(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Ошибка при поиске счетов в статусе BLOCKED", e);
            throw new IllegalStateException("Ошибка при работе с базой данных", e);
        }
        return accounts;
    }

    @Override
    public void updateBalance(Long accountId, BigDecimal newBalance) {
        AccountBalanceValidator.validateForUpdate(accountId, newBalance);

        String sql = "UPDATE accounts a SET balance = ? WHERE a.id = ?";

        try (JdbcConnectionScope scope = JdbcConnectionScope.open(connectionFactory);
             PreparedStatement statement = scope.getConnection().prepareStatement(sql)) {
            statement.setBigDecimal(1, newBalance);
            statement.setLong(2, accountId);
            int updateRows = statement.executeUpdate();
            if (updateRows == 0) {
                throw new AccountNotFoundException("Счёт не найден");
            }
        } catch (SQLException e) {
            log.error("Ошибка обновления баланса", e);
            throw new IllegalStateException("Ошибка при работе с базой данных", e);
        }
    }

    @Override
    public void updateStatus(Long accountId, AccountStatus status) {
        if (accountId == null) {
            throw new IllegalArgumentException("ID счёта не может быть null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Статус не может быть null");
        }
        String sql = """
                UPDATE accounts a
                SET status = ?,
                    closed_at = CASE
                        WHEN ? = 'CLOSED' THEN COALESCE(a.closed_at, CURRENT_TIMESTAMP)
                        ELSE a.closed_at
                    END
                WHERE a.id = ?
                  AND (a.status <> 'CLOSED' OR ? = 'CLOSED')
                  AND (? <> 'CLOSED' OR a.status = 'CLOSED' OR a.balance = 0)
                """;

        try (JdbcConnectionScope scope = JdbcConnectionScope.open(connectionFactory);
             PreparedStatement statement = scope.getConnection().prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setString(2, status.name());
            statement.setLong(3, accountId);
            statement.setString(4, status.name());
            statement.setString(5, status.name());
            int updateRows = statement.executeUpdate();
            if (updateRows == 0) {
                String statusSql = "SELECT status, balance FROM accounts WHERE id = ?";
                try (PreparedStatement statusStatement = scope.getConnection().prepareStatement(statusSql)) {
                    statusStatement.setLong(1, accountId);
                    try (ResultSet resultSet = statusStatement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new AccountNotFoundException("Счёт не найден");
                        }
                        boolean alreadyClosed = AccountStatus.CLOSED.name().equals(resultSet.getString("status"));
                        if (alreadyClosed && status != AccountStatus.CLOSED) {
                            throw new IllegalStateException("Закрытый счёт нельзя повторно открыть");
                        }
                        if (!alreadyClosed && status == AccountStatus.CLOSED
                                && resultSet.getBigDecimal("balance").signum() != 0) {
                            throw new IllegalStateException("Баланс должен быть равен 0");
                        }
                    }
                }
                throw new IllegalStateException("Не удалось обновить статус счёта");
            }
        } catch (SQLException e) {
            log.error("Ошибка обновления статуса", e);
            throw new IllegalStateException("Ошибка при работе с базой данных", e);
        }
    }

    @Override
    public Optional<Account> findByAccountNumberForUpdate(String accountNumber) {
        String sql = "SELECT a.*, c.id AS client_id_result, c.full_name, c.phone, c.created_at AS client_created_at " +
                "FROM accounts a JOIN clients c ON a.client_id = c.id  WHERE a.account_number = ? FOR UPDATE OF a";
        try(JdbcConnectionScope scope = JdbcConnectionScope.open(connectionFactory);
             PreparedStatement statement = scope.getConnection().prepareStatement(sql)) {
            statement.setString(1, accountNumber);
            try(ResultSet rs = statement.executeQuery()) {
                if(rs.next()) {
                    return Optional.of(mapAccount(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Ошибка при поиске счета по Account_number", e);
            throw new IllegalStateException("Ошибка при работе с базой данных", e);
        }
        return Optional.empty();
    }

    @Override
    public List<ClientBalanceSummary> getClientBalanceSummary() {
        String sql = """
                SELECT c.full_name,
                COUNT(a.id) AS account_count,
                COALESCE(SUM(a.balance), 0) AS total_balance,
                COALESCE(AVG(a.balance), 0) AS average_balance
                FROM clients c
                LEFT JOIN accounts a
                ON a.client_id = c.id
                GROUP BY c.id, c.full_name
                """;
        List<ClientBalanceSummary> summaries = new ArrayList<>();
        try(JdbcConnectionScope scope = JdbcConnectionScope.open(connectionFactory);
        PreparedStatement statement = scope.getConnection().prepareStatement(sql)) {
            try(ResultSet rs = statement.executeQuery()) {
                while(rs.next()) {
                    String fullName = rs.getString("full_name");
                    long accountCount = rs.getLong("account_count");
                    BigDecimal totalBalance = rs.getBigDecimal("total_balance");
                    BigDecimal averageBalance = rs.getBigDecimal("average_balance");
                    ClientBalanceSummary summary = new ClientBalanceSummary(fullName, accountCount,
                            totalBalance, averageBalance);
                    summaries.add(summary);
                }
            }
        } catch (SQLException e) {
            log.error("Ошибка при расчете суммы баланса счетов", e);
            throw new IllegalStateException("Ошибка при расчете суммы баланса счетов", e);
        }
        return summaries;
    }

    @Override
    public List<ClientBalanceSummary> findRichClients(BigDecimal minTotalBalance) {
        if(minTotalBalance == null) {
            throw new IllegalArgumentException("Минимальный баланс не может быть null");
        }
        if(minTotalBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Минимальный баланс не может быть отрицательным");
        }
        String sql = """
                SELECT c.full_name,
                COUNT(a.id) AS account_count,
                COALESCE(SUM(a.balance), 0) AS total_balance,
                COALESCE(AVG(a.balance), 0) AS average_balance
                FROM clients c
                LEFT JOIN accounts a
                ON a.client_id = c.id
                GROUP BY c.id, c.full_name
                HAVING COALESCE(SUM(a.balance), 0) > ?
                """;
        List<ClientBalanceSummary> summaries = new ArrayList<>();
        try(JdbcConnectionScope scope = JdbcConnectionScope.open(connectionFactory);
        PreparedStatement statement = scope.getConnection().prepareStatement(sql)) {
            statement.setBigDecimal(1, minTotalBalance);
            try(ResultSet rs = statement.executeQuery()) {
                while(rs.next()) {
                    String fullName = rs.getString("full_name");
                    long accountCount = rs.getLong("account_count");
                    BigDecimal totalBalance = rs.getBigDecimal("total_balance");
                    BigDecimal averageBalance = rs.getBigDecimal("average_balance");
                    ClientBalanceSummary summary = new ClientBalanceSummary(fullName, accountCount,
                            totalBalance, averageBalance);
                    summaries.add(summary);
                }
            }
        } catch (SQLException e) {
            log.error("Ошибка при расчете минимального баланса", e);
            throw new IllegalStateException("Ошибка при расчете минимального баланса", e);
        }
        return summaries;
    }

    @Override
    public List<Account> findAccountsWithTransfers() {
        String sql = """
                SELECT a.*,
                c.id AS client_id_result,
                c.full_name, c.phone,
                c.created_at AS client_created_at
                       FROM accounts a
                       JOIN clients c
                       ON a.client_id = c.id
                       WHERE EXISTS (
                             SELECT 1
                             FROM transfers t
                             WHERE t.from_account_id = a.id
                             OR t.to_account_id = a.id)
                """;
        List<Account> accounts = new ArrayList<>();
        try(JdbcConnectionScope scope = JdbcConnectionScope.open(connectionFactory);
        PreparedStatement statement = scope.getConnection().prepareStatement(sql)) {
            try(ResultSet rs = statement.executeQuery()) {
                while(rs.next()) {
                    accounts.add(mapAccount(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Ошибка поиска хотя бы одного перевода", e);
            throw new IllegalStateException("Ошибка при поиске хотя бы одного перевода", e);
        }
        return accounts;
    }

    private Account mapAccount(ResultSet rs) throws SQLException {
        long accountId = rs.getLong("id");
        String accountNumberFromDb = rs.getString("account_number");
        BigDecimal balance = rs.getBigDecimal("balance");
        Currency currency = Currency.valueOf(rs.getString("currency"));
        AccountStatus accountStatus = AccountStatus.valueOf(rs.getString("status"));
        Instant openedAt = rs.getObject("opened_at", OffsetDateTime.class).toInstant();
        OffsetDateTime closedDate = rs.getObject("closed_at", OffsetDateTime.class);
        Instant closedAt = null;
        if (closedDate != null) {
            closedAt = closedDate.toInstant();
        }
        long clientId = rs.getLong("client_id_result");
        String fullName = rs.getString("full_name");
        String phone = rs.getString("phone");
        Instant createdAt = rs.getObject("client_created_at", OffsetDateTime.class).toInstant();
        Client client = Client.fromDatabase(clientId, fullName, phone, createdAt);
        Account account = Account.fromDatabase(accountId, accountNumberFromDb, currency, balance,
                accountStatus, openedAt, closedAt, client);
        return account;
    }
}
