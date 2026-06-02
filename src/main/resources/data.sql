-- Sample seed data loaded on startup (dev profile only — create-drop recreates schema first)
-- This file is automatically picked up by Spring Boot when spring.jpa.hibernate.ddl-auto=create-drop

INSERT INTO orders (customer_name, customer_email, status, created_at, updated_at)
VALUES
    ('Alice Smith',   'alice@example.com',  'PENDING',    NOW(), NOW()),
    ('Bob Johnson',   'bob@example.com',    'PROCESSING', NOW(), NOW()),
    ('Carol White',   'carol@example.com',  'SHIPPED',    NOW(), NOW()),
    ('David Brown',   'david@example.com',  'DELIVERED',  NOW(), NOW());

INSERT INTO order_items (order_id, product_name, product_code, quantity, unit_price)
VALUES
    (1, 'Wireless Keyboard', 'KB-001', 1, 49.99),
    (1, 'USB Mouse',         'MS-001', 2, 29.99),
    (2, 'Laptop Stand',      'LS-001', 1, 79.99),
    (3, 'HDMI Cable 2m',     'HC-002', 3, 12.99),
    (4, 'Webcam HD',         'WC-001', 1, 89.99),
    (4, 'Ring Light',        'RL-001', 1, 34.99);
