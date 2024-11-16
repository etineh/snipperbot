package com.trading.snipperBot.model.outgoing;

import java.math.BigDecimal;
import java.util.Map;

//public record OpportunityResultM(
//        String tradeID, BigDecimal Slippage, double profit, String message, Map<String, Double> pairDetailMap
//) {
//
//    public OpportunityResultM updateMessage(String newMessage) {
//        return new OpportunityResultM(this.tradeID, this.Slippage, this.profit, newMessage, this.pairDetailMap);
//    }
//
//}

public class OpportunityResultM {
    private String tradeID;
    private String slippage;
    private String profit;
    private String message;
    private Long time;
    private Map<String, Double> pairDetailMap;

    // No-argument constructor (required by Firebase)
    public OpportunityResultM() {}

    // Constructor with parameters
    public OpportunityResultM(String tradeID, String slippage, String profit, String message,
                              Long time, Map<String, Double> pairDetailMap) {
        this.tradeID = tradeID;
        this.slippage = slippage;
        this.profit = profit;
        this.message = message;
        this.time = time;
        this.pairDetailMap = pairDetailMap;
    }

    // Getters and setters for each field
    public String getTradeID() { return tradeID; }
    public void setTradeID(String tradeID) { this.tradeID = tradeID; }

    public String getSlippage() { return slippage; }
    public void setSlippage(String slippage) { this.slippage = slippage; }

    public String getProfit() { return profit; }
    public void setProfit(String profit) { this.profit = profit; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Map<String, Double> getPairDetailMap() { return pairDetailMap; }
    public void setPairDetailMap(Map<String, Double> pairDetailMap) { this.pairDetailMap = pairDetailMap; }

}
