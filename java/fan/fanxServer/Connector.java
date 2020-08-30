/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fan.fanxServer;

import fan.fanxServer.NioEvent;
import static fan.fanxServer.NioSelector.debug;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author yangjiandong
 */
public class Connector extends Thread {

    private Selector selector;
    private NioSelector nioSelector;
    private WorkerFactory workerFactory;
    private ServerSocketChannel serverSocketChannel;
    private boolean isValid = true;
    int limit = 1024 * 10;

    public Connector(String host, int port, WorkerFactory workerFactory, int limit) {
        try {
            this.setName("Connector");
            this.workerFactory = workerFactory;
            this.limit = limit;
            
            nioSelector = new NioSelector();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            ServerSocket serverSocket = serverSocketChannel.socket();
            serverSocket.bind(new InetSocketAddress(host, port));
            
            
            nioSelector.start();
            
            System.out.println("Server start at: " + port);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public void close() {
        isValid = false;
        nioSelector.close();
        workerFactory.close();
        if (selector != null) selector.wakeup();
    }

    @Override
    public void run() {
        try {
            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            while (isValid) {
                try {
                    int n = selector.select();
                    if (n > 0) {
                        Set<SelectionKey> selectionKeys = selector.selectedKeys();
                        Iterator<SelectionKey> iterator = selectionKeys.iterator();
                        while (iterator.hasNext()) {
                            SelectionKey selectionKey = iterator.next();
                            iterator.remove();
                            handleKey(selectionKey);
                        }
                    }
                    else {
                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            selector.close();
            serverSocketChannel.close();
        } catch (IOException ex) {
            Logger.getLogger(Connector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void handleKey(SelectionKey selectionKey) throws IOException, InstantiationException, IllegalAccessException {
        ServerSocketChannel server = null;
        SocketChannel client = null;
        if (selectionKey.isAcceptable()) {
            server = (ServerSocketChannel) selectionKey.channel();
            client = server.accept();
            client.configureBlocking(false);
            
            if (workerFactory.pendingSize() > limit) {
                client.close();
                return;
            }
            
            Worker worker = workerFactory.create(nioSelector);
            //worker.setSelector(nioSelector);
            //worker.factory = workerFactory;
            NioEvent event = new NioEvent(client, worker);
            
            if (debug) System.out.println("accept event: "+event);
            nioSelector.register(event);
        }
    }
}
