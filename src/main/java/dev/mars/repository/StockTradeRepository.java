package dev.mars.repository;

import dev.mars.database.DatabaseManager;
import dev.mars.exception.ApiException;
import dev.mars.model.StockTrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository class for StockTrade data access operations
 */
@Singleton
public class StockTradeRepository {
    private static final Logger logger = LoggerFactory.getLogger(StockTradeRepository.class);
    
    private final DatabaseManager databaseManager;
    
    @Inject
    public StockTradeRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }
    
    /**
     * Find all stock trades with pagination
     */
    public List<StockTrade> findAll(int page, int size) {
        logger.debug("Finding all stock trades - page: {}, size: {}", page, size);
        
        String sql = """
            SELECT id, symbol, trade_type, quantity, price, total_value, 
                   trade_date_time, trader_id, exchange
            FROM stock_trades 
            ORDER BY trade_date_time DESC 
            LIMIT ? OFFSET ?
            """;
        
        List<StockTrade> trades = new ArrayList<>();
        int offset = page * size;
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setInt(1, size);
            statement.setInt(2, offset);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    trades.add(mapResultSetToStockTrade(resultSet));
                }
            }
            
            logger.debug("Found {} stock trades", trades.size());
            return trades;
            
        } catch (SQLException e) {
            logger.error("Failed to find stock trades", e);
            throw ApiException.internalError("Failed to retrieve stock trades", e);
        }
    }
    
    /**
     * Find stock trade by ID
     */
    public Optional<StockTrade> findById(Long id) {
        logger.debug("Finding stock trade by id: {}", id);
        
        String sql = """
            SELECT id, symbol, trade_type, quantity, price, total_value, 
                   trade_date_time, trader_id, exchange
            FROM stock_trades 
            WHERE id = ?
            """;
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setLong(1, id);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    StockTrade trade = mapResultSetToStockTrade(resultSet);
                    logger.debug("Found stock trade: {}", trade);
                    return Optional.of(trade);
                }
            }
            
            logger.debug("Stock trade not found with id: {}", id);
            return Optional.empty();
            
        } catch (SQLException e) {
            logger.error("Failed to find stock trade by id: {}", id, e);
            throw ApiException.internalError("Failed to retrieve stock trade", e);
        }
    }
    
    /**
     * Find stock trades by symbol with pagination
     */
    public List<StockTrade> findBySymbol(String symbol, int page, int size) {
        logger.debug("Finding stock trades by symbol: {} - page: {}, size: {}", symbol, page, size);
        
        String sql = """
            SELECT id, symbol, trade_type, quantity, price, total_value, 
                   trade_date_time, trader_id, exchange
            FROM stock_trades 
            WHERE symbol = ? 
            ORDER BY trade_date_time DESC 
            LIMIT ? OFFSET ?
            """;
        
        List<StockTrade> trades = new ArrayList<>();
        int offset = page * size;
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, symbol.toUpperCase());
            statement.setInt(2, size);
            statement.setInt(3, offset);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    trades.add(mapResultSetToStockTrade(resultSet));
                }
            }
            
            logger.debug("Found {} stock trades for symbol: {}", trades.size(), symbol);
            return trades;
            
        } catch (SQLException e) {
            logger.error("Failed to find stock trades by symbol: {}", symbol, e);
            throw ApiException.internalError("Failed to retrieve stock trades by symbol", e);
        }
    }
    
    /**
     * Count total number of stock trades
     */
    public long count() {
        logger.debug("Counting total stock trades");
        
        String sql = "SELECT COUNT(*) FROM stock_trades";
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            
            if (resultSet.next()) {
                long count = resultSet.getLong(1);
                logger.debug("Total stock trades count: {}", count);
                return count;
            }
            
            return 0;
            
        } catch (SQLException e) {
            logger.error("Failed to count stock trades", e);
            throw ApiException.internalError("Failed to count stock trades", e);
        }
    }
    
    /**
     * Count stock trades by symbol
     */
    public long countBySymbol(String symbol) {
        logger.debug("Counting stock trades by symbol: {}", symbol);
        
        String sql = "SELECT COUNT(*) FROM stock_trades WHERE symbol = ?";
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            statement.setString(1, symbol.toUpperCase());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    long count = resultSet.getLong(1);
                    logger.debug("Stock trades count for symbol {}: {}", symbol, count);
                    return count;
                }
            }
            
            return 0;
            
        } catch (SQLException e) {
            logger.error("Failed to count stock trades by symbol: {}", symbol, e);
            throw ApiException.internalError("Failed to count stock trades by symbol", e);
        }
    }
    
    private StockTrade mapResultSetToStockTrade(ResultSet resultSet) throws SQLException {
        StockTrade trade = new StockTrade();
        trade.setId(resultSet.getLong("id"));
        trade.setSymbol(resultSet.getString("symbol"));
        trade.setTradeType(resultSet.getString("trade_type"));
        trade.setQuantity(resultSet.getInt("quantity"));
        trade.setPrice(resultSet.getBigDecimal("price"));
        trade.setTotalValue(resultSet.getBigDecimal("total_value"));
        trade.setTradeDateTime(resultSet.getObject("trade_date_time", LocalDateTime.class));
        trade.setTraderId(resultSet.getString("trader_id"));
        trade.setExchange(resultSet.getString("exchange"));
        return trade;
    }
}
