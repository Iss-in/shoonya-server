package com.shoonya.trade_server.lib;

import com.neovisionaries.ws.client.*;
import com.noren.javaapi.NorenApiJava;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ShoonyaWebSocket {
    private WebSocket ws;
    private boolean websocketConnected = false;

    String source = "API";

    public void changeConnectionState(Boolean state){
        this.websocketConnected = state;
    }

    private Logger logger = LogManager.getLogger(ShoonyaWebSocket.class.getName());

    public ShoonyaWebSocket(String websocketEndpoint, NorenApiJava api, WebSocketHandler handler )  {

        try {
            WebSocketFactory factory = new WebSocketFactory();
            this.ws = factory.createSocket(websocketEndpoint);

            // send custom ping every 3 second to maintain a forever connection
            this.ws.setPingInterval(3000);
            ws.sendPing("{\"t\":\"h\"}");

            this.ws.addListener(new WebSocketAdapter() {
                @Override
                public void onTextMessage(WebSocket ws, String message) {
//                    logger.info(message);
                    handler.onTextMessage(message);
                }

                @Override
                public void onPongFrame(WebSocket websocket, WebSocketFrame frame) {
//                    System.out.println("Pong received: " + frame.getPayloadText());
                }

                @Override
                public void onBinaryMessage(WebSocket websocket, byte[] b) {
                    // Not Required for shoonya

                }

                @Override
                public void onError(WebSocket ws, WebSocketException cause) {
                    logger.error(cause.toString());
                }

                @Override
                public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws IOException, WebSocketException {
                    // create your reconnection logic here
                    logger.error("Websocker disconnected, connecting again");
                    ws = websocket.recreate().connect();
                }


                @Override
                public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {

                    changeConnectionState(true);

                    String actid = api.get_actid();
                    String susertoken = api.get_key();
                    JSONObject data = new JSONObject();
                    data.put("t", "c");
                    data.put("actid", actid);
                    data.put("uid", actid);
                    data.put("susertoken", susertoken);
                    data.put("source", source);
                    logger.info("sending cred data to websocket on connecting {}", data);
                    websocket.sendText(data.toString());
                }

            });
        } catch (Exception e) {
            logger.error("error in initializing websocket {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void subscribe(String instrument, NorenApiJava.FeedType feedType){
        if(this.websocketConnected) {
            logger.info("subscribing to {}", instrument);
            String feedTypeCode = feedType == NorenApiJava.FeedType.TOUCHLINE ? "t" : "d";
            JSONObject data = new JSONObject();
            data.put("t", feedTypeCode);
            data.put("k", instrument);
            ws.sendText(data.toString());
        }
        else
            logger.info("could not subscribe because of websocket state {}", websocketConnected);
    }
    public void unsubscribe(String instrument, NorenApiJava.FeedType feedType){
        if(this.websocketConnected) {
            logger.info("unsubscribing from  {}", instrument);
            String feedTypeCode = feedType == NorenApiJava.FeedType.TOUCHLINE ? "u" : "ud";
            JSONObject data = new JSONObject();
            data.put("t", feedTypeCode);
            data.put("k", instrument);
            this.ws.sendText(data.toString());        }
        else
            logger.info("could not subscribe because of websocket state {}", websocketConnected);
    }

    public void closeWebsocket(){
        if(this.websocketConnected ) {
            changeConnectionState(false);
            this.ws.setPingInterval(0);
        }
    }

    // Start the WebSocket connection
    public void connect()  {
//        this.ws.connect();
        this.ws.connectAsynchronously();// TODO: why is this better ?
    }


    public interface WebSocketHandler {
        void onTextMessage(String message);

        void onError(WebSocketException cause);
    }

}

