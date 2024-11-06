package com.trading.snipperBot.dao.impl;

import com.binance.connector.client.SpotClient;
import com.binance.connector.client.WebSocketStreamClient;
import com.binance.connector.client.exceptions.BinanceClientException;
import com.binance.connector.client.impl.SpotClientImpl;
import com.binance.connector.client.impl.WebSocketStreamClientImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.trading.snipperBot.dao.BinanceDoa;
import com.trading.snipperBot.model.LiveMarketModel;
import com.trading.snipperBot.model.OrderResponseModel;
import com.trading.snipperBot.model.outgoing.OpportunityResultM;
import com.trading.snipperBot.utils.BinanceConfig;
import com.trading.snipperBot.utils.BinanceUtils;
import com.trading.snipperBot.utils.TradeMapUtils;
import com.trading.snipperBot.constant.k;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.trading.snipperBot.utils.TradeMapUtils.tradeParamMap;

@Repository
public class BinanceDaoImpl implements BinanceDoa {

    @Value("${binance.api.key}")
    public String apiKey;

    @Value("${binance.secret.key}")
    public String secretKey;

    WebSocketStreamClient wsStreamClient; // defaults to live exchange unless stated.

    private boolean isConnected;

    @Override
    public void restartLiveMarketData() {
        isConnected = true;
    }

    @Override
    public void closeLiveMarketConnection() {

        BinanceUtils.calculateSlippagePercentage("HBARBTC");

//        wsStreamClient.closeAllConnections();
        isConnected = false;
    }

    @Override
    public void liveMarketData() {

        startBinanceWebSocket();

//        checkAllPairBalance();


//        checkBalanceOnPostMan();

//        wsStreamClient.aggTradeStream("DOGEUSDT", s -> System.out.println("what is+ " + s));
    }

    @Override
    public void startTrade() {


        String buyOrSell = k.SELL;
        String baseSymbol = "BTC";
        String quoteSymbol = k.USDT;
//        String quoteAmount = k.tokenBalancesMap.get(quoteSymbol); // the amount you want to buy with 'e.g' 100usdt
        String quoteAmount = "100"; // the amount you want to buy with 'e.g' 100usdt
        String limitOrderPrice = "0";
        String marketOrLimit = k.MARKET;

        Map<String, String> paramMap = TradeMapUtils.paramMap(buyOrSell, marketOrLimit, baseSymbol, quoteSymbol,
                quoteAmount, limitOrderPrice);

        prepareTradeParam(paramMap, k.BUY, "OGN", "BTC", k.USDT, 3);

//        for (String baseSymbol : k.tokenBalancesMap.keySet())// loop throught each key which is the symbol and set it as the baseSymbol
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
        if(buyOrSell.equals(k.BUY)) {
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
            tokenQuantity = k.tokenBalancesMap.get(baseSymbol) != null ? k.tokenBalancesMap.get(baseSymbol)
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
        System.out.println("BTC Previous Balance: " + BinanceUtils.checkOnePairBalance("BTC"));
        System.out.println("what is the quantity: " + createTradeMap.get("quantity"));

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("symbol", createTradeMap.get("symbol"));
        parameters.put("side", createTradeMap.get("buyOrSell"));
        parameters.put("type", createTradeMap.get("marketOrLimit"));
        parameters.put("quantity", createTradeMap.get("quantity"));
        parameters.put("recvWindow", 10000);  // Set a larger recvWindow
        if(createTradeMap.get("marketOrLimit").equalsIgnoreCase(k.LIMIT)) {
            parameters.put("price", createTradeMap.get("limitPrice"));
            parameters.put("timeInForce", "GTC");
        }

        SpotClient client = new SpotClientImpl(BinanceConfig.API_KEY, BinanceConfig.SECRET_KEY, BinanceConfig.BASE_URL);

        try {
            String result = client.createTrade().newOrder(parameters);

            if(result != null) {

                System.out.println("Order response:  " + result);

                Gson gson = new Gson();
                OrderResponseModel responseModel = gson.fromJson(result, OrderResponseModel.class);

                // Now you can access the details using accessor methods
                System.out.println("Order Status: " + responseModel.status());
                // Check fills and get the fill prices
                for (OrderResponseModel.Fill fill : responseModel.fills()) {
                    System.out.println("Fill price: " + fill.price());
                }
                System.out.println("cummulativeQuoteQty: " + responseModel.cummulativeQuoteQty());
                System.out.println("currect price: " + BinanceUtils.getLatestData(createTradeMap.get("symbol"))
                        .getLastPrice() + " ============ ============ ============");

                String newBalance = BinanceUtils.checkOnePairBalance(createTradeMap.get("symbolUpdate"));
                if(newBalance != null) {
                    // update the latest balance
                    k.tokenBalancesMap.put(createTradeMap.get("symbolUpdate"), newBalance);

                    round ++;
                    if(round > 3){
                        System.out.println("Trade has ended!");
                        return;
                    }

//                    String type = round == 3 ? k.LIMIT : k.MARKET;
                    String type = k.MARKET;

                    // call the next trade
                    if(nextTrade.equals(k.BUY)) {
                        String headAvailableAmount = k.tokenBalancesMap.get(nextQuoteS);
                        Map<String, String> paramMap = TradeMapUtils.paramMap(k.BUY, type, nextBaseS, nextQuoteS,
                                headAvailableAmount, createTradeMap.get("limitPrice"));

                        prepareTradeParam(paramMap, k.SELL, nextBaseS, stableCoin, null, round);

                    } else if(nextTrade.equals(k.SELL)) {
                        Map<String, String> paramMap = TradeMapUtils.paramMap(k.SELL, type, nextBaseS, nextQuoteS,
                                null, createTradeMap.get("limitPrice"));

                        prepareTradeParam(paramMap, k.SELL, nextQuoteS, stableCoin, null, round);
                    }

                }

            }

        } catch (Exception e) {
            System.err.println("Error on trade: " + e.getMessage() + ": The details are: " + parameters);

            Map<String, String> paramMap = TradeMapUtils.paramMap(k.SELL, k.MARKET, nextQuoteS, stableCoin,
                    null, createTradeMap.get("limitPrice"));

            prepareTradeParam(paramMap, k.SELL, nextQuoteS, stableCoin, null, 3);

        }

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
                if (symbol.endsWith(k.BTC)) pairToBtcMap.put(symbol, event);
                if (symbol.endsWith(k.BNB)) pairToBnbMap.put(symbol, event);
                if (symbol.endsWith(k.ETH)) pairToEthMap.put(symbol, event);

                // Add all pairs to latestMarketData
                k.latestMarketData.put(symbol, event);

                // Check for BTC, BNB, and ETH pair opportunities
                if (isConnected){
                    if (checkForBtcPairOpportunities(event, pairToBtcMap)) break;
                    if (checkForBnbPairOpportunities(event, pairToBnbMap)) break;
                    if (checkForEthPairOpportunities(event, pairToEthMap)) break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkForEthPairOpportunities(LiveMarketModel event, Map<String, LiveMarketModel> pairToEthMap)
    {
        String symbol = event.getSymbol();
        String baseSymbol = symbol.replace(k.USDT, "").replace(k.ETH, "");

        if(symbol.equals(k.ETHUSDT)) k.latestMarketData.put(symbol, event);

        LiveMarketModel ethUsdtModel = BinanceUtils.getLatestData(k.ETHUSDT);

        // Check for pairs ending with USDT and if the corresponding SOL pair exists
        if (ethUsdtModel != null && symbol.endsWith(k.USDT) && pairToEthMap.containsKey(baseSymbol + k.ETH))
        {
            String pairToEthSymbol = baseSymbol + k.ETH;
            LiveMarketModel pairToEthModel = pairToEthMap.get(pairToEthSymbol);

            if ( Double.parseDouble(pairToEthModel.getTotalTradedQuoteAssetVolume()) > 10 ) {  // Minimum ETH volume
                k.latestMarketData.put(symbol, event);  // Add the USDT pair
                k.latestMarketData.put(pairToEthSymbol, pairToEthModel);  // Also add the BTC pair

                double ethUsdtPrice = Double.parseDouble(ethUsdtModel.getLastPrice());
                double pairToUsdtPrice = Double.parseDouble(event.getLastPrice());
                double pairToEthPrice = Double.parseDouble(pairToEthModel.getLastPrice());

                System.out.println("number of trade pairToEth: " + pairToEthModel.getNumberOfTrades());

                if ( checkForArbitrageOpportunity(k.ETHUSDT, symbol, pairToEthSymbol,   // symbol = pairToUsdt
                        ethUsdtPrice, pairToUsdtPrice, pairToEthPrice, -200, 200) )
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
        String baseSymbol = symbol.replace(k.USDT, "").replace(k.BTC, "");

        if(symbol.equals(k.BTCUSDT)) k.latestMarketData.put(symbol, event);

        LiveMarketModel btcUsdtEvent = BinanceUtils.getLatestData(k.BTCUSDT);

        // Check for pairs ending with USDT and if the corresponding BTC pair exists
        if (btcUsdtEvent != null && symbol.endsWith(k.USDT) && pairToBtcMap.containsKey(baseSymbol + k.BTC))
        {
            String pairToBtcSymbol = baseSymbol + k.BTC;
            LiveMarketModel pairToBtcEvent = pairToBtcMap.get(pairToBtcSymbol);
            if ( Double.parseDouble(pairToBtcEvent.getTotalTradedQuoteAssetVolume()) > 5 ) {    // 7BTC
                k.latestMarketData.put(symbol, event);  // Add the USDT pair
                k.latestMarketData.put(pairToBtcSymbol, pairToBtcEvent);  // Also add the BTC pair

                double btcUsdtPrice = Double.parseDouble(btcUsdtEvent.getLastPrice());
                double pairToUsdtPrice = Double.parseDouble(event.getLastPrice());
                double pairToBtcPrice = Double.parseDouble(pairToBtcEvent.getLastPrice());

                System.out.println("number of trade pairToBtc: " + pairToBtcEvent.getNumberOfTrades() + " ============");

                if ( checkForArbitrageOpportunity(k.BTCUSDT, symbol, pairToBtcSymbol,   // symbol = pairToUsdt
                        btcUsdtPrice, pairToUsdtPrice, pairToBtcPrice, -500, 500) )
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
        String baseSymbol = symbol.replace(k.USDT, "").replace(k.BNB, "");

        if(symbol.equals(k.BNBUSDT)) k.latestMarketData.put(symbol, event);

        LiveMarketModel bnbUsdtModel = BinanceUtils.getLatestData(k.BNBUSDT);
        if (bnbUsdtModel == null) return false;

        // Check for pairs ending with USDT and if the corresponding SOL pair exists
        if (!symbol.endsWith(k.USDT) || !pairToBnbMap.containsKey(baseSymbol + k.BNB)) return false;

        String pairToBnbSymbol = baseSymbol + k.BNB;
        LiveMarketModel pairToBnbModel = pairToBnbMap.get(pairToBnbSymbol);

        // Minimum SOL volume
        if ( Double.parseDouble(pairToBnbModel.getTotalTradedQuoteAssetVolume()) < 100 ) return false;

        k.latestMarketData.put(symbol, event);  // Add the USDT pair
        k.latestMarketData.put(pairToBnbSymbol, pairToBnbModel);  // Also add the BTC pair

        double bnbUsdtPrice = Double.parseDouble(bnbUsdtModel.getLastPrice());
        double pairToUsdtPrice = Double.parseDouble(event.getLastPrice());
        double pairToBnbPrice = Double.parseDouble(pairToBnbModel.getLastPrice());

        System.out.println("number of trader on pairToBnb: " + pairToBnbModel.getNumberOfTrades());
        boolean isOpportunityDetected = checkForArbitrageOpportunity(k.BNBUSDT, symbol, pairToBnbSymbol,  // symbol = pairToUsdt
                bnbUsdtPrice, pairToUsdtPrice, pairToBnbPrice, -200, 200);

        if ( isOpportunityDetected ) {
            isConnected = false;
            return true;
        }

        return false;

    }

    private boolean checkForArbitrageOpportunity(String headUsdt, String pairToUSDT, String pairToHead,  // SOL, BTC, BNB
                                                 double headUsdtPrice, double pairToUSDTPrice, double pairToHeadPrice,
                                                 int minusTarget, int plusTarget)
    {   // head = btc, bnb, eth etc
        if(pairToHead.equalsIgnoreCase("wbtc")) return false;

        System.out.printf("%s Price: %.7f, %s Price: %.10f, %s Price: %.10f\n",
                headUsdt, headUsdtPrice, pairToUSDT, pairToUSDTPrice, pairToHead, pairToHeadPrice);

        double potentialUsdtValueFromBtc = (1 / pairToHeadPrice * k.BID_FEE) * pairToUSDTPrice * k.ASK_FEE;
        double potentialProfit = potentialUsdtValueFromBtc - (headUsdtPrice * k.ASK_FEE);

        BigDecimal realistProfitBigDecimal = BigDecimal.valueOf(Math.abs(potentialProfit / 1000.00));
        BigDecimal slippagePercentage = BinanceUtils.calculateSlippagePercentage(pairToHead);

        if(pairToHead.equalsIgnoreCase("VETBTC") && potentialProfit/1000 > 2.2) {   // remove later

            if (potentialProfit <= minusTarget  || potentialProfit >= plusTarget)   // profit detected!
            {
                System.out.println(potentialProfit +": Arbitrage Opportunity Found! Profit: " + potentialProfit/1000.00 + "%");

                String stableCoin = k.USDT;    // change Later to USDC, USDT, TRY
                String altPair = pairToUSDT.split(stableCoin)[0];
                String headPair = headUsdt.split(stableCoin)[0];

                // prepare trade execution
                Map<String, String> paramMap = TradeMapUtils.paramMap(k.BUY, k.MARKET, altPair, stableCoin,
                        "100", String.valueOf(headUsdtPrice));

                // Prepare the trade details for user output
                Map<String, Double> pairDetailMap = TradeMapUtils.tradePairDetails(headUsdt, headUsdtPrice,
                        pairToUSDT, pairToUSDTPrice, pairToHead, pairToHeadPrice);
                OpportunityResultM resultM = new OpportunityResultM(
                        slippagePercentage, potentialProfit / 1000.00, k.slippageOverProfitMsg, pairDetailMap
                );

                // Check if realistProfit is at least 3 times greater than slippagePercentage
//            if (realistProfitBigDecimal.compareTo(slippagePercentage.multiply(BigDecimal.valueOf(3))) > 0)
//            {
//                if (potentialProfit > 0) {  // it is positive profit, so buy the pair first, nextTrade should be SELL
//                    System.out.println(k.profitOverSlippagePositiveMsg(pairToUSDT));
//                    resultM.updateMessage(k.profitOverSlippagePositiveMsg(pairToUSDT));
//
//                    prepareTradeParam(paramMap, k.SELL, altPair, headPair, k.USDT, 1);
//
//                } else {    // it is negative profit, so buy the head first, nextTrade should be BUY 'e.g' LITBTC
                System.out.println(k.profitOverSlippageNegativeMsg(headUsdt));
                resultM.updateMessage(k.profitOverSlippagePositiveMsg(headUsdt));

                paramMap.put("baseSymbol", headPair);
                paramMap.put("limitOrderPrice", String.valueOf(pairToUSDTPrice));

                prepareTradeParam(paramMap, k.BUY, altPair, headPair, k.USDT,1);

//                }
//                return true;
//            }

//            System.out.println(k.slippageOverProfitMsg);    // send to database and resultM

                return true; // remove later
            }
        }

        System.out.println(potentialProfit + " No Profit: " + potentialProfit/1000.00 + "%");
        // update time regularly

        return false;
    }

    // Start listening
    public void startBinanceWebSocket() {
//        int streamID = wsStreamClient.allMiniTickerStream(this::handleLiveData);
        wsStreamClient = new WebSocketStreamClientImpl();
        wsStreamClient.allTickerStream(this::handleLiveData);
        isConnected = true;
    }


}
