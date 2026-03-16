package kernel.scheduler;

public class Scheduler {

    private PriorityScheduler priorityScheduler;
    private TimeSlice timeSlice;
    private boolean running;

    public Scheduler(){
        priorityScheduler=new PriorityScheduler();
        timeSlice=new TimeSlice(1000);// one second
        running=true;
    }
    public void submitProcess(TransactionProcess process){
        priorityScheduler.addProcess(process);
    }
    public void start(){
        Thread schedulerThread=new Thread(()->{
            while(running){
            TransactionProcess process=priorityScheduler.getNextProcess();
            if(process != null){
                timeSlice.execute(process);
            }
            else{
                try{
                    Thread.sleep(500);
                }
                catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                }
            }
        }});
        schedulerThread.start();
    }
    public void stop(){
        running=false;
    }
}