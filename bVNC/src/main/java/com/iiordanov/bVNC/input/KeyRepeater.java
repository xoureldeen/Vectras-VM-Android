package com.iiordanov.bVNC.input;

import android.os.Handler;
import android.view.KeyEvent;

import com.undatech.opaque.input.RemoteKeyboard;

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
