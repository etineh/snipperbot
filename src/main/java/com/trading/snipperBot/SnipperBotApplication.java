package com.trading.snipperBot;

import com.trading.snipperBot.utils.BinanceUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SnipperBotApplication {

	@Value("${binance.api.key}")
	private static String apiKey;

	@Value("${binance.secret.key}")
	private static String secretKey;


	public static void main(String[] args) {

		SpringApplication.run(SnipperBotApplication.class, args);

		try {
			BinanceUtils.checkOnePairBalance("ton");

			BinanceUtils.loadSymbolDecimals();
			BinanceUtils.saveAllPairLatestBalanceToMap();
		} catch (Exception e) {

			System.out.println("there is error: " + e.getMessage());
		}


//		BinanceUtils.tradeFee();

//		System.out.println("The step : " + k.symbolDecimalsMap.get("HBARBTC"));

	}

}
