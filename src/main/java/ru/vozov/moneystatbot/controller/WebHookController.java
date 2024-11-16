package ru.vozov.moneystatbot.controller;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.vozov.moneystatbot.telegram.MoneyStatBot;

@RestController
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WebHookController {
    final MoneyStatBot bot;

    @Autowired
    public WebHookController(MoneyStatBot bot) {
        this.bot = bot;
    }

    @PostMapping("/")
    public BotApiMethod<?> onWebhookUpdateReceived(@RequestBody Update update) {
        return bot.onWebhookUpdateReceived(update);
    }
}
