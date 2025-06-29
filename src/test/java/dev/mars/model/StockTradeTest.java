package dev.mars.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for StockTrade model
 */
public class StockTradeTest {

    private StockTrade stockTrade;
    private LocalDateTime testDateTime;

    @BeforeEach
    void setUp() {
        testDateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        stockTrade = new StockTrade();
    }

    @Test
    void testDefaultConstructor() {
        StockTrade trade = new StockTrade();
        assertThat(trade).isNotNull();
        assertThat(trade.getId()).isNull();
        assertThat(trade.getSymbol()).isNull();
    }

    @Test
    void testParameterizedConstructor() {
        StockTrade trade = new StockTrade(
            1L, "AAPL", "BUY", 100, 
            BigDecimal.valueOf(150.50), BigDecimal.valueOf(15050.00),
            testDateTime, "TRADER_001", "NASDAQ"
        );

        assertThat(trade.getId()).isEqualTo(1L);
        assertThat(trade.getSymbol()).isEqualTo("AAPL");
        assertThat(trade.getTradeType()).isEqualTo("BUY");
        assertThat(trade.getQuantity()).isEqualTo(100);
        assertThat(trade.getPrice()).isEqualTo(BigDecimal.valueOf(150.50));
        assertThat(trade.getTotalValue()).isEqualTo(BigDecimal.valueOf(15050.00));
        assertThat(trade.getTradeDateTime()).isEqualTo(testDateTime);
        assertThat(trade.getTraderId()).isEqualTo("TRADER_001");
        assertThat(trade.getExchange()).isEqualTo("NASDAQ");
    }

    @Test
    void testSettersAndGetters() {
        stockTrade.setId(1L);
        stockTrade.setSymbol("GOOGL");
        stockTrade.setTradeType("SELL");
        stockTrade.setQuantity(50);
        stockTrade.setPrice(BigDecimal.valueOf(2500.75));
        stockTrade.setTotalValue(BigDecimal.valueOf(125037.50));
        stockTrade.setTradeDateTime(testDateTime);
        stockTrade.setTraderId("TRADER_002");
        stockTrade.setExchange("NYSE");

        assertThat(stockTrade.getId()).isEqualTo(1L);
        assertThat(stockTrade.getSymbol()).isEqualTo("GOOGL");
        assertThat(stockTrade.getTradeType()).isEqualTo("SELL");
        assertThat(stockTrade.getQuantity()).isEqualTo(50);
        assertThat(stockTrade.getPrice()).isEqualTo(BigDecimal.valueOf(2500.75));
        assertThat(stockTrade.getTotalValue()).isEqualTo(BigDecimal.valueOf(125037.50));
        assertThat(stockTrade.getTradeDateTime()).isEqualTo(testDateTime);
        assertThat(stockTrade.getTraderId()).isEqualTo("TRADER_002");
        assertThat(stockTrade.getExchange()).isEqualTo("NYSE");
    }

    @Test
    void testEqualsAndHashCode() {
        StockTrade trade1 = new StockTrade();
        trade1.setId(1L);
        trade1.setSymbol("AAPL");

        StockTrade trade2 = new StockTrade();
        trade2.setId(1L);
        trade2.setSymbol("GOOGL"); // Different symbol, but same ID

        StockTrade trade3 = new StockTrade();
        trade3.setId(2L);
        trade3.setSymbol("AAPL");

        // Same ID should be equal
        assertThat(trade1).isEqualTo(trade2);
        assertThat(trade1.hashCode()).isEqualTo(trade2.hashCode());

        // Different ID should not be equal
        assertThat(trade1).isNotEqualTo(trade3);
        assertThat(trade1.hashCode()).isNotEqualTo(trade3.hashCode());

        // Null comparison
        assertThat(trade1).isNotEqualTo(null);

        // Self comparison
        assertThat(trade1).isEqualTo(trade1);
    }

    @Test
    void testToString() {
        stockTrade.setId(1L);
        stockTrade.setSymbol("AAPL");
        stockTrade.setTradeType("BUY");
        stockTrade.setQuantity(100);
        stockTrade.setPrice(BigDecimal.valueOf(150.50));
        stockTrade.setTotalValue(BigDecimal.valueOf(15050.00));
        stockTrade.setTradeDateTime(testDateTime);
        stockTrade.setTraderId("TRADER_001");
        stockTrade.setExchange("NASDAQ");

        String toString = stockTrade.toString();
        
        assertThat(toString).contains("StockTrade{");
        assertThat(toString).contains("id=1");
        assertThat(toString).contains("symbol='AAPL'");
        assertThat(toString).contains("tradeType='BUY'");
        assertThat(toString).contains("quantity=100");
        assertThat(toString).contains("price=150.5");
        assertThat(toString).contains("totalValue=15050.0"); // BigDecimal toString doesn't include trailing zeros
        assertThat(toString).contains("traderId='TRADER_001'");
        assertThat(toString).contains("exchange='NASDAQ'");
    }
}
