-- H2 Database initialization for postgres-trades (H2 version)
-- This creates the postgres-trades database using H2 with PostgreSQL-compatible schema

-- Drop existing tables if they exist
DROP TABLE IF EXISTS stock_trades;

-- Create stock_trades table (H2 compatible version)
CREATE TABLE IF NOT EXISTS stock_trades (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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

-- Insert sample data
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
('AMD', 'SELL', 120, 125.00, 15000.00, '2024-01-01 11:00:00', 'TRADER_004', 'NASDAQ'),
('ORCL', 'BUY', 90, 115.75, 10417.50, '2024-01-01 11:15:00', 'TRADER_005', 'NYSE'),
('CRM', 'SELL', 70, 265.50, 18585.00, '2024-01-01 11:30:00', 'TRADER_001', 'NYSE'),
('INTC', 'BUY', 250, 45.25, 11312.50, '2024-01-01 11:45:00', 'TRADER_002', 'NASDAQ'),
('IBM', 'SELL', 85, 135.80, 11543.00, '2024-01-01 12:00:00', 'TRADER_003', 'NYSE'),
('BABA', 'BUY', 110, 85.50, 9405.00, '2024-01-01 12:15:00', 'TRADER_004', 'NYSE'),
('UBER', 'SELL', 180, 55.75, 10035.00, '2024-01-01 12:30:00', 'TRADER_005', 'NYSE'),
('SNAP', 'BUY', 300, 12.25, 3675.00, '2024-01-01 12:45:00', 'TRADER_001', 'NYSE'),
('TWTR', 'SELL', 200, 42.80, 8560.00, '2024-01-01 13:00:00', 'TRADER_002', 'NYSE'),
('SPOT', 'BUY', 65, 125.50, 8157.50, '2024-01-01 13:15:00', 'TRADER_003', 'NYSE'),
('SQ', 'SELL', 95, 78.25, 7433.75, '2024-01-01 13:30:00', 'TRADER_004', 'NYSE');
