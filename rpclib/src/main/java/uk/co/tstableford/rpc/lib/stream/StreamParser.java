package uk.co.tstableford.rpc.lib.stream;

import uk.co.tstableford.rpc.lib.object.LObject;
import uk.co.tstableford.rpc.lib.object.LObjects;
import uk.co.tstableford.rpc.lib.object.LType;
import uk.co.tstableford.rpc.lib.serializer.LSerializer;

import java.nio.ByteBuffer;
import java.util.HashMap;

public class StreamParser {
    private static final int HEADER_SIZE = LType.UINT16.getSize() * 3;
    private StreamConnector connector;
    private State state;
    private byte[] headerBytes = new byte[HEADER_SIZE];
    private Header header = null;
    private byte[] buffer;
    private int bufferIndex;
    private HashMap<Integer, StreamHandler> handlers;

    private enum State {
        IDLE,
        PARSING;
    }

    public StreamParser(StreamConnector connector, byte buffer[]) {
        this.connector = connector;
        this.state = State.IDLE;
        this.buffer = buffer;
        this.handlers = new HashMap<>();
    }

    public void addHandler(int type, StreamHandler handler) {
        this.handlers.put(type, handler);
    }

    public void removeHandler(int type) {
        if (this.handlers.containsKey(type)) {
            this.handlers.remove(type);
        }
    }

    public int parse() {
        int readByte = connector.readData();
        if (readByte >= 0) {
            switch (state) {
                case IDLE:
                {
                    for (int i = 0; i < HEADER_SIZE - 1; i++) {
                        headerBytes[i] = headerBytes[i + 1];
                    }
                    headerBytes[HEADER_SIZE - 1] = (byte) (readByte & 0xff);

                    try {
                        this.header = new Header(headerBytes);
                    } catch (LSerializer.InvalidTypeException e) {
                        this.header = null;
                        break;
                    }
                    this.bufferIndex = 0;
                    this.state = State.PARSING;
                    break;
                }
                case PARSING:
                {
                    if (this.header.getSize() > this.buffer.length) {
                        this.state = State.IDLE;
                        break;
                    }
                    this.buffer[this.bufferIndex] = (byte) (readByte & 0xff);
                    this.bufferIndex++;

                    if ((this.bufferIndex + 1) > this.header.getSize()) {
                        if (this.handlers.containsKey(this.header.getType())) {
                            ByteBuffer buffer = ByteBuffer.allocate(this.header.getSize());
                            buffer.put(this.buffer, 0, this.header.getSize());
                            this.handlers.get(this.header.getType()).onPacket(this.header.getType(), this.header.getSize(), buffer);
                        }
                        this.state = State.IDLE;
                    }
                }
            }
        }

        return readByte;
    }

    public interface StreamHandler {
        void onPacket(int type, int size, ByteBuffer buffer);
    }

    public static byte[] WrapBuffer(int type, ByteBuffer buffer) {
        Header header = new Header(type, buffer.capacity());
        ByteBuffer outBuffer = ByteBuffer.allocate(header.getBytes().length + buffer.capacity());
        outBuffer.put(header.getBytes());
        outBuffer.put(buffer.array());
        return outBuffer.array();
    }

    private static class Header {
        private int type, size, crc;
        public Header(int type, int size) {
            this.type = type;
            this.size = size;
            try {
                this.crc = CRC16.CRC(this.getTopBytes());
            } catch (LSerializer.InvalidTypeException e) {
                // As we know the input data this should never throw an exception.
                // If it has then it's cause for a fatal shutdown.
                throw new RuntimeException(e);
            }
        }

        public int getType() {
            return this.type;
        }

        public int getSize() {
            return this.size;
        }

        public Header(byte bytes[]) throws LSerializer.InvalidTypeException {
            if (bytes.length < 6) {
                throw new LSerializer.InvalidTypeException("Input data length too short.");
            }
            ByteBuffer buffer = ByteBuffer.allocate(LType.UINT16.getSize() * 3);
            buffer.put(bytes, 0, LType.UINT16.getSize() * 3);

            int[] headerInts = new int[3];
            for (int i = 0; i < headerInts.length; i++) {
                headerInts[i] = (int) LObjects.Int(LType.UINT16, buffer, LType.UINT16.getSize() * i).getData();
            }
            this.type = (int) LObjects.Int(LType.UINT16, buffer, LType.UINT16.getSize() * 0).getData();
            this.size = (int) LObjects.Int(LType.UINT16, buffer, LType.UINT16.getSize() * 1).getData();
            this.crc = (int) LObjects.Int(LType.UINT16, buffer, LType.UINT16.getSize() * 2).getData();

            if (this.type == 0) {
                throw new LSerializer.InvalidTypeException("Invalid type 0.");
            }

            if (this.crc != CRC16.CRC(this.getTopBytes())) {
                throw new LSerializer.InvalidTypeException("CRC's do not match.");
            }
        }

        public byte[] getBytes() {
            ByteBuffer buffer = ByteBuffer.allocate(LType.UINT16.getSize() * 3);
            try {
                buffer.put(this.getTopBytes(), 0, LType.UINT16.getSize() * 2);
                LObject crc = LObjects.Int(LType.UINT16, this.crc);
                buffer.put(crc.getBytes().array(), 0, crc.getSize());
            } catch (LSerializer.InvalidTypeException e) {
                // As we know the input data this should never throw an exception.
                // If it has then it's cause for a fatal shutdown.
                throw new RuntimeException(e);
            }

            return buffer.array();
        }

        public boolean equals(Header other) {
            return this.crc == other.crc;
        }

        private byte[] getTopBytes() throws LSerializer.InvalidTypeException {
            LObject type = LObjects.Int(LType.UINT16, this.type);
            LObject size = LObjects.Int(LType.UINT16, this.size);
            ByteBuffer buffer = ByteBuffer.allocate(type.getSize() + size.getSize());
            buffer.put(type.getBytes().array(), 0, type.getSize());
            buffer.put(size.getBytes().array(), 0, size.getSize());

            return buffer.array();
        }
    }
}
