package com.example.trade_server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "intraday-updater")
public class IntradayUpdaterConfig {
    private String outFolder;
    private int maxTrades;
    private List<Index> indexes;

    @Getter
    @Setter
    public static class Index {
        private String name;
        private int token;
        private int minLotSize;
        private int futSl;
        private int maxFutSl;
        private List<Integer> targets;
        private int triggerDiff;
//      TODO: change name of diff

    }

}
