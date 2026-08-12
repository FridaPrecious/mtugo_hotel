package com.mtugo.mtugo_hotel.controller;

import com.mtugo.mtugo_hotel.dto.CartDTO;
import com.mtugo.mtugo_hotel.dto.CartItemDTO;
import com.mtugo.mtugo_hotel.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/cart")
public class CartController {

    private static final String SESSION_CART_KEY = "cartItems";

    private final CartService cartService;

    @Autowired
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    // View cart page
    @GetMapping
    public String viewCart(HttpSession session, Model model) {
        List<CartItemDTO> items = getCartFromSession(session);
        CartDTO cart = cartService.getCart(items);
        model.addAttribute("cart", cart);
        return "cart";
    }

    // Add item to cart via AJAX
    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<CartDTO> addToCart(@RequestParam Long mealId,
                                             @RequestParam(defaultValue = "1") Integer quantity,
                                             HttpSession session) {
        List<CartItemDTO> items = getCartFromSession(session);
        items = cartService.addItem(items, mealId, quantity);
        session.setAttribute(SESSION_CART_KEY, items);
        CartDTO cart = cartService.getCart(items);
        return ResponseEntity.ok(cart);
    }

    // Update quantity
    @PostMapping("/update")
    @ResponseBody
    public ResponseEntity<CartDTO> updateQuantity(@RequestParam Long mealId,
                                                  @RequestParam Integer quantity,
                                                  HttpSession session) {
        List<CartItemDTO> items = getCartFromSession(session);
        items = cartService.updateQuantity(items, mealId, quantity);
        session.setAttribute(SESSION_CART_KEY, items);
        CartDTO cart = cartService.getCart(items);
        return ResponseEntity.ok(cart);
    }

    // Remove item
    @PostMapping("/remove")
    @ResponseBody
    public ResponseEntity<CartDTO> removeItem(@RequestParam Long mealId,
                                              HttpSession session) {
        List<CartItemDTO> items = getCartFromSession(session);
        items = cartService.removeItem(items, mealId);
        session.setAttribute(SESSION_CART_KEY, items);
        CartDTO cart = cartService.getCart(items);
        return ResponseEntity.ok(cart);
    }

    // Clear cart
    @PostMapping("/clear")
    public String clearCart(HttpSession session) {
        cartService.clearCart(getCartFromSession(session));
        session.removeAttribute(SESSION_CART_KEY);
        return "redirect:/cart";
    }

    // Get cart count for header
    @GetMapping("/count")
    @ResponseBody
    public ResponseEntity<Integer> getCartCount(HttpSession session) {
        List<CartItemDTO> items = getCartFromSession(session);
        int count = items.stream().mapToInt(CartItemDTO::getQuantity).sum();
        return ResponseEntity.ok(count);
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