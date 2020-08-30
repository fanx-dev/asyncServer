/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fan.fanxServer;

import static fan.fanxServer.NioSelector.debug;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Watch the IO event
 */
public class NioSelector extends Thread {
    public static NioSelector defaultSelector = null;
    public static boolean debug = false;

    private Selector selector;
    private Queue<NioEvent> queue = new ConcurrentLinkedQueue<NioEvent>();
    private boolean isValid = true;
    
    
    public static NioSelector getDefaultSelector() {
        if (defaultSelector == null) {
            try {
                defaultSelector = new NioSelector();
                defaultSelector.start();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return defaultSelector;
    }
    

    public NioSelector() throws IOException {
        this.setName("NioSelector");
        selector = Selector.open();
    }
    
    public void register(NioEvent msg) {
        queue.offer(msg);
        selector.wakeup();
    }
    
    public void close() {
        isValid = false;
        if (selector != null) selector.wakeup();
    }
    
    @Override
    public void run() {
        try {
            while (isValid) {
                try {
                    boolean hasEvent = false;
                    
                    if (doRegister()) hasEvent = true;
                    
                    if (doSelect()) hasEvent = true;
                    
                    if (!hasEvent) {
                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            selector.close();
        } catch (IOException ex) {
            Logger.getLogger(NioSelector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private boolean doSelect() throws IOException {
        int n = selector.select();
        if (n > 0) {
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                iterator.remove();
                handleKey(selectionKey);
            }
            return true;
        }
        return false;
    }
    
    private boolean doRegister() throws ClosedChannelException {
        boolean hasEvent = false;
        while (true) {
            NioEvent msg = queue.poll();
            if (msg == null) break;
            
            try {
                if (msg.closed) {
                    if (msg.selectionKey != null) {
                        msg.selectionKey.cancel();
                    }
                }
                else {
                    if (msg.selectionKey != null) {
                        msg.selectionKey.interestOps(msg.interestOps);
                        if (debug) System.out.println("reset event: "+msg);
                    }
                    else {
                        SelectionKey key = msg.socket.register(selector, msg.interestOps);
                        key.attach(msg);
                        msg.selectionKey = key;
                        if (debug) System.out.println("register event: "+msg);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            hasEvent = true;
        }
        return hasEvent;
    }

    private void handleKey(SelectionKey selectionKey) throws IOException {
        try {
            NioEvent event = (NioEvent) selectionKey.attachment();
            
            if (!selectionKey.isValid()) {
                if (debug) System.out.println("invalid event: "+event);
                selectionKey.cancel();
                return;
            }
            
            if ((event.interestOps & selectionKey.readyOps()) != 0) {
                selectionKey.interestOps(0);
            
                if (debug) System.out.println("handle event: "+event);

                event.worker.sendTask(event);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
