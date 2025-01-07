package com.shoonya.trade_server.service;

import com.shoonya.trade_server.config.IntradayConfig;
import com.shoonya.trade_server.entity.DailyRecord;
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

import java.util.concurrent.TimeUnit;

@Service
@EnableScheduling
public class RiskManagementService {

    double pnl, peakPnl, brokerage;
    int tradeCount, maxTradeCount, maxLoss;

    ShoonyaHelper shoonyaHelper;
    IntradayConfig intradayConfig;
    DailyRecordRepository dailyRecordRepository;

    private Logger logger = LoggerFactory.getLogger(RiskManagementService.class.getName());

    public RiskManagementService(ShoonyaHelper shoonyaHelper, IntradayConfig intradayConfig, DailyRecordRepository dailyRecordRepository){
        this.shoonyaHelper = shoonyaHelper;
        this.intradayConfig = intradayConfig;
        this.dailyRecordRepository = dailyRecordRepository;
        this.pnl = 0;
        this.peakPnl = 0;
        this.tradeCount = 0;
        this.maxTradeCount = this.intradayConfig.getMaxTrades();
        this.brokerage = 0;
        this.maxLoss = getMaxloss();

    }

    public int getMaxloss(){
        LocalDate date = LocalDate.now();
        int maxLoss = 0;
        try {
            DailyRecord record = this.dailyRecordRepository.findById(date).orElseThrow(() -> new RecordNotFoundException("Record not found"));
            maxLoss = record.getMaxLoss();
        }  catch(Exception e) {
            logger.info("record not found for {}", date);
        }
        logger.info("max loss for today is {}", maxLoss);
        return maxLoss;
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

        if(this.pnl <= this.maxLoss * -3/2 ){
            logger.info("next trade loss would exceed max_loss limit, stopping today's session");
            return true;
        }
        return false;
    }


    public void checkRiskManagement() throws InterruptedException {
        update();
        if(killswitch()) {
            while (true) {
                shoonyaHelper.exitAllMarketOrders();
                shoonyaHelper.withdraw();
                TimeUnit.SECONDS.sleep(30);
            }
        }
    }

    @PostConstruct
    public void check(){
        logger.warn("checking Killswitch on startup" );
        update();
        if(killswitch()){
            logger.warn("Killswitch condition crossed");
            shoonyaHelper.exitAllMarketOrders();
            shoonyaHelper.withdraw();
        }
    }

    public int addFunds(Double money){
        return shoonyaHelper.requestFunds(money);
    }

    public JSONObject withdrawFunds(){
        return shoonyaHelper.withdraw();
    }
}
