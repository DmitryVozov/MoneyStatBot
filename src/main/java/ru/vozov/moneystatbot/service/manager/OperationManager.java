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

import static ru.vozov.moneystatbot.service.data.CallbackQueryData.*;
import static ru.vozov.moneystatbot.service.data.CommandData.INCOME_COMMAND;
import static ru.vozov.moneystatbot.service.data.MessageData.*;

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
        OperationType type = message.getText().equals(INCOME_COMMAND) ? OperationType.INCOME : OperationType.EXPENSE;

        if (operationRepository.existsByCustomerAndInCreationAndType(customerRepository.findById(chatId).orElseThrow(),true, type)) {
            return answerMessageFactory.getSendMessage(
                    chatId,
                    OPERATION_EXISTS_IN_CREATION_MESSAGE,
                    keyboardFactory.getInlineKeyboard(
                            List.of("Отменить операцию"),
                            List.of(1),
                            List.of(type + CANCEL)
                    )
            );
    }

        return answerMessageFactory.getSendMessage(
                chatId,
                getStartTextByOperationType(type),
                keyboardFactory.getInlineKeyboard(
                        List.of("Продолжить",
                                "Отмена"),
                        List.of(1, 1),
                        List.of(type + SUM, type + CANCEL)
                )
        );
    }

    @Transactional
    public BotApiMethod<?> answerCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        String[] splitCallbackQuery = data.split("_");

        try {
            switch (data) {
                case INCOME, EXPENSE -> {
                    return startMessage(callbackQuery);
                }
                case INCOME_SUM, EXPENSE_SUM -> {
                    return askSum(callbackQuery, splitCallbackQuery[0]);
                }
                case INCOME_FINISH, EXPENSE_FINISH -> {
                    return finish(callbackQuery, splitCallbackQuery[0]);
                }
                case INCOME_CANCEL, EXPENSE_CANCEL -> {
                    return cancel(callbackQuery, splitCallbackQuery[0]);
                }
            }
        }
        catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

        if (data.contains(INCOME_CATEGORY) || data.contains(EXPENSE_CATEGORY)) {
            return addCategory(callbackQuery, splitCallbackQuery[0],  splitCallbackQuery[2]);
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
                    OPERATION_EXISTS_IN_CREATION_MESSAGE,
                    keyboardFactory.getInlineKeyboard(
                            List.of(
                                "❌Отменить операцию",
                                "\uD83D\uDD19Назад"
                            ),
                            List.of(1, 1),
                            List.of(
                                type + CANCEL,
                                START
                            )
                    )
            );
        }

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                getStartTextByOperationType(type),
                keyboardFactory.getInlineKeyboard(
                        List.of(
                            "Продолжить",
                            "\uD83D\uDD19Назад"
                        ),
                        List.of(1, 1),
                        List.of(
                            type + SUM,
                            START
                        )
                )
        );
    }

    private BotApiMethod<?> askSum(CallbackQuery callbackQuery, String type) {
        Customer customer = customerRepository.findById(callbackQuery.getMessage().getChatId()).orElseThrow();
        customer.setStatus(
                type.equals(INCOME) ?
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
                ASK_OPERATION_SUM_MESSAGE,
                keyboardFactory.getInlineKeyboard(
                        List.of("❌Отмена"),
                        List.of(1),
                        List.of(type + CANCEL)
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
                    INCORRECT_OPERATION_SUM_MESSAGE,
                    keyboardFactory.getInlineKeyboard(
                            List.of("❌Отмена"),
                            List.of(1),
                            List.of(operationType + CANCEL)
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
                ASK_OPERATION_DATE_MESSAGE,
                keyboardFactory.getInlineKeyboard(
                        List.of("❌Отмена"),
                        List.of(1),
                        List.of(type + CANCEL)
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
                    INCORRECT_OPERATION_DATE_MESSAGE,
                    keyboardFactory.getInlineKeyboard(
                            List.of("❌Отмена"),
                            List.of(1),
                            List.of(type + CANCEL)
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
        data.add(type + CANCEL);

        return answerMessageFactory.getSendMessage(
                chatId,
                ASK_OPERATION_CATEGORY_MESSAGE,
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
        boolean isIncome = type.equals(INCOME);
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
                ASK_OPERATION_DESCRIPTION_MESSAGE,
                keyboardFactory.getInlineKeyboard(
                        List.of("✅Завершить",
                                "❌Отмена"),
                        List.of(1, 1),
                        List.of(operationType + FINISH,
                                operationType + CANCEL)
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
                OPERATION_SUCCESS_MESSAGE,
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
                    OPERATION_ALREADY_SAVED_MESSAGE
            );
        }

        operation.setInCreation(false);
        operationRepository.save(operation);

        bot.execute(
                answerMessageFactory.getAnswerCallbackQuery(
                        callbackQuery,
                        OPERATION_SUCCESS_MESSAGE
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
                            OPERATION_CANCEL_MESSAGE
                    )
            );
        }

        return answerMessageFactory.getDeleteMessage(
                callbackQuery.getMessage().getMessageId(),
                chatId
        );
    }

    private String getStartTextByOperationType(OperationType operationType) {
        return String.format(
                OPERATION_START_MESSAGE,
                operationType == OperationType.INCOME ? "доходы\uD83D\uDCC8" : "расходы\uD83D\uDCC9"
        );
    }
}
