package minichain;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.HexFormat;
import java.util.UUID;

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
            Signature sig = Signature.getInstance("Ed25519");
            sig.initSign(priv);
            sig.update((id + from + to + amount).getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = sig.sign();
            return new Transaction(id, from, to, amount, pub, signatureBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign transaction", e);
        }
    }

    public boolean hasValidSignature() {
        if (NETWORK.equals(from)) return true;
        if (senderKey == null || signature == null) return false;
        try {
            Signature sig = Signature.getInstance("Ed25519");
            sig.initVerify(senderKey);
            sig.update((id + from + to + amount).getBytes(StandardCharsets.UTF_8));
            return sig.verify(signature);
        } catch (Exception e) {
            return false;
        }
    }

    public String hash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            if (id != null) {
                digest.update(id.getBytes(StandardCharsets.UTF_8));
            }
            if (from != null) {
                digest.update(from.getBytes(StandardCharsets.UTF_8));
            }
            if (to != null) {
                digest.update(to.getBytes(StandardCharsets.UTF_8));
            }
            digest.update(ByteBuffer.allocate(4).putInt(amount).array());
            if (senderKey != null) {
                digest.update(senderKey.getEncoded());
            }
            if (signature != null) {
                digest.update(signature);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String deriveAddress(PublicKey key) {
        if (key == null) {
            throw new IllegalArgumentException("Public key cannot be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getEncoded());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String toString() {
        return from + " -> " + to + ": " + amount;
    }
}
