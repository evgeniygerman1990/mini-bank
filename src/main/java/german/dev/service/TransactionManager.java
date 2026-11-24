package german.dev.service;

import java.util.function.Supplier;

public interface TransactionManager {
    <T> T executeInTransaction(Supplier<T> action);
}
