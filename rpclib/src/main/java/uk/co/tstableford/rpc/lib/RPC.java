package uk.co.tstableford.rpc.lib;

import uk.co.tstableford.rpc.lib.object.LObject;
import uk.co.tstableford.rpc.lib.object.LObjects;
import uk.co.tstableford.rpc.lib.object.LType;
import uk.co.tstableford.rpc.lib.stream.StreamConnector;
import uk.co.tstableford.rpc.lib.stream.StreamParser;
import uk.co.tstableford.rpc.lib.serializer.LSerializer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RPC implements StreamParser.StreamHandler {
    public static final int RPC_PACKET_ID = 8;
    private HashMap<Integer, Handler> handlers;
    private StreamConnector connector;

    public RPC(StreamConnector connector) {
        this.connector = connector;
        this.handlers = new HashMap<>();
    }

    public void removeHandler(int functionId) {
        if (this.handlers.containsKey(functionId)) {
            this.handlers.remove(functionId);
        }
    }

    public synchronized void call(int functionId, LSerializer obj) throws LSerializer.InvalidTypeException {
        List<LObject> data = new ArrayList<>(obj.getData());
        data.add(0, LObjects.Int(LType.UINT16, functionId));
        LSerializer send = new LSerializer(data);
        connector.writeData(StreamParser.WrapBuffer(RPC.RPC_PACKET_ID, send.serialize()));
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
                    // Remove function ID
                    object.getData().remove(0);
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
