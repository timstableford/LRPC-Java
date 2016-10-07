package uk.co.tstableford.rpctests;

import uk.co.tstableford.rpclib.Object;
import uk.co.tstableford.rpclib.ObjectTypes;
import uk.co.tstableford.rpclib.StreamParser;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Test {
    public static void main(String args[]) {
        List<Object.ObjectType> objs = new ArrayList<>();
        objs.add(new ObjectTypes.RPCUInt16(8));
        objs.add(new ObjectTypes.RPCString("Hello world!"));
        objs.add(new ObjectTypes.RPCInt8(10));
        Object obj = new Object(objs);
        try {
            ByteBuffer buffer = obj.serialize();

            Object rebuiltObj = new Object();
            rebuiltObj.unserialize(buffer);
            System.out.println(rebuiltObj.toString());
        } catch (Object.InvalidTypeException e) {
            e.printStackTrace();
        }

        testStreamParser();
    }

    public static void testStreamParser() {
        final byte testCallBuffer[] = { 0x0, 0x8, 0x0, 0x19, 0x79, (byte) 0xae, 0x5, 0x5, 0x2, 0x3, 0x4, 0x1, 0xc, 0x0, 0xa, (byte) 0xf6, 0xa, 0x1, 0x40, 0x68, 0x65, 0x6c, 0x6c, 0x6f, 0x20, 0x77, 0x6f, 0x72, 0x6c, 0x64, 0x0 };

        StreamParser.StreamConnector connector = new StreamParser.StreamConnector() {
            int testCallIndex = 0;
            @Override
            public int readData() {
                if (testCallIndex >= testCallBuffer.length) {
                    return -1;
                }
                int b = testCallBuffer[testCallIndex] & 0xff;
                testCallIndex++;
                return b;
            }

            @Override
            public int writeData(byte[] data) {
                return 0;
            }
        };

        StreamParser parser = new StreamParser(connector, new byte[1024]);
        parser.addHandler(8, new StreamParser.StreamHandler() {
            @Override
            public void onPacket(int type, int size, ByteBuffer buffer) {
                System.out.println("Type received  - " + type);
            }
        });

        while (parser.parse() >= 0);
    }
}
