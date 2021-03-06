/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fan.asyncServer;

import fan.concurrent.Promise;
import fan.std.NioBuf;
import fan.std.NioBufPeer;
import fan.sys.IOErr;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author yangjiandong
 */
public class NioEvent implements Runnable {
    
    SocketChannel socket;
    int interestOps;
    Worker worker;
    SelectionKey selectionKey;
    
    //user data
    Promise promise;
    NioBuf buffer;
    long expectSize;
    long readOrWriteSize;
    
    //status
    boolean finished;
    boolean closed;

    public NioEvent(SocketChannel socket, Worker handler) {
        this.socket = socket;
        interestOps = SelectionKey.OP_READ;
        this.worker = handler;
        finished = false;
        closed = false;
    }

    public void cancel() {
        finished = true;
    }
    
    public void close() {
        closed = true;
        register();
    }
    
    public void register() {
        worker.getSelector().register(this);
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

    @Override
    public void run() {
        dispatchNioEvent();
    }
    
    protected void dispatchNioEvent() {
        Promise promise = this.promise;
        if (promise == null) {
            //new comming
            try {
                worker.onService(this.socket);
            } catch (Exception e) {
                e.printStackTrace();
                this.cancel();
                try {
                    this.socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            return;
        }

        try {
            if ((this.interestOps & SelectionKey.OP_READ) != 0) {
                onReadable();
            }
            else if ((this.interestOps & SelectionKey.OP_WRITE) != 0) {
                onWritable();
            }
            else if ((this.interestOps & SelectionKey.OP_CONNECT) != 0) {
                onConnect();
            }
            else {
                System.out.println("Unknow event:"+this.selectionKey.readyOps());
            }
        }
        catch (Exception e) {
            //e.printStackTrace();
            promise.complete(new fan.sys.Err(e), false);
            this.cancel();
            try {
                this.socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private void onReadable() throws IOException {
        Promise promise = this.promise;
        NioBuf buf = this.buffer;
            
        NioBufPeer peer = buf.peer;
        int n = this.socket.read(peer.toByteBuffer());
        
        if (NioSelector.debug) {
            System.out.println("onReadable: n:"+n
             +", expectSize:"+this.expectSize
             +", readOrWriteSize:"+(this.readOrWriteSize+n)
             +", buf:"+peer.toByteBuffer());
        }
        
        if (n < 0) {
            this.cancel();
            if (this.readOrWriteSize == 0) {
                promise.complete(-1L, true);
            }
            else {
                promise.complete(this.readOrWriteSize, true);
            }
            return;
        }
        
        this.readOrWriteSize += n;

        if (this.expectSize <= this.readOrWriteSize) {
            this.cancel();
            promise.complete(this.readOrWriteSize, true);
        }
        else {
            register();
        }
    }
    
    private void onWritable() throws IOException {
        Promise promise = this.promise;
        NioBuf buf = this.buffer;
            
        NioBufPeer peer = buf.peer;
        int n = this.socket.write(peer.toByteBuffer());
        
        if (NioSelector.debug) {
            System.out.println("onWritable: n:"+n
             +", expectSize:"+this.expectSize
             +", readOrWriteSize:"+(this.readOrWriteSize+n)
             +", buf:"+peer.toByteBuffer());
        }
        
        if (n < 0) {
            this.cancel();
            if (this.readOrWriteSize == 0) {
                promise.complete(IOErr.make("EOF"), false);
            }
            else {
                promise.complete(this.readOrWriteSize, true);
            }
            return;
        }
        
        this.readOrWriteSize += n;

        if (this.expectSize <= this.readOrWriteSize) {
            this.cancel();
            promise.complete(this.readOrWriteSize, true);
        }
        else {
            register();
        }
    }
    
    private void onConnect() throws IOException {
        Promise promise = this.promise;
        SocketChannel client = this.socket;
        this.cancel();
        client.finishConnect();

        Socket f = Socket.make();
        f.init(worker, client);
        //System.out.println("onConnect:"+f);
        promise.complete(f, true);
    }
    
}
