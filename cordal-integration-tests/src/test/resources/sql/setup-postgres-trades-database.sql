-- EXAMPLE IMPLEMENTATION: PostgreSQL-style Trades Database Setup Script
-- Creates tables and populates with sample data for postgres queries
-- This is NOT part of the core system and should only be used for integration testing

-- Drop tables if they exist
DROP TABLE IF EXISTS stock_trades;

-- Create stock_trades table matching PostgreSQL schema
CREATE TABLE stock_trades (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    symb VARCHAR(10) NOT NULL,
    trade_type VARCHAR(10) NOT NULL,
    quantity INTEGER NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    total_val DECIMAL(15,2) NOT NULL,
    trade_date_time TIMESTAMP NOT NULL,
    trader_id VARCHAR(50) NOT NULL,
    exchange VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX idx_stock_trades_symb ON stock_trades(symb);
CREATE INDEX idx_stock_trades_trader ON stock_trades(trader_id);
CREATE INDEX idx_stock_trades_date ON stock_trades(trade_date_time);

-- Insert comprehensive sample data for postgres endpoints - EXAMPLE DATA ONLY
INSERT INTO stock_trades (symb, trade_type, quantity, price, total_val, trade_date_time, trader_id, exchange, created_at, updated_at) VALUES
('AAPL', 'BUY', 100, 150.50, 15050.00, '2024-01-01 09:30:00', 'TRADER_001', 'NASDAQ', '2024-01-01 09:30:00', '2024-01-01 09:30:00'),
('GOOGL', 'SELL', 50, 2500.75, 125037.50, '2024-01-01 09:35:00', 'TRADER_002', 'NYSE', '2024-01-01 09:35:00', '2024-01-01 09:35:00'),
('AAPL', 'SELL', 75, 151.25, 11343.75, '2024-01-01 09:40:00', 'TRADER_003', 'NASDAQ', '2024-01-01 09:40:00', '2024-01-01 09:40:00'),
('MSFT', 'BUY', 200, 380.25, 76050.00, '2024-01-01 09:45:00', 'TRADER_001', 'NASDAQ', '2024-01-01 09:45:00', '2024-01-01 09:45:00'),
('TSLA', 'BUY', 150, 245.80, 36870.00, '2024-01-01 09:50:00', 'TRADER_004', 'NASDAQ', '2024-01-01 09:50:00', '2024-01-01 09:50:00');

-- Verify the setup
SELECT COUNT(*) as total_example_records FROM stock_trades;
