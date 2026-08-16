package com.marketanalyser.domain.repository;

import com.marketanalyser.domain.model.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findBySymbolIgnoreCase(String symbol);

    Optional<Stock> findBySymbolIgnoreCaseAndExchangeId(String symbol, Long exchangeId);

    @Query("""
        SELECT s FROM Stock s
        WHERE s.isActive = true
          AND (
            UPPER(s.symbol) LIKE UPPER(CONCAT(:query, '%'))
            OR UPPER(s.name) LIKE UPPER(CONCAT('%', :query, '%'))
          )
        ORDER BY
          CASE WHEN UPPER(s.symbol) = UPPER(:query) THEN 0
               WHEN UPPER(s.symbol) LIKE UPPER(CONCAT(:query, '%')) THEN 1
               ELSE 2 END,
          s.symbol
        """)
    Page<Stock> searchBySymbolOrName(@Param("query") String query, Pageable pageable);

    @Query("SELECT s FROM Stock s WHERE s.isActive = true AND s.exchange.mic = :mic ORDER BY s.symbol")
    Page<Stock> findByExchangeMic(@Param("mic") String mic, Pageable pageable);

    @Query("""
        SELECT s FROM Stock s WHERE s.isActive = true
          AND (:sector IS NULL OR s.sector = :sector)
          AND (:exchange IS NULL OR s.exchange.acronym = :exchange)
          AND (:country IS NULL OR s.country = :country)
        """)
    Page<Stock> findByScreenerCriteria(
            @Param("sector") String sector,
            @Param("exchange") String exchange,
            @Param("country") String country,
            Pageable pageable
    );

    List<Stock> findTop20ByIsActiveTrueOrderBySymbol();
}
