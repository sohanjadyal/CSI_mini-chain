package minichain;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BlockchainTest {

    @Test
    void startsWithGenesisBlock() {
        Blockchain chain = new Blockchain(2);
        assertEquals(1, chain.getChain().size());
        assertEquals("0", chain.getChain().get(0).getPreviousHash());
    }

    @Test
    void minerGetsReward() {
        Blockchain chain = new Blockchain(2);
        chain.minePending("asha");
        assertEquals(Blockchain.MINING_REWARD, chain.balanceOf("asha"));
    }

    @Test
    void paymentsMoveCoins() {
        Blockchain chain = new Blockchain(2);
        chain.minePending("asha");
        chain.addTransaction(new Transaction("asha", "ravi", 20));
        chain.minePending("meera");
        assertEquals(30, chain.balanceOf("asha"));
        assertEquals(20, chain.balanceOf("ravi"));
        assertEquals(50, chain.balanceOf("meera"));
    }

    @Test
    void cannotSpendMoreThanYouHave() {
        Blockchain chain = new Blockchain(2);
        chain.minePending("asha");
        assertThrows(IllegalArgumentException.class, () -> chain.addTransaction(new Transaction("asha", "ravi", 60)));
    }

    @Test
    void cannotPayYourself() {
        Blockchain chain = new Blockchain(2);
        chain.minePending("asha");
        assertThrows(IllegalArgumentException.class, () -> chain.addTransaction(new Transaction("asha", "asha", 5)));
    }

    @Test
    void cannotSpendQueuedCoins() {
        Blockchain chain = new Blockchain(2);
        chain.minePending("asha");
        chain.addTransaction(new Transaction("asha", "ravi", 40));
        assertThrows(IllegalArgumentException.class, () -> chain.addTransaction(new Transaction("asha", "meera", 40)));
        assertEquals(1, chain.getPending().size());
    }

    @Test
    void blocksAreMinedAndLinked() {
        Blockchain chain = new Blockchain(3);
        chain.minePending("asha");
        chain.minePending("ravi");
        for (int i = 1; i < chain.getChain().size(); i++) {
            Block block = chain.getChain().get(i);
            assertTrue(block.getHash().startsWith("000"));
            assertEquals(chain.getChain().get(i - 1).getHash(), block.getPreviousHash());
        }
        assertTrue(chain.isValid());
    }

    @Test
    void changingAPaymentIsDetected() {
        Blockchain chain = new Blockchain(2);
        chain.minePending("asha");
        Block block = chain.getChain().get(1);
        Transaction original = block.getTransactions().get(0);
        block.getTransactions().set(0, new Transaction(original.id(), original.from(), original.to(), 1_000_000));
        assertFalse(chain.isValid());
    }

    @Test
    void tamperingBlockOneIsDetected() {
        Blockchain chain = new Blockchain(2);
        chain.minePending("asha");
        chain.getChain().get(1).tamperPreviousHash("f".repeat(64));
        assertFalse(chain.isValid());
    }

    @Test
    void brokenLinkIsDetected() {
        Blockchain chain = new Blockchain(2);
        chain.minePending("asha");
        chain.minePending("ravi");
        chain.getChain().get(2).tamperPreviousHash("f".repeat(64));
        assertFalse(chain.isValid());
    }

    @Test
    void savedChainReloads(@TempDir Path dir) {
        Path file = dir.resolve("chain.dat");
        Blockchain chain = new Blockchain(2);
        chain.minePending("asha");
        Storage.save(chain, file);

        Blockchain loaded = Storage.load(file, 2);
        assertTrue(loaded.isValid());
        assertEquals(2, loaded.getDifficulty());
        assertEquals(chain.getChain().size(), loaded.getChain().size());
        assertEquals(chain.latestBlock().getHash(), loaded.latestBlock().getHash());
        assertEquals(50, loaded.balanceOf("asha"));
    }

    @Test
    void queuedPaymentsStayQueuedAfterReload(@TempDir Path dir) {
        Path file = dir.resolve("chain.dat");
        Blockchain chain = new Blockchain(2);
        chain.minePending("asha");
        chain.addTransaction(new Transaction("asha", "ravi", 20));
        Storage.save(chain, file);

        Blockchain loaded = Storage.load(file, 2);
        assertEquals(1, loaded.getPending().size());
        assertEquals(2, loaded.getChain().size());
        assertEquals(30, loaded.balanceOf("asha"));
    }

    @Test
    void tamperedChainIsNotSaved(@TempDir Path dir) {
        Path file = dir.resolve("chain.dat");
        Blockchain good = new Blockchain(2);
        good.minePending("asha");
        Storage.save(good, file);

        Blockchain tampered = new Blockchain(2);
        tampered.minePending("ravi");
        tampered.getChain().get(1).tamperPreviousHash("f".repeat(64));
        Storage.save(tampered, file);

        Blockchain loaded = Storage.load(file, 2);
        assertEquals(2, loaded.getChain().size());
        assertEquals(50, loaded.balanceOf("asha"));
    }

    @Test
    void missingOrUnreadableFileStartsANewChain(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("chain.dat");
        Blockchain fresh = Storage.load(file, 2);
        assertEquals(2, fresh.getDifficulty());
        assertEquals(1, fresh.getChain().size());

        Files.writeString(file, "not a chain");
        Blockchain afterGarbage = Storage.load(file, 2);
        assertEquals(2, afterGarbage.getDifficulty());
        assertEquals(1, afterGarbage.getChain().size());
    }
}
