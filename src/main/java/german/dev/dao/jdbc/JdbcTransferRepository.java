package german.dev.dao.jdbc;

import german.dev.config.JdbcConnectionFactory;
import german.dev.repository.TransferRepository;
import german.dev.validation.TransferValidator;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import german.dev.domain.Transfer;
import german.dev.domain.Account;
import german.dev.domain.AccountStatus;
import german.dev.domain.Client;
import german.dev.domain.Currency;
import german.dev.domain.TransferStatus;

@Slf4j
public class JdbcTransferRepository implements TransferRepository {

    private static final String BASE_TRANSFER_SELECT = """
            SELECT t.*,
                                           fa.account_number AS from_account_number,
                                           ta.account_number AS to_account_number,
                                           fc.full_name AS from_client_name,
                                           tc.full_name AS to_client_name,
                                           fa.currency AS from_currency,
                                           fa.balance AS from_balance,
                                           fa.status AS from_status,
                                           fa.opened_at AS from_opened_at,
                                           fa.closed_at AS from_closed_at,
                                           fc.id AS from_client_id,
                                           fc.phone AS from_client_phone,
                                           fc.created_at AS from_client_created_at,
                                           ta.currency AS to_currency,
                                           ta.balance AS to_balance,
                                           ta.status AS to_status,
                                           ta.opened_at AS to_opened_at,
                                           ta.closed_at AS to_closed_at,
                                           tc.id AS to_client_id,
                                           tc.phone AS to_client_phone,
                                           tc.created_at AS to_client_created_at
                                    FROM transfers t
                                    JOIN accounts fa
                                    ON t.from_account_id = fa.id
                                    JOIN accounts ta
                                    ON t.to_account_id = ta.id
                                    JOIN clients fc
                                    ON fa.client_id = fc.id
                                    JOIN clients tc
                                    ON ta.client_id = tc.id
        """;

    private final JdbcConnectionFactory jdbcConnectionFactory;

    public JdbcTransferRepository(JdbcConnectionFactory jdbcConnectionFactory) {
        this.jdbcConnectionFactory = jdbcConnectionFactory;
    }

    @Override
    public Transfer save(Transfer transfer) {
        TransferValidator.validateForSave(transfer);
        String sql = """
                INSERT INTO transfers (
                from_account_id,
                to_account_id,
                amount,
                status,
                comment)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id, created_at
                """;
        try(JdbcConnectionScope scope = JdbcConnectionScope.open(jdbcConnectionFactory);
            PreparedStatement statement = scope.getConnection().prepareStatement(sql)) {
            statement.setLong(1, transfer.getFromAccount().getId());
            statement.setLong(2, transfer.getToAccount().getId());
            statement.setBigDecimal(3, transfer.getAmount());
            statement.setString(4, transfer.getStatus().name());
            statement.setString(5, transfer.getComment());
            try(ResultSet rs = statement.executeQuery()) {
                if(rs.next()) {
                    long generatedId = rs.getLong("id");
                    Instant createdAt = rs.getObject("created_at", OffsetDateTime.class).toInstant();
                    transfer.assignGeneratedId(generatedId);
                    transfer.assignCreatedAt(createdAt);
                } else {
                    throw new IllegalStateException("PostgreSQL не вернул сохранённый перевод");
                }
                return transfer;
            }
        } catch (SQLException e) {
            log.error("Ошибка сохранения перевода", e);
            throw new IllegalStateException("Ошибка при сохранении перевода", e);
        }
    }

    @Override
    public List<Transfer> findByAccountId(Long accountId) {
        String sql = BASE_TRANSFER_SELECT + """
                WHERE t.to_account_id = ?
                UNION ALL
                """ + BASE_TRANSFER_SELECT + """
                WHERE t.from_account_id = ?
                ORDER BY created_at DESC
                """;
        List<Transfer> transfers = new ArrayList<>();
        if(accountId == null) {
            throw new IllegalArgumentException("ID счета не может быть null");
        }
        try(JdbcConnectionScope scope = JdbcConnectionScope.open(jdbcConnectionFactory);
            PreparedStatement statement = scope.getConnection().prepareStatement(sql)) {
            statement.setLong(1, accountId);
            statement.setLong(2, accountId);
            try(ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                   transfers.add(mapTransfer(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Ошибка поиска перевода", e);
            throw new IllegalStateException("Ошибка при поиске перевода", e);
        }
        return transfers;
    }

    @Override
    public List<Transfer> findOutgoingTransfers(Long accountId) {
        List<Transfer> transfers = new ArrayList<>();
        String sql = BASE_TRANSFER_SELECT + """
                WHERE t.from_account_id = ?
                ORDER BY t.created_at DESC
                """;
        if(accountId == null) {
            throw new IllegalArgumentException("ID счета не может быть null");
        }
            try (JdbcConnectionScope scope = JdbcConnectionScope.open(jdbcConnectionFactory);
                 PreparedStatement statement = scope.getConnection().prepareStatement(sql)) {
                statement.setLong(1, accountId);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        transfers.add(mapTransfer(rs));
                    }
                }
            } catch (SQLException e) {
                log.error("Ошибка поиска исходящих переводов", e);
                throw new IllegalStateException("Ошибка при поиске исходящих переводов", e);
            }
            return transfers;
    }

    @Override
    public List<Transfer> findIncomingTransfers(Long accountId) {
        List<Transfer> transfers = new ArrayList<>();
        String sql = BASE_TRANSFER_SELECT + """
                WHERE t.to_account_id = ?
                ORDER BY t.created_at DESC
                """;
        if(accountId == null) {
            throw new IllegalArgumentException("ID счета не может быть null");
        }
        try (JdbcConnectionScope scope = JdbcConnectionScope.open(jdbcConnectionFactory);
             PreparedStatement statement = scope.getConnection().prepareStatement(sql)) {
            statement.setLong(1, accountId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    transfers.add(mapTransfer(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Ошибка поиска входящих переводов", e);
            throw new IllegalStateException("Ошибка при поиске входящих переводов", e);
        }
        return transfers;
    }

    @Override
    public List<Transfer> findSuccessfulTransfers() {
        String sql = BASE_TRANSFER_SELECT + """
                                WHERE t.status = 'SUCCESS'
                                ORDER BY t.created_at DESC
                """;
        List<Transfer> transfers = new ArrayList<>();
        try(JdbcConnectionScope scope = JdbcConnectionScope.open(jdbcConnectionFactory);
            PreparedStatement statement = scope.getConnection().prepareStatement(sql)) {
            try(ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    transfers.add(mapTransfer(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Ошибка поиска успешных переводов", e);
            throw new IllegalStateException("Ошибка при поиске успешных переводов", e);
        }
        return transfers;
    }

    @Override
    public BigDecimal getTotalOutgoingAmount(Long accountId) {
        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM transfers
                WHERE from_account_id = ?
                AND status = 'SUCCESS'
                """;
        if(accountId == null) {
            throw  new IllegalArgumentException("ID счета не найден");
        }
        try(JdbcConnectionScope scope = JdbcConnectionScope.open(jdbcConnectionFactory);
            PreparedStatement statement = scope.getConnection().prepareStatement(sql)) {
            statement.setLong(1, accountId);
            try(ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    BigDecimal totalAmount = rs.getBigDecimal(1);
                    return totalAmount;
                }
                    throw new IllegalStateException("PostgreSQL не вернул сумму исходящих переводов");
            }
        } catch (SQLException e) {
            log.error("Ошибка подсчёта суммы исходящих переводов", e);
            throw new IllegalStateException("Ошибка подсчёта суммы исходящих переводов", e);
        }
    }

    @Override
    public BigDecimal getTotalIncomingAmount(Long accountId) {
        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM transfers
                WHERE to_account_id = ?
                AND status = 'SUCCESS'
                """;
        if (accountId == null) {
            throw new IllegalArgumentException("ID счета не найден");
        }
        try(JdbcConnectionScope scope = JdbcConnectionScope.open(jdbcConnectionFactory);
            PreparedStatement statement = scope.getConnection().prepareStatement(sql)) {
            statement.setLong(1, accountId);
            try(ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    BigDecimal totalAmount = rs.getBigDecimal(1);
                    return totalAmount;
                }
                throw new IllegalStateException("PostgreSQL не вернул сумму входящих переводов");
            }
        } catch (SQLException e) {
                log.error("Ошибка подсчёта суммы входящих переводов", e);
                throw new IllegalStateException("Ошибка подсчёта суммы входящих переводов", e);
        }
    }

    @Override
    public List<Transfer> findTransfersWithoutComment() {
        String sql = BASE_TRANSFER_SELECT + """
                                WHERE t.comment IS NULL
                                ORDER BY t.created_at DESC
                """;
        List<Transfer> transfers = new ArrayList<>();
        try(JdbcConnectionScope scope = JdbcConnectionScope.open(jdbcConnectionFactory);
            PreparedStatement statement = scope.getConnection().prepareStatement(sql)) {
            try(ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    transfers.add(mapTransfer(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Ошибка поиска переводов без комментария", e);
            throw new IllegalStateException("Ошибка при поиске переводов без комментария", e);
        }
        return transfers;
    }

    @Override
    public List<Transfer> findLargeTransfers(BigDecimal minAmount) {
        String sql = BASE_TRANSFER_SELECT + """
                                WHERE t.amount > ?
                                ORDER BY t.created_at DESC
                """;
        if(minAmount == null) {
            throw new IllegalArgumentException("Сумма перевода отсутствует");
        }
        if(minAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Сумма не может быть отрицательной");
        }
        List<Transfer> transfers = new ArrayList<>();
        try(JdbcConnectionScope scope = JdbcConnectionScope.open(jdbcConnectionFactory);
            PreparedStatement statement = scope.getConnection().prepareStatement(sql)) {
            statement.setBigDecimal(1, minAmount);
            try(ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    transfers.add(mapTransfer(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Ошибка поиска крупных переводов", e);
            throw new IllegalStateException("Ошибка при поиске крупных переводов", e);
        }
        return transfers;
    }

    private Transfer mapTransfer(ResultSet rs) throws SQLException {
        long fromClientId = rs.getLong("from_client_id");
        String fromClientName = rs.getString("from_client_name");
        String fromClientPhone = rs.getString("from_client_phone");
        Instant fromClientCreatedAt = rs.getObject("from_client_created_at", OffsetDateTime.class).toInstant();
        Client fromClient = Client.fromDatabase(fromClientId, fromClientName, fromClientPhone, fromClientCreatedAt);
        long toClientId = rs.getLong("to_client_id");
        String toClientName = rs.getString("to_client_name");
        String toClientPhone = rs.getString("to_client_phone");
        Instant toClientCreatedAt = rs.getObject("to_client_created_at", OffsetDateTime.class).toInstant();
        Client toClient = Client.fromDatabase(toClientId, toClientName, toClientPhone, toClientCreatedAt);
        long fromAccountId = rs.getLong("from_account_id");
        String fromAccountNumber = rs.getString("from_account_number");
        Currency fromCurrency = Currency.valueOf(rs.getString("from_currency"));
        BigDecimal fromBalance = rs.getBigDecimal("from_balance");
        AccountStatus fromStatus = AccountStatus.valueOf(rs.getString("from_status"));
        Instant fromOpenedAt = rs.getObject("from_opened_at", OffsetDateTime.class).toInstant();
        OffsetDateTime closedDateTime =
                rs.getObject("from_closed_at", OffsetDateTime.class);
        Instant fromClosedAt = null;
        if (closedDateTime != null) {
            fromClosedAt = closedDateTime.toInstant();
        }
        Account fromAccount = Account.fromDatabase(fromAccountId, fromAccountNumber, fromCurrency, fromBalance,
                fromStatus, fromOpenedAt, fromClosedAt, fromClient);
        long toAccountId = rs.getLong("to_account_id");
        String toAccountNumber = rs.getString("to_account_number");
        Currency toCurrency = Currency.valueOf(rs.getString("to_currency"));
        BigDecimal toBalance = rs.getBigDecimal("to_balance");
        AccountStatus toStatus = AccountStatus.valueOf(rs.getString("to_status"));
        Instant toOpenedAt = rs.getObject("to_opened_at", OffsetDateTime.class).toInstant();
        OffsetDateTime toClosedDateTime =
                rs.getObject("to_closed_at", OffsetDateTime.class);
        Instant toClosedAt = null;
        if (toClosedDateTime != null) {
            toClosedAt = toClosedDateTime.toInstant();
        }
        Account toAccount = Account.fromDatabase(toAccountId, toAccountNumber, toCurrency, toBalance,
                toStatus, toOpenedAt, toClosedAt, toClient);
        long transferId = rs.getLong("id");
        BigDecimal transferAmount = rs.getBigDecimal("amount");
        TransferStatus transferStatus = TransferStatus.valueOf(rs.getString("status"));
        String transferComment = rs.getString("comment");
        Instant transferCreatedAt = rs.getObject("created_at", OffsetDateTime.class).toInstant();
        Transfer transfer = Transfer.fromDatabase(transferId, fromAccount, toAccount, transferAmount,
                transferStatus, transferComment, transferCreatedAt);
        return transfer;
    }
}
