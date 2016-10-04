package uk.co.tstableford.rpclib;

import java.nio.ByteBuffer;
import java.util.HashMap;

public class StreamParser {
    private static final int HEADER_SIZE = Object.Type.UINT16.getSize() * 3;
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

    int parse() {
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
                    } catch (Object.InvalidTypeException e) {
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
                    }
                    this.state = State.IDLE;
                }
            }
        }

        return readByte;
    }

    public interface StreamConnector {
        int readData();
        int writeData(byte data[]);
    }

    public interface StreamHandler {
        void onPacket(int type, int size, ByteBuffer buffer);
    }

    private class Header {
        private int type, size, crc;
        public Header(int type, int size) {
            this.type = type;
            this.size = size;
            try {
                this.crc = CRC16.CRC(this.getTopBytes());
            } catch (Object.InvalidTypeException e) {
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

        public Header(byte bytes[]) throws Object.InvalidTypeException {
            if (bytes.length < 6) {
                throw new Object.InvalidTypeException("Input data length too short.");
            }
            ByteBuffer buffer = ByteBuffer.allocate(Object.Type.UINT16.getSize() * 3);
            buffer.put(bytes, 0, Object.Type.UINT16.getSize() * 3);

            ObjectTypes.RPCUInt16 type = new ObjectTypes.RPCUInt16(0);
            type.parse(buffer, 0, type.getSize());
            ObjectTypes.RPCUInt16 size = new ObjectTypes.RPCUInt16(0);
            size.parse(buffer, Object.Type.UINT16.getSize(), Object.Type.UINT16.getSize());
            ObjectTypes.RPCUInt16 crc = new ObjectTypes.RPCUInt16(0);
            crc.parse(buffer, Object.Type.UINT16.getSize() * 2, Object.Type.UINT16.getSize());

            this.type = (int) type.getData();
            this.size = (int) size.getData();
            this.crc = (int) crc.getData();

            if (this.crc != CRC16.CRC(this.getTopBytes())) {
                throw new Object.InvalidTypeException("CRC's do not match.");
            }
        }

        public byte[] getBytes() {
            ByteBuffer buffer = ByteBuffer.allocate(Object.Type.UINT16.getSize() * 3);
            try {
                buffer.put(this.getTopBytes(), 0, Object.Type.UINT16.getSize() * 2);
                Object.ObjectType crc = new ObjectTypes.RPCUInt16(this.crc);
                buffer.put(crc.getBytes().array(), 0, Object.Type.UINT16.getSize());
            } catch (Object.InvalidTypeException e) {
                // As we know the input data this should never throw an exception.
                // If it has then it's cause for a fatal shutdown.
                throw new RuntimeException(e);
            }

            return buffer.array();
        }

        public boolean equals(Header other) {
            return this.crc == other.crc;
        }

        private byte[] getTopBytes() throws Object.InvalidTypeException {
            Object.ObjectType type = new ObjectTypes.RPCUInt16(this.type);
            Object.ObjectType size = new ObjectTypes.RPCUInt16(this.size);
            ByteBuffer buffer = ByteBuffer.allocate(Object.Type.UINT16.getSize() * 2);
            buffer.put(type.getBytes().array(), 0, Object.Type.UINT16.getSize());
            buffer.put(size.getBytes().array(), 0, Object.Type.UINT16.getSize());

            return buffer.array();
        }
    }
}