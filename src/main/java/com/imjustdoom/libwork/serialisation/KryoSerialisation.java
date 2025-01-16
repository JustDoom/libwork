package com.imjustdoom.libwork.serialisation;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;

import java.nio.ByteBuffer;

public class KryoSerialisation implements Serialisation {

    private final Kryo kryo;
    private final ByteBufferInput bufferInput;
    private final ByteBufferOutput bufferOutput;

    public KryoSerialisation() {
        this.kryo = new Kryo();
        this.bufferInput = new ByteBufferInput();
        this.bufferOutput = new ByteBufferOutput();

        this.kryo.setReferences(true);
        this.kryo.setRegistrationRequired(true);
    }

    @Override
    public synchronized void write(ByteBuffer buffer, Object object) {
        this.bufferOutput.setBuffer(buffer);
        getKryo().writeClassAndObject(this.bufferOutput, object);
        this.bufferOutput.flush();
    }

    @Override
    public synchronized Object read(ByteBuffer buffer) {
        this.bufferInput.setBuffer(buffer);
        return getKryo().readClassAndObject(this.bufferInput);
    }

    @Override
    public void writeLength(ByteBuffer buffer, int length) {
        buffer.putInt(length);
    }

    @Override
    public int readLength(ByteBuffer buffer) {
        return buffer.getInt();
    }

    @Override
    public int getLengthLength() {
        return 4;
    }

    public Kryo getKryo() {
        return this.kryo;
    }
}
