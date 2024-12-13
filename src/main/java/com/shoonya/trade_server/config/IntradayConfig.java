package com.shoonya.trade_server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "intraday")
public class IntradayConfig {
    private String outFolder;
    private int maxTrades;
    private List<Index> indexes;

    @Getter
    @Setter
    public static class Index {
        private String name;
        private int token;
        private int minLotSize;
        private double futSl;
        private double maxFutSl;
        private List<Double> targets;
        private double triggerDiff;
    }

}
