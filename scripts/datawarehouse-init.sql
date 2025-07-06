-- Initialize datawarehouse H2 database

-- Drop existing tables if they exist
DROP TABLE IF EXISTS market_summary;

-- Create market_summary table for datawarehouse
CREATE TABLE IF NOT EXISTS market_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    summary_date DATE NOT NULL,
    total_volume BIGINT NOT NULL,
    total_trades INTEGER NOT NULL,
    avg_trade_value DECIMAL(15,2) NOT NULL,
    top_symbol VARCHAR(10) NOT NULL,
    top_symbol_volume BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert sample market summary data
INSERT INTO market_summary (summary_date, total_volume, total_trades, avg_trade_value, top_symbol, top_symbol_volume) VALUES
('2024-01-01', 5250, 5, 657650.00, 'AMZN', 300),
('2024-01-02', 3450, 5, 434062.50, 'TSLA', 1500),
('2024-01-03', 3900, 5, 315552.00, 'ORCL', 1200),
('2024-01-04', 3550, 5, 608475.00, 'AMZN', 450),
('2024-01-05', 4200, 8, 425000.00, 'AAPL', 1200);
