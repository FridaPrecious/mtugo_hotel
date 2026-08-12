package com.mtugo.mtugo_hotel.controller;

import com.mtugo.mtugo_hotel.dto.CartDTO;
import com.mtugo.mtugo_hotel.dto.CartItemDTO;
import com.mtugo.mtugo_hotel.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private static final String SESSION_CART_KEY = "cartItems";

    private final CartService cartService;

    @Autowired
    public CheckoutController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public String checkoutPage(HttpSession session, Model model) {
        List<CartItemDTO> items = getCartFromSession(session);
        if (items.isEmpty()) {
            return "redirect:/cart";
        }
        CartDTO cart = cartService.getCart(items);
        model.addAttribute("cart", cart);
        return "checkout";
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