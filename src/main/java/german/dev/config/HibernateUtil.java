package german.dev.config;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import german.dev.domain.Client;
import german.dev.domain.Account;
import german.dev.domain.Transfer;

public final class HibernateUtil {

    private static SessionFactory sessionFactory;

    private HibernateUtil() {
    }

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            Configuration configuration = new Configuration();
            configuration.addAnnotatedClass(Client.class);
            configuration.addAnnotatedClass(Account.class);
            configuration.addAnnotatedClass(Transfer.class);
            configuration.setProperty("hibernate.connection.url",
                    DatabaseConfig.getUrl());
            configuration.setProperty("hibernate.connection.username",
                    DatabaseConfig.getUser());
            configuration.setProperty("hibernate.connection.password",
                    DatabaseConfig.getPassword());
            configuration.setProperty("hibernate.hbm2ddl.auto", "validate");
            sessionFactory = configuration.buildSessionFactory();
        }
        return sessionFactory;
    }

    public static synchronized void closeSessionFactory() {
        if(sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
        sessionFactory = null;
    }
}
