package german.dev.dao.hibernate;

import org.hibernate.Session;

public final class HibernateSessionContext {

    private static final ThreadLocal<Session> CURRENT_SESSION = new ThreadLocal<>();

    public static Session getCurrentSession() {
        return CURRENT_SESSION.get();
    }

    public static void setCurrentSession(Session session) {
        CURRENT_SESSION.set(session);
    }

    public static void clear() {
        CURRENT_SESSION.remove();
    }
}
