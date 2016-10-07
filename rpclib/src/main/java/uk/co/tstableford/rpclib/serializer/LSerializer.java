package uk.co.tstableford.rpclib.serializer;

import uk.co.tstableford.rpclib.object.LObject;
import uk.co.tstableford.rpclib.object.LObjects;
import uk.co.tstableford.rpclib.object.LType;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LSerializer {
    private List<LObject> data;

    public LSerializer(List<LObject> data) {
        this.data = data;
    }

    public LSerializer(LObject... objects) {
        this.data = Arrays.asList(objects);
    }

    public LSerializer() {
        this(new ArrayList<LObject>());
    }

    public void setData(List<LObject> data) {
        this.data = data;
    }

    private int getStringOffset(int index) {
        int count = 0;
        for (int i = 0; i < index && i < this.data.size(); i++) {
            if (this.data.get(i).getType() == LType.STRING) {
                count++;
            }
        }

        return count;
    }

    public List<LObject> getData() {
        return this.data;
    }

    public int getDataSize() {
        int size = 0;
        for (LObject obj: this.data) {
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
        // Get the number of objects in the buffer.
        buffer.position(0);
        LObjects.RPCUInt8 size = new LObjects.RPCUInt8(0);
        size.parse(buffer, 0);
        this.data = new ArrayList<>((int)size.getData());

        // For each object calculate how many bytes it uses and count the number of strings in the string table.
        int numStrings = 0;
        int sizes[] = new int[(int) size.getData()];
        for (int i = 0; i < size.getData(); i++) {
            LObjects.RPCUInt8 typeId = new LObjects.RPCUInt8(0);
            typeId.parse(buffer, i + 1);
            LType type = LType.findType((int) typeId.getData());
            if (type != null) {
                sizes[i] = type.getSize();
                if (type == LType.STRING) {
                    LObjects.RPCUInt8 stringSize = new LObjects.RPCUInt8(0);
                    stringSize.parse(buffer, (int) (1 + size.getData() + numStrings));
                    sizes[i] = (int) stringSize.getData();
                    numStrings++;
                }
            }
        }

        // Set to the start of the data section.
        int dataOffset = (int) (1 + size.getData() + numStrings);

        // Parse each object in the buffer.
        for (int i = 0; i < size.getData(); i++) {
            LObjects.RPCUInt8 typeId = new LObjects.RPCUInt8(0);
            typeId.parse(buffer, 1 + i);
            LType type = LType.findType((int)typeId.getData());

            if (type == null) {
                throw new InvalidTypeException("Unknown type - " + typeId.getData());
            }
            switch (type) {
                case STRING:
                    this.data.add(i, LObjects.String(null).parse(buffer, dataOffset, sizes[i]));
                    break;
                case INT8:
                case UINT8:
                case INT16:
                case UINT16:
                case INT32:
                case UINT32:
                case INT64:
                    this.data.add(LObjects.Int(type, 0).parse(buffer, dataOffset, sizes[i]));
                    break;
                case FLOAT:
                    this.data.add(i, LObjects.Float(0).parse(buffer, dataOffset, sizes[i]));
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

        // Allocate a buffer large enough to store the object.
        int dataSize = LType.UINT8.getSize(); // Number of items.
        dataSize += this.data.size() * LType.UINT8.getSize(); // One byte per item for identifier.
        dataSize += this.getStringOffset(this.data.size()) * LType.STRING.getSize(); // Get the number of strings. Multiple by the number of bytes used for string size.
        dataSize += this.getDataSize();
        ByteBuffer buffer = ByteBuffer.allocate(dataSize);

        // Put the number of items into the buffer.
        putObject(buffer, LObjects.Int(LType.UINT8, this.data.size()));

        // Put the object identifiers for each object into the buffer.
        for (LObject object: this.data) {
            putObject(buffer, LObjects.Int(LType.UINT8, object.getType().getId()));
        }

        // Put the length of each string into the buffer.
        for (LObject object: this.data) {
            if (object.getType() == LType.STRING) {
                putObject(buffer, LObjects.Int(LType.UINT8, object.getSize()));
            }
        }

        // Put the objects themselves into the buffer.
        for (LObject object: this.data) {
            putObject(buffer, object);
        }

        return buffer;
    }

    private void putObject(ByteBuffer buffer, LObject object) throws InvalidTypeException {
        buffer.put(object.getBytes().array(), 0, object.getSize());
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

    public static class InvalidTypeException extends Exception {
        public InvalidTypeException(String message) {
            super(message);
        }

        public InvalidTypeException() {
            super();
        }
    }
}
