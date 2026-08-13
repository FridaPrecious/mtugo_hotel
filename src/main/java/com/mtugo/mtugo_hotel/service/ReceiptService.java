package com.mtugo.mtugo_hotel.service;

import com.mtugo.mtugo_hotel.entity.Order;
import com.mtugo.mtugo_hotel.entity.Transaction;
import com.mtugo.mtugo_hotel.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

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
     * download (Content-Disposition: attachment). Covers every item in the
     * same cart checkout, not just the one order that anchored the payment.
     */
    public String generateReceiptText(Long orderId) {
        Order order = orderService.findOrderById(orderId);
        List<Order> group = orderService.findGroupIncludingSelf(order);

        Transaction transaction = transactionRepository.findTopByOrder_IdOrderByIdDesc(orderId)
                .orElse(null);

        double total = group.stream().mapToDouble(Order::getTotalAmount).sum();

        StringBuilder sb = new StringBuilder();
        sb.append("================================\n");
        sb.append("        MTUGO HOTEL\n");
        sb.append("     Payment Receipt\n");
        sb.append("================================\n\n");
        sb.append("Order #        : ").append(order.getId()).append("\n\n");

        for (Order item : group) {
            sb.append(String.format("%-20s x%-3d KSH %.2f%n",
                    item.getMeal().getName(), item.getQuantity(), item.getTotalAmount()));
        }

        sb.append("\n--------------------------------\n");
        sb.append("Total Paid     : KSH ").append(String.format("%.2f", total)).append("\n");
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
