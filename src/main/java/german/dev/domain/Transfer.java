package german.dev.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.Instant;


@Entity
@Table(name = "transfers")
@Getter
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_account_id", nullable = false)
    private Account toAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private String comment;

    protected Transfer() {
    }

    public Transfer(Account fromAccount, Account toAccount, BigDecimal amount, String comment) {
        if (fromAccount == null || toAccount == null || amount == null) {
            throw new IllegalArgumentException("Счета и сумма перевода не должны быть null");
        }
        if (fromAccount == toAccount ||
                fromAccount.getId() != null && toAccount.getId() != null &&
                        fromAccount.getId().equals(toAccount.getId())) {
            throw new IllegalArgumentException("Счета должны быть разные");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Сумма перевода должна быть больше 0");
        }
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.status = TransferStatus.CREATED;
        this.comment = comment;
    }

    public void markSuccess() {
        if (status == TransferStatus.FAILED) {
            throw new IllegalStateException("Статус FAILED изменить нельзя");
        }
        if (status == TransferStatus.CREATED) {
            this.status = TransferStatus.SUCCESS;
        }
    }

    public void markFailed() {
        if (status == TransferStatus.SUCCESS) {
            throw new IllegalStateException("Статус SUCCESS изменить нельзя");
        }
        if (status == TransferStatus.CREATED) {
            this.status = TransferStatus.FAILED;
        }
    }

    public void assignGeneratedId(Long generatedId) {
        if (generatedId == null || generatedId <= 0) {
            throw new IllegalArgumentException("Некорректный ID перевода");
        }
        if (this.id != null) {
            throw new IllegalStateException("ID уже существует");
        }
        this.id = generatedId;
    }

    public void assignCreatedAt(Instant createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("Дата не может быть NULL");
        }
        if (this.createdAt != null) {
            throw new IllegalStateException("Дата создания перевода уже установлена");
        }
        this.createdAt = createdAt;
    }

    public static Transfer fromDatabase(
            Long id,
            Account fromAccount,
            Account toAccount,
            BigDecimal amount,
            TransferStatus status,
            String comment,
            Instant createdAt
    ) {
        Transfer newTransfer = new Transfer(fromAccount, toAccount, amount, comment);
        newTransfer.id = id;
        newTransfer.status = status;
        newTransfer.createdAt = createdAt;
        return newTransfer;
    }
}
