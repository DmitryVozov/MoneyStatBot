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
import ru.vozov.moneystatbot.service.util.DateHelper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static ru.vozov.moneystatbot.service.data.CallbackQueryData.*;
import static ru.vozov.moneystatbot.service.data.MessageData.*;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StatisticsManager {
    final AnswerMessageFactory answerMessageFactory;
    final KeyboardFactory keyboardFactory;
    final OperationRepository operationRepository;
    final DateHelper dateHelper;

    @Autowired
    public StatisticsManager(AnswerMessageFactory answerMessageFactory,
                             KeyboardFactory keyboardFactory,
                             OperationRepository operationRepository,
                             DateHelper dateHelper) {
        this.answerMessageFactory = answerMessageFactory;
        this.keyboardFactory = keyboardFactory;
        this.operationRepository = operationRepository;

        this.dateHelper = dateHelper;
    }

    public BotApiMethod<?> answerCommand(Message message) {
        return answerMessageFactory.getSendMessage(
                message.getChatId(),
                ASK_STATISTICS_TYPE_MESSAGE,
                keyboardFactory.getInlineKeyboard(
                        List.of("\uD83D\uDCC8Доходы", "\uD83D\uDCC9Расходы",
                                "Отмена"),
                        List.of(2, 1),
                        List.of(STATISTICS_INCOME, STATISTICS_EXPENSE,
                                STATISTICS_CANCEL)
                )
        );
    }

    public BotApiMethod<?> answerCallbackQuery(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();

        if (data.equals(STATISTICS_CANCEL)) {
            return cancel(callbackQuery);
        }

        String[] splitCallbackQuery = data.split("_");

        int length = splitCallbackQuery.length;

        switch (length) {
            case 2 -> {
                return askPeriod(callbackQuery);
            }
            case 3 -> {
                if (data.contains(ALL_TIME)) {
                    return getStatisticsForAllTimeByType(callbackQuery, splitCallbackQuery);
                }

                return askYear(callbackQuery, splitCallbackQuery);
            }
            case 4 -> {
                if (data.contains(YEAR)) {
                    return getStatisticsForYearByType(callbackQuery, splitCallbackQuery);
                }

                return askMonth(callbackQuery);
            }
            case 5 -> {
                if (data.contains(MONTH)) {
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
        String data = callbackQuery.getData();

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                ASK_STATISTICS_PERIOD_MESSAGE,
                keyboardFactory.getInlineKeyboard(
                        List.of("День","Месяц","Год",
                                "Все время",
                                "❌Отмена"),
                        List.of(3,1),
                        List.of(data + DAY, data + MONTH, data + YEAR,
                                data + ALL_TIME,
                                STATISTICS_CANCEL)
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
                    NO_STATISTICS_DATA_MESSAGE,
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
        data.add(STATISTICS_CANCEL);
        configuration.add(1);

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                ASK_STATISTICS_YEAR_MESSAGE,
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
                ASK_STATISTICS_MONTH_MESSAGE,
                keyboardFactory.getInlineKeyboard(
                        List.of("Январь","Февраль","Март","Апрель",
                                "Май","Июнь","Июль","Август",
                                "Сентябрь","Октябрь","Ноябрь","Декабрь",
                                "❌Отмена"),
                        List.of(4, 4, 4, 1),
                        List.of(data + JANUARY_NUMBER, data + FEBRUARY_NUMBER, data + MARCH_NUMBER, data + APRIL_NUMBER,
                                data + MAY_NUMBER, data + JUNE_NUMBER, data + JULY_NUMBER, data + AUGUST_NUMBER,
                                data + SEPTEMBER_NUMBER, data + OCTOBER_NUMBER, data + NOVEMBER_NUMBER, data + DECEMBER_NUMBER,
                                STATISTICS_CANCEL)
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
        data.add(STATISTICS_CANCEL);
        text.add("❌Отмена");

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                ASK_STATISTICS_DAY_MESSAGE,
                keyboardFactory.getInlineKeyboard(
                        text,
                        configuration,
                        data
                )
        );
    }

    private BotApiMethod<?> getStatisticsForAllTimeByType(CallbackQuery callbackQuery, String[] splitCallbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        String type = splitCallbackQuery[1];
        List<Object[]> statisticsData = operationRepository.getSumForAllTimeByCustomerChatIdAndTypeGroupByCategory(
                chatId,
                type
        );

        if (statisticsData.isEmpty()) {
            return answerMessageFactory.getEditMessageText(
                    callbackQuery,
                    NO_STATISTICS_DATA_MESSAGE,
                    null
            );
        }

        Double sumTotal = (Double) operationRepository.getSumForAllTimeByCustomerChatIdAndType(chatId, type)[0];

        StringBuilder statistics = new StringBuilder();
        statistics.append(
                String.format(
                    STATISTICS_FOR_ALL_TIME_MESSAGE,
                    type.equals(OperationType.INCOME.toString()) ? "доходам" : "расходам"
                )
        );

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                getStatisticsData(statisticsData, statistics, type, sumTotal),
                null
        );
    }

    private BotApiMethod<?> getStatisticsForYearByType(CallbackQuery callbackQuery, String[] splitCallbackQuery) {
        Integer year = Integer.parseInt(splitCallbackQuery[3]);
        String type = splitCallbackQuery[1];

        StringBuilder statistics = new StringBuilder();
        statistics.append(
                String.format(
                        STATISTICS_FOR_YEAR_MESSAGE,
                        type.equals(OperationType.INCOME.toString()) ? "доходам" : "расходам",
                        year
                )
        );

        Long chatId = callbackQuery.getMessage().getChatId();
        List<Object[]> statisticsData = operationRepository.getSumForYearByCustomerChatIdAndTypeGroupByCategory(chatId, type, year);
        Double sumTotal = (Double) operationRepository.getSumForYearByCustomerChatIdAndType(chatId, type, year)[0];

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                getStatisticsData(statisticsData, statistics, type, sumTotal),
                null
        );
    }

    private BotApiMethod<?> getStatisticsForMonthByType(CallbackQuery callbackQuery, String[] splitCallbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer year = Integer.parseInt(splitCallbackQuery[3]);
        Integer month = Integer.parseInt(splitCallbackQuery[4]);
        String type = splitCallbackQuery[1];
        List<Object[]> statisticsData = operationRepository.getSumForMonthByCustomerChatIdAndTypeGroupByCategory(chatId, type, year, month);

        if (statisticsData.isEmpty()) {
            return answerMessageFactory.getEditMessageText(
                    callbackQuery,
                    String.format(
                        NO_STATISTICS_DATA_FOR_MONTH_MESSAGE,
                        type.equals(OperationType.INCOME.toString()) ? "доходов" : "расходов"
                    ),
                    null
            );
        }

        StringBuilder statistics = new StringBuilder();

        statistics.append(
                String.format(
                        STATISTICS_FOR_MONTH_MESSAGE,
                        type.equals(OperationType.INCOME.toString()) ? "доходам" : "расходам",
                        dateHelper.getMonthName(month, false),
                        year
                )
        );

        Double sumTotal = (Double) operationRepository.getSumForMonthByCustomerChatIdAndType(chatId, type, year, month)[0];

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                getStatisticsData(statisticsData, statistics, type, sumTotal),
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
        String type = splitCallbackQuery[1];
        List<Object[]> statisticsData = operationRepository.getSumForDayByCustomerChatIdAndTypeGroupByCategory(chatId, date, type);

        if (statisticsData.isEmpty()) {
            return answerMessageFactory.getEditMessageText(
                    callbackQuery,
                    String.format(
                            NO_STATISTICS_DATA_FOR_DAY_MESSAGE,
                            type.equals(OperationType.INCOME.toString()) ? "доходов" : "расходов"
                    ),
                    null
            );
        }

        StringBuilder statistics = new StringBuilder();
        statistics.append(
                String.format(
                        STATISTICS_FOR_DAY_MESSAGE,
                        type.equals(OperationType.INCOME.toString()) ? "доходам" : "расходам",
                        date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                )
        );

        Double sumTotal = (Double) operationRepository.getSumForDayByCustomerChatIdAndType(chatId, date, type)[0];

        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                getStatisticsData(statisticsData, statistics, type, sumTotal),
                null
        );
    }

    private String getStatisticsData(List<Object[]> statisticsData,
                                     StringBuilder statistics,
                                     String type,
                                     Double sumTotal) {
        for (Object[] data : statisticsData) {
            String category = type.equals(OperationType.INCOME.toString()) ?
                    IncomeCategory.valueOf((String) data[0]).getName() :
                    ExpenseCategory.valueOf((String) data[0]).getName();
            Double sum = (Double) data[1];

            statistics.append(String.format(CATEGORY_STATISTICS_MESSAGE, category, sum / sumTotal * 100, sum));
        }

        return statistics.toString();
    }

    private BotApiMethod<?> cancel(CallbackQuery callbackQuery) {
        return answerMessageFactory.getDeleteMessage(
                callbackQuery.getMessage().getMessageId(),
                callbackQuery.getMessage().getChatId()
        );
    }

 }
