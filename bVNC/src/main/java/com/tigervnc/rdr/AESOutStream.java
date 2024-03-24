/* Copyright (C) 2022 Dinglan Peng
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

package com.tigervnc.rdr;

import java.io.IOException;

public class AESOutStream extends OutStream {

    private final AESCipher cipher;
    private final int start;
    private final int bufSize;
    private final byte[] buffer;
    private final byte[] counter;
    private final OutStream out;
    private int offset;
    public AESOutStream(OutStream _out, byte[] key) throws Exception {
        out = _out;
        bufSize = maxMessageSize;
        b = new byte[bufSize];
        buffer = new byte[bufSize + 16 + 2];
        ptr = offset = start = 0;
        end = start + bufSize;
        cipher = new AESCipher(key);
        counter = new byte[16];
    }

    public int length() {
        return offset + ptr - start;
    }

    public void flush() throws IOException {
        int sentUpTo = start;
        while (sentUpTo < ptr) {
            int n = writeMessage(b, sentUpTo, ptr - sentUpTo);
            sentUpTo += n;
            offset += n;
        }

        ptr = start;
    }

    protected int overrun(int itemSize, int nItems) throws IOException {
        if (itemSize > bufSize) {
            throw new IOException("AESOutStream overrun: max itemSize exceeded");
        }

        flush();

        int nAvail;
        nAvail = (end - ptr) / itemSize;
        return Math.min(nAvail, nItems);

    }

    protected int writeMessage(byte[] data, int dataPtr, int length) throws IOException {
        if (length == 0)
            return 0;
        buffer[0] = (byte) ((length & 0xff00) >> 8);
        buffer[1] = (byte) (length & 0xff);
        cipher.encrypt(data, dataPtr, length,
                buffer, 0, 2,
                counter,
                buffer, 2,
                buffer, 2 + length);
        out.write(buffer, 0, length + 16 + 2);
        out.flush();
        // Update nonce by incrementing the counter as a
        // 128bit little endian unsigned integer
        for (int i = 0; i < 16; ++i) {
            // increment until there is no carry
            if (++counter[i] != 0) {
                break;
            }
        }
        return length;
    }
}
