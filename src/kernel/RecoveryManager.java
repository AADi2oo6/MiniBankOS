package kernel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import system.PageTable;

public class RecoveryManager{
    private TransactionManager transactionManager;
    private PageTable pageTable;
    
    public RecoveryManager(TransactionManager transactionManager){
        this.transactionManager=transactionManager;
        this.pageTable=new PageTable();

    }

    public void recover(){
        System.out.println("Recovering transactions...");
        if(transactionManager.hasCommittedStorage()){
            return;
        }

        File transactionLog=pageTable.getFile("transaction-log");
        if(!transactionLog.exists()){
            return;
        }

        try(BufferedReader br= new BufferedReader(new FileReader(transactionLog))){
            String line;
            while((line=br.readLine())!=null){
                if(line.startsWith("COMMIT")){
                    String command =line.substring(7);//slicing till just after COMMIT word
                    replay(command);
                
                }
            }
        }
        catch(IOException e){
            System.out.println("Recovery failed.");
        }
    }
    private void replay(String command){
        try{
            String[] tokens=command.split("\\s+");
            if(tokens.length==0){
                return;
            }
            if(tokens[0].equalsIgnoreCase("CREATE") && tokens.length==3){
                String name=tokens[1];
                double balance=Double.parseDouble(tokens[2]);
                transactionManager.recoverCreateAccount(name, balance);
                
            }
            else if(tokens[0].equalsIgnoreCase("TRANSFER") && tokens.length==4){
                String from=tokens[1];
                String to=tokens[2];
                double amount=Double.parseDouble(tokens[3]);

                transactionManager.recoverTransfer(from, to, amount);
            }
            else if(tokens[0].equalsIgnoreCase("DEPOSIT") && tokens.length==3){
                String name=tokens[1];
                double amount=Double.parseDouble(tokens[2]);

                transactionManager.recoverDeposit(name, amount);
            }
            else if(tokens[0].equalsIgnoreCase("WITHDRAW") && tokens.length==3){
                String name=tokens[1];
                double amount=Double.parseDouble(tokens[2]);

                transactionManager.recoverWithdraw(name, amount);
            }
        }
        catch(NumberFormatException e){
            System.out.println("Skipping malformed transaction log entry.");
        }
    }
}
