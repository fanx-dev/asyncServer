/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fan.fanxServer;

import fan.concurrent.Promise;
import fan.concurrent.Promise$;
import fan.std.Buf;
import fan.std.NioBuf;
import fan.std.NioBufPeer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author yangjiandong
 */
public class Worker implements Runnable {
    
    NioSelector selector;
    WorkerFactory factory;
    
    private Queue<Runnable> queue = new LinkedList<Runnable>();
    volatile boolean isRunning = false;
    
    public Worker() {
    }
    
    public void send(Runnable msg) {
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
    
    
    public void onService(SocketChannel socket) {
        
    }

    
    
    
}
