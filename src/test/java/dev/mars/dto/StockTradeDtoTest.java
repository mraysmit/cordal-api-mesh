package dev.mars.dto;

import dev.mars.model.StockTrade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for StockTradeDto
 */
class StockTradeDtoTest {

    private StockTrade stockTrade;
    private LocalDateTime testDateTime;

    @BeforeEach
    void setUp() {
        testDateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        stockTrade = new StockTrade(
            1L, "AAPL", "BUY", 100,
            BigDecimal.valueOf(150.50), BigDecimal.valueOf(15050.00),
            testDateTime, "TRADER_001", "NASDAQ"
        );
    }

    @Test
    void testDefaultConstructor() {
        StockTradeDto dto = new StockTradeDto();
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isNull();
        assertThat(dto.getSymbol()).isNull();
    }

    @Test
    void testConstructorFromEntity() {
        StockTradeDto dto = new StockTradeDto(stockTrade);

        assertThat(dto.getId()).isEqualTo(stockTrade.getId());
        assertThat(dto.getSymbol()).isEqualTo(stockTrade.getSymbol());
        assertThat(dto.getTradeType()).isEqualTo(stockTrade.getTradeType());
        assertThat(dto.getQuantity()).isEqualTo(stockTrade.getQuantity());
        assertThat(dto.getPrice()).isEqualTo(stockTrade.getPrice());
        assertThat(dto.getTotalValue()).isEqualTo(stockTrade.getTotalValue());
        assertThat(dto.getTradeDateTime()).isEqualTo(stockTrade.getTradeDateTime());
        assertThat(dto.getTraderId()).isEqualTo(stockTrade.getTraderId());
        assertThat(dto.getExchange()).isEqualTo(stockTrade.getExchange());
    }

    @Test
    void testFromEntityStaticMethod() {
        StockTradeDto dto = StockTradeDto.fromEntity(stockTrade);

        assertThat(dto.getId()).isEqualTo(stockTrade.getId());
        assertThat(dto.getSymbol()).isEqualTo(stockTrade.getSymbol());
        assertThat(dto.getTradeType()).isEqualTo(stockTrade.getTradeType());
        assertThat(dto.getQuantity()).isEqualTo(stockTrade.getQuantity());
        assertThat(dto.getPrice()).isEqualTo(stockTrade.getPrice());
        assertThat(dto.getTotalValue()).isEqualTo(stockTrade.getTotalValue());
        assertThat(dto.getTradeDateTime()).isEqualTo(stockTrade.getTradeDateTime());
        assertThat(dto.getTraderId()).isEqualTo(stockTrade.getTraderId());
        assertThat(dto.getExchange()).isEqualTo(stockTrade.getExchange());
    }

    @Test
    void testSettersAndGetters() {
        StockTradeDto dto = new StockTradeDto();
        
        dto.setId(2L);
        dto.setSymbol("GOOGL");
        dto.setTradeType("SELL");
        dto.setQuantity(75);
        dto.setPrice(BigDecimal.valueOf(2800.25));
        dto.setTotalValue(BigDecimal.valueOf(210018.75));
        dto.setTradeDateTime(testDateTime);
        dto.setTraderId("TRADER_002");
        dto.setExchange("NYSE");

        assertThat(dto.getId()).isEqualTo(2L);
        assertThat(dto.getSymbol()).isEqualTo("GOOGL");
        assertThat(dto.getTradeType()).isEqualTo("SELL");
        assertThat(dto.getQuantity()).isEqualTo(75);
        assertThat(dto.getPrice()).isEqualTo(BigDecimal.valueOf(2800.25));
        assertThat(dto.getTotalValue()).isEqualTo(BigDecimal.valueOf(210018.75));
        assertThat(dto.getTradeDateTime()).isEqualTo(testDateTime);
        assertThat(dto.getTraderId()).isEqualTo("TRADER_002");
        assertThat(dto.getExchange()).isEqualTo("NYSE");
    }

    @Test
    void testFromEntityWithNullValues() {
        StockTrade nullTrade = new StockTrade();
        StockTradeDto dto = StockTradeDto.fromEntity(nullTrade);

        assertThat(dto.getId()).isNull();
        assertThat(dto.getSymbol()).isNull();
        assertThat(dto.getTradeType()).isNull();
        assertThat(dto.getQuantity()).isNull();
        assertThat(dto.getPrice()).isNull();
        assertThat(dto.getTotalValue()).isNull();
        assertThat(dto.getTradeDateTime()).isNull();
        assertThat(dto.getTraderId()).isNull();
        assertThat(dto.getExchange()).isNull();
    }
}
