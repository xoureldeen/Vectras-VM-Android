package com.tigervnc.rdr;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class RawOutStream extends OutStream {
    DataOutputStream os;

    public RawOutStream(OutputStream os) {
        this.os = new DataOutputStream(os);
    }

    @Override
    public int length() {
        return 0;
    }

    @Override
    protected int overrun(int itemSize, int nItems) {
        return itemSize * nItems;
    }

    @Override
    public void write(int b) throws IOException {
        os.write(b);
    }

    public void write(byte b[]) throws IOException {
        os.write(b, 0, b.length);
    }

    public void write(byte b[], int off, int len) throws IOException {
        os.write(b, off, len);
    }

    public void writeU8(int u) throws IOException {
        os.writeByte(u);
    }

    public void writeU16(int u) throws IOException {
        os.writeShort(u);
    }

    public void writeU32(int u) throws IOException {
        os.writeInt(u);
    }

    public void writeBytes(byte[] data, int dataPtr, int length) throws IOException {
        os.write(data, dataPtr, length);
    }

    @Override
    public void flush() throws IOException {
        os.flush();
    }
}
