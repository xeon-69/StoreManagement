-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'CASHIER' -- 'ADMIN', 'CASHIER'
);
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
    category TEXT,
    -- Legacy text field, or FK? Keeping text for simplicity or migration. 
    -- ideally: category_id INTEGER REFERENCES categories(id)
    cost_price REAL NOT NULL DEFAULT 0.0,
    selling_price REAL NOT NULL,
    stock INTEGER NOT NULL DEFAULT 0,
    image_path TEXT,
    -- New column for product image
    image_blob BLOB
);
-- Migration: Add image_path if missing (for existing databases)
-- Note: This might fail if column exists, but DatabaseManager will ignore the error.
ALTER TABLE products
ADD COLUMN image_path TEXT;
ALTER TABLE products
ADD COLUMN image_blob BLOB;
-- Sales Header Table
CREATE TABLE IF NOT EXISTS sales (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    total_amount REAL NOT NULL,
    total_profit REAL,
    -- Calculated as (selling_price - cost_price) * quantity
    sale_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
-- Sale Items Table (Line Items)
CREATE TABLE IF NOT EXISTS sale_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sale_id INTEGER,
    product_id INTEGER,
    quantity INTEGER NOT NULL,
    price_at_sale REAL NOT NULL,
    -- Selling price at that moment
    cost_at_sale REAL,
    -- Cost price at that moment (to lock in profit logic)
    FOREIGN KEY (sale_id) REFERENCES sales(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);
-- Expenses Table (New for Finance)
CREATE TABLE IF NOT EXISTS expenses (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    category TEXT NOT NULL,
    -- e.g., 'Rent', 'Utilities', 'Salaries'
    amount REAL NOT NULL,
    description TEXT,
    expense_date DATETIME DEFAULT CURRENT_TIMESTAMP
);
-- Initial Admin User (Default password: admin)
INSERT
    OR IGNORE INTO users (username, password, role)
VALUES ('admin', 'admin', 'ADMIN');
-- Ensure password is updated if user already exists
UPDATE users
SET password = 'admin'
WHERE username = 'admin';