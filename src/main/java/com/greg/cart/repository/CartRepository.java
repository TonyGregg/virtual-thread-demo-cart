package com.greg.cart.repository;

import com.greg.cart.model.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CartRepository extends MongoRepository<Cart, String> {
    Cart findByUserId(String userId); // Custom query for finding by userId
}
