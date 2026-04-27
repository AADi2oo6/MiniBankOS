package system;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import kernel.ReadWriteManager;

public class BankDatabase {

    // ─── Physical Frame Limit (simulates fixed RAM size) ─────────────────────
    private static final int MAX_FRAMES = 5;

    private static final String USER_HEADER        = "name,balance,state";
    private static final String TRANSACTION_HEADER = "id,type,from,to,amount,state";

    // ─── LRU Cache = Physical Frames (TLB/RAM simulation) ────────────────────
    // LinkedHashMap with accessOrder=true → tracks LRU order automatically
    // removeEldestEntry() = page replacement policy trigger
    private final LinkedHashMap<String, Account> frames;

    private final ReadWriteManager lockManager = new ReadWriteManager();
    private final PageTable pageTable           = new PageTable();

    public BankDatabase() {
        frames = new LinkedHashMap<String, Account>(MAX_FRAMES, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Account> eldest) {
                if (size() > MAX_FRAMES) {
                    // ── PAGE-OUT: LRU account evicted from physical frames ──
                    // Data is already persisted on last write, safe to evict
                    System.out.println(
                        "[PAGE-OUT] Evicting '" + eldest.getKey() +
                        "' from frames (LRU). Frames full (" + MAX_FRAMES + "/" + MAX_FRAMES + ")."
                    );
                    return true;
                }
                return false;
            }
        };
        bootstrapStorage();
        // No loadAccounts() — lazy page-in on demand (virtual memory style)
    }

    // =========================================================================
    //  PAGE FAULT HANDLER — loads account from disk into frames
    // =========================================================================
    private Account pageIn(String name) {
        System.out.println("[PAGE FAULT] '" + name + "' not in frames → loading from disk...");
        try {
            List<String[]> rows = CsvStorage.readRows(pageTable.getFile("balance"));
            for (String[] row : rows) {
                if (row.length >= 3 && row[0].equals(name) && row[2].equalsIgnoreCase("COMMIT")) {
                    double balance = Double.parseDouble(row[1]);
                    Account account = new Account(name, balance);
                    synchronized (frames) {
                        frames.put(name, account); // may trigger LRU eviction via removeEldestEntry
                    }
                    System.out.println("[PAGE-IN]  '" + name + "' loaded into frames.");
                    printFrameStatus();
                    return account;
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("[PAGE FAULT] Page-in failed for account: " + name);
        }
        return null; // account does not exist on disk either
    }

    // =========================================================================
    //  TLB LOOKUP — cache hit → return immediately, cache miss → page fault
    // =========================================================================
    private Account getAccount(String name) {
        synchronized (frames) {
            Account cached = frames.get(name); // get() updates LRU access order
            if (cached != null) {
                System.out.println("[TLB HIT]  '" + name + "' found in frames. (access order updated → '" + name + "' is now MRU)");
                printFrameStatus();
                return cached;
            }
        }
        // TLB miss → trigger page fault handler
        return pageIn(name);
    }

    // =========================================================================
    //  PERSIST — write one account row back to CSV (write-through policy)
    //  All accounts (cached + evicted) are preserved in CSV at all times
    // =========================================================================
    private boolean persistAccount(Account account) {
        try {
            List<String[]> rows = CsvStorage.readRows(pageTable.getFile("balance"));
            boolean found = false;
            for (String[] row : rows) {
                if (row.length >= 3 && row[0].equals(account.getName())) {
                    row[1] = Double.toString(account.getBalance());
                    row[2] = "COMMIT";
                    found  = true;
                    break;
                }
            }
            if (!found) {
                rows.add(new String[]{
                    account.getName(),
                    Double.toString(account.getBalance()),
                    "COMMIT"
                });
            }
            CsvStorage.writeRows(pageTable.getFile("balance"), USER_HEADER, rows);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // =========================================================================
    //  PUBLIC API
    // =========================================================================

    /**
     * Reads CSV directly — does NOT rely on cache.
     * Used by RecoveryManager to check if committed data exists on disk.
     */
    public boolean hasCommittedAccounts() {
        try {
            List<String[]> rows = CsvStorage.readRows(pageTable.getFile("balance"));
            for (String[] row : rows) {
                if (row.length >= 3 && row[2].equalsIgnoreCase("COMMIT")) {
                    return true;
                }
            }
        } catch (IOException e) {
            // ignore — treat as no committed accounts
        }
        return false;
    }

    /**
     * Check cache first (fast path), then CSV (slow path).
     * Does NOT trigger a page-in — just checks existence.
     */
    public boolean hasAccount(String name) {
        synchronized (frames) {
            if (frames.containsKey(name)) return true;
        }
        try {
            List<String[]> rows = CsvStorage.readRows(pageTable.getFile("balance"));
            for (String[] row : rows) {
                if (row.length >= 3 && row[0].equals(name) && row[2].equalsIgnoreCase("COMMIT")) {
                    return true;
                }
            }
        } catch (IOException e) {
            // ignore
        }
        return false;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    public boolean createAccount(String name, double balance) {
        return createAccount(name, balance, true);
    }

    public boolean recoverCreateAccount(String name, double balance) {
        return createAccount(name, balance, false);
    }

    private boolean createAccount(String name, double balance, boolean showMessages) {
        if (hasAccount(name)) {
            if (showMessages) System.out.println("This account name already exists.");
            return false;
        }
        if (balance < 0) {
            if (showMessages) System.out.println("Balance cannot be negative.");
            return false;
        }

        Account account = new Account(name, balance);
        synchronized (frames) {
            frames.put(name, account); // page-in new account (may evict LRU)
        }
        printFrameStatus();

        if (!persistAccount(account)) {
            synchronized (frames) {
                frames.remove(name);
            }
            if (showMessages) System.out.println("Account storage update failed.");
            return false;
        }

        recordTransaction("CREATE", "SYSTEM", name, balance);
        if (showMessages) System.out.println("Account created for " + name);
        return true;
    }

    // ── Balance ───────────────────────────────────────────────────────────────

    public Double getBalanceValue(String name) {
        Account acc = getAccount(name); // TLB lookup or page fault
        if (acc == null) {
            return null;
        }
        ReentrantReadWriteLock lock = lockManager.getLock(name);
        lock.readLock().lock();
        try {
            return acc.getBalance();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void checkBalance(String name) {
        Account acc = getAccount(name); // TLB lookup or page fault
        if (acc == null) {
            System.out.println("Account not found.");
            return;
        }
        ReentrantReadWriteLock lock = lockManager.getLock(name);
        lock.readLock().lock();
        try {
            System.out.println("Name: " + name + "\nBalance = " + acc.getBalance());
        } finally {
            lock.readLock().unlock();
        }
    }

    // ── Deposit ───────────────────────────────────────────────────────────────

    public boolean deposit(String name, double amount) {
        return deposit(name, amount, true);
    }

    public boolean recoverDeposit(String name, double amount) {
        return deposit(name, amount, false);
    }

    private boolean deposit(String name, double amount, boolean showMessages) {
        if (amount <= 0) {
            if (showMessages) System.out.println("Invalid amount.");
            return false;
        }
        Account account = getAccount(name);
        if (account == null) {
            if (showMessages) System.out.println("Account not found.");
            return false;
        }
        ReentrantReadWriteLock lock = lockManager.getLock(name);
        lock.writeLock().lock();
        try {
            account.deposit(amount);
            if (!persistAccount(account)) {
                account.withdraw(amount);
                if (showMessages) System.out.println("Account storage update failed.");
                return false;
            }
            recordTransaction("DEPOSIT", "CASH", name, amount);
            if (showMessages) System.out.println(amount + " deposited to " + name);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ── Withdraw ──────────────────────────────────────────────────────────────

    public boolean withdraw(String name, double amount) {
        return withdraw(name, amount, true);
    }

    public boolean recoverWithdraw(String name, double amount) {
        return withdraw(name, amount, false);
    }

    private boolean withdraw(String name, double amount, boolean showMessages) {
        if (amount <= 0) {
            if (showMessages) System.out.println("Invalid amount.");
            return false;
        }
        Account account = getAccount(name);
        if (account == null) {
            if (showMessages) System.out.println("Account not found.");
            return false;
        }
        ReentrantReadWriteLock lock = lockManager.getLock(name);
        lock.writeLock().lock();
        try {
            if (account.getBalance() < amount) {
                if (showMessages) System.out.println("Insufficient funds.");
                return false;
            }
            account.withdraw(amount);
            if (!persistAccount(account)) {
                account.deposit(amount);
                if (showMessages) System.out.println("Account storage update failed.");
                return false;
            }
            recordTransaction("WITHDRAW", name, "CASH", amount);
            if (showMessages) System.out.println(amount + " withdrawn from " + name);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ── Transfer ──────────────────────────────────────────────────────────────

    public boolean transfer(String from, String to, double amount) {
        return transfer(from, to, amount, true);
    }

    public boolean recoverTransfer(String from, String to, double amount) {
        return transfer(from, to, amount, false);
    }

    private boolean transfer(String from, String to, double amount, boolean showMessages) {
        if (from.equals(to)) {
            if (showMessages) System.out.println("Cannot transfer to the same account.");
            return false;
        }
        if (amount <= 0) {
            if (showMessages) System.out.println("Invalid amount.");
            return false;
        }

        // Load both accounts into frames before acquiring locks
        // MAX_FRAMES=5 ensures both fit simultaneously
        Account sender   = getAccount(from);
        Account receiver = getAccount(to);

        if (sender == null || receiver == null) {
            if (showMessages) System.out.println("Account not found.");
            return false;
        }

        // Lock in consistent order to prevent deadlock
        ReentrantReadWriteLock lock1      = lockManager.getLock(from);
        ReentrantReadWriteLock lock2      = lockManager.getLock(to);
        ReentrantReadWriteLock firstLock  = from.compareTo(to) <= 0 ? lock1 : lock2;
        ReentrantReadWriteLock secondLock = from.compareTo(to) <= 0 ? lock2 : lock1;

        firstLock.writeLock().lock();
        secondLock.writeLock().lock();
        try {
            if (sender.getBalance() < amount) {
                if (showMessages) System.out.println("Insufficient funds.");
                return false;
            }
            sender.withdraw(amount);
            receiver.deposit(amount);

            if (!persistAccount(sender) || !persistAccount(receiver)) {
                sender.deposit(amount);
                receiver.withdraw(amount);
                if (showMessages) System.out.println("Account storage update failed.");
                return false;
            }

            recordTransaction("TRANSFER", from, to, amount);
            if (showMessages)
                System.out.println("\n" + amount + " transferred from " + from + " to " + to);
            return true;
        } finally {
            secondLock.writeLock().unlock();
            firstLock.writeLock().unlock();
        }
    }

    // ── Transactions ──────────────────────────────────────────────────────────

    public List<Map<String, String>> getTransactionsList(String name) {
        List<Map<String, String>> result = new ArrayList<>();
        try {
            pageTable.ensureFile(PageTable.TRANSACTIONS_FILE, TRANSACTION_HEADER);
            List<String[]> rows = CsvStorage.readRows(pageTable.getFile("transactions"));
            for (String[] row : rows) {
                if (row.length < 6 || !row[5].equalsIgnoreCase("COMMIT")) continue;
                if (row[2].equals(name) || row[3].equals(name)) {
                    Map<String, String> txn = new LinkedHashMap<>();
                    txn.put("timestamp", row[0]);
                    txn.put("type", row[1]);
                    txn.put("from", row[2]);
                    txn.put("to", row[3]);
                    txn.put("amount", row[4]);
                    result.add(txn);
                }
            }
        } catch (IOException e) {
            return result;
        }
        return result;
    }

    public void printTransactionsFor(String name) {
        boolean found = false;
        try {
            pageTable.ensureFile(PageTable.TRANSACTIONS_FILE, TRANSACTION_HEADER);
            List<String[]> rows = CsvStorage.readRows(pageTable.getFile("transactions"));
            for (String[] row : rows) {
                if (row.length < 6 || !row[5].equalsIgnoreCase("COMMIT")) continue;
                if (row[2].equals(name) || row[3].equals(name)) {
                    if (!found) System.out.println("Committed transactions for " + name + ":");
                    found = true;
                    System.out.println(row[1] + " | from=" + row[2] + " | to=" + row[3] + " | amount=" + row[4]);
                }
            }
        } catch (IOException e) {
            System.out.println("Transaction lookup failed.");
            return;
        }
        if (!found) System.out.println("No committed transactions found for " + name + ".");
    }

    // =========================================================================
    //  INTERNAL HELPERS
    // =========================================================================

    private void bootstrapStorage() {
        try {
            pageTable.ensureFile(PageTable.USER_FILE,        USER_HEADER);
            pageTable.ensureFile(PageTable.TRANSACTIONS_FILE, TRANSACTION_HEADER);
        } catch (IOException e) {
            System.out.println("Storage bootstrap failed.");
        }
    }

    // =========================================================================
    //  FRAME STATE VISUALIZER — shows current LRU order of frames
    // =========================================================================
    private void printFrameStatus() {
        synchronized (frames) {
            System.out.println();
            System.out.println("  +------------------------------------------------------------+");
            System.out.printf ("  |  FRAME STATUS : %d/%d frames used                          |%n", frames.size(), MAX_FRAMES);
            System.out.println("  +------------------------------------------------------------+");
            System.out.print  ("  |  LRU -->  ");
            for (String key : frames.keySet()) {
                System.out.printf("[ %-8s ]", key);
            }
            for (int i = frames.size(); i < MAX_FRAMES; i++) {
                System.out.print("[  EMPTY   ]");
            }
            System.out.println("  --> MRU  |");
            System.out.println("  +------------------------------------------------------------+");
            System.out.println();
        }
    }

    private boolean recordTransaction(String type, String from, String to, double amount) {
        try {
            CsvStorage.appendRow(
                pageTable.getFile("transactions"),
                TRANSACTION_HEADER,
                new String[]{
                    Long.toString(System.currentTimeMillis()),
                    type, from, to,
                    Double.toString(amount),
                    "COMMIT"
                }
            );
            return true;
        } catch (IOException e) {
            System.out.println("Transaction storage update failed.");
            return false;
        }
    }
}
