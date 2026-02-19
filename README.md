# Desktop Store Management System (JavaFX + SQLite)

A complete Point of Sale (POS) system for desktop, featuring inventory management, sales tracking, financial reporting, and data export.

## Key Features
- **Point of Sale**: Fast checkout, barcode scanning support.
- **Inventory Control**: Stock tracking, low stock alerts, product management.
- **Finance**: Expense tracking, profit/loss calculation.
- **Exporting**: PDF/Excel reports for sales and inventory.
- **User Management**: Admin and Cashier roles.

## Prerequisites
- **Java JDK 17+**: Required for modern JavaFX.
- **Maven**: For dependency management and building.

## Getting Started

1.  **Database Setup**:
    The application will automatically create `store.db` in the run directory upon first launch, using `src/main/resources/schema.sql`.

2.  **Running the Application**:
    ```bash
    mvn javafx:run
    ```

3.  **Building for Distribution (EXE)**:
    We use `jpackage` to bundle the application with a custom JRE.
    
    First, build the shaded JAR (fat jar):
    ```bash
    mvn package
    ```
    
    Then, run the `jpackage` command (ensure you are on Windows):
    ```bash
    jpackage --input target/ \
      --name StoreManager \
      --main-jar store-manager-1.0.jar \
      --main-class com.pos.system.App \
      --type app-image \
      --win-dir-chooser
    ```
    *(Note: You can use `--type exe` or `--type msi` for installers)*

## Project Structure
- `src/main/java`: Source code (Controllers, Models, DAOs).
- `src/main/resources`: FXML views, CSS, and SQL schema.
- `store.db`: The SQLite database file (created at runtime).

## Default Login
- **Username**: `admin`
- **Password**: `SalesAdmin123`
