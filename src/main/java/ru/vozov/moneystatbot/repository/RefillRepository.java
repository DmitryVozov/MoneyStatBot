package ru.vozov.moneystatbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.vozov.moneystatbot.model.customer.Customer;
import ru.vozov.moneystatbot.model.refill.Refill;

import java.util.UUID;

@Repository
public interface RefillRepository extends JpaRepository<Refill, UUID> {
    boolean existsByCustomerAndIsCreate(Customer customer, boolean isCreate);
    void deleteByCustomerAndIsCreate(Customer customer, boolean isCreate);
    Refill findByCustomerAndIsCreate(Customer customer, boolean isCreate);
}
