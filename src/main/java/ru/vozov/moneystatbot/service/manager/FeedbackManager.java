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
                """
                        По вопросам и предложениям Вы можете связаться с нами по данным контактам:
                        https://t.me/mintl0l
                        https://github.com/DmitryVozov
                        """,
                null
        );
    }

    public BotApiMethod<?> answerCallBackQuery(CallbackQuery callbackQuery) {
        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                """
                        По вопросам и предложениям Вы можете связаться с нами по данным контактам:
                        https://t.me/mintl0l
                        https://github.com/DmitryVozov
                        """,
                keyboardFactory.getInlineKeyboard(
                        List.of("Назад"),
                        List.of(1),
                        List.of("START")
                )
        );
    }
}
