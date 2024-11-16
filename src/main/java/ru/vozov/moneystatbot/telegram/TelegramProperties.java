package ru.vozov.moneystatbot.telegram;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TelegramProperties {
    @Value("${telegram-bot.path}")
    String path;
    @Value("${telegram-bot.username}")
    String username;
    @Value("${telegram-bot.token}")
    String token;
}
