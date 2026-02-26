package com.pos.system.services;

import com.pos.system.dao.ProductDAO;
import javafx.concurrent.Task;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ImageCacheService {

    private static ImageCacheService instance;
    private static final int MAX_CACHE_SIZE = 200; // Limit to 200 images to prevent OOM

    private final Map<Integer, Image> cache;
    private final ExecutorService executor;
    private final Set<Integer> pendingRequests;

    private ImageCacheService() {
        // LRU Cache
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(MAX_CACHE_SIZE + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, Image> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        });

        this.executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("Image-Loader-Thread");
            return t;
        });

        this.pendingRequests = ConcurrentHashMap.newKeySet();
    }

    public static synchronized ImageCacheService getInstance() {
        if (instance == null) {
            instance = new ImageCacheService();
        }
        return instance;
    }

    public Image getCachedImage(int productId) {
        return cache.get(productId);
    }

    /**
     * Loads an image asynchronously.
     * @param productId The product ID to load
     * @param onSuccess Callback on JavaFX Application Thread with the loaded image
     */
    public void loadImage(int productId, Consumer<Image> onSuccess) {
        if (cache.containsKey(productId)) {
            Image img = cache.get(productId);
            if (img != null && onSuccess != null) {
                onSuccess.accept(img);
            }
            return;
        }

        if (pendingRequests.contains(productId)) {
            // Already loading, do nothing (or we could queue the callback, but simple dedup is fine for scrolling)
            return;
        }

        pendingRequests.add(productId);

        Task<byte[]> task = new Task<>() {
            @Override
            protected byte[] call() throws Exception {
                try (ProductDAO dao = new ProductDAO()) {
                    return dao.getProductImage(productId);
                }
            }
        };

        task.setOnSucceeded(e -> {
            pendingRequests.remove(productId);
            byte[] data = task.getValue();
            Image img = null;
            if (data != null && data.length > 0) {
                try {
                    img = new Image(new ByteArrayInputStream(data));
                } catch (Exception ex) {
                    // Invalid image
                }
            }
            // Cache even nulls to avoid repeatedly trying to fetch missing images
            cache.put(productId, img);

            if (img != null && onSuccess != null) {
                onSuccess.accept(img);
            }
        });

        task.setOnFailed(e -> {
            pendingRequests.remove(productId);
            // Optionally log error
        });

        executor.submit(task);
    }
}
