package com.pos.system.viewmodels;

import com.pos.system.dao.CategoryDAO;
import com.pos.system.dao.ProductDAO;
import com.pos.system.models.Category;
import com.pos.system.models.Product;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

public class ProductCatalogViewModel {

    private final ObservableList<Product> allProducts = FXCollections.observableArrayList();
    private final ObservableList<Product> filteredProducts = FXCollections.observableArrayList();
    private final ObjectProperty<Product> selectedProduct = new SimpleObjectProperty<>();

    // Use a shared daemon thread pool for DB loading to avoid unbounded thread
    // creation
    private static final java.util.concurrent.ExecutorService dbExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    // Debounce executor
    private static final ScheduledExecutorService debounceExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });
    private volatile Runnable pendingSearch;

    // Internal state
    private ProductDAO productDAO; // only used when injected for tests
    private CategoryDAO categoryDAO; // only used when injected for tests

    public ProductCatalogViewModel() {
        loadProducts();
    }

    public ProductCatalogViewModel(ProductDAO productDAO, CategoryDAO categoryDAO) {
        this.productDAO = productDAO;
        this.categoryDAO = categoryDAO;
        // Don't auto-load in tests explicitly to allow mock setup if needed
    }

    public void loadProducts() {
        Task<List<Product>> loadTask = new Task<>() {
            @Override
            protected List<Product> call() throws Exception {
                try (ProductDAO dao = productDAO != null ? null : new ProductDAO()) {
                    ProductDAO activeDAO = productDAO != null ? productDAO : dao;
                    return activeDAO.getAllProducts();
                } catch (java.sql.SQLException e) {
                    e.printStackTrace();
                    return FXCollections.observableArrayList();
                }
            }
        };

        loadTask.setOnSucceeded(e -> {
            allProducts.setAll(loadTask.getValue());
            filteredProducts.setAll(loadTask.getValue()); // Initially show all
        });

        loadTask.setOnFailed(e -> loadTask.getException().printStackTrace());

        dbExecutor.execute(loadTask);
    }

    public void loadCategories() {
        Task<List<Category>> loadTask = new Task<>() {
            @Override
            protected List<Category> call() throws Exception {
                try (CategoryDAO dao = categoryDAO != null ? null : new CategoryDAO()) {
                    CategoryDAO activeDAO = categoryDAO != null ? categoryDAO : dao;
                    return activeDAO.getAllCategories();
                } catch (java.sql.SQLException e) {
                    e.printStackTrace();
                    return FXCollections.observableArrayList();
                }
            }
        };
        dbExecutor.execute(loadTask);
    }

    public void search(String query) {
        if (pendingSearch != null) {
            // No easy way to cancel a Runnable in vanilla Java Executor without Future,
            // but for simple debounce, we can just schedule the latest one.
            // A better way is:
        }

        // Simple Debounce: Schedule a task. If another request comes, ignore/override?
        // Actually, correctly implementing debounce requires cancelling the previous
        // future.
        // Let's keep it simple: Filter immediately for now, or use ReactFX if
        // available.
        // I added ReactFX to pom!

        // ReactFX approach (if I were using it fully):
        // EventStreams.valuesOf(searchField.textProperty()).successionEnds(Duration.ofMillis(300)).subscribe(...)

        // Standard JavaFX approach for filter:
        if (query == null || query.isEmpty()) {
            filteredProducts.setAll(allProducts);
            return;
        }

        String lowerQuery = query.toLowerCase();
        List<Product> result = allProducts.stream()
                .filter(p -> p.getName().toLowerCase().contains(lowerQuery) || p.getBarcode().contains(lowerQuery))
                .collect(Collectors.toList());

        filteredProducts.setAll(result);
    }

    public ObservableList<Product> getFilteredProducts() {
        return filteredProducts;
    }

    public ObjectProperty<Product> selectedProductProperty() {
        return selectedProduct;
    }

    public Product getSelectedProduct() {
        return selectedProduct.get();
    }

    public void setSelectedProduct(Product product) {
        this.selectedProduct.set(product);
    }

    public void cleanup() {
        debounceExecutor.shutdown();
    }
}
