package uk.co.tstableford.rpclib;

import java.nio.ByteBuffer;

public interface LObject {
    int getSize();
    LType getType();
    ByteBuffer getBytes() throws LSerializer.InvalidTypeException;
    LObject parse(ByteBuffer buffer, int offset, int size);
}
