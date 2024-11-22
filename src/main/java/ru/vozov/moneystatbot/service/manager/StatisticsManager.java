package ru.vozov.moneystatbot.service.manager;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.vozov.moneystatbot.model.operation.ExpenseCategory;
import ru.vozov.moneystatbot.model.operation.IncomeCategory;
import ru.vozov.moneystatbot.model.operation.OperationType;
import ru.vozov.moneystatbot.repository.OperationRepository;
import ru.vozov.moneystatbot.service.factory.AnswerMessageFactory;
import ru.vozov.moneystatbot.service.factory.KeyboardFactory;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StatisticsManager {
    final AnswerMessageFactory answerMessageFactory;
    final KeyboardFactory keyboardFactory;
    final OperationRepository operationRepository;

    @Autowired
    public StatisticsManager(AnswerMessageFactory answerMessageFactory,
                             KeyboardFactory keyboardFactory,
                             OperationRepository operationRepository) {
        this.answerMessageFactory = answerMessageFactory;
        this.keyboardFactory = keyboardFactory;
        this.operationRepository = operationRepository;

    }

    public BotApiMethod<?> answerCommand(Message message) {
        return answerMessageFactory.getSendMessage(
                message.getChatId(),
                """
                        Здесь вы можете получить статистику по Вашим тратам и пополнениям.
                        Выберите по какому типу Вы хотите получить статистику.
                        """,
                keyboardFactory.getInlineKeyboard(
                        List.of("Пополнения", "Списания",
                                "Отмена"),
                        List.of(2, 1),
                        List.of("STATISTICS_INCOME", "STATISTICS_EXPENSE",
                                "STATISTICS_CANCEL")
                )
        );
    }

    public BotApiMethod<?> answerCallbackQuery(CallbackQuery callbackQuery) {
        String[] splitCallbackQuery = callbackQuery.getData().split("_");

        if (splitCallbackQuery[1].equals("CANCEL")) {
            return cancel(callbackQuery);
        }

        int length = splitCallbackQuery.length;

        switch (length) {
            case 2 -> {
                return askPeriod(callbackQuery);
            }
            case 3 -> {
                if (splitCallbackQuery[2].equals("ALLTIME")) {
                    return getStatisticsForAllTimeByType(callbackQuery, splitCallbackQuery);
                }

                return askYear(callbackQuery, splitCallbackQuery);
            }
            case 4 -> {
                if (splitCallbackQuery[2].equals("YEAR")) {
                    return getStatisticsForYearByType(callbackQuery, splitCallbackQuery);
                }

                return askMonth(callbackQuery);
            }
            case 5 -> {
                if (splitCallbackQuery[2].equals("MONTH")) {
                    return getStatisticsForMonthByType(callbackQuery, splitCallbackQuery);
                }

                return askDay(callbackQuery, splitCallbackQuery);
            }
            case 6 -> {
                return getStatisticsForDayByType(callbackQuery, splitCallbackQuery);
            }
        }

        return null;
    }

    private BotApiMethod<?> askPeriod(CallbackQuery callbackQuery) {
        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                """
                        Выберите за какой промежуток времени Вы хотите получить статистику
                        """,
                keyboardFactory.getInlineKeyboard(
                        List.of("День","Месяц","Год",
                                "Все время",
                                "Отмена"),
                        List.of(3,1),
                        List.of(callbackQuery.getData()+"_DAY",callbackQuery.getData()+"_MONTH", callbackQuery.getData()+"_YEAR",
                                callbackQuery.getData()+"_ALLTIME",
                                "STATISTICS_CANCEL")
                )
        );
    }

    private BotApiMethod<?> askYear(CallbackQuery callbackQuery, String[] splitCallbackQuery) {
        String statisticsType = splitCallbackQuery[1];

        List<Object[]> years = operationRepository.getDistinctYearsByCustomerChatIdAndType(
                callbackQuery.getMessage().getChatId(),
                statisticsType
        );

        if (years.isEmpty()) {
            return answerMessageFactory.getEditMessageText(
                    callbackQuery,
                    """
                            У Вас еще нет данных для получения статистики.
                            Для того чтобы добавить данные вызовите команду /income или /expense
                            """,
                    null
            );
        }

        List<String> text = new ArrayList<>();
        List<Integer> configuration = new ArrayList<>();
        List<String> data = new ArrayList<>();

        for (int i = 0; i < years.size(); i++) {
            Double d = (Double) years.get(i)[0];
            int year = d.intValue();
            text.add(String.valueOf(year));
            data.add(callbackQuery.getData() + "_" + year);

            configuration.add(1);
        }

        text.add("Отмена");
        data.add("STATISTICS_CANCEL");
        configuration.add(1);

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                """
                        Выберите за какой год Вы хотите получить статистику
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
                        Выберите за какой месяц Вы хотите получить статистику
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
                                "STATISTICS_CANCEL")
                )
        );
    }

    private BotApiMethod<?> askDay(CallbackQuery callbackQuery, String[] splitCallbackQuery) {
        Month month = Month.of(Integer.parseInt(splitCallbackQuery[4]));
        int year = Integer.parseInt(splitCallbackQuery[3]);
        int monthLength = month.length(year % 4 == 0);

        List<Integer> configuration;

        if (monthLength == 31) {
            configuration = List.of(5, 5, 5, 5, 5, 6);
        }
        else if (monthLength == 30) {
            configuration = List.of(5, 5, 5, 5, 5, 5);
        }
        else if (monthLength == 29) {
            configuration = List.of(5, 5, 5, 5, 5, 4);
        }
        else {
            configuration = List.of(5, 5, 5, 5, 4, 4);
        }

        List<String> days = new ArrayList<>();
        List<String> data = new ArrayList<>();

        for (int i = 1; i <= monthLength; i++) {
            days.add(String.valueOf(i));
            data.add(callbackQuery.getData() + "_" + i);
        }

        data.add("STATISTICS_CANCEL");

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                """
                        Выберите за какой день Вы хотите получить статистику
                        """,
                keyboardFactory.getInlineKeyboard(
                        days,
                        configuration,
                        data
                )
        );
    }

    private BotApiMethod<?> getStatisticsForAllTimeByType(CallbackQuery callbackQuery, String[] splitCallbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        OperationType operationType = OperationType.valueOf(splitCallbackQuery[1]);

        List<Object[]> statisticsData = operationRepository.getSumForAllTimeByCustomerChatIdAndTypeGroupByCategory(
                chatId,
                operationType.toString()
        );

        if (statisticsData.isEmpty()) {
            return answerMessageFactory.getEditMessageText(
                    callbackQuery,
                    """
                            У Вас еще нет данных для получения статистики.
                            Для того чтобы добавить данные вызовите команду /income или /expense
                            """,
                    null
            );
        }

        Double sumTotal = (Double) operationRepository.getSumForAllTimeByCustomerChatIdAndType(chatId, operationType.toString())[0];

        StringBuilder statistics = new StringBuilder();
        statistics.append(
                String.format(
                    "Ваша статистика по %s за все время\n\n",
                    operationType == OperationType.INCOME ? "пополнениям" : "списаниям"
                )
        );

        for (Object[] data : statisticsData) {
            String category = operationType == OperationType.INCOME ?
                    IncomeCategory.valueOf((String) data[0]).getName() :
                    ExpenseCategory.valueOf((String) data[0]).getName();
            Double sum = (Double) data[1];

            statistics.append(String.format("%s %.2f%% %.2f₽\n", category, sum / sumTotal * 100, sum));
        }

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                statistics.toString(),
                null
        );
    }

    private BotApiMethod<?> getStatisticsForYearByType(CallbackQuery callbackQuery, String[] splitCallbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer year = Integer.parseInt(splitCallbackQuery[3]);
        OperationType type = OperationType.valueOf(splitCallbackQuery[1]);
        List<Object[]> statisticsData = operationRepository.getSumForYearByCustomerChatIdAndTypeGroupByCategory(chatId, type.toString(), year);
        Double sumTotal = (Double) operationRepository.getSumForYearByCustomerChatIdAndType(chatId, type.toString(), year)[0];

        StringBuilder statistics = new StringBuilder();
        statistics.append(
                String.format(
                        "Ваша статистика по %s за %d год\n\n",
                        type == OperationType.INCOME ? "пополнениям" : "списаниям",
                        year
                )
        );

        for (Object[] data : statisticsData) {
            String category = type == OperationType.INCOME ?
                    IncomeCategory.valueOf((String) data[0]).getName() :
                    ExpenseCategory.valueOf((String) data[0]).getName();
            Double sum = (Double) data[1];

            statistics.append(String.format("%s %.2f%% %.2f₽\n", category, sum / sumTotal * 100, sum));
        }

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                statistics.toString(),
                null
        );
    }

    private BotApiMethod<?> getStatisticsForMonthByType(CallbackQuery callbackQuery, String[] splitCallbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer year = Integer.parseInt(splitCallbackQuery[3]);
        Integer month = Integer.parseInt(splitCallbackQuery[4]);
        OperationType type = OperationType.valueOf(splitCallbackQuery[1]);
        List<Object[]> statisticsData = operationRepository.getSumForMonthByCustomerChatIdAndTypeGroupByCategory(chatId, type.toString(), year, month);

        if (statisticsData.isEmpty()) {
            return answerMessageFactory.getEditMessageText(
                    callbackQuery,
                    String.format(
                        "За данный месяц у Вас не было %s.",
                        type == OperationType.INCOME ? "пополнений" : "списаний"
                    ),
                    null
            );
        }

        Double sumTotal = (Double) operationRepository.getSumForMonthByCustomerChatIdAndType(chatId, type.toString(), year, month)[0];

        String monthName = getMonthName(month);

        StringBuilder statistics = new StringBuilder();
        statistics.append(
                String.format(
                        "Ваша статистика по %s за %s %d года\n\n",
                        type == OperationType.INCOME ? "пополнениям" : "списаниям",
                        monthName,
                        year
                )
        );

        for (Object[] data : statisticsData) {
            String category = type == OperationType.INCOME ?
                    IncomeCategory.valueOf((String) data[0]).getName() :
                    ExpenseCategory.valueOf((String) data[0]).getName();
            Double sum = (Double) data[1];

            statistics.append(String.format("%s %.2f%% %.2f₽\n", category, sum / sumTotal * 100, sum));
        }

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                statistics.toString(),
                null
        );
    }

    private BotApiMethod<?> getStatisticsForDayByType(CallbackQuery callbackQuery, String[] splitCallbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        LocalDate date = LocalDate.of(
                Integer.parseInt(splitCallbackQuery[3]),
                Integer.parseInt(splitCallbackQuery[4]),
                Integer.parseInt(splitCallbackQuery[5])
        );
        OperationType type = OperationType.valueOf(splitCallbackQuery[1]);
        List<Object[]> statisticsData = operationRepository.getSumForDayByCustomerChatIdAndTypeGroupByCategory(chatId, date, type.toString());

        if (statisticsData.isEmpty()) {
            return answerMessageFactory.getEditMessageText(
                    callbackQuery,
                    String.format(
                            "За данный день у Вас не было %s.",
                            type == OperationType.INCOME ? "пополнений" : "списаний"
                    ),
                    null
            );
        }

        Double sumTotal = (Double) operationRepository.getSumForDayByCustomerChatIdAndType(chatId, date, type.toString())[0];

        StringBuilder statistics = new StringBuilder();
        statistics.append(
                String.format(
                        "Ваша статистика по %s за %s\n\n",
                        type == OperationType.INCOME ? "пополнениям" : "списаниям",
                        date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                )
        );

        for (Object[] data : statisticsData) {
            String category = type == OperationType.INCOME ?
                    IncomeCategory.valueOf((String) data[0]).getName() :
                    ExpenseCategory.valueOf((String) data[0]).getName();
            Double sum = (Double) data[1];

            statistics.append(String.format("%s %.2f%% %.2f₽\n", category, sum / sumTotal * 100, sum));
        }

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                statistics.toString(),
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
