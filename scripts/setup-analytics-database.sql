-- Analytics Database Setup Script
-- Creates tables and populates with sample data for analytics queries

-- Drop tables if they exist
DROP TABLE IF EXISTS trades;

-- Create trades table for analytics
CREATE TABLE trades (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    symbol VARCHAR(10) NOT NULL,
    trade_type VARCHAR(10) NOT NULL,
    quantity INTEGER NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    total_value DECIMAL(15,2) NOT NULL,
    trade_date TIMESTAMP NOT NULL,
    trader_id VARCHAR(50) NOT NULL,
    exchange VARCHAR(20) NOT NULL
);

-- Create indexes for better performance
CREATE INDEX idx_trades_symbol ON trades(symbol);
CREATE INDEX idx_trades_date ON trades(trade_date);
CREATE INDEX idx_trades_trader ON trades(trader_id);

-- Insert sample data for analytics
INSERT INTO trades (symbol, trade_type, quantity, price, total_value, trade_date, trader_id, exchange) VALUES
('AAPL', 'BUY', 100, 150.00, 15000.00, '2024-01-15 09:30:00', 'TRADER001', 'NASDAQ'),
('AAPL', 'SELL', 50, 152.00, 7600.00, '2024-01-15 14:30:00', 'TRADER001', 'NASDAQ'),
('TSLA', 'BUY', 200, 200.00, 40000.00, '2024-01-15 10:00:00', 'TRADER002', 'NASDAQ'),
('TSLA', 'SELL', 100, 202.20, 20220.00, '2024-01-15 15:00:00', 'TRADER002', 'NASDAQ'),
('GOOGL', 'BUY', 75, 2800.00, 210000.00, '2024-01-15 11:00:00', 'TRADER003', 'NASDAQ'),
('GOOGL', 'SELL', 25, 2850.00, 71250.00, '2024-01-15 16:00:00', 'TRADER003', 'NASDAQ'),
('MSFT', 'BUY', 150, 380.00, 57000.00, '2024-01-16 09:30:00', 'TRADER004', 'NASDAQ'),
('MSFT', 'SELL', 75, 385.00, 28875.00, '2024-01-16 14:30:00', 'TRADER004', 'NASDAQ'),
('AMZN', 'BUY', 80, 3200.00, 256000.00, '2024-01-16 10:30:00', 'TRADER005', 'NASDAQ'),
('AMZN', 'SELL', 40, 3250.00, 130000.00, '2024-01-16 15:30:00', 'TRADER005', 'NASDAQ'),
('NVDA', 'BUY', 120, 500.00, 60000.00, '2024-01-17 09:45:00', 'TRADER006', 'NASDAQ'),
('NVDA', 'SELL', 60, 520.00, 31200.00, '2024-01-17 14:45:00', 'TRADER006', 'NASDAQ'),
('META', 'BUY', 90, 350.00, 31500.00, '2024-01-17 10:15:00', 'TRADER007', 'NASDAQ'),
('META', 'SELL', 45, 365.00, 16425.00, '2024-01-17 15:15:00', 'TRADER007', 'NASDAQ'),
('NFLX', 'BUY', 60, 450.00, 27000.00, '2024-01-18 09:30:00', 'TRADER008', 'NASDAQ'),
('NFLX', 'SELL', 30, 465.00, 13950.00, '2024-01-18 14:30:00', 'TRADER008', 'NASDAQ'),
('ORCL', 'BUY', 200, 110.00, 22000.00, '2024-01-18 10:00:00', 'TRADER009', 'NYSE'),
('ORCL', 'SELL', 100, 115.00, 11500.00, '2024-01-18 15:00:00', 'TRADER009', 'NYSE'),
('CRM', 'BUY', 85, 250.00, 21250.00, '2024-01-19 09:45:00', 'TRADER010', 'NYSE'),
('CRM', 'SELL', 42, 260.00, 10920.00, '2024-01-19 14:45:00', 'TRADER010', 'NYSE');

-- Add more recent data for better analytics
INSERT INTO trades (symbol, trade_type, quantity, price, total_value, trade_date, trader_id, exchange) VALUES
('AAPL', 'BUY', 150, 155.00, 23250.00, '2024-01-20 09:30:00', 'TRADER011', 'NASDAQ'),
('TSLA', 'BUY', 180, 205.00, 36900.00, '2024-01-20 10:00:00', 'TRADER012', 'NASDAQ'),
('GOOGL', 'BUY', 50, 2900.00, 145000.00, '2024-01-20 11:00:00', 'TRADER013', 'NASDAQ'),
('MSFT', 'BUY', 120, 390.00, 46800.00, '2024-01-20 12:00:00', 'TRADER014', 'NASDAQ'),
('AMZN', 'BUY', 70, 3300.00, 231000.00, '2024-01-20 13:00:00', 'TRADER015', 'NASDAQ');

COMMIT;
