package minichain;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.UUID;
import java.util.Base64;

// bump version only for incompatible changes, adding a field is safe
public record Transaction(String id, String from, String to, int amount, PublicKey senderKey, byte[] signature) implements Serializable {
    private static final long serialVersionUID = 2L;

    // Sender used for mining rewards.
    public static final String NETWORK = "network";

    // For mining rewards
    public Transaction(String from, String to, int amount) {
        this(UUID.randomUUID().toString(), from, to, amount, null, null);
    }

    public static Transaction create(String from, String to, int amount, PrivateKey priv, PublicKey pub) {
        try {
            String id = UUID.randomUUID().toString();
            Signature rsa = Signature.getInstance("SHA256withRSA");
            rsa.initSign(priv);
            rsa.update((id + from + to + amount).getBytes(StandardCharsets.UTF_8));
            byte[] sig = rsa.sign();
            return new Transaction(id, from, to, amount, pub, sig);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign transaction", e);
        }
    }

    public boolean hasValidSignature() {
        if (NETWORK.equals(from)) return true;
        if (senderKey == null || signature == null) return false;
        try {
            Signature rsa = Signature.getInstance("SHA256withRSA");
            rsa.initVerify(senderKey);
            rsa.update((id + from + to + amount).getBytes(StandardCharsets.UTF_8));
            return rsa.verify(signature);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return from + " -> " + to + ": " + amount;
    }
}
