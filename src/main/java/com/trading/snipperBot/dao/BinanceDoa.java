package com.trading.snipperBot.dao;

public interface BinanceDoa {

    void liveMarketData();

    void restartLiveMarketData();

    void closeLiveMarketConnection();

    void startTrade();

}
