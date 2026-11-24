package german.dev.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "clients")
@Getter
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    @Setter
    private String fullName;

    @Column(name = "phone", nullable = false, length = 15)
    @Setter
    private String phone;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @OneToMany(mappedBy = "client")
    private List<Account> accounts = new ArrayList<>();

    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    protected Client () {
    }

    public Client (String fullName, String phone) {
        this.fullName = fullName;
        this.phone = phone;
    }

    public static Client fromDatabase (Long id, String fullName, String phone, Instant createdAt) {
        Client client = new Client(fullName, phone);
        client.id = id;
        client.createdAt = createdAt;
        return client;
    }
}