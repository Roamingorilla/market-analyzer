package com.marketanalyser.domain.repository;

import com.marketanalyser.domain.model.HistoricalPrice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HistoricalPriceRepository extends JpaRepository<HistoricalPrice, Long> {

    Optional<HistoricalPrice> findByStockIdAndDate(Long stockId, LocalDate date);

    List<HistoricalPrice> findByStockIdAndDateBetweenOrderByDateAsc(Long stockId, LocalDate from, LocalDate to);

    @Query("SELECT h FROM HistoricalPrice h WHERE h.stock.id = :stockId ORDER BY h.date DESC")
    Page<HistoricalPrice> findLatestByStockId(@Param("stockId") Long stockId, Pageable pageable);

    @Query("SELECT h FROM HistoricalPrice h WHERE h.stock.id = :stockId ORDER BY h.date DESC LIMIT 1")
    Optional<HistoricalPrice> findMostRecentByStockId(@Param("stockId") Long stockId);

    @Query("SELECT h FROM HistoricalPrice h WHERE h.stock.id = :stockId ORDER BY h.date ASC LIMIT 1")
    Optional<HistoricalPrice> findOldestByStockId(@Param("stockId") Long stockId);

    @Query("SELECT COUNT(h) FROM HistoricalPrice h WHERE h.stock.id = :stockId")
    long countByStockId(@Param("stockId") Long stockId);

    @Query("SELECT MAX(h.date) FROM HistoricalPrice h WHERE h.stock.id = :stockId")
    Optional<LocalDate> findMaxDateByStockId(@Param("stockId") Long stockId);
}
