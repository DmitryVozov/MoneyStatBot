package ru.vozov.moneystatbot.model.refill;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.vozov.moneystatbot.model.customer.Customer;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "refill")
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Refill {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    Double sum;

    LocalDate date;

    @Enumerated(EnumType.STRING)
    RefillType type;

    String description;

    @ManyToOne
    @JoinColumn(name = "customer_chat_id", referencedColumnName = "chat_id")
    Customer customer;

    @Column(name = "in_creation")
    Boolean inCreation;
}
