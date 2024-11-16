package com.trading.snipperBot.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtil {

    public static String getTodayDateWithDay() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E_yyyy-MM-dd"); // e.g., "Tue_2024-11-13"
        return today.format(formatter);
    }

}
