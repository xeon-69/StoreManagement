# System Overview

This document provides a comprehensive scan of the current status, architecture, and functionality of the Store Management System repository (`c:\POS`).

## 1. Tech Stack & Architecture
- **Language**: Java (JDK 17+)
- **UI Framework**: JavaFX utilizing FXML for layout and `AtlantaFX` (PrimerLight theme) for modern styling. Uses `ControlsFX` for specialized UI components like `GridView`.
- **Database**: SQLite (`store.db`), created and managed dynamically on startup (`schema.sql`).
- **Build Tool**: Maven (`pom.xml` configures `javafx-maven-plugin` and standard dependencies).
- **Architecture Pattern**: MVC/DAO/Service Architecture.
  - **Controllers** handle UI interactions and data binding.
  - **Services** encapsulating business logic and transaction management (e.g., `CheckoutService`).
  - **DAOs** (Data Access Objects) layer handling database CRUD operations using raw JDBC.
- **Internationalization (i18n)**: Fully supported via `ResourceBundle`. Currently supports English, Burmese (မြန်မာစာ), and Chinese (中文).

---

## 2. Core Modules & Functionality

### 2.1 Starting Point & Dashboard (`App.java` & `DashboardController.java`)
- **Login**: The application starts at a standard Login screen (`login.fxml`). Checks against the `users` table (default: admin/admin).
- **Dashboard**: The main shell of the application (`dashboard.fxml`). It features a side navigation pane to switch between different modules (POS, Inventory, Categories, Finance, Reports) and a top bar with a language selection dropdown. 
- **Session Management**: Tracks the logged-in user to stamp transactions with their ID (`SessionManager`). 

### 2.2 Point of Sale (POS) (`POSController.java` & `pos.fxml`)
- **Product Grid**: Displays products as visual tiles (Image, Name, Price, Stock level) using ControlsFX `GridView`.
- **Search**: Real-time filtering by product name or barcode.
- **Cart System**: A receipt-style table for cart items. Includes an intuitive '+ / -' stepper control on product tiles to add/remove items dynamically while checking stock limits.
- **Checkout Process**: Handled atomically by `CheckoutService`. When completing a sale, it:
  1. Creates a Sales Header (Total amount, Total calculated profit based on moving average or standard cost).
  2. Inserts individual Sale Items (locking in the historical price and cost).
  3. Deducts stock from inventory tables securely.

### 2.3 Inventory Management (`InventoryController.java` & `inventory.fxml`)
- **Product Listing**: Standard datatable view of all products including barcode, category, cost, price, and current stock. Provides inline images if available.
- **CRUD Operations**: Dedicated modals to Add or Edit products (`AddProductController`).
- **Stock Adjustments**: A specialized modal (`AdjustStockController`) for adding/removing stock outside of normal sales (e.g., damages, manual counts).
- **Inventory Ledger**: Complete traceability. Each product has a "Ledger" view (`InventoryTransactionDAO`) showing exactly when and why stock increased or decreased, tied to batches.
- **Batch & Expiry Management**: The system tracks products by batch, enabling FIFO/FEFO rules, allowing the system to scan for and retire expired items automatically or manually.

### 2.4 Categories (`CategoryController.java` & `categories.fxml`)
- simple CRUD management for grouping products.

### 2.5 Finance (`FinanceController.java` & `finance.fxml`)
- **Expense Tracking**: Allows users to log operational expenses (Rent, Utilities, etc.) with a description, amount, and automatic timestamp.

### 2.6 Reports (`ReportsController.java` & `reports.fxml`)
- **Sales Analytics**: Dashboard summaries showing "Total Sales" and "Total Profit".
- **Dynamic Filtering**: Presets for "Today", "This Week", "This Month", "All Time", and a "Custom Date Range" picker to filter sales data on the fly.
- **Recent Transactions Ledger**: A datatable listing all sales matching the filter, providing rapid visibility into cashier activity.

---

## 3. Background Processing
- **StoreMonitorService**: A daemon thread launched on startup to continuously check health, perform automated tasks (like marking items as expired), or refresh real-time dashboards in the background without freezing the UI.

---

## Conclusion
The repository forms a very cohesive, well-architected Minimum Viable Product (MVP). It employs excellent separation of concerns, robust transaction handling for checkouts (preventing data corruption during crashes), and a clean, responsive UI. It is primed perfectly for adding the remaining production features outlined in the `status_report.md`.
