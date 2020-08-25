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

public class Socket {
    Worker worker;
    SocketChannel socket;
    
    public void init(Worker worker, SocketChannel socket) {
        this.worker = worker;
        this.socket = socket;
    }
    
    public static Socket make() {
        return new Socket();
    }

    public Promise read(Buf buf, long size) {
        final Promise promise = Promise$.make();
        NioEvent event = new NioEvent(socket, worker);
        event.promise = promise;
        event.buffer = (NioBuf)buf;
        event.expectSize = size;
        
        worker.selector.register(event);
        return promise;
    }

    public Promise write(Buf buf) {
        return write(buf, buf.remaining());
    }
    
    public Promise write(Buf buf, long size) {
        final Promise promise = Promise$.make();
        NioEvent event = new NioEvent(socket, worker);
        event.interestOps = SelectionKey.OP_WRITE;
        event.promise = promise;
        event.buffer = (NioBuf)buf;
        event.expectSize = size;
        
        worker.selector.register(event);
        return promise;
    }
    
    public boolean close() {
        try {
            socket.close();
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public static Promise connect(String host, long port) {
        Worker worker = (Worker)fan.concurrent.Actor.locals().get("fanxServer.worker");
        if (worker == null) {
            throw fan.sys.Err.make("worker not setup in concurrent Acotr.locals");
        }
        return connect(host, (int)port, worker);
    }

    public static Promise connect(String host, int port, Worker worker) {
        final Promise promise = Promise$.make();
        try {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            
            InetSocketAddress addr = new InetSocketAddress(host, port);
            socketChannel.connect(addr);
            
            NioEvent event = new NioEvent(socketChannel, worker);
            event.interestOps = SelectionKey.OP_CONNECT;
            event.promise = promise;
            
            worker.selector.register(event);
            return promise;
        } catch (IOException ex) {
            promise.complete(ex, false);
            //System.out.println(ex);
            return promise;
        }
    }
}
