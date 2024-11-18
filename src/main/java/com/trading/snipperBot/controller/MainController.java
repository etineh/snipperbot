package com.trading.snipperBot.controller;

import com.trading.snipperBot.model.incoming.PlaceTradeM;
import com.trading.snipperBot.model.outgoing.TestM;
import com.trading.snipperBot.service.BinanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MainController {

    BinanceService binanceService;

    @Autowired
    public MainController(BinanceService binanceService) {
        this.binanceService = binanceService;
    }

    @GetMapping("")
    public String helloWorld() {
        return "Hello World!";
    }

    @GetMapping("/test")
    private TestM test(){
        return new TestM("test successful", System.currentTimeMillis());
    }

    @GetMapping("/live")
    public TestM live() {
        binanceService.liveMarketData();

        return new TestM("Live data has started", System.currentTimeMillis());

    }

    @GetMapping("/res")
    public void restart() {
        binanceService.restartLiveMarketData();
    }


    @GetMapping("/stop")
    public String stop() {
        binanceService.closeLiveMarketConnection();

        return "I have stop live data";
    }

    @PostMapping("/trade")
    public void startTrade(@RequestBody PlaceTradeM placeTradeM) {
        binanceService.startTrade(placeTradeM);
//        return "I have start trade";
    }


}
