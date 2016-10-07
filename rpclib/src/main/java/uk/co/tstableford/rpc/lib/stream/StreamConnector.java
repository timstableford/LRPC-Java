package uk.co.tstableford.rpc.lib.stream;

public interface StreamConnector {
    int NO_DATA = -1;
    int ERROR_EXIT = -20;

    int readData();
    int writeData(byte data[]);
}