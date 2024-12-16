package com.shoonya.trade_server.service;

import com.shoonya.trade_server.entity.DailyRecord;
import com.shoonya.trade_server.entity.Trade;
import com.shoonya.trade_server.entity.ParsedTrade;

import com.shoonya.trade_server.lib.ShoonyaHelper;
import com.shoonya.trade_server.repositories.DailyRecordRepository;
import com.shoonya.trade_server.repositories.ParsedTradeRepository;
import com.shoonya.trade_server.repositories.TradeRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.core.Local;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.primitives.Doubles.max;


class TradeDetail {
    LocalDateTime initialBuyTime;
    LocalDateTime lastBuyTime;
    int totalQty;
    double priceDifference;

    public TradeDetail(LocalDateTime initialBuyTime, LocalDateTime lastBuyTime, int totalQty,
                       double priceDifference) {
        this.initialBuyTime = initialBuyTime;
        this.lastBuyTime = lastBuyTime;
        this.totalQty = totalQty;
        this.priceDifference = priceDifference;
    }

    @Override
    public String toString() {
        return "TradeDetail{" +
                "initialBuyTime=" + initialBuyTime +
                ", lastBuyTime=" + lastBuyTime +
                ", totalQty=" + totalQty +
                ", priceDifference=" + priceDifference +
                '}';
    }
}


@Service
public class TradeParserService {

    ShoonyaHelper shoonyaHelper;
    TradeRepository tradeRepository;
    ParsedTradeRepository parsedTradeRepository;
    DailyRecordRepository dailyRecordRepository;

    private Logger logger = LoggerFactory.getLogger(TradeRepository.class.getName());

    public  TradeParserService( ShoonyaHelper shoonyaHelper,  TradeRepository tradeRepository,
                                ParsedTradeRepository parsedTradeRepository, DailyRecordRepository dailyRecordRepository){
        this.shoonyaHelper = shoonyaHelper;
        this.tradeRepository = tradeRepository;
        this.parsedTradeRepository = parsedTradeRepository;
        this.dailyRecordRepository = dailyRecordRepository;
    }

    // TODO: add a valid trading day variable, put 0 manually when needed in case of holiday ?
    @Scheduled(cron = "30 15 * * * ?")
    public void parseTrades(){
        JSONArray res = shoonyaHelper.getTradebook();
        List<String> orderUid = new ArrayList<>();
        for (int i = res.length() -1; i >=0; i--) {
            JSONObject order = res.getJSONObject(i);
            String orderId = order.getString("norenordno");
            if (!orderUid.contains(orderId) && order.getString("s_prdt_ali").equals("NRML")) {
                orderUid.add(orderId);
                String date = order.getString("fltm").split("\\s+")[0];
                String time = order.getString("fltm").split("\\s+")[1];

                DateTimeFormatter SimpleDateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

                Trade trade = new Trade(LocalDateTime.parse(order.getString("fltm"),SimpleDateFormat),
                        order.getString("tsym"), order.getInt("qty"), order.getString("trantype"),
                        Double.parseDouble(order.getString("avgprc")));
                tradeRepository.save(trade);
            }
        }
        logger.info("Trades uploaded to db for today");
        updateProcessTrades();
        updateDailyRecords();
    }

    public void updateProcessTrades(){
        List<Trade> trades = tradeRepository.findTodayTrades();

        List<ParsedTrade> parsedTrades = new ArrayList<>();

        List<Trade> buys = new ArrayList<>();
        List<Trade> sells = new ArrayList<>();

        for (Trade trade : trades) {
            if (trade.getOrderType().equals("B")) {
                buys.add(trade);
            } else if (trade.getOrderType().equals("S")) {
                sells.add(trade);
            }
            // Check if a complete trade is formed (all buys matched with sells)
            int totalBuyQty = buys.stream().mapToInt(Trade::getQty).sum();
            int totalSellQty = sells.stream().mapToInt(Trade::getQty).sum();

            if (totalBuyQty == totalSellQty && !buys.isEmpty() && !sells.isEmpty()) {
                LocalDateTime initialBuyTime = buys.get(0).getTimestamp();
                LocalDateTime lastBuyTime = buys.get(buys.size() - 1).getTimestamp();
                double totalBuyPrice = buys.stream().mapToDouble(t -> t.getPrice() * t.getQty()).sum();
                double totalSellPrice = sells.stream().mapToDouble(t -> t.getPrice() * t.getQty()).sum();
                double priceDifference = (totalSellPrice / totalSellQty) - (totalBuyPrice / totalBuyQty);
                priceDifference = Math.round(priceDifference * 10.0) / 10.0;

                ParsedTrade parsedTrade = new ParsedTrade(initialBuyTime, lastBuyTime, totalBuyQty, priceDifference);
                parsedTradeRepository.save(parsedTrade);

                // Clear buys and sells for the next trade
                buys.clear();
                sells.clear();
            }
        }
        logger.info("Parsed trades uploaded to db");
    }

    public static LocalDate getNextNonWeekendDay(LocalDate date) {
        // Increment the date until it's not Saturday or Sunday
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }
    public static LocalDate getPreviousNonWeekendDay(LocalDate date) {
        // Decrement the date until it's not Saturday or Sunday
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.minusDays(1);
        }
        return date;
    }


    //#TODO: fetch last working day from db only ?
    // TODO: implement trading day as well ?
    // TODO: get maxLoss without updating today ?, update today at eod only
    // TODO: successful trades part
    public void updateDailyRecords(){
        LocalDate today = LocalDate.now();
        LocalDate nextDay = getNextNonWeekendDay(today);
        LocalDate prevDay = getPreviousNonWeekendDay(today);

        int trades = shoonyaHelper.getTradeCount();
        double pnl = shoonyaHelper.getPnl();
        DailyRecord lastDayRecord = dailyRecordRepository.findRecordByDate(prevDay);
        double prevAccValue = lastDayRecord.getAccountValue();
        double prevPeakValue = lastDayRecord.getPeakValue();
        double currentAccValue = prevAccValue + pnl;
        double currentPeakValue = max(prevPeakValue, currentAccValue);
        double drawdown = currentPeakValue - currentAccValue;
        double maxLoss = max(1000, 0); //TODO: fetch pnl of last 10 trades

        DailyRecord todayRecord = new DailyRecord(today, trades, 0, (int)currentAccValue, (int)currentPeakValue,
                (int)drawdown, (int)pnl, (int)maxLoss);

    }
}
