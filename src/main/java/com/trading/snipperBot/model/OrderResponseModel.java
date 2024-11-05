package com.trading.snipperBot.model;

import java.util.List;

public record OrderResponseModel(
        String symbol,
        long orderId,
        long orderListId,
        String clientOrderId,
        long transactTime,
        String price,
        String origQty,
        String executedQty,
        String cummulativeQuoteQty,
        String status,
        String timeInForce,
        String type,
        String side,
        long workingTime,
        List<Fill> fills, // List of Fill records for detailed fill info
        String selfTradePreventionMode
) {
    // Inner record for Fill details
    public record Fill(
            String price,
            String qty,
            String commission,
            String commissionAsset,
            long tradeId
    ) {}
}

