package com.shoonya.trade_server.controller;

import com.shoonya.trade_server.entity.DailyRecord;
import com.shoonya.trade_server.entity.Quote;
import com.shoonya.trade_server.lib.ShoonyaHelper;
import com.shoonya.trade_server.repositories.DailyRecordRepository;
import com.shoonya.trade_server.repositories.QuoteRepository;
import com.shoonya.trade_server.service.OptionUpdateService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.cglib.core.Local;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
public class PollingController {

    long lastQuoteId = -1;

    OptionUpdateService optionUpdateService;
    QuoteRepository quoteRepository;
    DailyRecordRepository  dailyRecordRepository;
    ShoonyaHelper shoonyaHelper;

    public PollingController(OptionUpdateService optionUpdateService, QuoteRepository quoteRepository ,
                             ShoonyaHelper shoonyaHelper, DailyRecordRepository  dailyRecordRepository ){
        this.optionUpdateService = optionUpdateService;
        this.quoteRepository = quoteRepository;
        this.shoonyaHelper = shoonyaHelper;
        this.dailyRecordRepository = dailyRecordRepository;
    }

//    @GetMapping("/atmSymbols")
//    public Map<String, Object> getAtmSymbols() {
//        Map<String, Object> map = optionUpdateService.getAtmSymbols().toMap();
//        return map;
//    }

    @GetMapping("/atmPrice/{symbol}")
    public Map<String, Object> getAtmPrice(@PathVariable String symbol) {
//        JSONObject prices = n
        JSONObject prices = new JSONObject();
        prices.put("NIFTY26DEC24C0", 123);
        prices.put("NIFTY26DEC24P0", 223);
        Map<String, Object> map = prices.toMap();
        return map;
    }

    @GetMapping("/latestPe")
    public Map<String, Object> getLatestPe() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", optionUpdateService.getAtmPe());
        return data;
    }

    @GetMapping("/quote")
    public Map<String, Object> quote() {
        JSONObject res = new JSONObject();
        Quote quote = quoteRepository.getRandomQuote(lastQuoteId);
        lastQuoteId = quote.getId();
        res.put("quote", quote.getQuote());

        Map<String, Object> map = res.toMap();
        return map;
    }

    @GetMapping("/positions")
    public Map<String, Object> positions() {
//        JSONArray positions = shoonyaHelper.getPositions();
        JSONArray x = new JSONArray();
        JSONObject res = new JSONObject();
        res.put("symbol", "NIFTY23JUN18000CE");
        res.put("entryPrice", "NIFTY23JUN18000CE");
        res.put("symbol", "NIFTY23JUN18000CE");
        res.put("symbol", "NIFTY23JUN18000CE");
        res.put("symbol", "NIFTY23JUN18000CE");

        Map<String, Object> map = res.toMap();
        return map;
    }

    @GetMapping("/pnl")
    public Map<LocalDate, Integer> getPnlData(
        @RequestParam("start")  LocalDate start, @RequestParam("end") LocalDate end ) {

        Map<LocalDate, Integer> res = new HashMap<>();
//        res.put(LocalDate.of(2024, 12 , 26), 1000);
//        res.put(LocalDate.of(2024, 12 , 25), -400);
        List<DailyRecord> records =  dailyRecordRepository.getPnlBetweenDates(start, end);
        for(DailyRecord record : records)
            res.put(record.getDate(), record.getPnl());
//        Map<String, Object> map = res.toMap();
        return res;
    }

    @PostMapping("/firstFetch")
    public void test() throws IOException, InterruptedException {
        optionUpdateService.setLastestOptions(true);
    }


}
