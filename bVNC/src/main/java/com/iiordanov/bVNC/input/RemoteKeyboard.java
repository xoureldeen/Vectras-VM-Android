package com.iiordanov.bVNC.input;

import android.content.Context;
import android.os.Handler;

import com.undatech.opaque.RfbConnectable;

public abstract class RemoteKeyboard extends com.undatech.opaque.input.RemoteKeyboard {
    public RemoteKeyboard(RfbConnectable r, Context v, Handler h, boolean debugLog) {
        super(r, v, h, debugLog);
    }

    public abstract void sendMetaKey(MetaKeyBean meta);
}
