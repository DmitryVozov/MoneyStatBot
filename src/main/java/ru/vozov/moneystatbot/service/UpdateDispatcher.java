package ru.vozov.moneystatbot.service;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.vozov.moneystatbot.service.handler.CallbackQueryHandler;
import ru.vozov.moneystatbot.service.handler.CommandHandler;
import ru.vozov.moneystatbot.service.handler.MessageHandler;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateDispatcher {
    final CommandHandler commandHandler;
    final MessageHandler messageHandler;
    final CallbackQueryHandler callbackQueryHandler;

    public UpdateDispatcher(CommandHandler commandHandler, MessageHandler messageHandler, CallbackQueryHandler callbackQueryHandler) {
        this.commandHandler = commandHandler;
        this.messageHandler = messageHandler;
        this.callbackQueryHandler = callbackQueryHandler;
    }

    public BotApiMethod<?> distribute(Update update) {
        if (update.hasCallbackQuery()) {
            return callbackQueryHandler.answer(update);
        }
        else if (update.hasMessage()
                && update.getMessage().hasText()
                && update.getMessage().getText().charAt(0) == '/') {
            return commandHandler.answer(update);
        }

        return messageHandler.answer(update);
    }
}
