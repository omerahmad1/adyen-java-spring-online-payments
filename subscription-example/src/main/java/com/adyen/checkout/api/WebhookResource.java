// File: WebhookResource.java

package com.adyen.checkout.api;

import com.adyen.checkout.ApplicationProperty;
import com.adyen.checkout.util.Storage;
import com.adyen.model.notification.NotificationRequest;
import com.adyen.model.notification.NotificationRequestItem;
import com.adyen.util.HMACValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.SignatureException;

@RestController
@RequestMapping("/api")
public class WebhookResource {
    private final Logger log = LoggerFactory.getLogger(WebhookResource.class);
    private final ApplicationProperty applicationProperty;
    private final HMACValidator hmacValidator;

    @Autowired
    public WebhookResource(ApplicationProperty applicationProperty, HMACValidator hmacValidator) {
        this.applicationProperty = applicationProperty;
        this.hmacValidator = hmacValidator;
    }

    @PostMapping("/webhooks/notifications")
    public ResponseEntity<String> webhooks(@RequestBody String json) {
        log.info("Received Adyen webhook notification.");

        // Asynchronously process the notification to send a quick response
        new Thread(() -> {
            try {
                var notificationRequest = NotificationRequest.fromJson(json);
                notificationRequest.getNotificationItems().forEach(this::processNotification);
            } catch (IOException e) {
                log.error("Error parsing webhook JSON", e);
            }
        }).start();

        // --- PROBLEM 1 FIX: Acknowledge immediately with the correct response ---
        return ResponseEntity.ok("[accepted]"); // <-- Correct acknowledgement
    }

    private void processNotification(NotificationRequestItem item) {
        try {
            if (!hmacValidator.validateHMAC(item, this.applicationProperty.getHmacKey())) {
                log.warn("HMAC validation failed for incoming webhook.");
                return; // Stop processing
            }

            log.info("Processing eventCode: {} for pspReference: {}", item.getEventCode(), item.getPspReference());

            // --- PROBLEM 2 FIX: Idempotency Check ---
            // Before processing, check if this PSP reference has been seen before.
            if (Storage.isAlreadyProcessed(item.getPspReference())) { // <-- You must implement this check
                log.info("PSP reference {} has already been processed. Ignoring duplicate.", item.getPspReference());
                return; // Stop, do not process again
            }

            // Consume payload if successful
            if (item.isSuccess() && "AUTHORISATION".equals(item.getEventCode())) {
                log.info("Payment authorized for PspReference {}. Storing details.", item.getPspReference());

                // Since it's a new, valid notification, process it.
                // This call should now mark the PSP reference as "processed" internally.
                Storage.add(item.getPspReference(), item.getPaymentMethod(), item.getMerchantReference());
                // --- ADD THIS NEW BLOCK ---
            } else if (item.isSuccess() && "CAPTURE".equals(item.getEventCode())) {
                    log.info("Payment CAPTURED for PspReference {}. Finalizing order.", item.getPspReference());
                    // This is where you would update your order status from "Authorized" to "Paid/Completed".
                    // You can also trigger shipping or service activation from here.
                    Storage.add(item.getPspReference(), item.getPaymentMethod(), item.getMerchantReference()); // Mark this event as processed too
            } else {
                log.warn("Webhook was not a successful AUTHORISATION event. Event code: {}", item.getEventCode());
            }

        } catch (SignatureException e) {
            log.error("Error while validating HMAC Key", e);
        }
    }
}
