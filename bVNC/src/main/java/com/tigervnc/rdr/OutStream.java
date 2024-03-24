/* Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
 * Copyright (C) 2011-2019 Brian P. Hinz
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 * USA.
 */

//
// rdr::OutStream marshalls data into a buffer stored in RDR (RFB Data
// Representation).
//

package com.tigervnc.rdr;

import java.io.IOException;

abstract public class OutStream {

    // check() ensures there is buffer space for at least one item of size
    // itemSize bytes.  Returns the number of items which fit (up to a maximum
    // of nItems).

    static final int maxMessageSize = 8192;
    protected byte[] b = new byte[maxMessageSize];

    // writeU/SN() methods write unsigned and signed N-bit integers.
    protected int ptr;
    protected int end;

    protected OutStream() {
    }

    public final int check(int itemSize, int nItems) throws IOException {
        int nAvail;

        if (itemSize > (end - ptr)) {
            return overrun(itemSize, nItems);
        }

        nAvail = (end - ptr) / itemSize;
        return Math.min(nAvail, nItems);
    }

    // writeBytes() writes an exact number of bytes from an array at an offset.

    public final void check(int itemSize) throws IOException {
        if (ptr + itemSize > end)
            overrun(itemSize, 1);
    }

    // length() returns the length of the stream.

    public void writeU8(int u) throws IOException {
        check(1);
        b[ptr++] = (byte) u;
        flush();
    }

    // flush() requests that the stream be flushed.

    public void writeU16(int u) throws IOException {
        check(2);
        b[ptr++] = (byte) (u >> 8);
        b[ptr++] = (byte) u;
        flush();
    }

    public void writeU32(int u) throws IOException {
        check(4);
        b[ptr++] = (byte) (u >> 24);
        b[ptr++] = (byte) (u >> 16);
        b[ptr++] = (byte) (u >> 8);
        b[ptr++] = (byte) u;
        flush();
    }

    public final void pad(int bytes) throws IOException {
        while (bytes-- > 0) writeU8(0);
    }

    public void writeBytes(byte[] data, int dataPtr, int length) throws IOException {
        int dataEnd = dataPtr + length;
        while (dataPtr < dataEnd) {
            int n = check(1, dataEnd - dataPtr);
            System.arraycopy(data, dataPtr, b, ptr, n);
            ptr += n;
            dataPtr += n;
        }
        flush();
    }

    abstract public int length();

    public void flush() throws IOException {
    }

    abstract protected int overrun(int itemSize, int nItems) throws IOException;

    public void write(byte b[]) throws IOException {
        this.writeBytes(b, 0, b.length);
    }

    public void write(byte b[], int off, int len) throws IOException {
        this.writeBytes(b, off, len);
    }

    public void write(int i) throws IOException {
        this.writeU32(i);
    }
}
