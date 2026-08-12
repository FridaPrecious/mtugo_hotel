package com.mtugo.mtugo_hotel.service;

import com.mtugo.mtugo_hotel.dto.CartDTO;
import com.mtugo.mtugo_hotel.dto.CartItemDTO;
import com.mtugo.mtugo_hotel.entity.Meal;
import com.mtugo.mtugo_hotel.repository.MealRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    private final MealRepository mealRepository;

    @Autowired
    public CartService(MealRepository mealRepository) {
        this.mealRepository = mealRepository;
    }

    public CartDTO getCart(List<CartItemDTO> items) {
        if (items == null) {
            items = new ArrayList<>();
        }
        CartDTO cart = CartDTO.builder().items(items).build();
        cart.calculateTotals();
        return cart;
    }

    public List<CartItemDTO> addItem(List<CartItemDTO> items, Long mealId, Integer quantity) {
        if (items == null) {
            items = new ArrayList<>();
        }

        Optional<Meal> mealOpt = mealRepository.findById(mealId);
        if (mealOpt.isEmpty()) {
            throw new RuntimeException("Meal not found");
        }

        Meal meal = mealOpt.get();

        // Check if item already exists in cart
        for (CartItemDTO item : items) {
            if (item.getMealId().equals(mealId)) {
                item.setQuantity(item.getQuantity() + quantity);
                item.setSubtotal(item.getPrice() * item.getQuantity());
                return items;
            }
        }

        // Add new item
        CartItemDTO newItem = CartItemDTO.builder()
                .mealId(mealId)
                .name(meal.getName())
                .price(meal.getPrice())
                .imageUrl(meal.getImageUrl())
                .quantity(quantity)
                .subtotal(meal.getPrice() * quantity)
                .build();
        items.add(newItem);
        return items;
    }

    public List<CartItemDTO> updateQuantity(List<CartItemDTO> items, Long mealId, Integer quantity) {
        if (items == null) {
            return new ArrayList<>();
        }

        for (CartItemDTO item : items) {
            if (item.getMealId().equals(mealId)) {
                if (quantity <= 0) {
                    return removeItem(items, mealId);
                }
                item.setQuantity(quantity);
                item.setSubtotal(item.getPrice() * quantity);
                break;
            }
        }
        return items;
    }

    public List<CartItemDTO> removeItem(List<CartItemDTO> items, Long mealId) {
        if (items == null) {
            return new ArrayList<>();
        }
        items.removeIf(item -> item.getMealId().equals(mealId));
        return items;
    }

    public void clearCart(List<CartItemDTO> items) {
        if (items != null) {
            items.clear();
        }
    }
}