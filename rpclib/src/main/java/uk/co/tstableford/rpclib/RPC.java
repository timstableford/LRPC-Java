package uk.co.tstableford.rpclib;

import uk.co.tstableford.rpclib.serializer.LSerializer;
import uk.co.tstableford.rpclib.stream.StreamParser;

import java.nio.ByteBuffer;
import java.util.HashMap;

public class RPC implements StreamParser.StreamHandler {
    public static final int RPC_PACKET_ID = 8;
    private HashMap<Integer, Handler> handlers;

    public RPC() {
        this.handlers = new HashMap<>();
    }

    public void removeHandler(int functionId) {
        if (this.handlers.containsKey(functionId)) {
            this.handlers.remove(functionId);
        }
    }

    public void addHandler(int functionId, Handler handler) {
        this.handlers.put(functionId, handler);
    }

    @Override
    public void onPacket(int type, int size, ByteBuffer buffer) {
        if (type == RPC_PACKET_ID) {
            try {
                LSerializer object = new LSerializer(buffer);
                int functionId = (int) object.intAt(0).getData();
                if (this.handlers.containsKey(functionId)) {
                    this.handlers.get(functionId).onRPC(object);
                }
            } catch (LSerializer.InvalidTypeException e) {
                System.err.println("Failed to de-serialize object [" + buffer.toString() + "]");
            }
        } else {
            System.err.println("RPC given wrong packet type.");
        }
    }

    public interface Handler {
        void onRPC(LSerializer object);
    }
}
