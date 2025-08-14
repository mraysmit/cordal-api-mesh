-- Schema Setup Script for trades_db_1
-- This script creates the stock_trades table and indexes for the first database
--
-- Usage:
-- psql -h localhost -U testuser -d trades_db_1 -f setup-schema-trades-db-1.sql

-- Connect to trades_db_1
\c trades_db_1;

-- Drop table if exists (for clean setup)
DROP TABLE IF EXISTS stock_trades CASCADE;

-- Create stock_trades table
CREATE TABLE stock_trades (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(10) NOT NULL,
    trade_type VARCHAR(10) NOT NULL CHECK (trade_type IN ('BUY', 'SELL')),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    price DECIMAL(10,2) NOT NULL CHECK (price > 0),
    total_value DECIMAL(15,2) NOT NULL CHECK (total_value > 0),
    trade_date_time TIMESTAMP NOT NULL,
    trader_id VARCHAR(50) NOT NULL,
    exchange VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_stock_trades_symbol ON stock_trades(symbol);
CREATE INDEX idx_stock_trades_trader_id ON stock_trades(trader_id);
CREATE INDEX idx_stock_trades_trade_date ON stock_trades(trade_date_time);
CREATE INDEX idx_stock_trades_exchange ON stock_trades(exchange);
CREATE INDEX idx_stock_trades_trade_type ON stock_trades(trade_type);
CREATE INDEX idx_stock_trades_created_at ON stock_trades(created_at);

-- Create a composite index for common query patterns
CREATE INDEX idx_stock_trades_symbol_date ON stock_trades(symbol, trade_date_time);
CREATE INDEX idx_stock_trades_trader_symbol ON stock_trades(trader_id, symbol);

-- Add a trigger to automatically update the updated_at timestamp
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

-- Insert sample data for trades_db_1
INSERT INTO stock_trades (symbol, trade_type, quantity, price, total_value, trade_date_time, trader_id, exchange) VALUES
-- Technology stocks
('AAPL', 'BUY', 100, 175.50, 17550.00, '2025-07-15 09:30:00', 'TRADER001', 'NASDAQ'),
('AAPL', 'SELL', 50, 176.25, 8812.50, '2025-07-15 14:15:00', 'TRADER001', 'NASDAQ'),
('GOOGL', 'BUY', 25, 2850.75, 71268.75, '2025-07-15 10:45:00', 'ALGO_TRADER_A', 'NASDAQ'),
('MSFT', 'BUY', 200, 335.20, 67040.00, '2025-07-15 11:30:00', 'INSTITUTIONAL_01', 'NASDAQ'),
('MSFT', 'SELL', 100, 336.80, 33680.00, '2025-07-15 15:45:00', 'INSTITUTIONAL_01', 'NASDAQ'),
('NVDA', 'BUY', 75, 450.30, 33772.50, '2025-07-15 13:20:00', 'HEDGE_FUND_ALPHA', 'NASDAQ'),
('META', 'BUY', 150, 285.60, 42840.00, '2025-07-15 12:10:00', 'QUANT_TRADER_1', 'NASDAQ'),

-- Financial stocks
('JPM', 'BUY', 300, 145.80, 43740.00, '2025-07-14 09:45:00', 'PENSION_FUND_01', 'NYSE'),
('BAC', 'SELL', 500, 32.45, 16225.00, '2025-07-14 11:20:00', 'MUTUAL_FUND_A', 'NYSE'),
('WFC', 'BUY', 250, 42.15, 10537.50, '2025-07-14 14:30:00', 'RETAIL_TRADER_X', 'NYSE'),

-- Energy stocks
('XOM', 'BUY', 400, 108.25, 43300.00, '2025-07-13 10:15:00', 'TRADER002', 'NYSE'),
('CVX', 'SELL', 200, 155.90, 31180.00, '2025-07-13 13:45:00', 'TRADER002', 'NYSE'),

-- Healthcare stocks
('JNJ', 'BUY', 180, 162.40, 29232.00, '2025-07-12 09:30:00', 'DAY_TRADER_01', 'NYSE'),
('PFE', 'SELL', 600, 28.75, 17250.00, '2025-07-12 15:20:00', 'SWING_TRADER_01', 'NYSE'),

-- Consumer stocks
('KO', 'BUY', 350, 58.90, 20615.00, '2025-07-11 11:45:00', 'SCALPER_01', 'NYSE'),
('PEP', 'SELL', 120, 172.30, 20676.00, '2025-07-11 14:10:00', 'RETAIL_TRADER_Y', 'NASDAQ'),

-- More recent trades
('TSLA', 'BUY', 80, 245.60, 19648.00, '2025-07-16 09:15:00', 'ALGO_TRADER_B', 'NASDAQ'),
('TSLA', 'SELL', 40, 248.90, 9956.00, '2025-07-16 16:30:00', 'ALGO_TRADER_B', 'NASDAQ'),
('AMZN', 'BUY', 35, 3420.50, 119717.50, '2025-07-16 10:30:00', 'HEDGE_FUND_BETA', 'NASDAQ'),
('NFLX', 'SELL', 90, 485.75, 43717.50, '2025-07-16 13:45:00', 'TRADER003', 'NASDAQ'),

-- Additional diverse trades
('ORCL', 'BUY', 220, 118.45, 26059.00, '2025-07-10 12:20:00', 'INSTITUTIONAL_02', 'NYSE'),
('CRM', 'SELL', 85, 215.80, 18343.00, '2025-07-10 15:10:00', 'QUANT_TRADER_2', 'NYSE'),
('ADBE', 'BUY', 60, 525.30, 31518.00, '2025-07-09 11:35:00', 'TRADER001', 'NASDAQ'),
('INTC', 'SELL', 450, 35.20, 15840.00, '2025-07-09 14:50:00', 'MUTUAL_FUND_A', 'NASDAQ'),
('AMD', 'BUY', 180, 125.75, 22635.00, '2025-07-08 10:05:00', 'HEDGE_FUND_ALPHA', 'NASDAQ'),
('PYPL', 'SELL', 140, 68.90, 9646.00, '2025-07-08 16:15:00', 'RETAIL_TRADER_X', 'NASDAQ');

-- Verify the setup
\echo 'Schema setup completed for trades_db_1'
\echo 'Table created: stock_trades'
\echo 'Indexes created: 8 indexes for optimal query performance'
\echo 'Sample data inserted: 25 stock trades'
\echo ''

-- Display summary statistics
SELECT 
    COUNT(*) as total_trades,
    COUNT(DISTINCT symbol) as unique_symbols,
    COUNT(DISTINCT trader_id) as unique_traders,
    COUNT(DISTINCT exchange) as exchanges,
    ROUND(AVG(total_value), 2) as avg_trade_value,
    MIN(trade_date_time) as earliest_trade,
    MAX(trade_date_time) as latest_trade
FROM stock_trades;

\echo ''
\echo 'trades_db_1 setup completed successfully!'
