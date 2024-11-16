package ru.vozov.moneystatbot.service.handler;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.vozov.moneystatbot.service.factory.AnswerMessageFactory;
import ru.vozov.moneystatbot.service.manager.FeedbackManager;
import ru.vozov.moneystatbot.service.manager.HelpManager;
import ru.vozov.moneystatbot.service.manager.RefillManager;
import ru.vozov.moneystatbot.service.manager.StartManager;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommandHandler implements Handler {
    final StartManager startManager;
    final HelpManager helpManager;
    final FeedbackManager feedbackManager;
    final RefillManager refillManager;
    final AnswerMessageFactory answerMessageFactory;

    @Autowired
    public CommandHandler(StartManager startManager, HelpManager helpManager, FeedbackManager feedbackManager, RefillManager refillManager, AnswerMessageFactory answerMessageFactory) {
        this.startManager = startManager;
        this.helpManager = helpManager;
        this.feedbackManager = feedbackManager;
        this.refillManager = refillManager;
        this.answerMessageFactory = answerMessageFactory;
    }

    @Override
    public BotApiMethod<?> answer(Update update) {
        Message message = update.getMessage();

        switch (message.getText()) {
            case "/start" -> {
                return startManager.answerCommand(message);
            }
            case "/help" -> {
                return helpManager.answerCommand(message);
            }
            case "/feedback" -> {
                return feedbackManager.answerCommand(message);
            }
            case "/refill" -> {
                return refillManager.answerCommand(message);
            }
            default -> {
                return defaultAnswer(message);
            }
        }
    }

    private BotApiMethod<?> defaultAnswer(Message message) {
        return answerMessageFactory.getSendMessage(
                message.getChatId(),
                "Данная команда не поддерживается",
                null
        );
    }
}