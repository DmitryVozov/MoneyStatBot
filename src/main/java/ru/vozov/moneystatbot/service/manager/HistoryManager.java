package ru.vozov.moneystatbot.service.manager;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
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
import ru.vozov.moneystatbot.repository.OperationRepository;
import ru.vozov.moneystatbot.service.factory.AnswerMessageFactory;
import ru.vozov.moneystatbot.service.factory.KeyboardFactory;
import ru.vozov.moneystatbot.telegram.MoneyStatBot;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HistoryManager {
    final AnswerMessageFactory answerMessageFactory;
    final KeyboardFactory keyboardFactory;
    final OperationRepository operationRepository;
    final MoneyStatBot bot;
    final static int MESSAGE_MAX_LENGTH = 4096;

    @Autowired
    public HistoryManager(AnswerMessageFactory answerMessageFactory,
                          KeyboardFactory keyboardFactory,
                          OperationRepository operationRepository,
                          @Lazy MoneyStatBot bot) {
        this.answerMessageFactory = answerMessageFactory;
        this.keyboardFactory = keyboardFactory;
        this.operationRepository = operationRepository;
        this.bot = bot;
    }

    public BotApiMethod<?> answerCommand(Message message) {
        return answerMessageFactory.getSendMessage(
                message.getChatId(),
                """
                        Здесь вы можете получить историю по Вашим тратам и пополнениям.
                        Выберите по какому типу Вы хотите получить историю.
                        """,
                keyboardFactory.getInlineKeyboard(
                        List.of("Пополнения", "Списания",
                                "Отмена"),
                        List.of(2, 1),
                        List.of("HISTORY_INCOME", "HISTORY_EXPENSE",
                                "HISTORY_CANCEL")
                )
        );
    }

    public BotApiMethod<?> answerCallbackQuery(CallbackQuery callbackQuery) {
        String[] splitCallbackQuery = callbackQuery.getData().split("_");

        if (splitCallbackQuery[1].equals("CANCEL")) {
            return cancel(callbackQuery);
        }

        int length = splitCallbackQuery.length;

        try {
            switch (length) {
                case 2 -> {
                    return askPeriod(callbackQuery);
                }
                case 3 -> {
                    if (splitCallbackQuery[2].equals("ALLTIME")) {
                        return getHistoryForAllTimeByType(callbackQuery, splitCallbackQuery);
                    }

                    return askYear(callbackQuery, splitCallbackQuery);
                }
                case 4 -> {
                    if (splitCallbackQuery[2].equals("YEAR")) {
                        return getHistoryForYearByType(callbackQuery, splitCallbackQuery);
                    }

                    return askMonth(callbackQuery);
                }
                case 5 -> {
                    if (splitCallbackQuery[2].equals("MONTH")) {
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
            throw new RuntimeException(e);
        }

        return null;
    }

    private BotApiMethod<?> askPeriod(CallbackQuery callbackQuery) {
        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                """
                        Выберите за какой промежуток времени Вы хотите получить историю
                        """,
                keyboardFactory.getInlineKeyboard(
                        List.of("День","Месяц","Год",
                                "Все время",
                                "Отмена"),
                        List.of(3,1),
                        List.of(callbackQuery.getData()+"_DAY",callbackQuery.getData()+"_MONTH", callbackQuery.getData()+"_YEAR",
                                callbackQuery.getData()+"_ALLTIME",
                                "HISTORY_CANCEL")
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
                    """
                            У Вас еще нет данных для получения истории.
                            Для того чтобы добавить данные вызовите команду /income или /expense
                            """,
                    null
            );
        }

        List<String> text = new ArrayList<>();
        List<Integer> configuration = new ArrayList<>();
        List<String> data = new ArrayList<>();

        for (Object[] objects : years) {
            Double d = (Double) objects[0];
            int year = d.intValue();
            text.add(String.valueOf(year));
            data.add(callbackQuery.getData() + "_" + year);

            configuration.add(1);
        }

        text.add("Отмена");
        data.add("HISTORY_CANCEL");
        configuration.add(1);

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                """
                        Выберите за какой год Вы хотите получить историю
                        """,
                keyboardFactory.getInlineKeyboard(
                        text,
                        configuration,
                        data
                )
        );
    }

    private BotApiMethod<?> askMonth(CallbackQuery callbackQuery) {
        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                """
                        Выберите за какой месяц Вы хотите получить историю
                        """,
                keyboardFactory.getInlineKeyboard(
                        List.of("Январь","Февраль","Март","Апрель",
                                "Май","Июнь","Июль","Август",
                                "Сентябрь","Октябрь","Ноябрь","Декабрь",
                                "Отмена"),
                        List.of(4, 4, 4, 1),
                        List.of(callbackQuery.getData() + "_1", callbackQuery.getData() + "_2", callbackQuery.getData() + "_3", callbackQuery.getData() + "_4",
                                callbackQuery.getData() + "_5", callbackQuery.getData() + "_6", callbackQuery.getData() + "_7", callbackQuery.getData() + "_8",
                                callbackQuery.getData() + "_9", callbackQuery.getData() + "_10", callbackQuery.getData() + "_11", callbackQuery.getData() + "_12",
                                "HISTORY_CANCEL")
                )
        );
    }

    private BotApiMethod<?> askDay(CallbackQuery callbackQuery, String[] splitCallbackQuery) {
        Month month = Month.of(Integer.parseInt(splitCallbackQuery[4]));
        int year = Integer.parseInt(splitCallbackQuery[3]);
        int monthLength = month.length(year % 4 == 0);

        List<Integer> configuration;

        if (monthLength == 31) {
            configuration = List.of(5, 5, 5, 5, 5, 6, 1);
        }
        else if (monthLength == 30) {
            configuration = List.of(5, 5, 5, 5, 5, 5, 1);
        }
        else if (monthLength == 29) {
            configuration = List.of(5, 5, 5, 5, 5, 4, 1);
        }
        else {
            configuration = List.of(5, 5, 5, 5, 4, 4, 1);
        }

        List<String> text = new ArrayList<>();
        List<String> data = new ArrayList<>();

        for (int i = 1; i <= monthLength; i++) {
            text.add(String.valueOf(i));
            data.add(callbackQuery.getData() + "_" + i);
        }

        text.add("Отмена");
        data.add("HISTORY_CANCEL");

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                """
                        Выберите за какой день Вы хотите получить историю
                        """,
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

        List<Operation> historyData = operationRepository.findByCustomerChatIdAndType(
                chatId,
                type
        );

        if (historyData.isEmpty()) {
            return answerMessageFactory.getEditMessageText(
                    callbackQuery,
                    """
                            У Вас еще нет данных для получения истории.
                            Для того чтобы добавить данные вызовите команду /income или /expense
                            """,
                    null
            );
        }

        StringBuilder history = new StringBuilder();

        history.append(
                String.format(
                        "Ваша история по %s за все время\n\n",
                        type.equals("INCOME") ? "пополнениям" : "списаниям"
                )
        );

        for (Operation operation : historyData) {
            String text = String.format(
                    "%s %s %.2f\n",
                    operation.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    type.equals("INCOME") ?
                            IncomeCategory.valueOf(operation.getCategory()).getName() :
                            ExpenseCategory.valueOf(operation.getCategory()).getName(),
                    operation.getSum()
            );

            if (history.length() + text.length() > MESSAGE_MAX_LENGTH) {
                bot.execute(
                        answerMessageFactory.getSendMessage(
                                chatId,
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

    private BotApiMethod<?> getHistoryForYearByType(CallbackQuery callbackQuery, String[] splitCallbackQuery) throws TelegramApiException {
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer year = Integer.parseInt(splitCallbackQuery[3]);
        String type = splitCallbackQuery[1];
        List<Operation> historyData = operationRepository.findByCustomerChatIdAndTypeAndYear(chatId, type, year);

        StringBuilder history = new StringBuilder();
        history.append(
                String.format(
                        "Ваша история по %s за %d год\n\n",
                        type.equals("INCOME") ? "пополнениям" : "списаниям",
                        year
                )
        );

        for (Operation operation : historyData) {
            String text = String.format(
                    "%s %s %.2f\n",
                    operation.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    type.equals("INCOME") ?
                            IncomeCategory.valueOf(operation.getCategory()).getName() :
                            ExpenseCategory.valueOf(operation.getCategory()).getName(),
                    operation.getSum()
            );

            if (history.length() + text.length() > MESSAGE_MAX_LENGTH) {
                bot.execute(
                        answerMessageFactory.getSendMessage(
                                chatId,
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

    private BotApiMethod<?> getHistoryForMonthByType(CallbackQuery callbackQuery, String[] splitCallbackQuery) throws TelegramApiException {
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer year = Integer.parseInt(splitCallbackQuery[3]);
        Integer month = Integer.parseInt(splitCallbackQuery[4]);
        String type = splitCallbackQuery[1];
        List<Operation> historyData = operationRepository.findByCustomerChatIdAndTypeAndMonth(chatId, type, year, month);

        if (historyData.isEmpty()) {
            return answerMessageFactory.getEditMessageText(
                    callbackQuery,
                    String.format(
                            "За данный месяц у Вас не было %s.",
                            type.equals("INCOME") ? "пополнений" : "списаний"
                    ),
                    null
            );
        }

        StringBuilder history = new StringBuilder();
        history.append(
                String.format(
                        "Ваша история по %s за %s %d года\n\n",
                        type.equals("INCOME") ? "пополнениям" : "списаниям",
                        getMonthName(month),
                        year
                )
        );

        for (Operation operation : historyData) {
            String text = String.format(
                    "%s %s %.2f\n",
                    operation.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    type.equals("INCOME") ?
                            IncomeCategory.valueOf(operation.getCategory()).getName() :
                            ExpenseCategory.valueOf(operation.getCategory()).getName(),
                    operation.getSum()
            );

            if (history.length() + text.length() > MESSAGE_MAX_LENGTH) {
                bot.execute(
                        answerMessageFactory.getSendMessage(
                                chatId,
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

    private BotApiMethod<?> getHistoryForDayByType(CallbackQuery callbackQuery, String[] splitCallbackQuery) throws TelegramApiException {
        Long chatId = callbackQuery.getMessage().getChatId();
        LocalDate date = LocalDate.of(
                Integer.parseInt(splitCallbackQuery[3]),
                Integer.parseInt(splitCallbackQuery[4]),
                Integer.parseInt(splitCallbackQuery[5])
        );
        String type = splitCallbackQuery[1];
        List<Operation> historyData = operationRepository.findByCustomerChatIdAndTypeAndDate(chatId, type, date);

        if (historyData.isEmpty()) {
            return answerMessageFactory.getEditMessageText(
                    callbackQuery,
                    String.format(
                            "За данный день у Вас не было %s.",
                            type.equals("INCOME") ? "пополнений" : "списаний"
                    ),
                    null
            );
        }

        StringBuilder history = new StringBuilder();
        history.append(
                String.format(
                        "Ваша статистика по %s за %s\n\n",
                        type.equals("INCOME") ? "пополнениям" : "списаниям",
                        date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                )
        );

        for (Operation operation : historyData) {
            String text = String.format(
                    "%s %s %.2f\n",
                    operation.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    type.equals("INCOME") ?
                            IncomeCategory.valueOf(operation.getCategory()).getName() :
                            ExpenseCategory.valueOf(operation.getCategory()).getName(),
                    operation.getSum()
            );

            if (history.length() + text.length() > MESSAGE_MAX_LENGTH) {
                bot.execute(
                        answerMessageFactory.getSendMessage(
                                chatId,
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

    private String getMonthName(Integer monthNumber) {
        switch (monthNumber) {
            case 1 -> {
                return "Январь";
            }
            case 2 -> {
                return "Февраль";
            }
            case 3 -> {
                return "Март";
            }
            case 4 -> {
                return "Апрель";
            }
            case 5 -> {
                return "Май";
            }
            case 6 -> {
                return "Июнь";
            }
            case 7 -> {
                return "Июль";
            }
            case 8 -> {
                return "Август";
            }
            case 9 -> {
                return "Сентябрь";
            }
            case 10 -> {
                return "Октябрь";
            }
            case 11 -> {
                return "Ноябрь";
            }
            case 12 -> {
                return "Декабрь";
            }
        }

        return null;
    }
}
