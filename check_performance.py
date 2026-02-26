import sqlite3
import time
import random
import os

DB_PATH = "store.db"

def init_db():
    if os.path.exists(DB_PATH):
        os.remove(DB_PATH)

    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    # Read schema from file or just use the important parts
    schema = """
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
    CREATE TABLE IF NOT EXISTS sales (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_id INTEGER,
        subtotal REAL DEFAULT 0.0,
        tax_amount REAL DEFAULT 0.0,
        discount_amount REAL DEFAULT 0.0,
        total_amount REAL NOT NULL,
        total_profit REAL,
        sale_date DATETIME DEFAULT CURRENT_TIMESTAMP
    );
    CREATE TABLE IF NOT EXISTS sale_items (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        sale_id INTEGER,
        product_id INTEGER,
        quantity INTEGER NOT NULL,
        price_at_sale REAL NOT NULL,
        cost_at_sale REAL,
        discount_amount REAL DEFAULT 0.0,
        tax_amount REAL DEFAULT 0.0
    );
    """
    cursor.executescript(schema)
    conn.commit()
    return conn

def populate_db(conn, num_products, num_sales):
    cursor = conn.cursor()

    print(f"Populating {num_products} products...")
    products = []
    # Create a dummy image blob (500KB)
    dummy_image = b'\x00' * 1024 * 500

    for i in range(num_products):
        products.append((
            f"BC{i}", f"Product {i}", 1, 10.0, 20.0, 100, dummy_image
        ))

    cursor.executemany("INSERT INTO products (barcode, name, category_id, cost_price, selling_price, stock, image_blob) VALUES (?, ?, ?, ?, ?, ?, ?)", products)
    conn.commit()

    print(f"Populating {num_sales} sales...")
    sales = []
    sale_items = []

    # Spread dates over last year
    from datetime import datetime, timedelta
    start_date = datetime.now() - timedelta(days=365)

    for i in range(num_sales):
        sale_date = start_date + timedelta(seconds=random.randint(0, 365*24*3600))
        sales.append((
            1, 100.0, 10.0, 0.0, 110.0, 50.0, sale_date.strftime("%Y-%m-%d %H:%M:%S")
        ))

        # Add 1-5 items per sale
        for _ in range(random.randint(1, 5)):
             sale_items.append((
                 i+1, random.randint(1, num_products), 1, 20.0, 10.0, 0.0, 2.0
             ))

    cursor.executemany("INSERT INTO sales (user_id, subtotal, tax_amount, discount_amount, total_amount, total_profit, sale_date) VALUES (?, ?, ?, ?, ?, ?, ?)", sales)
    cursor.executemany("INSERT INTO sale_items (sale_id, product_id, quantity, price_at_sale, cost_at_sale, discount_amount, tax_amount) VALUES (?, ?, ?, ?, ?, ?, ?)", sale_items)
    conn.commit()

def benchmark_products(conn):
    cursor = conn.cursor()
    start_time = time.time()
    cursor.execute("SELECT * FROM products")
    products = cursor.fetchall()
    end_time = time.time()
    print(f"Loaded {len(products)} products in {end_time - start_time:.4f} seconds")
    return end_time - start_time

def benchmark_products_summary(conn):
    cursor = conn.cursor()
    start_time = time.time()
    # Simulate getAllProductsSummary
    cursor.execute("SELECT id, barcode, name, category_id, cost_price, selling_price, stock FROM products")
    products = cursor.fetchall()
    end_time = time.time()
    print(f"Loaded {len(products)} products (summary) in {end_time - start_time:.4f} seconds")
    return end_time - start_time

def benchmark_sales_range(conn):
    cursor = conn.cursor()
    # Query for a full year
    from datetime import datetime, timedelta
    start_date = (datetime.now() - timedelta(days=365)).strftime("%Y-%m-%d %H:%M:%S")
    end_date = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    start_time = time.time()
    cursor.execute("""
        SELECT s.*,
        (SELECT GROUP_CONCAT(p.name || ' (x' || si.quantity || ')', ', ')
         FROM sale_items si JOIN products p ON si.product_id = p.id WHERE si.sale_id = s.id) AS details
        FROM sales s WHERE s.sale_date >= ? AND s.sale_date <= ? ORDER BY s.sale_date DESC
    """, (start_date, end_date))
    sales = cursor.fetchall()
    end_time = time.time()
    print(f"Loaded {len(sales)} sales with details in {end_time - start_time:.4f} seconds")
    return end_time - start_time

if __name__ == "__main__":
    conn = init_db()
    # 2,000 products, 1,000 sales (reduced for speed, checking blob load time)
    populate_db(conn, 2000, 1000)

    print("\n--- Benchmarking ---")
    benchmark_products(conn)
    benchmark_products_summary(conn)
    # benchmark_sales_range(conn) # Skip this, already known slow

    conn.close()
    if os.path.exists(DB_PATH):
        os.remove(DB_PATH)
