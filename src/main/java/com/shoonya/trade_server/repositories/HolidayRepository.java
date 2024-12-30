package com.shoonya.trade_server.repositories;

import com.shoonya.trade_server.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, LocalDate> {
    @Query("SELECT h.date FROM Holiday h")
    List<LocalDate> findAllHolidayDates();
}
