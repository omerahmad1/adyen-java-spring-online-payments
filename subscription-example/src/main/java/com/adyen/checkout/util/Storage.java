package com.adyen.checkout.util;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/*
 Temp storage to keep in memory the generated tokens
 */
public class Storage {

    // shopper reference: constant value for demo purpose supporting one shopper
    public static final String SHOPPER_REFERENCE = "SHOPPER_ID_";

   private record Token(String recurringReference, String paymentMethod, String shopperReference) {
   }

   private static Set<Token> tokens = new HashSet<>();

   public static Set<Token> getAllTokens() {
       return tokens;
   }

    // Add a Set to store processed PSP references
    private static final Set<String> processedPspReferences = new HashSet<>();

    // New method for the idempotency check
    public static synchronized boolean isAlreadyProcessed(String pspReference) {
        if (pspReference == null || pspReference.isEmpty()) {
            return false;
        }
        return processedPspReferences.contains(pspReference);
    }

    // Update your 'add' method to record the PSP reference
    public static synchronized void add(String pspReference, String paymentMethod, String merchantReference) {
        if (pspReference != null && !pspReference.isEmpty()) {
            processedPspReferences.add(pspReference);
        }
    }

   public static void remove(String token, String shopperReference) {
        tokens.removeIf(x -> x.recurringReference.equals(token) && x.shopperReference.equals(shopperReference));
   }
}
