package com.imjustdoom.libwork;

import com.esotericsoftware.kryo.Kryo;
import com.imjustdoom.libwork.network.Connection;
import com.imjustdoom.libwork.network.EndPoint;
import com.imjustdoom.libwork.network.Listener;
import com.imjustdoom.libwork.serialisation.KryoSerialisation;
import com.imjustdoom.libwork.serialisation.Serialisation;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class Client extends Connection implements EndPoint {

    // Network stuff
    private SocketChannel client;
    private Selector selector;

    private String address;
    private int port;
    private boolean running;
    private final Serialisation serialisation;

    public Client() {
        super();

        this.serialisation = new KryoSerialisation();
    }

    @Override
    public void run() {
        // TODO: Register internal packets, like keepalive

        // Start a new thread to read messages from the server
        try {
            this.running = true;
            while (this.running) {
                update();
            }
        } catch (IOException | ClosedSelectorException e) {
            stop();
        }
    }

    @Override
    public void start() {
        new Thread(this, "Client").start();
    }

    @Override
    public void update() throws IOException {
        this.selector.select(250);

        // Get the selected keys and process them
        Iterator<SelectionKey> keys = this.selector.selectedKeys().iterator();

        synchronized (keys) {
            while (keys.hasNext()) {
                SelectionKey key = keys.next();

                if (!key.isValid()) {
                    continue;
                }

                keys.remove();

                if (key.isConnectable()) {
                    try {
                        connect(key, this.selector);
                        submitConnected();
                    } catch (IOException exception) {
                        Logger.error(Logger.CLIENT, "An error occurred while trying to connect to the server");
//                        LoadingScreen.setText("An error occurred while trying to connect to the server");
                        exception.printStackTrace();
                        stop();
                    }
                } else if (key.isReadable()) {
                    while (true) {
                        Object packetObject = read();
                        if (packetObject == null) break;

                        submitReceived(packetObject);
                    }
                }
            }
        }
    }

    @Override
    public void close() {
        try {
            super.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void stop() {
        close();
        try {
            this.selector.close();
        } catch (IOException exception) {
            Logger.error(Logger.CLIENT, "Well dang. Closing the client gave an error");
            exception.printStackTrace();
        }
        this.running = false;
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

    public void connect() throws IOException {
        this.client = SocketChannel.open();

        initialise(this.client, 1024, 1024, this.serialisation);

        this.client.configureBlocking(false);
        this.client.connect(new InetSocketAddress(this.address, this.port));
        Logger.log(Logger.CLIENT, "Connection to the server socket has been made");

        this.selector = Selector.open();
        this.client.register(selector, SelectionKey.OP_CONNECT);
    }

    public void setAddress(String address) {
        String[] split = address.split(":");

        if (split.length > 1) {
            this.address = split[0];
            this.port = Integer.parseInt(split[1]);
        } else {
            this.address = address;
            this.port = 5000;
        }
    }
}
