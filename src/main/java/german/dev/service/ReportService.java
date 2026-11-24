package german.dev.service;

import german.dev.domain.Transfer;
import german.dev.exception.AccountNotFoundException;
import german.dev.repository.AccountRepository;
import german.dev.domain.Account;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import german.dev.repository.TransferRepository;
import german.dev.repository.dto.ClientBalanceSummary;

public class ReportService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;

    public ReportService(AccountRepository accountRepository, TransferRepository transferRepository) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
    }

    public void printClientAccountsReport(Long clientId) {
        if(clientId == null) {
            throw new IllegalArgumentException("ID клиента не может быть null");
        }
        List<Account> accounts = accountRepository.findAllByClientId(clientId);
        if(accounts.isEmpty()) {
            System.out.println("У клиента нет счетов");
            return;
        }
        String clientName = accounts.get(0).getClient().getFullName();
        System.out.println("Клиент: " + clientName);
        for(Account account : accounts) {
            System.out.println("Номер счёта: " + account.getAccountNumber());
            System.out.println("Валюта: " + account.getCurrency());
            System.out.println("Баланс: " + formatMoney(account.getBalance()));
            System.out.println("Статус: " + account.getStatus());
            System.out.println();
        }
    }

    public void printAccountTransferHistory(String accountNumber) {
        if(accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Номер счёта не может быть null или пустой строкой");
        }
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Счёт не найден"));
        List<Transfer> transfers = transferRepository.findByAccountId(account.getId());
        if(transfers.isEmpty()) {
            System.out.println("По данному счёту переводов нет");
            return;
        }
        for(Transfer transfer : transfers) {
            String operationType;
            if(transfer.getFromAccount().getId().equals(account.getId())) {
                operationType = "OUTCOME";
            } else {
                operationType = "INCOME";
            }
            System.out.println("Дата перевода: " + transfer.getCreatedAt());
            System.out.println("Тип операции: " + operationType);
            System.out.println("Счёт отправителя: " + transfer.getFromAccount().getAccountNumber());
            System.out.println("Счёт получателя: " + transfer.getToAccount().getAccountNumber());
            System.out.println("Сумма перевода: " + formatMoney(transfer.getAmount()));
            System.out.println("Статус перевода: " + transfer.getStatus());
            if(transfer.getComment() == null) {
                System.out.println("Комментарий: отсутствует");
            } else {
                System.out.println("Комментарий: " + transfer.getComment());
            }
            System.out.println();
        }
    }

    public void printClientBalanceSummary() {
        List<ClientBalanceSummary> summaries = accountRepository.getClientBalanceSummary();
        if(summaries.isEmpty()) {
            System.out.println("Данные о клиентах отсутствуют");
            return;
        }
        for (ClientBalanceSummary summary : summaries) {
            System.out.println("ФИО клиента: " + summary.fullName());
            System.out.println("Количество счетов клиента: " + summary.accountCount());
            System.out.println("Сумма денег на всех счетах: " + formatMoney(summary.totalBalance()));
            System.out.println("Средний баланс счёта: " + formatMoney(summary.averageBalance()));
            System.out.println();
        }
    }

    public void printRichClients(BigDecimal minTotalBalance) {
        if(minTotalBalance == null) {
            throw new IllegalArgumentException("Минимальный баланс не может быть null");
        }
        if(minTotalBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Минимальный баланс не может быть отрицательным");
        }
        List<ClientBalanceSummary> richClients = accountRepository.findRichClients(minTotalBalance);
        if(richClients.isEmpty()) {
            System.out.println("Клиенты с указанным балансом не найдены");
            return;
        }
        for(ClientBalanceSummary richClient : richClients) {
            System.out.println("ФИО клиента: " + richClient.fullName());
            System.out.println("Количество счетов клиента: " + richClient.accountCount());
            System.out.println("Сумма денег на всех счетах: " + formatMoney(richClient.totalBalance()));
            System.out.println("Средний баланс счёта: " + formatMoney(richClient.averageBalance()));
            System.out.println();
        }
    }

    public void printAccountsWithTransfers() {
        List<Account> accounts = accountRepository.findAccountsWithTransfers();
        if(accounts.isEmpty()) {
            System.out.println("Счета с переводами не найдены");
            return;
        }
        for (Account account : accounts) {
            System.out.println("Номер счёта: " + account.getAccountNumber());
            System.out.println("Валюта: " + account.getCurrency());
            System.out.println("Баланс: " + formatMoney(account.getBalance()));
            System.out.println("Статус: " + account.getStatus());
            System.out.println();
        }
    }

    private String formatMoney(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
