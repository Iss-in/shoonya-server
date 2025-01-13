package com.shoonya.trade_server.logging;

import com.shoonya.trade_server.config.ShoonyaConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Properties;

//@Order(Ordered.HIGHEST_PRECEDENCE)
@Configuration
public class Log4j2DynamicConfig   {

//    @Value("${spring.custom.logger.folder}")
//    private String logFolder;
//
//    @Value("${spring.custom.logger.level}")
//    private String logLevel;


    public Log4j2DynamicConfig() {
        // Constructor required by SpringApplicationRunListener
        System.out.println("test");
    }

//    @PostConstruct
//    public void onApplicationEvent() {
//        // Set the system property for Log4j2
//        System.setProperty("spring.custom.logger.folder", logFolder);
//        System.setProperty("spring.custom.logger.level", logLevel);
//    }
//

//    @Override
//    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
//        Properties properties = System.getProperties();
//        String logLevel = properties.getProperty("spring.custom.logger.level");
//        properties.setProperty("spring.custom.logger.folder", logFolder);
//        properties.setProperty("spring.custom.logger.level", logLevel);
////        if (logLevel != null) {
////            System.setProperty("spring.custom.logger.level", logLevel);
////        }
//    }


}

