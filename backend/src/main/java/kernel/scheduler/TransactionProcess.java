package kernel.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class TransactionProcess {
    @JsonIgnore
    private Runnable task;
    private int priority;
    private String processId;
    
    private String name;
    private long burstTime;
    private long remainingTime;
    private long arrivalTime;
    private long startTime = -1;
    private long completionTime;

    public TransactionProcess(String processId, Runnable task, int priority){
        this(processId, task, priority, "Transaction", 1);
    }

    public TransactionProcess(String processId, Runnable task, int priority, String name, long burstTime){
        this.task = task;
        this.priority = priority;
        this.processId = processId;
        this.name = name;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
        this.arrivalTime = System.currentTimeMillis();
    }

    public int getPriority() { return priority; }
    public String getProcessId() { return processId; }
    public String getName() { return name; }
    public long getBurstTime() { return burstTime; }
    public long getRemainingTime() { return remainingTime; }
    public long getArrivalTime() { return arrivalTime; }
    public long getStartTime() { return startTime; }
    public long getCompletionTime() { return completionTime; }
    
    public long getTurnAroundTime() {
        if(completionTime == 0) return 0;
        return completionTime - arrivalTime;
    }
    
    public long getWaitingTime() {
        return getTurnAroundTime() - burstTime;
    }

    public void setArrivalTime(long arrival) { this.arrivalTime = arrival; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public void setCompletionTime(long completionTime) { this.completionTime = completionTime; }
    public void setRemainingTime(long remainingTime) { this.remainingTime = remainingTime; }
    public void setBurstTime(long burstTime) { this.burstTime = burstTime; this.remainingTime = burstTime; }
    public void setName(String name) { this.name = name; }

    public void execute(){
        if(task != null) {
            task.run();
        }
    }
}
