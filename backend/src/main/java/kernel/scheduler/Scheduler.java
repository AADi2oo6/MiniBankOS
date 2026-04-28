package kernel.scheduler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scheduler {

    private PriorityScheduler priorityScheduler;
    private long globalClock = 0;
    private List<TransactionProcess> history;
    private List<TransactionProcess> arrivalQueue;
    private List<Map<String, Object>> ganttSlices;

    public Scheduler(){
        priorityScheduler = new PriorityScheduler();
        history = new ArrayList<>();
        arrivalQueue = new ArrayList<>();
        ganttSlices = new ArrayList<>();
    }

    public void submitProcess(TransactionProcess process){
        // Do not override arrival time. Controller provides it.
        arrivalQueue.add(process);
    }

    public synchronized void runPendingProcesses(long quantum){
        // Sort the arrival queue chronologically
        arrivalQueue.sort(Comparator.comparingLong(TransactionProcess::getArrivalTime));
        
        System.out.println("\n=======================================================");
        System.out.println("[OS KERNEL] STARTING BATCH SCHEDULER EXECUTION");
        System.out.println("[OS KERNEL] Quantum Length: " + quantum + "cs");
        System.out.println("=======================================================\n");

        while(!arrivalQueue.isEmpty() || priorityScheduler.hasProcesses()) {
            
            // Unload all processes that have officially "arrived" by the current globalClock
            while(!arrivalQueue.isEmpty() && arrivalQueue.get(0).getArrivalTime() <= globalClock) {
                TransactionProcess arriving = arrivalQueue.remove(0);
                System.out.println("[KERNEL ARRIVAL] Time: " + globalClock + "cs | PID: " + arriving.getProcessId() + " (" + arriving.getName() + ") added to Ready Queue.");
                priorityScheduler.addProcess(arriving);
            }
            
            TransactionProcess process = priorityScheduler.getNextProcess();
            
            if(process == null) {
                // If priorityScheduler is empty, it means we are waiting for the next process to arrive. Fast forward clock.
                if(!arrivalQueue.isEmpty()) {
                    long nextArrival = arrivalQueue.get(0).getArrivalTime();
                    System.out.println("[KERNEL IDLE] No tasks in Ready Queue. CPU idling for " + (nextArrival - globalClock) + "cs...");
                    globalClock = nextArrival;
                }
                continue;
            }
            
            if(process.getStartTime() == -1) {
                process.setStartTime(globalClock);
            }
            
            long runTime = Math.min(quantum, process.getRemainingTime());
            
            System.out.println("[KERNEL DISPATCH] Time: " + globalClock + "cs | Context Switching to PID: " + process.getProcessId() + " (" + process.getName() + ")");
            System.out.println("[KERNEL EXECUTE]  Executing " + runTime + " cycles... [Priority: " + process.getPriority() + ", Remaining: " + process.getRemainingTime() + "cs]");
            
            if(process.getRemainingTime() == process.getBurstTime()) {
                process.execute(); 
            }
            
            // Record execution slice for Gantt visualization
            Map<String, Object> slice = new HashMap<>();
            slice.put("processId", process.getProcessId());
            slice.put("name", process.getName());
            slice.put("priority", process.getPriority());
            slice.put("startTime", globalClock);
            slice.put("runTime", runTime);
            slice.put("endTime", globalClock + runTime);
            ganttSlices.add(slice);
            
            globalClock += runTime;
            process.setRemainingTime(process.getRemainingTime() - runTime);
            
            // Before putting the current process back in the queue, check if new processes have arrived during this execution slice
            while(!arrivalQueue.isEmpty() && arrivalQueue.get(0).getArrivalTime() <= globalClock) {
                TransactionProcess arriving = arrivalQueue.remove(0);
                System.out.println("[KERNEL INTERRUPT] Time: " + arriving.getArrivalTime() + "cs | Hardware interrupt -> New arrival: PID " + arriving.getProcessId() + " added to queue.");
                priorityScheduler.addProcess(arriving);
            }
            
            if(process.getRemainingTime() > 0) {
                System.out.println("[KERNEL PREEMPT]  Time: " + globalClock + "cs | PID: " + process.getProcessId() + " quantum expired. Moving back to Ready Queue.");
                priorityScheduler.addProcess(process);
            } else {
                System.out.println("[KERNEL COMPLETE] Time: " + globalClock + "cs | PID: " + process.getProcessId() + " execution successfully finished.");
                process.setCompletionTime(globalClock);
                history.add(process);
            }
            System.out.println("-------------------------------------------------------");
        }
        System.out.println("[OS KERNEL] SCHEDULER BATCH COMPLETED. Returning Telemetry.");
        System.out.println("=======================================================\n");
    }

    public List<TransactionProcess> getHistory() {
        return history;
    }
    
    public List<Map<String, Object>> getGanttSlices() {
        return ganttSlices;
    }
    
    public void resetHistory() {
        globalClock = 0;
        history.clear();
        arrivalQueue.clear();
        ganttSlices.clear();
    }
}
