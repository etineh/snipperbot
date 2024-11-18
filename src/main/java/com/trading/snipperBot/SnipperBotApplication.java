package com.trading.snipperBot;

import com.trading.snipperBot.dao.impl.BinanceDaoImpl;
import com.trading.snipperBot.utils.BinanceUtils;
import com.trading.snipperBot.utils.DateUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan({"config", "com.trading.snipperBot"})
@SpringBootApplication
public class SnipperBotApplication {

	public static void main(String[] args) {

		SpringApplication.run(SnipperBotApplication.class, args);

//		try {
//			BinanceUtils.checkOnePairBalance("btc");
//
//			BinanceUtils.loadSymbolDecimals();
//			BinanceUtils.saveAllPairLatestBalanceToMap();
//		} catch (Exception e) {
//
//			System.out.println("there is error: " + e.getMessage());
//		}

//		BinanceDaoImpl binanceDao = new BinanceDaoImpl();
//		binanceDao.liveMarketData();

	}

}
