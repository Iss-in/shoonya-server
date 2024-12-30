package com.shoonya.trade_server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

import java.time.LocalDate;
@Getter
@Entity
public class Holiday {

    @Id
    @Column(name="date")
    private LocalDate date;

    @Column(name="description")
    private String description;

}
