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
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

public class ProductCatalogViewModel {

    private final ObservableList<Product> allProducts = FXCollections.observableArrayList();
    private final ObservableList<Product> filteredProducts = FXCollections.observableArrayList();
    private final ObjectProperty<Product> selectedProduct = new SimpleObjectProperty<>();

    private String currentQuery = "";
    private Integer currentCategoryId = null; // null or 0 for All

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
            applyFilters(); // Initially show all/filtered
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
        this.currentQuery = query == null ? "" : query.trim().toLowerCase();
        applyFilters();
    }

    public void filterByCategory(Integer categoryId) {
        // Treat 0 or null as "All Categories"
        if (categoryId != null && categoryId == 0) {
            this.currentCategoryId = null;
        } else {
            this.currentCategoryId = categoryId;
        }
        applyFilters();
    }

    private void applyFilters() {
        List<Product> result = allProducts.stream()
                .filter(p -> {
                    // Search Filter
                    String name = p.getName() != null ? p.getName().toLowerCase() : "";
                    String barcode = p.getBarcode() != null ? p.getBarcode().toLowerCase() : "";

                    boolean matchesSearch = currentQuery.isEmpty() ||
                            name.contains(currentQuery) ||
                            barcode.contains(currentQuery);

                    // Category Filter
                    boolean matchesCategory = currentCategoryId == null ||
                            p.getCategoryId() == currentCategoryId;

                    return matchesSearch && matchesCategory;
                })
                .collect(Collectors.toList());

        filteredProducts.setAll(result);
    }

    public ObservableList<Product> getFilteredProducts() {
        return filteredProducts;
    }

    public ObservableList<Product> getAllProducts() {
        return allProducts;
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
