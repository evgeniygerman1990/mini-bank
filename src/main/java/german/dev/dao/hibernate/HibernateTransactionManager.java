package german.dev.dao.hibernate;

import german.dev.service.TransactionManager;
import org.hibernate.SessionFactory;
import java.util.function.Supplier;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class HibernateTransactionManager implements TransactionManager {

    private final SessionFactory sessionFactory;

    public HibernateTransactionManager(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public <T> T executeInTransaction(Supplier<T> action) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            HibernateSessionContext.setCurrentSession(session);
            try {
                T result = action.get();
                transaction.commit();
                return result;
            } catch (RuntimeException e) {
                try {
                    transaction.rollback();
                } catch (RuntimeException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            } finally {
                HibernateSessionContext.clear();
            }
        }
    }
}
