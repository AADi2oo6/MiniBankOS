package kernel;

import system.BankDatabase;
import system.LoanManager;
import logging.Logger;
import kernel.scheduler.*;

import Auth.AuthManager;
import Auth.Session;
import Auth.user;

public class TransactionManager{
    private BankDatabase bank;
    private Logger logger;
    private Scheduler scheduler;
    private AuthManager authManager;
    private ModeBit modeBit;
    private LoanManager loanManager;


    public TransactionManager(BankDatabase bank, Scheduler scheduler, AuthManager authManager, ModeBit modeBit){
        this.bank=bank;
        this.scheduler=scheduler;
        this.logger=new Logger();//new logger object for each thread
        this.authManager=authManager;
        this.modeBit=modeBit;
        this.loanManager=new LoanManager();
        this.loanManager.startRealtimeUpdates();
    }
    public void createAccount(String name, double balance){
        user current=Session.getCurrentUser();
        
        if(current==null || !current.isAdmin()){
            System.out.println("Access denied: Only admin can create accounts.");
            return;
        }
        System.out.println("Warning: account created without login credentials. Use: create <account_name> <password> <opening_balance>");
        submitCreateAccount(name, balance);
    }

    public void createAccount(String name, String password, double balance){
        user current=Session.getCurrentUser();
        
        if(current==null || !current.isAdmin()){
            System.out.println("Access denied: Only admin can create accounts.");
            return;
        }
        if(password==null || password.trim().isEmpty()){
            System.out.println("Password cannot be empty.");
            return;
        }

        boolean accountExists=bank.hasAccount(name);
        boolean loginExists=authManager.getuser(name)!=null;

        if(accountExists && loginExists){
            System.out.println("This account name already exists and already has login credentials.");
            return;
        }

        if(accountExists){
            boolean createdLogin=authManager.register(name, password);
            if(createdLogin){
                System.out.println("Account already exists. Login credentials created for "+name+".");
            }
            else{
                System.out.println("Login user could not be created.");
            }
            return;
        }

        boolean createdLogin=false;
        if(!loginExists){
            createdLogin=authManager.register(name, password);
            if(!createdLogin){
                System.out.println("Login user could not be created.");
                return;
            }
        }
        else{
            System.out.println("Login user already exists. Keeping the existing password.");
        }

        boolean createdAccount=submitCreateAccount(name, balance);
        if(!createdAccount && createdLogin){
            authManager.deleteUser(name);
        }
    }

    public void recoverCreateAccount(String name, double balance){
        submitRecoveryCreateAccount(name, balance);
    }

    private boolean submitCreateAccount(String name, double balance){
        String logEntry="CREATE "+ name+ " " + balance;
        boolean[] success={false};
        TransactionProcess process= new TransactionProcess(
            "TX-"+System.currentTimeMillis(),
            () ->{
            logger.log("BEGIN "+logEntry);
            if(bank.createAccount(name, balance)){
                success[0]=true;
                logger.log("COMMIT "+logEntry);
            }
        },
        2//priority
    );
        submitKernelProcess(process);
        return success[0];
    }

    private void submitRecoveryCreateAccount(String name, double balance){
        TransactionProcess process= new TransactionProcess(
            "REC-"+System.currentTimeMillis(),
            () -> bank.recoverCreateAccount(name, balance),
            2
        );
        submitKernelProcess(process);
    }
    public void transfer(String from, String to, double amount){
        user current = Session.getCurrentUser();
        if(current==null){
            System.out.println("Access denied: not logged in.");
            return;
        }

        //admin cannot transfer
        if(current.isAdmin()){
            System.out.println("Access denied: Admin cannot perform transfers.");
            return;
        }

        //must own source account
        if(!current.getUsername().equals(from)){
            System.out.println("Access denied: Cannot trasnfer from other accounts.");
            return;
        }

        //permission check
        if(!current.canTransfer()){
            System.out.println("Access denied: Insufficient permissions.");
            return;
        }

        submitTransfer(from, to, amount);
    }

    public void recoverTransfer(String from, String to, double amount){
        submitRecoveryTransfer(from, to, amount);
    }

    private void submitTransfer(String from, String to, double amount){
        String logEntry="TRANSFER "+from+" "+to+" "+amount;
        TransactionProcess process=new TransactionProcess(
            "TX-"+System.currentTimeMillis(),
            () ->{
                logger.log("BEGIN "+logEntry);
                if(bank.transfer(from, to, amount)){
                    logger.log("COMMIT "+logEntry);
                }
            },
            1//higher priority
        );
        submitKernelProcess(process);
    }

    private void submitRecoveryTransfer(String from, String to, double amount){
        TransactionProcess process=new TransactionProcess(
            "REC-"+System.currentTimeMillis(),
            () -> bank.recoverTransfer(from, to, amount),
            1
        );
        submitKernelProcess(process);
    }

    public void deposit(String name, double amount){
        user current=Session.getCurrentUser();
        if(current==null){
            System.out.println("Access denied: not logged in.");
            return;
        }
        if(!current.isAdmin() && !current.getUsername().equals(name)){
            System.out.println("Access denied: Cannot deposit to other accounts.");
            return;
        }
        submitDeposit(name, amount);
    }

    public void recoverDeposit(String name, double amount){
        submitRecoveryDeposit(name, amount);
    }

    private void submitDeposit(String name, double amount){
        String logEntry="DEPOSIT "+name+" "+amount;
        TransactionProcess process=new TransactionProcess(
            "TX-"+System.currentTimeMillis(),
            () ->{
                logger.log("BEGIN "+logEntry);
                if(bank.deposit(name, amount)){
                    logger.log("COMMIT "+logEntry);
                }
            },
            2
        );
        submitKernelProcess(process);
    }

    private void submitRecoveryDeposit(String name, double amount){
        TransactionProcess process=new TransactionProcess(
            "REC-"+System.currentTimeMillis(),
            () -> bank.recoverDeposit(name, amount),
            2
        );
        submitKernelProcess(process);
    }

    public void withdraw(String name, double amount){
        user current=Session.getCurrentUser();
        if(current==null){
            System.out.println("Access denied: not logged in.");
            return;
        }
        if(!current.isAdmin() && !current.getUsername().equals(name)){
            System.out.println("Access denied: Cannot withdraw from other accounts.");
            return;
        }
        submitWithdraw(name, amount);
    }

    public void recoverWithdraw(String name, double amount){
        submitRecoveryWithdraw(name, amount);
    }

    private void submitWithdraw(String name, double amount){
        String logEntry="WITHDRAW "+name+" "+amount;
        TransactionProcess process=new TransactionProcess(
            "TX-"+System.currentTimeMillis(),
            () ->{
                logger.log("BEGIN "+logEntry);
                if(bank.withdraw(name, amount)){
                    logger.log("COMMIT "+logEntry);
                }
            },
            2
        );
        submitKernelProcess(process);
    }

    private void submitRecoveryWithdraw(String name, double amount){
        TransactionProcess process=new TransactionProcess(
            "REC-"+System.currentTimeMillis(),
            () -> bank.recoverWithdraw(name, amount),
            2
        );
        submitKernelProcess(process);
    }

    private void submitKernelProcess(TransactionProcess process){
        modeBit.enterKernelMode();
        try{
            scheduler.submitProcess(process);
            scheduler.runPendingProcesses();
        }
        finally{
            modeBit.enterUserMode();
        }
    }

    public void grantTransfer(String username){
        user current=Session.getCurrentUser();

        if(current==null|| !current.isAdmin()){
            System.out.println("Access denied: Only admins can grant permissions.");
            return;
        }
        user target=authManager.getuser(username);
        if(target==null){
            System.out.println("User not found.");
            return;
        }
        if(!authManager.setTransferPermission(username, true)){
            System.out.println("Permission update failed.");
            return;
        }
        System.out.println("Permission granted for user "+username);
    }

    public void revokeTransfer(String username){
        user current=Session.getCurrentUser();

        if(current==null || !current.isAdmin()){
            System.out.println("Access denied: Only admins can revoke permissions.");
            return;
        }
        user target=authManager.getuser(username);
        if(target==null){
            System.out.println("User not found.");
            return;
        }
        if(!authManager.setTransferPermission(username, false)){
            System.out.println("Permission update failed.");
            return;
        }
        System.out.println("Transfer permission revoked for user "+username);
    }

    public void deleteUser(String username){
        user currentUser=Session.getCurrentUser();
        if(currentUser==null){
            System.out.println("Please login first.");
            return;
        }

        if(!currentUser.isAdmin()){
            System.out.println("Only admins can delete users.");
            return;
        }
        if(username.equalsIgnoreCase("root")){
            System.out.println("Root user cannot be deleted.");
            return;
        }
        if(currentUser.getUsername().equals(username)){
            System.out.println("Cannot delete youself.");
            return;
        }
        boolean success=authManager.deleteUser(username);
        if(!success){
            System.out.println("User not found.");
            return;
        }
        System.out.println("User "+username+" deleted.");
    }

    public void register(String username, String password){
        if(Session.isLoggedIn()){
            System.out.println("Logout before registering a new user.");
            return;
        }

        boolean success=authManager.register(username, password);
        if(success){
            System.out.println("User registered successfully.");
        }
        else{
            System.out.println("User already exists.");
        }
    }
    public void login(String username, String password){
        if(Session.isLoggedIn()){
            System.out.println("Logout before logging in.");
            return;
        }
        user u=authManager.login(username, password);
        if(u==null){
            System.out.println("Invalid credentials."); 
            return;
        }
        
        Session.login(u);
        System.out.println("Login successful. Welcome "+username );

    }
    public void logout(){
        if(!Session.isLoggedIn()){
            System.out.println("No user is currently logged in.");
            return;
        }
        Session.logout();
        System.out.println("Logout Successfull.");
    }

    public void checkBalance(String name){
        user current=Session.getCurrentUser();

        if(current == null){
            System.out.println("Access denied: not logged in.");
            return;
        }
        
        //admin can only viewe any account balance
        if(current.isAdmin()){
            bank.checkBalance(name);
            return;
        }

        //user can only view their own account balance
        if(!current.getUsername().equals(name)){
            System.out.println("Access denied: not unauthorized access");
            return;
        }
        bank.checkBalance(name);
    }

    public void showTransactions(String name){
        user current=Session.getCurrentUser();

        if(current == null){
            System.out.println("Access denied: not logged in.");
            return;
        }

        if(current.isAdmin()){
            bank.printTransactionsFor(name);
            return;
        }

        if(!current.getUsername().equals(name)){
            System.out.println("Access denied: not unauthorized access");
            return;
        }
        bank.printTransactionsFor(name);
    }

    public boolean hasCommittedStorage(){
        return bank.hasCommittedAccounts();
    }

    public void createLoan(String type, String borrower, double amount, int durationYears){
        user current=Session.getCurrentUser();

        if(current==null){
            System.out.println("Access denied: not logged in.");
            return;
        }

        if(!current.isAdmin() && !current.getUsername().equals(borrower)){
            System.out.println("Access denied: Cannot create loan for other users.");
            return;
        }

        if(current.isAdmin() && authManager.getuser(borrower)==null && !bank.hasAccount(borrower)){
            System.out.println("Borrower not found.");
            return;
        }

        loanManager.createLoan(type, borrower, amount, durationYears);
    }

    public void showLoans(String borrower){
        user current=Session.getCurrentUser();

        if(current==null){
            System.out.println("Access denied: not logged in.");
            return;
        }

        if(!current.isAdmin() && !current.getUsername().equals(borrower)){
            System.out.println("Access denied: Cannot view other users' loans.");
            return;
        }

        loanManager.printLoansFor(borrower);
    }

    public void showLoanRates(){
        loanManager.printRates();
    }

    public void updateLoansNow(){
        user current=Session.getCurrentUser();

        if(current==null || !current.isAdmin()){
            System.out.println("Access denied: Only admins can run loan updates manually.");
            return;
        }
        loanManager.updateAccruedInterest(true);
    }

    public void shutdown(){
        loanManager.shutdown();
    }
}
