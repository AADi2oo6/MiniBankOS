package kernel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class LockManager{

    private ConcurrentHashMap<String, ReentrantLock> locks=new ConcurrentHashMap<>();

    public ReentrantLock getLock(String accountName){
        locks.putIfAbsent(accountName, new ReentrantLock());


        return locks.get(accountName);
    }
}
