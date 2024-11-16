package com.trading.snipperBot.constant;

import com.trading.snipperBot.model.LiveMarketModel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class K {

    public static String BTCUSDT = "BTCUSDT";
    public static String SOLUSDT = "SOLUSDT";
    public static String BNBUSDT = "BNBUSDT";
    public static String ETHUSDT = "ETHUSDT";
    public static String USDT = "USDT";
    public static String BTC = "BTC";
    public static String SOL = "SOL";
    public static String BNB = "BNB";
    public static String ETH = "ETH";

    public static String BUY = "BUY";
    public static String SELL = "SELL";
    public static String LIMIT = "LIMIT";
    public static String MARKET = "MARKET";

    public static final double BID_FEE = 0.999; // 0.1% fee per bid transaction (1 - 0.001)
    public static final double ASK_FEE = 0.999; // 0.1% fee per ask transaction (1 - 0.001)

    public static final Map<String, Integer> symbolDecimalsMap = new HashMap<>();
    public static Map<String, String> tokenBalancesMap = new HashMap<>(); // To store token balances

    // Define a map to hold the latest state of each symbol
    public static final Map<String, LiveMarketModel> latestMarketData = new ConcurrentHashMap<>();

    public static final String slippageOverProfitMsg = "Note: Cannot enter trade - The slippage is higher than the realist profit.";
//    public static final String profitOverSlippageMinusMsg = "Cannot enter trade: The slippage is higher than the realist profit.";
    public static String profitOverSlippagePositiveMsg(String pairToUsdt) {
        return "Note: Profit on positive - so entering straight trade by: buying " + pairToUsdt + " first";
    }
    public static String profitOverSlippageNegativeMsg(String headUsdt) {
        return "Note: Profit on negative - so reversing trade by: buying " + headUsdt + " first";
    }


    //    public static final String slippageOverProfitMsg = "Cannot enter trade: The slippage is higher than the realist profit.";

}
