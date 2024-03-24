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


package com.iiordanov.bVNC.input;

import android.os.Handler;

import com.iiordanov.bVNC.RemoteCanvas;

public class PanRepeater implements Runnable {

    // Multiplier used to reduce initial velocity.
    static final float speedFactor = 0.008f;
    // Delay next scheduled pan by this value.
    int delay = 5;
    private RemoteCanvas canvas = null;
    private Handler handler = null;
    // Used to hold current velocity
    private float velocityX;
    private float velocityY;

    public PanRepeater(RemoteCanvas canvas, Handler handler) {
        this.canvas = canvas;
        this.handler = handler;
    }

    public void start(float velocityX, float velocityY) {
        stop();
        this.velocityX = velocityX * speedFactor;
        this.velocityY = velocityY * speedFactor;
        //android.util.Log.i ("PanRepeater", "Initial velocities: " + velocityX + "x" + velocityY);
        handler.post(this);
    }

    public void stop() {
        handler.removeCallbacks(this);
    }

    @Override
    public void run() {
        float pX = Math.abs(velocityX);
        float pY = Math.abs(velocityY);
        if (pX >= 1 || pY >= 1) {
            //android.util.Log.i ("PanRepeater", "Panning by: " + velocityX + "x" + velocityY);
            canvas.relativePan((int) velocityX, (int) velocityY);
            velocityX = velocityX / 1.23f;
            velocityY = velocityY / 1.23f;
            handler.postDelayed(this, delay);
        }
    }

}
