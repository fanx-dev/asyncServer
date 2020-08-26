
package fan.fanxServer;

import fan.concurrent.Async;
import fan.concurrent.Promise;
import static fan.fanxServer.NioSelector.debug;
import fan.std.Map;
import fan.sys.Err;
import fan.sys.Func;
import java.nio.channels.SocketChannel;

/**
 * Handler to support async/await
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
                        /*
                        在onService调用异步onService重载函数后，任务已经被async.runner执行了。我们需要添加一个最终的打印异常函数和关闭socket。
                        */
                        if (err != null) {
                            ((Err)err).trace();
                        }
                        if (debug) System.out.println("close socket:"+socket);
                        f.close();
                        return null;
                    }
                });
    }
    
    @Override
    public void onRunning() {
        fan.concurrent.Actor.locals().set("fanxServer.worker", this);

        /*
        async/await需要一个线程looper来运行接下来的任务，所以需要注册Actor.locals["async.runner"]。
        这个async.runner做的事情就是给Promise连接一个完成回调，回调在指定线程looper执行。async.runner和当前的worker一一对应。
        因为共享执行线程，所以每次Actor.locals都有初始化。
        */
        fan.concurrent.Actor.locals().set("async.runner", asyncRunner);
    }

    private IOAsyncRunner asyncRunner = new IOAsyncRunner();

    class IOAsyncRunner implements fan.concurrent.AsyncRunner {

        @Override public boolean awaitOther(Async s, Object awaitObj) {
            return false;
        }
        @Override public void run(Async s) {
            Handler.this.send(new Runnable() {
                @Override
                public void run() {
                    s.step();
                }
            });
        }
    }
    
    abstract Async onService(Socket socket);
}
