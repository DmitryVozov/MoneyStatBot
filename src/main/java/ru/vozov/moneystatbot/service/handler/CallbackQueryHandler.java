package ru.vozov.moneystatbot.service.handler;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.vozov.moneystatbot.service.manager.*;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CallbackQueryHandler implements Handler {
    final StartManager startManager;
    final HelpManager helpManager;
    final FeedbackManager feedbackManager;
    final OperationManager operationManager;
    final StatisticsManager statisticsManager;
    final HistoryManager historyManager;

    @Autowired
    public CallbackQueryHandler(StartManager startManager,
                                HelpManager helpManager,
                                FeedbackManager feedbackManager,
                                OperationManager operationManager,
                                StatisticsManager statisticsManager, HistoryManager historyManager) {
        this.startManager = startManager;
        this.helpManager = helpManager;
        this.feedbackManager = feedbackManager;
        this.operationManager = operationManager;
        this.statisticsManager = statisticsManager;
        this.historyManager = historyManager;
    }

    @Override
    public BotApiMethod<?> answer(Update update) {
        String data = update.getCallbackQuery().getData().split("_")[0];

        switch (data) {
            case "START" -> {
                return startManager.answerCallbackQuery(update.getCallbackQuery());
            }
            case "HELP" -> {
                return helpManager.answerCallbackQuery(update.getCallbackQuery());
            }
            case "FEEDBACK" -> {
                return feedbackManager.answerCallBackQuery(update.getCallbackQuery());
            }
            case "INCOME", "EXPENSE" -> {
                return operationManager.answerCallbackQuery(update.getCallbackQuery());
            }
            case "STATISTICS" -> {
                return statisticsManager.answerCallbackQuery(update.getCallbackQuery());
            }
            case "HISTORY" -> {
                return historyManager.answerCallbackQuery(update.getCallbackQuery());
            }
        }
        return null;
    }
}
