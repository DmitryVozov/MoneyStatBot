package ru.vozov.moneystatbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.vozov.moneystatbot.model.customer.Customer;
import ru.vozov.moneystatbot.model.expense.Expense;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    boolean existsByCustomerAndInCreation(Customer customer, boolean inCreation);
    void deleteByCustomerAndInCreation(Customer customer, boolean inCreation);
    Expense findByCustomerAndInCreation(Customer customer, boolean inCreation);

    @Query(value = """
            select distinct date_part('year', e.date)
                from expense e
                where e.customer_chat_id = :customer_chat_id
                    and e.in_creation = false
                order by date_part('year', e.date)
            """,
            nativeQuery = true)
    List<Object[]> getDistinctYearsByCustomer(@Param("customer_chat_id") Long customerChatId);

    @Query(value = """
            select  t.type
                ,   t.sum
                from (
                    select  e.type type
                        ,   sum(e.sum) sum
                        from expense e
                        where e.customer_chat_id = :customer_chat_id
                            and e.in_creation = false
                        group by e.type
                ) t
                order by t.sum desc
            """,
            nativeQuery = true)
    List<Object[]> getSumForAllTimeByCustomerGroupByType(@Param("customer_chat_id") Long customerChatId);

    @Query(value = """
                select sum(e.sum)
                    from expense e
                    where e.customer_chat_id = :customer_chat_id
                        and e.in_creation = false
            """,
            nativeQuery = true)
    Object[] getSumForAllTimeByCustomer(@Param("customer_chat_id") Long customerChatId);

    @Query(value = """
            select  t.type
                ,   t.sum
                from (
                    select  e.type type
                        ,   sum(e.sum) sum
                        from expense e
                        where e.customer_chat_id = :customer_chat_id
                            and date_part('year', e.date) = :year
                            and e.in_creation = false
                        group by e.type
                ) t
                order by t.sum desc
            """,
            nativeQuery = true)
    List<Object[]> getSumForYearByCustomerGroupByType(@Param("customer_chat_id") Long customerChatId, @Param("year") Integer year);

    @Query(value = """
                select sum(e.sum)
                    from expense e
                    where e.customer_chat_id = :customer_chat_id
                        and date_part('year', e.date) = :year
                        and e.in_creation = false
            """,
            nativeQuery = true)
    Object[] getSumForYearByCustomer(@Param("customer_chat_id") Long customerChatId, @Param("year") Integer year);

    @Query(value = """
            select  t.type
                ,   t.sum
                from (
                    select  e.type type
                        ,   sum(e.sum) sum
                        from expense e
                        where e.customer_chat_id = :customer_chat_id
                            and date_part('year', e.date) = :year
                            and date_part('month', e.date) = :month
                            and e.in_creation = false
                        group by e.type
                ) t
                order by t.sum desc
            """,
            nativeQuery = true)
    List<Object[]> getSumForMonthByCustomerGroupByType(@Param("customer_chat_id") Long customerChatId, @Param("year") Integer year, @Param("month") Integer month);

    @Query(value = """
                select sum(e.sum)
                    from expense e
                    where e.customer_chat_id = :customer_chat_id
                        and date_part('year', e.date) = :year
                        and date_part('month', e.date) = :month
                        and e.in_creation = false
            """,
            nativeQuery = true)
    Object[] getSumForMonthByCustomer(@Param("customer_chat_id") Long customerChatId, @Param("year") Integer year, @Param("month") Integer month);

    @Query(value = """
            select  t.type
                ,   t.sum
                from (
                    select  e.type type
                        ,   sum(e.sum) sum
                        from expense e
                        where r.customer_chat_id = :customer_chat_id
                            and e.date = :date
                            and e.in_creation = false
                        group by e.type
                ) t
                order by t.sum desc
            """,
            nativeQuery = true)
    List<Object[]> getSumForDayByCustomerGroupByType(@Param("customer_chat_id") Long customerChatId, @Param("date") LocalDate date);

    @Query(value = """
                select sum(e.sum)
                    from expense e
                    where e.customer_chat_id = :customer_chat_id
                        and e.date = :date
                        and e.in_creation = false
            """,
            nativeQuery = true)
    Object[] getSumForDayByCustomer(@Param("customer_chat_id") Long customerChatId, @Param("date") LocalDate date);
}
