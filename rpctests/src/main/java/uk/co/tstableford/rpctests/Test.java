package uk.co.tstableford.rpctests;

import uk.co.tstableford.rpc.lib.RPC;
import uk.co.tstableford.rpc.lib.object.LObjects;
import uk.co.tstableford.rpc.lib.object.LType;
import uk.co.tstableford.rpc.lib.serializer.LSerializer;
import uk.co.tstableford.rpc.lib.stream.StreamConnector;
import uk.co.tstableford.rpc.lib.stream.StreamParser;

import java.nio.ByteBuffer;

public class Test {
    public static void main(String args[]) {
        SerialConnectorPing serialConnectorPing = new SerialConnectorPing();

        testBasicObject();
        System.out.println();

        testStreamParser();
        System.out.println();
    }

    public static void testBasicObject() {
        LSerializer obj = new LSerializer(
                LObjects.Int(LType.UINT16, 8),
                LObjects.String("Hello world!"),
                LObjects.Int(LType.INT8, 10));
        try {
            ByteBuffer buffer = obj.serialize();

            LSerializer rebuiltObj = new LSerializer(buffer);
            System.out.println(rebuiltObj.toString());

            System.out.println("testBasicObject PASS = " + obj.equals(rebuiltObj));
        } catch (LSerializer.InvalidTypeException e) {
            e.printStackTrace();
        }
    }

    public static void testStreamParser() {
        final byte testCallBuffer[] = { 0x0, 0x8, 0x0, 0x19, 0x79, (byte) 0xae, 0x5, 0x5, 0x2, 0x3, 0x4, 0x1, 0xc, 0x0, 0xa, (byte) 0xf6, 0xa, 0x1, 0x40, 0x68, 0x65, 0x6c, 0x6c, 0x6f, 0x20, 0x77, 0x6f, 0x72, 0x6c, 0x64, 0x0 };

        StreamConnector connector = new StreamConnector() {
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

        RPC rpc = new RPC();
        rpc.addHandler(10, new RPC.Handler() {
            @Override
            public void onRPC(LSerializer obj) {
                System.out.println("RPC Callback received.");
                System.out.println(obj.toString());
                boolean pass = LObjects.Int(LType.UINT16, 10).equals(obj.getData().get(0)) &&
                        LObjects.Int(LType.INT8, 246).equals(obj.getData().get(1)) &&
                        LObjects.Int(LType.UINT8, 10).equals(obj.getData().get(2)) &&
                        LObjects.Int(LType.INT16, 320).equals(obj.getData().get(3)) &&
                        LObjects.String("hello world").equals(obj.getData().get(4));
                System.out.println("Stream parser static buffer test PASS = " + pass);
            }
        });

        StreamParser parser = new StreamParser(connector, new byte[1024]);
        parser.addHandler(RPC.RPC_PACKET_ID, rpc);

        while (parser.parse() >= 0);
    }
}
