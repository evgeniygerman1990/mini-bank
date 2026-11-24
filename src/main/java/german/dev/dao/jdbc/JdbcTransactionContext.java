package german.dev.dao.jdbc;

import java.sql.Connection;

public final class JdbcTransactionContext {

    private static final ThreadLocal<Connection> CURRENT_CONNECTION = new ThreadLocal<>();

    public static Connection getConnection() {
        return CURRENT_CONNECTION.get();
    }

    public static void setConnection (Connection connection) {
        if(connection == null) {
            throw new IllegalArgumentException("Не удалось установить соединение");
        }
        if(CURRENT_CONNECTION.get() != null) {
            throw new IllegalStateException("Соединение уже установлено");
        }
        CURRENT_CONNECTION.set(connection);
    }

    public static void clearConnection() {
        CURRENT_CONNECTION.remove();
    }
}
