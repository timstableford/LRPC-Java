package uk.co.tstableford.rpc.connectors.pc;

import com.fazecast.jSerialComm.SerialPort;

import uk.co.tstableford.rpc.lib.stream.StreamConnector;

class SerialConnector implements StreamConnector {
    private SerialPort serialPort;
    public SerialConnector(SerialPort port) {
        this.serialPort = port;
    }

    @Override
    public int readData() {
        if (serialPort.isOpen()) {
            byte[] buffer = new byte[1];
            if (serialPort.readBytes(buffer, 1) == 1) {
                return (buffer[0] & 0xff);
            } else {
                return StreamConnector.NO_DATA;
            }
        } else {
            return StreamConnector.ERROR_EXIT;
        }
    }

    @Override
    public int writeData(byte[] data) {
        if (serialPort.isOpen()) {
            return serialPort.writeBytes(data, data.length);
        } else {
            return 0;
        }
    }
}