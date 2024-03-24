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

import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * The RemoteCanvasActivity has several different ways of handling input from the touchscreen,
 * keyboard, buttons and trackball.  These will be represented by different implementations
 * of this interface.  Putting the different modes in different classes
 * will keep the logic clean.  The relevant Activity callbacks in RemoteCanvasActivity
 * are forwarded to methods in AbstractInputHandler.
 * <p>
 * It is expected that the implementations will be contained within
 * RemoteCanvasActivity, so they can do things like super.RemoteCanvasActivity.onXXX to invoke
 * default behavior.
 * @author Michael A. MacDonald
 *
 */
public interface AbstractInputHandler {
    /**
     * Note: Menu key code is handled before this is called
     * @see android.app.Activity#onKeyDown(int keyCode, KeyEvent evt)
     */
    boolean onKeyDown(int keyCode, KeyEvent evt);

    /**
     * Note: Menu key code is handled before this is called
     * @see android.app.Activity#onKeyUp(int keyCode, KeyEvent evt)
     */
    boolean onKeyUp(int keyCode, KeyEvent evt);

    /* (non-Javadoc)
     * @see android.app.Activity#onTrackballEvent(android.view.MotionEvent)
     */
    boolean onTrackballEvent(MotionEvent evt);

    /* (non-Javadoc)
     * @see android.app.Activity#onTouchEvent(android.view.MotionEvent)
     */
    boolean onTouchEvent(MotionEvent evt);

    /**
     * Return a user-friendly description for this mode; it will be displayed in a toaster
     * when changing modes.
     * @return
     */
    CharSequence getHandlerDescription();

    /**
     * Return an internal name for this handler; this name will be stable across language
     * and version changes
     */
    String getName();
}
