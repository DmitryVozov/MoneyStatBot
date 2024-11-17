package ru.vozov.moneystatbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.vozov.moneystatbot.model.customer.Customer;
import ru.vozov.moneystatbot.model.expense.Expense;
import ru.vozov.moneystatbot.model.refill.Refill;

import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    boolean existsByCustomerAndIsCreate(Customer customer, boolean isCreate);
    void deleteByCustomerAndIsCreate(Customer customer, boolean isCreate);
    Expense findByCustomerAndIsCreate(Customer customer, boolean isCreate);
}
