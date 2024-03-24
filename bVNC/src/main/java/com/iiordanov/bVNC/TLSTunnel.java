/*
 * Copyright (C) 2003 Sun Microsystems, Inc.
 * Copyright (C) 2003-2010 Martin Koegler
 * Copyright (C) 2006 OCCAM Financial Technology
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
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */

package com.iiordanov.bVNC;

import android.util.Log;

import com.iiordanov.bVNC.exceptions.AnonCipherUnsupportedException;

import java.net.Socket;
import java.util.ArrayList;

import javax.net.ssl.SSLSocket;

public class TLSTunnel extends TLSTunnelBase {
    private static final String TAG = "TLSTunnel";

    public TLSTunnel(Socket sock_) {
        super(sock_);
    }

    protected void setParam(SSLSocket sock) throws AnonCipherUnsupportedException {
        String[] supported;
        ArrayList<String> enabled = new ArrayList<String>();

        supported = sock.getSupportedCipherSuites();

        for (int i = 0; i < supported.length; i++) {
            if (supported[i].matches(".*DH_anon.*")) {
                enabled.add(supported[i]);
                Log.i(TAG, "Adding cipher: " + supported[i]);
            }
        }

        if (enabled.isEmpty()) {
            throw new AnonCipherUnsupportedException();
        }

        sock.setEnabledCipherSuites((String[]) enabled.toArray(new String[0]));
    }

}
