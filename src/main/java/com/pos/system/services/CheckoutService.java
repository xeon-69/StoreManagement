package com.pos.system.services;

import com.pos.system.dao.ProductDAO;
import com.pos.system.dao.SaleDAO;
import com.pos.system.dao.SalePaymentDAO;
import com.pos.system.database.DatabaseManager;
import com.pos.system.models.Sale;
import com.pos.system.models.SaleItem;
import com.pos.system.models.SalePayment;
import com.pos.system.models.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class CheckoutService {
    private static final Logger logger = LoggerFactory.getLogger(CheckoutService.class);

    private final InventoryService inventoryService;

    public CheckoutService() {
        this(new InventoryService());
    }

    // For Dependency Injection in testing
    public CheckoutService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    // Protected method to allow mocking the connection in tests
    protected Connection getConnection() throws SQLException {
        return DatabaseManager.getInstance().getConnection();
    }

    // Protected methods to allow mocking DAOs in tests
    protected SaleDAO getSaleDAO(Connection connection) {
        return new SaleDAO(connection);
    }

    protected ProductDAO getProductDAO(Connection connection) {
        return new ProductDAO(connection);
    }

    protected SalePaymentDAO getSalePaymentDAO(Connection connection) {
        return new SalePaymentDAO(connection);
    }

    public void processCheckout(Sale sale, List<SaleItem> items) throws SQLException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false); // Begin transaction

            try (SaleDAO saleDAO = getSaleDAO(connection);
                    ProductDAO productDAO = getProductDAO(connection)) {

                // 1. Create Sale Header
                int saleId = saleDAO.insertSale(sale);
                logger.info("Sale header created with ID: {}", saleId);

                // 2. Insert Sale Items
                saleDAO.insertSaleItems(saleId, items);
                logger.info("Sale items inserted for Sale ID: {}", saleId);

                // 3. Update Inventory (Atomic Decrement)
                for (SaleItem item : items) {
                    inventoryService.deductStock(connection, item.getProductId(), item.getQuantity(),
                            TransactionType.SALE, "SALE-" + saleId, sale.getUserId());
                }
                logger.info("Inventory updated for {} items.", items.size());

                // 4. Commit Transaction
                connection.commit();
                logger.info("Checkout transaction committed successfully.");

            } catch (SQLException e) {
                logger.error("Checkout failed, rolling back transaction.", e);
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true); // Reset auto-commit
            }
        }
    }

    public void processCheckoutWithPayments(Sale sale, List<SaleItem> items,
            List<SalePayment> payments) throws SQLException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false); // Begin transaction

            try (SaleDAO saleDAO = getSaleDAO(connection);
                    ProductDAO productDAO = getProductDAO(connection);
                    SalePaymentDAO paymentDAO = getSalePaymentDAO(connection)) {

                // 1. Create Sale Header
                int saleId = saleDAO.insertSale(sale);
                sale.setId(saleId);
                logger.info("Sale header created with ID: {}", saleId);

                // 2. Insert Sale Items
                saleDAO.insertSaleItems(saleId, items);
                logger.info("Sale items inserted for Sale ID: {}", saleId);

                // 3. Insert Sale Payments
                for (SalePayment item : payments) {
                    item.setSaleId(saleId);
                    paymentDAO.create(item);
                }

                // 4. Update Inventory (Atomic Decrement)
                for (SaleItem item : items) {
                    inventoryService.deductStock(connection, item.getProductId(), item.getQuantity(),
                            TransactionType.SALE, "SALE-" + saleId, sale.getUserId());
                }
                logger.info("Inventory updated for {} items.", items.size());

                // 5. Commit Transaction
                connection.commit();
                logger.info("Checkout transaction committed successfully.");

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
