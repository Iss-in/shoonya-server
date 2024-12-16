package com.shoonya.trade_server.controller;

import com.noren.javaapi.NorenApiJava;
import com.shoonya.trade_server.lib.ShoonyaHelper;
import com.shoonya.trade_server.service.RiskManagementService;
import com.shoonya.trade_server.service.ShoonyaLoginService;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import okhttp3.*;

@RestController
@RequestMapping("/api")
public class MoneyController {

    RiskManagementService riskManagementService;

    public MoneyController(RiskManagementService riskManagementService){
        this.riskManagementService = riskManagementService;
    }



    String post(String var1, String var2, JSONObject var3) {
        OkHttpClient client = new OkHttpClient();
        final MediaType JSON = MediaType.get("application/json; charset=utf-8");

        String var10000 = var3.toString();
        String var4 = "jData=" + var10000 + "&jKey=" + var2;
        System.out.println(var1 + " " + var4);
        RequestBody var5 = RequestBody.create(var4, JSON);
        Request var6 = (new Request.Builder()).url(var1).post(var5).build();

        try {
            Response var7 = client.newCall(var6).execute();
            return var7.body().string();
        } catch (IOException var8) {
            var8.printStackTrace();
            return var8.toString();
        }
    }



    @PostMapping("/addFunds/{money}")
    ResponseEntity<String>  addFunds(@PathVariable Double money) {

       int returnCode  = riskManagementService.addFunds(money);
       return ResponseEntity.ok("Return code for funds addition is  " + returnCode);
    }

    @PostMapping("/withdrawFunds")
    ResponseEntity<String>  withdrawFunds() {

        JSONObject res  = riskManagementService.withdrawFunds();
        return ResponseEntity.ok("Return code for funds addition is  " + res);
    }




}
