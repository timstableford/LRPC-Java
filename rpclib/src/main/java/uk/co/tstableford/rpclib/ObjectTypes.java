package uk.co.tstableford.rpclib;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class ObjectTypes {
    public static class RPCString implements Object.ObjectType {
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
        public Object.Type getType() {
            return Object.Type.STRING;
        }

        @Override
        public ByteBuffer getBytes() throws Object.InvalidTypeException {
            if (this.data.length() > 255) {
                throw new Object.InvalidTypeException();
            }
            ByteBuffer buffer = ByteBuffer.allocate(this.getSize() + 1);
            buffer.put(this.data.getBytes(Charset.forName("UTF-8")));
            buffer.put((byte) 0x00);
            return buffer;
        }

        @Override
        public Object.ObjectType parse(ByteBuffer buffer, int offset, int size) {
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

    public static class RPCFloat implements Object.ObjectType {
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
        public Object.Type getType() {
            return Object.Type.FLOAT;
        }

        @Override
        public ByteBuffer getBytes() throws Object.InvalidTypeException {
            ByteBuffer buffer = ByteBuffer.allocate(this.getSize());
            return buffer.putFloat(this.data);
        }

        @Override
        public Object.ObjectType parse(ByteBuffer buffer, int offset, int size) {
            this.data = buffer.getFloat(offset);
            return this;
        }

        @Override
        public String toString() {
            return java.lang.Float.toString(this.data);
        }
    }

    private static abstract class Number implements Object.ObjectType {
        protected long data;
        protected Object.Type type;
        public Number(Object.Type type, long data) {
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
        public Object.Type getType() {
            return this.type;
        }

        public abstract void parse(ByteBuffer buffer, int offset);
        @Override
        public Object.ObjectType parse(ByteBuffer buffer, int offset, int size) {
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
            super(Object.Type.INT8, data);
        }

        @Override
        public ByteBuffer getBytes() throws Object.InvalidTypeException {
            if (this.data > Byte.MAX_VALUE || this.data < Byte.MIN_VALUE) {
                throw new Object.InvalidTypeException();
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
            super(Object.Type.UINT8, data);
        }

        @Override
        public ByteBuffer getBytes() throws Object.InvalidTypeException {
            if (this.data > 255 || this.data < 0) {
                throw new Object.InvalidTypeException();
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
            super(Object.Type.INT16, data);
        }

        @Override
        public ByteBuffer getBytes() throws Object.InvalidTypeException {
            if (this.data > Short.MAX_VALUE || this.data < Short.MIN_VALUE) {
                throw new Object.InvalidTypeException();
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
            super(Object.Type.UINT16, data);
        }

        @Override
        public ByteBuffer getBytes() throws Object.InvalidTypeException {
            if (this.data > 65535 || this.data < 0) {
                throw new Object.InvalidTypeException();
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
            super(Object.Type.INT32, data);
        }

        @Override
        public ByteBuffer getBytes() throws Object.InvalidTypeException {
            if (this.data > Integer.MAX_VALUE || this.data < Integer.MIN_VALUE) {
                throw new Object.InvalidTypeException();
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
            super(Object.Type.UINT32, data);
        }

        @Override
        public ByteBuffer getBytes() throws Object.InvalidTypeException {
            if (this.data > 4294967295L || this.data < 0) {
                throw new Object.InvalidTypeException();
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
            super(Object.Type.INT64, data);
        }

        @Override
        public ByteBuffer getBytes() throws Object.InvalidTypeException {
            if (this.data > Integer.MAX_VALUE || this.data < Integer.MIN_VALUE) {
                throw new Object.InvalidTypeException();
            }
            return ByteBuffer.allocate(this.getSize()).putLong(this.data);
        }

        @Override
        public void parse(ByteBuffer buffer, int offset) {
            this.setData(buffer.getLong(offset));
        }
    }
}
