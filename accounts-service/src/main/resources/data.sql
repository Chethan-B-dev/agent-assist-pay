-- Sample account data for testing
INSERT INTO accounts (customer_id, balance, currency, status, created_at) VALUES
('c_123', 1000.00, 'USD', 'ACTIVE', NOW()),
('c_456', 500.50, 'USD', 'ACTIVE', NOW()),
('c_789', 250.75, 'USD', 'ACTIVE', NOW()),
('c_suspended', 100.00, 'USD', 'SUSPENDED', NOW()),
('c_low_balance', 10.00, 'USD', 'ACTIVE', NOW());