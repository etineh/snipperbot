package com.trading.snipperBot.dao;

import com.trading.snipperBot.model.incoming.PlaceTradeM;

public interface BinanceDoa {

    void liveMarketData();

    void restartLiveMarketData();

    void closeLiveMarketConnection();

    void startTrade(PlaceTradeM placeTradeM);

}
