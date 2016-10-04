package uk.co.tstableford.rpctests;

import uk.co.tstableford.rpclib.Object;
import uk.co.tstableford.rpclib.ObjectTypes;
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
    }
}
