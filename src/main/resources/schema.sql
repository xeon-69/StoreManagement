-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'CASHIER',
    force_password_change BOOLEAN DEFAULT 0
);
-- Migration for existing databases
ALTER TABLE users
ADD COLUMN force_password_change BOOLEAN DEFAULT 0;
-- Categories Table
CREATE TABLE IF NOT EXISTS categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    description TEXT
);
-- Products Table
CREATE TABLE IF NOT EXISTS products (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    barcode TEXT UNIQUE,
    name TEXT NOT NULL,
    category_id INTEGER,
    cost_price REAL NOT NULL DEFAULT 0.0,
    selling_price REAL NOT NULL,
    stock INTEGER NOT NULL DEFAULT 0,
    image_path TEXT,
    image_blob BLOB
);
-- Migration: Add image_path if missing (for existing databases)
ALTER TABLE products
ADD COLUMN image_path TEXT;
ALTER TABLE products
ADD COLUMN image_blob BLOB;
-- Settings Table (New for Store config, tax rates, etc.)
CREATE TABLE IF NOT EXISTS settings (
    setting_key TEXT PRIMARY KEY,
    setting_value TEXT
);
-- Sales Header Table
CREATE TABLE IF NOT EXISTS sales (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    subtotal REAL DEFAULT 0.0,
    tax_amount REAL DEFAULT 0.0,
    discount_amount REAL DEFAULT 0.0,
    total_amount REAL NOT NULL,
    total_profit REAL,
    sale_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
-- Migrations for existing databases
ALTER TABLE sales
ADD COLUMN subtotal REAL DEFAULT 0.0;
ALTER TABLE sales
ADD COLUMN tax_amount REAL DEFAULT 0.0;
ALTER TABLE sales
ADD COLUMN discount_amount REAL DEFAULT 0.0;
-- Sale Payments Table (New for Split Payments)
CREATE TABLE IF NOT EXISTS sale_payments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sale_id INTEGER NOT NULL,
    payment_method TEXT NOT NULL,
    amount REAL NOT NULL,
    payment_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE
);
-- Sale Items Table (Line Items)
CREATE TABLE IF NOT EXISTS sale_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sale_id INTEGER,
    product_id INTEGER,
    quantity INTEGER NOT NULL,
    price_at_sale REAL NOT NULL,
    cost_at_sale REAL,
    discount_amount REAL DEFAULT 0.0,
    tax_amount REAL DEFAULT 0.0,
    FOREIGN KEY (sale_id) REFERENCES sales(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);
-- Migrations for existing databases
ALTER TABLE sale_items
ADD COLUMN discount_amount REAL DEFAULT 0.0;
ALTER TABLE sale_items
ADD COLUMN tax_amount REAL DEFAULT 0.0;
-- Batches Table
CREATE TABLE IF NOT EXISTS batches (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id INTEGER NOT NULL,
    batch_number TEXT NOT NULL,
    expiry_date TEXT,
    cost_price REAL NOT NULL,
    remaining_quantity INTEGER NOT NULL CHECK (remaining_quantity >= 0),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE
);
-- Inventory Transactions Table
CREATE TABLE IF NOT EXISTS inventory_transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id INTEGER NOT NULL,
    batch_id INTEGER,
    quantity_change INTEGER NOT NULL,
    transaction_type TEXT NOT NULL,
    reference_id TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_by INTEGER,
    FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY(batch_id) REFERENCES batches(id)
);
-- Expenses Table
CREATE TABLE IF NOT EXISTS expenses (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    category TEXT NOT NULL,
    amount REAL NOT NULL,
    description TEXT,
    expense_date DATETIME DEFAULT CURRENT_TIMESTAMP
);
-- Audit Logs Table (New for Security)
CREATE TABLE IF NOT EXISTS audit_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    action TEXT NOT NULL,
    entity_name TEXT,
    entity_id TEXT,
    details TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
-- Initial Admin User (Default password: admin - BCrypt format)
-- We'll just put a known hash for 'admin': $2a$10$I2jdGIOzwA6L9EjQCfovRu9H1YCns/sKPR6zqI4kLo1ce/yRFF5R6
INSERT
    OR IGNORE INTO users (username, password, role, force_password_change)
VALUES (
        'admin',
        '$2a$10$I2jdGIOzwA6L9EjQCfovRu9H1YCns/sKPR6zqI4kLo1ce/yRFF5R6',
        'ADMIN',
        1
    );
-- Ensure password is updated if user already exists
UPDATE users
SET password = '$2a$10$I2jdGIOzwA6L9EjQCfovRu9H1YCns/sKPR6zqI4kLo1ce/yRFF5R6',
    force_password_change = 1
WHERE username = 'admin';