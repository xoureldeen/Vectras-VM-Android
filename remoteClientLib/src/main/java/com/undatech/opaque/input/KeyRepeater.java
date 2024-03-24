/**
 * Copyright (C) 2013- Iordan Iordanov
 * <p>
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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

package com.undatech.opaque.input;

import android.os.Handler;
import android.view.KeyEvent;

public class KeyRepeater implements Runnable {

    private RemoteKeyboard keyboard = null;
    private Handler handler = null;
    private int keyCode = 0;
    private KeyEvent event = null;
    private int initialDelay = 400;
    private int defaultDelay = 100;
    private boolean starting = false;

    public KeyRepeater(RemoteKeyboard keyboard, Handler handler) {
        this.keyboard = keyboard;
        this.handler = handler;
    }

    public void start(int keyCode, KeyEvent event) {
        stop();
        this.keyCode = keyCode;
        this.event = event;
        // This is here in order to ensure the key event is sent over at least once.
        // Otherwise with very quick repeated sending of events, the removeCallbacks
        // call causes events to be deleted before they've been sent out even once.
        keyboard.keyEvent(keyCode, KeyEvent.changeAction(event, KeyEvent.ACTION_DOWN));
        keyboard.keyEvent(keyCode, KeyEvent.changeAction(event, KeyEvent.ACTION_UP));
        starting = true;
        handler.post(this);
    }

    public void stop() {
        handler.removeCallbacks(this);
    }

    @Override
    public void run() {
        int delay = defaultDelay;
        if (starting) {
            starting = false;
            delay = initialDelay;
        } else {
            keyboard.keyEvent(keyCode, KeyEvent.changeAction(event, KeyEvent.ACTION_DOWN));
            keyboard.keyEvent(keyCode, KeyEvent.changeAction(event, KeyEvent.ACTION_UP));
        }

        handler.postDelayed(this, delay);
    }

}
