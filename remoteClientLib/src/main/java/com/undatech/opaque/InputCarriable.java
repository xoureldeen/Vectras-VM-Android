package com.undatech.opaque;

import com.undatech.opaque.input.RemoteKeyboard;
import com.undatech.opaque.input.RemotePointer;

public interface InputCarriable {
    RemotePointer getPointer();
    RemoteKeyboard getKeyboard();
}
