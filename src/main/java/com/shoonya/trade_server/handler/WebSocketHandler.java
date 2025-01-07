package com.shoonya.trade_server.handler;

import com.shoonya.trade_server.service.OptionUpdateService;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Getter
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    private WebSocketSession session;
    boolean lock = true;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("Connected to WebSocket client");
        this.session = session;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        logger.info("Received message: {}", message.getPayload());
        // Echo the message back to the client
        if(message.getPayload().equals("frontend connected"))
            this.lock = false;

//        session.sendMessage(new TextMessage("Echo: " + message.getPayload()));
    }

    public WebSocketSession getSession() {
//        while (session == null ) {
//            try {
//                logger.info("websocket session not active yet, sleeping for a second");
//                Thread.sleep(1000);
//            } catch (Exception e) {
//            logger.error("Failed to wait for websocket session: {}", e.getMessage());
//            }
//        }
        return session;
    }
}