package com.mtugo.mtugo_hotel.controller;

import com.mtugo.mtugo_hotel.dto.CartDTO;
import com.mtugo.mtugo_hotel.dto.CartItemDTO;
import com.mtugo.mtugo_hotel.entity.Order;
import com.mtugo.mtugo_hotel.service.CartService;
import com.mtugo.mtugo_hotel.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

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
        model.addAttribute("totalAmount", cart.getTotalAmount());
        return "checkout";
    }

    @PostMapping("/place")
    public String placeOrder(HttpSession session,
                             @RequestParam("phoneNumber") String phoneNumber,
                             Model model) {
        List<CartItemDTO> items = getCartFromSession(session);
        if (items.isEmpty()) {
            return "redirect:/cart";
        }
        CartDTO cart = cartService.getCart(items);

        // Create the order using the service
        Order order = orderService.createOrderFromCart(cart, phoneNumber);

        // Clear the cart after order is placed
        session.removeAttribute(SESSION_CART_KEY);

        // Redirect to payment page with order id
        return "redirect:/payment/initiate?orderId=" + order.getId();
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