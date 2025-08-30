package com.greg.cart.service;

import com.greg.cart.model.Cart;
import com.greg.cart.repository.CartRepository;
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
public class CartService {
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private ExecutorService virtualThreadExecutor; // Injected for selective virtual thread usage
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
        }
    }
    // Synchronous add item (no virtual threads, blocking I/O)
    public void addItemSync(String userId, Cart.CartItem item) {
        Cart cart = cartRepository.findByUserId(userId);
        if (cart == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found for user: " + userId);
        }
        cart.getItems().add(item);
        cartRepository.save(cart); // Blocking save
    }

    // Asynchronous add item using virtual threads (offloads to virtual thread, but waits for completion)
    public void addItemAsync(String userId, Cart.CartItem item) {
        CompletableFuture.runAsync(() -> {
            Cart cart = cartRepository.findByUserId(userId);
            if (cart == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found for user: " + userId);
            }
            cart.getItems().add(item);
            cartRepository.save(cart); // Save on virtual thread
        }, virtualThreadExecutor).join(); // Wait for completion to ensure success before returning
    }
}