package com.mtugo.mtugo_hotel.service;

import com.mtugo.mtugo_hotel.entity.Order;
import com.mtugo.mtugo_hotel.entity.Transaction;
import com.mtugo.mtugo_hotel.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class ReceiptService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss");

    private final OrderService orderService;
    private final TransactionRepository transactionRepository;

    @Autowired
    public ReceiptService(OrderService orderService, TransactionRepository transactionRepository) {
        this.orderService = orderService;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Builds a plain-text receipt for a paid order, suitable for direct
     * download (Content-Disposition: attachment).
     */
    public String generateReceiptText(Long orderId) {
        Order order = orderService.findOrderById(orderId);

        Transaction transaction = transactionRepository.findTopByOrder_IdOrderByIdDesc(orderId)
                .orElse(null);

        StringBuilder sb = new StringBuilder();
        sb.append("================================\n");
        sb.append("        MTUGO HOTEL\n");
        sb.append("     Payment Receipt\n");
        sb.append("================================\n\n");
        sb.append("Order #        : ").append(order.getId()).append("\n");
        sb.append("Meal           : ").append(order.getMeal().getName()).append("\n");
        sb.append("Quantity       : ").append(order.getQuantity()).append("\n");
        sb.append("Total Paid     : KSH ").append(String.format("%.2f", order.getTotalAmount())).append("\n");
        sb.append("Phone Number   : ").append(order.getCustomerPhone()).append("\n");
        sb.append("Status         : ").append(order.getStatus()).append("\n");

        if (order.getPaidAt() != null) {
            sb.append("Paid At        : ").append(order.getPaidAt().format(FORMATTER)).append("\n");
        }
        if (order.getExpectedReadyAt() != null) {
            sb.append("Ready By       : ").append(order.getExpectedReadyAt().format(FORMATTER)).append("\n");
        }
        if (transaction != null && transaction.getMpesaReceiptNumber() != null
                && !transaction.getMpesaReceiptNumber().isBlank()) {
            sb.append("M-Pesa Receipt : ").append(transaction.getMpesaReceiptNumber()).append("\n");
        }

        sb.append("\n--------------------------------\n");
        sb.append("   Thank you for ordering with\n");
        sb.append("          Mtugo Hotel!\n");
        sb.append("================================\n");

        return sb.toString();
    }
}
