package uk.co.tstableford.rpctests;

import com.fazecast.jSerialComm.SerialPort;
import uk.co.tstableford.rpc.lib.stream.StreamParser;
import uk.co.tstableford.rpc.connectors.pc.SerialConnector;

public class SerialConnectorPing {
    private SerialPort serialPort;

    public SerialConnectorPing() {
        SerialPort[] ports = SerialPort.getCommPorts();
        serialPort = null;
        for (SerialPort port: ports) {
            System.out.println(port.getSystemPortName());
            if (port.getSystemPortName().contains("ACM") ||
                    port.getSystemPortName().contains("USB")) {
                serialPort = port;
                break;
            }
        }

        if (serialPort == null) {
            System.err.println("Serial port not found!");
        } else {
            serialPort.setBaudRate(115200);
            serialPort.openPort();
        }

        SerialConnector serialConnector = new SerialConnector(serialPort);
    }
}
