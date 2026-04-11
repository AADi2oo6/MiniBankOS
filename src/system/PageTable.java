package system;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;

public class PageTable {
    public static final String USER_FILE = "user.csv";
    public static final String AUTH_FILE = "auth.csv";
    public static final String TRANSACTIONS_FILE = "transactions.csv";
    public static final String TRANSACTION_LOG_FILE = "transaction.log";
    public static final String HOUSE_LOANS_FILE = "house_loans.csv";
    public static final String EDUCATION_LOANS_FILE = "education_loans.csv";
    public static final String BUSINESS_LOANS_FILE = "business_loans.csv";

    private Hashtable<String, String> table = new Hashtable<>();
    private File dataDirectory;

    public PageTable(){
        dataDirectory = findDataDirectory();

        table.put("create", USER_FILE);
        table.put("balance", USER_FILE);
        table.put("deposit", USER_FILE);
        table.put("withdraw", USER_FILE);
        table.put("transfer", USER_FILE);

        table.put("register", AUTH_FILE);
        table.put("login", AUTH_FILE);
        table.put("logout", AUTH_FILE);
        table.put("grant", AUTH_FILE);
        table.put("revoke", AUTH_FILE);
        table.put("delete", AUTH_FILE);

        table.put("transactions", TRANSACTIONS_FILE);
        table.put("history", TRANSACTIONS_FILE);
        table.put("transaction-log", TRANSACTION_LOG_FILE);

        table.put("loan-house", HOUSE_LOANS_FILE);
        table.put("loan-education", EDUCATION_LOANS_FILE);
        table.put("loan-business", BUSINESS_LOANS_FILE);
        table.put("loans", HOUSE_LOANS_FILE);
        table.put("loanrates", HOUSE_LOANS_FILE);
        table.put("loanupdate", HOUSE_LOANS_FILE);
    }

    public String getFileName(String operation){
        String fileName = table.get(operation.toLowerCase());
        if(fileName == null){
            return operation;
        }
        return fileName;
    }

    public File getFile(String operation){
        return getFileByName(getFileName(operation));
    }

    public File getFileByName(String fileName){
        ensureDataDirectory();
        return new File(dataDirectory, fileName);
    }

    public void ensureFile(String fileName, String header) throws IOException{
        ensureDataDirectory();
        File file = getFileByName(fileName);
        if(!file.exists() || file.length() == 0){
            try(FileWriter writer = new FileWriter(file)){
                writer.write(header);
                writer.write(System.lineSeparator());
            }
        }
    }

    private void ensureDataDirectory(){
        if(!dataDirectory.exists()){
            dataDirectory.mkdirs();
        }
    }

    private File findDataDirectory(){
        File srcDataDirectory = new File("src" + File.separator + "data");
        if(srcDataDirectory.exists()){
            return srcDataDirectory;
        }
        return new File("data");
    }
}
