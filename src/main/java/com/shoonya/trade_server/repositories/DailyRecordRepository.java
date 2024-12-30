package com.shoonya.trade_server.repositories;

import com.shoonya.trade_server.entity.DailyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyRecordRepository extends JpaRepository<DailyRecord, LocalDate> {
    // Using @Query for a custom SQL query
    @Query("SELECT dr FROM DailyRecord dr WHERE dr.date = :date")
    DailyRecord findRecordByDate(@Param("date") LocalDate date);

    @Query(value = "SELECT * FROM daily_record WHERE date < :today ORDER BY date DESC LIMIT 10", nativeQuery = true)
    List<DailyRecord> findLastNEntriesBeforeToday(@Param("today")  LocalDate date);

    @Query(value = "SELECT * FROM daily_record WHERE date < :today ORDER BY date DESC LIMIT 1", nativeQuery = true)
    DailyRecord findLastDay(@Param("today") LocalDate date);

    @Query("SELECT dr FROM DailyRecord dr WHERE dr.date >= :start  AND dr.date <= :end ")
    List<DailyRecord> getPnlBetweenDates(@Param("start")  LocalDate start, @Param("end")  LocalDate end);

}

