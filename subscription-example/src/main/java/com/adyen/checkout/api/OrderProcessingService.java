package com.adyen.checkout.api;

import com.adyen.checkout.util.Order;
import com.adyen.model.notification.NotificationRequestItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrderProcessingService {

    private final Logger log = LoggerFactory.getLogger(OrderProcessingService.class);

    // Use a thread-safe map to store orders in memory.
    // The key is the order reference (your unique order ID).
    private final Map<String, Order> orderStore = new ConcurrentHashMap<>();

    // A method to create and store an order when your checkout begins.
    // You would call this from your checkout controller.
    public void createOrder(Order order) {
        orderStore.put(order.getReference(), order);
        log.info("Created and stored in-memory order: {}", order.getReference());
    }

    /**
     * Processes a single notification item from the webhook.
     * This method is idempotent for an in-memory store.
     */
    public void processNotification(NotificationRequestItem item) {
        String orderReference = item.getMerchantReference();
        String eventCode = item.getEventCode();

        // Find the order in our in-memory map.
        Order order = orderStore.get(orderReference);

        if (order == null) {
            log.warn("Received notification for an order not found in memory. Merchant Reference: {}", orderReference);
            return;
        }

        // --- IDEMPOTENCY CHECK ---
        // Before processing, check the in-memory order's status.
        if ("AUTHORISATION".equals(eventCode) && "PAID".equals(order.getStatus())) {
            log.info("In-memory order {} is already marked as PAID. Ignoring duplicate AUTHORISATION webhook.", order.getReference());
            return; // Stop processing
        }

        // --- BUSINESS LOGIC ---
        if ("AUTHORISATION".equals(eventCode) && item.isSuccess()) {
            log.info("Payment authorized for in-memory order: {}. Updating status to PAID.", order.getReference());

            // Update the order object in the map.
            order.setStatus("PAID");
            order.setPspReference(item.getPspReference());

            // No 'save' needed, as we're modifying the object in the map directly.
            // You can trigger other services from here.
            // emailService.sendOrderConfirmation(order);
        } else {
            log.warn("Unhandled or failed event '{}' for order {}", eventCode, order.getReference());
        }
    }
}
