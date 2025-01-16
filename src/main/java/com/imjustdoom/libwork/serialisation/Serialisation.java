package com.imjustdoom.libwork.serialisation;

import java.nio.ByteBuffer;

public interface Serialisation {

    void write(ByteBuffer buffer, Object object);

    Object read(ByteBuffer buffer);

    void writeLength(ByteBuffer buffer, int length);

    int readLength(ByteBuffer buffer);

    int getLengthLength();
}
