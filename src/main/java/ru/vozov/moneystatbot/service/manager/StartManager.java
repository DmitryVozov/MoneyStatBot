package ru.vozov.moneystatbot.service.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

import ru.vozov.moneystatbot.model.customer.Customer;
import ru.vozov.moneystatbot.model.customer.CustomerStatus;
import ru.vozov.moneystatbot.repository.CustomerRepository;
import ru.vozov.moneystatbot.service.factory.AnswerMessageFactory;
import ru.vozov.moneystatbot.service.factory.KeyboardFactory;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class StartManager {
    final AnswerMessageFactory answerMessageFactory;
    final KeyboardFactory keyboardFactory;
    final CustomerRepository customerRepository;

    @Autowired
    public StartManager(AnswerMessageFactory answerMessageFactory, KeyboardFactory keyboardFactory, CustomerRepository customerRepository) {
        this.answerMessageFactory = answerMessageFactory;
        this.keyboardFactory = keyboardFactory;
        this.customerRepository = customerRepository;
    }

    public BotApiMethod<?> answerCommand(Message message) {
        Long chatId = message.getChatId();

        if (!customerRepository.existsById(chatId)) {
            Customer customer = Customer.builder().
                    chatId(chatId).
                    status(CustomerStatus.FREE).
                    createAt(LocalDateTime.now()).
                    build();

            customerRepository.save(customer);
        }

        return answerMessageFactory.getSendMessage(
                message.getChatId(),
                """
                        Вас приветствует бот для учета финансов, он Вам поможет сохранять информацию о Ваших пополнениях и тратах и создавать отчеты по указанным данным за разные периоды.
                        """,
                keyboardFactory.getInlineKeyboard(
                        List.of("Добавить пополнение", "Добавить списание",
                                "Помощь", "Обратная связь"
                        ),
                        List.of(2, 2),
                        List.of("INCOME", "EXPENSE",
                                "HELP", "FEEDBACK"
                        )
                )
        );
    }

    public BotApiMethod<?> answerCallbackQuery(CallbackQuery callbackQuery) {
        return answerMessageFactory.getEditMessageText(
                callbackQuery,
                """
                        Вас приветствует бот для учета финансов, он Вам поможет сохранять информацию о Ваших пополнениях и тратах и создавать отчеты по указанным данным за разные периоды.
                        """,
                keyboardFactory.getInlineKeyboard(
                        List.of("Добавить пополнение", "Добавить списание",
                                "Помощь", "Обратная связь"
                        ),
                        List.of(2, 2),
                        List.of("INCOME", "EXPENSE",
                                "HELP", "FEEDBACK"
                        )
                )
        );
    }
}
