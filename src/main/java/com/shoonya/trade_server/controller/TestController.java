package com.shoonya.trade_server.controller;

import com.shoonya.trade_server.entity.TokenInfo;
import com.shoonya.trade_server.entity.Trade;

import com.shoonya.trade_server.lib.Misc;
import com.shoonya.trade_server.lib.ShoonyaHelper;
import com.shoonya.trade_server.repositories.TradeRepository;
import com.shoonya.trade_server.service.*;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.sql.SQLOutput;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@RestController
@RequestMapping("/api")
public class TestController {

    ShoonyaHelper shoonyaHelper;
    RiskManagementService riskManagementService;
    TradeRepository tradeRepository ;
    TradeParserService tradeParserService;
    WebSocketService webSocketService;
    OptionUpdateService optionUpdateService;
    double price = 20;
    private static final Logger logger = LoggerFactory.getLogger(TestController.class);


    @Autowired
    Misc misc;
    @Autowired
    private TradeManagementService tradeManagementService;

    public TestController(ShoonyaHelper shoonyaHelper, RiskManagementService riskManagementService,
                          TradeRepository tradeRepository, TradeParserService tradeParserService,
                          WebSocketService webSocketService, OptionUpdateService optionUpdateService) {
        this.shoonyaHelper = shoonyaHelper;
        this.riskManagementService = riskManagementService;
        this.tradeRepository = tradeRepository;
        this.tradeParserService = tradeParserService;
        this.webSocketService = webSocketService;
        this.optionUpdateService = optionUpdateService;
    }

    //    }
    @GetMapping("/getToken")
    public ResponseEntity<String> getToken(@RequestBody TokenInfo tokenInfo) {
        String exch = tokenInfo.getExch();
        String tsym = tokenInfo.getTsym();

        String tk = misc.getToken(exch, tsym);
        return ResponseEntity.ok("token is : " + tk);
    }

    @GetMapping("/getminLotSize")
    public ResponseEntity<String> getminLotSize(@RequestBody TokenInfo tokenInfo) {
        String exch = tokenInfo.getExch();
        String tk = tokenInfo.getToken();

        int lotSize = misc.getMinLotSize(exch, tk);
        return ResponseEntity.ok("token is : " + lotSize);
    }

    @PostMapping("/endSession")
    public ResponseEntity<String> endSession() {
        riskManagementService.withdrawFunds();
        webSocketService.sendToast("Session closed","Fuck off and do work now");
        tradeParserService.checkAndPerformTask(true);
        return ResponseEntity.ok("Session ended");
    }


    // Base url http://localhost:8080/

    // Get mapping to get status whether file exists or not (/getStatus/{filePath})
    // response 200 status if it does otherwise 404

    // Post mapping to upload file (/upload/{filePath}/{uploadEndpoint})
    // reponse 200 status if done, otherwise some custom error code

    // Post mapping to compress file (/compress/{filePath})
    // reponse 200 status with filepath in body if done, otherwise some custom error code

    @GetMapping("/fetchHistoricalData/{tsym}")
    public List<Map<String, Object>> test(@PathVariable String tsym) {
        // Get the token for the given symbol
        logger.info("fetching historical data for symbol {}", tsym);
        String token = misc.getToken("NFO", tsym);

        // Calculate the start time, one week before the current date
        long starttime = LocalDate.now().minus(1, ChronoUnit.WEEKS)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().getEpochSecond();

        // Get the time price series data
        JSONArray res = shoonyaHelper.getTimePriceSeries("NFO", token, String.valueOf(starttime), null, "3");

        // Convert JSONArray to List of Maps (each map representing a candlestick)
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = res.length()-1; i >=0 ; i--) {
            JSONObject jsonObject = res.getJSONObject(i);
            Map<String, Object> map = new HashMap<>();
            map.put("into", jsonObject.get("into"));
            map.put("stat", jsonObject.get("stat"));
            map.put("ssboe", jsonObject.get("ssboe"));
            map.put("intvwap", jsonObject.get("intvwap"));
            map.put("intoi", jsonObject.get("intoi"));
            map.put("intc", jsonObject.get("intc"));
            map.put("intv", jsonObject.get("intv"));
            map.put("v", jsonObject.get("v"));
            map.put("inth", jsonObject.get("inth"));
            map.put("oi", jsonObject.get("oi"));
            map.put("time", jsonObject.get("time"));
            map.put("intl", jsonObject.get("intl"));
            list.add(map);
        }
        list.reversed();

        return list;
    }

    Countdown timer;
    @PostMapping("/test")
    public ResponseEntity<String> test() {
        timer = new Countdown(10, webSocketService);
        return ResponseEntity.ok("Session ended");
    }

}

@Setter
@Getter
class Countdown{
    private int seconds ;
    private WebSocketService webSocketService;
    private Thread timerThread;

    Countdown(int seconds, WebSocketService webSocketService){
        this.seconds = seconds;
        this.webSocketService = webSocketService;
        startTimer();
    }
    // Method to start the timer countdown
    private String format(int seconds){
        int min = seconds / 60;
        // Calculate the remaining seconds
        int sec = seconds % 60;

        // Format the minutes and seconds to ensure two digits
        String formattedTime = String.format("%02d:%02d", min, sec);

        return formattedTime;
    }
    private void startTimer() {
        stopTimer();

        Thread timerThread = new Thread(() -> {
            while (seconds >= 0) {
                try {
                    Thread.sleep(1000); // Sleep for 1 second
                    String ts = format(seconds);
                    webSocketService.updateTimer(ts);
                    seconds--;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        timerThread.start();
    }

    public void stopTimer() {
        if (timerThread != null) {
            timerThread.interrupt();
            try {
                timerThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}