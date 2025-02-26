package com.shoonya.trade_server.entity;

import com.shoonya.trade_server.config.IntradayConfig;
import com.shoonya.trade_server.exceptions.RecordNotFoundException;
import com.shoonya.trade_server.lib.Misc;
import com.shoonya.trade_server.repositories.DailyRecordRepository;
import com.shoonya.trade_server.service.TradeManagementService;
import jakarta.persistence.Entity;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Getter
@Component
public class SessionVars {
    int buyQty;
    int maxLoss;
    LocalDate niftyExpiry;
    private DailyRecordRepository dailyRecordRepository;
    private static final Logger logger = LogManager.getLogger(SessionVars.class.getName());
    private IntradayConfig intradayConfig;

    public SessionVars( DailyRecordRepository dailyRecordRepository, IntradayConfig intradayConfig, Misc misc ) {
        this.dailyRecordRepository = dailyRecordRepository;
        this.intradayConfig = intradayConfig;
        this.buyQty = fetchBuyQty();
        this.maxLoss = fetchMaxLoss();
        this.niftyExpiry = misc.getNiftyExpiry(0);
    }


    public int fetchMaxLoss(){
        LocalDate date = LocalDate.now();
        int maxLoss = 0;
        try {
            DailyRecord record = this.dailyRecordRepository.findById(date).orElseThrow(() -> new RecordNotFoundException("Record not found"));
            maxLoss = record.getMaxLoss();
        }  catch(Exception e) {
            logger.info("record not found for {}", date);
        }
        maxLoss = Math.max(maxLoss, 20 * buyQty);
        logger.info("max loss for today is {}", maxLoss);
        return maxLoss;
    }

    public int fetchBuyQty() {
        List<IntradayConfig.Index> indexes = intradayConfig.getIndexes();
        int qty = 0;
        for(IntradayConfig.Index index: indexes){
            if(index.getName().equals("NIFTY"))
                qty =  index.getBuyQty();
        }
        return qty;
    }


}
