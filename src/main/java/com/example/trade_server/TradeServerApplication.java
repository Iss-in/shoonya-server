package com.example.trade_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import com.noren.javaapi.NorenApiJava;

@SpringBootApplication
public class TradeServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradeServerApplication.class, args);
		System.out.println("Hello and Welcome to Noren!");
		NorenApiJava api = new com.noren.javaapi.NorenApiJava("https://api.shoonya.com/NorenWClientTP/");

		String response = api.login(user_id, password, twoFa, vc, api_key, "abc1234");
		System.out.println(response);
		return;

	}

}
