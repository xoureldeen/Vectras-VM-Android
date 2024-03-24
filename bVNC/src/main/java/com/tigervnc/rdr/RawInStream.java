package com.tigervnc.rdr;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class RawInStream extends InStream {
    DataInputStream is;

    public RawInStream(InputStream is) {
        this.is = new DataInputStream(is);
    }

    @Override
    protected int overrun(int itemSize, int nItems, boolean wait) throws IOException {
        return itemSize * nItems;
    }

    public void readBytes(ByteBuffer data, int length) throws IOException {
        is.readFully(data.array(), 0, length);
    }

    public void readBytes(byte[] bytes, int off, int length) throws IOException {
        is.readFully(bytes, off, length);
    }

    public int readByte() throws IOException {
        return is.readByte();
    }

    public int readShort() throws IOException {
        return is.readShort();
    }

    public int readInt() throws IOException {
        return is.readInt();
    }

    public long readLong() throws Exception {
        return is.readLong();
    }

    public int readUnsignedByte() throws IOException {
        return is.readUnsignedByte();
    }

    public int readUnsignedShort() throws IOException {
        return is.readUnsignedShort();
    }

    public int readUnsignedInt() throws IOException {
        return is.readInt();
    }
}
