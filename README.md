# Desktop Store Management System (JavaFX + SQLite)

A complete Point of Sale (POS) system for desktop, featuring inventory management, sales tracking, financial reporting, and data export.

## Key Features
- **Point of Sale**: Fast checkout, barcode scanning support.
- **Inventory Control**: Stock tracking, low stock alerts, product management.
- **Finance**: Expense tracking, profit/loss calculation.
- **Exporting**: PDF/Excel reports for sales and inventory.
- **User Management**: Admin and Cashier roles.

## 🚀 Installation & Setup

You do not need to compile the code or install Java manually to run this application. Pre-packaged installers are available directly on GitHub.

1. Go to the **Releases** page of this repository (located on the right-hand sidebar).
2. Download the latest `.msi` installer from the **Assets** section.
3. Run the installer and follow the setup wizard on your desktop.

### 🗄️ Database Initialization
The application automatically creates and initializes its database file (`store.db`) in its running directory upon the very first launch. No manual SQL configuration is required.

### 🔑 Default Login
- **Username**: `admin`
- **Password**: `admin`

---

## 🛠️ Development & Building from Source

If you want to modify the source code, run the project locally, or compile your own distribution packages, follow the steps below.

### Prerequisites
- **Java JDK 17+**: Required for modern JavaFX features.
- **Maven**: For dependency management and project building.

### Running the Application Locally
1. Clone the repository.
2. Execute the following command in your terminal to start the development environment:
   ```bash
   mvn javafx:run
   ```

### Building for Distribution (EXE / MSI)
We use the native `jpackage` tool to bundle the application along with a lightweight, custom runtime environment so it can run standalone.

1. Build the shaded fat JAR:
   ```bash
   mvn package
   ```
   
2. Run the `jpackage` command (ensure you are executing this on a Windows environment):
   ```bash
   jpackage --input target/ \
     --name StoreManager \
     --main-jar store-manager-1.0.jar \
     --main-class com.pos.system.App \
     --type app-image \
     --win-dir-chooser
   ```
   *(Note: You can swap `--type app-image` out for `--type msi` or `--type exe` depending on your target distribution format).*

---

## Project Structure
- `src/main/java`: Source code (Controllers, Models, DAOs).
- `src/main/resources`: FXML views, CSS styles, and the core SQL schema.
- `store.db`: The local SQLite database file generated at runtime.
