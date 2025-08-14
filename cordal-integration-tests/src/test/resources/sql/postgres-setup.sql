-- EXAMPLE IMPLEMENTATION: PostgreSQL setup for postgres-trades database
-- This script provides example PostgreSQL database setup for stock trading data
-- This is NOT part of the core system and should only be used for integration testing

-- ================================================================================
-- Create database and user for postgres-trades (example database)
-- ================================================================================

-- Create user for postgres-trades database
CREATE USER postgres_trades_user WITH PASSWORD 'postgres_trades_pass';

-- Create database for postgres-trades
CREATE DATABASE "postgres-trades" WITH OWNER postgres_trades_user;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE "postgres-trades" TO postgres_trades_user;

-- Connect to the new database
\c "postgres-trades"

-- ================================================================================
-- Create the example stock_trades table
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
    total_val DECIMAL(15, 2) NOT NULL,
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
-- Insert example sample data (compatible with existing H2 data structure)
-- ================================================================================

INSERT INTO stock_trades (symb, trade_type, quantity, price, total_val, trade_date_time, trader_id, exchange) VALUES
-- Technology stocks
('AAPL', 'BUY', 100, 150.50, 15050.00, '2024-01-01 09:30:00', 'TRADER_001', 'NASDAQ'),
('GOOGL', 'SELL', 50, 2500.75, 125037.50, '2024-01-01 09:35:00', 'TRADER_002', 'NYSE'),
('AAPL', 'SELL', 75, 151.25, 11343.75, '2024-01-01 09:40:00', 'TRADER_003', 'NASDAQ'),
('MSFT', 'BUY', 200, 380.25, 76050.00, '2024-01-01 09:45:00', 'TRADER_001', 'NASDAQ'),
('TSLA', 'BUY', 150, 245.80, 36870.00, '2024-01-01 09:50:00', 'TRADER_004', 'NASDAQ');

-- Grant all privileges on the table to the postgres user
GRANT ALL PRIVILEGES ON TABLE stock_trades TO postgres;
GRANT ALL PRIVILEGES ON SEQUENCE stock_trades_id_seq TO postgres;

-- ================================================================================
-- Verification queries
-- ================================================================================

-- Display table structure
\d stock_trades

-- Show sample data
SELECT 'Sample data from example stock_trades:' AS info;
SELECT id, symb, trade_type, quantity, price, total_val, trade_date_time, trader_id, exchange 
FROM stock_trades 
ORDER BY trade_date_time DESC 
LIMIT 5;

-- Show record count
SELECT 'Total records in example stock_trades:' AS info, COUNT(*) AS total_records FROM stock_trades;

-- ================================================================================
-- Setup complete message
-- ================================================================================

SELECT '================================================================================';
SELECT 'PostgreSQL setup for example postgres-trades database completed successfully!';
SELECT '================================================================================';
SELECT 'Database: postgres-trades (EXAMPLE FOR INTEGRATION TESTING)';
SELECT 'Table: stock_trades (' || COUNT(*) || ' example records)' FROM stock_trades;
SELECT 'Connection URL: jdbc:postgresql://localhost:5432/postgres-trades?currentSchema=public';
SELECT 'Username: postgres_trades_user';
SELECT 'Password: postgres_trades_pass';
SELECT '================================================================================';
