package com.shoonya.trade_server.service;

import com.shoonya.trade_server.lib.ShoonyaHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class OrderManagementService {

    ShoonyaHelper shoonyaHelper;
    public OrderManagementService(ShoonyaHelper shoonyaHelper) {
        this.shoonyaHelper = shoonyaHelper;
    }

    public void modifyOrder(String norenordno, double newPrice ){
        JSONArray orderBook = shoonyaHelper.getOrderBook();

        for (int i = 0; i < orderBook.length(); i++) {
            JSONObject order = orderBook.getJSONObject(i);
            if (order.getString("norenordno").equals(norenordno)) {

                if (order.getString("prctyp").equals("LMT"))
                    shoonyaHelper.modifyOrder(order.getString("exch"), order.getString("tsym"), norenordno,
                            order.getInt("qty"), "LMT", newPrice, 0.0);

                if (order.getString("prctyp").equals("SL-LMT"))
                    shoonyaHelper.modifyOrder(order.getString("exch"), order.getString("tsym"), norenordno,
                            order.getInt("qty"), "SL-LMT", newPrice, newPrice - 0.2);

                break;
            }
        }
    }
}
