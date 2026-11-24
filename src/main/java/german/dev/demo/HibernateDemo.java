package german.dev.demo;

import german.dev.domain.Account;
import german.dev.domain.Client;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.stat.Statistics;

import java.util.List;

public final class HibernateDemo {

    private final SessionFactory sessionFactory;
    private final Statistics statistics;

    public HibernateDemo(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.statistics = sessionFactory.getStatistics();
    }

    public void run() {
        boolean statisticsEnabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        try {
            Long accountId;
            Long clientId;
            try (Session session = sessionFactory.openSession()) {
                accountId = session.createQuery("select a.id from Account a order by a.id", Long.class)
                        .setMaxResults(1)
                        .getSingleResultOrNull();
                if (accountId == null) {
                    throw new IllegalStateException("Для Hibernate-демо нужен хотя бы один счёт");
                }
                clientId = session.find(Account.class, accountId).getClient().getId();
            }
            showLazyLoading(accountId);
            showProxy(clientId);
            showFirstLevelCache(clientId);
            showDirtyChecking(clientId);
            showNPlusOne();
        } finally {
            statistics.setStatisticsEnabled(statisticsEnabled);
        }
    }

    private void showLazyLoading(Long accountId) {
        try (Session session = sessionFactory.openSession()) {
            Client client = session.find(Account.class, accountId).getClient();
            boolean initialized = Hibernate.isInitialized(client);
            long statementsBefore = statistics.getPrepareStatementCount();
            client.getFullName();
            System.out.printf("Lazy loading — Account.client: %b → %b, SQL при чтении имени: %d%n",
                    initialized, Hibernate.isInitialized(client),
                    statistics.getPrepareStatementCount() - statementsBefore);
        }
    }

    private void showProxy(Long clientId) {
        try (Session session = sessionFactory.openSession()) {
            long statementsBefore = statistics.getPrepareStatementCount();
            Client reference = session.getReference(Client.class, clientId);
            long statementsAfterReference = statistics.getPrepareStatementCount();
            boolean initialized = Hibernate.isInitialized(reference);
            reference.getFullName();
            System.out.printf("Proxy — %s, инициализация: %b → %b, SQL при getReference: %d, при чтении имени: %d%n",
                    reference.getClass().getSimpleName(), initialized, Hibernate.isInitialized(reference),
                    statementsAfterReference - statementsBefore,
                    statistics.getPrepareStatementCount() - statementsAfterReference);
        }
    }

    private void showFirstLevelCache(Long clientId) {
        try (Session session = sessionFactory.openSession()) {
            Client first = session.find(Client.class, clientId);
            long statementsBefore = statistics.getPrepareStatementCount();
            Client second = session.find(Client.class, clientId);
            System.out.printf("First-level cache — тот же объект: %b, SQL при повторной загрузке: %d%n",
                    first == second, statistics.getPrepareStatementCount() - statementsBefore);
        }
    }

    private void showDirtyChecking(Long clientId) {
        String originalName;
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                Client client = session.find(Client.class, clientId);
                originalName = client.getFullName();
                String demoName = "Hibernate demo".equals(originalName) ? "Hibernate demo updated" : "Hibernate demo";
                long updatesBefore = statistics.getEntityUpdateCount();
                client.setFullName(demoName);
                session.flush();
                System.out.printf("Dirty checking — обновлений сущностей после flush(): %d%n",
                        statistics.getEntityUpdateCount() - updatesBefore);
            } finally {
                transaction.rollback();
            }
        }
        try (Session session = sessionFactory.openSession()) {
            boolean restored = originalName.equals(session.find(Client.class, clientId).getFullName());
            System.out.printf("Dirty checking — исходное имя после rollback(): %b%n", restored);
        }
    }

    private void showNPlusOne() {
        long lazyStatements = countAccountLoadingStatements("from Client");
        long fetchStatements = countAccountLoadingStatements("select c from Client c left join fetch c.accounts");
        System.out.printf("N+1 — SQL при ленивой загрузке: %d, при JOIN FETCH: %d%n",
                lazyStatements, fetchStatements);
    }

    private long countAccountLoadingStatements(String hql) {
        try (Session session = sessionFactory.openSession()) {
            long statementsBefore = statistics.getPrepareStatementCount();
            List<Client> clients = session.createQuery(hql, Client.class).getResultList();
            for (Client client : clients) {
                client.getAccounts().size();
            }
            return statistics.getPrepareStatementCount() - statementsBefore;
        }
    }
}
