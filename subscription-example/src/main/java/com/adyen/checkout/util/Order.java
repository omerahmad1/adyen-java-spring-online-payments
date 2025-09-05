package com.adyen.checkout.util;

public class Order {

    // Your unique reference for the order (e.g., "ORDER-12345")
    private String reference;

    // The current status of the order (e.g., "PENDING", "PAID", "FAILED")
    private String status;

    // The Adyen pspReference for the transaction, stored after authorization
    private String pspReference;

    // Constructor to create a new order
    public Order(String reference) {
        this.reference = reference;
        this.status = "PENDING"; // All new orders start as PENDING
    }

    // --- Getters and Setters ---

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPspReference() {
        return pspReference;
    }

    public void setPspReference(String pspReference) {
        this.pspReference = pspReference;
    }
}
