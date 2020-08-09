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
    
    private Queue<Object> queue = new LinkedList<Object>();
    volatile boolean isRunning = false;
    
    public Worker() {
    }
    
    public void send(Object msg) {
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
            Object msg = null;
            synchronized(this) {
                msg = queue.poll();
                if (msg == null) {
                    isRunning = false;
                    break;
                }
            }
            
            if (msg instanceof NioSelector.Event) {
                NioSelector.Event event = (NioSelector.Event)msg;
                dispatchNioEvent(event);
            }
            else if (msg instanceof Runnable) {
                Runnable r = (Runnable)msg;
                try {
                    r.run();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    protected void onRunning() {
        
    }
    
    private void dispatchNioEvent(NioSelector.Event event) {
        Promise promise = (Promise)event.promise;
        if (promise == null) {
            //new comming
            try {
                onService(event.socket);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        try {
            if ((event.interestOps & SelectionKey.OP_READ) != 0) {
                onReadable(event);
            }
            else if ((event.interestOps & SelectionKey.OP_WRITE) != 0) {
                onWritable(event);
            }
            else if ((event.interestOps & SelectionKey.OP_CONNECT) != 0) {
                onConnect(event);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            promise.complete(e, false);
        }
    }
    
    private void onReadable(NioSelector.Event event) throws IOException {
        Promise promise = (Promise)event.promise;
        NioBuf buf = (NioBuf)event.buffer;
            
        NioBufPeer peer = buf.peer;
        int n = event.socket.read(peer.toByteBuffer());
        
        if (n < 0) {
            event.finished = true;
            promise.complete(event.readOrWriteSize, true);
            return;
        }
        
        event.readOrWriteSize += n;

        if (event.expectSize <= event.readOrWriteSize) {
            event.finished = true;
            promise.complete(event.readOrWriteSize, true);
        }
        else {
            selector.register(event);
        }
    }
    
    private void onWritable(NioSelector.Event event) throws IOException {
        Promise promise = (Promise)event.promise;
        NioBuf buf = (NioBuf)event.buffer;
            
        NioBufPeer peer = buf.peer;
        int n = event.socket.write(peer.toByteBuffer());
        
        if (n < 0) {
            event.finished = true;
            promise.complete(event.readOrWriteSize, true);
            return;
        }
        
        event.readOrWriteSize += n;

        if (event.expectSize <= event.readOrWriteSize) {
            event.finished = true;
            promise.complete(event.readOrWriteSize, true);
        }
        else {
            selector.register(event);
        }
    }
    
    private void onConnect(NioSelector.Event msg) throws IOException {
        Promise promise = (Promise)msg.promise;
        SocketChannel client = msg.socket;
        msg.finished = true;
        client.finishConnect();
        promise.complete(client, true);
    }
    
    public void onService(SocketChannel socket) {
        
    }
    
    public Promise read(SocketChannel socket, Buf buf, long size) {
        final Promise promise = Promise$.make();
        NioSelector.Event event = new NioSelector.Event(socket, this);
        event.promise = promise;
        event.buffer = buf;
        event.expectSize = size;
        
        selector.register(event);
        return promise;
    }
    
    public Promise write(SocketChannel socket, Buf buf, long size) {
        final Promise promise = Promise$.make();
        NioSelector.Event event = new NioSelector.Event(socket, this);
        event.interestOps = SelectionKey.OP_WRITE;
        event.promise = promise;
        event.buffer = buf;
        event.expectSize = size;
        
        selector.register(event);
        return promise;
    }
    
    
    public Promise connect(String host, int port) {
        final Promise promise = Promise$.make();
        try {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            
            InetSocketAddress addr = new InetSocketAddress(host, port);
            socketChannel.connect(addr);
            
            NioSelector.Event event = new NioSelector.Event(socketChannel, this);
            event.interestOps = SelectionKey.OP_CONNECT;
            event.promise = promise;
            
            selector.register(event);
            return promise;
        } catch (IOException ex) {
            promise.complete(ex, false);
            return promise;
        }
    }
    
}
