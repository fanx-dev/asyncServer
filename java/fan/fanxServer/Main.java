/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fan.fanxServer;

import fan.concurrent.Promise;
import fan.fanxServer.WorkerFactory;
import fan.std.Buf;
import fan.std.NioBuf;
import fan.sys.Func;
import fanx.main.BootEnv;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Test Web Server
 */
public class Main {
    
    static class TestHandler extends Worker {
        @Override
        public void onService(SocketChannel socket) {
            System.out.println("user accept:"+socket);
            
            Buf buf = NioBuf.makeMem(1024);
            Promise promise = this.read(socket, buf, 1);
            promise.then(new Func(){
                @Override
                public Object call(Object result, Object err) {
                    System.out.println("user receive:"+result +",err:"+err);
                    
                    buf.flip();
                    String res = buf.readAllStr();
                    System.out.println("user receiveContent:"+res);
                    
                    buf.clear();
                    buf.printLine("Hello");
                    buf.flip();
                    System.out.println("user send:"+buf);
                    TestHandler.this.write(socket, buf, 1);
                    
                    TestHandler.this.connect("www.baidu.com", 80);
                    
                    return null;
                }
            });
        }
    }
    
    public static void boot(String[] args)
    {
        System.getProperties().put("fan.home",    "/Users/yangjiandong/workspace/code/fanx/env/");
        BootEnv.setArgs(args);
    }
    
    public static void main(String[] args) throws IOException {
        boot(args);
        int port = 8888;
        Connector server = new Connector("localhost", port, new WorkerFactory(10) {
            @Override
            public Worker create() {
                return new TestHandler();
            }
        }, 10240);
        server.start();
        try {
            server.join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}
