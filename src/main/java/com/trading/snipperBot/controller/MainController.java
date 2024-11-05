package com.trading.snipperBot.controller;

import com.trading.snipperBot.service.BinanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/main")
public class MainController {

    BinanceService binanceService;

    @Autowired
    public MainController(BinanceService binanceService) {
        this.binanceService = binanceService;
    }

    @GetMapping("/")
    public String helloWorld() {
        return "Hello World!";
    }

    @GetMapping("/test")
    private String test(){
        return "it is working..";
    }

    @GetMapping("/live")
    public String live() {
        binanceService.liveMarketData();

        return "Now printing live data";

    }

    @GetMapping("/res")
    public String restart() {
        binanceService.restartLiveMarketData();

        return "I have restart live data";

    }

    @GetMapping("/stop")
    public String stop() {
        binanceService.closeLiveMarketConnection();

        return "I have stop live data";
    }

    @GetMapping("/trade")
    public String startTrade() {
        binanceService.startTrade();

        return "I have start trade";
    }


}
