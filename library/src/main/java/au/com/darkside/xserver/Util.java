package au.com.darkside.xserver;

import java.io.IOException;

/**
 * Utility functions.
 *
 * @author Matthew Kwan
 */
public class Util {
    /**
     * Count the number of bits in an integer.
     *
     * @param n The integer containing the bits.
     * @return The number of bits in the integer.
     */
    public static int bitcount(int n) {
        int c = 0;

        while (n != 0) {
            c += n & 1;
            n >>= 1;
        }

        return c;
    }

    /**
     * Write the header of a reply.
     *
     * @param client The remote client.
     * @param arg    Optional argument.
     * @throws IOException
     */
    public static void writeReplyHeader(Client client, byte arg) throws IOException {
        InputOutput io = client.getInputOutput();
        short sn = (short) (client.getSequenceNumber() & 0xffff);

        io.writeByte((byte) 1);    // Reply.
        io.writeByte(arg);
        io.writeShort(sn);
    }
}