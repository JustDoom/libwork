package com.imjustdoom.libwork.network;

import java.util.HashMap;
import java.util.function.BiConsumer;

public interface Listener {

    default void connected(Connection connection) {

    }

    default void disconnected(Connection connection) {

    }

    default void received(Connection connection, Object data) {

    }

    class TypeListener implements Listener {

        @SuppressWarnings("rawtypes")
        private final HashMap<Class<?>, BiConsumer> listeners = new HashMap<>();

        @SuppressWarnings("unchecked")
        @Override
        public void received(Connection connection, Object data) {
            if (!this.listeners.containsKey(data.getClass())) return;

            this.listeners.get(data.getClass()).accept(connection, data);
        }

        public <T> void addPacketListener(Class<T> clazz, BiConsumer<? super Connection, ? super T> listener) {
            this.listeners.put(clazz, listener);
        }

        public <T> void removePacketListener(Class<T> clazz) {
            this.listeners.remove(clazz);
        }
    }

    // TODO: Type for running code on a separate thread so it doesnt block
}
