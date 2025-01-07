package com.shoonya.trade_server.lib;
import com.shoonya.trade_server.config.IntradayConfig;
import com.shoonya.trade_server.service.OptionUpdateService;
import com.shoonya.trade_server.service.StartupService;

import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Component;
import tech.tablesaw.api.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Component
public class Misc {

    private final Map<String, Table > dataFrames;
    private final  List<IntradayConfig.Index> indexes;

    private final LocalDate nseExpiry;
    private static final Logger logger = LogManager.getLogger(Misc.class.getName());


    public Misc(StartupService startupService, IntradayConfig intradayConfig){
        this.dataFrames = startupService.getDataFrames();
        this.indexes = intradayConfig.getIndexes();
        this.nseExpiry = getNseWeeklyExpiry("NIFTY", 0);
    }

    public String getToken(String exch, String tsym) {
        Table df = dataFrames.get(exch);
        String col = "TradingSymbol";
//        String type = df.column("TradingSymbol").getClass().getName();

        if (df.column(col) instanceof StringColumn) {
            StringColumn tradingSymbolColumn = df.stringColumn(col);
            Table filtered = df.where(tradingSymbolColumn.isEqualTo(tsym));
            if (filtered.rowCount() > 0) {
                // Retrieve and return "Token" as a string
                return filtered.intColumn("Token").get(0).toString();
            }
        }
        else if (df.column(col) instanceof TextColumn) {
            TextColumn tradingSymbolColumn = df.textColumn(col);
            Table filtered = df.where(tradingSymbolColumn.isEqualTo(tsym));
            if (filtered.rowCount() > 0) {
                // Retrieve and return "Token" as a string
                return filtered.intColumn("Token").get(0).toString();
            }
        }

        return null; // Return null if no match is found
    }


    public LocalDate getNseWeeklyExpiry(String symbol, int week) {
        Table df = dataFrames.get("NFO");
        Table filteredTable = df.where(df.stringColumn("Symbol").isEqualTo(symbol));

        // Parse the "Expiry" column into LocalDate
        StringColumn expiryColumn = filteredTable.stringColumn("Expiry");
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("dd-MMM-yyyy")
                .toFormatter();

        List<LocalDate> parsedDates = new ArrayList<>();

        for (String dateStr : expiryColumn) {
            try {
                String toParse = dateStr.substring(0, 4).toUpperCase() + dateStr.substring(4).toLowerCase();
                //            toParse = dateStr;
                parsedDates.add(LocalDate.parse(toParse, formatter));
            } catch (Exception e) {
                System.err.println("Failed to parse date: " + dateStr + " - " + e.getMessage());
            }
        }

        Collections.sort(parsedDates);
        logger.info("expiry date is {}", parsedDates.get(week) );
        return parsedDates.get(week);
    }

    public String getSpotSymbol(String exch, String token) {
        Table df = dataFrames.get(exch);

        String symbol = "";
//        String type = df.column("Symbol").getClass().getName();

        if (df.column("Token") instanceof IntColumn) {
            IntColumn tokenColumn = df.intColumn("Token");
            Table filtered = df.where(tokenColumn.isEqualTo(Integer.parseInt(token)));
            if (filtered.rowCount() > 0) {
                    StringColumn symbolColumn = filtered.stringColumn("Symbol");
                    symbol = symbolColumn.get(0);

            }
        }

        if(symbol.equals("BSXOPT")) symbol = "SENSEX";
        else if(symbol.equals("BKXOPT")) symbol = "BANKEX";

        return symbol; // Return null if no match is found
    }

    public Double getMaxFutSl(String exch, String token) {
        String spotSymbol = getSpotSymbol(exch, token);
        Double maxfutSl = 0.0;

        for(IntradayConfig.Index index: indexes){
            if(index.getName().equals(spotSymbol))
                maxfutSl =  index.getMaxFutSl();
        }
        return maxfutSl;
    }

    public Double getMaxSl(String exch, String token) {
        String spotSymbol = getSpotSymbol(exch, token);
        Double futSl = 0.0;

        for(IntradayConfig.Index index: indexes){
            if(index.getName().equals(spotSymbol))
                futSl = index.getFutSl();
        }
        return futSl;
    }

    public Double getTriggerdiff(String exch, String token) {
        String spotSymbol = getSpotSymbol(exch, token);
        Double triggerDiff = 0.0;

        for(IntradayConfig.Index index: indexes){
            if(index.getName().equals(spotSymbol))
                triggerDiff = index.getTriggerDiff();
        }
        return triggerDiff;
    }

    public int getMinLotSize(String exch, String token) {
        String spotSymbol = getSpotSymbol(exch, token);
        int minLotSize = 0;

        for(IntradayConfig.Index index: indexes){
            if(index.getName().equals(spotSymbol))
                minLotSize = index.getMinLotSize();
        }
        return minLotSize;
    }
    public List<Double> getTargets(String exch, String token) {
        String spotSymbol = getSpotSymbol(exch, token);
        List<Double> targets = List.of();

        for(IntradayConfig.Index index: indexes){
            if(index.getName().equals(spotSymbol))
                targets = index.getTargets();
        }
        return targets;
    }



}

