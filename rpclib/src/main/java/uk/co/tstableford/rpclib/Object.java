package uk.co.tstableford.rpclib;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Object {
    private List<ObjectType> data;

    public Object(List<ObjectType> data) {
        this.data = data;
    }

    public Object() {
        this(new ArrayList<ObjectType>());
    }

    public void setData(List<ObjectType> data) {
        this.data = data;
    }

    private int getStringOffset(int index) {
        int count = 0;
        for (int i = 0; i < index && i < this.data.size(); i++) {
            if (this.data.get(i).getType() == Type.STRING) {
                count++;
            }
        }

        return count;
    }

    public List<ObjectType> getData() {
        return this.data;
    }

    public int getDataSize() {
        int size = 0;
        for (ObjectType obj: this.data) {
            size += obj.getSize();
        }
        return size;
    }

    private int getDataIndexOf(int index) {
        int dataIndex = 0;

        if(index > this.data.size()) {
            return 0;
        }

        for(int i = 0; i < index && i < this.data.size(); i++) {
            dataIndex += this.data.get(i).getSize();
        }

        return dataIndex;
    }

    public void unserialize(ByteBuffer buffer) throws InvalidTypeException {
        buffer.position(0);
        ObjectTypes.RPCUInt8 size = new ObjectTypes.RPCUInt8(0);
        size.parse(buffer, 0);
        this.data = new ArrayList<>((int)size.getData());

        int numStrings = 0;
        int sizes[] = new int[(int) size.getData()];
        for (int i = 0; i < size.getData(); i++) {
            ObjectTypes.RPCUInt8 typeId = new ObjectTypes.RPCUInt8(0);
            typeId.parse(buffer, i + 1);
            Type type = Type.findType((int) typeId.getData());
            if (type != null) {
                sizes[i] = type.getSize();
                if (type == Type.STRING) {
                    ObjectTypes.RPCUInt8 stringSize = new ObjectTypes.RPCUInt8(0);
                    stringSize.parse(buffer, (int) (1 + size.getData() + numStrings));
                    sizes[i] = (int) stringSize.getData();
                    numStrings++;
                }
            }
        }

        // Set to the start of the data section.
        int dataOffset = (int) (1 + size.getData() + numStrings);
        for (int i = 0; i < size.getData(); i++) {
            ObjectTypes.RPCUInt8 typeId = new ObjectTypes.RPCUInt8(0);
            typeId.parse(buffer, 1 + i);
            Type type = Type.findType((int)typeId.getData());

            if (type == null) {
                throw new InvalidTypeException("Unknown type - " + typeId.getData());
            }
            switch (type) {
                case STRING:
                    this.data.add(i, new ObjectTypes.RPCString(buffer, dataOffset, sizes[i]));
                    break;
                case INT8:
                    this.data.add(i, new ObjectTypes.RPCInt8(0).parse(buffer, dataOffset, sizes[i]));
                    break;
                case UINT8:
                    this.data.add(i, new ObjectTypes.RPCUInt8(0).parse(buffer, dataOffset, sizes[i]));
                    break;
                case INT16:
                    this.data.add(i, new ObjectTypes.RPCInt16(0).parse(buffer, dataOffset, sizes[i]));
                    break;
                case UINT16:
                    this.data.add(i, new ObjectTypes.RPCUInt16(0).parse(buffer, dataOffset, sizes[i]));
                    break;
                case INT32:
                    this.data.add(i, new ObjectTypes.RPCInt32(0).parse(buffer, dataOffset, sizes[i]));
                    break;
                case UINT32:
                    this.data.add(i, new ObjectTypes.RPCUInt32(0).parse(buffer, dataOffset, sizes[i]));
                    break;
                case INT64:
                    this.data.add(i, new ObjectTypes.RPCInt64(0).parse(buffer, dataOffset, sizes[i]));
                    break;
                case FLOAT:
                    this.data.add(i, new ObjectTypes.RPCFloat(0).parse(buffer, dataOffset, sizes[i]));
                default:
                    throw new InvalidTypeException("Unsupported type - " + type.toString());
            }
            dataOffset += this.data.get(i).getSize();
        }
    }

    public ByteBuffer serialize() throws InvalidTypeException {
        if (this.data.size() > 255) {
            throw new InvalidTypeException("Too many data items!");
        }

        int dataSize = Type.UINT8.getSize(); // Number of items.
        dataSize += this.data.size() * Type.UINT8.getSize(); // One byte per item for identifier.
        dataSize += this.getStringOffset(this.data.size()) * Type.STRING.getSize(); // Get the number of strings. Multiple by the number of bytes used for string size.
        dataSize += this.getDataSize();

        ByteBuffer buffer = ByteBuffer.allocate(dataSize);

        buffer.put(new ObjectTypes.RPCUInt8(this.data.size()).getBytes().array(), 0, Type.UINT8.getSize());
        for (ObjectType object: this.data) {
            buffer.put(new ObjectTypes.RPCUInt8(object.getType().getId()).getBytes().array(), 0, Type.UINT8.getSize());
        }
        for (ObjectType object: this.data) {
            if (object.getType() == Type.STRING) {
                buffer.put(new ObjectTypes.RPCUInt8(object.getSize()).getBytes().array(), 0, Type.UINT8.getSize());
            }
        }
        for (ObjectType object: this.data) {
            buffer.put(object.getBytes().array(), 0, object.getSize());
        }

        buffer.position(0);

        return buffer;
    }

    public enum Type {
        STRING(0x01, 0x01),
        INT8(0x02, 0x01),
        UINT8(0x03, 0x01),
        INT16(0x04, 0x02),
        UINT16(0x05, 0x02),
        INT32(0x06, 0x04),
        UINT32(0x07, 0x04),
        INT64(0x08, 0x08),
        UINT64(0x09, 0x08),
        FLOAT(0x0c, 0x04);

        private int size, id;
        Type(int id, int size) {
            this.size = size;
            this.id = id;
        }

        public int getSize() {
            return this.size;
        }

        public int getId() {
            return this.id;
        }

        public static Type findType(int typeId) {
            for (Type type: values()) {
                if (type.id == typeId) {
                    return type;
                }
            }
            return null;
        }
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < this.data.size(); i++) {
            output.append("[").append(i).append("](")
                    .append(this.data.get(i).getType().toString())
                    .append(") ")
                    .append(this.data.get(i).toString())
                    .append("\n");
        }
        return output.toString();
    }

    public interface ObjectType {
        int getSize();
        Type getType();
        ByteBuffer getBytes() throws InvalidTypeException;
        ObjectType parse(ByteBuffer buffer, int offset, int size);
    }

    public static class InvalidTypeException extends Exception {
        public InvalidTypeException(String message) {
            super(message);
        }

        public InvalidTypeException() {
            super();
        }
    }
}
