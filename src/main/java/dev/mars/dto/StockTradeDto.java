package dev.mars.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import dev.mars.model.StockTrade;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for StockTrade
 */
public class StockTradeDto {
    private Long id;
    private String symbol;
    private String tradeType;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalValue;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime tradeDateTime;
    
    private String traderId;
    private String exchange;

    // Default constructor
    public StockTradeDto() {}

    // Constructor from StockTrade entity
    public StockTradeDto(StockTrade stockTrade) {
        this.id = stockTrade.getId();
        this.symbol = stockTrade.getSymbol();
        this.tradeType = stockTrade.getTradeType();
        this.quantity = stockTrade.getQuantity();
        this.price = stockTrade.getPrice();
        this.totalValue = stockTrade.getTotalValue();
        this.tradeDateTime = stockTrade.getTradeDateTime();
        this.traderId = stockTrade.getTraderId();
        this.exchange = stockTrade.getExchange();
    }

    // Static factory method
    public static StockTradeDto fromEntity(StockTrade stockTrade) {
        return new StockTradeDto(stockTrade);
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
}
