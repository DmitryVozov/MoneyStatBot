package ru.vozov.moneystatbot.service.handler;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.vozov.moneystatbot.service.manager.FeedbackManager;
import ru.vozov.moneystatbot.service.manager.HelpManager;
import ru.vozov.moneystatbot.service.manager.RefillManager;
import ru.vozov.moneystatbot.service.manager.StartManager;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CallbackQueryHandler implements Handler {
    final StartManager startManager;
    final HelpManager helpManager;
    final FeedbackManager feedbackManager;
    final RefillManager refillManager;

    @Autowired
    public CallbackQueryHandler(StartManager startManager, HelpManager helpManager, FeedbackManager feedbackManager, RefillManager refillManager) {
        this.startManager = startManager;
        this.helpManager = helpManager;
        this.feedbackManager = feedbackManager;
        this.refillManager = refillManager;
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
        }
        return null;
    }
}
