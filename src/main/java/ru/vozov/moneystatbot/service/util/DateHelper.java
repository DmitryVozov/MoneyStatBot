package ru.vozov.moneystatbot.service.util;

import org.springframework.stereotype.Component;

import java.time.Month;
import java.util.ArrayList;
import java.util.List;

@Component
public class DateHelper {
    public String getMonthName(Integer monthNumber, boolean isGenitiveCase) {
        switch (monthNumber) {
            case 1 -> {
                return isGenitiveCase ? "января" : "январь";
            }
            case 2 -> {
                return isGenitiveCase ? "февраля" : "февраль";
            }
            case 3 -> {
                return isGenitiveCase ? "марта" : "март";
            }
            case 4 -> {
                return isGenitiveCase ? "апреля" : "апрель";
            }
            case 5 -> {
                return isGenitiveCase ? "мая" : "май";
            }
            case 6 -> {
                return isGenitiveCase ? "июня" : "июнь";
            }
            case 7 -> {
                return isGenitiveCase ? "июля" : "июль";
            }
            case 8 -> {
                return isGenitiveCase ? "августа" : "август";
            }
            case 9 -> {
                return isGenitiveCase ? "сентября" : "сентябрь";
            }
            case 10 -> {
                return isGenitiveCase ? "октября" : "октябрь";
            }
            case 11 -> {
                return isGenitiveCase ? "ноября" : "ноябрь";
            }
            case 12 -> {
                return isGenitiveCase ? "декабря" : "декабрь";
            }
        }

        return null;
    }

    public List<Integer> getMonthDaysKeyboardConfiguration(int monthNumber, int year) {
        int monthLength = Month.of(monthNumber).length(year % 4 == 0);
        List<Integer> configuration = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            configuration.add(5);
        }

        if (monthLength == 31) {
            configuration.add(5);
            configuration.add(6);
        }
        else if (monthLength == 30) {
            configuration.add(5);
            configuration.add(5);
        }
        else if (monthLength == 29) {
            configuration.add(5);
            configuration.add(4);
        }
        else {
            configuration.add(4);
            configuration.add(4);
        }

        return configuration;
    }
}
