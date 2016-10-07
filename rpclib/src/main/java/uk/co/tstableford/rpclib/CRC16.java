package uk.co.tstableford.rpclib;

public class CRC16 {

    public static int CRC(final byte[] buffer) {
        int crc = 0xFFFF;
        int polynomial = 0x1021;   // 0001 0000 0010 0001  (0, 5, 12)

        for (byte b : buffer) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b   >> (7-i) & 1) == 1);
                boolean c15 = ((crc >> 15    & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }

        short crc16 = (short)(crc & 0xffff);
        crc16 = Short.reverseBytes(crc16);
        crc = (int) (crc16 & 0xffff);

        crc &= 0xffff;
        return crc;
    }

}