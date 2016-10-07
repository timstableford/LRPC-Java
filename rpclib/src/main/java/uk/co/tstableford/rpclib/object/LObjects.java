package uk.co.tstableford.rpclib.object;

import uk.co.tstableford.rpclib.serializer.LSerializer;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class LObjects {
    public static class LString implements LObject {
        private String data;

        public LString(String data) {
            this.data = data;
        }

        public LString(ByteBuffer buffer, int offset, int size) {
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
        public LType getType() {
            return LType.STRING;
        }

        @Override
        public ByteBuffer getBytes() throws LSerializer.InvalidTypeException {
            if (this.data.length() > 255) {
                throw new LSerializer.InvalidTypeException();
            }
            ByteBuffer buffer = ByteBuffer.allocate(this.getSize() + 1);
            buffer.put(this.data.getBytes(Charset.forName("UTF-8")));
            buffer.put((byte) 0x00);
            return buffer;
        }

        @Override
        public LString parse(ByteBuffer buffer, int offset, int size) {
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

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof LString)) {
                return false;
            }
            LString other = (LString) obj;
            if (this.data == null || other.data == null) {
                return false;
            }
            if (!this.data.equals(other.data)) {
                return false;
            }
            return true;
        }
    }

    public static class LFloat implements LObject {
        protected float data;
        public LFloat(float data) {
            this.data = data;
        }

        public LFloat(ByteBuffer buffer, int offset, int size) {
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
        public LType getType() {
            return LType.FLOAT;
        }

        @Override
        public ByteBuffer getBytes() throws LSerializer.InvalidTypeException {
            ByteBuffer buffer = ByteBuffer.allocate(this.getSize());
            return buffer.putFloat(this.data);
        }

        @Override
        public LFloat parse(ByteBuffer buffer, int offset, int size) {
            this.data = buffer.getFloat(offset);
            return this;
        }

        @Override
        public String toString() {
            return java.lang.Float.toString(this.data);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof LFloat)) {
                return false;
            }
            LFloat other = (LFloat) obj;
            if (this.data != other.data) {
                return false;
            }
            return true;
        }
    }

    public static abstract class LNumber implements LObject {
        protected long data;
        protected LType type;
        public LNumber(LType type, long data) {
            this.data = data;
            this.type = type;
        }

        public LNumber(ByteBuffer buffer, int offset) {
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
        public LType getType() {
            return this.type;
        }

        public abstract void parse(ByteBuffer buffer, int offset);
        @Override
        public LNumber parse(ByteBuffer buffer, int offset, int size) {
            this.parse(buffer, offset);
            return this;
        }

        @Override
        public String toString() {
            return Long.toString(this.data);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof LNumber)) {
                return false;
            }
            LNumber other = (LNumber) obj;
            if (this.data != other.data) {
                return false;
            }
            if (this.type != other.type) {
                return false;
            }
            return true;
        }
    }

    public static class LInt8 extends LNumber {
        public LInt8(long data) {
            super(LType.INT8, data);
        }

        @Override
        public ByteBuffer getBytes() throws LSerializer.InvalidTypeException {
            if (this.data > Byte.MAX_VALUE || this.data < Byte.MIN_VALUE) {
                throw new LSerializer.InvalidTypeException();
            }
            return ByteBuffer.allocate(this.getSize()).put((byte)(this.data & 0xff));
        }

        @Override
        public void parse(ByteBuffer buffer, int offset) {
            this.setData(buffer.get(offset) & (long) 0xff);
        }
    }

    public static class LUInt8 extends LNumber {
        public LUInt8(long data) {
            super(LType.UINT8, data);
        }

        @Override
        public ByteBuffer getBytes() throws LSerializer.InvalidTypeException {
            if (this.data > 255 || this.data < 0) {
                throw new LSerializer.InvalidTypeException();
            }
            return ByteBuffer.allocate(this.getSize()).put((byte)(this.data & 0xff));
        }

        @Override
        public void parse(ByteBuffer buffer, int offset) {
            this.setData(buffer.get(offset) & (long) 0xff);
        }
    }

    public static class LInt16 extends LNumber {
        public LInt16(long data) {
            super(LType.INT16, data);
        }

        @Override
        public ByteBuffer getBytes() throws LSerializer.InvalidTypeException {
            if (this.data > Short.MAX_VALUE || this.data < Short.MIN_VALUE) {
                throw new LSerializer.InvalidTypeException();
            }
            return ByteBuffer.allocate(this.getSize()).putShort((short)this.data);
        }

        @Override
        public void parse(ByteBuffer buffer, int offset) {
            this.setData(buffer.getShort(offset));
        }
    }

    public static class LUInt16 extends LNumber {
        public LUInt16(long data) {
            super(LType.UINT16, data);
        }

        @Override
        public ByteBuffer getBytes() throws LSerializer.InvalidTypeException {
            if (this.data > 65535 || this.data < 0) {
                throw new LSerializer.InvalidTypeException();
            }
            return ByteBuffer.allocate(this.getSize()).putShort((short) (this.data & 0xffff));
        }

        @Override
        public void parse(ByteBuffer buffer, int offset) {
            this.setData((long) (buffer.getShort(offset) & 0xffff));
        }
    }

    public static class LInt32 extends LNumber {
        public LInt32(long data) {
            super(LType.INT32, data);
        }

        @Override
        public ByteBuffer getBytes() throws LSerializer.InvalidTypeException {
            if (this.data > Integer.MAX_VALUE || this.data < Integer.MIN_VALUE) {
                throw new LSerializer.InvalidTypeException();
            }
            return ByteBuffer.allocate(this.getSize()).putInt((int)this.data);
        }

        @Override
        public void parse(ByteBuffer buffer, int offset) {
            this.setData(buffer.getInt(offset));
        }
    }

    public static class LUInt32 extends LNumber {
        public LUInt32(long data) {
            super(LType.UINT32, data);
        }

        @Override
        public ByteBuffer getBytes() throws LSerializer.InvalidTypeException {
            if (this.data > 4294967295L || this.data < 0) {
                throw new LSerializer.InvalidTypeException();
            }
            return ByteBuffer.allocate(this.getSize()).putInt((int) (this.data & 0xffffffffL));
        }

        @Override
        public void parse(ByteBuffer buffer, int offset) {
            this.setData((buffer.getInt(offset) & 0xffffffffL));
        }
    }

    public static class LInt64 extends LNumber {
        public LInt64(long data) {
            super(LType.INT64, data);
        }

        @Override
        public ByteBuffer getBytes() throws LSerializer.InvalidTypeException {
            if (this.data > Integer.MAX_VALUE || this.data < Integer.MIN_VALUE) {
                throw new LSerializer.InvalidTypeException();
            }
            return ByteBuffer.allocate(this.getSize()).putLong(this.data);
        }

        @Override
        public void parse(ByteBuffer buffer, int offset) {
            this.setData(buffer.getLong(offset));
        }
    }

    public static LNumber Int(LType type, long number) {
        switch (type) {
            case INT8:
                return new LInt8(number);
            case UINT8:
                return new LUInt8(number);
            case INT16:
                return new LInt16(number);
            case UINT16:
                return new LUInt16(number);
            case INT32:
                return new LInt32(number);
            case UINT32:
                return new LUInt32(number);
            case INT64:
                return new LInt64(number);
            default:
                return null;
        }
    }

    public static LNumber Int(LType type, ByteBuffer buffer, int offset) {
        LNumber num = Int(type, 0);
        return num.parse(buffer, offset, num.getSize());
    }

    public static LFloat Float(float number) {
        return new LFloat(number);
    }

    public static LFloat Float(ByteBuffer buffer, int offset) {
        LFloat num = Float(0);
        return num.parse(buffer, offset, num.getSize());
    }

    public static LString String(String value) {
        return new LString(value);
    }

    public static LString String(ByteBuffer buffer, int offset, int length) {
        return String(null).parse(buffer, offset, length);
    }
}
