package com.shoonya.trade_server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "shoonya")
public class ShoonyaConfig {
    private String user;
    private String password;
    private String vc;
    private String apiKey;
    private String imei;
    private String totpKey;
    private String emailId;
    private String cliname;
    private String prfname;
    private String acctnum;
    private String bankn;
    private String upi;
    private String ifscCode;
    private String logFolder;
    private List<Exchange> exchanges;
    private String host;
    private String websocket;

    @Getter
    @Setter
    public static class Exchange {
        private String name;
        private String fileUri;
    }
}
