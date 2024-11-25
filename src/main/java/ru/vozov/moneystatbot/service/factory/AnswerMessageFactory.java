package ru.vozov.moneystatbot.service.factory;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

@Component
public class AnswerMessageFactory {
    public SendMessage getSendMessage(Long chatId,
                                      String text,
                                      ReplyKeyboard replyKeyboard) {
        return SendMessage.builder().
                chatId(chatId).
                text(text).
                replyMarkup(replyKeyboard).
                disableWebPagePreview(true).
                build();
    }

    public EditMessageText getEditMessageText(CallbackQuery callbackQuery,
                                              String text,
                                              InlineKeyboardMarkup replyKeyboard) {
        return EditMessageText.builder().
                chatId(callbackQuery.getMessage().getChatId()).
                text(text).
                replyMarkup(replyKeyboard).
                messageId(callbackQuery.getMessage().getMessageId()).
                disableWebPagePreview(true).
                build();
    }

    public AnswerCallbackQuery getAnswerCallbackQuery(CallbackQuery callbackQuery,
                                                      String text) {
        return AnswerCallbackQuery.builder().
                callbackQueryId(callbackQuery.getId()).
                text(text).
                build();
    }

    public DeleteMessage getDeleteMessage(Integer messageId, Long chatId) {
        return DeleteMessage.builder().
                messageId(messageId).
                chatId(chatId).
                build();
    }
}
