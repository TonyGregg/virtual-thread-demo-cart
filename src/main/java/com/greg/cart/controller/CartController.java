package com.greg.cart.controller;

import com.greg.cart.model.Cart;
import com.greg.cart.service.CartService;
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/carts")
public class CartController {
    @Autowired
    private CartService cartService;

    @PostMapping
    public Cart createCart(@RequestBody Cart cart) {
        return cartService.createCart(cart);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cart> getCart(@PathVariable String id) {
        return cartService.getCart(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public Cart updateCart(@PathVariable String id, @RequestBody Cart cart) {
        return cartService.updateCart(id, cart);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCart(@PathVariable String id) {
        cartService.deleteCart(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Cart> getCartByUserId(@PathVariable String userId) {
        Cart cart = cartService.getCartByUserId(userId);
        return cart != null ? ResponseEntity.ok(cart) : ResponseEntity.notFound().build();
    }

    // Endpoint to generate fake carts (for testing)
    @PostMapping("/fake/{count}")
    public ResponseEntity<String> generateFakeCarts(@PathVariable int count) {
        cartService.generateFakeCarts(count);
        return ResponseEntity.ok("Generated " + count + " fake carts.");
    }

    @GetMapping
    public ResponseEntity<List<Cart>> getAllCarts() {
        List<Cart> carts = cartService.findAll();
        return ResponseEntity.ok(carts);
    }

    @GetMapping("/getAllUsersIds")
    public ResponseEntity<Set<String>> getAllUserIds() {
        Set<String> userIds = cartService.getAllUserIds();
        return ResponseEntity.ok(userIds);
    }

    // Synchronous add item endpoint (no virtual threads)
    @PostMapping("/add-item/sync/{userId}")
    public ResponseEntity<Void> addItemSync(@PathVariable String userId, @RequestBody Cart.CartItem item) {
        if (item == null) {
            item = getFakeCartItem();
        }
        cartService.addItemSync(userId, item);
        return ResponseEntity.ok().build();
    }

    // Asynchronous add item endpoint using virtual threads
    @PostMapping("/add-item/async/{userId}")
    public ResponseEntity<Void> addItemAsync(@PathVariable String userId, @RequestBody Cart.CartItem item) {
        if (item == null) {
            item = getFakeCartItem();
        }
        cartService.addItemAsync(userId, item);
        return ResponseEntity.ok().build();
    }

    // Synchronous remove item endpoint (no virtual threads)
    @PostMapping("/remove-item/sync/{userId}")
    public ResponseEntity<Void> removeItemSync(@PathVariable String userId, @RequestBody String productId) {
        cartService.removeItemSync(userId, productId);
        return ResponseEntity.ok().build();
    }

    // Asynchronous remove item endpoint using virtual threads
    @PostMapping("/remove-item/async/{userId}")
    public ResponseEntity<Void> removeItemAsync(@PathVariable String userId, @RequestBody String productId) {
        cartService.removeItemAsync(userId, productId);
        return ResponseEntity.ok().build();
    }

    // Synchronous add or increase item quantity endpoint (no virtual threads)
    @PostMapping("/add-or-increase/sync/{userId}")
    public ResponseEntity<Void> addOrIncreaseItemSync(@PathVariable String userId, @RequestBody Cart.CartItem item) {
        if (item == null) {
            item = getFakeCartItem();
        }
        cartService.addOrIncreaseItemSync(userId, item);
        return ResponseEntity.ok().build();
    }

    // Asynchronous add or increase item quantity endpoint using virtual threads
    @PostMapping("/add-or-increase/async/{userId}")
    public ResponseEntity<Void> addOrIncreaseItemAsync(@PathVariable String userId, @RequestBody Cart.CartItem item) {
        if (item == null) {
            item = getFakeCartItem();
        }
        cartService.addOrIncreaseItemAsync(userId, item);
        return ResponseEntity.ok().build();
    }

    private Cart.CartItem getFakeCartItem() {
        Faker faker = new Faker();

        Cart.CartItem item = new Cart.CartItem();
        item.setProductId(faker.commerce().promotionCode());
        item.setProductName(faker.commerce().productName());
        item.setQuantity(faker.number().numberBetween(1, 10));
        item.setPrice(faker.number().randomDouble(2, 10, 1000));
        return item;
    }
}
