package uk.co.tstableford.rpctests;

import com.fazecast.jSerialComm.SerialPort;
import uk.co.tstableford.rpc.lib.RPC;
import uk.co.tstableford.rpc.lib.object.LObject;
import uk.co.tstableford.rpc.lib.object.LObjects;
import uk.co.tstableford.rpc.lib.object.LType;
import uk.co.tstableford.rpc.lib.serializer.LSerializer;
import uk.co.tstableford.rpc.lib.stream.StreamConnector;
import uk.co.tstableford.rpc.lib.stream.StreamParser;
import uk.co.tstableford.rpc.connectors.pc.SerialConnector;

import java.util.concurrent.TimeUnit;

public class SerialConnectorPing {
    private static final int PING_FID = 1;
    private static final int PONG_FID = 2;
    private SerialPort serialPort;
    private StreamParser parser;
    private RPC rpc;
    private boolean run;
    private Thread readThread, writeThread;

    public SerialConnectorPing(String portDescriptor) {
        if (portDescriptor == null) {
            SerialPort[] ports = SerialPort.getCommPorts();
            this.serialPort = null;
            for (SerialPort port : ports) {
                System.out.println(port.getSystemPortName());
                if (port.getSystemPortName().contains("ACM") ||
                        port.getSystemPortName().contains("USB") ||
                        port.getSystemPortName().contains("rfcomm")) {
                    this.serialPort = port;
                }
            }
        } else {
            this.serialPort = SerialPort.getCommPort(portDescriptor);
        }

        serialPort.setBaudRate(9600);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 1000, 0);

        SerialConnector serialConnector = new SerialConnector(serialPort);
        this.rpc = new RPC(serialConnector);
        this.rpc.addHandler(new RPC.Handler() {
            @Override
            public boolean onRPC(int functionId, LSerializer object) {
                switch (functionId) {
                    case PING_FID:
                    {
                        LSerializer response = new LSerializer(LObjects.Int(LType.INT64, System.currentTimeMillis()));
                        System.out.println("Received ping");
                        System.out.println(object.toString());
                        try {
                            rpc.call(PONG_FID, response);
                        } catch (LSerializer.InvalidTypeException e) {
                            e.printStackTrace();
                        }
                        return true;
                    }
                    case PONG_FID:
                    {
                        long time = System.currentTimeMillis();
                        System.out.println("Local time is " + time);
                        System.out.println("Time from device is " + object.intAt(0));
                        return true;
                    }
                    default:
                        return false;
                }
            }
        });

        this.parser = new StreamParser(serialConnector, new byte[1024]);
        this.parser.addHandler(RPC.RPC_PACKET_ID, rpc);
    }

    private void start() {
        serialPort.openPort();
        if (!this.run) {
            this.run = true;

            Runnable streamThread = new Runnable() {
                @Override
                public void run() {
                    while (parser.parse() != StreamConnector.ERROR_EXIT && run);
                }
            };
            readThread = new Thread(streamThread);
            readThread.start();

            Runnable pingThread = new Runnable() {
                @Override
                public void run() {
                    while (run) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        System.out.println("Sending ping");
                        LSerializer response = new LSerializer(LObjects.Int(LType.INT64, System.currentTimeMillis()));
                        try {
                            rpc.call(PING_FID, response);
                        } catch (LSerializer.InvalidTypeException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            writeThread = new Thread(pingThread);
            writeThread.start();
        }
    }

    public void stop() {
        this.run = false;
        try {
            readThread.join();
        } catch (InterruptedException e) {
            System.err.println("Failed to join serial read thread.");
        }
        try {
            writeThread.join();
        } catch (InterruptedException e) {
            System.err.println("Failed to join serial ping thread.");
        }
        serialPort.closePort();
    }

    public static void main(String[] args) {
        System.out.println("Running ping pong for 10 seconds.");
        String port = null;
        if (args.length > 0) {
            port = args[0];
        }
        SerialConnectorPing ping = new SerialConnectorPing(port);
        ping.start();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Stopping.");
        ping.stop();
        System.out.println("Stopped.");

    }
}
