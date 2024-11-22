package ru.vozov.moneystatbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.vozov.moneystatbot.model.customer.Customer;
import ru.vozov.moneystatbot.model.operation.Operation;
import ru.vozov.moneystatbot.model.operation.OperationType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface OperationRepository extends JpaRepository<Operation, UUID> {
    boolean existsByCustomerAndInCreationAndType(Customer customer, boolean inCreation, OperationType type);
    void deleteByCustomerAndInCreationAndType(Customer customer, boolean inCreation, OperationType type);
    Operation findByCustomerAndInCreationAndType(Customer customer, boolean inCreation, OperationType type);

    @Query(value = """
            select distinct date_part('year', o.date)
                from operation o
                where o.customer_chat_id = :customer_chat_id
                    and o.type = :type
                    and o.in_creation = false
                order by date_part('year', o.date)
            """,
            nativeQuery = true)
    List<Object[]> getDistinctYearsByCustomerChatIdAndType(
            @Param("customer_chat_id") Long customerChatId,
            @Param("type") String type
    );

    @Query(value = """
            select  t.category
                ,   t.sum
                from (
                    select  o.category category
                        ,   sum(o.sum) sum
                        from operation o
                        where o.customer_chat_id = :customer_chat_id
                            and o.type = :type
                            and o.in_creation = false
                        group by o.category
                ) t
                order by t.sum desc
            """,
            nativeQuery = true)
    List<Object[]> getSumForAllTimeByCustomerChatIdAndTypeGroupByCategory(
            @Param("customer_chat_id") Long customerChatId,
            @Param("type") String type
    );

    @Query(value = """
                select sum(o.sum)
                    from operation o
                    where o.customer_chat_id = :customer_chat_id
                        and o.type = :type
                        and o.in_creation = false
            """,
            nativeQuery = true)
    Object[] getSumForAllTimeByCustomerChatIdAndType(
            @Param("customer_chat_id") Long customerChatId,
            @Param("type") String type
    );

    @Query(value = """
            select  t.category
                ,   t.sum
                from (
                    select  o.category category
                        ,   sum(o.sum) sum
                        from operation o
                        where o.customer_chat_id = :customer_chat_id
                            and o.type = :type
                            and date_part('year', o.date) = :year
                            and o.in_creation = false
                        group by o.category
                ) t
                order by t.sum desc
            """,
            nativeQuery = true)
    List<Object[]> getSumForYearByCustomerChatIdAndTypeGroupByCategory(
            @Param("customer_chat_id") Long customerChatId,
            @Param("type") String type,
            @Param("year") Integer year
    );

    @Query(value = """
                select sum(o.sum)
                    from operation o
                    where o.customer_chat_id = :customer_chat_id
                        and o.type = :type
                        and date_part('year', o.date) = :year
                        and o.in_creation = false
            """,
            nativeQuery = true)
    Object[] getSumForYearByCustomerChatIdAndType(
            @Param("customer_chat_id") Long customerChatId,
            @Param("type") String type,
            @Param("year") Integer year
    );

    @Query(value = """
            select  t.category
                ,   t.sum
                from (
                    select  o.category category
                        ,   sum(o.sum) sum
                        from operation o
                        where o.customer_chat_id = :customer_chat_id
                            and o.type = :type
                            and date_part('year', o.date) = :year
                            and date_part('month', o.date) = :month
                            and o.in_creation = false
                        group by o.category
                ) t
                order by t.sum desc
            """,
            nativeQuery = true)
    List<Object[]> getSumForMonthByCustomerChatIdAndTypeGroupByCategory(
            @Param("customer_chat_id") Long customerChatId,
            @Param("type") String type,
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    @Query(value = """
                select sum(o.sum)
                    from operation o
                    where o.customer_chat_id = :customer_chat_id
                        and o.type = :type
                        and date_part('year', o.date) = :year
                        and date_part('month', o.date) = :month
                        and o.in_creation = false
            """,
            nativeQuery = true)
    Object[] getSumForMonthByCustomerChatIdAndType(
            @Param("customer_chat_id") Long customerChatId,
            @Param("type") String type,
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    @Query(value = """
            select  t.category
                ,   t.sum
                from (
                    select  o.category category
                        ,   sum(o.sum) sum
                        from operation o
                        where o.customer_chat_id = :customer_chat_id
                            and o.date = :date
                            and o.type = :type
                            and o.in_creation = false
                        group by o.category
                ) t
                order by t.sum desc
            """,
            nativeQuery = true)
    List<Object[]> getSumForDayByCustomerChatIdAndTypeGroupByCategory(
            @Param("customer_chat_id") Long customerChatId,
            @Param("date") LocalDate date,
            @Param("type") String type
    );

    @Query(value = """
                select sum(o.sum)
                    from operation o
                    where o.customer_chat_id = :customer_chat_id
                        and o.date = :date
                        and o.type = :type
                        and o.in_creation = false
            """,
            nativeQuery = true)
    Object[] getSumForDayByCustomerChatIdAndType(
            @Param("customer_chat_id") Long customerChatId,
            @Param("date") LocalDate date,
            @Param("type") String type
    );

    @Query(value = """
                select *
                    from operation o
                    where o.customer_chat_id = :customer_chat_id
                        and o.type = :type
                        and o.in_creation = false
                    order by o.date desc
            """,
            nativeQuery = true)
    List<Operation> findByCustomerChatIdAndType(
            @Param("customer_chat_id") Long customerChatId,
            @Param("type") String type
    );

    @Query(value = """
                select *
                    from operation o
                    where o.customer_chat_id = :customer_chat_id
                        and o.type = :type
                        and date_part('year', o.date) = :year
                        and o.in_creation = false
                    order by o.date desc
            """,
            nativeQuery = true)
    List<Operation> findByCustomerChatIdAndTypeAndYear(
            @Param("customer_chat_id") Long customerChatId,
            @Param("type") String type,
            @Param("year") Integer year
    );

    @Query(value = """
                select *
                    from operation o
                    where o.customer_chat_id = :customer_chat_id
                        and o.type = :type
                        and date_part('year', o.date) = :year
                        and date_part('month', o.date) = :month
                        and o.in_creation = false
                    order by o.date desc
            """,
            nativeQuery = true)
    List<Operation> findByCustomerChatIdAndTypeAndMonth(
            @Param("customer_chat_id") Long customerChatId,
            @Param("type") String type,
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    @Query(value = """
                select *
                    from operation o
                    where o.customer_chat_id = :customer_chat_id
                        and o.type = :type
                        and o.date = :date
                        and o.in_creation = false
                    order by o.date desc
            """,
            nativeQuery = true)
    List<Operation> findByCustomerChatIdAndTypeAndDate(
            @Param("customer_chat_id") Long customerChatId,
            @Param("type") String type,
            @Param("date") LocalDate date
    );
}
