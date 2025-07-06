-- Initialize H2 Databases with Required Tables and Sample Data
-- This script creates the necessary tables and inserts sample data for the H2 databases

-- ============================================================================
-- STOCKTRADES DATABASE INITIALIZATION
-- ============================================================================

-- Connect to stocktrades database
-- CREATE SCHEMA IF NOT EXISTS stocktrades;

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

-- Insert sample data for stock_trades
INSERT INTO stock_trades (symbol, trade_type, quantity, price, total_value, trade_date_time, trader_id, exchange) VALUES
('AAPL', 'BUY', 100, 150.25, 15025.00, '2024-01-15 09:30:00', 'TRADER_001', 'NASDAQ'),
('GOOGL', 'BUY', 50, 2800.50, 140025.00, '2024-01-15 09:35:00', 'TRADER_002', 'NASDAQ'),
('MSFT', 'SELL', 75, 380.75, 28556.25, '2024-01-15 09:40:00', 'TRADER_001', 'NASDAQ'),
('TSLA', 'BUY', 200, 245.80, 49160.00, '2024-01-15 09:45:00', 'TRADER_003', 'NASDAQ'),
('AMZN', 'BUY', 30, 3200.25, 96007.50, '2024-01-15 09:50:00', 'TRADER_002', 'NASDAQ'),
('AAPL', 'SELL', 50, 151.00, 7550.00, '2024-01-15 10:00:00', 'TRADER_004', 'NASDAQ'),
('GOOGL', 'BUY', 25, 2805.75, 70143.75, '2024-01-15 10:05:00', 'TRADER_001', 'NASDAQ'),
('MSFT', 'BUY', 100, 382.50, 38250.00, '2024-01-15 10:10:00', 'TRADER_003', 'NASDAQ'),
('TSLA', 'SELL', 150, 247.25, 37087.50, '2024-01-15 10:15:00', 'TRADER_002', 'NASDAQ'),
('AMZN', 'SELL', 20, 3205.00, 64100.00, '2024-01-15 10:20:00', 'TRADER_004', 'NASDAQ'),
('NVDA', 'BUY', 80, 875.50, 70040.00, '2024-01-15 10:25:00', 'TRADER_001', 'NASDAQ'),
('META', 'BUY', 60, 485.25, 29115.00, '2024-01-15 10:30:00', 'TRADER_003', 'NASDAQ'),
('NFLX', 'SELL', 40, 520.75, 20830.00, '2024-01-15 10:35:00', 'TRADER_002', 'NASDAQ'),
('ORCL', 'BUY', 120, 115.80, 13896.00, '2024-01-15 10:40:00', 'TRADER_004', 'NYSE'),
('CRM', 'BUY', 90, 265.50, 23895.00, '2024-01-15 10:45:00', 'TRADER_001', 'NYSE'),
('AAPL', 'BUY', 75, 152.25, 11418.75, '2024-01-15 11:00:00', 'TRADER_002', 'NASDAQ'),
('GOOGL', 'SELL', 35, 2810.00, 98350.00, '2024-01-15 11:05:00', 'TRADER_003', 'NASDAQ'),
('MSFT', 'BUY', 85, 383.75, 32618.75, '2024-01-15 11:10:00', 'TRADER_004', 'NASDAQ'),
('TSLA', 'BUY', 110, 248.50, 27335.00, '2024-01-15 11:15:00', 'TRADER_001', 'NASDAQ'),
('AMZN', 'BUY', 45, 3210.75, 144483.75, '2024-01-15 11:20:00', 'TRADER_002', 'NASDAQ');

-- ============================================================================
-- ANALYTICS DATABASE INITIALIZATION
-- ============================================================================

-- Create trades table for analytics
CREATE TABLE IF NOT EXISTS trades (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(10) NOT NULL,
    trade_type VARCHAR(4) NOT NULL CHECK (trade_type IN ('BUY', 'SELL')),
    quantity INTEGER NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    total_value DECIMAL(15,2) NOT NULL,
    trade_date TIMESTAMP NOT NULL,
    trader_id VARCHAR(50) NOT NULL,
    exchange VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert sample data for analytics trades
INSERT INTO trades (symbol, trade_type, quantity, price, total_value, trade_date, trader_id, exchange) VALUES
('AAPL', 'BUY', 1000, 150.25, 150250.00, '2024-01-01 09:30:00', 'TRADER_001', 'NASDAQ'),
('GOOGL', 'BUY', 500, 2800.50, 1400250.00, '2024-01-01 09:35:00', 'TRADER_002', 'NASDAQ'),
('MSFT', 'SELL', 750, 380.75, 285562.50, '2024-01-01 09:40:00', 'TRADER_001', 'NASDAQ'),
('TSLA', 'BUY', 2000, 245.80, 491600.00, '2024-01-01 09:45:00', 'TRADER_003', 'NASDAQ'),
('AMZN', 'BUY', 300, 3200.25, 960075.00, '2024-01-01 09:50:00', 'TRADER_002', 'NASDAQ'),
('AAPL', 'SELL', 500, 151.00, 75500.00, '2024-01-02 10:00:00', 'TRADER_004', 'NASDAQ'),
('GOOGL', 'BUY', 250, 2805.75, 701437.50, '2024-01-02 10:05:00', 'TRADER_001', 'NASDAQ'),
('MSFT', 'BUY', 1000, 382.50, 382500.00, '2024-01-02 10:10:00', 'TRADER_003', 'NASDAQ'),
('TSLA', 'SELL', 1500, 247.25, 370875.00, '2024-01-02 10:15:00', 'TRADER_002', 'NASDAQ'),
('AMZN', 'SELL', 200, 3205.00, 641000.00, '2024-01-02 10:20:00', 'TRADER_004', 'NASDAQ'),
('NVDA', 'BUY', 800, 875.50, 700400.00, '2024-01-03 10:25:00', 'TRADER_001', 'NASDAQ'),
('META', 'BUY', 600, 485.25, 291150.00, '2024-01-03 10:30:00', 'TRADER_003', 'NASDAQ'),
('NFLX', 'SELL', 400, 520.75, 208300.00, '2024-01-03 10:35:00', 'TRADER_002', 'NASDAQ'),
('ORCL', 'BUY', 1200, 115.80, 138960.00, '2024-01-03 10:40:00', 'TRADER_004', 'NYSE'),
('CRM', 'BUY', 900, 265.50, 238950.00, '2024-01-03 10:45:00', 'TRADER_001', 'NYSE'),
('AAPL', 'BUY', 750, 152.25, 114187.50, '2024-01-04 11:00:00', 'TRADER_002', 'NASDAQ'),
('GOOGL', 'SELL', 350, 2810.00, 983500.00, '2024-01-04 11:05:00', 'TRADER_003', 'NASDAQ'),
('MSFT', 'BUY', 850, 383.75, 326187.50, '2024-01-04 11:10:00', 'TRADER_004', 'NASDAQ'),
('TSLA', 'BUY', 1100, 248.50, 273350.00, '2024-01-04 11:15:00', 'TRADER_001', 'NASDAQ'),
('AMZN', 'BUY', 450, 3210.75, 1444837.50, '2024-01-04 11:20:00', 'TRADER_002', 'NASDAQ');

-- Create market_summary table for datawarehouse
CREATE TABLE IF NOT EXISTS market_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    summary_date DATE NOT NULL,
    total_volume BIGINT NOT NULL,
    total_trades INTEGER NOT NULL,
    avg_trade_value DECIMAL(15,2) NOT NULL,
    top_symbol VARCHAR(10) NOT NULL,
    top_symbol_volume BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert sample market summary data
INSERT INTO market_summary (summary_date, total_volume, total_trades, avg_trade_value, top_symbol, top_symbol_volume) VALUES
('2024-01-01', 5250, 5, 657650.00, 'AMZN', 300),
('2024-01-02', 3450, 5, 434062.50, 'TSLA', 1500),
('2024-01-03', 3900, 5, 315552.00, 'ORCL', 1200),
('2024-01-04', 3550, 5, 608475.00, 'AMZN', 450),
('2024-01-05', 4200, 8, 425000.00, 'AAPL', 1200);
