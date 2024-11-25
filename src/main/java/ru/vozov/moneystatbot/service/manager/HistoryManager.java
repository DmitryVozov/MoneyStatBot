package ru.vozov.moneystatbot.service.manager;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.vozov.moneystatbot.model.operation.ExpenseCategory;
import ru.vozov.moneystatbot.model.operation.IncomeCategory;
import ru.vozov.moneystatbot.model.operation.Operation;
import ru.vozov.moneystatbot.model.operation.OperationType;
import ru.vozov.moneystatbot.repository.OperationRepository;
import ru.vozov.moneystatbot.service.factory.AnswerMessageFactory;
import ru.vozov.moneystatbot.service.factory.KeyboardFactory;
import ru.vozov.moneystatbot.service.util.DateHelper;
import ru.vozov.moneystatbot.telegram.MoneyStatBot;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static ru.vozov.moneystatbot.service.data.CallbackQueryData.*;
import static ru.vozov.moneystatbot.service.data.MessageData.*;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class HistoryManager {
    final AnswerMessageFactory answerMessageFactory;
    final KeyboardFactory keyboardFactory;
    final OperationRepository operationRepository;
    final MoneyStatBot bot;
    final DateHelper dateHelper;

    final static int MESSAGE_MAX_LENGTH = 4096;

    @Autowired
    public HistoryManager(AnswerMessageFactory answerMessageFactory,
                          KeyboardFactory keyboardFactory,
                          OperationRepository operationRepository,
                          @Lazy MoneyStatBot bot,
                          DateHelper dateHelper) {
        this.answerMessageFactory = answerMessageFactory;
        this.keyboardFactory = keyboardFactory;
        this.operationRepository = operationRepository;
        this.bot = bot;
        this.dateHelper = dateHelper;
    }

    public BotApiMethod<?> answerCommand(Message message) {
        return answerMessageFactory.getSendMessage(
                message.getChatId(),
                ASK_HISTORY_TYPE_MESSAGE,
                keyboardFactory.getInlineKeyboard(
                        List.of("\uD83D\uDCC8Доходы", "\uD83D\uDCC9Расходы",
                                "❌Отмена"),
                        List.of(2, 1),
                        List.of(HISTORY_INCOME, HISTORY_EXPENSE,
                                HISTORY_CANCEL)
                )
        );
    }

    public BotApiMethod<?> answerCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();

        if (data.equals(HISTORY_CANCEL)) {
            return cancel(callbackQuery);
        }

        String[] splitCallbackQuery = data.split("_");

        int length = splitCallbackQuery.length;

        try {
            switch (length) {
                case 2 -> {
                    return askPeriod(callbackQuery);
                }
                case 3 -> {
                    if (data.contains(ALL_TIME)) {
                        return getHistoryForAllTimeByType(callbackQuery, splitCallbackQuery);
                    }

                    return askYear(callbackQuery, splitCallbackQuery);
                }
                case 4 -> {
                    if (data.contains(YEAR)) {
                        return getHistoryForYearByType(callbackQuery, splitCallbackQuery);
                    }

                    return askMonth(callbackQuery);
                }
                case 5 -> {
                    if (data.contains(MONTH)) {
                        return getHistoryForMonthByType(callbackQuery, splitCallbackQuery);
                    }

                    return askDay(callbackQuery, splitCallbackQuery);
                }
                case 6 -> {
                    return getHistoryForDayByType(callbackQuery, splitCallbackQuery);
                }
            }
        }
        catch (TelegramApiException e) {
            log.error(e.getMessage());
        }

        return null;
    }

    private BotApiMethod<?> askPeriod(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                ASK_HISTORY_PERIOD_MESSAGE,
                keyboardFactory.getInlineKeyboard(
                        List.of("День","Месяц","Год",
                                "Все время",
                                "❌Отмена"),
                        List.of(3, 1, 1),
                        List.of(data + DAY, data + MONTH, data + YEAR,
                                data + ALL_TIME,
                                HISTORY_CANCEL)
                )
        );
    }

    private BotApiMethod<?> askYear(CallbackQuery callbackQuery, String[] splitCallbackQuery) {
        String type = splitCallbackQuery[1];

        List<Object[]> years = operationRepository.getDistinctYearsByCustomerChatIdAndType(
                callbackQuery.getMessage().getChatId(),
                type
        );

        if (years.isEmpty()) {
            return answerMessageFactory.getEditMessageText(
                    callbackQuery,
                    NO_HISTORY_DATA_MESSAGE,
                    null
            );
        }

        List<String> text = new ArrayList<>();
        List<Integer> configuration = new ArrayList<>();
        List<String> data = new ArrayList<>();
        String callbackQueryData = callbackQuery.getData();

        for (Object[] objects : years) {
            Double d = (Double) objects[0];
            int year = d.intValue();
            text.add(String.valueOf(year));
            data.add(callbackQueryData + "_" + year);
            configuration.add(1);
        }

        text.add("❌Отмена");
        data.add(HISTORY_CANCEL);
        configuration.add(1);

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                ASK_HISTORY_YEAR_MESSAGE,
                keyboardFactory.getInlineKeyboard(
                        text,
                        configuration,
                        data
                )
        );
    }

    private BotApiMethod<?> askMonth(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                ASK_HISTORY_MONTH_MESSAGE,
                keyboardFactory.getInlineKeyboard(
                        List.of("Январь","Февраль","Март","Апрель",
                                "Май","Июнь","Июль","Август",
                                "Сентябрь","Октябрь","Ноябрь","Декабрь",
                                "❌Отмена"),
                        List.of(4, 4, 4, 1),
                        List.of(data + JANUARY_NUMBER, data + FEBRUARY_NUMBER, data + MARCH_NUMBER, data + APRIL_NUMBER,
                                data + MAY_NUMBER, data + JUNE_NUMBER, data + JULY_NUMBER, data + AUGUST_NUMBER,
                                data + SEPTEMBER_NUMBER, data + OCTOBER_NUMBER, data + NOVEMBER_NUMBER, data + DECEMBER_NUMBER,
                                HISTORY_CANCEL)
                )
        );
    }

    private BotApiMethod<?> askDay(CallbackQuery callbackQuery, String[] splitCallbackQuery) {
        int monthNumber = Integer.parseInt(splitCallbackQuery[4]);
        int year = Integer.parseInt(splitCallbackQuery[3]);

        List<Integer> configuration = dateHelper.getMonthDaysKeyboardConfiguration(monthNumber, year);
        List<String> text = new ArrayList<>();
        List<String> data = new ArrayList<>();

        int dayNumber = 1;
        String callbackQueryData = callbackQuery.getData();

        for (Integer size : configuration) {
            for (int i = 0; i < size; i++) {
                text.add(String.valueOf(dayNumber));
                data.add(callbackQueryData + "_" + dayNumber);
                dayNumber++;
            }
        }

        configuration.add(1);
        text.add("❌Отмена");
        data.add(HISTORY_CANCEL);

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                ASK_HISTORY_DAY_MESSAGE,
                keyboardFactory.getInlineKeyboard(
                        text,
                        configuration,
                        data
                )
        );
    }

    private BotApiMethod<?> getHistoryForAllTimeByType(CallbackQuery callbackQuery, String[] splitCallbackQuery) throws TelegramApiException {
        Long chatId = callbackQuery.getMessage().getChatId();
        String type = splitCallbackQuery[1];

        List<Operation> operations = operationRepository.findByCustomerChatIdAndType(
                chatId,
                type
        );

        if (operations.isEmpty()) {
            return answerMessageFactory.getEditMessageText(
                    callbackQuery,
                    NO_HISTORY_DATA_MESSAGE,
                    null
            );
        }

        StringBuilder history = new StringBuilder();

        history.append(
                String.format(
                        HISTORY_FOR_ALL_TIME_MESSAGE,
                        type.equals(OperationType.INCOME.toString()) ? "доходов" : "расходов"
                )
        );

        return sendHistory(operations, type, history, callbackQuery, false);
    }

    private BotApiMethod<?> getHistoryForYearByType(CallbackQuery callbackQuery, String[] splitCallbackQuery) throws TelegramApiException {
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer year = Integer.parseInt(splitCallbackQuery[3]);
        String type = splitCallbackQuery[1];
        List<Operation> operations = operationRepository.findByCustomerChatIdAndTypeAndYear(chatId, type, year);

        StringBuilder history = new StringBuilder();
        history.append(
                String.format(
                        HISTORY_FOR_YEAR_MESSAGE,
                        type.equals(OperationType.INCOME.toString()) ? "доходов" : "расходов",
                        year
                )
        );

        return sendHistory(operations, type, history, callbackQuery, false);
    }

    private BotApiMethod<?> getHistoryForMonthByType(CallbackQuery callbackQuery, String[] splitCallbackQuery) throws TelegramApiException {
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer year = Integer.parseInt(splitCallbackQuery[3]);
        Integer month = Integer.parseInt(splitCallbackQuery[4]);
        String type = splitCallbackQuery[1];
        List<Operation> operations = operationRepository.findByCustomerChatIdAndTypeAndMonth(chatId, type, year, month);

        if (operations.isEmpty()) {
            return answerMessageFactory.getEditMessageText(
                    callbackQuery,
                    String.format(
                            NO_HISTORY_DATA_FOR_MONTH_MESSAGE,
                            type.equals(OperationType.INCOME.toString()) ? "доходов" : "расходов"
                    ),
                    null
            );
        }

        StringBuilder history = new StringBuilder();
        history.append(
                String.format(
                        HISTORY_FOR_MONTH_MESSAGE,
                        type.equals(OperationType.INCOME.toString()) ? "доходов" : "расходов",
                        dateHelper.getMonthName(month, false),
                        year
                )
        );

        return sendHistory(operations, type, history, callbackQuery, false);
    }

    private BotApiMethod<?> getHistoryForDayByType(CallbackQuery callbackQuery, String[] splitCallbackQuery) throws TelegramApiException {
        Long chatId = callbackQuery.getMessage().getChatId();
        LocalDate date = LocalDate.of(
                Integer.parseInt(splitCallbackQuery[3]),
                Integer.parseInt(splitCallbackQuery[4]),
                Integer.parseInt(splitCallbackQuery[5])
        );
        String type = splitCallbackQuery[1];
        List<Operation> operations = operationRepository.findByCustomerChatIdAndTypeAndDate(chatId, type, date);

        if (operations.isEmpty()) {
            return answerMessageFactory.getEditMessageText(
                    callbackQuery,
                    String.format(
                            NO_HISTORY_DATA_FOR_DAY_MESSAGE,
                            type.equals(OperationType.INCOME.toString()) ? "доходов" : "расходов"
                    ),
                    null
            );
        }

        StringBuilder history = new StringBuilder();
        history.append(
                String.format(
                        HISTORY_FOR_DAY_MESSAGE,
                        type.equals(OperationType.INCOME.toString()) ? "доходов" : "расходов",
                        date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                )
        );

        return sendHistory(operations, type, history, callbackQuery, true);
    }

    private BotApiMethod<?> sendHistory(List<Operation> operations,
                                        String type,
                                        StringBuilder history,
                                        CallbackQuery callbackQuery,
                                        boolean isHistoryForDay) throws TelegramApiException {
        LocalDate date = null;

        for (Operation operation : operations) {
            StringBuilder text = new StringBuilder();

            if (!isHistoryForDay && (date == null || !date.equals(operation.getDate()))) {
                date = operation.getDate();
                text.append(
                        String.format(
                                HISTORY_DAY_MESSAGE,
                                date.getDayOfMonth(),
                                dateHelper.getMonthName(date.getMonthValue(), true),
                                date.getYear()
                        )
                );
            }

            String description = operation.getDescription();

            text.append(
                    String.format(
                            HISTORY_OPERATION_MESSAGE,
                            type.equals(OperationType.INCOME.toString()) ?
                                    IncomeCategory.valueOf(operation.getCategory()).getName() :
                                    ExpenseCategory.valueOf(operation.getCategory()).getName(),
                            operation.getSum(),
                            description == null ? "" : description
                    )
            );

            if (history.length() + text.length() > MESSAGE_MAX_LENGTH) {
                bot.execute(
                        answerMessageFactory.getSendMessage(
                                callbackQuery.getMessage().getChatId(),
                                history.toString(),
                                null
                        )
                );

                history = new StringBuilder();
            }

            history.append(text);
        }

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                history.toString(),
                null
        );
    }

    private BotApiMethod<?> cancel(CallbackQuery callbackQuery) {
        return answerMessageFactory.getDeleteMessage(
                callbackQuery.getMessage().getMessageId(),
                callbackQuery.getMessage().getChatId()
        );
    }
}