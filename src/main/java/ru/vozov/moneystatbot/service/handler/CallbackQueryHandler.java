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
    final RefillManager refillManager;
    final ExpenseManager expenseManager;
    final StatisticsManager statisticsManager;

    @Autowired
    public CallbackQueryHandler(StartManager startManager, HelpManager helpManager, FeedbackManager feedbackManager, RefillManager refillManager, ExpenseManager expenseManager, StatisticsManager statisticsManager) {
        this.startManager = startManager;
        this.helpManager = helpManager;
        this.feedbackManager = feedbackManager;
        this.refillManager = refillManager;
        this.expenseManager = expenseManager;
        this.statisticsManager = statisticsManager;
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
            case "REFILL" -> {
                return refillManager.answerCallbackQuery(update.getCallbackQuery());
            }
            case "EXPENSE" -> {
                return expenseManager.answerCallbackQuery(update.getCallbackQuery());
            }
            case "STATISTICS" -> {
                return statisticsManager.answerCallbackQuery(update.getCallbackQuery());
            }
        }
        return null;
    }
}
