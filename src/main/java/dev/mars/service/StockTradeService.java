package dev.mars.service;

import dev.mars.dto.PagedResponse;
import dev.mars.dto.StockTradeDto;
import dev.mars.exception.ApiException;
import dev.mars.model.StockTrade;
import dev.mars.repository.StockTradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * Service class for StockTrade business logic
 */
@Singleton
public class StockTradeService {
    private static final Logger logger = LoggerFactory.getLogger(StockTradeService.class);
    
    private final StockTradeRepository stockTradeRepository;
    private final Executor executor;
    
    @Inject
    public StockTradeService(StockTradeRepository stockTradeRepository) {
        this.stockTradeRepository = stockTradeRepository;
        this.executor = ForkJoinPool.commonPool();
    }
    
    /**
     * Get all stock trades with pagination (synchronous)
     */
    public PagedResponse<StockTradeDto> getAllStockTrades(int page, int size) {
        logger.debug("Getting all stock trades - page: {}, size: {}", page, size);
        
        validatePaginationParameters(page, size);
        
        List<StockTrade> trades = stockTradeRepository.findAll(page, size);
        long totalElements = stockTradeRepository.count();
        
        List<StockTradeDto> tradeDtos = trades.stream()
                .map(StockTradeDto::fromEntity)
                .collect(Collectors.toList());
        
        logger.debug("Retrieved {} stock trades out of {} total", tradeDtos.size(), totalElements);
        
        return PagedResponse.of(tradeDtos, page, size, totalElements);
    }
    
    /**
     * Get all stock trades with pagination (asynchronous)
     */
    public CompletableFuture<PagedResponse<StockTradeDto>> getAllStockTradesAsync(int page, int size) {
        logger.debug("Getting all stock trades asynchronously - page: {}, size: {}", page, size);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getAllStockTrades(page, size);
            } catch (Exception e) {
                logger.error("Error getting stock trades asynchronously", e);
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    /**
     * Get stock trade by ID (synchronous)
     */
    public StockTradeDto getStockTradeById(Long id) {
        logger.debug("Getting stock trade by id: {}", id);
        
        if (id == null || id <= 0) {
            throw ApiException.badRequest("Invalid stock trade ID");
        }
        
        return stockTradeRepository.findById(id)
                .map(StockTradeDto::fromEntity)
                .orElseThrow(() -> ApiException.notFound("Stock trade not found with ID: " + id));
    }
    
    /**
     * Get stock trade by ID (asynchronous)
     */
    public CompletableFuture<StockTradeDto> getStockTradeByIdAsync(Long id) {
        logger.debug("Getting stock trade by id asynchronously: {}", id);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getStockTradeById(id);
            } catch (Exception e) {
                logger.error("Error getting stock trade by id asynchronously: {}", id, e);
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    /**
     * Get stock trades by symbol with pagination (synchronous)
     */
    public PagedResponse<StockTradeDto> getStockTradesBySymbol(String symbol, int page, int size) {
        logger.debug("Getting stock trades by symbol: {} - page: {}, size: {}", symbol, page, size);
        
        validatePaginationParameters(page, size);
        validateSymbol(symbol);
        
        List<StockTrade> trades = stockTradeRepository.findBySymbol(symbol, page, size);
        long totalElements = stockTradeRepository.countBySymbol(symbol);
        
        List<StockTradeDto> tradeDtos = trades.stream()
                .map(StockTradeDto::fromEntity)
                .collect(Collectors.toList());
        
        logger.debug("Retrieved {} stock trades for symbol {} out of {} total", 
                    tradeDtos.size(), symbol, totalElements);
        
        return PagedResponse.of(tradeDtos, page, size, totalElements);
    }
    
    /**
     * Get stock trades by symbol with pagination (asynchronous)
     */
    public CompletableFuture<PagedResponse<StockTradeDto>> getStockTradesBySymbolAsync(String symbol, int page, int size) {
        logger.debug("Getting stock trades by symbol asynchronously: {} - page: {}, size: {}", symbol, page, size);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getStockTradesBySymbol(symbol, page, size);
            } catch (Exception e) {
                logger.error("Error getting stock trades by symbol asynchronously: {}", symbol, e);
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    /**
     * Get health status of the service
     */
    public CompletableFuture<Boolean> getHealthStatusAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simple health check by counting records
                stockTradeRepository.count();
                return true;
            } catch (Exception e) {
                logger.error("Health check failed", e);
                return false;
            }
        }, executor);
    }
    
    private void validatePaginationParameters(int page, int size) {
        if (page < 0) {
            throw ApiException.badRequest("Page number cannot be negative");
        }
        
        if (size <= 0) {
            throw ApiException.badRequest("Page size must be positive");
        }
        
        if (size > 1000) {
            throw ApiException.badRequest("Page size cannot exceed 1000");
        }
    }
    
    private void validateSymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw ApiException.badRequest("Symbol cannot be null or empty");
        }
        
        if (symbol.length() > 10) {
            throw ApiException.badRequest("Symbol cannot exceed 10 characters");
        }
        
        if (!symbol.matches("^[A-Za-z]+$")) {
            throw ApiException.badRequest("Symbol can only contain letters");
        }
    }
}
