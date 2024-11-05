package com.trading.snipperBot.utils;
import com.binance.connector.client.SpotClient;
import com.binance.connector.client.exceptions.BinanceClientException;
import com.binance.connector.client.impl.SpotClientImpl;
import com.trading.snipperBot.model.LiveMarketModel;
import com.trading.snipperBot.constant.k;
import org.json.JSONArray;
import org.json.JSONObject;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;


public class BinanceUtils {

    public static String generateSignature(String queryString, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec spec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(spec);
            byte[] hmacData = mac.doFinal(queryString.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hmacData); // Implement bytesToHex to convert byte[] to hex string
        } catch (Exception e){
            System.out.println("Error with signature: " + e.getMessage());
        }
        return null;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


    public static void saveAllPairLatestBalanceToMap() {

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("recvWindow", 50000);  // Set time
        SpotClient client = new SpotClientImpl(BinanceConfig.API_KEY, BinanceConfig.SECRET_KEY, BinanceConfig.BASE_URL);

        new Thread(()->{

            String result = client.createTrade().account(parameters);
            System.out.println("Wallet Balance Map done");

            // Parse the response to save balances to the map
            JSONObject jsonResponse = new JSONObject(result);
            JSONArray balances = jsonResponse.getJSONArray("balances");

            for (int i = 0; i < balances.length(); i++) {
                JSONObject balanceObj = balances.getJSONObject(i);
                String asset = balanceObj.getString("asset");
                String freeBalance = balanceObj.getString("free");

                // Store the free balance in the map
                k.tokenBalancesMap.put(asset, freeBalance);
            }

        }).start();

    }


    public static String checkOnePairBalance(String asset) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("recvWindow", 50000);  // Set time out

        SpotClient client = new SpotClientImpl(BinanceConfig.API_KEY, BinanceConfig.SECRET_KEY, BinanceConfig.BASE_URL);

        try {
            String result = client.createTrade().account(parameters);

            // Parse the response to find the balance of the specific asset
            JSONObject jsonResponse = new JSONObject(result);
            JSONArray balances = jsonResponse.getJSONArray("balances");
            for (int i = 0; i < balances.length(); i++) {
                JSONObject balanceObj = balances.getJSONObject(i);
                if (balanceObj.getString("asset").equalsIgnoreCase(asset)) {
                    String freeBalance = balanceObj.getString("free");
                    String lockedBalance = balanceObj.getString("locked");
                    System.out.println("Balance for " + asset + ": Free = " + freeBalance + ", Locked = " + lockedBalance);
                    return freeBalance; // Exit after finding the asset
                }
            }
            return "Asset " + asset + " not found in account.";

        } catch (BinanceClientException e) {
            System.err.println("Binance Client Exception: " + e.getMessage());
        }

        return "Asset " + asset + " not found in account.";
    }

    public static void getPrecisionData() {

        SpotClient client = new SpotClientImpl();
        Map<String, Object> parameters = new LinkedHashMap<>();

        // Fetch exchange information
        String result = client.createMarket().exchangeInfo(parameters);

        // Parse the result using org.json
        JSONObject jsonObject = new JSONObject(result);
        JSONArray symbols = jsonObject.getJSONArray("symbols");

        // Iterate through the symbols array to extract precision data
        for (int i = 0; i < symbols.length(); i++) {
            JSONObject symbolInfo = symbols.getJSONObject(i);
            String symbol = symbolInfo.getString("symbol");
            int pricePrecision = symbolInfo.getInt("quotePrecision");
            int quantityPrecision = symbolInfo.getInt("baseAssetPrecision");

            // Print the symbol with its price and quantity precision
            System.out.printf("Symbol: %s, Price Precision: %d, Quantity Precision: %d\n",
                    symbol, pricePrecision, quantityPrecision);
        }
    }

    // Load all symbols and their max decimals at startup
    public static void loadSymbolDecimals() {

        new Thread(()-> {
            SpotClient client = new SpotClientImpl(BinanceConfig.API_KEY, BinanceConfig.SECRET_KEY, BinanceConfig.BASE_URL);
            String result = client.createMarket().exchangeInfo(new LinkedHashMap<>());

            JSONObject jsonResponse = new JSONObject(result);
            JSONArray symbols = jsonResponse.getJSONArray("symbols");

            for (int i = 0; i < symbols.length(); i++) {
                JSONObject symbolInfo = symbols.getJSONObject(i);
                String symbol = symbolInfo.getString("symbol");
                JSONArray filters = symbolInfo.getJSONArray("filters");

                for (int j = 0; j < filters.length(); j++) {
                    JSONObject filter = filters.getJSONObject(j);
                    if (filter.getString("filterType").equals("LOT_SIZE")) {
                        BigDecimal stepSize = new BigDecimal(filter.getString("stepSize"));
                        int maxDecimals = stepSize.stripTrailingZeros().scale();
                        k.symbolDecimalsMap.put(symbol, maxDecimals);
                        break;
                    }
                }
            }
//            System.out.println("Symbol decimals map loaded: " + k.symbolDecimalsMap);
        }).start();

    }


    public static String adjustToAllowedDecimals(String quantity, String symbol) {
        int getMaxDecimal = k.symbolDecimalsMap.getOrDefault(symbol, 0); // return 0 if symbol not found
        BigDecimal fullBalance = new BigDecimal(quantity);
        return fullBalance.setScale(getMaxDecimal, RoundingMode.DOWN).toPlainString();
    }

    public static void tradeFee() {
        // Assuming you're using the Binance Java SDK
        Map<String, Object> parameters = new LinkedHashMap<>();
//        parameters.put("symbol", "BTCUSDT"); // Use your desired trading pair

        SpotClient client = new SpotClientImpl(BinanceConfig.API_KEY, BinanceConfig.SECRET_KEY, BinanceConfig.BASE_URL);

        try {
            // Retrieve trade fee information for the specified symbol
            String result = client.createWallet().tradeFee(parameters);
            System.out.println("Trade Fee Info: " + result);
        } catch (BinanceClientException e) {
            System.err.println("Error fetching trade fee: " + e.getMessage());
        }

    }

    public static BigDecimal calculateSlippagePercentage(String symbol) {

        if( getLatestData(symbol) == null) return new BigDecimal("2024");

        // Get the current market prices
        BigDecimal currentBidPrice = new BigDecimal(getLatestData(symbol).getBestBidPrice()); // For buy orders
        BigDecimal currentAskPrice = new BigDecimal(getLatestData(symbol).getBestAskPrice()); // For sell orders
        BigDecimal expectedExecutionPrice = new BigDecimal(getLatestData(symbol).getLastPrice());

        //  Calculate the absolute difference
        BigDecimal bidDifference = expectedExecutionPrice.subtract(currentBidPrice).abs();
        BigDecimal askDifference = expectedExecutionPrice.subtract(currentAskPrice).abs();

        // Choose the price that has the highest difference
        BigDecimal minBidOrAskPrice = (bidDifference.compareTo(askDifference) >= 0) ? currentBidPrice : currentAskPrice;

//        System.out.println("Current Bid Price: " + currentBidPrice + "============");
//        System.out.println("Current Ask Price: " + currentAskPrice);
////        System.out.println("Min Bid or Ask Price chosen: " + minBidOrAskPrice);
//        System.out.println("Expected Execution Price: " + expectedExecutionPrice);

        // Calculate slippage
        BigDecimal slippage = expectedExecutionPrice.subtract(minBidOrAskPrice)  // minBidOrAskPrice
                .divide(minBidOrAskPrice, 8, RoundingMode.DOWN)
                .multiply(BigDecimal.valueOf(100))
                .abs(); // Ensures slippage is positive

        // Add the trading fee of 0.1% to the slippage
        BigDecimal tradingFee = BigDecimal.valueOf(0.1); // 0.1%
        slippage = slippage.add(tradingFee);

        System.out.println("Slippage with trading fee: (" + symbol + ") : " + slippage + " %");
        return slippage; // This will give you the slippage percentage including the fee
    }



    public static LiveMarketModel getLatestData(String symbol) {
        return k.latestMarketData.get(symbol);
    }

    private void checkBalanceOnPostMan() {
        long timestamp = System.currentTimeMillis(); // Current timestamp
        long recvWindow = 50000; // Optional, adjust as needed

        String queryString = "timestamp=" + timestamp + "&recvWindow=" + recvWindow;
        String signature = BinanceUtils.generateSignature(queryString, BinanceConfig.SECRET_KEY);

        System.out.println("Timestamp: " + timestamp);
        System.out.println("Signature: " + signature);

    }


}
