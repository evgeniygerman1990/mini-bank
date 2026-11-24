package german.dev.dao.jdbc;

import german.dev.config.JdbcConnectionFactory;
import lombok.Getter;
import java.sql.Connection;
import java.sql.SQLException;

public final class JdbcConnectionScope implements AutoCloseable {

    @Getter
    private final Connection connection;

    private final boolean ownsConnection;

    public JdbcConnectionScope(Connection connection, boolean ownsConnection) {
        if(connection == null) {
            throw new IllegalArgumentException("Соединение отсутствует");
        }
        this.connection = connection;
        this.ownsConnection = ownsConnection;
    }

    @Override
    public void close() throws SQLException {
        if(ownsConnection) {
            connection.close();
        }
    }
    public static JdbcConnectionScope open(JdbcConnectionFactory factory) throws SQLException {
        Connection currentConnection = JdbcTransactionContext.getConnection();
        if (currentConnection != null) {
            return new JdbcConnectionScope(currentConnection, false);
        }
        return new JdbcConnectionScope(factory.getConnection(), true);
    }
}
