package com.trading.snipperBot.model;

public record LiveMarketModel(
        String e,  // Event type
        long E,    // Event time
        String s,  // Symbol
        String p,  // Price change
        String P,  // Price change percentage
        String w,  // Weighted average price
        String x,  // Last price on the previous day
        String c,  // Last price
        String Q,  // Last quantity
        String b,  // Best bid price
        String B,  // Best bid quantity
        String a,  // Best ask price
        String A,  // Best ask quantity
        String o,  // Open price
        String h,  // High price
        String l,  // Low price
        String v,  // Total traded base asset volume
        String q,  // Total traded quote asset volume
        long O,    // Statistics open time
        long C,    // Statistics close time
        long F,    // First trade ID
        long L,    // Last trade ID
        int n      // Number of trades
) {
    // Getter for Event type
    public String getEventType() {
        return e;
    }

    // Getter for Event time
    public long getEventTime() {
        return E;
    }

    // Getter for Symbol
    public String getSymbol() {
        return s;
    }

    // Getter for Price change
    public String getPriceChange() {
        return p;
    }

    // Getter for Price change percentage
    public String getPriceChangePercentage() {
        return P;
    }

    // Getter for Weighted average price
    public String getWeightedAveragePrice() {
        return w;
    }

    // Getter for Last price on the previous day
    public String getLastPricePreviousDay() {
        return x;
    }

    // Getter for Last price
    public String getLastPrice() {
        return c;
    }

    // Getter for Last quantity
    public String getLastQuantity() {
        return Q;
    }

    // Getter for Best bid price
    public String getBestBidPrice() {
        return b;
    }

    // Getter for Best bid quantity
    public String getBestBidQuantity() {
        return B;
    }

    // Getter for Best ask price
    public String getBestAskPrice() {
        return a;
    }

    // Getter for Best ask quantity
    public String getBestAskQuantity() {
        return A;
    }

    // Getter for Open price
    public String getOpenPrice() {
        return o;
    }

    // Getter for High price
    public String getHighPrice() {
        return h;
    }

    // Getter for Low price
    public String getLowPrice() {
        return l;
    }

    // Getter for Total traded base asset volume
    public String getTotalTradedBaseAssetVolume() {
        return v;
    }

    // Getter for Total traded quote asset volume
    public String getTotalTradedQuoteAssetVolume() {
        return q;
    }

    // Getter for Statistics open time
    public long getStatisticsOpenTime() {
        return O;
    }

    // Getter for Statistics close time
    public long getStatisticsCloseTime() {
        return C;
    }

    // Getter for First trade ID
    public long getFirstTradeID() {
        return F;
    }

    // Getter for Last trade ID
    public long getLastTradeID() {
        return L;
    }

    // Getter for Number of trades
    public int getNumberOfTrades() {
        return n;
    }
}
