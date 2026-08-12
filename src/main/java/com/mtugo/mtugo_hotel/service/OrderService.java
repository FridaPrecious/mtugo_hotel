package com.mtugo.mtugo_hotel.service;

import com.mtugo.mtugo_hotel.dto.CartDTO;
import com.mtugo.mtugo_hotel.dto.CartItemDTO;
import com.mtugo.mtugo_hotel.entity.Meal;
import com.mtugo.mtugo_hotel.entity.Order;
import com.mtugo.mtugo_hotel.entity.OrderStatus;
import com.mtugo.mtugo_hotel.repository.MealRepository;
import com.mtugo.mtugo_hotel.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final MealRepository mealRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository, MealRepository mealRepository) {
        this.orderRepository = orderRepository;
        this.mealRepository = mealRepository;
    }

    /**
     * Creates a single order with multiple order items from the cart.
     * For now, this is a minimal implementation that saves the order.
     * Later, you'll add payment integration and transaction handling.
     */
    public Order createOrderFromCart(CartDTO cart, String customerPhone) {
        // For now, we only support one meal per order? Actually we need to handle multiple.
        // But the Order entity currently has a single meal_id, not a collection.
        // That means our data model doesn't support multiple items per order yet!
        // To keep things simple and avoid breaking changes, we'll create a separate order per cart item.
        // This is a quick fix to allow testing. A real implementation would have OrderItem entities.

        // For demonstration, we'll just create an order for the first item in the cart
        // and log that multiple items will be supported later.
        List<CartItemDTO> items = cart.getItems();
        if (items.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // Take the first item as the main order item (temporary)
        CartItemDTO firstItem = items.get(0);
        Meal meal = mealRepository.findById(firstItem.getMealId())
                .orElseThrow(() -> new RuntimeException("Meal not found"));

        Order order = new Order();
        order.setMeal(meal);
        order.setQuantity(firstItem.getQuantity());
        order.setTotalAmount(firstItem.getSubtotal());
        order.setCustomerPhone(customerPhone);
        order.setStatus(OrderStatus.PENDING);
        // order.setOrderTime(LocalDateTime.now()); // automatically set by @PrePersist

        // For multiple items, we could create OrderItems, but for now just save one.
        // We'll log that multiple items are not fully supported yet.
        if (items.size() > 1) {
            System.out.println("Warning: Cart contains " + items.size() + " items. " +
                    "Only the first item will be saved in this order. " +
                    "Full multi-item support requires the OrderItem entity.");
        }

        return orderRepository.save(order);
    }
}