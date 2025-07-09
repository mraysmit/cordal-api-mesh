-- ================================================================================
-- PostgreSQL Database Setup for postgres-trades
-- ================================================================================
-- This script creates the PostgreSQL database and tables required for the
-- Generic API Service postgres-trades database configuration.
--
-- Prerequisites:
-- 1. PostgreSQL server must be installed and running
-- 2. PostgreSQL user 'postgres' with password 'postgres' must exist
-- 3. Run this script as a PostgreSQL superuser or user with CREATE DATABASE privileges
--
-- Usage:
-- psql -U postgres -h localhost -f postgres-setup.sql
-- ================================================================================

-- Create the database
CREATE DATABASE "postgres-trades"
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'English_United States.1252'
    LC_CTYPE = 'English_United States.1252'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

-- Connect to the new database
\c "postgres-trades"

-- ================================================================================
-- Create the stock_trades table
-- ================================================================================
-- Note: The PostgreSQL queries use different column names than the H2 version:
-- - H2 uses 'SYMBOL', PostgreSQL queries expect 'symb' 
-- - H2 uses 'TOTAL_VALUE', PostgreSQL queries expect 'total_val'
-- We'll create the table with PostgreSQL-compatible column names
-- ================================================================================

CREATE TABLE IF NOT EXISTS stock_trades (
    id BIGSERIAL PRIMARY KEY,
    symb VARCHAR(10) NOT NULL,
    trade_type VARCHAR(4) NOT NULL CHECK (trade_type IN ('BUY', 'SELL')),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    price DECIMAL(10, 2) NOT NULL CHECK (price > 0),
    total_val DECIMAL(15, 2) NOT NULL CHECK (total_val > 0),
    trade_date_time TIMESTAMP NOT NULL,
    trader_id VARCHAR(50) NOT NULL,
    exchange VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ================================================================================
-- Create indexes for better query performance
-- ================================================================================

-- Index on symbol for symbol-based queries
CREATE INDEX IF NOT EXISTS idx_stock_trades_symb ON stock_trades(symb);

-- Index on trader_id for trader-based queries
CREATE INDEX IF NOT EXISTS idx_stock_trades_trader_id ON stock_trades(trader_id);

-- Index on trade_date_time for date range queries and ordering
CREATE INDEX IF NOT EXISTS idx_stock_trades_trade_date_time ON stock_trades(trade_date_time);

-- Index on exchange for exchange-based queries
CREATE INDEX IF NOT EXISTS idx_stock_trades_exchange ON stock_trades(exchange);

-- Index on trade_type for trade type queries
CREATE INDEX IF NOT EXISTS idx_stock_trades_trade_type ON stock_trades(trade_type);

-- Composite index for common query patterns
CREATE INDEX IF NOT EXISTS idx_stock_trades_symb_date ON stock_trades(symb, trade_date_time);

-- ================================================================================
-- Create trigger function to automatically update updated_at timestamp
-- ================================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to automatically update updated_at on row updates
CREATE TRIGGER update_stock_trades_updated_at 
    BEFORE UPDATE ON stock_trades 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- ================================================================================
-- Insert sample data (compatible with existing H2 data structure)
-- ================================================================================

INSERT INTO stock_trades (symb, trade_type, quantity, price, total_val, trade_date_time, trader_id, exchange) VALUES
-- Technology stocks
('AAPL', 'BUY', 100, 150.50, 15050.00, '2024-01-01 09:30:00', 'TRADER_001', 'NASDAQ'),
('GOOGL', 'SELL', 50, 2500.75, 125037.50, '2024-01-01 09:35:00', 'TRADER_002', 'NYSE'),
('AAPL', 'SELL', 75, 151.25, 11343.75, '2024-01-01 09:40:00', 'TRADER_003', 'NASDAQ'),
('MSFT', 'BUY', 200, 380.25, 76050.00, '2024-01-01 09:45:00', 'TRADER_001', 'NASDAQ'),
('TSLA', 'BUY', 150, 245.80, 36870.00, '2024-01-01 09:50:00', 'TRADER_004', 'NASDAQ'),

-- Financial stocks
('JPM', 'BUY', 300, 165.40, 49620.00, '2024-01-01 10:00:00', 'TRADER_002', 'NYSE'),
('BAC', 'SELL', 500, 32.15, 16075.00, '2024-01-01 10:05:00', 'TRADER_005', 'NYSE'),
('WFC', 'BUY', 250, 45.30, 11325.00, '2024-01-01 10:10:00', 'TRADER_003', 'NYSE'),

-- Healthcare stocks
('JNJ', 'BUY', 180, 168.90, 30402.00, '2024-01-01 10:15:00', 'TRADER_006', 'NYSE'),
('PFE', 'SELL', 400, 28.75, 11500.00, '2024-01-01 10:20:00', 'TRADER_001', 'NYSE'),

-- Energy stocks
('XOM', 'BUY', 220, 112.45, 24739.00, '2024-01-01 10:25:00', 'TRADER_007', 'NYSE'),
('CVX', 'SELL', 160, 158.30, 25328.00, '2024-01-01 10:30:00', 'TRADER_002', 'NYSE'),

-- Consumer goods
('KO', 'BUY', 350, 58.20, 20370.00, '2024-01-01 10:35:00', 'TRADER_008', 'NYSE'),
('PG', 'SELL', 120, 155.75, 18690.00, '2024-01-01 10:40:00', 'TRADER_004', 'NYSE'),

-- More recent trades for testing date ranges
('AAPL', 'BUY', 80, 152.30, 12184.00, '2024-01-02 09:30:00', 'TRADER_001', 'NASDAQ'),
('GOOGL', 'BUY', 25, 2510.50, 62762.50, '2024-01-02 09:35:00', 'TRADER_003', 'NYSE'),
('MSFT', 'SELL', 150, 382.75, 57412.50, '2024-01-02 09:40:00', 'TRADER_005', 'NASDAQ'),
('TSLA', 'SELL', 100, 248.90, 24890.00, '2024-01-02 09:45:00', 'TRADER_007', 'NASDAQ'),

-- International exchanges for variety
('NVDA', 'BUY', 60, 875.20, 52512.00, '2024-01-02 10:00:00', 'TRADER_002', 'NASDAQ'),
('AMD', 'SELL', 180, 142.60, 25668.00, '2024-01-02 10:05:00', 'TRADER_006', 'NASDAQ');

-- ================================================================================
-- Create views for compatibility (if needed)
-- ================================================================================

-- Create a view that maps H2 column names to PostgreSQL column names
-- This can be useful for applications that expect H2-style column names
CREATE OR REPLACE VIEW stock_trades_h2_compat AS
SELECT 
    id,
    symb AS symbol,
    trade_type,
    quantity,
    price,
    total_val AS total_value,
    trade_date_time,
    trader_id,
    exchange,
    created_at,
    updated_at
FROM stock_trades;

-- ================================================================================
-- Grant permissions
-- ================================================================================

-- Grant all privileges on the table to the postgres user
GRANT ALL PRIVILEGES ON TABLE stock_trades TO postgres;
GRANT ALL PRIVILEGES ON SEQUENCE stock_trades_id_seq TO postgres;
GRANT ALL PRIVILEGES ON TABLE stock_trades_h2_compat TO postgres;

-- ================================================================================
-- Verification queries
-- ================================================================================

-- Display table structure
\d stock_trades

-- Show sample data
SELECT 'Sample data from stock_trades:' AS info;
SELECT id, symb, trade_type, quantity, price, total_val, trade_date_time, trader_id, exchange 
FROM stock_trades 
ORDER BY trade_date_time DESC 
LIMIT 5;

-- Show record count
SELECT 'Total records in stock_trades:' AS info, COUNT(*) AS total_records FROM stock_trades;

-- Show unique symbols
SELECT 'Unique symbols:' AS info, COUNT(DISTINCT symb) AS unique_symbols FROM stock_trades;

-- Show unique traders
SELECT 'Unique traders:' AS info, COUNT(DISTINCT trader_id) AS unique_traders FROM stock_trades;

-- Show unique exchanges
SELECT 'Unique exchanges:' AS info, COUNT(DISTINCT exchange) AS unique_exchanges FROM stock_trades;

-- ================================================================================
-- Setup complete message
-- ================================================================================

SELECT '================================================================================';
SELECT 'PostgreSQL setup for postgres-trades database completed successfully!';
SELECT '================================================================================';
SELECT 'Database: postgres-trades';
SELECT 'Table: stock_trades (' || COUNT(*) || ' records)' FROM stock_trades;
SELECT 'Connection URL: jdbc:postgresql://localhost:5432/postgres-trades?currentSchema=public';
SELECT 'Username: postgres';
SELECT 'Password: postgres';
SELECT '================================================================================';
