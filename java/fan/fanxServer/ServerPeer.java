
package fan.fanxServer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerPeer {
    public static ServerPeer make(Server self) {
        return new ServerPeer();
    }
    
    public void start(Server self) {
        Connector server = new Connector(self.host(), (int)self.port(),
          new WorkerFactory((int)self.threadSize()) {
            @Override
            public Worker create() {
                return self.create();
            }
        }, (int)self.limit());
        server.start();
        try {
            server.join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}
