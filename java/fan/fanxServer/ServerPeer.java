
package fan.fanxServer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerPeer {
    public static ServerPeer make(Server self) {
        return new ServerPeer();
    }
    
    public void start(Server self) {
        start(self, true);
    }
    
    public void start(Server self, boolean join) {
        Connector server = new Connector(self.host(), (int)self.port(),
          new WorkerFactory((int)self.threadSize()) {
            @Override
            Worker create(NioSelector selector) {
                AsyncWorker w = new AsyncWorker();
                w.setPool(this);
                w.setSelector(selector);
                w.handler = self.handler;
                return w;
            }
        }, (int)self.limit());
        server.start();
        
        if (join) {
            try {
                server.join();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
}
