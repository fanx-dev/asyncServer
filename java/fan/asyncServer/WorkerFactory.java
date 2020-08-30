/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fan.asyncServer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author yangjiandong
 */
abstract class WorkerFactory {
    private ThreadPoolExecutor workThreadPool;
    
    public WorkerFactory(int threadSize) {
        workThreadPool = new ThreadPoolExecutor(2, threadSize,
                                      60L, TimeUnit.SECONDS,
                                      new LinkedBlockingQueue<Runnable>());
    }
    
    public int pendingSize() {
        return workThreadPool.getQueue().size();
    }
    
    public ExecutorService getWorkThreadPool() {
        return workThreadPool;
    }
    
    public void close() { workThreadPool.shutdown(); }
    
    abstract Worker create(NioSelector selector);
}
