package ru.vozov.moneystatbot.service.handler;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.vozov.moneystatbot.model.customer.Customer;
import ru.vozov.moneystatbot.repository.CustomerRepository;
import ru.vozov.moneystatbot.service.manager.RefillManager;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageHandler implements Handler {
    final RefillManager refillManager;
    final CustomerRepository customerRepository;

    @Autowired
    public MessageHandler(RefillManager refillManager, CustomerRepository customerRepository) {
        this.refillManager = refillManager;
        this.customerRepository = customerRepository;
    }

    @Override
    public BotApiMethod<?> answer(Update update) {
        Customer customer = customerRepository.findById(update.getMessage().getChatId()).orElseThrow();
        String keyWord = customer.getStatus().toString().split("_")[1];

        switch (keyWord) {
            case "REFILL" -> {
                return refillManager.answerMessage(update.getMessage(), customer);
            }
        }
        return null;
    }
}