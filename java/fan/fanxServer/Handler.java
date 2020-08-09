
package fan.fanxServer;

import fan.concurrent.Async;
import fan.concurrent.Promise;
import static fan.fanxServer.NioSelector.debug;
import fan.std.Map;
import fan.sys.Err;
import fan.sys.Func;
import java.nio.channels.SocketChannel;

/**
 * Handler to support async await
 */
public abstract class Handler extends Worker {
    Map locals = Map.make();
//    public Promise connect(String host, int port) {
//        return this.connect(host, port);
//    }
    
    public static void make$(Handler self) {
    }
    
    @Override
    public void onService(SocketChannel socket) {
        Socket f = Socket.make();
        f.init(this, socket);
        Async async = this.onService(f);
        async.then(new Func(){
                    @Override
                    public Object call(Object result, Object err) {
                        
                        if (debug) System.out.println("close socket:"+socket);
                        f.close();
                        return null;
                    }
                });
    }
    
    @Override
    public void onRunning() {
        fan.concurrent.Actor.locals().set("async.runner", new Func() {
            @Override
            public Object call(Object o) {
                Async s = (Async)o;
                runAsync(s);
                return null;
            }
        });
    }
    
    private void runAsync(Async s) {
        if (s.next()) {
            Object awaitObj = s.awaitObj;
            if (awaitObj instanceof Promise) {
                Promise promise = (Promise)awaitObj;
                promise.then(new Func(){
                    @Override
                    public Object call(Object result, Object err) {
                        s.awaitObj = result;
                        s.err = (Err)err;
                        Handler.this.send(new Runnable() {
                            @Override
                            public void run() {
                                //continue call Actor.locals["async.runner"]
                                //s.run();
                                runAsync(s);
                            }
                        });
                        return null;
                    }
                });
            }
        }
    }
    
    abstract Async onService(Socket socket);
}
