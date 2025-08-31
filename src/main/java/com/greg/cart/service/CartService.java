package com.greg.cart.service;

import com.greg.cart.model.Cart;
import com.greg.cart.repository.CartRepository;
import net.datafaker.Faker;
import net.datafaker.providers.base.Camera;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
public class CartService {
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private ExecutorService virtualThreadExecutor;
    @Autowired
    private MongoTemplate mongoTemplate;
    // Injected for selective virtual thread usage
    // CRUD Operations
    public Cart createCart(Cart cart) {
        Faker faker = new Faker();
        cart.setUserId(faker.idNumber().valid());
        List<Cart.CartItem> items = new ArrayList<>();

        int itemCount = faker.number().numberBetween(1, 10);
        for (int j = 0; j < itemCount; j++) {
            Cart.CartItem item = new Cart.CartItem();
            item.setProductId(faker.commerce().promotionCode());
            item.setProductName(faker.commerce().productName());
            item.setQuantity(faker.number().numberBetween(1, 10));
            item.setPrice(faker.number().randomDouble(2, 10, 1000));
            items.add(item);
        }
        cart.setItems(items);
        return cartRepository.save(cart);
    }

    public Optional<Cart> getCart(String id) {
        return cartRepository.findById(id);
    }

    // Get unique user IDs
    public Set<String> getAllUserIds() {
        return new HashSet<>(mongoTemplate.findDistinct("userId", Cart.class, String.class));
    }

    public Cart updateCart(String id, Cart updatedCart) {
        return cartRepository.findById(id)
                .map(cart -> {
                    cart.setUserId(updatedCart.getUserId());
                    cart.setItems(updatedCart.getItems());
                    return cartRepository.save(cart);
                })
                .orElseThrow(() -> new RuntimeException("Cart not found"));
    }

    public void deleteCart(String id) {
        cartRepository.deleteById(id);
    }

    // Find by userId (for convenience)
    public Cart getCartByUserId(String userId) {
        return cartRepository.findByUserId(userId);
    }

    public List<Cart> findAll() {
        return cartRepository.findAll();
    }

    // Generate fake carts (using DataFaker)
    public void generateFakeCarts(int count) {
        Faker faker = new Faker();
        for (int i = 0; i < count; i++) {
            virtualThreadExecutor.submit(() -> {
                Cart cart = new Cart();
                cart.setUserId(faker.idNumber().valid());

                List<Cart.CartItem> items = new ArrayList<>();
                int itemCount = faker.number().numberBetween(1, 5);
                for (int j = 0; j < itemCount; j++) {
                    Cart.CartItem item = new Cart.CartItem();
                    item.setProductId(faker.commerce().promotionCode());
                    item.setProductName(faker.commerce().productName());
                    item.setQuantity(faker.number().numberBetween(1, 10));
                    item.setPrice(faker.number().randomDouble(2, 10, 1000));
                    items.add(item);
                }
                cart.setItems(items);

                cartRepository.save(cart);
            });

        }
    }
    // Synchronous add item (no virtual threads, blocking I/O)
    public void addItemSync(String userId, Cart.CartItem item) {
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item cannot be null");
        }
        Cart cart = cartRepository.findByUserId(userId);
        if (cart == null) {
            cart = new Cart();
            cart.setUserId(userId);
            cart.setItems(new ArrayList<>());
        }
        cart.getItems().add(item);
        cartRepository.save(cart); // Blocking save
    }

    // Asynchronous add item using virtual threads (offloads to virtual thread, but waits for completion)
    public void addItemAsync(String userId, Cart.CartItem item) {
        virtualThreadExecutor.submit(() -> {
            if (item == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item cannot be null");
            }
            Cart cart = cartRepository.findByUserId(userId);
            if (cart == null) {
                cart = new Cart();
                cart.setUserId(userId);
                cart.setItems(new ArrayList<>());
            }
            cart.getItems().add(item);
            cartRepository.save(cart); // Save on virtual thread
        });
    }

    // Synchronous remove item (no virtual threads, blocking I/O)
    public void removeItemSync(String userId, String productId) {
        if (productId == null || productId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product ID cannot be null or empty");
        }
        Cart cart = cartRepository.findByUserId(userId);
        if (cart == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found for user: " + userId);
        }
        boolean removed = cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item with product ID " + productId + " not found in cart");
        }
        cartRepository.save(cart); // Blocking save
    }

    // Asynchronous remove item using virtual threads (offloads to virtual thread, but waits for completion)
    public void removeItemAsync(String userId, String productId) {
        virtualThreadExecutor.submit(() -> {
            if (productId == null || productId.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product ID cannot be null or empty");
            }
            Cart cart = cartRepository.findByUserId(userId);
            if (cart == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found for user: " + userId);
            }
            boolean removed = cart.getItems().removeIf(item -> item.getProductId().equals(productId));
            if (!removed) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item with product ID " + productId + " not found in cart");
            }
            cartRepository.save(cart); // Save on virtual thread
        }, virtualThreadExecutor); // Wait for completion to ensure success before returning
    }

    // Synchronous add or increase item quantity (no virtual threads, blocking I/O)
    public void addOrIncreaseItemSync(String userId, Cart.CartItem item) {
        if (item == null || item.getProductId() == null || item.getProductId().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item or product ID cannot be null or empty");
        }
        if (item.getQuantity() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity cannot be zero");
        }
        Cart cart = cartRepository.findByUserId(userId);
        if (cart == null) {
            cart = new Cart();
            cart.setUserId(userId);
            cart.setItems(new ArrayList<>());
        }
        Optional<Cart.CartItem> existingItem = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(item.getProductId()))
                .findFirst();
        if (existingItem.isPresent()) {
            int newQuantity = existingItem.get().getQuantity() + item.getQuantity();
            if (newQuantity <= 0) {
                cart.getItems().removeIf(i -> i.getProductId().equals(item.getProductId()));
            } else {
                existingItem.get().setQuantity(newQuantity);
            }
        } else {
            if (item.getQuantity() > 0) {
                cart.getItems().add(item);
            }
        }
        cartRepository.save(cart); // Blocking save
    }

    // Asynchronous add or increase item quantity using virtual threads (offloads to virtual thread, but waits for completion)
    public void addOrIncreaseItemAsync(String userId, Cart.CartItem item) {
        virtualThreadExecutor.submit(() -> {
            if (item == null || item.getProductId() == null || item.getProductId().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item or product ID cannot be null or empty");
            }
            if (item.getQuantity() == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity cannot be zero");
            }
            Cart cart = cartRepository.findByUserId(userId);
            if (cart == null) {
                cart = new Cart();
                cart.setUserId(userId);
                cart.setItems(new ArrayList<>());
            }
            Optional<Cart.CartItem> existingItem = cart.getItems().stream()
                    .filter(i -> i.getProductId().equals(item.getProductId()))
                    .findFirst();
            if (existingItem.isPresent()) {
                int newQuantity = existingItem.get().getQuantity() + item.getQuantity();
                if (newQuantity <= 0) {
                    cart.getItems().removeIf(i -> i.getProductId().equals(item.getProductId()));
                } else {
                    existingItem.get().setQuantity(newQuantity);
                }
            } else {
                if (item.getQuantity() > 0) {
                    cart.getItems().add(item);
                }
            }
            cartRepository.save(cart); // Save on virtual thread
        }, virtualThreadExecutor);
    }
}