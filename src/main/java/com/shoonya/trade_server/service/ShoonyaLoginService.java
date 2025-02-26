package com.shoonya.trade_server.service;

import com.shoonya.trade_server.config.ShoonyaConfig;
import com.noren.javaapi.NorenApiJava;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.apache.commons.codec.DecoderException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static com.shoonya.trade_server.service.ShoonyaLoginService.logger;
import static com.shoonya.trade_server.service.TOTPGenerator.generateTOTP;

@Service
public class ShoonyaLoginService {

    private ShoonyaConfig config;

    @Getter
    private NorenApiJava api;

    public static final Logger logger = LogManager.getLogger(ShoonyaLoginService.class.getName());

    public ShoonyaLoginService(ShoonyaConfig config){

        String classpath = System.getProperty("java.class.path");
//        logger.error(classpath);
        this.config = config;
        logger.info("initiating api login");
        this.api = loginToApi();
    }



    private NorenApiJava loginToApi(){

        String host = config.getHost();

        NorenApiJava sessionApi = new com.noren.javaapi.NorenApiJava(host);
        String totpKey = config.getTotpKey();
        logger.info("totp key is x- {}", totpKey);

        String twoFa =  generateTOTP(totpKey, 6);
        logger.info("totp is {}", twoFa );
        String response = sessionApi.login(config.getUser(), config.getPassword(), twoFa, config.getVc(), config.getApiKey(), config.getImei(),
                config.getEmailId(), config.getCliname(), config.getPrfname(), config.getAcctnum(),
                config.getBankn(), config.getIfscCode(), config.getUpi());

        logger.info("login response is {}", response );
        JSONObject res = new JSONObject(response);
        if(res.get("stat").equals("Not_Ok")){
            logger.info("login unsuccessful with error:{}", res.get("emsg"));
            System.exit(1);
        }
        return sessionApi;
    }
}



class TOTPGenerator {

    private static final int TIME_STEP_SECONDS = 30; // Default time step for TOTP
    private static final String HMAC_ALGORITHM = "HmacSHA1"; // SHA-1 is the default for TOTP

    private TOTPGenerator() {}

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


