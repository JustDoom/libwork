package com.imjustdoom.libwork.network;

import com.imjustdoom.libwork.Logger;
import com.imjustdoom.libwork.serialisation.Serialisation;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class Connection {

    protected final List<Listener> listeners;
    private SocketChannel socketChannel;

    private Serialisation serialisation;
    private ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;

    private int currentLength;
    private final Object writeLock = new Object();

    public Connection() {
        this.listeners = new ArrayList<>();
    }

    public void initialise(SocketChannel channel, int readBufferLength, int writeBufferLength, Serialisation serialisation) {
        this.socketChannel = channel;

        this.serialisation = serialisation;
        this.readBuffer = ByteBuffer.allocate(readBufferLength);
        this.writeBuffer = ByteBuffer.allocate(writeBufferLength);

        this.readBuffer.flip();
    }

    public SelectionKey accept(Selector selector) throws IOException {
        this.socketChannel.configureBlocking(false);
        SelectionKey key = this.socketChannel.register(selector, SelectionKey.OP_READ);
        Logger.log(Logger.COMMON, "Accepted connection from " + this.socketChannel);
        return key;
    }

    public void connect(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        if (channel.isConnectionPending()) {
            channel.finishConnect();
        }
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        Logger.log(Logger.COMMON, "Connected to " + channel);
    }

    public Object read() throws IOException {

        if (this.currentLength == 0) {
            int lengthLength = getSerialisation().getLengthLength();
            if (!isMoreToRead(lengthLength)) return null;

            this.currentLength = getSerialisation().readLength(this.readBuffer);
        }

        int length = this.currentLength;

        if (!isMoreToRead(length)) return null;
        this.currentLength = 0;

        int startPosition = this.readBuffer.position();
        int oldLimit = this.readBuffer.limit();
        this.readBuffer.limit(startPosition + length);
        Object object = getSerialisation().read(this.readBuffer);

        this.readBuffer.limit(oldLimit);
        return object;
    }

    private boolean isMoreToRead(int length) throws IOException {
        if (this.readBuffer.remaining() < length) {
            this.readBuffer.compact();
            int bytesRead = this.socketChannel.read(this.readBuffer);
            this.readBuffer.flip();
            if (bytesRead == -1) {
                Logger.log(Logger.COMMON, "Connection has been closed");
                close();
                return false;
            }

            return this.readBuffer.remaining() >= length;
        }

        return true;
    }

    public void send(Object object) throws IOException {
        synchronized (this.writeLock) {
            this.writeBuffer.clear();

            // Free space for the object size
            this.writeBuffer.position(getSerialisation().getLengthLength());

            getSerialisation().write(this.writeBuffer, object);
            int end = this.writeBuffer.position();

            this.writeBuffer.position(0);
            this.getSerialisation().writeLength(this.writeBuffer, end - getSerialisation().getLengthLength());
            this.writeBuffer.position(end);

            this.writeBuffer.flip();
            this.socketChannel.write(this.writeBuffer);
        }
    }

    public void close() throws IOException {
        if (this.socketChannel != null) {
            this.socketChannel.close();
            this.socketChannel = null;

            submitDisconnected();
        }
    }

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }

    public void submitConnected() {
        for (Listener listener : this.listeners) {
            listener.connected(this);
        }
    }

    public void submitDisconnected() {
        for (Listener listener : this.listeners) {
            listener.disconnected(this);
        }
    }

    public void submitReceived(Object object) {
        for (Listener listener : this.listeners) {
            listener.received(this, object);
        }
    }

    public Serialisation getSerialisation() {
        return this.serialisation;
    }
}
