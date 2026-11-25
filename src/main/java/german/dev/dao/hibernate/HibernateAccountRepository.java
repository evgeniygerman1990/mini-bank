package german.dev.dao.hibernate;

import german.dev.domain.Account;
import german.dev.domain.AccountStatus;
import german.dev.domain.Currency;
import german.dev.exception.AccountNotFoundException;
import german.dev.repository.AccountRepository;
import german.dev.repository.dto.ClientBalanceSummary;
import german.dev.validation.AccountBalanceValidator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hibernate.query.SelectionQuery;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.Session;
import org.hibernate.query.MutationQuery;
import org.hibernate.LockMode;

public class HibernateAccountRepository implements AccountRepository {

    private final SessionFactory sessionFactory;

    public HibernateAccountRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Optional<Account> findById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            Account account = session.get(Account.class, id);
            if(account != null) {
                Hibernate.initialize(account.getClient());
            }
            return Optional.ofNullable(account);
        }
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        String hql = """
        FROM Account a
        WHERE a.accountNumber = :accountNumber
        """;
        try (Session session = sessionFactory.openSession()) {
            SelectionQuery<Account> query = session.createQuery(hql, Account.class);
            query.setParameter("accountNumber", accountNumber);
            Account account = query.getSingleResultOrNull();
            if(account != null) {
                Hibernate.initialize(account.getClient());
            }
            return Optional.ofNullable(account);
        }
    }

    @Override
    public List<Account> findAllByClientId(Long clientId) {
        String hql = """
                FROM Account a
                JOIN FETCH a.client
                WHERE a.client.id = :clientId
                """;
        try (Session session = sessionFactory.openSession()) {
            SelectionQuery<Account> query = session.createQuery(hql, Account.class);
            query.setParameter("clientId", clientId);
            List<Account> accounts = query.getResultList();
            return accounts;
        }
    }

    @Override
    public List<Account> findActiveAccounts() {
        String hql = """
                FROM Account a
                JOIN FETCH a.client
                WHERE a.status = :status
                """;
        try (Session session = sessionFactory.openSession()) {
            SelectionQuery<Account> query = session.createQuery(hql, Account.class);
            query.setParameter("status", AccountStatus.ACTIVE);
            List<Account> accounts = query.getResultList();
            return accounts;
        }
    }

    @Override
    public List<Account> findAccountsWithBalanceGreaterThan(BigDecimal amount) {
        if(amount == null) {
            throw new IllegalArgumentException("Сумма не может быть null");
        }
        String hql = """
                FROM Account a
                JOIN FETCH a.client
                WHERE a.balance > :amount
                """;
        try (Session session = sessionFactory.openSession()) {
            SelectionQuery<Account> query = session.createQuery(hql, Account.class);
            query.setParameter("amount", amount);
            List<Account> accounts = query.getResultList();
            return accounts;
        }
    }

    @Override
    public List<Account> findAccountsByCurrency(String currency) {
        if(currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Валюта не может быть null или пустой строкой");
        }
        String hql = """
                FROM Account a
                JOIN FETCH a.client
                WHERE a.currency = :currency
                """;
        try (Session session = sessionFactory.openSession()) {
            SelectionQuery<Account> query = session.createQuery(hql, Account.class);
            query.setParameter("currency", Currency.valueOf(currency));
            List<Account> accounts = query.getResultList();
            return accounts;
        }
    }

    @Override
    public List<Account> findBlockedAccounts() {
        String hql = """
                FROM Account a
                JOIN FETCH a.client
                WHERE a.status = :status
                """;
        try (Session session = sessionFactory.openSession()) {
            SelectionQuery<Account> query = session.createQuery(hql, Account.class);
            query.setParameter("status", AccountStatus.BLOCKED);
            List<Account> accounts = query.getResultList();
            return accounts;
        }
    }

    @Override
    public void updateBalance(Long accountId, BigDecimal newBalance) {
        AccountBalanceValidator.validateForUpdate(accountId, newBalance);
        Session session = HibernateSessionContext.getCurrentSession();
        if(session == null) {
            throw new IllegalStateException("Транзакционная сессия отсутствует");
        }
        if(!session.getTransaction().isActive()) {
            throw new IllegalStateException("Hibernate-транзакция не активна");
        }
        String hql = """
                UPDATE Account a
                SET a.balance = :newBalance
                WHERE a.id = :accountId
                """;
        MutationQuery query = session.createMutationQuery(hql);
        query.setParameter("newBalance", newBalance);
        query.setParameter("accountId", accountId);
        int updateRows = query.executeUpdate();
        if(updateRows == 0) {
            throw new AccountNotFoundException("Счёт не найден");
        }
        Account account = session.get(Account.class, accountId);
        session.refresh(account);
    }

    @Override
    public void updateStatus(Long accountId, AccountStatus status) {
        if(accountId == null) {
            throw new IllegalArgumentException("ID счёта не может быть null");
        }
        if(status == null) {
            throw new IllegalArgumentException("Новый статус не может быть null");
        }
        Session session = HibernateSessionContext.getCurrentSession();
        if(session == null) {
            throw new IllegalStateException("Транзакционная сессия отсутствует");
        }
        if(!session.getTransaction().isActive()) {
            throw new IllegalStateException("Hibernate-транзакция не активна");
        }
        Account account = session.get(Account.class, accountId, LockMode.PESSIMISTIC_WRITE);
        if (account == null) {
            throw new AccountNotFoundException("Счёт не найден");
        }
        // Read the locked row even when the entity was already cached in this session.
        Object[] lockedState = session.createQuery(
                        "select a.status, a.balance from Account a where a.id = :accountId", Object[].class)
                .setParameter("accountId", accountId)
                .getSingleResultOrNull();
        if (lockedState == null) {
            throw new AccountNotFoundException("Счёт не найден");
        }
        boolean alreadyClosed = lockedState[0] == AccountStatus.CLOSED;
        if (alreadyClosed && status != AccountStatus.CLOSED) {
            throw new IllegalStateException("Закрытый счёт нельзя повторно открыть");
        }
        if (!alreadyClosed && status == AccountStatus.CLOSED && ((BigDecimal) lockedState[1]).signum() != 0) {
            throw new IllegalStateException("Баланс должен быть равен 0");
        }

        String hql = """
                UPDATE Account a
                SET a.status = :status,
                    a.closedAt = CASE
                        WHEN :status = :closedStatus THEN COALESCE(a.closedAt, instant)
                        ELSE a.closedAt
                    END
                WHERE a.id = :accountId
                """;
        MutationQuery query = session.createMutationQuery(hql);
        query.setParameter("status", status);
        query.setParameter("closedStatus", AccountStatus.CLOSED);
        query.setParameter("accountId", accountId);
        int updateRows = query.executeUpdate();
        if (updateRows == 0) {
            throw new AccountNotFoundException("Счёт не найден");
        }
        session.refresh(account);
    }

    @Override
    public Optional<Account> findByAccountNumberForUpdate(String accountNumber) {
        if(accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Номер счёта не может быть null или пустой строкой");
        }
        Session session = HibernateSessionContext.getCurrentSession();
        if(session == null) {
            throw new IllegalStateException("Транзакционная сессия отсутствует");
        }
        if(!session.getTransaction().isActive()) {
            throw new IllegalStateException("Hibernate-транзакция не активна");
        }
        String hql = """
                FROM Account a
                WHERE a.accountNumber = :accountNumber
                """;
        SelectionQuery<Account> query = session.createQuery(hql, Account.class);
        query.setParameter("accountNumber", accountNumber);
        query.setHibernateLockMode(LockMode.PESSIMISTIC_WRITE);
        Account account = query.getSingleResultOrNull();
        if(account != null) {
            Hibernate.initialize(account.getClient());
        }
        return Optional.ofNullable(account);
    }

    @Override
    public List<ClientBalanceSummary> getClientBalanceSummary() {
        String hql = """
                SELECT c.fullName,
                COUNT(a.id) AS account_count,
                COALESCE(SUM(a.balance), 0) AS total_balance,
                CAST(COALESCE(AVG(a.balance), 0) AS BigDecimal) AS average_balance
                FROM Client c
                LEFT JOIN c.accounts a
                GROUP BY c.id, c.fullName
                """;
        try (Session session = sessionFactory.openSession()) {
            SelectionQuery<Object[]> query = session.createQuery(hql, Object[].class);
            List<Object[]> rows = query.getResultList();
            List<ClientBalanceSummary> summaries = new ArrayList<>();
            for (Object[] row : rows) {
                String fullName = (String) row[0];
                long accountCount = (Long) row[1];
                BigDecimal totalBalance = (BigDecimal) row[2];
                BigDecimal averageBalance = (BigDecimal) row[3];
                ClientBalanceSummary summary = new ClientBalanceSummary(fullName, accountCount,
                        totalBalance, averageBalance);
                summaries.add(summary);
            }
            return summaries;
        }
    }

    @Override
    public List<ClientBalanceSummary> findRichClients(BigDecimal minTotalBalance) {
        String hql = """
                SELECT c.fullName,
                COUNT(a.id) AS account_count,
                COALESCE(SUM(a.balance), 0) AS total_balance,
                CAST(COALESCE(AVG(a.balance), 0) AS BigDecimal) AS average_balance
                FROM Client c
                LEFT JOIN c.accounts a
                GROUP BY c.id, c.fullName
                HAVING COALESCE(SUM(a.balance), 0) > :minTotalBalance
                """;
        if (minTotalBalance == null) {
            throw new IllegalArgumentException("Минимальный баланс не может быть null");
        }
        if (minTotalBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Минимальный баланс не может быть отрицательным");
        }
        try (Session session = sessionFactory.openSession()) {
            SelectionQuery<Object[]> query = session.createQuery(hql, Object[].class);
            query.setParameter("minTotalBalance", minTotalBalance);
            List<Object[]> rows = query.getResultList();
            List<ClientBalanceSummary> summaries = new ArrayList<>();
            for (Object[] objects : rows) {
                String fullName = (String) objects[0];
                long accountCount = (Long) objects[1];
                BigDecimal totalBalance = (BigDecimal) objects[2];
                BigDecimal averageBalance = (BigDecimal) objects[3];
                ClientBalanceSummary summary = new ClientBalanceSummary(fullName, accountCount,
                        totalBalance, averageBalance);
                summaries.add(summary);
            }
            return summaries;
        }
    }

    @Override
    public List<Account> findAccountsWithTransfers() {
        String hql = """
                FROM Account a
                JOIN FETCH a.client
                WHERE EXISTS (
                      SELECT 1
                      FROM Transfer t
                      WHERE t.fromAccount.id = a.id
                      OR t.toAccount.id = a.id
                )
                """;
        try (Session session = sessionFactory.openSession()) {
            SelectionQuery<Account> query = session.createQuery(hql, Account.class);
            List<Account> accounts = query.getResultList();
            return accounts;
        }
    }
}
