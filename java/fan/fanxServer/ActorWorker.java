/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fan.fanxServer;

import fan.concurrent.Actor;
import fan.concurrent.ActorPool;
import fan.concurrent.Async;
import static fan.fanxServer.NioSelector.debug;
import fan.std.Unsafe;
import fan.sys.Err;
import fan.sys.Func;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * Adapter between Actor and Worker
 */
public class ActorWorker implements Worker {
    private NioSelector selector;
    private Actor actor;

    //only valid in server-side socket
    public Handler handler;
    
    public static ActorWorker fromActor(Actor actor) {
        ActorWorker w = new ActorWorker();
        w.actor = actor;
        w.handler = null;
        
        w.selector = NioSelector.getDefaultSelector();
        return w;
    }
    
    public static ActorWorker makeNew(ActorPool actorPool) {
        ActorWorker w = new ActorWorker();
        w.actor = Actor.make(actorPool, new Func() {
            @Override
            public Object call(Object obj) {
                w.onReceive(obj);
                return null;
            }
        });
        w.handler = null;
        w.selector = NioSelector.getDefaultSelector();
        return w;
    }

    @Override
    public void sendTask(Runnable msg) {
        Unsafe us = Unsafe.make(msg);
        actor.send(us);
    }
    
    public boolean onReceive(Object obj) {
        onRunning();
        if (obj == null) return false;
        if (obj instanceof Unsafe) {
            Object val = ((Unsafe)obj).val();
            if (val instanceof Runnable) {
                ((Runnable)val).run();
                return true;
            }
        }
        return false;
    }

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
    public NioSelector getSelector() {
        return selector;
    }
    
    private void onRunning() {
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
            ActorWorker.this.sendTask(new Runnable() {
                @Override
                public void run() {
                    s.step();
                }
            });
        }
    }
}
