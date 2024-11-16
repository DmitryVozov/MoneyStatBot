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
import ru.vozov.moneystatbot.model.refill.Refill;
import ru.vozov.moneystatbot.model.refill.RefillType;
import ru.vozov.moneystatbot.repository.CustomerRepository;
import ru.vozov.moneystatbot.repository.RefillRepository;
import ru.vozov.moneystatbot.service.factory.AnswerMessageFactory;
import ru.vozov.moneystatbot.service.factory.KeyboardFactory;
import ru.vozov.moneystatbot.telegram.MoneyStatBot;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefillManager {
    final AnswerMessageFactory answerMessageFactory;
    final KeyboardFactory keyboardFactory;
    final CustomerRepository customerRepository;
    final RefillRepository refillRepository;
    final MoneyStatBot bot;

    @Autowired
    public RefillManager(AnswerMessageFactory answerMessageFactory,
                         KeyboardFactory keyboardFactory,
                         CustomerRepository customerRepository,
                         RefillRepository refillRepository,
                         @Lazy MoneyStatBot bot) {
        this.answerMessageFactory = answerMessageFactory;
        this.keyboardFactory = keyboardFactory;
        this.customerRepository = customerRepository;
        this.refillRepository = refillRepository;
        this.bot = bot;
    }

    public BotApiMethod<?> answerCommand(Message message) {
        Long chatId = message.getChatId();

        if (refillRepository.existsByCustomerAndIsCreate(customerRepository.findById(chatId).orElseThrow(),false)) {
            return answerMessageFactory.getSendMessage(
                    chatId,
                    "У вас есть пополнение в процессе создания, для того чтобы создать новое необходимо отменить предыдущее. После отмены повторите попытку.",
                    keyboardFactory.getInlineKeyboard(
                            List.of("Отменить предыдущее пополнение"),
                            List.of(1),
                            List.of("REFILL_CANCEL")
                    )
            );
        }
        return answerMessageFactory.getSendMessage(
                chatId,
                "Здесь вы можете сохранять ваши Пополнения. Нажмите продолжить для создания.",
                keyboardFactory.getInlineKeyboard(
                        List.of("Продолжить",
                                "Отмена"),
                        List.of(1, 1),
                        List.of("REFILL_SUM","REFILL_CANCEL")
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
            case SENDING_REFILL_SUM -> {
                return addSum(message, customer);
            }
            case SENDING_REFILL_DATE -> {
                return addDate(message, customer);
            }
            case SENDING_REFILL_DESC -> {
                return addDescription(message, customer);
            }
        }

        return null;
    }


    private BotApiMethod<?> startMessage(CallbackQuery callbackQuery) {
        if (refillRepository.existsByCustomerAndIsCreate(customerRepository.findById(callbackQuery.getMessage().getChatId()).orElseThrow(), false)) {
            return answerMessageFactory.getEditMessageText(
                    callbackQuery,
                    "У вас есть пополнение в процессе создания, для того чтобы создать новое необходимо отменить предыдущее. После отмены повторите попытку.",
                    keyboardFactory.getInlineKeyboard(
                            List.of("Отменить предыдущее пополнение",
                                    "Назад"),
                            List.of(1, 1),
                            List.of("REFILL_CANCEL","START")
                    )
            );
        }

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                "Здесь вы можете сохранять ваши Пополнения. Нажмите продолжить для создания.",
                keyboardFactory.getInlineKeyboard(
                        List.of("Продолжить",
                                "Назад"),
                        List.of(1, 1),
                        List.of("REFILL_SUM","START")
                )
        );
    }

    private BotApiMethod<?> askSum(CallbackQuery callbackQuery) {
        Customer customer = customerRepository.findById(callbackQuery.getMessage().getChatId()).orElseThrow();
        customer.setStatus(CustomerStatus.SENDING_REFILL_SUM);

        Refill refill = Refill.builder()
                .customer(customer)
                .isCreate(false)
                .build();
        refillRepository.save(refill);

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                "Введите сумму пополнения. Если число дробное, разделяйтя целую и дробную часть точкой. Например 243.21",
                keyboardFactory.getInlineKeyboard(
                        List.of("Отмена"),
                        List.of(1),
                        List.of("REFILL_CANCEL")
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
                    "Некорректная сумма пополнения.Повторите попытку.",
                    keyboardFactory.getInlineKeyboard(
                            List.of("Отмена"),
                            List.of(1),
                            List.of("REFILL_CANCEL")
                    )
            );
        }

        customer.setStatus(CustomerStatus.SENDING_REFILL_DATE);
        customerRepository.save(customer);
        Refill refill = refillRepository.findByCustomerAndIsCreate(customer, false);
        refill.setSum(sum);
        refillRepository.save(refill);

        return answerMessageFactory.getSendMessage(
                chatId,
                """
                        Введите дату пополнения в данном формате: дд.мм.гггг
                        
                        Например 01.05.2000
                        """,
                keyboardFactory.getInlineKeyboard(
                        List.of("Отмена"),
                        List.of(1),
                        List.of("REFILL_CANCEL")
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
                    "Некорректная дата пополнения.Повторите попытку.",
                    keyboardFactory.getInlineKeyboard(
                            List.of("Отмена"),
                            List.of(1),
                            List.of("REFILL_CANCEL")
                    )
            );
        }

        customer.setStatus(CustomerStatus.FREE);
        customerRepository.save(customer);
        Refill refill = refillRepository.findByCustomerAndIsCreate(customer,false);
        refill.setDate(date);
        refillRepository.save(refill);

        return answerMessageFactory.getSendMessage(
                chatId,
                "Выберете тип пополнения.",
                keyboardFactory.getInlineKeyboard(
                        List.of("Зарплата", "Проценты",
                                "Кешбек","Подарок",
                                "Отмена"),
                        List.of(2, 2, 1),
                        List.of("REFILL_TYPE_" + RefillType.PAYCHECK, "REFILL_TYPE_" + RefillType.INTEREST,
                                "REFILL_TYPE_" + RefillType.CASHBACK, "REFILL_TYPE_" + RefillType.GIFT,
                                "REFILL_CANCEL")
                )
        );
    }

    private BotApiMethod<?> addType(CallbackQuery callbackQuery, String type) {
        Long chatId = callbackQuery.getMessage().getChatId();
        Customer customer = customerRepository.findById(chatId).orElseThrow();
        Refill refill = refillRepository.findByCustomerAndIsCreate(customer, false);
        refill.setType(RefillType.valueOf(type));
        refillRepository.save(refill);

        customer.setStatus(CustomerStatus.SENDING_REFILL_DESC);
        customerRepository.save(customer);

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                """
                        Укажите описание транзакции.
                        Если в этом нет надобности, можете нажать кнопку Завершить для сохранения пополнения.
                        """,
                keyboardFactory.getInlineKeyboard(
                        List.of("Завершить",
                                "Отмена"),
                        List.of(1, 1),
                        List.of("REFILL_FINISH",
                                "REFILL_CANCEL")
                )
        );
    }

    private BotApiMethod<?> addDescription(Message message, Customer customer) {
        Long chatId = message.getChatId();
        customer.setStatus(CustomerStatus.FREE);
        customerRepository.save(customer);
        Refill refill = refillRepository.findByCustomerAndIsCreate(customer, false);
        refill.setDescription(message.getText());
        refill.setIsCreate(true);
        refillRepository.save(refill);

        return answerMessageFactory.getSendMessage(
                chatId,
                "Пополнение успешно сохранено.",
                null
        );
    }

    private BotApiMethod<?> finish(CallbackQuery callbackQuery) throws TelegramApiException {
        Customer customer = customerRepository.findById(callbackQuery.getMessage().getChatId()).orElseThrow();
        customer.setStatus(CustomerStatus.FREE);
        customerRepository.save(customer);
        Refill refill = refillRepository.findByCustomerAndIsCreate(customer, false);
        refill.setIsCreate(true);
        refillRepository.save(refill);

        bot.execute(
                answerMessageFactory.getAnswerCallbackQuery(
                        callbackQuery,
                        "Пополнение успешно сохранено"
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

        if (refillRepository.existsByCustomerAndIsCreate(customer, false)) {
            refillRepository.deleteByCustomerAndIsCreate(
                    customer,
                    false
            );
            customer.setStatus(CustomerStatus.FREE);
            customerRepository.save(customer);

            bot.execute(
                    answerMessageFactory.getAnswerCallbackQuery(
                            callbackQuery,
                            "Пополнение успешно отменено"
                    )
            );
        }
        else {
            bot.execute(
                    answerMessageFactory.getAnswerCallbackQuery(
                            callbackQuery,
                            "Невозможно отменить пополнение, так как оно уже создано"
                    )
            );
        }

        return answerMessageFactory.getDeleteMessage(
                callbackQuery.getMessage().getMessageId(),
                chatId
        );
    }
}
