package minichain;

import java.io.Serializable;
import java.util.UUID;

// bump version only for incompatible changes, adding a field is safe
public record Transaction(String id, String from, String to, int amount) implements Serializable {
    private static final long serialVersionUID = 1L;

    // Sender used for mining rewards.
    public static final String NETWORK = "network";

    public Transaction(String from, String to, int amount) {
        this(UUID.randomUUID().toString(), from, to, amount);
    }

    @Override
    public String toString() {
        return from + " -> " + to + ": " + amount;
    }
}
