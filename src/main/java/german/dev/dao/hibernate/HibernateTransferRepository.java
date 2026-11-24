package german.dev.dao.hibernate;

import german.dev.domain.Transfer;
import german.dev.domain.TransferStatus;
import german.dev.repository.TransferRepository;
import german.dev.validation.TransferValidator;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import java.math.BigDecimal;
import java.util.List;

public class HibernateTransferRepository implements TransferRepository {

    private final SessionFactory sessionFactory;

    public HibernateTransferRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Transfer save(Transfer transfer) {
        TransferValidator.validateForSave(transfer);

        Session session = HibernateSessionContext.getCurrentSession();
        if (session == null) {
            throw new IllegalStateException("Транзакционная сессия отсутствует");
        }
        if (!session.getTransaction().isActive()) {
            throw new IllegalStateException("Hibernate-транзакция не активна");
        }

        session.persist(transfer);
        return transfer;
    }

    @Override
    public List<Transfer> findByAccountId(Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("ID счёта не может быть null");
        }

        String hql = """
                SELECT t
                FROM Transfer t
                JOIN FETCH t.fromAccount
                JOIN FETCH t.toAccount
                WHERE t.fromAccount.id = :accountId OR t.toAccount.id = :accountId
                ORDER BY t.createdAt DESC
                """;
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(hql, Transfer.class)
                    .setParameter("accountId", accountId)
                    .getResultList();
        }
    }

    @Override
    public List<Transfer> findOutgoingTransfers(Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("ID счёта не может быть null");
        }

        String hql = """
                SELECT t
                FROM Transfer t
                JOIN FETCH t.fromAccount
                JOIN FETCH t.toAccount
                WHERE t.fromAccount.id = :accountId
                ORDER BY t.createdAt DESC
                """;
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(hql, Transfer.class)
                    .setParameter("accountId", accountId)
                    .getResultList();
        }
    }

    @Override
    public List<Transfer> findIncomingTransfers(Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("ID счёта не может быть null");
        }

        String hql = """
                SELECT t
                FROM Transfer t
                JOIN FETCH t.fromAccount
                JOIN FETCH t.toAccount
                WHERE t.toAccount.id = :accountId
                ORDER BY t.createdAt DESC
                """;
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(hql, Transfer.class)
                    .setParameter("accountId", accountId)
                    .getResultList();
        }
    }

    @Override
    public List<Transfer> findSuccessfulTransfers() {
        String hql = """
                SELECT t
                FROM Transfer t
                JOIN FETCH t.fromAccount
                JOIN FETCH t.toAccount
                WHERE t.status = :status
                ORDER BY t.createdAt DESC
                """;
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(hql, Transfer.class)
                    .setParameter("status", TransferStatus.SUCCESS)
                    .getResultList();
        }
    }

    @Override
    public BigDecimal getTotalOutgoingAmount(Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("ID счёта не может быть null");
        }

        String hql = """
                SELECT COALESCE(SUM(t.amount), 0)
                FROM Transfer t
                WHERE t.fromAccount.id = :accountId
                  AND t.status = :status
                """;
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(hql, BigDecimal.class)
                    .setParameter("accountId", accountId)
                    .setParameter("status", TransferStatus.SUCCESS)
                    .getSingleResult();
        }
    }

    @Override
    public BigDecimal getTotalIncomingAmount(Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("ID счёта не может быть null");
        }

        String hql = """
                SELECT COALESCE(SUM(t.amount), 0)
                FROM Transfer t
                WHERE t.toAccount.id = :accountId
                  AND t.status = :status
                """;
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(hql, BigDecimal.class)
                    .setParameter("accountId", accountId)
                    .setParameter("status", TransferStatus.SUCCESS)
                    .getSingleResult();
        }
    }

    @Override
    public List<Transfer> findTransfersWithoutComment() {
        String hql = """
                SELECT t
                FROM Transfer t
                JOIN FETCH t.fromAccount
                JOIN FETCH t.toAccount
                WHERE t.comment IS NULL
                ORDER BY t.createdAt DESC
                """;
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(hql, Transfer.class).getResultList();
        }
    }

    @Override
    public List<Transfer> findLargeTransfers(BigDecimal minAmount) {
        if (minAmount == null) {
            throw new IllegalArgumentException("Сумма перевода отсутствует");
        }
        if (minAmount.signum() < 0) {
            throw new IllegalArgumentException("Сумма не может быть отрицательной");
        }

        String hql = """
                SELECT t
                FROM Transfer t
                JOIN FETCH t.fromAccount
                JOIN FETCH t.toAccount
                WHERE t.amount > :minAmount
                ORDER BY t.createdAt DESC
                """;
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(hql, Transfer.class)
                    .setParameter("minAmount", minAmount)
                    .getResultList();
        }
    }
}
