package com.mtugo.mtugo_hotel.controller;

import com.mtugo.mtugo_hotel.dto.CartDTO;
import com.mtugo.mtugo_hotel.dto.CartItemDTO;
import com.mtugo.mtugo_hotel.entity.Order;
import com.mtugo.mtugo_hotel.service.CartService;
import com.mtugo.mtugo_hotel.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private static final String SESSION_CART_KEY = "cartItems";

    private final CartService cartService;
    private final OrderService orderService;

    @Autowired
    public CheckoutController(CartService cartService, OrderService orderService) {
        this.cartService = cartService;
        this.orderService = orderService;
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

    /**
     * Creates one Order per cart item (grouped so they're paid for and tracked
     * together), then clears the cart. Returns a primary orderId + the combined
     * total so the frontend can feed straight into the existing /api/mpesa/stkpush
     * flow unchanged.
     */
    @PostMapping("/api/create-orders")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createOrdersFromCart(
            @RequestBody(required = false) Map<String, String> body,
            HttpSession session) {

        List<CartItemDTO> items = getCartFromSession(session);
        if (items.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cart is empty"));
        }

        String phone = body != null ? body.get("phone") : null;
        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phone number is required"));
        }

        LocalDateTime pickupTime = null;
        String pickupTimeRaw = body.get("pickupTime");
        if (pickupTimeRaw != null && !pickupTimeRaw.isBlank()) {
            pickupTime = LocalDateTime.parse(pickupTimeRaw);
        }

        CartDTO cart = cartService.getCart(items);
        List<Order> createdOrders = orderService.createOrderFromCart(cart, phone, pickupTime);

        // Clear the cart now that orders have been created from it
        session.removeAttribute(SESSION_CART_KEY);

        Order primary = createdOrders.get(0);
        double total = createdOrders.stream().mapToDouble(Order::getTotalAmount).sum();

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", primary.getId());
        response.put("totalAmount", total);
        response.put("itemCount", createdOrders.size());
        return ResponseEntity.ok(response);
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
