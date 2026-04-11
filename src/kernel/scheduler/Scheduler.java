package kernel.scheduler;

public class Scheduler {

    private PriorityScheduler priorityScheduler;
    private TimeSlice timeSlice;

    public Scheduler(){
        priorityScheduler=new PriorityScheduler();
        timeSlice=new TimeSlice(1000);// one second
    }
    public void submitProcess(TransactionProcess process){
        priorityScheduler.addProcess(process);
    }

    public void runPendingProcesses(){
        TransactionProcess process;
        while((process=priorityScheduler.getNextProcess()) != null){
            timeSlice.execute(process);
        }
    }
}
