package german.dev.config;

public final class DatabaseConfig {

    private DatabaseConfig() {
    }

    public static String getUrl() {
        String url = System.getenv("DB_URL");
        if(url == null || url.isBlank()) {
            throw new IllegalStateException("Переменная окружения DB_URL не задана");
        }
        return url;
    }

    public static String getUser() {
        String user = System.getenv("DB_USER");
        if(user == null || user.isBlank()) {
            throw new IllegalStateException("Переменная окружения DB_USER не задана");
        }
        return user;
    }

    public static String getPassword() {
        String password = System.getenv("DB_PASSWORD");
        if(password == null || password.isBlank()) {
            throw new IllegalStateException("Переменная окружения DB_PASSWORD не задана");
        }
        return password;
    }
}
