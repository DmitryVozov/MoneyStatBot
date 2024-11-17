package ru.vozov.moneystatbot.model.expense;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.vozov.moneystatbot.model.customer.Customer;


import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "expense")
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    Double sum;

    LocalDate date;

    @Enumerated(EnumType.STRING)
    ExpenseType type;

    String description;

    @ManyToOne
    @JoinColumn(name = "customer_chat_id", referencedColumnName = "chat_id")
    Customer customer;

    @Column(name = "is_create")
    Boolean isCreate;
}
