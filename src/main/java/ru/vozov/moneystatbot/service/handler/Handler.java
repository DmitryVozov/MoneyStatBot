package ru.vozov.moneystatbot.service.handler;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface Handler {
    BotApiMethod<?> answer(Update update);
}
