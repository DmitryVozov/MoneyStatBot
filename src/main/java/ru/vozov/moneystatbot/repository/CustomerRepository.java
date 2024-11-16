package ru.vozov.moneystatbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.vozov.moneystatbot.model.customer.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
