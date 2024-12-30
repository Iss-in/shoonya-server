package com.shoonya.trade_server.controller;

import com.shoonya.trade_server.entity.TokenInfo;
import com.shoonya.trade_server.entity.Trade;

import com.shoonya.trade_server.lib.Misc;
import com.shoonya.trade_server.lib.ShoonyaHelper;
import com.shoonya.trade_server.repositories.TradeRepository;
import com.shoonya.trade_server.service.RiskManagementService;
import com.shoonya.trade_server.service.TradeParserService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLOutput;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@RestController
@RequestMapping("/api")
public class TestController {

    ShoonyaHelper shoonyaHelper;
    RiskManagementService riskManagementService;
    TradeRepository tradeRepository ;
    TradeParserService tradeParserService;

    @Autowired
    Misc misc;

    public TestController(  ShoonyaHelper shoonyaHelper, RiskManagementService riskManagementService, TradeRepository tradeRepository, TradeParserService tradeParserService) {
        this.shoonyaHelper = shoonyaHelper;
        this.riskManagementService = riskManagementService;
        this.tradeRepository = tradeRepository;
        this.tradeParserService = tradeParserService;
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

    @PostMapping("/test")
    public void test() {
//        tradeParserService.updateParsedTrades();
        tradeParserService.updateDailyRecords();

    }
}
