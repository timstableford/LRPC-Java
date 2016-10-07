package uk.co.tstableford.rpclib;

import java.nio.ByteBuffer;
import java.util.HashMap;

public class StreamParser {
    private static final int HEADER_SIZE = RPCObjectSerializer.Type.UINT16.getSize() * 3;
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
                    } catch (RPCObjectSerializer.InvalidTypeException e) {
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
            } catch (RPCObjectSerializer.InvalidTypeException e) {
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

        public Header(byte bytes[]) throws RPCObjectSerializer.InvalidTypeException {
            if (bytes.length < 6) {
                throw new RPCObjectSerializer.InvalidTypeException("Input data length too short.");
            }
            ByteBuffer buffer = ByteBuffer.allocate(RPCObjectSerializer.Type.UINT16.getSize() * 3);
            buffer.put(bytes, 0, RPCObjectSerializer.Type.UINT16.getSize() * 3);

            RPCObjects.RPCUInt16 type = new RPCObjects.RPCUInt16(0);
            type.parse(buffer, 0, type.getSize());
            RPCObjects.RPCUInt16 size = new RPCObjects.RPCUInt16(0);
            size.parse(buffer, RPCObjectSerializer.Type.UINT16.getSize(), RPCObjectSerializer.Type.UINT16.getSize());
            RPCObjects.RPCUInt16 crc = new RPCObjects.RPCUInt16(0);
            crc.parse(buffer, RPCObjectSerializer.Type.UINT16.getSize() * 2, RPCObjectSerializer.Type.UINT16.getSize());

            this.type = (int) type.getData();
            this.size = (int) size.getData();
            this.crc = (int) crc.getData();

            if (this.crc != CRC16.CRC(this.getTopBytes())) {
                throw new RPCObjectSerializer.InvalidTypeException("CRC's do not match.");
            }

            if (this.type == 0 && this.size == 0) {
                throw new RPCObjectSerializer.InvalidTypeException("Invalid type and size.");
            }
        }

        public byte[] getBytes() {
            ByteBuffer buffer = ByteBuffer.allocate(RPCObjectSerializer.Type.UINT16.getSize() * 3);
            try {
                buffer.put(this.getTopBytes(), 0, RPCObjectSerializer.Type.UINT16.getSize() * 2);
                RPCObjectSerializer.ObjectType crc = new RPCObjects.RPCUInt16(this.crc);
                buffer.put(crc.getBytes().array(), 0, RPCObjectSerializer.Type.UINT16.getSize());
            } catch (RPCObjectSerializer.InvalidTypeException e) {
                // As we know the input data this should never throw an exception.
                // If it has then it's cause for a fatal shutdown.
                throw new RuntimeException(e);
            }

            return buffer.array();
        }

        public boolean equals(Header other) {
            return this.crc == other.crc;
        }

        private byte[] getTopBytes() throws RPCObjectSerializer.InvalidTypeException {
            RPCObjectSerializer.ObjectType type = new RPCObjects.RPCUInt16(this.type);
            RPCObjectSerializer.ObjectType size = new RPCObjects.RPCUInt16(this.size);
            ByteBuffer buffer = ByteBuffer.allocate(RPCObjectSerializer.Type.UINT16.getSize() * 2);
            buffer.put(type.getBytes().array(), 0, RPCObjectSerializer.Type.UINT16.getSize());
            buffer.put(size.getBytes().array(), 0, RPCObjectSerializer.Type.UINT16.getSize());

            return buffer.array();
        }
    }
}
