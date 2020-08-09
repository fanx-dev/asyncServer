/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fan.fanxServer;

import java.io.IOException;
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
    
    public static boolean debug = false;

    private Selector selector;
    private Queue<Event> queue = new ConcurrentLinkedQueue<Event>();
    private boolean isValid = true;
    
    public static class Event {
        SocketChannel socket;
        int interestOps;
        Worker handler;
        
        Object promise;
        Object buffer;
        long expectSize;
        long readOrWriteSize;
        boolean finished;
        
        public Event(SocketChannel socket, Worker handler) {
            this.socket = socket;
            interestOps = SelectionKey.OP_READ;
            this.handler = handler;
            finished = false;
        }
        
        public String toString() {
            StringBuilder res = new StringBuilder();
            if ((interestOps & SelectionKey.OP_READ) != 0) {
                res.append("read");
            }
            if ((interestOps & SelectionKey.OP_WRITE) != 0) {
                res.append("write");
            }
            if ((interestOps & SelectionKey.OP_CONNECT) != 0) {
                res.append("connet");
            }
            if ((interestOps & SelectionKey.OP_ACCEPT) != 0) {
                res.append("accept");
            }
            res.append(", ").append("promise:").append(promise);
            return res.toString();
        }
    }

    public NioSelector() throws IOException {
        this.setName("NioSelector");
    }
    
    public void register(Event msg) {
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
            selector = Selector.open();
            
            while (isValid) {
                try {
                    int n = selector.select();
                    boolean hasEvent = false;
                    
                    if (n > 0) {
                        hasEvent = true;
                        Set<SelectionKey> selectionKeys = selector.selectedKeys();
                        Iterator<SelectionKey> iterator = selectionKeys.iterator();
                        while (iterator.hasNext()) {
                            SelectionKey selectionKey = iterator.next();
                            iterator.remove();
                            handleKey(selectionKey);
                        }
                    }
                    
                    Event msg = queue.poll();
                    while (msg != null) {
                        
                        SelectionKey key = msg.socket.register(selector, msg.interestOps);
                        key.attach(msg);
                        
                        if (debug) System.out.println("register event: "+msg);
                        
                        msg = queue.poll();
                        hasEvent = true;
                    }
                    
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

    private void handleKey(SelectionKey selectionKey) throws IOException {
        try {
            Event event = (Event) selectionKey.attachment();
            selectionKey.interestOps(0);
            
            if (debug) System.out.println("handle event: "+event);
            
            event.handler.send(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
