package com.shoonya.trade_server.controller;

import com.shoonya.trade_server.lib.ShoonyaHelper;
import com.shoonya.trade_server.service.OrderManagementService;
import com.shoonya.trade_server.service.TradeManagementService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OrderController {
    private TradeManagementService tradeManagementService;
    private Logger logger = LoggerFactory.getLogger(OrderController.class.getName());
    private ShoonyaHelper shoonyaHelper;
    private OrderManagementService orderManagementService;
    public OrderController(ShoonyaHelper shoonyaHelper, TradeManagementService tradeManagementService,
                           OrderManagementService orderManagementService){
        this.shoonyaHelper = shoonyaHelper;
        this.tradeManagementService = tradeManagementService;
        this.orderManagementService = orderManagementService;
    }

    @PostMapping("/buyOrder/{symbol}/{priceType}/{price}")
    public ResponseEntity<String> buyOrder(@PathVariable String symbol,@PathVariable String priceType,  @PathVariable double price) {
        JSONObject res = orderManagementService.buyOrder(symbol, priceType, price);
        return ResponseEntity.ok("order placed");
    }

//    @GetMapping("/openOrders")
//    public List<Map<String, Object>> getOpenOrders() {
//        return tradeManagementService.getOpenOrders();
//    }

    @PostMapping("/cancelOrder/{norenordno}")
    public ResponseEntity<String > cancelOrder(@PathVariable String norenordno){
        logger.info("cancelling order with order number {}", norenordno);
        shoonyaHelper.cancelOrderNo(norenordno);
        return ResponseEntity.ok("order cancelled");
    }

    @PostMapping("/modifyOrder/{norenordno}/{newPrice}")
    public ResponseEntity<String > modifyOrder(@PathVariable String norenordno, @PathVariable Double newPrice){
        orderManagementService.modifyOrder(norenordno, newPrice);
        return ResponseEntity.ok("order modofied");
    }

}
