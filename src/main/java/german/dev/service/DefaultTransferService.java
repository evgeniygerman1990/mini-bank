    package german.dev.service;

    import german.dev.domain.Account;
    import german.dev.domain.AccountStatus;
    import german.dev.domain.Transfer;
    import german.dev.repository.AccountRepository;
    import german.dev.repository.TransferRepository;
    import java.math.BigDecimal;
    import german.dev.exception.AccountNotFoundException;
    import german.dev.exception.NotEnoughMoneyException;
    import german.dev.exception.InvalidTransferException;


    public class DefaultTransferService implements TransferService{

        private final AccountRepository accountRepository;
        private final TransferRepository transferRepository;
        private final TransactionManager transactionManager;

        public DefaultTransferService(AccountRepository accountRepository, TransferRepository transferRepository, TransactionManager transactionManager) {
            this.accountRepository = accountRepository;
            this.transferRepository = transferRepository;
            this.transactionManager = transactionManager;
        }

        @Override
        public void transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount, String comment) {
            if(amount == null) {
                throw new InvalidTransferException("Должна быть указана сумма перевода");
            }
            if(amount.compareTo(BigDecimal.ZERO) <= 0 ) {
                throw new InvalidTransferException("Сумма перевода должна быть больше 0");
            }
            if (amount.stripTrailingZeros().scale() > 2) {
                throw new InvalidTransferException("Сумма перевода должна иметь не более двух знаков после запятой");
            }
            if(fromAccountNumber == null || fromAccountNumber.isBlank()) {
                throw new InvalidTransferException("Номер счёта отправителя не может быть null или пустой строкой");
            }
            if(toAccountNumber == null || toAccountNumber.isBlank()) {
                throw new InvalidTransferException("Номер счёта получателя не может быть null или пустой строкой");
            }
            if(fromAccountNumber.equals(toAccountNumber)) {
                throw new InvalidTransferException("Номера счетов отправителя и получателя должны быть разными");
            }
            transactionManager.executeInTransaction(() -> {
                String firstAccountNumber;
                String secondAccountNumber;
                if(fromAccountNumber.compareTo(toAccountNumber) < 0) {
                    firstAccountNumber = fromAccountNumber;
                    secondAccountNumber = toAccountNumber;
                } else {
                    firstAccountNumber = toAccountNumber;
                    secondAccountNumber = fromAccountNumber;
                }
                Account firstAccount = accountRepository.findByAccountNumberForUpdate(firstAccountNumber)
                        .orElseThrow(() -> new AccountNotFoundException("Первый счёт не найден"));
                Account secondAccount = accountRepository.findByAccountNumberForUpdate(secondAccountNumber)
                        .orElseThrow(() -> new AccountNotFoundException("Второй счёт не найден"));
                Account fromAccount;
                Account toAccount;
                if(firstAccountNumber.equals(fromAccountNumber)) {
                    fromAccount = firstAccount;
                    toAccount = secondAccount;
                } else {
                    fromAccount = secondAccount;
                    toAccount = firstAccount;
                }
                if(fromAccount.getStatus() != AccountStatus.ACTIVE) {
                    throw new InvalidTransferException("Счёт отправителя не активен");
                }
                if(toAccount.getStatus() != AccountStatus.ACTIVE) {
                    throw new InvalidTransferException("Счёт получателя не активен");
                }
                if(fromAccount.getCurrency() != toAccount.getCurrency()) {
                    throw new InvalidTransferException("Переводы между счетами с разной валютой не поддерживаются");
                }
                if(fromAccount.getBalance().compareTo(amount) < 0) {
                    throw new NotEnoughMoneyException("Недостаточно средств на счёте");
                }
                BigDecimal newFromBalance = fromAccount.getBalance().subtract(amount);
                BigDecimal newToBalance = toAccount.getBalance().add(amount);
                accountRepository.updateBalance(fromAccount.getId(), newFromBalance);
                accountRepository.updateBalance(toAccount.getId(), newToBalance);
                Transfer transfer = new Transfer(fromAccount, toAccount, amount, comment);
                transfer.markSuccess();
                transferRepository.save(transfer);
                return null;
            });
        }
    }
