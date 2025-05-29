package org.ivc.dbms.Main;

import java.security.MessageDigest;

public class HashUtil {

    
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(input.getBytes("UTF-8"));

            StringBuilder hex = new StringBuilder();
            for (byte b : encoded) {
                hex.append(String.format("%02x", b)); // lowercase hex
            }

            return hex.toString(); // already lowercase
        } catch (Exception e) {
            throw new RuntimeException("Error hashing PIN", e);
        }
    }
}
