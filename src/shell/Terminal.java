package shell;

import java.util.Scanner;
import Auth.*;

import kernel.TransactionManager;

public class Terminal {
    private CommandParser parser;
    private AuthManager authManager;

    public Terminal(TransactionManager transactionManager, AuthManager authManager){
        parser=new CommandParser(transactionManager);
        this.authManager=authManager;
    }
    public void start(){
        Scanner sc=new Scanner(System.in);
        
        System.out.println("Booting BankingOS...");
        System.out.println("BankingOS Terminal Ready. Type 'help' for a list of commands.\n Type 'exit' to shutdown.");

        while(true){
            try{
                String username="guest";
                if(Session.isLoggedIn()){
                    username=Session.getCurrentUser().getUsername();
                }

                System.out.print("BankingOS@"+username+"> ");
                String command=sc.nextLine().trim();

                if(command.isEmpty()){
                    continue;
                }

                if(command.equalsIgnoreCase("exit")){
                    parser.shutdown();
                    System.out.println("Shutting down BankingOS...");
                    break;
                }

                String []tokens=command.trim().split("\\s+");
                String cmd=tokens[0];

                if(cmd.equalsIgnoreCase("help")){
                    parser.parse(command);
                    System.out.println();
                    continue;
                }

                if(cmd.equalsIgnoreCase("register")){
                    if(tokens.length!=3){
                        HelpPrinter.printUsage("register");
                        continue;
                    }
                    boolean success=authManager.register(tokens[1],tokens[2]);

                    if(success){
                        System.out.println("User registered sucessfully.");
                    }
                    else{
                        System.out.println("User already exists.");
                    }
                    continue;
                }
                if(cmd.equalsIgnoreCase("login")){
                    if(tokens.length!=3){
                        HelpPrinter.printUsage("login");
                        continue;
                    }
                    user u=authManager.login(tokens[1], tokens[2]);
                    if(u!=null){
                        Session.login(u);
                        System.out.println("Login successful");
                    }
                    else{
                        System.out.println("Invalid credentials.");
                    }
                    continue;
                }
                if(cmd.equalsIgnoreCase("logout")){
                    if(tokens.length!=1){
                        HelpPrinter.printUsage("logout");
                        continue;
                    }
                    Session.logout();
                    System.out.println("Logged out.");
                    continue;
                }
                
                if (!Session.isLoggedIn()){
                    System.out.println("Please login first.");
                    continue;
                }
                parser.parse(command);
                System.out.println();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
