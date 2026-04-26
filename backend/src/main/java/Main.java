import Auth.AuthManager;
import kernel.ModeBit;
import kernel.RecoveryManager;
import kernel.scheduler.Scheduler;
import shell.*;
import system.BankDatabase;
import kernel.TransactionManager;


public class Main {
    public static void main(String args[]){
        BankDatabase bank=new BankDatabase();
        Scheduler scheduler=new Scheduler();
        ModeBit modeBit=new ModeBit();
        
        AuthManager authManager=new AuthManager();
        TransactionManager transactionManager= new TransactionManager(bank, scheduler, authManager, modeBit);

        RecoveryManager recovery=new RecoveryManager(transactionManager);
        recovery.recover();

        Terminal terminal=new Terminal(transactionManager, authManager);
        terminal.start();
    }
}
