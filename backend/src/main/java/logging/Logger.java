package logging;

import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.io.PrintWriter;
import system.PageTable;

public class Logger{
    private final PageTable pageTable=new PageTable();
    private final File logFile=pageTable.getFile("transaction-log");

    public Logger(){
        try{
            //if non existent create new file and directory
            if(!logFile.exists()){
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
            }
        }
        catch(IOException e){
            System.out.println("Error creating log files.");
        }
    }

    public synchronized void log(String entry){
        try(FileWriter fw=new FileWriter(logFile, true);
            PrintWriter out=new PrintWriter(fw)){
                out.println(entry);
            }
            catch(IOException e){
                System.out.println("Logging failed.");
            }
    }
}
