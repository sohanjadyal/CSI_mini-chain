package minichain;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Saves the chain to a file and reads it back, so it survives a restart. */
final class Storage {
    private Storage() {}

    // A chain that doesn't validate is never written, so the last good one stays on disk.
    static void save(Blockchain chain, Path file) {
        if (!chain.isValid()) {
            System.out.println("The chain is invalid, so it was not saved.");
            return;
        }
        // Write to a temp file and move it into place, so a crash mid-write can't destroy the last good chain.
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(temp))) {
            out.writeObject(chain);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            System.out.println("Saved " + file + ".");
        } catch (IOException e) {
            System.out.println("Couldn't save the chain: " + e.getMessage());
        }
    }

    // A missing, unreadable or stale file just means we start a new chain.
    static Blockchain load(Path file, int difficulty) {
        if (Files.notExists(file)) {
            return new Blockchain(difficulty);
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            Blockchain chain = (Blockchain) in.readObject();
            if (!chain.isValid()) {
                System.out.println(file + " doesn't hold a valid chain, so a new one was started.");
                return new Blockchain(difficulty);
            }
            System.out.println("Loaded " + file + ".");
            return chain;
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            System.out.println("Couldn't read " + file + " (" + e.getClass().getSimpleName() + "), so a new chain was started.");
            return new Blockchain(difficulty);
        }
    }
}
