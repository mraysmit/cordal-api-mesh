-- EXAMPLE IMPLEMENTATION: Simple PostgreSQL setup for stock_trades table
-- This script provides example PostgreSQL table setup for stock trading data
-- This is NOT part of the core system and should only be used for integration testing
--
-- Usage: Connect to postgres-trades database and run this script
-- psql -U postgres -h localhost -d postgres-trades -f postgres-setup-simple.sql
-- ================================================================================

-- Create the example stock_trades table with PostgreSQL-compatible column names
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

-- Insert example sample data
INSERT INTO stock_trades (symb, trade_type, quantity, price, total_val, trade_date_time, trader_id, exchange) VALUES
('AAPL', 'BUY', 100, 150.50, 15050.00, '2024-01-01 09:30:00', 'TRADER_001', 'NASDAQ'),
('GOOGL', 'SELL', 50, 2500.75, 125037.50, '2024-01-01 09:35:00', 'TRADER_002', 'NYSE'),
('AAPL', 'SELL', 75, 151.25, 11343.75, '2024-01-01 09:40:00', 'TRADER_003', 'NASDAQ'),
('MSFT', 'BUY', 200, 380.25, 76050.00, '2024-01-01 09:45:00', 'TRADER_001', 'NASDAQ'),
('TSLA', 'BUY', 150, 245.80, 36870.00, '2024-01-01 09:50:00', 'TRADER_004', 'NASDAQ'),
('AMZN', 'SELL', 30, 3200.00, 96000.00, '2024-01-01 10:00:00', 'TRADER_005', 'NASDAQ'),
('NVDA', 'BUY', 80, 520.25, 41620.00, '2024-01-01 10:15:00', 'TRADER_001', 'NASDAQ'),
('META', 'SELL', 60, 350.75, 21045.00, '2024-01-01 10:30:00', 'TRADER_003', 'NASDAQ'),
('NFLX', 'BUY', 40, 480.50, 19220.00, '2024-01-01 10:45:00', 'TRADER_002', 'NASDAQ'),
('AMD', 'SELL', 120, 125.00, 15000.00, '2024-01-01 11:00:00', 'TRADER_004', 'NASDAQ');

-- Grant permissions
GRANT ALL PRIVILEGES ON TABLE stock_trades TO postgres;
GRANT ALL PRIVILEGES ON SEQUENCE stock_trades_id_seq TO postgres;

-- Verification
SELECT 'PostgreSQL example stock_trades table setup completed!' AS status;
SELECT 'Total example records: ' || COUNT(*) AS summary FROM stock_trades;
