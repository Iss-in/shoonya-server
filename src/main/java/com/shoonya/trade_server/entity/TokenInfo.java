package com.shoonya.trade_server.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenInfo {
    private String exch;
    private String token;
    private String tsym;

    // Constructor
    public TokenInfo(String exch, String token, String tsym) {
        this.exch = exch;
        this.token = token;
        this.tsym = tsym;
    }

    // Default constructor
    public TokenInfo() {}

    public String getInstrument(){
        return exch + '|' + token;
    }
}

//#TODO: shift df logic to db, uplod entire file to db instead