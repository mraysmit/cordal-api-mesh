package dev.mars.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Stock Trade entity representing a stock trading transaction
 */
public class StockTrade {
    private Long id;
    private String symbol;
    private String tradeType; // BUY or SELL
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalValue;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime tradeDateTime;
    
    private String traderId;
    private String exchange;

    // Default constructor
    public StockTrade() {}

    // Constructor with all fields
    public StockTrade(Long id, String symbol, String tradeType, Integer quantity, 
                     BigDecimal price, BigDecimal totalValue, LocalDateTime tradeDateTime, 
                     String traderId, String exchange) {
        this.id = id;
        this.symbol = symbol;
        this.tradeType = tradeType;
        this.quantity = quantity;
        this.price = price;
        this.totalValue = totalValue;
        this.tradeDateTime = tradeDateTime;
        this.traderId = traderId;
        this.exchange = exchange;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getTradeType() {
        return tradeType;
    }

    public void setTradeType(String tradeType) {
        this.tradeType = tradeType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(BigDecimal totalValue) {
        this.totalValue = totalValue;
    }

    public LocalDateTime getTradeDateTime() {
        return tradeDateTime;
    }

    public void setTradeDateTime(LocalDateTime tradeDateTime) {
        this.tradeDateTime = tradeDateTime;
    }

    public String getTraderId() {
        return traderId;
    }

    public void setTraderId(String traderId) {
        this.traderId = traderId;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockTrade that = (StockTrade) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "StockTrade{" +
                "id=" + id +
                ", symbol='" + symbol + '\'' +
                ", tradeType='" + tradeType + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", totalValue=" + totalValue +
                ", tradeDateTime=" + tradeDateTime +
                ", traderId='" + traderId + '\'' +
                ", exchange='" + exchange + '\'' +
                '}';
    }
}
