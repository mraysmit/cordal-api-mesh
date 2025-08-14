-- EXAMPLE IMPLEMENTATION: Initialize stocktrades H2 database for integration testing
-- This script provides example database initialization for stock trading data
-- This is NOT part of the core system and should only be used for integration testing

-- Drop existing tables if they exist
DROP TABLE IF EXISTS stock_trades;

-- Create stock_trades table
CREATE TABLE IF NOT EXISTS stock_trades (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(10) NOT NULL,
    trade_type VARCHAR(4) NOT NULL CHECK (trade_type IN ('BUY', 'SELL')),
    quantity INTEGER NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    total_value DECIMAL(15,2) NOT NULL,
    trade_date_time TIMESTAMP NOT NULL,
    trader_id VARCHAR(50) NOT NULL,
    exchange VARCHAR(20) NOT NULL
);

-- Insert sample data for stock_trades - EXAMPLE DATA ONLY
INSERT INTO stock_trades (symbol, trade_type, quantity, price, total_value, trade_date_time, trader_id, exchange) VALUES
('AAPL', 'BUY', 100, 150.25, 15025.00, '2024-01-15 09:30:00', 'TRADER_001', 'NASDAQ'),
('GOOGL', 'BUY', 50, 2800.50, 140025.00, '2024-01-15 09:35:00', 'TRADER_002', 'NASDAQ'),
('MSFT', 'SELL', 75, 380.75, 28556.25, '2024-01-15 09:40:00', 'TRADER_001', 'NASDAQ'),
('TSLA', 'BUY', 200, 245.80, 49160.00, '2024-01-15 09:45:00', 'TRADER_003', 'NASDAQ'),
('AMZN', 'BUY', 30, 3200.25, 96007.50, '2024-01-15 09:50:00', 'TRADER_002', 'NASDAQ'),
('AAPL', 'SELL', 50, 151.00, 7550.00, '2024-01-15 10:00:00', 'TRADER_004', 'NASDAQ'),
('GOOGL', 'SELL', 25, 2805.75, 70143.75, '2024-01-15 10:30:00', 'TRADER_001', 'NASDAQ'),
('MSFT', 'BUY', 100, 382.00, 38200.00, '2024-01-15 11:15:00', 'TRADER_003', 'NASDAQ'),
('TSLA', 'SELL', 150, 248.50, 37275.00, '2024-01-15 12:00:00', 'TRADER_002', 'NASDAQ'),
('AMZN', 'SELL', 15, 3195.00, 47925.00, '2024-01-15 13:45:00', 'TRADER_004', 'NASDAQ'),
('NVDA', 'BUY', 80, 520.25, 41620.00, '2024-01-15 14:20:00', 'TRADER_001', 'NASDAQ'),
('META', 'BUY', 60, 350.75, 21045.00, '2024-01-15 15:10:00', 'TRADER_003', 'NASDAQ'),
('NFLX', 'SELL', 40, 480.50, 19220.00, '2024-01-15 16:00:00', 'TRADER_002', 'NASDAQ'),
('AMD', 'BUY', 120, 125.00, 15000.00, '2024-01-16 09:15:00', 'TRADER_004', 'NASDAQ'),
('INTC', 'BUY', 200, 45.25, 9050.00, '2024-01-16 10:30:00', 'TRADER_001', 'NASDAQ');

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_stock_trades_symbol ON stock_trades(symbol);
CREATE INDEX IF NOT EXISTS idx_stock_trades_trader ON stock_trades(trader_id);
CREATE INDEX IF NOT EXISTS idx_stock_trades_date ON stock_trades(trade_date_time);
CREATE INDEX IF NOT EXISTS idx_stock_trades_type ON stock_trades(trade_type);

-- Display summary
SELECT 'Example stock_trades table initialized with ' || COUNT(*) || ' sample records' as summary FROM stock_trades;
