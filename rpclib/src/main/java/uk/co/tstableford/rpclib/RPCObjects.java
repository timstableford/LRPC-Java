package uk.co.tstableford.rpclib;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class RPCObjects {
    public static class RPCString implements RPCObjectSerializer.ObjectType {
        private String data;

        public RPCString(String data) {
            this.data = data;
        }

        public RPCString(ByteBuffer buffer, int offset, int size) {
            this.parse(buffer, offset, size);
        }

        public void setData(String data) {
            this.data = data;
        }

        public String getData() {
            return this.data;
        }

        @Override
        public int getSize() {
            // +1 for the null terminator.
            return this.data.length() + 1;
        }

        @Override
        public RPCType getType() {
            return RPCType.STRING;
        }

        @Override
        public ByteBuffer getBytes() throws RPCObjectSerializer.InvalidTypeException {
            if (this.data.length() > 255) {
                throw new RPCObjectSerializer.InvalidTypeException();
            }
            ByteBuffer buffer = ByteBuffer.allocate(this.getSize() + 1);
            buffer.put(this.data.getBytes(Charset.forName("UTF-8")));
            buffer.put((byte) 0x00);
            return buffer;
        }

        @Override
        public RPCObjectSerializer.ObjectType parse(ByteBuffer buffer, int offset, int size) {
            byte strArray[] = new byte[size];
            buffer.position(offset);
            buffer.get(strArray, 0, size);
            if (strArray[size - 1] == 0x00) {
                size--;
            }
            this.data = new String(strArray, 0, size, Charset.forName("UTF-8"));

            return this;
        }

        @Override
        public String toString() {
            return this.data;
        }
    }

    public static class RPCFloat implements RPCObjectSerializer.ObjectType {
        protected float data;
        public RPCFloat(float data) {
            this.data = data;
        }

        public RPCFloat(ByteBuffer buffer, int offset, int size) {
            this.parse(buffer, offset, size);
        }

        public float getData() {
            return this.data;
        }

        public void setData(float data) {
            this.data = data;
        }

        @Override
        public int getSize() {
            return this.getType().getSize();
        }

        @Override
        public RPCType getType() {
            return RPCType.FLOAT;
        }

        @Override
        public ByteBuffer getBytes() throws RPCObjectSerializer.InvalidTypeException {
            ByteBuffer buffer = ByteBuffer.allocate(this.getSize());
            return buffer.putFloat(this.data);
        }

        @Override
        public RPCObjectSerializer.ObjectType parse(ByteBuffer buffer, int offset, int size) {
            this.data = buffer.getFloat(offset);
            return this;
        }

        @Override
        public String toString() {
            return java.lang.Float.toString(this.data);
        }
    }

    private static abstract class Number implements RPCObjectSerializer.ObjectType {
        protected long data;
        protected RPCType type;
        public Number(RPCType type, long data) {
            this.data = data;
            this.type = type;
        }

        public Number(ByteBuffer buffer, int offset) {
            this.parse(buffer, offset);
        }

        public long getData() {
            return this.data;
        }

        public void setData(long data) {
            this.data = data;
        }

        @Override
        public int getSize() {
            return this.getType().getSize();
        }

        @Override
        public RPCType getType() {
            return this.type;
        }

        public abstract void parse(ByteBuffer buffer, int offset);
        @Override
        public RPCObjectSerializer.ObjectType parse(ByteBuffer buffer, int offset, int size) {
            this.parse(buffer, offset);
            return this;
        }

        @Override
        public String toString() {
            return Long.toString(this.data);
        }
    }

    public static class RPCInt8 extends Number {
        public RPCInt8(long data) {
            super(RPCType.INT8, data);
        }

        @Override
        public ByteBuffer getBytes() throws RPCObjectSerializer.InvalidTypeException {
            if (this.data > Byte.MAX_VALUE || this.data < Byte.MIN_VALUE) {
                throw new RPCObjectSerializer.InvalidTypeException();
            }
            return ByteBuffer.allocate(this.getSize()).put((byte)(this.data & 0xff));
        }

        @Override
        public void parse(ByteBuffer buffer, int offset) {
            this.setData(buffer.get(offset) & (long) 0xff);
        }
    }

    public static class RPCUInt8 extends Number {
        public RPCUInt8(long data) {
            super(RPCType.UINT8, data);
        }

        @Override
        public ByteBuffer getBytes() throws RPCObjectSerializer.InvalidTypeException {
            if (this.data > 255 || this.data < 0) {
                throw new RPCObjectSerializer.InvalidTypeException();
            }
            return ByteBuffer.allocate(this.getSize()).put((byte)(this.data & 0xff));
        }

        @Override
        public void parse(ByteBuffer buffer, int offset) {
            this.setData(buffer.get(offset) & (long) 0xff);
        }
    }

    public static class RPCInt16 extends Number {
        public RPCInt16(long data) {
            super(RPCType.INT16, data);
        }

        @Override
        public ByteBuffer getBytes() throws RPCObjectSerializer.InvalidTypeException {
            if (this.data > Short.MAX_VALUE || this.data < Short.MIN_VALUE) {
                throw new RPCObjectSerializer.InvalidTypeException();
            }
            return ByteBuffer.allocate(this.getSize()).putShort((short)this.data);
        }

        @Override
        public void parse(ByteBuffer buffer, int offset) {
            this.setData(buffer.getShort(offset));
        }
    }

    public static class RPCUInt16 extends Number {
        public RPCUInt16(long data) {
            super(RPCType.UINT16, data);
        }

        @Override
        public ByteBuffer getBytes() throws RPCObjectSerializer.InvalidTypeException {
            if (this.data > 65535 || this.data < 0) {
                throw new RPCObjectSerializer.InvalidTypeException();
            }
            return ByteBuffer.allocate(this.getSize()).putShort((short) (this.data & 0xffff));
        }

        @Override
        public void parse(ByteBuffer buffer, int offset) {
            this.setData((long) (buffer.getShort(offset) & 0xffff));
        }
    }

    public static class RPCInt32 extends Number {
        public RPCInt32(long data) {
            super(RPCType.INT32, data);
        }

        @Override
        public ByteBuffer getBytes() throws RPCObjectSerializer.InvalidTypeException {
            if (this.data > Integer.MAX_VALUE || this.data < Integer.MIN_VALUE) {
                throw new RPCObjectSerializer.InvalidTypeException();
            }
            return ByteBuffer.allocate(this.getSize()).putInt((int)this.data);
        }

        @Override
        public void parse(ByteBuffer buffer, int offset) {
            this.setData(buffer.getInt(offset));
        }
    }

    public static class RPCUInt32 extends Number {
        public RPCUInt32(long data) {
            super(RPCType.UINT32, data);
        }

        @Override
        public ByteBuffer getBytes() throws RPCObjectSerializer.InvalidTypeException {
            if (this.data > 4294967295L || this.data < 0) {
                throw new RPCObjectSerializer.InvalidTypeException();
            }
            return ByteBuffer.allocate(this.getSize()).putInt((int) (this.data & 0xffffffffL));
        }

        @Override
        public void parse(ByteBuffer buffer, int offset) {
            this.setData((buffer.getInt(offset) & 0xffffffffL));
        }
    }

    public static class RPCInt64 extends Number {
        public RPCInt64(long data) {
            super(RPCType.INT64, data);
        }

        @Override
        public ByteBuffer getBytes() throws RPCObjectSerializer.InvalidTypeException {
            if (this.data > Integer.MAX_VALUE || this.data < Integer.MIN_VALUE) {
                throw new RPCObjectSerializer.InvalidTypeException();
            }
            return ByteBuffer.allocate(this.getSize()).putLong(this.data);
        }

        @Override
        public void parse(ByteBuffer buffer, int offset) {
            this.setData(buffer.getLong(offset));
        }
    }

    public static RPCObjectSerializer.ObjectType Int(RPCType type, long number) {
        switch (type) {
            case INT8:
                return new RPCInt8(number);
            case UINT8:
                return new RPCUInt8(number);
            case INT16:
                return new RPCInt16(number);
            case UINT16:
                return new RPCUInt16(number);
            case INT32:
                return new RPCInt32(number);
            case UINT32:
                return new RPCUInt32(number);
            case INT64:
                return new RPCInt64(number);
            default:
                return null;
        }
    }

    public static RPCObjectSerializer.ObjectType Float(float number) {
        return new RPCFloat(number);
    }

    public static RPCObjectSerializer.ObjectType String(String value) {
        return new RPCString(value);
    }
}
