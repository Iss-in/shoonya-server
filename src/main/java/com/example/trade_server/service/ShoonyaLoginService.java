package com.example.trade_server.service;

import com.example.trade_server.config.ShoonyaConfig;
import com.noren.javaapi.NorenApiJava;
import org.apache.commons.codec.DecoderException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class ShoonyaLoginService {

    @Autowired
    private ShoonyaConfig config;

    private NorenApiJava api;
    public JSONObject shoonyaLoginState;

//    ShoonyaLoginService (){
//        feedOpened = Boolean.FALSE;
//    }
    public static Logger logger = LogManager.getLogger(ShoonyaLoginService.class.getName());
//    TODO: add logging statements in this class
    public NorenApiJava login(String host, String websocket){
        api = new NorenApiJava(host, websocket);
        String totp_key = config.getTotpKey();

        TOTPGenerator totpGenerator = new TOTPGenerator();
        String twoFa =  totpGenerator.generateTOTP(totp_key, 6);
        String response = api.login(config.getUser(), config.getPassword(), twoFa, config.getVc(), config.getApiKey(), config.getImei(),
                config.getEmailId(), config.getCliname(), config.getPrfname(), config.getAcctnum(),
                config.getBankn(), config.getIfscCode());

        System.out.println("response is " + response );

//        shoonyaLoginState.put(api.actid,
        return api;
    }

}

class TOTPGenerator {

    private static final int TIME_STEP_SECONDS = 30; // Default time step for TOTP
    private static final String HMAC_ALGORITHM = "HmacSHA1"; // SHA-1 is the default for TOTP

    public static String generateTOTP(String secretKey, int digits) {
        try {
            // Decode the base32-encoded secret key
            Base32 base32 = new Base32();
            byte[] keyBytes = base32.decode(secretKey);

            // Get the current time step
            long timeStep = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS;

            // Generate the TOTP
            byte[] data = Hex.decodeHex(String.format("%016x", timeStep).toCharArray());
            SecretKeySpec signKey = new SecretKeySpec(keyBytes, HMAC_ALGORITHM);

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(signKey);
            byte[] hash = mac.doFinal(data);

            // Extract the dynamic offset
            int offset = hash[hash.length - 1] & 0x0F;

            // Calculate the binary code (dynamic truncation)
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            // Generate the TOTP value
            int otp = binary % (int) Math.pow(10, digits);
            return String.format("%0" + digits + "d", otp);
        } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalArgumentException |
                 ArrayIndexOutOfBoundsException | DecoderException ex) {
            throw new RuntimeException("Error generating TOTP", ex);
        }
    }

}


