
package fan.asyncServer;

import fan.concurrent.Async;
import fan.concurrent.Promise;
import static fan.asyncServer.NioSelector.debug;
import fan.std.Map;
import fan.sys.Err;
import fan.sys.Func;
import java.nio.channels.SocketChannel;

/**
 */
public interface Handler {
    abstract Async onService(Socket socket);
}
