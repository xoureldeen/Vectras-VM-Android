package com.iiordanov.bVNC.input;

import android.content.Context;
import android.os.Handler;

import com.undatech.opaque.InputCarriable;
import com.undatech.opaque.RfbConnectable;
import com.undatech.opaque.Viewable;

public abstract class RemotePointer extends com.undatech.opaque.input.RemotePointer {
    public RemotePointer(
            RfbConnectable rfbConnectable, Context context, InputCarriable remoteInput,
            Viewable canvas, Handler handler, boolean debugLogging
    ) {
        super(rfbConnectable, context, remoteInput, canvas, handler, debugLogging);
    }
}
