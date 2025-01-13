package com.shoonya.trade_server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TradeServerApplication {

	public static final Logger logger = LogManager.getLogger(TradeServerApplication.class.getName());

	public static void main(String[] args) {
		System.setProperty("LOG_EXCEPTION_CONVERSION_WORD", "%throwable");
		SpringApplication.run(TradeServerApplication.class, args);
		logger.info("Hello and Welcome to Shoonya trade app");

	}

}
