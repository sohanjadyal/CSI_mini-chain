package minichain;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;


// bump version only for incompatible changes, adding a field is safe
public class Block implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int index;
    private final long timestamp;
    private final List<Transaction> transactions;
    private String previousHash;
    private int nonce = 0;
    private String hash;

    public Block(int index, long timestamp, List<Transaction> transactions, String previousHash) {
        this.index = index;
        this.timestamp = timestamp;
        this.transactions = transactions;
        this.previousHash = previousHash;
        this.hash = computeHash();
    }

    public String computeHash() {
        String content = index + "|" + timestamp + "|" + previousHash + "|" + nonce + "|" + transactions;
        return sha256(content);
    }

    // Proof of work: keep bumping the nonce until the hash starts with `difficulty` zeros.
    public void mine(int difficulty) {
        String target = "0".repeat(difficulty);
        while (!hash.startsWith(target)) {
            nonce++;
            hash = computeHash();
        }
    }

    static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public int getIndex() { return index; }
    public long getTimestamp() { return timestamp; }
    public List<Transaction> getTransactions() { return transactions; }
    public String getPreviousHash() { return previousHash; }
    public int getNonce() { return nonce; }
    public String getHash() { return hash; }

    // Used by tests to simulate someone editing the chain.
    void tamperPreviousHash(String previousHash) {
        this.previousHash = previousHash;
        this.hash = computeHash();
    }
}
