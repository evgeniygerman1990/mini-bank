package german.dev.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "accounts")
@Getter
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", unique = true, nullable = false)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @CreationTimestamp
    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @OneToMany(mappedBy = "fromAccount")
    private List<Transfer> transfersFrom = new ArrayList<>();

    @OneToMany(mappedBy = "toAccount")
    private List<Transfer> transfersTo = new ArrayList<>();

    protected Account() {
    }

    public Account(String accountNumber, Currency currency, Client client) {
        this.accountNumber = accountNumber;
        this.currency = currency;
        this.status = AccountStatus.ACTIVE;
        this.client = client;
    }

    public List<Transfer> getTransfersFrom() {
        return Collections.unmodifiableList(transfersFrom);
    }

    public List<Transfer> getTransfersTo() {
        return Collections.unmodifiableList(transfersTo);
    }

    public void closeAccount() {
        if (status != AccountStatus.CLOSED) {
            if (balance.signum() != 0) {
                throw new IllegalStateException("Баланс должен быть равен 0");
            }
                this.status = AccountStatus.CLOSED;
                this.closedAt = Instant.now();
        }
    }
    public static Account fromDatabase (Long id, String accountNumber, Currency currency, BigDecimal balance,
                                        AccountStatus status, Instant openedAt, Instant closedAt, Client client) {
        Account account = new Account(accountNumber, currency, client);
        account.id = id;
        account.balance = balance;
        account.status = status;
        account.openedAt = openedAt;
        account.closedAt = closedAt;
        return account;
    }
}
