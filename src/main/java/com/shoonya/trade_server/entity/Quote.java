package com.shoonya.trade_server.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
public class Quote {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(name="quote")
    private String quote;

}
