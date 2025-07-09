-- ================================================================================
-- Simple PostgreSQL Setup for postgres-trades (Run after database creation)
-- ================================================================================
-- This script assumes the postgres-trades database already exists.
-- Run this if you've already created the database manually.
--
-- Usage: Connect to postgres-trades database and run this script
-- psql -U postgres -h localhost -d postgres-trades -f postgres-setup-simple.sql
-- ================================================================================

-- Create the stock_trades table with PostgreSQL-compatible column names
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

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_stock_trades_symb ON stock_trades(symb);
CREATE INDEX IF NOT EXISTS idx_stock_trades_trader_id ON stock_trades(trader_id);
CREATE INDEX IF NOT EXISTS idx_stock_trades_trade_date_time ON stock_trades(trade_date_time);
CREATE INDEX IF NOT EXISTS idx_stock_trades_exchange ON stock_trades(exchange);
CREATE INDEX IF NOT EXISTS idx_stock_trades_trade_type ON stock_trades(trade_type);

-- Create trigger function for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger
CREATE TRIGGER update_stock_trades_updated_at 
    BEFORE UPDATE ON stock_trades 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Insert sample data
INSERT INTO stock_trades (symb, trade_type, quantity, price, total_val, trade_date_time, trader_id, exchange) VALUES
('AAPL', 'BUY', 100, 150.50, 15050.00, '2024-01-01 09:30:00', 'TRADER_001', 'NASDAQ'),
('GOOGL', 'SELL', 50, 2500.75, 125037.50, '2024-01-01 09:35:00', 'TRADER_002', 'NYSE'),
('AAPL', 'SELL', 75, 151.25, 11343.75, '2024-01-01 09:40:00', 'TRADER_003', 'NASDAQ'),
('MSFT', 'BUY', 200, 380.25, 76050.00, '2024-01-01 09:45:00', 'TRADER_001', 'NASDAQ'),
('TSLA', 'BUY', 150, 245.80, 36870.00, '2024-01-01 09:50:00', 'TRADER_004', 'NASDAQ'),
('JPM', 'BUY', 300, 165.40, 49620.00, '2024-01-01 10:00:00', 'TRADER_002', 'NYSE'),
('BAC', 'SELL', 500, 32.15, 16075.00, '2024-01-01 10:05:00', 'TRADER_005', 'NYSE'),
('WFC', 'BUY', 250, 45.30, 11325.00, '2024-01-01 10:10:00', 'TRADER_003', 'NYSE'),
('JNJ', 'BUY', 180, 168.90, 30402.00, '2024-01-01 10:15:00', 'TRADER_006', 'NYSE'),
('PFE', 'SELL', 400, 28.75, 11500.00, '2024-01-01 10:20:00', 'TRADER_001', 'NYSE'),
('XOM', 'BUY', 220, 112.45, 24739.00, '2024-01-01 10:25:00', 'TRADER_007', 'NYSE'),
('CVX', 'SELL', 160, 158.30, 25328.00, '2024-01-01 10:30:00', 'TRADER_002', 'NYSE'),
('KO', 'BUY', 350, 58.20, 20370.00, '2024-01-01 10:35:00', 'TRADER_008', 'NYSE'),
('PG', 'SELL', 120, 155.75, 18690.00, '2024-01-01 10:40:00', 'TRADER_004', 'NYSE'),
('AAPL', 'BUY', 80, 152.30, 12184.00, '2024-01-02 09:30:00', 'TRADER_001', 'NASDAQ'),
('GOOGL', 'BUY', 25, 2510.50, 62762.50, '2024-01-02 09:35:00', 'TRADER_003', 'NYSE'),
('MSFT', 'SELL', 150, 382.75, 57412.50, '2024-01-02 09:40:00', 'TRADER_005', 'NASDAQ'),
('TSLA', 'SELL', 100, 248.90, 24890.00, '2024-01-02 09:45:00', 'TRADER_007', 'NASDAQ'),
('NVDA', 'BUY', 60, 875.20, 52512.00, '2024-01-02 10:00:00', 'TRADER_002', 'NASDAQ'),
('AMD', 'SELL', 180, 142.60, 25668.00, '2024-01-02 10:05:00', 'TRADER_006', 'NASDAQ');

-- Grant permissions
GRANT ALL PRIVILEGES ON TABLE stock_trades TO postgres;
GRANT ALL PRIVILEGES ON SEQUENCE stock_trades_id_seq TO postgres;

-- Verification
SELECT 'PostgreSQL stock_trades table setup completed!' AS status;
SELECT 'Total records: ' || COUNT(*) AS summary FROM stock_trades;
