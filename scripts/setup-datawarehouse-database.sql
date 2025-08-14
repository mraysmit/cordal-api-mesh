-- Data Warehouse Database Setup Script
-- Creates tables and populates with sample data for market summary queries

-- Drop tables if they exist
DROP TABLE IF EXISTS market_summary;

-- Create market_summary table for data warehouse
CREATE TABLE market_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    summary_date DATE NOT NULL,
    total_volume BIGINT NOT NULL,
    total_trades INTEGER NOT NULL,
    avg_trade_value DECIMAL(15,2) NOT NULL,
    top_symbol VARCHAR(10) NOT NULL,
    top_symbol_volume BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_market_summary_date ON market_summary(summary_date);
CREATE INDEX idx_market_summary_created ON market_summary(created_at);

-- Insert sample market summary data
INSERT INTO market_summary (summary_date, total_volume, total_trades, avg_trade_value, top_symbol, top_symbol_volume, created_at) VALUES
('2024-01-15', 1250000, 45, 27777.78, 'AAPL', 150000, '2024-01-15 18:00:00'),
('2024-01-16', 1380000, 52, 26538.46, 'TSLA', 200000, '2024-01-16 18:00:00'),
('2024-01-17', 1420000, 48, 29583.33, 'GOOGL', 180000, '2024-01-17 18:00:00'),
('2024-01-18', 1350000, 41, 32926.83, 'MSFT', 225000, '2024-01-18 18:00:00'),
('2024-01-19', 1480000, 55, 26909.09, 'AMZN', 150000, '2024-01-19 18:00:00'),
('2024-01-20', 1520000, 58, 26206.90, 'NVDA', 300000, '2024-01-20 18:00:00'),
('2024-01-21', 1390000, 44, 31590.91, 'META', 165000, '2024-01-21 18:00:00'),
('2024-01-22', 1610000, 62, 25967.74, 'NFLX', 240000, '2024-01-22 18:00:00');

COMMIT;
