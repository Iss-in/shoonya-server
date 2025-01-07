package com.shoonya.trade_server.controller;

import com.shoonya.trade_server.entity.TokenInfo;
import com.shoonya.trade_server.entity.Trade;

import com.shoonya.trade_server.lib.Misc;
import com.shoonya.trade_server.lib.ShoonyaHelper;
import com.shoonya.trade_server.repositories.TradeRepository;
import com.shoonya.trade_server.service.OptionUpdateService;
import com.shoonya.trade_server.service.RiskManagementService;
import com.shoonya.trade_server.service.TradeParserService;
import com.shoonya.trade_server.service.WebSocketService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        return ResponseEntity.ok("Session ended");
    }



    @GetMapping("/test/{tsym}")
    public List<Map<String, Object>> test(@PathVariable String tsym) {
        // Get the token for the given symbol
        String token = misc.getToken("NFO", tsym);

        // Calculate the start time, one week before the current date
        long starttime = LocalDate.now().minus(1, ChronoUnit.WEEKS)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().getEpochSecond();

        // Get the time price series data
        JSONArray res = shoonyaHelper.getTimePriceSeries("NFO", token, String.valueOf(starttime), null, "1");

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
}
