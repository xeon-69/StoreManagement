package com.pos.system.services;

import com.pos.system.dao.ProductDAO;
import com.pos.system.dao.SaleDAO;
import com.pos.system.database.DatabaseManager;
import com.pos.system.models.Sale;
import com.pos.system.models.SaleItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class CheckoutService {
    private static final Logger logger = LoggerFactory.getLogger(CheckoutService.class);

    public void processCheckout(Sale sale, List<SaleItem> items) throws SQLException {
        // Use a single connection for the entire transaction
        try (Connection connection = DatabaseManager.getInstance().getConnection()) {
            connection.setAutoCommit(false); // Begin transaction

            try (SaleDAO saleDAO = new SaleDAO(connection);
                    ProductDAO productDAO = new ProductDAO(connection)) {

                // 1. Create Sale Header
                int saleId = saleDAO.insertSale(sale);
                logger.info("Sale header created with ID: {}", saleId);

                // 2. Insert Sale Items
                saleDAO.insertSaleItems(saleId, items);
                logger.info("Sale items inserted for Sale ID: {}", saleId);

                // 3. Update Inventory (Atomic Decrement)
                for (SaleItem item : items) {
                    productDAO.updateStockQuantity(item.getProductId(), item.getQuantity());
                }
                logger.info("Inventory updated for {} items.", items.size());

                // 4. Commit Transaction
                connection.commit();
                logger.info("Checkout transaction committed successfully.");

                // 5. Trigger Receipt Implementation (TODO)
                // ReceiptService.generateReceipt(saleId);

            } catch (SQLException e) {
                logger.error("Checkout failed, rolling back transaction.", e);
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true); // Reset auto-commit
            }
        }
    }
}
