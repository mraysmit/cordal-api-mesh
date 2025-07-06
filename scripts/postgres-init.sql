-- Initialize PostgreSQL stocktrades database
-- This script creates the stock_trades table and inserts sample data

-- Connect to the stocktrades database (run this manually first):
-- CREATE DATABASE stocktrades;
-- \c stocktrades;

-- Drop existing tables if they exist
DROP TABLE IF EXISTS stock_trades;

-- Create stock_trades table for PostgreSQL
CREATE TABLE IF NOT EXISTS stock_trades (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(10) NOT NULL,
    trade_type VARCHAR(4) NOT NULL CHECK (trade_type IN ('BUY', 'SELL')),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    price DECIMAL(10,2) NOT NULL CHECK (price > 0),
    total_value DECIMAL(15,2) NOT NULL,
    trade_date_time TIMESTAMP NOT NULL,
    trader_id VARCHAR(50) NOT NULL,
    exchange VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_stock_trades_symbol ON stock_trades(symbol);
CREATE INDEX IF NOT EXISTS idx_stock_trades_trader_id ON stock_trades(trader_id);
CREATE INDEX IF NOT EXISTS idx_stock_trades_trade_date_time ON stock_trades(trade_date_time);
CREATE INDEX IF NOT EXISTS idx_stock_trades_exchange ON stock_trades(exchange);
CREATE INDEX IF NOT EXISTS idx_stock_trades_trade_type ON stock_trades(trade_type);

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
('AMZN', 'BUY', 45, 3210.75, 144483.75, '2024-01-15 11:20:00', 'TRADER_002', 'NASDAQ'),
('NVDA', 'SELL', 60, 878.25, 52695.00, '2024-01-15 11:25:00', 'TRADER_003', 'NASDAQ'),
('META', 'SELL', 40, 487.50, 19500.00, '2024-01-15 11:30:00', 'TRADER_004', 'NASDAQ'),
('NFLX', 'BUY', 55, 522.00, 28710.00, '2024-01-15 11:35:00', 'TRADER_001', 'NASDAQ'),
('ORCL', 'SELL', 80, 116.25, 9300.00, '2024-01-15 11:40:00', 'TRADER_002', 'NYSE'),
('CRM', 'SELL', 70, 267.75, 18742.50, '2024-01-15 11:45:00', 'TRADER_003', 'NYSE');

-- Create a trigger to automatically update the updated_at column
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_stock_trades_updated_at 
    BEFORE UPDATE ON stock_trades 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Display table information
SELECT 'PostgreSQL stock_trades table created successfully' as status;
SELECT COUNT(*) as total_records FROM stock_trades;
SELECT DISTINCT symbol FROM stock_trades ORDER BY symbol;
