package kernel;

import system.BankDatabase;
import logging.Logger;
import kernel.scheduler.*;

public class TransactionManager{
    private BankDatabase bank;
    private Logger logger;
    private Scheduler scheduler;


    public TransactionManager(BankDatabase bank, Scheduler scheduler){
        this.bank=bank;
        this.scheduler=scheduler;
        this.logger=new Logger();//new logger object for each thread
    }
    public void createAccount(String name, double balance){
        String logEntry="CREATE "+ name+ " " + balance;
        TransactionProcess process= new TransactionProcess(
            "TX-"+System.currentTimeMillis(),
            () ->{
            logger.log("BEGIN "+logEntry);
            bank.createAccount(name, balance);
            logger.log("COMMIT "+logEntry);
        },
        2//priority
    );
        scheduler.submitProcess(process);
    }
    public void transfer(String from, String to, double amount){
        String logEntry="TRANSFER "+from+" "+to+" "+amount;
        TransactionProcess process=new TransactionProcess(
            "TX-"+System.currentTimeMillis(),
            () ->{
                logger.log("BEGIN "+logEntry);
                bank.transfer(from, to, amount);
                logger.log("COMMIT "+logEntry);
            },
            1//higher priority
        );
        scheduler.submitProcess(process);
    }
    public void checkBalance(String name){
        bank.checkBalance(name);
    }
}