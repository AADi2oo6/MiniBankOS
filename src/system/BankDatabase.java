package system;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import kernel.ReadWriteManager;

public class BankDatabase{

    private HashMap<String, Account> accounts=new HashMap<>();
    private ReadWriteManager lockManager=new ReadWriteManager();
    private PageTable pageTable=new PageTable();
    private static final String USER_HEADER="name,balance,state";
    private static final String TRANSACTION_HEADER="id,type,from,to,amount,state";

    public BankDatabase(){
        bootstrapStorage();
        loadAccounts();
    }

    public boolean hasCommittedAccounts(){
        return !accounts.isEmpty();
    }

    public boolean hasAccount(String name){
        return accounts.containsKey(name);
    }

    public boolean createAccount(String name, double balance){
        return createAccount(name, balance, true);
    }

    public boolean recoverCreateAccount(String name, double balance){
        return createAccount(name, balance, false);
    }

    private boolean createAccount(String name, double balance, boolean showMessages){

        if(accounts.containsKey(name)){
            if(showMessages){
                System.out.println("This account name already exists.");
            }
            return false;
        }

        if(balance<0){
            if(showMessages){
                System.out.println("Balance cannot be negative.");
            }
            return false;
        }

        Account account=new Account(name,balance);
        accounts.put(name,account);

        if(!persistAccounts()){
            accounts.remove(name);
            if(showMessages){
                System.out.println("Account storage update failed.");
            }
            return false;
        }

        recordTransaction("CREATE", "SYSTEM", name, balance);

        if(showMessages){
            System.out.println("Account created for "+name);
        }
        return true;

    }

    public void checkBalance(String name){

        Account acc=accounts.get(name);

        if(acc==null){
            System.out.println("Account not found.");
            return;
        }
        ReentrantReadWriteLock lock= lockManager.getLock(name);
        lock.readLock().lock();

        try{
            System.out.println("Name :"+ name +"\n Balance = "+acc.getBalance());
        }
        finally{
            lock.readLock().unlock();
        }
    }

    public boolean transfer(String from, String to, double amount){
        return transfer(from, to, amount, true);
    }

    public boolean recoverTransfer(String from, String to, double amount){
        return transfer(from, to, amount, false);
    }

    private boolean transfer(String from, String to, double amount, boolean showMessages){
        if(from.equals(to)){
            if(showMessages){
                System.out.println("Cannot transfer to the same account.");
            }
            return false;
        }
        if(amount<=0){
            if(showMessages){
                System.out.println("Invalid amount.");
            }
            return false;
        }

        Account sender=accounts.get(from);
        Account receiver=accounts.get(to);

        if(sender==null||receiver==null){
            if(showMessages){
                System.out.println("Account not found.");
            }
            return false;
        }
        ReentrantReadWriteLock lock1=lockManager.getLock(from);
        ReentrantReadWriteLock lock2=lockManager.getLock(to);
        ReentrantReadWriteLock firstLock=from.compareTo(to)<=0 ? lock1 : lock2;
        ReentrantReadWriteLock secondLock=from.compareTo(to)<=0 ? lock2 : lock1;

        firstLock.writeLock().lock();
        secondLock.writeLock().lock();

        try{
            
            if(sender.getBalance() < amount){
                if(showMessages){
                    System.out.println("Insufficient funds.");
                }
                return false;
            }
            sender.withdraw(amount);
            receiver.deposit(amount);

            if(!persistAccounts()){
                sender.deposit(amount);
                receiver.withdraw(amount);
                if(showMessages){
                    System.out.println("Account storage update failed.");
                }
                return false;
            }

            recordTransaction("TRANSFER", from, to, amount);

            if(showMessages){
                System.out.println("\n"+ amount +" transferred from "+ from +" to "+ to);
            }
            return true;
        }
        finally{
            secondLock.writeLock().unlock();
            firstLock.writeLock().unlock();
        }
    }

    public boolean deposit(String name, double amount){
        return deposit(name, amount, true);
    }

    public boolean recoverDeposit(String name, double amount){
        return deposit(name, amount, false);
    }

    private boolean deposit(String name, double amount, boolean showMessages){
        if(amount<=0){
            if(showMessages){
                System.out.println("Invalid amount.");
            }
            return false;
        }

        Account account=accounts.get(name);
        if(account==null){
            if(showMessages){
                System.out.println("Account not found.");
            }
            return false;
        }

        ReentrantReadWriteLock lock=lockManager.getLock(name);
        lock.writeLock().lock();

        try{
            account.deposit(amount);
            if(!persistAccounts()){
                account.withdraw(amount);
                if(showMessages){
                    System.out.println("Account storage update failed.");
                }
                return false;
            }

            recordTransaction("DEPOSIT", "CASH", name, amount);

            if(showMessages){
                System.out.println(amount+" deposited to "+name);
            }
            return true;
        }
        finally{
            lock.writeLock().unlock();
        }
    }

    public boolean withdraw(String name, double amount){
        return withdraw(name, amount, true);
    }

    public boolean recoverWithdraw(String name, double amount){
        return withdraw(name, amount, false);
    }

    private boolean withdraw(String name, double amount, boolean showMessages){
        if(amount<=0){
            if(showMessages){
                System.out.println("Invalid amount.");
            }
            return false;
        }

        Account account=accounts.get(name);
        if(account==null){
            if(showMessages){
                System.out.println("Account not found.");
            }
            return false;
        }

        ReentrantReadWriteLock lock=lockManager.getLock(name);
        lock.writeLock().lock();

        try{
            if(account.getBalance()<amount){
                if(showMessages){
                    System.out.println("Insufficient funds.");
                }
                return false;
            }
            account.withdraw(amount);
            if(!persistAccounts()){
                account.deposit(amount);
                if(showMessages){
                    System.out.println("Account storage update failed.");
                }
                return false;
            }

            recordTransaction("WITHDRAW", name, "CASH", amount);

            if(showMessages){
                System.out.println(amount+" withdrawn from "+name);
            }
            return true;
        }
        finally{
            lock.writeLock().unlock();
        }
    }

    public void printTransactionsFor(String name){
        boolean found=false;
        try{
            pageTable.ensureFile(PageTable.TRANSACTIONS_FILE, TRANSACTION_HEADER);
            List<String[]> rows=CsvStorage.readRows(pageTable.getFile("transactions"));
            for(String[] row:rows){
                if(row.length<6 || !row[5].equalsIgnoreCase("COMMIT")){
                    continue;
                }
                if(row[2].equals(name) || row[3].equals(name)){
                    if(!found){
                        System.out.println("Committed transactions for "+name+":");
                    }
                    found=true;
                    System.out.println(row[1]+" | from="+row[2]+" | to="+row[3]+" | amount="+row[4]);
                }
            }
        }
        catch(IOException e){
            System.out.println("Transaction lookup failed.");
            return;
        }

        if(!found){
            System.out.println("No committed transactions found for "+name+".");
        }
    }

    private void bootstrapStorage(){
        try{
            pageTable.ensureFile(PageTable.USER_FILE, USER_HEADER);
            pageTable.ensureFile(PageTable.TRANSACTIONS_FILE, TRANSACTION_HEADER);
        }
        catch(IOException e){
            System.out.println("Storage bootstrap failed.");
        }
    }

    private void loadAccounts(){
        try{
            List<String[]> rows=CsvStorage.readRows(pageTable.getFile("balance"));
            for(String[] row:rows){
                if(row.length<3 || !row[2].equalsIgnoreCase("COMMIT")){
                    continue;
                }
                double balance=Double.parseDouble(row[1]);
                accounts.put(row[0], new Account(row[0], balance));
            }
        }
        catch(IOException | NumberFormatException e){
            System.out.println("Account storage load failed.");
        }
    }

    private boolean persistAccounts(){
        List<String[]> rows=new ArrayList<>();
        for(Account account:accounts.values()){
            rows.add(new String[]{
                account.getName(),
                Double.toString(account.getBalance()),
                "COMMIT"
            });
        }

        try{
            CsvStorage.writeRows(pageTable.getFile("balance"), USER_HEADER, rows);
            return true;
        }
        catch(IOException e){
            return false;
        }
    }

    private boolean recordTransaction(String type, String from, String to, double amount){
        try{
            CsvStorage.appendRow(
                pageTable.getFile("transactions"),
                TRANSACTION_HEADER,
                new String[]{
                    Long.toString(System.currentTimeMillis()),
                    type,
                    from,
                    to,
                    Double.toString(amount),
                    "COMMIT"
                }
            );
            return true;
        }
        catch(IOException e){
            System.out.println("Transaction storage update failed.");
            return false;
        }
    }
}
