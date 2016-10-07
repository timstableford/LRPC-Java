package uk.co.tstableford.rpctests;

import uk.co.tstableford.rpclib.RPCObjectSerializer;
import uk.co.tstableford.rpclib.RPCObjects;
import uk.co.tstableford.rpclib.RPCType;
import uk.co.tstableford.rpclib.StreamParser;

import java.nio.ByteBuffer;

public class Test {
    public static void main(String args[]) {
        RPCObjectSerializer obj = new RPCObjectSerializer(
                RPCObjects.Int(RPCType.UINT16, 8),
                RPCObjects.String("Hello world!"),
                RPCObjects.Int(RPCType.INT8, 10));
        try {
            ByteBuffer buffer = obj.serialize();

            RPCObjectSerializer rebuiltObj = new RPCObjectSerializer();
            rebuiltObj.unserialize(buffer);
            System.out.println(rebuiltObj.toString());
        } catch (RPCObjectSerializer.InvalidTypeException e) {
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
                RPCObjectSerializer obj = new RPCObjectSerializer();
                try {
                    obj.unserialize(buffer);
                } catch (RPCObjectSerializer.InvalidTypeException e) {
                    e.printStackTrace();
                }
                System.out.println("Type received  - " + type);
            }
        });

        while (parser.parse() >= 0);
    }
}
