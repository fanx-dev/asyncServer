/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fan.asyncServer;

import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;

/**
 *
 */
public abstract class AbstractWorker implements Worker, Runnable {
    private NioSelector selector;
    private WorkerFactory factory;
    
    private Queue<Runnable> queue = new LinkedList<Runnable>();
    private volatile boolean isRunning = false;
    
    public void setPool(WorkerFactory factory) {
        this.factory = factory;
    }
    
    public void setSelector(NioSelector selector) {
        this.selector = selector;
    }
    
    public NioSelector getSelector() {
        return this.selector;
    }
    
    public void sendTask(Runnable msg) {
        synchronized(this) {
            queue.offer(msg);
            if (!isRunning) {
                factory.getWorkThreadPool().execute(this);
            }
        }
    }

    @Override
    public void run() {
        synchronized(this) {
            isRunning = true;
            onRunning();
        }
        while (true) {
            Runnable msg = null;
            synchronized(this) {
                msg = queue.poll();
                if (msg == null) {
                    isRunning = false;
                    break;
                }
            }
            
            try {
                msg.run();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    protected void onRunning() {
    }
    
    
    abstract public void onService(SocketChannel socket);
}
