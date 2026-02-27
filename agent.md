# Agent Guide: Installer Creation & Version Updates

To ensure the MSI installer for the Store Manager POS system builds correctly, preserves user data during updates, and avoids performance issues, follow these protocols.

## 1. Installer Packaging Protocol (Prevention of JVM Launch Errors)
To prevent the **"Failed to launch JVM"** error (which occurs when `jpackage` incorrectly includes multiple JARs or conflicting dependencies in the installation bundle), always isolate the target fat JAR.

**Procedure:**
1. Run `mvn clean package -DskipTests`. (The `clean` is vital to remove old versions).
2. Create a clean input directory: `mkdir target/installer-input`.
3. Copy **only** the fat JAR to this directory: `copy target/store-manager-X.X.X.jar target/installer-input/`.
4. Run `jpackage` pointing to this isolated directory:
   ```powershell
   jpackage --input target/installer-input/ `
            --name StoreManager `
            --main-jar store-manager-X.X.X.jar `
            --main-class com.pos.system.Launcher `
            --type msi `
            --app-version X.X.X `
            --vendor "Store Management System" `
            --win-dir-chooser --win-shortcut --win-menu
   ```
*Verification: After building, check the installation size. If it's double the expected size (e.g. 200MB instead of 100MB), multiple JARs were likely included.*

## 2. Ensuring Data Persistence
The application is designed to preserve data automatically across updates and re-installations by using the user's roaming AppData directory.

- **Database Location:** All data is stored in `%APPDATA%\StoreManager\store.db`.
- **Persistence Rule:** Standard MSI uninstallation (and thus upgrades) will **not** remove the `%APPDATA%` directory or the SQLite database.
- **Verification:** Always check `com.pos.system.utils.AppDataUtils` to ensure it resolves to the correct platform-specific writable folder.

## 3. Smooth Update Behavior
To ensure a higher version is treated as an update by Windows:
- **Version Number:** Always increment the version in `pom.xml` and the `--app-version` flag (e.g., 1.0.4 -> 1.0.5).
- **Metadata Consistency:** Keep `--name` ("StoreManager") and `--vendor` ("Store Management System") identical across builds. This allows the Windows Installer service to recognize the existing installation and perform a "Major Upgrade" automatically.

## 4. Database Concurrency (Prevention of "Not Responding" Issues)
To prevent the application from entering a **"Not Responding"** state (hanging) during startup or high-load operations:

- **Enable WAL Mode**: Always ensure `PRAGMA journal_mode = WAL` is executed on startup in `DatabaseManager`. This prevents background tasks from blocking the UI thread.
- **Use Background Tasks**: Never run heavy queries directly on the JavaFX Application Thread. Always utilize `Task` or `Thread` with `Platform.runLater` for UI updates.
- **Tune Connection Pool**: Keep `maximumPoolSize` around 10 for SQLite to minimize lock contention.
- **Safe Migrations**: Wrap migrations in transactions and ensure high-resource tasks (like bulk updates) run in the background after the UI is visible.

## 5. Asynchronous Startup Requirement
To ensure the window is immediately "Responding" on Windows:
- **Off-thread Initialization**: Call `DatabaseManager.getInstance()` from a background thread in `App.start()`.
- **UI Responsiveness**: This allows the JavaFX `start` method to return quickly, starting the event loop and showing the window as active/interactive even if database setup is still in progress.
- **DAO Handling**: Since `DatabaseManager.getInstance()` is synchronized, any DAO used later by the UI thread will naturally wait for initialization to complete if it hasn't already.

## 6. Final Verification Checklist (Pre-Release)
- **JVM Check**: Run the generated MSI, install, and launch. If it fails with "Failed to launch JVM", verify Section 1.
- **Hanging Check**: Navigate to "Sales & Analytics" and generate a report. Verify the UI remains interactive.
- **Data Check**: Install a higher version over an existing one. Log in and verify that previous sales data and settings are intact.
