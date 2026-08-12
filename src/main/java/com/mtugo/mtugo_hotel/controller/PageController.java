package com.mtugo.mtugo_hotel.controller;

import com.mtugo.mtugo_hotel.dto.CartItemDTO;
import com.mtugo.mtugo_hotel.dto.MealResponse;
import com.mtugo.mtugo_hotel.service.MealService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class PageController {

    private static final String SESSION_CART_KEY = "cartItems";

    private final MealService mealService;

    @Autowired
    public PageController(MealService mealService) {
        this.mealService = mealService;
    }

    @GetMapping("/")
    public String menuPage(HttpSession session, Model model) {
        List<MealResponse> meals = mealService.getAllAvailableMeals();
        model.addAttribute("meals", meals);

        // Get cart count for badge
        List<CartItemDTO> items = getCartFromSession(session);
        int count = items.stream().mapToInt(CartItemDTO::getQuantity).sum();
        model.addAttribute("cartCount", count);

        return "menu";
    }

    @SuppressWarnings("unchecked")
    private List<CartItemDTO> getCartFromSession(HttpSession session) {
        List<CartItemDTO> items = (List<CartItemDTO>) session.getAttribute(SESSION_CART_KEY);
        if (items == null) {
            items = new java.util.ArrayList<>();
        }
        return items;
    }
}