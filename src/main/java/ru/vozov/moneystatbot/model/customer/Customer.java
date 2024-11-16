package ru.vozov.moneystatbot.model.customer;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.vozov.moneystatbot.model.refill.Refill;
import ru.vozov.moneystatbot.model.expense.Expense;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "customer")
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Customer {
    @Id
    @Column(name = "chat_id")
    Long chatId;

    @Column(name = "created_at")
    LocalDateTime createAt;

    @Enumerated(EnumType.STRING)
    CustomerStatus status;

    @OneToMany(mappedBy = "customer")
    List<Refill> refillData;

    @OneToMany(mappedBy = "customer")
    List<Expense> expenseData;
}
