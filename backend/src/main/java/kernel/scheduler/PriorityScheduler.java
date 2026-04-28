package kernel.scheduler;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class PriorityScheduler {
    private TreeMap<Integer, RoundRobinQueue> priorityQueue;

    public PriorityScheduler(){
        priorityQueue=new TreeMap<>(Collections.reverseOrder());
    }

    public synchronized void addProcess(TransactionProcess process){
        int priority=process.getPriority();

        priorityQueue.putIfAbsent(priority, new RoundRobinQueue());

        priorityQueue.get(priority).addProcess(process);
    }

    public synchronized TransactionProcess getNextProcess(){
        for(Map.Entry<Integer, RoundRobinQueue> entry: priorityQueue.entrySet()){

            RoundRobinQueue queue=entry.getValue();

            TransactionProcess process=queue.getNext();

            if(process!=null){
                return process;
            }
        }
        return null;
    }

    public synchronized boolean hasProcesses() {
        for(RoundRobinQueue queue : priorityQueue.values()){
            if(!queue.isEmpty()) return true;
        }
        return false;
    }
}
