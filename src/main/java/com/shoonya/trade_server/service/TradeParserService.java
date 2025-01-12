package com.shoonya.trade_server.service;

import com.shoonya.trade_server.entity.DailyRecord;
import com.shoonya.trade_server.entity.Holiday;
import com.shoonya.trade_server.entity.Trade;
import com.shoonya.trade_server.entity.ParsedTrade;

import com.shoonya.trade_server.lib.Misc;
import com.shoonya.trade_server.lib.ShoonyaHelper;
import com.shoonya.trade_server.repositories.DailyRecordRepository;
import com.shoonya.trade_server.repositories.HolidayRepository;
import com.shoonya.trade_server.repositories.ParsedTradeRepository;
import com.shoonya.trade_server.repositories.TradeRepository;
import jakarta.annotation.PostConstruct;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.core.Local;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.postgresql.util.PSQLException;
import org.springframework.data.domain.Pageable;

import static com.google.common.primitives.Doubles.max;


//class TradeDetail {
//    LocalDateTime initialBuyTime;
//    LocalDateTime lastBuyTime;
//    int totalQty;
//    double priceDifference;
//
//    public TradeDetail(LocalDateTime initialBuyTime, LocalDateTime lastBuyTime, int totalQty,
//                       double priceDifference) {
//        this.initialBuyTime = initialBuyTime;
//        this.lastBuyTime = lastBuyTime;
//        this.totalQty = totalQty;
//        this.priceDifference = priceDifference;
//    }
//
//    @Override
//    public String toString() {
//        return "TradeDetail{" +
//                "initialBuyTime=" + initialBuyTime +
//                ", lastBuyTime=" + lastBuyTime +
//                ", totalQty=" + totalQty +
//                ", priceDifference=" + priceDifference +
//                '}';
//    }
//}


@Service
public class TradeParserService {

    ShoonyaHelper shoonyaHelper;
    TradeRepository tradeRepository;
    ParsedTradeRepository parsedTradeRepository;
    DailyRecordRepository dailyRecordRepository;
    HolidayRepository holidayRepository;
    AtrTrailingStopService atrTrailingStopService;
    Misc misc;

    private Logger logger = LoggerFactory.getLogger(TradeRepository.class.getName());

    // Define the time threshold (e.g., 14:00)
    private static final LocalTime THRESHOLD_TIME = LocalTime.of(15, 40);
    private boolean taskPerformed = false;
    private boolean validDay = true;

    public  TradeParserService( ShoonyaHelper shoonyaHelper,  TradeRepository tradeRepository,
                                ParsedTradeRepository parsedTradeRepository, DailyRecordRepository dailyRecordRepository,
                                Misc misc, HolidayRepository holidayRepository, AtrTrailingStopService atrTrailingStopService ){
        this.shoonyaHelper = shoonyaHelper;
        this.tradeRepository = tradeRepository;
        this.parsedTradeRepository = parsedTradeRepository;
        this.dailyRecordRepository = dailyRecordRepository;
        this.misc = misc;
        this.holidayRepository = holidayRepository;
        this.validDay =  checkValidDay(LocalDate.now());
        this.atrTrailingStopService = atrTrailingStopService;
    }


    @Scheduled(fixedRate = 60000) // Check every minute
    public void checkTimePeriodically() {
        checkAndPerformTask(false);
    }


    void checkAndPerformTask(boolean manual) {
        if (!taskPerformed && validDay) {
            // Get the current time
            LocalTime currentTime = LocalTime.now();

            // Check if the current time is past the threshold time
            if (currentTime.isAfter(THRESHOLD_TIME) || manual) {
                // Run the desired task
                parseTrades();
                taskPerformed = true;
            }
        }
    }

    // TODO: add a valid trading day variable, put 0 manually when needed in case of holiday ?
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

                Trade trade = new Trade(order.getString("exch"), orderId, LocalDateTime.parse(order.getString("fltm"),SimpleDateFormat),
                        order.getString("tsym"), order.getInt("qty"), order.getString("trantype"),
                        Double.parseDouble(order.getString("avgprc")));
                tradeRepository.save(trade);

            }
        }
        logger.info("Trades uploaded to db for today");
        updateParsedTrades();
        updateDailyRecords();
        updateMaxRun();
    }

    public void updateParsedTrades(){
        LocalDate today = LocalDate.now();
//        LocalDate today = LocalDate.now().minusDays(1);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        List<Trade> trades = tradeRepository.findTradesByDate(startOfDay, endOfDay);
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
                LocalDateTime lastBuyTime = sells.get(sells.size() - 1).getTimestamp();
                double totalBuyPrice = buys.stream().mapToDouble(t -> t.getPrice() * t.getQty()).sum();
                double totalSellPrice = sells.stream().mapToDouble(t -> t.getPrice() * t.getQty()).sum();

                double avgBuyPrice = totalBuyPrice / totalBuyQty;
                double avgSellPrice =  totalSellPrice / totalSellQty;

                double priceDifference = avgSellPrice - avgBuyPrice;
                priceDifference = Math.round(priceDifference * 10.0) / 10.0;

                String tradingSymbol = trade.getTradingSymbol();
                String exch = trade.getExch();

                ParsedTrade parsedTrade = new ParsedTrade(exch, tradingSymbol, initialBuyTime, lastBuyTime, totalBuyQty, avgBuyPrice, avgSellPrice,  priceDifference, 0.0);

                parsedTradeRepository.save(parsedTrade);

                // Clear buys and sells for the next trade
                buys.clear();
                sells.clear();
            }
        }
        logger.info("Parsed trades uploaded to db");
    }

    public  LocalDate getNextNonWeekendDay(LocalDate date) {
        // Increment the date until it's not Saturday or Sunday
        date = date.plusDays(1);
        List<LocalDate> holidays = holidayRepository.findAllHolidayDates();
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY || holidays.contains(date)){
            date = date.plusDays(1);
        }
        return date;
    }
    public  LocalDate getPreviousNonWeekendDay(LocalDate date) {
        // Decrement the date until it's not Saturday or Sunday
        date = date.minusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.minusDays(1);
        }
        return date;
    }

    public boolean checkValidDay(LocalDate date){
        List<LocalDate> holidays = holidayRepository.findAllHolidayDates();
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY || holidays.contains(date))
            return false;
        return true;
    }


    //#TODO: fetch last working day from db only ?
    // TODO: implement trading day as well ?
    // TODO: get maxLoss without updating today ?, update today at eod only
    // TODO: successful trades part -- check if it work
    public void updateDailyRecords(){
        LocalDate today = LocalDate.now();
        LocalDate nextDay = getNextNonWeekendDay(today);
        LocalDate prevDay = getPreviousNonWeekendDay(today);

        int trades = shoonyaHelper.getTradeCount();
        double pnl = shoonyaHelper.getPnl();
//        DailyRecord lastDayRecord = dailyRecordRepository.findRecordByDate(prevDay);
        DailyRecord lastDayRecord = dailyRecordRepository.findLastDay(today);
        double prevAccValue = lastDayRecord.getAccountValue();
        double prevPeakValue = lastDayRecord.getPeakValue();
        double currentAccValue = prevAccValue + pnl;
        double currentPeakValue = max(prevPeakValue, currentAccValue);
        double drawdown =   Math.min(currentAccValue - currentPeakValue, 0);

//        Pageable pageable = PageRequest.of(0, 10); // First page with n results
        List<DailyRecord> records = dailyRecordRepository.findLastNEntriesBeforeToday(today);

        int total=0;
        for (DailyRecord record:records)
            total += record.getPnl();
        double maxLoss =  total/records.size();
        maxLoss = max(1000, maxLoss);

        int successfulTrades = parsedTradeRepository.countByColumnValueExceeds(5, today);
        DailyRecord todayRecord = dailyRecordRepository.findRecordByDate(today);
        todayRecord.setTrades(trades);
        todayRecord.setSuccessfulTrades(successfulTrades);
        todayRecord.setAccountValue((int)currentAccValue);
        todayRecord.setDrawdown((int)drawdown);
        todayRecord.setPeakValue((int)currentPeakValue);
        todayRecord.setPnl((int)pnl);

        DailyRecord nextRecord = new DailyRecord();
        nextRecord.setDate(nextDay);
        nextRecord.setMaxLoss((int)maxLoss);
        dailyRecordRepository.save(todayRecord);
        dailyRecordRepository.save(nextRecord);

        logger.info("updated daily record for today");

    }

    public void updateMaxRun(){
        LocalDate today = LocalDate.now();
//        LocalDate yesterday = today.minusDays(1);
        List<ParsedTrade> parsedTrades = parsedTradeRepository.getTradesByDate(today);

        int tradeCount = 1;
        for(ParsedTrade parsedTrade:parsedTrades){
            LocalDateTime buyTime = parsedTrade.getBuyTime();
            String tradingSymbol = parsedTrade.getTradingSymbol();
            String exch = parsedTrade.getExch();
            String token = misc.getToken(exch, tradingSymbol);
            double maxPoints = 0;
            double buyPrice = parsedTrade.getBuyPrice();
            double points = parsedTrade.getSellPrice() - buyPrice;
            double slPoints  =  misc.getMaxSl(exch, token) / 2;
            if(points < 0)
                maxPoints = points;
            else {
                double slPrice = buyPrice - slPoints;
                List<Double> targets = misc.getTargets(exch, token);
                List<Double> targetsReached = new ArrayList<>();

                long buyTs = buyTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000;

                JSONArray timePriceSeries = shoonyaHelper.getTimePriceSeries(exch, token, "" + buyTs,
                        null, "1");
                for (int i = timePriceSeries.length()-2;i>=0; i--) {
                    JSONObject candle = timePriceSeries.getJSONObject(i);
                    double high = candle.getDouble("inth");
                    double low = candle.getDouble("intl");
                    double currentPoints = high - buyPrice;
                    String timestamp = candle.getString("time");
                    if (low < slPrice) {
                        maxPoints = slPrice - buyPrice;
                        break;
                    }
                    for (double target : targets) {
                        if (high > target + buyPrice && !targetsReached.contains(target)) {
                            targetsReached.add(target);
                            maxPoints += target / 2;
                        }
                        if (2 == targetsReached.size())
                            break;
                    }
                    if (2 == targetsReached.size())
                        break;

                }
            }
            maxPoints = Math.round(maxPoints * 10.0) / 10.0;
            maxPoints = Math.max(-1 * slPoints, maxPoints);
            logger.info("max points for trade {} are {}" ,tradeCount++ , maxPoints);
            parsedTrade.setMaxPoints(maxPoints);
            parsedTradeRepository.save(parsedTrade);
//            count = count + 1;

        }
    }

    public void updateMaxRun2() {
        LocalDate today = LocalDate.now();
//        LocalDate yesterday = today.minusDays(1);
        List<ParsedTrade> parsedTrades = parsedTradeRepository.getTradesByDate(today);

        int tradeCount = 1;
        for (ParsedTrade parsedTrade : parsedTrades) {
            LocalDateTime buyTime = parsedTrade.getBuyTime();
            String tradingSymbol = parsedTrade.getTradingSymbol();
            String exch = parsedTrade.getExch();
            String token = misc.getToken(exch, tradingSymbol);
            double maxPoints = 0;
            double buyPrice = parsedTrade.getBuyPrice();
            double points = parsedTrade.getSellPrice() - buyPrice;
            double slPoints = misc.getMaxSl(exch, token) / 2;
            if (points < 0) {
                maxPoints = points;
            }
            else {
                double slPrice = buyPrice - slPoints;
                List<Double> targets = misc.getTargets(exch, token);
                List<Double> targetsReached = new ArrayList<>();

                long buyTs = buyTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000;

                JSONArray timePriceSeries = shoonyaHelper.getTimePriceSeries(exch, token, "" + buyTs,
                        null, "1");
                double pts = 0;
                for (int i = timePriceSeries.length() - 2; i >= 0; i--) {
                    JSONObject candle = timePriceSeries.getJSONObject(i);
                    JSONObject prevCandle = timePriceSeries.getJSONObject(i + 1);
                    JSONObject nextCandle = timePriceSeries.getJSONObject(i - 1);

                    double high = candle.getDouble("inth");
                    double low = candle.getDouble("intl");
                    double close = candle.getDouble("intc");
                    double previousClose = prevCandle.getDouble("intc");
                    double nextLow = nextCandle.getDouble("intl");

                    atrTrailingStopService.updatePrice(high, low, close, previousClose);
                    maxPoints =  nextLow - buyPrice;
                    if(atrTrailingStopService.checkExit(nextLow)){
                        break;
                    }
                }
            }
            logger.info("max points for trade {} are {}" ,tradeCount++ ,maxPoints);

        }
    }
}
