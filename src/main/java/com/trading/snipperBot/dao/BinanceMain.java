package com.trading.snipperBot.dao;

import com.binance.connector.client.impl.SpotClientImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;

@Repository
public class BinanceMain {

    @Value("${binance.api.key}")
    private String apiKey;

    @Value("${binance.secret.key}")
    private String secretKey;

    public static void main(String[] args) {



        String apiKey = "your_api_key";
        String secretKey = "your_secret_key";

//        RequestOptions options = new RequestOptions();
        SpotClientImpl client = new SpotClientImpl(apiKey, secretKey);

        // Call the account information endpoint
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
//        String result = client.createSubAccount().accountStatus(parameters);
//        System.out.println(result);

        System.out.println("working");
    }


}
