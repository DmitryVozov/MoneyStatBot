package ru.vozov.moneystatbot.model.refill;

import lombok.Getter;

@Getter
public enum RefillType {
    PAYCHECK("Зарплата"),
    INTEREST("Проценты"),
    GIFT("Подарок"),
    CASHBACK("Кэшбек");

    private final String name;

    RefillType(String name) {
        this.name = name;
    }
}
