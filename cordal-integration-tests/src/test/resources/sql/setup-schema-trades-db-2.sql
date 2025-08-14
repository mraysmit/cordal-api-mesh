-- Schema Setup Script for trades_db_2
-- This script creates the stock_trades table and indexes for the second database
--
-- Usage:
-- psql -h localhost -U testuser -d trades_db_2 -f setup-schema-trades-db-2.sql

-- Connect to trades_db_2
\c trades_db_2;

-- Drop table if exists (for clean setup)
DROP TABLE IF EXISTS stock_trades CASCADE;

-- Create stock_trades table (identical structure to trades_db_1)
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

-- Insert different sample data for trades_db_2 (to demonstrate dual database functionality)
INSERT INTO stock_trades (symbol, trade_type, quantity, price, total_value, trade_date_time, trader_id, exchange) VALUES
-- Different technology stocks focus
('UBER', 'BUY', 300, 45.80, 13740.00, '2025-07-15 09:45:00', 'TRADER004', 'NYSE'),
('LYFT', 'SELL', 200, 12.35, 2470.00, '2025-07-15 14:20:00', 'TRADER004', 'NASDAQ'),
('SPOT', 'BUY', 150, 185.60, 27840.00, '2025-07-15 10:30:00', 'ALGO_TRADER_C', 'NYSE'),
('TWTR', 'BUY', 400, 52.75, 21100.00, '2025-07-15 11:15:00', 'INSTITUTIONAL_03', 'NYSE'),
('SNAP', 'SELL', 250, 8.90, 2225.00, '2025-07-15 15:30:00', 'INSTITUTIONAL_03', 'NYSE'),
('ZM', 'BUY', 120, 68.45, 8214.00, '2025-07-15 13:10:00', 'HEDGE_FUND_GAMMA', 'NASDAQ'),
('DOCU', 'SELL', 180, 55.20, 9936.00, '2025-07-15 12:45:00', 'QUANT_TRADER_3', 'NASDAQ'),

-- Different financial focus
('GS', 'BUY', 80, 385.90, 30872.00, '2025-07-14 09:30:00', 'PENSION_FUND_02', 'NYSE'),
('MS', 'SELL', 150, 88.75, 13312.50, '2025-07-14 11:45:00', 'MUTUAL_FUND_B', 'NYSE'),
('C', 'BUY', 350, 48.60, 17010.00, '2025-07-14 14:15:00', 'RETAIL_TRADER_Z', 'NYSE'),

-- Different energy focus
('SLB', 'BUY', 280, 45.30, 12684.00, '2025-07-13 10:30:00', 'TRADER005', 'NYSE'),
('HAL', 'SELL', 320, 32.85, 10512.00, '2025-07-13 13:20:00', 'TRADER005', 'NYSE'),

-- Different healthcare focus
('UNH', 'BUY', 60, 485.20, 29112.00, '2025-07-12 09:45:00', 'DAY_TRADER_02', 'NYSE'),
('ABBV', 'SELL', 200, 145.75, 29150.00, '2025-07-12 15:10:00', 'SWING_TRADER_02', 'NYSE'),

-- Different consumer focus
('WMT', 'BUY', 180, 158.40, 28512.00, '2025-07-11 11:30:00', 'SCALPER_02', 'NYSE'),
('TGT', 'SELL', 140, 125.90, 17626.00, '2025-07-11 14:25:00', 'RETAIL_TRADER_W', 'NYSE'),

-- More recent different trades
('SHOP', 'BUY', 90, 68.75, 6187.50, '2025-07-16 09:20:00', 'ALGO_TRADER_D', 'NYSE'),
('SQ', 'SELL', 160, 75.40, 12064.00, '2025-07-16 16:45:00', 'ALGO_TRADER_D', 'NYSE'),
('ROKU', 'BUY', 220, 58.30, 12826.00, '2025-07-16 10:15:00', 'HEDGE_FUND_DELTA', 'NASDAQ'),
('PINS', 'SELL', 300, 28.65, 8595.00, '2025-07-16 13:30:00', 'TRADER006', 'NYSE'),

-- Additional diverse trades for trades_db_2
('V', 'BUY', 100, 245.80, 24580.00, '2025-07-10 12:10:00', 'INSTITUTIONAL_04', 'NYSE'),
('MA', 'SELL', 75, 385.60, 28920.00, '2025-07-10 15:25:00', 'QUANT_TRADER_4', 'NYSE'),
('DIS', 'BUY', 200, 95.45, 19090.00, '2025-07-09 11:20:00', 'TRADER004', 'NYSE'),
('NFLX', 'SELL', 45, 485.75, 21858.75, '2025-07-09 14:35:00', 'MUTUAL_FUND_B', 'NASDAQ'),
('CRM', 'BUY', 110, 215.80, 23738.00, '2025-07-08 10:50:00', 'HEDGE_FUND_GAMMA', 'NYSE'),
('SNOW', 'SELL', 85, 145.25, 12346.25, '2025-07-08 16:05:00', 'RETAIL_TRADER_Z', 'NYSE');

-- Verify the setup
\echo 'Schema setup completed for trades_db_2'
\echo 'Table created: stock_trades'
\echo 'Indexes created: 8 indexes for optimal query performance'
\echo 'Sample data inserted: 25 stock trades (different from trades_db_1)'
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
\echo 'trades_db_2 setup completed successfully!'
