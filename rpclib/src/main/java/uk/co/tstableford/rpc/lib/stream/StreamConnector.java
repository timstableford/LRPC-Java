package uk.co.tstableford.rpc.lib.stream;

public interface StreamConnector {
    int readData();
    int writeData(byte data[]);
}