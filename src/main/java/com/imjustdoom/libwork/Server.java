package com.imjustdoom.libwork;

import com.esotericsoftware.kryo.Kryo;
import com.imjustdoom.libwork.network.Connection;
import com.imjustdoom.libwork.network.EndPoint;
import com.imjustdoom.libwork.network.Listener;
import com.imjustdoom.libwork.serialisation.KryoSerialisation;
import com.imjustdoom.libwork.serialisation.Serialisation;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Server implements EndPoint {

    // Network stuff
    private final List<Connection> connections;
    private final List<Listener> listeners;
    private Selector selector;
    private ServerSocketChannel serverSocket;

    private final CountDownLatch portLatch;
    private final CountDownLatch startLatch;
    private int port;
    private volatile boolean running;
    private final Serialisation serialisation;

    private final Listener dispatch = new Listener.TypeListener() {
        @Override
        public void connected(Connection connection) {
            for (Listener listener : listeners) {
                listener.connected(connection);
            }
        }

        @Override
        public void disconnected(Connection connection) {
            connections.remove(connection);
            for (Listener listener : listeners) {
                listener.disconnected(connection);
            }
        }

        @Override
        public void received(Connection connection, Object object) {
            for (Listener listener : listeners) {
                listener.received(connection, object);
            }
        }
    };

    public Server() {
        this.connections = Collections.synchronizedList(new ArrayList<>());
        this.listeners = new ArrayList<>();

        this.portLatch = new CountDownLatch(1);
        this.startLatch = new CountDownLatch(1);
        this.running = false;
        this.serialisation = new KryoSerialisation();
    }

    protected Connection newConnection() { //(SocketChannel channel, int readBufferLength, int writeBufferLength, Serialisation serialisation)
        return new Connection();
    }

    @Override
    public void run() {
        this.running = true;
        this.startLatch.countDown();
        while (this.running) {
            try {
                update();
            } catch (IOException exception) {
                Logger.error(Logger.SERVER, "There was an error with the server :(");
                exception.printStackTrace();
                stop();
            } catch (ClosedSelectorException ignored) {}
        }

    }

    @Override
    public void start() {
        new Thread(this, "Server").start();
    }

    @Override
    public void update() throws IOException {
        this.selector.select(250);
        Iterator<SelectionKey> keys = this.selector.selectedKeys().iterator();

        synchronized (keys) {
            while (keys.hasNext()) {
                SelectionKey key = keys.next();

                if (!key.isValid()) {
                    continue;
                }

                keys.remove();

                if (key.isAcceptable()) {
                    try {
                        SocketChannel channel = this.serverSocket.accept();
                        Connection connection = newConnection();
                        connection.addListener(this.dispatch);
                        connection.initialise(channel, 1024, 1024, this.serialisation);
                        connection.accept(this.selector).attach(connection);
                        this.connections.add(connection);

                        connection.submitConnected();

                    } catch (IOException exception) {
                        Logger.error(Logger.SERVER, "Unable to accept the client");
                        exception.printStackTrace();
                    }

                } else if (key.isReadable()) {
                    Connection connection = (Connection) key.attachment();

                    while (true) {
                        Object packetObject = connection.read();
                        if (packetObject == null) break;

                        connection.submitReceived(packetObject);
                    }
                }
            }
        }
    }

    @Override
    public void close() {
        synchronized (this.connections) {
            Iterator<Connection> connectionIterator = this.connections.iterator();
            while (connectionIterator.hasNext()) {
                Connection connection = connectionIterator.next();
                try {
                    connectionIterator.remove();
                    connection.close();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
        }

        try {
            this.serverSocket.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void stop() {
        this.running = false;
        close();
        try {
            this.selector.close();
        } catch (IOException exception) {
            Logger.error(Logger.SERVER, "Well dang. Closing the server gave an error");
            exception.printStackTrace();
        }
    }

    @Override
    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public Kryo getKryo() {
        return this.serialisation instanceof KryoSerialisation ? ((KryoSerialisation) this.serialisation).getKryo() : null;
    }

    /**
     * Creates and binds the socket to the port
     *
     * @throws IOException
     */
    public void bind() throws IOException {
        // TODO: Register internal packets, like keepalive when added

        this.selector = Selector.open();
        this.serverSocket = ServerSocketChannel.open();
        this.serverSocket.bind(new InetSocketAddress("localhost", 5000)); // TODO: Make it randomly select a port if the default can't be used
        this.serverSocket.configureBlocking(false);
        this.serverSocket.register(this.selector, SelectionKey.OP_ACCEPT);

        this.port = this.serverSocket.socket().getLocalPort();
        Logger.log(Logger.SERVER, "Server opened on port " + this.port); // TODO: Remove game specific logging from here maybe

        this.portLatch.countDown();
    }

    public void sendToAll(Object packet) {
        for (Connection connection : this.connections) {
            try {
                connection.send(packet);
            } catch (IOException exception) {
//                Logger.log(Logger.SERVER, "Client " + ((Player) connection).getName() + " has disconnected");
                try {
                    connection.close();
                } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Using a CountDownLatch to wait for the server to start and report it successfully started on the chosen port
     *
     * @return the port that the server is running on
     * @throws InterruptedException
     */
    public int awaitPort() throws InterruptedException {
        // Wait for this latch to reach 0 (The port is confirmed)
        this.portLatch.await();
        return this.port;
    }

    public boolean awaitStart() throws InterruptedException {
        this.startLatch.await();
        return this.running;
    }

    public boolean isRunning() {
        return this.running;
    }
}
