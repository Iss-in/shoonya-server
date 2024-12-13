package com.shoonya.trade_server.controller;

import com.shoonya.trade_server.entity.TokenInfo;
import com.shoonya.trade_server.lib.Misc;
import com.shoonya.trade_server.lib.ShoonyaHelper;
import com.shoonya.trade_server.service.ShoonyaWebsocketService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api")
public class TestController {

    ShoonyaWebsocketService shoonyaWebsocketService;
    ShoonyaHelper shoonyaHelper;

    @Autowired
    Misc misc;

    public TestController(ShoonyaWebsocketService shoonyaWebsocketService, ShoonyaHelper shoonyaHelper) {
        this.shoonyaWebsocketService = shoonyaWebsocketService;
        this.shoonyaHelper = shoonyaHelper;
    }

    @PostMapping("/subscribe")
    public ResponseEntity<String> subscribeToken(@RequestBody TokenInfo tokenInfo) {
        shoonyaWebsocketService.subscribe(tokenInfo);
        return ResponseEntity.ok("Received token with instrument: " + tokenInfo.getInstrument());
    }

    @PostMapping("/testMisc")
    public ResponseEntity<String> testMisc(@RequestBody TokenInfo tokenInfo) {
        String exch = tokenInfo.getExch();
        String token = tokenInfo.getToken();

        Double sl = misc.getMaxSl(exch, token) / 2;
        Double diff = misc.getTriggerdiff(exch, token);
        int minLotSize = misc.getMinLotSize(exch, token);
        List<Double> targets = misc.getTargets(exch, token);
        System.out.println(sl + "," + diff + "," + minLotSize + "," + targets.toString());

        shoonyaWebsocketService.subscribe(tokenInfo);
        return ResponseEntity.ok("Received token with instrument: " + tokenInfo.getInstrument());
    }

    //    }
    @GetMapping("/getToken")
    public ResponseEntity<String> getToken(@RequestBody TokenInfo tokenInfo) {
        String exch = tokenInfo.getExch();
        String tsym = tokenInfo.getTsym();

        String tk = misc.getToken(exch, tsym);
        return ResponseEntity.ok("token is : " + tk);
    }

    @GetMapping("/getminLotSize")
    public ResponseEntity<String> getminLotSize(@RequestBody TokenInfo tokenInfo) {
        String exch = tokenInfo.getExch();
        String tk = tokenInfo.getToken();

        int lotSize = misc.getMinLotSize(exch, tk);
        return ResponseEntity.ok("token is : " + lotSize);
    }

    @PostMapping("/test")
    public void test() {
        JSONArray positions = shoonyaHelper.getPositions();
        for (int i = 0; i < positions.length(); i++) {
            JSONObject position = positions.getJSONObject(i);
//            int netQty = Integer.parseInt(position.getInt());
        }
    }
}



//		String exch = "NFO";
//		String token = "46122";
//		Double sl = misc.getMaxSl(exch, token) / 2;
//		Double diff = misc.getTriggerdiff(exch, token );
//		int minLotSize = misc.getMinLotSize(exch, token );
//		List<Double> targets = misc.getTargets(exch, token );
//		System.out.println(sl + "," + diff + "," + minLotSize + "," + targets.toString());