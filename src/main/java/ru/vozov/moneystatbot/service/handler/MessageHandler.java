package ru.vozov.moneystatbot.service.handler;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.vozov.moneystatbot.model.customer.Customer;
import ru.vozov.moneystatbot.repository.CustomerRepository;
import ru.vozov.moneystatbot.service.manager.OperationManager;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageHandler implements Handler {
    final OperationManager operationManager;
    final CustomerRepository customerRepository;

    @Autowired
    public MessageHandler(OperationManager operationManager,
                          CustomerRepository customerRepository) {
        this.operationManager = operationManager;
        this.customerRepository = customerRepository;
    }

    @Override
    public BotApiMethod<?> answer(Update update) {
        Customer customer = customerRepository.findById(update.getMessage().getChatId()).orElseThrow();
        String status = customer.getStatus().toString();

        String keyWord = !status.equals("FREE") ? status.split("_")[1] : status;

        switch (keyWord) {
            case "INCOME", "EXPENSE" -> {
                return operationManager.answerMessage(update.getMessage(), customer);
            }
        }
        return null;
    }
}
