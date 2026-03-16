package kernel.scheduler;

import java.util.LinkedList;
import java.util.Queue;

public class RoundRobinQueue {
    private Queue<TransactionProcess> queue=new LinkedList<>();
    public synchronized void addProcess(TransactionProcess p){
        queue.add(p);
    }
    public synchronized TransactionProcess getNext(){
        TransactionProcess p=queue.poll();
        if(p!=null){
            queue.add(p);

        }
        return p;
    }
}
