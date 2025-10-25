package au.com.darkside.xserver;

import java.io.IOException;


/**
 * This class stores details of a pixmap format.
 *
 * @author Matthew Kwan
 */
public class Format {
    private final byte _depth;
    private final byte _bitsPerPixel;
    private final byte _scanlinePad;

    /**
     * Constructor.
     *
     * @param depth        The depth in bits.
     * @param bitsPerPixel Number of bits per pixel.
     * @param scanlinePad  Number of bits to pad each scan line.
     */
    public Format(byte depth, byte bitsPerPixel, byte scanlinePad) {
        _depth = depth;
        _bitsPerPixel = bitsPerPixel;
        _scanlinePad = scanlinePad;
    }

    /**
     * Write details of the format.
     *
     * @param io The input/output stream.
     * @throws IOException
     */
    public void write(InputOutput io) throws IOException {
        io.writeByte(_depth);        // Depth.
        io.writeByte(_bitsPerPixel);    // Bits per pixel.
        io.writeByte(_scanlinePad);    // Scanline pad.
        io.writePadBytes(5);    // Unused.
    }
}