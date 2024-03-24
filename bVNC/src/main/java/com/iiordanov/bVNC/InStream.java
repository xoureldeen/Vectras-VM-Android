/**
 * Copyright (C) 2002-2005 RealVNC Ltd.  All Rights Reserved.
 * <p>
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */

//
// rdr::InStream marshalls data from a buffer stored in RDR (RFB Data
// Representation).
//
package com.iiordanov.bVNC;


abstract public class InStream {

    // check() ensures there is buffer data for at least one item of size
    // itemSize bytes.  Returns the number of items in the buffer (up to a
    // maximum of nItems).

    protected byte[] b;
    protected int ptr;

    // readU/SN() methods read unsigned and signed N-bit integers.
    protected int end;

    protected InStream() {
    }

    public final int check(int itemSize, int nItems) throws Exception {
        if (ptr + itemSize * nItems > end) {
            if (ptr + itemSize > end)
                return overrun(itemSize, nItems);

            nItems = (end - ptr) / itemSize;
        }
        return nItems;
    }

    public final void check(int itemSize) throws Exception {
        if (ptr + itemSize > end)
            overrun(itemSize, 1);
    }

    public final int readS8() throws Exception {
        check(1);
        return b[ptr++];
    }

    public final int readS16() throws Exception {
        check(2);
        int b0 = b[ptr++];
        int b1 = b[ptr++] & 0xff;
        return b0 << 8 | b1;
    }

    public final int readS32() throws Exception {
        check(4);
        int b0 = b[ptr++];
        int b1 = b[ptr++] & 0xff;
        int b2 = b[ptr++] & 0xff;
        int b3 = b[ptr++] & 0xff;
        return b0 << 24 | b1 << 16 | b2 << 8 | b3;
    }

    // readBytes() reads an exact number of bytes into an array at an offset.

    public final int readU8() throws Exception {
        return readS8() & 0xff;
    }

    // readOpaqueN() reads a quantity "without byte-swapping".  Because java has
    // no byte-ordering, we just use big-endian.

    public final int readU16() throws Exception {
        return readS16() & 0xffff;
    }

    public final int readU32() throws Exception {
        return readS32() & 0xffffffff;
    }

    public final void skip(int bytes) throws Exception {
        while (bytes > 0) {
            int n = check(1, bytes);
            ptr += n;
            bytes -= n;
        }
    }

    public void readBytes(byte[] data, int offset, int length) throws Exception {
        int offsetEnd = offset + length;
        while (offset < offsetEnd) {
            int n = check(1, offsetEnd - offset);
            System.arraycopy(b, ptr, data, offset, n);
            ptr += n;
            offset += n;
        }
    }

    public final int readOpaque8() throws Exception {
        return readU8();
    }

    // pos() returns the position in the stream.

    public final int readOpaque16() throws Exception {
        return readU16();
    }

    // bytesAvailable() returns true if at least one byte can be read from the
    // stream without blocking.  i.e. if false is returned then readU8() would
    // block.

    public final int readOpaque32() throws Exception {
        return readU32();
    }

    // getbuf(), getptr(), getend() and setptr() are "dirty" methods which allow
    // you to manipulate the buffer directly.  This is useful for a stream which
    // is a wrapper around an underlying stream.

    public final int readOpaque24A() throws Exception {
        check(3);
        int b0 = b[ptr++];
        int b1 = b[ptr++];
        int b2 = b[ptr++];
        return b0 << 24 | b1 << 16 | b2 << 8;
    }

    public final int readOpaque24B() throws Exception {
        check(3);
        int b0 = b[ptr++];
        int b1 = b[ptr++];
        int b2 = b[ptr++];
        return b0 << 16 | b1 << 8 | b2;
    }

    abstract public int pos();

    public boolean bytesAvailable() {
        return end != ptr;
    }

    // overrun() is implemented by a derived class to cope with buffer overrun.
    // It ensures there are at least itemSize bytes of buffer data.  Returns
    // the number of items in the buffer (up to a maximum of nItems).  itemSize
    // is supposed to be "small" (a few bytes).

    public final byte[] getbuf() {
        return b;
    }

    public final int getptr() {
        return ptr;
    }

    public final int getend() {
        return end;
    }

    public final void setptr(int p) {
        ptr = p;
    }

    abstract protected int overrun(int itemSize, int nItems) throws Exception;
}
