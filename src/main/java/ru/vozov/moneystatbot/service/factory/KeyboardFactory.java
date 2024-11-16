package ru.vozov.moneystatbot.service.factory;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
public class KeyboardFactory {
    public InlineKeyboardMarkup getInlineKeyboard(List<String> text,
                                                  List<Integer> configuration,
                                                  List<String> data) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        int index = 0;

        for (Integer rowSize: configuration) {
            List<InlineKeyboardButton> row = new ArrayList<>();

            for (int i = 0; i<rowSize; i++) {
                InlineKeyboardButton button = new InlineKeyboardButton(text.get(index));
                button.setCallbackData(data.get(index));
                row.add(button);
                index++;
            }

            keyboard.add(row);
        }

        return new InlineKeyboardMarkup(keyboard);
    }
}
