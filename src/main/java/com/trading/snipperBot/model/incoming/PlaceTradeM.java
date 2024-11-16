package com.trading.snipperBot.model.incoming;

import java.util.Map;

public record PlaceTradeM (Map<String, String> paramMap, int round) {
}
