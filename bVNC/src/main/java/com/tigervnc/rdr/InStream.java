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
// rdr::InStream marshalls data from a buffer stored in RDR (RFB Data
// Representation).
//

package com.tigervnc.rdr;

import java.io.IOException;
import java.nio.ByteBuffer;

abstract public class InStream {

    // check() ensures there is buffer data for at least one item of size
    // itemSize bytes.  Returns the number of items in the buffer (up to a
    // maximum of nItems).

    protected byte[] b;
    protected int ptr;
    protected int end;

    protected InStream() {
    }

    public int check(int itemSize, int nItems, boolean wait) throws IOException {
        int nAvail;

        if (itemSize > (end - ptr)) {
            return overrun(itemSize, nItems, wait);
        }

        nAvail = (end - ptr) / itemSize;
        return Math.min(nAvail, nItems);
    }

    public int check(int itemSize, int nItems) throws IOException {
        return check(itemSize, nItems, true);
    }

    public int check(int itemSize) throws IOException {
        return check(itemSize, 1);
    }

    public int readByte() throws IOException {
        check(1);
        return b[ptr++];
    }

    public int readShort() throws IOException {
        check(2);
        int b0 = b[ptr++];
        int b1 = b[ptr++] & 0xff;
        return b0 << 8 | b1;
    }

    public int readInt() throws IOException {
        check(4);
        int b0 = b[ptr++];
        int b1 = b[ptr++] & 0xff;
        int b2 = b[ptr++] & 0xff;
        int b3 = b[ptr++] & 0xff;
        return b0 << 24 | b1 << 16 | b2 << 8 | b3;
    }

    // readBytes() reads an exact number of bytes

    public long readLong() throws Exception {
        check(8);
        long b0 = b[ptr++];
        long b1 = b[ptr++] & 0xff;
        long b2 = b[ptr++] & 0xff;
        long b3 = b[ptr++] & 0xff;
        long b4 = b[ptr++] & 0xff;
        long b5 = b[ptr++] & 0xff;
        long b6 = b[ptr++] & 0xff;
        long b7 = b[ptr++] & 0xff;
        return b0 << 56 | b1 << 48 | b2 << 40 | b3 << 32 | b4 << 24 | b5 << 16 | b6 << 8 | b7;
    }

    public int readUnsignedByte() throws IOException {
        return readByte() & 0xff;
    }

    public int readUnsignedShort() throws IOException {
        return readShort() & 0xffff;
    }

    public int readUnsignedInt() throws IOException {
        return readInt() & 0xffffffff;
    }

    // overrun() is implemented by a derived class to cope with buffer overrun.
    // It ensures there are at least itemSize bytes of buffer data.  Returns
    // the number of items in the buffer (up to a maximum of nItems).  itemSize
    // is supposed to be "small" (a few bytes).

    public void readBytes(ByteBuffer data, int length) throws IOException {
        while (length > 0) {
            int n = check(1, length);
            data.put(b, ptr, n);
            ptr += n;
            length -= n;
        }
    }

    public void readBytes(byte[] bytes, int off, int length) throws IOException {
        readBytes(ByteBuffer.wrap(bytes, off, length), length);
    }

    public int getptr() {
        return ptr;
    }

    public int getend() {
        return end;
    }

    abstract protected int overrun(int itemSize, int nItems, boolean wait) throws IOException;
}
