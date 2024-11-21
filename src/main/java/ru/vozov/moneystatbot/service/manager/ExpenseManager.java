package ru.vozov.moneystatbot.service.manager;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.vozov.moneystatbot.model.customer.Customer;
import ru.vozov.moneystatbot.model.customer.CustomerStatus;
import ru.vozov.moneystatbot.model.expense.Expense;
import ru.vozov.moneystatbot.model.expense.ExpenseType;
import ru.vozov.moneystatbot.repository.CustomerRepository;
import ru.vozov.moneystatbot.repository.ExpenseRepository;
import ru.vozov.moneystatbot.service.factory.AnswerMessageFactory;
import ru.vozov.moneystatbot.service.factory.KeyboardFactory;
import ru.vozov.moneystatbot.telegram.MoneyStatBot;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExpenseManager {
    final AnswerMessageFactory answerMessageFactory;
    final KeyboardFactory keyboardFactory;
    final CustomerRepository customerRepository;
    final ExpenseRepository expenseRepository;
    final MoneyStatBot bot;

    @Autowired
    public ExpenseManager(AnswerMessageFactory answerMessageFactory,
                         KeyboardFactory keyboardFactory,
                         CustomerRepository customerRepository,
                          ExpenseRepository expenseRepository,
                         @Lazy MoneyStatBot bot) {
        this.answerMessageFactory = answerMessageFactory;
        this.keyboardFactory = keyboardFactory;
        this.customerRepository = customerRepository;
        this.expenseRepository = expenseRepository;
        this.bot = bot;
    }

    public BotApiMethod<?> answerCommand(Message message) {
        Long chatId = message.getChatId();

        if (expenseRepository.existsByCustomerAndInCreation(customerRepository.findById(chatId).orElseThrow(),true)) {
            return answerMessageFactory.getSendMessage(
                    chatId,
                    "У вас есть списание в процессе создания, для того чтобы создать новое необходимо отменить предыдущее. После отмены повторите попытку.",
                    keyboardFactory.getInlineKeyboard(
                            List.of("Отменить предыдущее списание"),
                            List.of(1),
                            List.of("EXPENSE_CANCEL")
                    )
            );
        }
        return answerMessageFactory.getSendMessage(
                chatId,
                "Здесь вы можете сохранять Ваши списания денежных средств. Нажмите продолжить для создания.",
                keyboardFactory.getInlineKeyboard(
                        List.of("Продолжить",
                                "Отмена"),
                        List.of(1, 1),
                        List.of("EXPENSE_SUM","EXPENSE_CANCEL")
                )
        );
    }

    @Transactional
    public BotApiMethod<?> answerCallbackQuery(CallbackQuery callbackQuery) {
        String[] splitCallbackQuery = callbackQuery.getData().split("_");

        if (splitCallbackQuery.length == 1) {
            return startMessage(callbackQuery);
        }

        switch (splitCallbackQuery[1]) {
            case "SUM" -> {
                return askSum(callbackQuery);
            }
            case "TYPE" -> {
                return addType(callbackQuery, splitCallbackQuery[2]);
            }
            case "FINISH" -> {
                try {
                    return finish(callbackQuery);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
            case "CANCEL" -> {
                try {
                    return cancel(callbackQuery);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    public BotApiMethod<?> answerMessage(Message message, Customer customer) {
        CustomerStatus status = customer.getStatus();

        switch (status) {
            case SENDING_EXPENSE_SUM -> {
                return addSum(message, customer);
            }
            case SENDING_EXPENSE_DATE -> {
                return addDate(message, customer);
            }
            case SENDING_EXPENSE_DESC -> {
                return addDescription(message, customer);
            }
        }

        return null;
    }


    private BotApiMethod<?> startMessage(CallbackQuery callbackQuery) {
        if (expenseRepository.existsByCustomerAndInCreation(customerRepository.findById(callbackQuery.getMessage().getChatId()).orElseThrow(), true)) {
            return answerMessageFactory.getEditMessageText(
                    callbackQuery,
                    "У вас есть списание в процессе создания, для того чтобы создать новое необходимо отменить предыдущее. После отмены повторите попытку.",
                    keyboardFactory.getInlineKeyboard(
                            List.of("Отменить предыдущее списание",
                                    "Назад"),
                            List.of(1, 1),
                            List.of("EXPENSE_CANCEL","START")
                    )
            );
        }

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                "Здесь вы можете сохранять Ваши списания денежных средств. Нажмите продолжить для создания.",
                keyboardFactory.getInlineKeyboard(
                        List.of("Продолжить",
                                "Назад"),
                        List.of(1, 1),
                        List.of("EXPENSE_SUM","START")
                )
        );
    }

    private BotApiMethod<?> askSum(CallbackQuery callbackQuery) {
        Customer customer = customerRepository.findById(callbackQuery.getMessage().getChatId()).orElseThrow();
        customer.setStatus(CustomerStatus.SENDING_EXPENSE_SUM);

        Expense expense = Expense.builder()
                .customer(customer)
                .inCreation(true)
                .build();
        expenseRepository.save(expense);

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                "Введите сумму списания. Если число дробное, разделяйтя целую и дробную часть точкой. Например 243.21",
                keyboardFactory.getInlineKeyboard(
                        List.of("Отмена"),
                        List.of(1),
                        List.of("EXPENSE_CANCEL")
                )
        );
    }

    private BotApiMethod<?> addSum(Message message, Customer customer) {
        Long chatId = message.getChatId();
        boolean isCorrect = true;
        double sum = 0;

        try {
            sum = Double.parseDouble(message.getText());

            if (sum <= 0) {
                isCorrect = false;
            }
        }
        catch (NumberFormatException e) {
            isCorrect = false;
        }

        if (!isCorrect) {
            return answerMessageFactory.getSendMessage(
                    chatId,
                    "Некорректная сумма списания. Повторите попытку.",
                    keyboardFactory.getInlineKeyboard(
                            List.of("Отмена"),
                            List.of(1),
                            List.of("EXPENSE_CANCEL")
                    )
            );
        }

        customer.setStatus(CustomerStatus.SENDING_EXPENSE_DATE);
        customerRepository.save(customer);
        Expense expense = expenseRepository.findByCustomerAndInCreation(customer, true);
        expense.setSum(sum);
        expenseRepository.save(expense);

        return answerMessageFactory.getSendMessage(
                chatId,
                """
                        Введите дату списания в данном формате: дд.мм.гггг
                        
                        Например 01.05.2000
                        """,
                keyboardFactory.getInlineKeyboard(
                        List.of("Отмена"),
                        List.of(1),
                        List.of("EXPENSE_CANCEL")
                )
        );
    }

    private BotApiMethod<?> addDate(Message message, Customer customer) {
        Long chatId = message.getChatId();
        LocalDate date;

        try {
            date = LocalDate.parse(message.getText(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }
        catch (DateTimeParseException e) {
            return answerMessageFactory.getSendMessage(
                    chatId,
                    "Некорректная дата списания. Повторите попытку.",
                    keyboardFactory.getInlineKeyboard(
                            List.of("Отмена"),
                            List.of(1),
                            List.of("EXPENSE_CANCEL")
                    )
            );
        }

        customer.setStatus(CustomerStatus.FREE);
        customerRepository.save(customer);
        Expense expense = expenseRepository.findByCustomerAndInCreation(customer,true);
        expense.setDate(date);
        expenseRepository.save(expense);

        return answerMessageFactory.getSendMessage(
                chatId,
                "Выберите тип списания.",
                keyboardFactory.getInlineKeyboard(
                        List.of("Еда", "Транспорт", "Алкоголь",
                                "Дом","Книги", "Интернет",
                                "Здоровье", "Одежда", "Образование",
                                "Кафе", "Подарок", "Парикмахерская",
                                "Отмена"),
                        List.of(3, 3, 3, 3, 1),
                        List.of("EXPENSE_TYPE_" + ExpenseType.FOOD, "EXPENSE_TYPE_" + ExpenseType.TRANSPORT, "EXPENSE_TYPE_" + ExpenseType.ALCOHOL,
                                "EXPENSE_TYPE_" + ExpenseType.HOME, "EXPENSE_TYPE_" + ExpenseType.BOOK, "EXPENSE_TYPE_" + ExpenseType.INTERNET,
                                "EXPENSE_TYPE_" + ExpenseType.HEALTH, "EXPENSE_TYPE_" + ExpenseType.CLOTHES, "EXPENSE_TYPE_" + ExpenseType.EDUCATION,
                                "EXPENSE_TYPE_" + ExpenseType.CAFE, "EXPENSE_TYPE_" + ExpenseType.GIFT, "EXPENSE_TYPE_" + ExpenseType.BARBERSHOP,
                                "EXPENSE_CANCEL")
                )
        );
    }

    private BotApiMethod<?> addType(CallbackQuery callbackQuery, String type) {
        Long chatId = callbackQuery.getMessage().getChatId();
        Customer customer = customerRepository.findById(chatId).orElseThrow();
        Expense expense = expenseRepository.findByCustomerAndInCreation(customer, true);
        expense.setType(ExpenseType.valueOf(type));
        expenseRepository.save(expense);

        customer.setStatus(CustomerStatus.SENDING_EXPENSE_DESC);
        customerRepository.save(customer);

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                """
                        Укажите описание транзакции.
                        Если в этом нет надобности, можете нажать кнопку Завершить для сохранения списания.
                        """,
                keyboardFactory.getInlineKeyboard(
                        List.of("Завершить",
                                "Отмена"),
                        List.of(1, 1),
                        List.of("EXPENSE_FINISH",
                                "EXPENSE_CANCEL")
                )
        );
    }

    private BotApiMethod<?> addDescription(Message message, Customer customer) {
        Long chatId = message.getChatId();
        customer.setStatus(CustomerStatus.FREE);
        customerRepository.save(customer);
        Expense expense = expenseRepository.findByCustomerAndInCreation(customer, true);
        expense.setDescription(message.getText());
        expense.setInCreation(false);
        expenseRepository.save(expense);

        return answerMessageFactory.getSendMessage(
                chatId,
                "Списание успешно сохранено.",
                null
        );
    }

    private BotApiMethod<?> finish(CallbackQuery callbackQuery) throws TelegramApiException {
        Customer customer = customerRepository.findById(callbackQuery.getMessage().getChatId()).orElseThrow();
        customer.setStatus(CustomerStatus.FREE);
        customerRepository.save(customer);
        Expense expense = expenseRepository.findByCustomerAndInCreation(customer, true);
        expense.setInCreation(false);
        expenseRepository.save(expense);

        bot.execute(
                answerMessageFactory.getAnswerCallbackQuery(
                        callbackQuery,
                        "Списание успешно сохранено"
                )
        );

        return answerMessageFactory.getDeleteMessage(
                callbackQuery.getMessage().getMessageId(),
                callbackQuery.getMessage().getChatId()
        );
    }

    private BotApiMethod<?> cancel(CallbackQuery callbackQuery) throws TelegramApiException {
        Long chatId = callbackQuery.getMessage().getChatId();
        Customer customer = customerRepository.findById(chatId).orElseThrow();

        if (expenseRepository.existsByCustomerAndInCreation(customer, true)) {
            expenseRepository.deleteByCustomerAndInCreation(
                    customer,
                    true
            );
            customer.setStatus(CustomerStatus.FREE);
            customerRepository.save(customer);

            bot.execute(
                    answerMessageFactory.getAnswerCallbackQuery(
                            callbackQuery,
                            "Списание успешно отменено"
                    )
            );
        }
        else {
            bot.execute(
                    answerMessageFactory.getAnswerCallbackQuery(
                            callbackQuery,
                            "Невозможно отменить списание, так как оно уже создано"
                    )
            );
        }

        return answerMessageFactory.getDeleteMessage(
                callbackQuery.getMessage().getMessageId(),
                chatId
        );
    }
}
