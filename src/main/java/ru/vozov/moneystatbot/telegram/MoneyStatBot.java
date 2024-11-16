package ru.vozov.moneystatbot.telegram;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.vozov.moneystatbot.service.UpdateDispatcher;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MoneyStatBot extends TelegramWebhookBot {
    final TelegramProperties properties;
    final UpdateDispatcher updateDispatcher;

    @Autowired
    public MoneyStatBot(TelegramProperties properties, UpdateDispatcher updateDispatcher) {
        super(properties.getToken());
        this.properties = properties;
        this.updateDispatcher = updateDispatcher;
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        return updateDispatcher.distribute(update);
    }

    @Override
    public String getBotPath() {
        return properties.getPath();
    }

    @Override
    public String getBotUsername() {
        return properties.getUsername();
    }
}
