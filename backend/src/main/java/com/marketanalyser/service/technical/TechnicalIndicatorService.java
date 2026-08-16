package com.marketanalyser.service.technical;

import com.marketanalyser.shared.model.HistoricalCandle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Technical indicator calculations performed server-side on cached historical data.
 * All calculations use cached price data — NO additional Marketstack calls.
 */
@Service
@Slf4j
public class TechnicalIndicatorService {

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    // --- Moving Averages ---

    public List<BigDecimal> sma(List<HistoricalCandle> candles, int period) {
        List<BigDecimal> result = new ArrayList<>();
        List<BigDecimal> closes = closes(candles);

        for (int i = 0; i < closes.size(); i++) {
            if (i < period - 1) {
                result.add(null);
            } else {
                BigDecimal sum = BigDecimal.ZERO;
                for (int j = i - period + 1; j <= i; j++) {
                    sum = sum.add(closes.get(j));
                }
                result.add(sum.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP));
            }
        }
        return result;
    }

    public List<BigDecimal> ema(List<HistoricalCandle> candles, int period) {
        List<BigDecimal> result = new ArrayList<>();
        List<BigDecimal> closes = closes(candles);

        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (period + 1));
        BigDecimal ema = null;

        for (int i = 0; i < closes.size(); i++) {
            if (i < period - 1) {
                result.add(null);
            } else if (i == period - 1) {
                // First EMA = SMA
                BigDecimal sum = BigDecimal.ZERO;
                for (int j = 0; j < period; j++) sum = sum.add(closes.get(j));
                ema = sum.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
                result.add(ema);
            } else {
                ema = closes.get(i).subtract(ema).multiply(multiplier).add(ema);
                result.add(ema.setScale(4, RoundingMode.HALF_UP));
            }
        }
        return result;
    }

    // --- RSI ---

    public List<BigDecimal> rsi(List<HistoricalCandle> candles, int period) {
        List<BigDecimal> result = new ArrayList<>();
        List<BigDecimal> closes = closes(candles);

        BigDecimal avgGain = BigDecimal.ZERO;
        BigDecimal avgLoss = BigDecimal.ZERO;

        for (int i = 0; i < closes.size(); i++) {
            if (i == 0) { result.add(null); continue; }

            BigDecimal change = closes.get(i).subtract(closes.get(i - 1));
            BigDecimal gain = change.compareTo(BigDecimal.ZERO) > 0 ? change : BigDecimal.ZERO;
            BigDecimal loss = change.compareTo(BigDecimal.ZERO) < 0 ? change.abs() : BigDecimal.ZERO;

            if (i < period) {
                avgGain = avgGain.add(gain);
                avgLoss = avgLoss.add(loss);
                result.add(null);
            } else if (i == period) {
                avgGain = avgGain.divide(BigDecimal.valueOf(period), 6, RoundingMode.HALF_UP);
                avgLoss = avgLoss.divide(BigDecimal.valueOf(period), 6, RoundingMode.HALF_UP);
                result.add(calculateRsiValue(avgGain, avgLoss));
            } else {
                avgGain = avgGain.multiply(BigDecimal.valueOf(period - 1)).add(gain)
                        .divide(BigDecimal.valueOf(period), 6, RoundingMode.HALF_UP);
                avgLoss = avgLoss.multiply(BigDecimal.valueOf(period - 1)).add(loss)
                        .divide(BigDecimal.valueOf(period), 6, RoundingMode.HALF_UP);
                result.add(calculateRsiValue(avgGain, avgLoss));
            }
        }
        return result;
    }

    private BigDecimal calculateRsiValue(BigDecimal avgGain, BigDecimal avgLoss) {
        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) return new BigDecimal("100");
        BigDecimal rs = avgGain.divide(avgLoss, 6, RoundingMode.HALF_UP);
        return new BigDecimal("100").subtract(
                new BigDecimal("100").divide(BigDecimal.ONE.add(rs), 2, RoundingMode.HALF_UP)
        );
    }

    // --- MACD ---

    public record MACDResult(List<BigDecimal> macd, List<BigDecimal> signal, List<BigDecimal> histogram) {}

    public MACDResult macd(List<HistoricalCandle> candles, int fastPeriod, int slowPeriod, int signalPeriod) {
        List<BigDecimal> fastEma = ema(candles, fastPeriod);
        List<BigDecimal> slowEma = ema(candles, slowPeriod);

        List<BigDecimal> macdLine = new ArrayList<>();
        for (int i = 0; i < candles.size(); i++) {
            if (fastEma.get(i) == null || slowEma.get(i) == null) {
                macdLine.add(null);
            } else {
                macdLine.add(fastEma.get(i).subtract(slowEma.get(i)).setScale(4, RoundingMode.HALF_UP));
            }
        }

        // Signal line = EMA of MACD line (using non-null values)
        List<BigDecimal> signalLine = emaOfValues(macdLine, signalPeriod);

        List<BigDecimal> histogram = new ArrayList<>();
        for (int i = 0; i < macdLine.size(); i++) {
            if (macdLine.get(i) == null || signalLine.get(i) == null) {
                histogram.add(null);
            } else {
                histogram.add(macdLine.get(i).subtract(signalLine.get(i)).setScale(4, RoundingMode.HALF_UP));
            }
        }

        return new MACDResult(macdLine, signalLine, histogram);
    }

    // --- Bollinger Bands ---

    public record BollingerBands(List<BigDecimal> upper, List<BigDecimal> middle, List<BigDecimal> lower) {}

    public BollingerBands bollingerBands(List<HistoricalCandle> candles, int period, double stdMultiplier) {
        List<BigDecimal> middle = sma(candles, period);
        List<BigDecimal> upper = new ArrayList<>();
        List<BigDecimal> lower = new ArrayList<>();
        List<BigDecimal> closes = closes(candles);

        for (int i = 0; i < closes.size(); i++) {
            if (middle.get(i) == null) {
                upper.add(null);
                lower.add(null);
            } else {
                BigDecimal std = stdDev(closes, i, period, middle.get(i));
                BigDecimal band = std.multiply(BigDecimal.valueOf(stdMultiplier)).setScale(4, RoundingMode.HALF_UP);
                upper.add(middle.get(i).add(band));
                lower.add(middle.get(i).subtract(band));
            }
        }
        return new BollingerBands(upper, middle, lower);
    }

    // --- ATR ---

    public List<BigDecimal> atr(List<HistoricalCandle> candles, int period) {
        List<BigDecimal> result = new ArrayList<>();
        result.add(null);

        List<BigDecimal> trueRanges = new ArrayList<>();
        for (int i = 1; i < candles.size(); i++) {
            BigDecimal high = candles.get(i).getHigh();
            BigDecimal low = candles.get(i).getLow();
            BigDecimal prevClose = candles.get(i - 1).getClose();

            BigDecimal tr = high.subtract(low).abs()
                    .max(high.subtract(prevClose).abs())
                    .max(low.subtract(prevClose).abs());
            trueRanges.add(tr);
        }

        BigDecimal atr = null;
        for (int i = 0; i < trueRanges.size(); i++) {
            if (i < period - 1) {
                result.add(null);
            } else if (i == period - 1) {
                BigDecimal sum = BigDecimal.ZERO;
                for (int j = 0; j < period; j++) sum = sum.add(trueRanges.get(j));
                atr = sum.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
                result.add(atr);
            } else {
                atr = atr.multiply(BigDecimal.valueOf(period - 1)).add(trueRanges.get(i))
                        .divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
                result.add(atr);
            }
        }
        return result;
    }

    // --- Helper methods ---

    private List<BigDecimal> closes(List<HistoricalCandle> candles) {
        return candles.stream().map(HistoricalCandle::getClose).toList();
    }

    private BigDecimal stdDev(List<BigDecimal> values, int endIdx, int period, BigDecimal mean) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = endIdx - period + 1; i <= endIdx; i++) {
            BigDecimal diff = values.get(i).subtract(mean);
            sum = sum.add(diff.multiply(diff));
        }
        double variance = sum.divide(BigDecimal.valueOf(period), 10, RoundingMode.HALF_UP).doubleValue();
        return BigDecimal.valueOf(Math.sqrt(variance)).setScale(6, RoundingMode.HALF_UP);
    }

    private List<BigDecimal> emaOfValues(List<BigDecimal> values, int period) {
        List<BigDecimal> result = new ArrayList<>();
        BigDecimal multiplier = BigDecimal.valueOf(2.0 / (period + 1));
        BigDecimal ema = null;
        int nonNullCount = 0;
        BigDecimal sumForSeed = BigDecimal.ZERO;

        for (BigDecimal val : values) {
            if (val == null) { result.add(null); continue; }
            if (ema == null) {
                nonNullCount++;
                sumForSeed = sumForSeed.add(val);
                if (nonNullCount == period) {
                    ema = sumForSeed.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
                    result.add(ema);
                } else {
                    result.add(null);
                }
            } else {
                ema = val.subtract(ema).multiply(multiplier).add(ema);
                result.add(ema.setScale(4, RoundingMode.HALF_UP));
            }
        }
        return result;
    }
}
