package kernel.scheduler;

public class TransactionProcess {
    private Runnable task;
    private int priority;
    private String processId;

    public TransactionProcess(String processId, Runnable task,  int priority){
        this.task=task;
        this.priority=priority;
        this.processId=processId;
    }
    public int getPriority(){
        return priority;
    }
    public String getProcessId(){
        return processId;
    }
    public void execute(){
        task.run();
    }
}
