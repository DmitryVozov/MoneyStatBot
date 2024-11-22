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
import ru.vozov.moneystatbot.model.operation.ExpenseCategory;
import ru.vozov.moneystatbot.model.operation.IncomeCategory;
import ru.vozov.moneystatbot.model.operation.Operation;
import ru.vozov.moneystatbot.model.operation.OperationType;
import ru.vozov.moneystatbot.repository.CustomerRepository;
import ru.vozov.moneystatbot.repository.OperationRepository;
import ru.vozov.moneystatbot.service.factory.AnswerMessageFactory;
import ru.vozov.moneystatbot.service.factory.KeyboardFactory;
import ru.vozov.moneystatbot.telegram.MoneyStatBot;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OperationManager {
    final AnswerMessageFactory answerMessageFactory;
    final KeyboardFactory keyboardFactory;
    final CustomerRepository customerRepository;
    final OperationRepository operationRepository;
    final MoneyStatBot bot;

    @Autowired
    public OperationManager(AnswerMessageFactory answerMessageFactory,
                            KeyboardFactory keyboardFactory,
                            CustomerRepository customerRepository,
                            OperationRepository operationRepository,
                            @Lazy MoneyStatBot bot) {
        this.answerMessageFactory = answerMessageFactory;
        this.keyboardFactory = keyboardFactory;
        this.customerRepository = customerRepository;
        this.operationRepository = operationRepository;
        this.bot = bot;
    }

    public BotApiMethod<?> answerCommand(Message message) {
        Long chatId = message.getChatId();
        OperationType type = message.getText().equals("/income") ? OperationType.INCOME : OperationType.EXPENSE;

        if (operationRepository.existsByCustomerAndInCreationAndType(customerRepository.findById(chatId).orElseThrow(),true, type)) {
            return answerMessageFactory.getSendMessage(
                    chatId,
                    getTextByTransactionType(
                            "У вас есть %s в процессе создания, для того чтобы создать новое необходимо отменить предыдущее. После отмены повторите попытку.",
                            "EXISTS_IN_CREATION",
                            type
                    ),
                    keyboardFactory.getInlineKeyboard(
                            List.of(
                                    getTextByTransactionType(
                                            "Отменить предыдущее %s",
                                            "CANCEL_PREVIOUS",
                                            type
                                    )
                            ),
                            List.of(1),
                            List.of(type + "_CANCEL")
                    )
            );
        }

        return answerMessageFactory.getSendMessage(
                chatId,
                getTextByTransactionType(
                        "Здесь Вы можете сохранять Ваши %s денежных средств. Нажмите продолжить для создания.",
                        "START",
                        type
                ),
                keyboardFactory.getInlineKeyboard(
                        List.of("Продолжить",
                                "Отмена"),
                        List.of(1, 1),
                        List.of(type + "_SUM", type + "_CANCEL")
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
                return askSum(callbackQuery, splitCallbackQuery[0]);
            }
            case "CATEGORY" -> {
                return addCategory(callbackQuery, splitCallbackQuery[0],  splitCallbackQuery[2]);
            }
            case "FINISH" -> {
                try {
                    return finish(callbackQuery, splitCallbackQuery[0]);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
            case "CANCEL" -> {
                try {
                    return cancel(callbackQuery, splitCallbackQuery[0]);
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
            case SENDING_INCOME_SUM, SENDING_EXPENSE_SUM -> {
                return addSum(message, customer);
            }
            case SENDING_INCOME_DATE, SENDING_EXPENSE_DATE -> {
                return addDate(message, customer);
            }
            case SENDING_INCOME_DESC, SENDING_EXPENSE_DESC -> {
                try {
                    return addDescription(message, customer);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return null;
    }


    private BotApiMethod<?> startMessage(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        OperationType type = OperationType.valueOf(callbackQuery.getData());

        if (operationRepository.existsByCustomerAndInCreationAndType(customerRepository.findById(chatId).orElseThrow(), true, type)) {
            return answerMessageFactory.getEditMessageText(
                    callbackQuery,
                    getTextByTransactionType(
                            "У вас есть %s в процессе создания, для того чтобы создать новое необходимо отменить предыдущее. После отмены повторите попытку.",
                            "EXISTS_IN_CREATION",
                            type
                    ),
                    keyboardFactory.getInlineKeyboard(
                            List.of(
                                getTextByTransactionType(
                                        "Отменить предыдущее %s",
                                        "CANCEL_PREVIOUS",
                                        type
                                ),
                                "Назад"
                            ),
                            List.of(1, 1),
                            List.of(
                                type + "_CANCEL",
                                "START"
                            )
                    )
            );
        }

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                getTextByTransactionType(
                        "Здесь Вы можете сохранять Ваши %s денежных средств. Нажмите продолжить для создания.",
                        "START",
                        type
                ),
                keyboardFactory.getInlineKeyboard(
                        List.of(
                            "Продолжить",
                            "Назад"
                        ),
                        List.of(1, 1),
                        List.of(
                            type + "_SUM",
                            "START"
                        )
                )
        );
    }

    private BotApiMethod<?> askSum(CallbackQuery callbackQuery, String type) {
        Customer customer = customerRepository.findById(callbackQuery.getMessage().getChatId()).orElseThrow();
        customer.setStatus(
                type.equals("INCOME") ?
                CustomerStatus.SENDING_INCOME_SUM :
                CustomerStatus.SENDING_EXPENSE_SUM
        );
        customerRepository.save(customer);

        Operation operation = Operation.builder()
                .customer(customer)
                .type(OperationType.valueOf(type))
                .inCreation(true)
                .build();
        operationRepository.save(operation);

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                getTextByTransactionType(
                        "Введите сумму %s. Если число дробное, разделяйтя целую и дробную часть точкой. Например 243.21",
                        "ASK_SUM",
                        OperationType.valueOf(type)
                ),
                keyboardFactory.getInlineKeyboard(
                        List.of("Отмена"),
                        List.of(1),
                        List.of(type + "_CANCEL")
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

        boolean isIncome = customer.getStatus() == CustomerStatus.SENDING_INCOME_SUM;
        OperationType operationType = isIncome ? OperationType.INCOME : OperationType.EXPENSE;

        if (!isCorrect) {
            return answerMessageFactory.getSendMessage(
                    chatId,
                    getTextByTransactionType(
                            "Некорректная сумма %s. Повторите попытку.",
                            "INCORRECT_SUM",
                            operationType
                    ),
                    keyboardFactory.getInlineKeyboard(
                            List.of("Отмена"),
                            List.of(1),
                            List.of(operationType + "_CANCEL")
                    )
            );
        }

        customer.setStatus(isIncome ? CustomerStatus.SENDING_INCOME_DATE : CustomerStatus.SENDING_EXPENSE_DATE);
        customerRepository.save(customer);
        Operation operation = operationRepository.findByCustomerAndInCreationAndType(customer, true, operationType);
        operation.setSum(sum);
        operationRepository.save(operation);

        return askDate(chatId, operationType);
    }

    private BotApiMethod<?> askDate(Long chatId, OperationType type) {
        return answerMessageFactory.getSendMessage(
                chatId,
                getTextByTransactionType(
                        """
                            Введите дату %s в данном формате: дд.мм.гггг
                        
                            Например 01.05.2000
                        """,
                        "ASK_DATE",
                        type
                ),
                keyboardFactory.getInlineKeyboard(
                        List.of("Отмена"),
                        List.of(1),
                        List.of(type + "_CANCEL")
                )
        );
    }

    private BotApiMethod<?> addDate(Message message, Customer customer) {
        Long chatId = message.getChatId();
        OperationType type = customer.getStatus() == CustomerStatus.SENDING_INCOME_DATE ? OperationType.INCOME : OperationType.EXPENSE;
        LocalDate date;

        try {
            date = LocalDate.parse(message.getText(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }
        catch (DateTimeParseException e) {
            return answerMessageFactory.getSendMessage(
                    chatId,
                    getTextByTransactionType(
                            "Некорректная дата %s. Повторите попытку.",
                            "INCORRECT_DATE",
                            type
                    ),
                    keyboardFactory.getInlineKeyboard(
                            List.of("Отмена"),
                            List.of(1),
                            List.of(type + "_CANCEL")
                    )
            );
        }

        customer.setStatus(CustomerStatus.FREE);
        customerRepository.save(customer);
        Operation operation = operationRepository.findByCustomerAndInCreationAndType(customer, true, type);
        operation.setDate(date);
        operationRepository.save(operation);

        return askCategory(chatId, type);
    }

    private BotApiMethod<?> askCategory(Long chatId, OperationType type) {
        List<String> text;
        List<Integer> configuration;
        List<String> data;

        if (type == OperationType.INCOME) {
            text = IncomeCategory.getCategoryNames();
            configuration = IncomeCategory.getInlineKeyboardConfiguration();
            data = IncomeCategory.getCallbackQueryDataNames();
        }
        else {
            text = ExpenseCategory.getCategoryNames();
            configuration = ExpenseCategory.getInlineKeyboardConfiguration();
            data = ExpenseCategory.getCallbackQueryDataNames();
        }

        text.add("Отмена");
        configuration.add(1);
        data.add(type + "_CANCEL");

        return answerMessageFactory.getSendMessage(
                chatId,
                getTextByTransactionType(
                        "Выберите категорию %s.",
                        "ASK_CATEGORY",
                        type
                ),
                keyboardFactory.getInlineKeyboard(
                        text,
                        configuration,
                        data
                )
        );
    }

    private BotApiMethod<?> addCategory(CallbackQuery callbackQuery, String type, String category) {
        Long chatId = callbackQuery.getMessage().getChatId();
        Customer customer = customerRepository.findById(chatId).orElseThrow();
        boolean isIncome = type.equals("INCOME");
        OperationType operationType = OperationType.valueOf(type);
        Operation operation = operationRepository.findByCustomerAndInCreationAndType(customer, true, operationType);
        operation.setCategory(isIncome ? IncomeCategory.valueOf(category).toString() : ExpenseCategory.valueOf(category).toString());
        operationRepository.save(operation);

        customer.setStatus(isIncome ? CustomerStatus.SENDING_INCOME_DESC : CustomerStatus.SENDING_EXPENSE_DESC);
        customerRepository.save(customer);

        return askDescription(callbackQuery, operationType);
    }

    private BotApiMethod<?> askDescription(CallbackQuery callbackQuery, OperationType operationType) {
        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                getTextByTransactionType(
                        """
                                Укажите описание транзакции.
                                Если в этом нет надобности, можете нажать кнопку Завершить для сохранения %s.
                                """,
                        "ASK_DESCRIPTION",
                        operationType
                ),
                keyboardFactory.getInlineKeyboard(
                        List.of("Завершить",
                                "Отмена"),
                        List.of(1, 1),
                        List.of(operationType + "_FINISH",
                                operationType + "_CANCEL")
                )
        );
    }

    private BotApiMethod<?> addDescription(Message message, Customer customer) throws TelegramApiException {
        Long chatId = message.getChatId();
        OperationType type = customer.getStatus() == CustomerStatus.SENDING_INCOME_DESC ? OperationType.INCOME : OperationType.EXPENSE;
        customer.setStatus(CustomerStatus.FREE);
        customerRepository.save(customer);
        Operation operation = operationRepository.findByCustomerAndInCreationAndType(customer, true, type);
        operation.setDescription(message.getText());
        operation.setInCreation(false);
        operationRepository.save(operation);

        return answerMessageFactory.getSendMessage(
                chatId,
                getTextByTransactionType(
                        "%s успешно сохранено.",
                        "SAVED",
                        type
                ),
                null
        );
    }

    private BotApiMethod<?> finish(CallbackQuery callbackQuery, String type) throws TelegramApiException {
        Customer customer = customerRepository.findById(callbackQuery.getMessage().getChatId()).orElseThrow();
        customer.setStatus(CustomerStatus.FREE);
        customerRepository.save(customer);
        OperationType operationType = OperationType.valueOf(type);
        Operation operation = operationRepository.findByCustomerAndInCreationAndType(customer, true, operationType);

        if (operation == null) {
            return answerMessageFactory.getAnswerCallbackQuery(
                    callbackQuery,
                    getTextByTransactionType(
                            "Данное %s уже сохранено",
                            "ALREADY_SAVED",
                            operationType
                    )
            );
        }

        operation.setInCreation(false);
        operationRepository.save(operation);

        bot.execute(
                answerMessageFactory.getAnswerCallbackQuery(
                        callbackQuery,
                        getTextByTransactionType(
                                "%s успешно сохранено.",
                                "SAVED",
                                operationType
                        )
                )
        );

        return answerMessageFactory.getDeleteMessage(
                callbackQuery.getMessage().getMessageId(),
                callbackQuery.getMessage().getChatId()
        );
    }

    private BotApiMethod<?> cancel(CallbackQuery callbackQuery, String type) throws TelegramApiException {
        Long chatId = callbackQuery.getMessage().getChatId();
        OperationType operationType = OperationType.valueOf(type);
        Customer customer = customerRepository.findById(chatId).orElseThrow();

        if (operationRepository.existsByCustomerAndInCreationAndType(customer, true, operationType)) {
            operationRepository.deleteByCustomerAndInCreationAndType(
                    customer,
                    true,
                    operationType
            );
            customer.setStatus(CustomerStatus.FREE);
            customerRepository.save(customer);

            bot.execute(
                    answerMessageFactory.getAnswerCallbackQuery(
                            callbackQuery,
                            getTextByTransactionType(
                                    "%s успешно отменено",
                                    "CANCELED",
                                    operationType
                            )
                    )
            );
        }

        return answerMessageFactory.getDeleteMessage(
                callbackQuery.getMessage().getMessageId(),
                chatId
        );
    }

    private String getTextByTransactionType(String text, String textType, OperationType operationType) {
        boolean isIncome = operationType == OperationType.INCOME;

        switch (textType) {
            case "EXISTS_IN_CREATION",
                 "CANCEL_PREVIOUS",
                 "ALREADY_SAVED" -> {
                return String.format(
                        text,
                        isIncome ? "пополнение" : "списание"
                );
            }
            case "SAVED",
                 "CANCELED" -> {
                return String.format(
                        text,
                        isIncome ? "Пополнение" : "Списание"
                );
            }
            default -> {
                return String.format(
                        text,
                        isIncome ? "пополнения" : "списания"
                );
            }
        }
    }
}
