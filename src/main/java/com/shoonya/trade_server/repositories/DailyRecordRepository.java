package com.shoonya.trade_server.repositories;

import com.shoonya.trade_server.entity.DailyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface DailyRecordRepository extends JpaRepository<DailyRecord, LocalDate> {
    // Using @Query for a custom SQL query
    @Query("SELECT dr FROM DailyRecord dr WHERE dr.date = :date")
    DailyRecord findRecordByDate(@Param("date") LocalDate date);
}

