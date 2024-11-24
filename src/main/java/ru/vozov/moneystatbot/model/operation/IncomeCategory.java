package ru.vozov.moneystatbot.model.operation;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

import static ru.vozov.moneystatbot.service.data.CallbackQueryData.INCOME_CATEGORY;

@Getter
public enum IncomeCategory {
    PAYCHECK("\uD83D\uDCB0Зарплата"),
    INTEREST("\uD83C\uDFE6Проценты"),
    GIFT("\uD83C\uDF81Подарок"),
    CASHBACK("\uD83D\uDCB8Кэшбэк");

    private final String name;

    IncomeCategory(String name) {
        this.name = name;
    }

    public static List<String> getCategoryNames() {
        List<String> categoryNames = new ArrayList<>();

        for (IncomeCategory category : IncomeCategory.values()) {
            categoryNames.add(category.name);
        }

        return categoryNames;
    }

    public static List<Integer> getInlineKeyboardConfiguration() {
        List<Integer> configuration = new ArrayList<>();
        configuration.add(2);
        configuration.add(2);

        return configuration;
    }

    public static List<String> getCallbackQueryDataNames() {
        List<String> callbackQueryDataNames = new ArrayList<>();

        for (IncomeCategory category : IncomeCategory.values()) {
            callbackQueryDataNames.add(INCOME_CATEGORY + category);
        }

        return callbackQueryDataNames;
    }
}
