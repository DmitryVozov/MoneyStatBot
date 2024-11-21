package ru.vozov.moneystatbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.vozov.moneystatbot.model.customer.Customer;
import ru.vozov.moneystatbot.model.refill.Refill;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface RefillRepository extends JpaRepository<Refill, UUID> {
    boolean existsByCustomerAndInCreation(Customer customer, boolean inCreation);
    void deleteByCustomerAndInCreation(Customer customer, boolean inCreation);
    Refill findByCustomerAndInCreation(Customer customer, boolean inCreation);

    @Query(value = """
            select distinct date_part('year', r.date)
                from refill r
                where r.customer_chat_id = :customer_chat_id
                    and r.in_creation = false
                order by date_part('year', r.date)
            """,
            nativeQuery = true)
    List<Object[]> getDistinctYearsByCustomer(@Param("customer_chat_id") Long customerChatId);

    @Query(value = """
            select  t.type
                ,   t.sum
                from (
                    select  r.type type
                        ,   sum(r.sum) sum
                        from refill r
                        where r.customer_chat_id = :customer_chat_id
                            and r.in_creation = false
                        group by r.type
                ) t
                order by t.sum desc
            """,
            nativeQuery = true)
    List<Object[]> getSumForAllTimeByCustomerGroupByType(@Param("customer_chat_id") Long customerChatId);

    @Query(value = """
                select sum(r.sum)
                    from refill r
                    where r.customer_chat_id = :customer_chat_id
                        and r.in_creation = false
            """,
            nativeQuery = true)
    Object[] getSumForAllTimeByCustomer(@Param("customer_chat_id") Long customerChatId);

    @Query(value = """
            select  t.type
                ,   t.sum
                from (
                    select  r.type type
                        ,   sum(r.sum) sum
                        from refill r
                        where r.customer_chat_id = :customer_chat_id
                            and date_part('year', r.date) = :year
                            and r.in_creation = false
                        group by r.type
                ) t
                order by t.sum desc
            """,
            nativeQuery = true)
    List<Object[]> getSumForYearByCustomerGroupByType(@Param("customer_chat_id") Long customerChatId, @Param("year") Integer year);

    @Query(value = """
                select sum(r.sum)
                    from refill r
                    where r.customer_chat_id = :customer_chat_id
                        and date_part('year', r.date) = :year
                        and r.in_creation = false
            """,
            nativeQuery = true)
    Object[] getSumForYearByCustomer(@Param("customer_chat_id") Long customerChatId, @Param("year") Integer year);

    @Query(value = """
            select  t.type
                ,   t.sum
                from (
                    select  r.type type
                        ,   sum(r.sum) sum
                        from refill r
                        where r.customer_chat_id = :customer_chat_id
                            and date_part('year', r.date) = :year
                            and date_part('month', r.date) = :month
                            and r.in_creation = false
                        group by r.type
                ) t
                order by t.sum desc
            """,
            nativeQuery = true)
    List<Object[]> getSumForMonthByCustomerGroupByType(@Param("customer_chat_id") Long customerChatId, @Param("year") Integer year, @Param("month") Integer month);

    @Query(value = """
                select sum(r.sum)
                    from refill r
                    where r.customer_chat_id = :customer_chat_id
                        and date_part('year', r.date) = :year
                        and date_part('month', r.date) = :month
                        and r.in_creation = false
            """,
            nativeQuery = true)
    Object[] getSumForMonthByCustomer(@Param("customer_chat_id") Long customerChatId, @Param("year") Integer year, @Param("month") Integer month);

    @Query(value = """
            select  t.type
                ,   t.sum
                from (
                    select  r.type type
                        ,   sum(r.sum) sum
                        from refill r
                        where r.customer_chat_id = :customer_chat_id
                            and r.date = :date
                            and r.in_creation = false
                        group by r.type
                ) t
                order by t.sum desc
            """,
            nativeQuery = true)
    List<Object[]> getSumForDayByCustomerGroupByType(@Param("customer_chat_id") Long customerChatId, @Param("date") LocalDate date);

    @Query(value = """
                select sum(r.sum)
                    from refill r
                    where r.customer_chat_id = :customer_chat_id
                        and r.date = :date
                        and r.in_creation = false
            """,
            nativeQuery = true)
    Object[] getSumForDayByCustomer(@Param("customer_chat_id") Long customerChatId, @Param("date") LocalDate date);
}
