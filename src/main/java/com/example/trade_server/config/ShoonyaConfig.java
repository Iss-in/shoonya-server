package com.example.trade_server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

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
    private String ifscCode;
    private String logFolder;

    private String nseFile;
    private String nfoFile;

    private String bseFile;
    private String bfoFile;
}
