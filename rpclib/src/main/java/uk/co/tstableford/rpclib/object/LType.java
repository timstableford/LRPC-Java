package uk.co.tstableford.rpclib.object;

public enum LType {
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
    LType(int id, int size) {
        this.size = size;
        this.id = id;
    }

    public int getSize() {
        return this.size;
    }

    public int getId() {
        return this.id;
    }

    public static LType findType(int typeId) {
        for (LType type: values()) {
            if (type.id == typeId) {
                return type;
            }
        }
        return null;
    }
}
