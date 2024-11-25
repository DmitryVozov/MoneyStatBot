package ru.vozov.moneystatbot.model.operation;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static ru.vozov.moneystatbot.service.data.CallbackQueryData.EXPENSE_CATEGORY;

@Getter
public enum ExpenseCategory {
    FOOD("\uD83C\uDF54Еда"),
    TRANSPORT("\uD83D\uDE8CТранспорт"),
    ALCOHOL("\uD83C\uDF7EАлкоголь"),
    HOME("\uD83C\uDFE0Дом"),
    BOOK("\uD83D\uDCDAКниги"),
    INTERNET("\uD83D\uDC68\u200D\uD83D\uDCBBИнтернет"),
    HEALTH("\uD83D\uDC8AЗдоровье"),
    CLOTHES("\uD83D\uDC55Одежда"),
    EDUCATION("\uD83D\uDC68\u200D\uD83C\uDF93Образование"),
    CAFE("\uD83C\uDF5DРесторан"),
    GIFT("\uD83C\uDF81Подарок"),
    BARBERSHOP("\uD83D\uDC87\u200D♂\uFE0FПарикмахерская");

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
        configuration.add(2);
        configuration.add(2);
        configuration.add(2);
        configuration.add(2);
        configuration.add(2);
        configuration.add(2);

        return configuration;
    }

    public static List<String> getCallbackQueryDataNames(UUID operationId) {
        List<String> callbackQueryDataNames = new ArrayList<>();

        for (ExpenseCategory category : ExpenseCategory.values()) {
            callbackQueryDataNames.add(EXPENSE_CATEGORY + category + "_" + operationId);
        }

        return callbackQueryDataNames;
    }
}
