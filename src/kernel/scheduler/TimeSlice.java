package kernel.scheduler;

public class TimeSlice {
    private long quantum;

    public TimeSlice(long quantum){
        this.quantum=quantum;
    }
    public void execute(TransactionProcess p){
        try{
            p.execute();
            Thread.sleep(quantum);
        }
        catch(InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }
    public long getQuantum(){
        return quantum;
    }
}
