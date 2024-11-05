package com.trading.snipperBot.model.outgoing;

import java.math.BigDecimal;
import java.util.Map;

public record OpportunityResultM(BigDecimal Slippage, double profit, String message, Map<String, Double> pairDetailMap) {

    public OpportunityResultM updateMessage(String newMessage) {
        return new OpportunityResultM(this.Slippage, this.profit, newMessage, this.pairDetailMap);
    }

}
