package fan.fanxServer;

import fan.concurrent.Promise;
import fan.std.Buf;
import java.io.IOException;
import java.nio.channels.SocketChannel;
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
        return worker.read(socket, buf, size);
    }
    
    public Promise write(Buf buf) {
        return worker.write(socket, buf, buf.remaining());
    }
    
    public Promise write(Buf buf, long size) {
        return worker.write(socket, buf, size);
    }
    
    public boolean close() {
        try {
            socket.close();
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
}
