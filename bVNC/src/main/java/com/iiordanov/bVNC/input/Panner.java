/**
 * Copyright (C) 2012 Iordan Iordanov
 * Copyright (C) 2009 Michael A. MacDonald
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

package com.iiordanov.bVNC.input;

import android.graphics.PointF;
import android.os.Handler;
import android.os.SystemClock;

import com.iiordanov.bVNC.RemoteCanvas;
import com.iiordanov.bVNC.RemoteCanvasActivity;

/**
 * Handles panning the screen continuously over a period of time
 * @author Michael A. MacDonald
 */
public class Panner implements Runnable {

    private static final String TAG = "PANNER";
    final int freq = 10;
    RemoteCanvasActivity activity;
    Handler handler;
    PointF velocity;
    long lastSent;
    VelocityUpdater updater;

    public Panner(RemoteCanvasActivity act, Handler hand) {
        activity = act;
        velocity = new PointF();
        handler = hand;
    }

    public void stop() {
        handler.removeCallbacks(this);
    }

    public void start(float xv, float yv, VelocityUpdater update) {
        if (update == null)
            update = DefaultUpdater.instance;
        updater = update;
        velocity.x = xv;
        velocity.y = yv;
        //Log.v(TAG, String.format("pan start %f %f", velocity.x, velocity.y));
        lastSent = SystemClock.uptimeMillis();

        handler.postDelayed(this, freq);
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        long interval = SystemClock.uptimeMillis() - lastSent;
        lastSent += interval;
        double scale = (double) interval / 50.0;
        RemoteCanvas canvas = activity.getCanvas();
        //Log.v(TAG, String.format("panning %f %d %d", scale, (int)((double)velocity.x * scale), (int)((double)velocity.y * scale)));
        if (canvas.relativePan((int) ((double) velocity.x * scale), (int) ((double) velocity.y * scale))) {
            if (updater.updateVelocity(velocity, interval)) {
                handler.postDelayed(this, freq);
            } else {
                canvas.invalidate();
                stop();
            }
        } else {
            canvas.invalidate();
            stop();
        }
    }

    /**
     * Specify how the panning velocity changes over time
     * @author Michael A. MacDonald
     */
    interface VelocityUpdater {
        /**
         * Called approximately every 50 ms to update the velocity of panning
         * @param p X and Y components to update
         * @param interval Milliseconds since last update
         * @return False if the panning should stop immediately; true otherwise
         */
        boolean updateVelocity(PointF p, long interval);
    }

    static class DefaultUpdater implements VelocityUpdater {

        static DefaultUpdater instance = new DefaultUpdater();

        /**
         * Don't change velocity
         */
        @Override
        public boolean updateVelocity(PointF p, long interval) {
            return true;
        }

    }
}
