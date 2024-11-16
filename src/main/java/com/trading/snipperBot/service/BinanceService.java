package com.trading.snipperBot.service;

import com.trading.snipperBot.dao.BinanceDoa;
import com.trading.snipperBot.model.incoming.PlaceTradeM;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BinanceService implements BinanceDoa {

    BinanceDoa binanceDoa;

    @Autowired
    public BinanceService(BinanceDoa binanceDoa) {
        this.binanceDoa = binanceDoa;
    }


    @Override
    public void liveMarketData() {
        binanceDoa.liveMarketData();
    }

    @Override
    public void restartLiveMarketData() {
        binanceDoa.restartLiveMarketData();
    }

    @Override
    public void closeLiveMarketConnection() {
        binanceDoa.closeLiveMarketConnection();
    }

    @Override
    public void startTrade(PlaceTradeM placeTradeM) {
        binanceDoa.startTrade(placeTradeM);
    }
}
