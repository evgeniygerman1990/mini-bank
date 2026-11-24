package german.dev.dao.jdbc;

import german.dev.config.JdbcConnectionFactory;
import german.dev.service.TransactionManager;
import lombok.extern.slf4j.Slf4j;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

@Slf4j
public class JdbcTransactionManager implements TransactionManager {

    private final JdbcConnectionFactory jdbcConnectionFactory;

    public JdbcTransactionManager(JdbcConnectionFactory jdbcConnectionFactory) {
        this.jdbcConnectionFactory = jdbcConnectionFactory;
    }

    @Override
    public <T> T executeInTransaction(Supplier<T> action) {
        if (action == null) {
            throw new IllegalArgumentException("Действие не может быть null");
        }
        if (JdbcTransactionContext.getConnection() != null) {
            throw new IllegalStateException("Вложенные JDBC-транзакции не поддерживаются");
        }
        try (Connection connection = jdbcConnectionFactory.getConnection()) {
            connection.setAutoCommit(false);
            JdbcTransactionContext.setConnection(connection);
            try {
                T result = action.get();
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                if (e instanceof RuntimeException) {
                    log.error("Ошибка при выполнении JDBC-транзакции", e);
                }
                throw e;
            }
        } catch (SQLException e) {
            log.error("Ошибка при выполнении JDBC-транзакции", e);
            throw new IllegalStateException("Ошибка при выполнении JDBC-транзакции", e);
        } finally {
            JdbcTransactionContext.clearConnection();
        }
    }
}
