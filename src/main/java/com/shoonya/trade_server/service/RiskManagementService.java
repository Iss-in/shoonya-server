package com.shoonya.trade_server.service;

import com.shoonya.trade_server.config.IntradayConfig;
import com.shoonya.trade_server.entity.DailyRecord;
import com.shoonya.trade_server.entity.SessionVars;
import com.shoonya.trade_server.exceptions.RecordNotFoundException;
import com.shoonya.trade_server.lib.ShoonyaHelper;
import com.shoonya.trade_server.repositories.DailyRecordRepository;
import jakarta.annotation.PostConstruct;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@EnableScheduling
public class RiskManagementService {

    double pnl, peakPnl, brokerage;
    int tradeCount, maxTradeCount, maxLoss;

    ShoonyaHelper shoonyaHelper;
    IntradayConfig intradayConfig;
    DailyRecordRepository dailyRecordRepository;
    TradeParserService tradeParserService;
    private Logger logger = LoggerFactory.getLogger(RiskManagementService.class.getName());

    public RiskManagementService(ShoonyaHelper shoonyaHelper, IntradayConfig intradayConfig,
                                 DailyRecordRepository dailyRecordRepository, TradeParserService tradeParserService,
                                 SessionVars sessionVars) {
        this.shoonyaHelper = shoonyaHelper;
        this.intradayConfig = intradayConfig;
        this.dailyRecordRepository = dailyRecordRepository;
        this.pnl = 0;
        this.peakPnl = 0;
        this.tradeCount = 0;
        this.maxTradeCount = this.intradayConfig.getMaxTrades();
        this.brokerage = 0;
        this.tradeParserService = tradeParserService;
        this.maxLoss = sessionVars.getMaxLoss();
    }


    public int getMaxLoss(){
        LocalDate date = LocalDate.now();
        int maxLoss = 0;
        try {
            DailyRecord record = this.dailyRecordRepository.findById(date).orElseThrow(() -> new RecordNotFoundException("Record not found"));
            maxLoss = record.getMaxLoss();
            maxLoss = Math.max(maxLoss, 10 * getBuyQty() );
        }  catch(Exception e) {
            logger.info("record not found for {}", date);
        }
        logger.info("max loss for today is {}", maxLoss);
        return maxLoss;
    }

    public int getBuyQty() {
        List<IntradayConfig.Index> indexes = intradayConfig.getIndexes();
        int qty = 0;
        for(IntradayConfig.Index index: indexes){
            if(index.getName().equals("NIFTY"))
                qty =  index.getBuyQty();
        }
        return qty;
    }

    @Scheduled(fixedRate = 60000)
    void update(){
//        System.out.println("Scheduled task is running at: " + System.currentTimeMillis());
        logger.info("updating trade details");
        this.pnl = shoonyaHelper.getPnl();

        if(this.pnl > this.peakPnl)
            this.peakPnl = this.pnl;

        this.tradeCount = shoonyaHelper.getTradeCount();
        this.brokerage = shoonyaHelper.getBrokerage();
//        logger.info("{}, {}, {}, {}", pnl, peakPnl, tradeCount, brokerage);
    }

    boolean killswitch(){
        logger.info("Checking kill switch: pnl={}, peakPnl={}, maxLoss={}, tradeCount={}, maxTradeCount={}",
                this.pnl, this.peakPnl, this.maxLoss, this.tradeCount, this.maxTradeCount);

        if(this.pnl - this.peakPnl <= -1 * this.maxLoss){
            logger.info("max loss crossed");
            return true;
        }

        if(this.tradeCount >= this.maxTradeCount){
            logger.info("max trades crossed");
            return true;
        }

        if(this.pnl <= this.maxLoss * -2/3 ){  // <= -1000
            logger.info("next trade loss would exceed max_loss limit, stopping today's session");
            return true;
        }
        return false;
    }

    public void checkRiskManagement() throws InterruptedException {
        update();
        if(killswitch()) {
//            while (true) {
            logger.info("entering killswithc loop");
            shoonyaHelper.exitAllMarketOrders();
            shoonyaHelper.withdraw();
            tradeParserService.checkAndPerformTask(true);
            TimeUnit.SECONDS.sleep(30);
//        }
        }
    }

    @PostConstruct
    public void checkRiskManagementOnStartup() throws InterruptedException {
        update();
        if(killswitch()) {
            shoonyaHelper.exitAllMarketOrders();
            shoonyaHelper.withdraw();
            tradeParserService.checkAndPerformTask(true);
            TimeUnit.SECONDS.sleep(30);
        }
    }


    public int addFunds(Double money){
        return shoonyaHelper.requestFunds(money);
    }

    public JSONObject withdrawFunds(){
        return shoonyaHelper.withdraw();
    }
}
