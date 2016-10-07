package uk.co.tstableford.rpclib.stream;

public interface StreamConnector {
    int readData();
    int writeData(byte data[]);
}