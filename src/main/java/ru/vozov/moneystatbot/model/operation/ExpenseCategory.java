package ru.vozov.moneystatbot.model.operation;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public enum ExpenseCategory {
    FOOD("Еда"),
    TRANSPORT("Транспорт"),
    ALCOHOL("Алкоголь"),
    HOME("Дом"),
    BOOK("Книги"),
    INTERNET("Интернет"),
    HEALTH("Здоровье"),
    CLOTHES("Одежда"),
    EDUCATION("Образование"),
    CAFE("Ресторан"),
    GIFT("Подарок"),
    BARBERSHOP("Парикмахерская");

    private final String name;

    ExpenseCategory(String name) {
        this.name = name;
    }

    public static List<String> getCategoryNames() {
        List<String> categoryNames = new ArrayList<>();

        for (ExpenseCategory category : ExpenseCategory.values()) {
            categoryNames.add(category.name);
        }

        return categoryNames;
    }

    public static List<Integer> getInlineKeyboardConfiguration() {
        List<Integer> configuration = new ArrayList<>();
        configuration.add(3);
        configuration.add(3);
        configuration.add(3);
        configuration.add(3);

        return configuration;
    }

    public static List<String> getCallbackQueryDataNames() {
        List<String> callbackQueryDataNames = new ArrayList<>();

        for (ExpenseCategory category : ExpenseCategory.values()) {
            callbackQueryDataNames.add("EXPENSE_CATEGORY_" + category);
        }

        return callbackQueryDataNames;
    }
}
