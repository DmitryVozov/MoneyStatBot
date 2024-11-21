package ru.vozov.moneystatbot.model.expense;

import lombok.Getter;

@Getter
public enum ExpenseType {
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

    ExpenseType(String name) {
        this.name = name;
    }
}
