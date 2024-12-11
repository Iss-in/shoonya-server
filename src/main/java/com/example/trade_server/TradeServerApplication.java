package com.example.trade_server;

import com.example.trade_server.service.ShoonyaLoginService;
//import com.example.trade_server.service.ShoonyaWebsocketService;
import com.example.trade_server.service.ShoonyaWebsocketService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import com.noren.javaapi.NorenApiJava;

@SpringBootApplication
public class TradeServerApplication {

	static Boolean feed_opened = false;

	@Autowired
	static ShoonyaLoginService shoonyaLoginService;

	@Autowired
	static ShoonyaWebsocketService shoonyaWebsocketService;

	public TradeServerApplication(ShoonyaLoginService shoonyaLoginService,  ShoonyaWebsocketService shoonyaWebsocketService){
		this.shoonyaLoginService = shoonyaLoginService;
		this.shoonyaWebsocketService = shoonyaWebsocketService;
	}

	public static Logger logger = LogManager.getLogger(TradeServerApplication.class.getName());

	public static void main(String[] args) throws Exception {
		SpringApplication.run(TradeServerApplication.class, args);
		System.out.println("Hello and Welcome to Noren!");
		logger.info("Testing logger file");

		NorenApiJava api = shoonyaLoginService.login("https://api.shoonya.com/NorenWClientTP/","wss://api.shoonya.com/NorenWSTP/");
//		System.out.println("_key: " + api.get_key());
//		System.out.println("_key: " + api.get_limits());
		shoonyaWebsocketService.startWebsocket(api);
		shoonyaWebsocketService.subscribe("NSE", "26000");

	}

}
