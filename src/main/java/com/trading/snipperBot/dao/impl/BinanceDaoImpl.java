package com.trading.snipperBot.dao.impl;

import com.binance.connector.client.SpotClient;
import com.binance.connector.client.WebSocketStreamClient;
import com.binance.connector.client.impl.SpotClientImpl;
import com.binance.connector.client.impl.WebSocketStreamClientImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.trading.snipperBot.dao.BinanceDoa;
import com.trading.snipperBot.model.LiveMarketModel;
import com.trading.snipperBot.model.OrderResponseModel;
import com.trading.snipperBot.model.incoming.PlaceTradeM;
import com.trading.snipperBot.model.outgoing.OpportunityResultM;
import com.trading.snipperBot.utils.*;
import com.trading.snipperBot.constant.K;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.trading.snipperBot.utils.TradeMapUtils.tradeParamMap;

@Repository
public class BinanceDaoImpl implements BinanceDoa {

//    @Value("${binance.api.key}")
//    public String apiKey;
//
//    @Value("${binance.secret.key}")
//    public String secretKey;

    WebSocketStreamClient wsStreamClient; // defaults to live exchange unless stated.

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private long lastWebSocketActive = System.currentTimeMillis(); // Last active time in milliseconds

    private boolean isConnected;

    @Override
    public void restartLiveMarketData() {
        isConnected = true;
    }

    @Override
    public void closeLiveMarketConnection() {
//        wsStreamClient.closeAllConnections();
        isConnected = false;
    }


    @Override
    public void liveMarketData() {

        startBinanceWebSocket();

        scheduler.scheduleAtFixedRate(
                this::checkWebSocketConnect4min,  // The task you want to execute
                0,                  // Initial delay
                4,                  // Period (in minutes)
                TimeUnit.MINUTES    // Time unit
        );

    }


    private void checkWebSocketConnect4min() {
        long currentTime = System.currentTimeMillis(); // Get the current time
        long timeDifference = currentTime - lastWebSocketActive; // Calculate the difference

        // Check if the time difference is greater than or equal to 2 minutes (120,000 milliseconds)
        if (timeDifference >= 2 * 60 * 1000) {
            startBinanceWebSocket();
            System.out.println("WebSocket_ is reconnected");
        }

        System.out.println("Checking connection");

        DatabaseReference lastSocketConnectRef = FirebaseDatabase.getInstance().getReference("lastWebsocketConnect");
        lastSocketConnectRef.child("lastTime").setValueAsync(lastWebSocketActive);

    }


    @Override
    public void startTrade(PlaceTradeM placeTradeM) {

        String buyOrSell = K.SELL;
        String baseSymbol = placeTradeM.paramMap().get("baseSymbol");
        String quoteSymbol = placeTradeM.paramMap().get("quoteSymbol");
        String quoteAmount = placeTradeM.paramMap().get("quoteAmount");
//        String quoteAmount = "100"; // the amount you want to buy with 'e.g' 100usdt
        String limitOrderPrice = "0";
        String marketOrLimit = placeTradeM.paramMap().get("marketOrLimit");;
        int round = placeTradeM.round();

        Map<String, String> paramMap = TradeMapUtils.paramMap(buyOrSell, marketOrLimit, baseSymbol, quoteSymbol,
                quoteAmount, limitOrderPrice);

        prepareTradeParam(paramMap, null, null, null, null, round);

//        for (String baseSymbol : k.tokenBalancesMap.keySet())// loop through each key which is the symbol and set it as the baseSymbol
//        {}

    }

    private void prepareTradeParam(Map<String, String> paramMap, String nextTrade, String nextBaseS,
                                   String nextQuoteS, String stableCoin, int round)
    {
        String baseSymbol = paramMap.get("baseSymbol");
        String quoteSymbol = paramMap.get("quoteSymbol");
        String buyOrSell = paramMap.get("buyOrSell");
        String marketOrLimit = paramMap.get("marketOrLimit");
        String limitOrderPrice = paramMap.get("limitOrderPrice");
        String tokenQuantity;
        String symbol = baseSymbol.toUpperCase() + quoteSymbol.toUpperCase();

        BigDecimal slippagePercentage = BinanceUtils.calculateSlippagePercentage(symbol);
        if(slippagePercentage.compareTo(BigDecimal.valueOf(2024)) == 0) {
            System.out.println("slippage value not yet detected");
            return;
        }

        String updateSymbolBal;
        if(buyOrSell.equals(K.BUY)) {
            BigDecimal availableQuoteAmount = new BigDecimal(paramMap.get("quoteAmount"));
            BigDecimal currentTokenPrice = new BigDecimal( BinanceUtils.getLatestData(symbol).getLastPrice() );

            // Adjust for slippage by reducing the quantity slightly
            BigDecimal initialQuantity = availableQuoteAmount.divide(currentTokenPrice, 8, RoundingMode.DOWN);
            BigDecimal slippageAdjustment = initialQuantity.multiply(slippagePercentage.divide(BigDecimal.valueOf(100), 8, RoundingMode.DOWN));
            tokenQuantity = initialQuantity.subtract(slippageAdjustment).toPlainString();

            updateSymbolBal = baseSymbol;

            // Check for negative or zero token quantity
            if (new BigDecimal(tokenQuantity).compareTo(BigDecimal.ZERO) <= 0) {
                System.out.println("Calculated token quantity is zero or negative. Trade will not proceed.");
                return; // or handle this situation as needed
            }

        } else {    // it is SELL   -- get the base token balance e.g BTC (BTCUSDT)
            tokenQuantity = K.tokenBalancesMap.get(baseSymbol) != null ? K.tokenBalancesMap.get(baseSymbol)
                    : BinanceUtils.checkOnePairBalance(baseSymbol);

            updateSymbolBal = quoteSymbol;
        }

        String decimalQuantity = BinanceUtils.adjustToAllowedDecimals(tokenQuantity, symbol);

        Map<String, String> createTradeMap = tradeParamMap(symbol, buyOrSell, marketOrLimit, decimalQuantity,
                updateSymbolBal, limitOrderPrice);

        createTrade(createTradeMap, nextTrade, nextBaseS, nextQuoteS, stableCoin, round);

    }

    private void createTrade(Map<String, String> createTradeMap, String nextTrade, String nextBaseS,
                             String nextQuoteS, String stableCoin, int round)
    {
//        System.out.println("BTC Previous Balance: " + BinanceUtils.checkOnePairBalance("BTC"));
//        System.out.println("what is the quantity: " + createTradeMap.get("quantity"));

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", createTradeMap.get("symbol"));
        parameters.put("side", createTradeMap.get("buyOrSell"));
        parameters.put("type", createTradeMap.get("marketOrLimit"));
        parameters.put("quantity", createTradeMap.get("quantity"));
        parameters.put("recvWindow", 10000);  // Set a larger recvWindow
        if(createTradeMap.get("marketOrLimit").equalsIgnoreCase(K.LIMIT)) {
            parameters.put("price", createTradeMap.get("limitPrice"));
            parameters.put("timeInForce", "GTC");
        }

        SpotClient client = new SpotClientImpl(BinanceConfig.API_KEY, BinanceConfig.SECRET_KEY, BinanceConfig.BASE_URL);

        try {
            String result = client.createTrade().newOrder(parameters);

            if(result != null) {

//                Gson gson = new Gson();
//                OrderResponseModel responseModel = gson.fromJson(result, OrderResponseModel.class);

                // Now you can access the details using accessor methods
//                System.out.println("Order Status: " + responseModel.status());
                // Check fills and get the fill prices
//                for (OrderResponseModel.Fill fill : responseModel.fills()) {
//                    System.out.println("Fill price: " + fill.price());
//                }
//                System.out.println("cummulativeQuoteQty: " + responseModel.cummulativeQuoteQty());
//                System.out.println("currect price: " + BinanceUtils.getLatestData(createTradeMap.get("symbol"))
//                        .getLastPrice() + " ============ ============ ============");

                String newBalance = BinanceUtils.checkOnePairBalance(createTradeMap.get("symbolUpdate"));
                if(newBalance != null) {
                    // update the latest balance
                    K.tokenBalancesMap.put(createTradeMap.get("symbolUpdate"), newBalance);

                    round ++;
                    if (round > 3) {
                        scheduler.schedule(this::performEndOfTradeActions, 3, TimeUnit.SECONDS); // Delay of 3 seconds
                        System.out.println("Trade has ended!");
                        return;
                    }

//                    String type = round == 3 ? k.LIMIT : k.MARKET;
                    String type = K.MARKET;

                    // call the next trade
                    if(nextTrade.equals(K.BUY)) {
                        String headAvailableAmount = K.tokenBalancesMap.get(nextQuoteS);
                        Map<String, String> paramMap = TradeMapUtils.paramMap(K.BUY, type, nextBaseS, nextQuoteS,
                                headAvailableAmount, createTradeMap.get("limitPrice"));

                        prepareTradeParam(paramMap, K.SELL, nextBaseS, stableCoin, null, round);

                    } else if(nextTrade.equals(K.SELL)) {
                        Map<String, String> paramMap = TradeMapUtils.paramMap(K.SELL, type, nextBaseS, nextQuoteS,
                                null, createTradeMap.get("limitPrice"));

                        prepareTradeParam(paramMap, K.SELL, nextQuoteS, stableCoin, null, round);
                    }

                }

            }

        } catch (Exception e) { // return back the token to usdt if error occur
            System.err.println("Error on trade: " + e.getMessage() + ": The details are: " + parameters);

            Map<String, String> paramMap = TradeMapUtils.paramMap(K.SELL, K.MARKET, nextQuoteS, stableCoin,
                    null, createTradeMap.get("limitPrice"));

            prepareTradeParam(paramMap, K.SELL, nextQuoteS, stableCoin, null, 3);

            // Prepare the trade details for user output
            OpportunityResultM resultM = new OpportunityResultM(
                    "null", "null", "null",
                    e.getMessage(), System.currentTimeMillis(), new HashMap<>()
            );

            sendToDatabase(resultM, "onError");

            scheduler.schedule(this::performEndOfTradeActions, 5, TimeUnit.SECONDS); // Delay of 5 seconds

        }

    }

    private void performEndOfTradeActions() {
        restartLiveMarketData(); // resume live socket detection
        BinanceUtils.saveAllPairLatestBalanceToMap();
        isConnected = true;
    }


    public void handleLiveData(String jsonResponse) {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, LiveMarketModel> pairToBtcMap = new HashMap<>();
        Map<String, LiveMarketModel> pairToBnbMap = new HashMap<>();
        Map<String, LiveMarketModel> pairToEthMap = new HashMap<>();

        try {
            // Parse the response to get a list of market events
            List<LiveMarketModel> marketEvents = objectMapper.readValue(jsonResponse, new TypeReference<>() {});

            for (LiveMarketModel event : marketEvents) {
                String symbol = event.getSymbol();

                // Store pairs ending with BTC, BNB, and ETH in separate maps
                if (symbol.endsWith(K.BTC)) pairToBtcMap.put(symbol, event);
                if (symbol.endsWith(K.BNB)) pairToBnbMap.put(symbol, event);
                if (symbol.endsWith(K.ETH)) pairToEthMap.put(symbol, event);

                lastWebSocketActive = System.currentTimeMillis();

                // Add all pairs to latestMarketData
                K.latestMarketData.put(symbol, event);

                // Check for BTC, BNB, and ETH pair opportunities
                if (isConnected){
                    if (checkForBtcPairOpportunities(event, pairToBtcMap)) break;
                    if (checkForBnbPairOpportunities(event, pairToBnbMap)) break;
                    if (checkForEthPairOpportunities(event, pairToEthMap)) break;
                }
            }

        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

    private boolean checkForEthPairOpportunities(LiveMarketModel event, Map<String, LiveMarketModel> pairToEthMap)
    {
        String symbol = event.getSymbol();
        String baseSymbol = symbol.replace(K.USDT, "").replace(K.ETH, "");

        if(symbol.equals(K.ETHUSDT)) K.latestMarketData.put(symbol, event);

        LiveMarketModel ethUsdtModel = BinanceUtils.getLatestData(K.ETHUSDT);

        // Check for pairs ending with USDT and if the corresponding SOL pair exists
        if (ethUsdtModel != null && symbol.endsWith(K.USDT) && pairToEthMap.containsKey(baseSymbol + K.ETH))
        {
            String pairToEthSymbol = baseSymbol + K.ETH;
            LiveMarketModel pairToEthModel = pairToEthMap.get(pairToEthSymbol);

            if ( Double.parseDouble(pairToEthModel.getTotalTradedQuoteAssetVolume()) > 10 ) {  // Minimum ETH volume
                K.latestMarketData.put(symbol, event);  // Add the USDT pair
                K.latestMarketData.put(pairToEthSymbol, pairToEthModel);  // Also add the BTC pair

                double ethUsdtPrice = Double.parseDouble(ethUsdtModel.getLastPrice());
                double pairToUsdtPrice = Double.parseDouble(event.getLastPrice());
                double pairToEthPrice = Double.parseDouble(pairToEthModel.getLastPrice());

//                System.out.println("number of trade pairToEth: " + pairToEthModel.getNumberOfTrades());
                String tradeId = RandomNumUtil.random4NumId(event.getFirstTradeID());

                if ( checkForArbitrageOpportunity(K.ETHUSDT, symbol, pairToEthSymbol,   // symbol = pairToUsdt
                        ethUsdtPrice, pairToUsdtPrice, pairToEthPrice, -200, 200, tradeId) )
                {
                    isConnected = false;
                    return true;
                }
            }
        }
        return false;

    }

    private boolean checkForBtcPairOpportunities(LiveMarketModel event, Map<String, LiveMarketModel> pairToBtcMap)
    {
        String symbol = event.getSymbol();
        String baseSymbol = symbol.replace(K.USDT, "").replace(K.BTC, "");

        if(symbol.equals(K.BTCUSDT)) K.latestMarketData.put(symbol, event);

        LiveMarketModel btcUsdtEvent = BinanceUtils.getLatestData(K.BTCUSDT);

        // Check for pairs ending with USDT and if the corresponding BTC pair exists
        if (btcUsdtEvent != null && symbol.endsWith(K.USDT) && pairToBtcMap.containsKey(baseSymbol + K.BTC))
        {
            String pairToBtcSymbol = baseSymbol + K.BTC;
            LiveMarketModel pairToBtcEvent = pairToBtcMap.get(pairToBtcSymbol);
            if ( Double.parseDouble(pairToBtcEvent.getTotalTradedQuoteAssetVolume()) > 10 ) {    // 7BTC
                K.latestMarketData.put(symbol, event);  // Add the USDT pair
                K.latestMarketData.put(pairToBtcSymbol, pairToBtcEvent);  // Also add the BTC pair

                double btcUsdtPrice = Double.parseDouble(btcUsdtEvent.getLastPrice());
                double pairToUsdtPrice = Double.parseDouble(event.getLastPrice());
                double pairToBtcPrice = Double.parseDouble(pairToBtcEvent.getLastPrice());

//                System.out.println("number of trade pairToBtc: " + pairToBtcEvent.getNumberOfTrades() + " ============");
                String tradeId = RandomNumUtil.random4NumId(event.getFirstTradeID());

                if ( checkForArbitrageOpportunity(K.BTCUSDT, symbol, pairToBtcSymbol,   // symbol = pairToUsdt
                        btcUsdtPrice, pairToUsdtPrice, pairToBtcPrice, -1000, 1000, tradeId) )
                {
                    isConnected = false;
                    return true;
                }
            }
        }
        return false;

    }

    private boolean checkForBnbPairOpportunities(LiveMarketModel event, Map<String, LiveMarketModel> pairToBnbMap)
    {
        String symbol = event.getSymbol();
        String baseSymbol = symbol.replace(K.USDT, "").replace(K.BNB, "");

        if(symbol.equals(K.BNBUSDT)) K.latestMarketData.put(symbol, event);

        LiveMarketModel bnbUsdtModel = BinanceUtils.getLatestData(K.BNBUSDT);
        if (bnbUsdtModel == null) return false;

        // Check for pairs ending with USDT and if the corresponding SOL pair exists
        if (!symbol.endsWith(K.USDT) || !pairToBnbMap.containsKey(baseSymbol + K.BNB)) return false;

        String pairToBnbSymbol = baseSymbol + K.BNB;
        LiveMarketModel pairToBnbModel = pairToBnbMap.get(pairToBnbSymbol);

        // Minimum SOL volume
        if ( Double.parseDouble(pairToBnbModel.getTotalTradedQuoteAssetVolume()) < 100 ) return false;

        K.latestMarketData.put(symbol, event);  // Add the USDT pair
        K.latestMarketData.put(pairToBnbSymbol, pairToBnbModel);  // Also add the BTC pair

        double bnbUsdtPrice = Double.parseDouble(bnbUsdtModel.getLastPrice());
        double pairToUsdtPrice = Double.parseDouble(event.getLastPrice());
        double pairToBnbPrice = Double.parseDouble(pairToBnbModel.getLastPrice());

//        System.out.println("number of trader on pairToBnb: " + pairToBnbModel.getNumberOfTrades());
        String tradeId = RandomNumUtil.random4NumId(event.getFirstTradeID());

        boolean isOpportunityDetected = checkForArbitrageOpportunity(K.BNBUSDT, symbol, pairToBnbSymbol,  // symbol = pairToUsdt
                bnbUsdtPrice, pairToUsdtPrice, pairToBnbPrice, -200, 200, tradeId);

        if ( isOpportunityDetected ) {
            isConnected = false;
            return true;
        }

        return false;

    }

    private boolean checkForArbitrageOpportunity(String headUsdt, String pairToUSDT, String pairToHead,  // SOL, BTC, BNB
                                                 double headUsdtPrice, double pairToUSDTPrice, double pairToHeadPrice,
                                                 int minusTarget, int plusTarget, String tradeId)
    {   // head = btc, bnb, eth etc
        if(pairToHead.equalsIgnoreCase("wbtc")) return false;

//        System.out.printf("%s Price: %.7f, %s Price: %.10f, %s Price: %.10f\n",
//                headUsdt, headUsdtPrice, pairToUSDT, pairToUSDTPrice, pairToHead, pairToHeadPrice);

        double potentialUsdtValueFromBtc = (1 / pairToHeadPrice * K.BID_FEE) * pairToUSDTPrice * K.ASK_FEE;
        double potentialProfit = potentialUsdtValueFromBtc - (headUsdtPrice * K.ASK_FEE);

        BigDecimal realistProfitBigDecimal = BigDecimal.valueOf(Math.abs(potentialProfit / 1000.00));
        BigDecimal slippagePercentage = BinanceUtils.calculateSlippagePercentage(pairToHead);

        if (potentialProfit <= minusTarget  || potentialProfit >= plusTarget)   // profit detected!
        {
//            System.out.println(potentialProfit +": Arbitrage Opportunity Found! Profit: " + potentialProfit/1000.00 + "%");

            String stableCoin = K.USDT;    // change Later to USDC, USDT, TRY
            String altPair = pairToUSDT.split(stableCoin)[0];
            String headPair = headUsdt.split(stableCoin)[0];

            // prepare trade execution
            Map<String, String> paramMap = TradeMapUtils.paramMap(K.BUY, K.MARKET, altPair, stableCoin,
                    "100", String.valueOf(headUsdtPrice));

            // Prepare the trade details for user output
            Map<String, Double> pairDetailMap = TradeMapUtils.tradePairDetails(headUsdt, headUsdtPrice,
                    pairToUSDT, pairToUSDTPrice, pairToHead, pairToHeadPrice);
            OpportunityResultM resultM = new OpportunityResultM(
                    tradeId, slippagePercentage.toPlainString(), realistProfitBigDecimal.toPlainString(),
                    K.slippageOverProfitMsg, System.currentTimeMillis(), pairDetailMap
            );

            // Check if realistProfit is at least 2 times greater than slippagePercentage
//            if(pairToHead.equalsIgnoreCase("VETBTC"))
            if (realistProfitBigDecimal.compareTo(slippagePercentage.multiply(BigDecimal.valueOf(2))) > 0)
            {
                if (potentialProfit > 0) {  // it is positive profit, so buy the pair first, nextTrade should be SELL
                    System.out.println(K.profitOverSlippagePositiveMsg(pairToUSDT));
                    resultM.setMessage(K.profitOverSlippagePositiveMsg(pairToUSDT));

                    prepareTradeParam(paramMap, K.SELL, altPair, headPair, K.USDT, 1);

                } else {    // it is negative profit, so buy the head first, nextTrade should be BUY 'e.g' LITBTC
                    System.out.println(K.profitOverSlippageNegativeMsg(headUsdt));
                    resultM.setMessage(K.profitOverSlippagePositiveMsg(headUsdt));

                    paramMap.put("baseSymbol", headPair);
                    paramMap.put("limitOrderPrice", String.valueOf(pairToUSDTPrice));

                    prepareTradeParam(paramMap, K.BUY, altPair, headPair, K.USDT,1);

                }
                sendToDatabase(resultM, "onTrade");
                return true;
            }

//            System.out.println(K.slippageOverProfitMsg);    // send to database and resultM
            sendToDatabase(resultM, "onDetect");
            return false;
        }

//        System.out.println(potentialProfit + " No Profit: " + potentialProfit/1000.00 + "%");
        return false;
    }

    private void sendToDatabase(OpportunityResultM resultM, String path) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDateWithDay = sdf.format(new Date());  // This will return the current date in the format "yyyy-MM-dd"

        String tradeTime = String.valueOf(System.currentTimeMillis());

        DatabaseReference tradeDetectRef = FirebaseDatabase.getInstance().getReference("tradeDetect");
        tradeDetectRef.child(todayDateWithDay).child(path).child(tradeTime).setValueAsync(resultM);

    }

    // Start listening
    public void startBinanceWebSocket() {
//        int streamID = wsStreamClient.allMiniTickerStream(this::handleLiveData);
        wsStreamClient = new WebSocketStreamClientImpl();
        wsStreamClient.allTickerStream(this::handleLiveData);
        isConnected = true;
        lastWebSocketActive = System.currentTimeMillis();
    }


}
