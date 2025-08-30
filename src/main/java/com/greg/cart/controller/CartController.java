package com.greg.cart.controller;

import com.greg.cart.model.Cart;
import com.greg.cart.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // Synchronous add item endpoint (no virtual threads)
    @PostMapping("/add-item/sync/{userId}")
    public ResponseEntity<Void> addItemSync(@PathVariable String userId, @RequestBody Cart.CartItem item) {
        cartService.addItemSync(userId, item);
        return ResponseEntity.ok().build();
    }

    // Asynchronous add item endpoint using virtual threads
    @PostMapping("/add-item/async/{userId}")
    public ResponseEntity<Void> addItemAsync(@PathVariable String userId, @RequestBody Cart.CartItem item) {
        cartService.addItemAsync(userId, item);
        return ResponseEntity.ok().build();
    }
}
