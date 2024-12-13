package com.shoonya.trade_server.logging;

import com.shoonya.trade_server.config.ShoonyaConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class Log4j2DynamicConfig implements ApplicationListener<ApplicationReadyEvent> {

    private ShoonyaConfig shoonyaConfig;

    public Log4j2DynamicConfig(ShoonyaConfig shoonyaConfig){
        this.shoonyaConfig = shoonyaConfig;
    }
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Set the system property for Log4j2
        System.setProperty("dynamic.log.folder.name", shoonyaConfig.getLogFolder());
    }
}

