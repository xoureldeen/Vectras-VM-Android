package com.iiordanov.bVNC.input;

import android.view.KeyEvent;

public interface KeyInputHandler {
    boolean onKeyDownEvent(int keyCode, KeyEvent event);

    boolean onKeyUpEvent(int keyCode, KeyEvent event);
}
