/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fan.asyncServer;

import fan.concurrent.Async;
import static fan.asyncServer.NioSelector.debug;
import fan.sys.Err;
import fan.sys.Func;
import java.nio.channels.SocketChannel;

/**
 *
 * async/await support
 */
public class AsyncWorker extends AbstractWorker {
    
    public Handler handler;
    
    @Override
    public void onService(SocketChannel socket) {
        Socket f = Socket.make();
        f.init(this, socket);
        Async async = handler.onService(f);
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
            AsyncWorker.this.sendTask(new Runnable() {
                @Override
                public void run() {
                    s.step();
                }
            });
        }
    }
}
