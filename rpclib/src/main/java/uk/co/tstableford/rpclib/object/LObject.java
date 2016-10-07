package uk.co.tstableford.rpclib.object;

import uk.co.tstableford.rpclib.serializer.LSerializer;

import java.nio.ByteBuffer;

public interface LObject {
    int getSize();
    LType getType();
    ByteBuffer getBytes() throws LSerializer.InvalidTypeException;
    LObject parse(ByteBuffer buffer, int offset, int size);
}
