package ru.vozov.moneystatbot.service.handler;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.vozov.moneystatbot.service.factory.AnswerMessageFactory;
import ru.vozov.moneystatbot.service.manager.*;

import static ru.vozov.moneystatbot.service.data.CommandData.*;
import static ru.vozov.moneystatbot.service.data.MessageData.UNSUPPORTED_COMMAND_MESSAGE;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommandHandler implements Handler {
    final StartManager startManager;
    final HelpManager helpManager;
    final FeedbackManager feedbackManager;
    final OperationManager operationManager;
    final StatisticsManager statisticsManager;
    final HistoryManager historyManager;
    final AnswerMessageFactory answerMessageFactory;

    @Autowired
    public CommandHandler(StartManager startManager,
                          HelpManager helpManager,
                          FeedbackManager feedbackManager,
                          OperationManager operationManager,
                          StatisticsManager statisticsManager,
                          HistoryManager historyManager,
                          AnswerMessageFactory answerMessageFactory) {
        this.startManager = startManager;
        this.helpManager = helpManager;
        this.feedbackManager = feedbackManager;
        this.operationManager = operationManager;
        this.statisticsManager = statisticsManager;
        this.historyManager = historyManager;
        this.answerMessageFactory = answerMessageFactory;
    }

    @Override
    public BotApiMethod<?> answer(Update update) {
        Message message = update.getMessage();

        switch (message.getText()) {
            case START_COMMAND -> {
                return startManager.answerCommand(message);
            }
            case HELP_COMMAND -> {
                return helpManager.answerCommand(message);
            }
            case FEEDBACK_COMMAND -> {
                return feedbackManager.answerCommand(message);
            }
            case INCOME_COMMAND, EXPENSE_COMMAND -> {
                return operationManager.answerCommand(message);
            }
            case STATISTICS_COMMAND -> {
                return statisticsManager.answerCommand(message);
            }
            case HISTORY_COMMAND -> {
                return historyManager.answerCommand(message);
            }
            default -> {
                return defaultAnswer(message);
            }
        }
    }

    private BotApiMethod<?> defaultAnswer(Message message) {
        return answerMessageFactory.getSendMessage(
                message.getChatId(),
                UNSUPPORTED_COMMAND_MESSAGE,
                null
        );
    }
}
