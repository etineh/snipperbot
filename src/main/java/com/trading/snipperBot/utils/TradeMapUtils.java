package com.trading.snipperBot.utils;

import java.util.HashMap;
import java.util.Map;

public class TradeMapUtils {

    public static Map<String, String> tradeParamMap(String symbol, String buyOrSell, String marketOrLimit, String quantity,
                                                    String symbolUpdate, String limitPrice)
    {
        Map<String, String> tradeMap = new HashMap<>();
        tradeMap.put("symbol", symbol);
        tradeMap.put("buyOrSell", buyOrSell);
        tradeMap.put("marketOrLimit", marketOrLimit);
        tradeMap.put("quantity", quantity);
        tradeMap.put("symbolUpdate", symbolUpdate);
        tradeMap.put("limitPrice", limitPrice);
//        tradeMap.put("quoteAmount", quoteAmount);

        return tradeMap;
    }

    public static Map<String, String> paramMap(String buyOrSell, String marketOrLimit, String baseSymbol,
                                               String quoteSymbol, String quoteAmount, String limitOrderPrice)
    {
        Map<String, String> tradeMap = new HashMap<>();
        tradeMap.put("buyOrSell", buyOrSell);
        tradeMap.put("marketOrLimit", marketOrLimit);
        tradeMap.put("baseSymbol", baseSymbol);
        tradeMap.put("quoteSymbol", quoteSymbol);
        tradeMap.put("quoteAmount", quoteAmount);
        tradeMap.put("limitOrderPrice", limitOrderPrice);

        return tradeMap;
    }

    public static Map<String, Double> tradePairDetails(String headUsdt, double headUsdtPrice, String pairToUSDT,
                                                       double pairToUSDTPrice, String pairToHead, double pairToHeadPrice)
    {
        Map<String, Double> tradePairMap = new HashMap<>();
        tradePairMap.put(headUsdt, headUsdtPrice);
        tradePairMap.put(pairToUSDT, pairToUSDTPrice);
        tradePairMap.put(pairToHead, pairToHeadPrice);

        return tradePairMap;
    }

}
