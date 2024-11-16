package com.trading.snipperBot.utils;

import java.util.Random;

public class RandomNumUtil {

    public static String random4NumId(long tradeId) {

        Random rand = new Random();
        int random4Digit = rand.nextInt(9000) + 1000; // Ensures it's always a 4-digit number

        return String.valueOf(tradeId) + random4Digit;

    }

}
