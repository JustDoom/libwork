package com.imjustdoom.libwork.network;

import com.esotericsoftware.kryo.Kryo;

import java.io.IOException;

public interface EndPoint extends Runnable {

    /**
     * Runs the server
     */
    @Override
    void run();

    /**
     * Starts and runs the server in a new thread
     */
    void start();

    void update() throws IOException;

    /**
     * Closes the server from connections
     */
    void close();

    /**
     * Stops the server and closes server running
     */
    void stop();

    /**
     * Sets the listener object
     * @param listener
     */
    void addListener(Listener listener);

    void removeListener(Listener listener);

    Kryo getKryo();
}
