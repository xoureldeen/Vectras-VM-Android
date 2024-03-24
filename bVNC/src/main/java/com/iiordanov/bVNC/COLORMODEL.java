/**
 * Copyright (C) 2012 Iordan Iordanov
 * Copyright (C) 2010 Michael A. MacDonald
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

package com.iiordanov.bVNC;

import com.undatech.opaque.RfbConnectable;
import com.undatech.remoteClientUi.R;

import java.io.IOException;

public enum COLORMODEL {
    C24bit, C256, C64, C8, C4, C2;

    public int bpp() {
        switch (this) {
            case C24bit:
                return 4;
            default:
                return 1;
        }
    }

    public int[] palette() {
        switch (this) {
            case C24bit:
                return null;
            case C256:
                return ColorModel256.colors;
            case C64:
                return ColorModel64.colors;
            case C8:
                return ColorModel8.colors;
            case C4:
                return ColorModel64.colors;
            case C2:
                return ColorModel8.colors;
            default:
                return null;
        }
    }

    public String nameString() {
        return super.toString();
    }

    public void setPixelFormat(RfbConnectable rfb) throws IOException {
        switch (this) {
            case C24bit:
                // 24-bit color
                rfb.writeSetPixelFormat(32, 24, false, true, 255, 255, 255, 16, 8, 0, false);
                break;
            case C256:
                rfb.writeSetPixelFormat(8, 8, false, true, 7, 7, 3, 0, 3, 6, false);
                break;
            case C64:
                rfb.writeSetPixelFormat(8, 6, false, true, 3, 3, 3, 4, 2, 0, false);
                break;
            case C8:
                rfb.writeSetPixelFormat(8, 3, false, true, 1, 1, 1, 2, 1, 0, false);
                break;
            case C4:
                // Greyscale
                rfb.writeSetPixelFormat(8, 6, false, true, 3, 3, 3, 4, 2, 0, true);
                break;
            case C2:
                // B&W
                rfb.writeSetPixelFormat(8, 3, false, true, 1, 1, 1, 2, 1, 0, true);
                break;
            default:
                // Default is 24 bit color
                rfb.writeSetPixelFormat(32, 24, false, true, 255, 255, 255, 16, 8, 0, false);
                break;
        }
    }

    public String toString() {
        switch (this) {
            case C24bit:
                return App.getContext().getString(R.string.color_24_bit);
            case C256:
                return App.getContext().getString(R.string.color_256);
            case C64:
                return App.getContext().getString(R.string.color_64);
            case C8:
                return App.getContext().getString(R.string.color_8);
            case C4:
                return App.getContext().getString(R.string.color_greyscale);
            case C2:
                return App.getContext().getString(R.string.color_black_and_white);
            default:
                return App.getContext().getString(R.string.color_24_bit);
        }
    }
}
