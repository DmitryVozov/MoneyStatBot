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
public class HelpManager {
    final KeyboardFactory keyboardFactory;
    final AnswerMessageFactory answerMessageFactory;

    @Autowired
    public HelpManager(KeyboardFactory keyboardFactory, AnswerMessageFactory answerMessageFactory) {
        this.keyboardFactory = keyboardFactory;
        this.answerMessageFactory = answerMessageFactory;
    }

    public BotApiMethod<?> answerCommand(Message message) {
        return answerMessageFactory.getSendMessage(
                message.getChatId(),
                """
                        В главном меню указаны основные доступные команды:
                        /refill - добавление информации о пополнении
                        /expense - добавление информации о списании
                        /statistic - просмотр статистики Ваших трат и пополнений за все время или за указанный период
                        """,
                null
        );
    }

    public BotApiMethod<?> answerCallbackQuery(CallbackQuery callbackQuery) {
        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                """
                        В главном меню указаны основные доступные команды:
                        /refill - добавление информации о пополнении
                        /expense - добавление информации о списании
                        /statistic - просмотр статистики Ваших трат и пополнений за все время или за указанный период
                        """,

                keyboardFactory.getInlineKeyboard(
                        List.of("Назад"),
                        List.of(1),
                        List.of("START")
                )
        );
    }
}
