package ru.vozov.moneystatbot.service.manager;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.vozov.moneystatbot.service.factory.AnswerMessageFactory;
import ru.vozov.moneystatbot.service.factory.KeyboardFactory;

import java.util.List;

import static ru.vozov.moneystatbot.service.data.CallbackQueryData.START;
import static ru.vozov.moneystatbot.service.data.MessageData.FEEDBACK_MESSAGE;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FeedbackManager {
    final KeyboardFactory keyboardFactory;
    final AnswerMessageFactory answerMessageFactory;

    @Autowired
    public FeedbackManager(KeyboardFactory keyboardFactory, AnswerMessageFactory answerMessageFactory) {
        this.keyboardFactory = keyboardFactory;
        this.answerMessageFactory = answerMessageFactory;
    }

    public BotApiMethod<?> answerCommand(Message message) {
        return answerMessageFactory.getSendMessage(
                message.getChatId(),
                FEEDBACK_MESSAGE,
                null
        );
    }

    public BotApiMethod<?> answerCallBackQuery(CallbackQuery callbackQuery) {
        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                FEEDBACK_MESSAGE,
                keyboardFactory.getInlineKeyboard(
                        List.of("\uD83D\uDD19Назад"),
                        List.of(1),
                        List.of(START)
                )
        );
    }
}
