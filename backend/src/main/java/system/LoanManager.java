package system;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LoanManager {
    public static final String HOUSE = "house";
    public static final String EDUCATION = "education";
    public static final String BUSINESS = "business";

    private static final String LOAN_HEADER = "id,borrower,loanAmount,currentPrincipal,durationYears,remainingMonths,annualInterestRate,lastUpdatedAt,nextUpdateAt,status,state";
    private static final long MONTH_INTERVAL_MILLIS = 5L * 60L * 1000L;
    private static final double EDUCATION_INTEREST = 5.0;
    private static final double HOUSE_INTEREST = 8.0;
    private static final double BUSINESS_INTEREST = 12.0;

    private PageTable pageTable = new PageTable();
    private ScheduledExecutorService updater;
    private DecimalFormat moneyFormat = new DecimalFormat("0.00");

    public LoanManager(){
        bootstrapStorage();
    }

    public void startRealtimeUpdates(){
        if(updater != null && !updater.isShutdown()){
            return;
        }
        updater = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "loan-monthly-updater");
            thread.setDaemon(true);
            return thread;
        });
        updater.scheduleAtFixedRate(() -> updateAccruedInterest(false), 5, 5, TimeUnit.MINUTES);
    }

    public void shutdown(){
        if(updater != null){
            updater.shutdownNow();
        }
    }

    public synchronized boolean createLoan(String type, String borrower, double amount, int durationYears){
        String normalizedType = normalizeType(type);
        if(normalizedType == null){
            System.out.println("Invalid loan type. Use education, house, or business.");
            return false;
        }
        if(amount <= 0){
            System.out.println("Loan amount must be positive.");
            return false;
        }
        if(durationYears <= 0){
            System.out.println("Loan duration must be at least 1 year.");
            return false;
        }

        long now = System.currentTimeMillis();
        LoanRecord loan = new LoanRecord();
        loan.id = buildLoanId(normalizedType, now);
        loan.borrower = borrower;
        loan.loanAmount = roundMoney(amount);
        loan.currentPrincipal = roundMoney(amount);
        loan.durationYears = durationYears;
        loan.remainingMonths = durationYears * 12;
        loan.annualInterestRate = interestFor(normalizedType);
        loan.lastUpdatedAt = now;
        loan.nextUpdateAt = now + MONTH_INTERVAL_MILLIS;
        loan.status = "ACTIVE";
        loan.state = "COMMIT";

        try{
            CsvStorage.appendRow(fileFor(normalizedType), LOAN_HEADER, loan.toCsvRow());
            System.out.println("Loan approved. id="+loan.id+" type="+normalizedType+" yearlyInterest="+loan.annualInterestRate+"%");
            return true;
        }
        catch(IOException e){
            System.out.println("Loan storage update failed.");
            return false;
        }
    }

    public synchronized void printLoansFor(String borrower){
        updateAccruedInterest(false);
        boolean found = false;
        for(String type : new String[]{EDUCATION, HOUSE, BUSINESS}){
            try{
                List<LoanRecord> loans = readLoans(type);
                for(LoanRecord loan : loans){
                    if(!loan.borrower.equals(borrower) || !loan.state.equalsIgnoreCase("COMMIT")){
                        continue;
                    }
                    if(!found){
                        System.out.println("Loans for "+borrower+":");
                    }
                    found = true;
                    System.out.println(type+" | id="+loan.id+" | principal="+moneyFormat.format(loan.currentPrincipal)+" | durationYears="+loan.durationYears+" | remainingMonths="+loan.remainingMonths+" | yearlyInterest="+loan.annualInterestRate+"% | status="+loan.status);
                }
            }
            catch(IOException e){
                System.out.println("Loan lookup failed.");
                return;
            }
        }
        if(!found){
            System.out.println("No committed loans found for "+borrower+".");
        }
    }

    public List<Map<String, Object>> getLoansList(String borrower) {
        updateAccruedInterest(false);
        List<Map<String, Object>> result = new ArrayList<>();
        for(String type : new String[]{EDUCATION, HOUSE, BUSINESS}){
            try{
                List<LoanRecord> loans = readLoans(type);
                for(LoanRecord loan : loans){
                    if(!loan.borrower.equals(borrower) || !loan.state.equalsIgnoreCase("COMMIT")){
                        continue;
                    }
                    Map<String, Object> map = new java.util.LinkedHashMap<>();
                    map.put("category", type);
                    map.put("id", loan.id);
                    map.put("principal", loan.currentPrincipal);
                    map.put("originalAmount", loan.loanAmount);
                    map.put("durationYears", loan.durationYears);
                    map.put("remainingMonths", loan.remainingMonths);
                    map.put("yearlyInterest", loan.annualInterestRate);
                    map.put("status", loan.status);
                    result.add(map);
                }
            }
            catch(IOException e){
                // ignore
            }
        }
        return result;
    }

    public void printRates(){
        System.out.println("Education loan yearly interest: "+EDUCATION_INTEREST+"%");
        System.out.println("House loan yearly interest: "+HOUSE_INTEREST+"%");
        System.out.println("Business loan yearly interest: "+BUSINESS_INTEREST+"%");
        System.out.println("One real-time update happens every 5 minutes and represents 1 banking month.");
    }

    public Map<String, Object> getRatesData() {
        Map<String, Object> rates = new java.util.LinkedHashMap<>();
        rates.put(EDUCATION, EDUCATION_INTEREST);
        rates.put(HOUSE, HOUSE_INTEREST);
        rates.put(BUSINESS, BUSINESS_INTEREST);
        return rates;
    }

    public synchronized void updateAccruedInterest(boolean showMessage){
        int updatedCount = 0;
        for(String type : new String[]{EDUCATION, HOUSE, BUSINESS}){
            try{
                List<LoanRecord> loans = readLoans(type);
                boolean changed = false;
                long now = System.currentTimeMillis();
                for(LoanRecord loan : loans){
                    int appliedMonths = applyMonthlyInterest(loan, now);
                    if(appliedMonths > 0){
                        updatedCount += appliedMonths;
                        changed = true;
                    }
                }
                if(changed){
                    writeLoans(type, loans);
                }
            }
            catch(IOException e){
                System.out.println("Loan realtime update failed.");
            }
        }
        if(showMessage){
            System.out.println("Loan realtime update complete. Monthly updates applied: "+updatedCount);
        }
    }

    private int applyMonthlyInterest(LoanRecord loan, long now){
        if(!loan.state.equalsIgnoreCase("COMMIT") || !loan.status.equalsIgnoreCase("ACTIVE")){
            return 0;
        }

        int appliedMonths = 0;
        double monthlyRate = loan.annualInterestRate / 100.0 / 12.0;
        while(loan.remainingMonths > 0 && now >= loan.nextUpdateAt){
            loan.currentPrincipal = roundMoney(loan.currentPrincipal + (loan.currentPrincipal * monthlyRate));
            loan.remainingMonths--;
            loan.lastUpdatedAt = loan.nextUpdateAt;
            loan.nextUpdateAt = loan.nextUpdateAt + MONTH_INTERVAL_MILLIS;
            appliedMonths++;
        }

        if(loan.remainingMonths == 0){
            loan.status = "MATURED";
        }
        return appliedMonths;
    }

    private void bootstrapStorage(){
        try{
            pageTable.ensureFile(PageTable.HOUSE_LOANS_FILE, LOAN_HEADER);
            pageTable.ensureFile(PageTable.EDUCATION_LOANS_FILE, LOAN_HEADER);
            pageTable.ensureFile(PageTable.BUSINESS_LOANS_FILE, LOAN_HEADER);
        }
        catch(IOException e){
            System.out.println("Loan storage bootstrap failed.");
        }
    }

    private List<LoanRecord> readLoans(String type) throws IOException{
        List<LoanRecord> loans = new ArrayList<>();
        List<String[]> rows = CsvStorage.readRows(fileFor(type));
        for(String[] row : rows){
            LoanRecord loan = LoanRecord.fromCsvRow(row);
            if(loan != null){
                loans.add(loan);
            }
        }
        return loans;
    }

    private void writeLoans(String type, List<LoanRecord> loans) throws IOException{
        List<String[]> rows = new ArrayList<>();
        for(LoanRecord loan : loans){
            rows.add(loan.toCsvRow());
        }
        CsvStorage.writeRows(fileFor(type), LOAN_HEADER, rows);
    }

    private File fileFor(String type){
        return pageTable.getFile("loan-"+type);
    }

    private String normalizeType(String type){
        if(type == null){
            return null;
        }
        String normalized = type.toLowerCase();
        if(normalized.equals("edu")){
            return EDUCATION;
        }
        if(normalized.equals(HOUSE) || normalized.equals(EDUCATION) || normalized.equals(BUSINESS)){
            return normalized;
        }
        return null;
    }

    private double interestFor(String type){
        if(type.equals(EDUCATION)){
            return EDUCATION_INTEREST;
        }
        if(type.equals(HOUSE)){
            return HOUSE_INTEREST;
        }
        return BUSINESS_INTEREST;
    }

    private String buildLoanId(String type, long now){
        return type.toUpperCase()+"-"+now;
    }

    private double roundMoney(double value){
        return Math.round(value * 100.0) / 100.0;
    }

    private static class LoanRecord {
        private String id;
        private String borrower;
        private double loanAmount;
        private double currentPrincipal;
        private int durationYears;
        private int remainingMonths;
        private double annualInterestRate;
        private long lastUpdatedAt;
        private long nextUpdateAt;
        private String status;
        private String state;

        private String[] toCsvRow(){
            return new String[]{
                id,
                borrower,
                Double.toString(loanAmount),
                Double.toString(currentPrincipal),
                Integer.toString(durationYears),
                Integer.toString(remainingMonths),
                Double.toString(annualInterestRate),
                Long.toString(lastUpdatedAt),
                Long.toString(nextUpdateAt),
                status,
                state
            };
        }

        private static LoanRecord fromCsvRow(String[] row){
            if(row.length < 11){
                return null;
            }
            try{
                LoanRecord loan = new LoanRecord();
                loan.id = row[0];
                loan.borrower = row[1];
                loan.loanAmount = Double.parseDouble(row[2]);
                loan.currentPrincipal = Double.parseDouble(row[3]);
                loan.durationYears = Integer.parseInt(row[4]);
                loan.remainingMonths = Integer.parseInt(row[5]);
                loan.annualInterestRate = Double.parseDouble(row[6]);
                loan.lastUpdatedAt = Long.parseLong(row[7]);
                loan.nextUpdateAt = Long.parseLong(row[8]);
                loan.status = row[9];
                loan.state = row[10];
                return loan;
            }
            catch(NumberFormatException e){
                return null;
            }
        }
    }
}
