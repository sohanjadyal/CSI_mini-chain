package minichain;

import java.nio.file.Path;
import java.util.Scanner;

/** Menu for trying out the chain from the terminal. */
public class Main {
    private static final Path SAVE_FILE = Path.of("chain.dat");

    public static void main(String[] args) {
        Blockchain chain = Storage.load(SAVE_FILE, 4);
        Scanner in = new Scanner(System.in);
        System.out.println("mini-chain. Mine a block first to get some coins.");

        while (true) {
            System.out.println("""

                    1. Send coins
                    2. Mine a block
                    3. Show the chain
                    4. Show balances
                    5. Check a balance
                    6. Is the chain valid?
                    7. Tamper with a block
                    8. Quit""");
            System.out.print("> ");
            if (!in.hasNextLine()) {
                Storage.save(chain, SAVE_FILE);
                return;
            }
            String choice = in.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> {
                        String from = ask(in, "From: ");
                        String to = ask(in, "To: ");
                        int amount = Integer.parseInt(ask(in, "Amount: "));
                        chain.addTransaction(new Transaction(from, to, amount));
                        System.out.println("Queued. It's confirmed when the next block is mined.");
                    }
                    case "2" -> {
                        String miner = ask(in, "Your name (you get the " + Blockchain.MINING_REWARD + " coin reward): ");
                        System.out.println("Mining...");
                        Block block = chain.minePending(miner);
                        System.out.println("Mined block " + block.getIndex() + " after " + block.getNonce() + " tries: " + block.getHash());
                        Storage.save(chain, SAVE_FILE);
                    }
                    case "3" -> {
                        for (Block block : chain.getChain()) {
                            System.out.println("\nBlock " + block.getIndex());
                            System.out.println("  hash:     " + block.getHash());
                            System.out.println("  previous: " + block.getPreviousHash());
                            block.getTransactions().forEach(tx -> System.out.println("  " + tx));
                        }
                    }
                    case "4" -> {
                        System.out.println("Balances (queued payments included):");
                        chain.balances().forEach((name, coins) -> System.out.println(name + ": " + coins));
                    }
                    case "5" -> {
                        String name = ask(in, "Name: ");
                        System.out.println(name + " has " + chain.balanceOf(name) + " coins");
                    }
                    case "6" -> System.out.println(chain.isValid() ? "The chain is valid." : "The chain has been tampered with.");
                    case "7" -> {
                        int index = Integer.parseInt(ask(in, "Which block? "));
                        Block block = chain.getChain().get(index);
                        if (block.getTransactions().isEmpty()) {
                            System.out.println("That block has no payments to change. Mine a block first.");
                            continue;
                        }
                        Transaction original = block.getTransactions().get(0);
                        block.getTransactions().set(0, new Transaction(original.id(), original.from(), original.to(), 1_000_000));
                        System.out.println("Changed \"" + original + "\" to \"" + block.getTransactions().get(0) + "\". Now check if the chain is valid! Saving is now off for this run.");
                    }
                    case "8" -> { Storage.save(chain, SAVE_FILE); return; }
                    default -> System.out.println("Please pick 1-8.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please type a whole number.");
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            } catch (IndexOutOfBoundsException e) {
                System.out.println("There's no block with that number.");
            }
        }
    }

    private static String ask(Scanner in, String prompt) {
        System.out.print(prompt);
        return in.nextLine().trim();
    }
}
