/**
 * Copyright (C) 2023 Iordan Iordanov
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

package com.iiordanov.bVNC.input

import android.app.Activity
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import com.iiordanov.bVNC.Constants
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class RemoteClientsInputListener(
    val activity: Activity,
    private val keyInputHandler: KeyInputHandler?,
    private val touchInputHandler: TouchInputHandler?,
    val resetOnScreenKeys: (input: Int) -> Int,
    private val useDpadAsArrows: Boolean,
) : View.OnKeyListener {
    private val tag: String = "OnKeyListener"
    private val workerPool: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onKey(v: View?, keyCode: Int, evt: KeyEvent): Boolean {
        var consumed: Boolean? = false
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            return if (evt.action == KeyEvent.ACTION_DOWN) activity.onKeyDown(
                keyCode, evt
            ) else activity.onKeyUp(keyCode, evt)
        }
        try {
            if (evt.action == KeyEvent.ACTION_DOWN || evt.action == KeyEvent.ACTION_MULTIPLE) {
                consumed = keyInputHandler?.onKeyDownEvent(keyCode, evt)
            } else if (evt.action == KeyEvent.ACTION_UP) {
                consumed = keyInputHandler?.onKeyUpEvent(keyCode, evt)
            }
            resetOnScreenKeys(keyCode)
        } catch (e: NullPointerException) {
            Log.e(tag, "NullPointerException ignored")
        }
        return consumed ?: false
    }

    fun sendText(s: String) {
        workerPool.submit { sendTextSync(s) }
    }

    private fun sendTextSync(s: String) {
        for (i in s.indices) {
            var event: KeyEvent?
            val c = s[i]
            if (Character.isISOControl(c)) {
                if (c == '\n') {
                    val keyCode = KeyEvent.KEYCODE_ENTER
                    keyInputHandler?.onKeyDownEvent(
                        keyCode, KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
                    )
                    throttleInputByWaiting()
                    keyInputHandler?.onKeyUpEvent(keyCode, KeyEvent(KeyEvent.ACTION_UP, keyCode))
                }
            } else {
                event = KeyEvent(
                    SystemClock.uptimeMillis(), s.substring(i, i + 1), KeyCharacterMap.FULL, 0
                )
                keyInputHandler?.onKeyDownEvent(event.keyCode, event)
                throttleInputByWaiting()
            }
        }
    }

    private fun throttleInputByWaiting() {
        try {
            Thread.sleep(5)
        } catch (e: InterruptedException) {
            Log.e(tag, "InterruptedException ignored")
        }
    }

    fun onTrackballEvent(event: MotionEvent?): Boolean {
        try {
            // If we are using the Dpad as arrow keys, don't send the event to the inputHandler.
            return if (useDpadAsArrows) false else this.touchInputHandler?.onTouchEvent(event) ?: false
        } catch (e: NullPointerException) {
            Log.e(tag, "NullPointerException ignored")
        }
        return false
    }

    // Send touch events or mouse events like button clicks to be handled.
    fun onTouchEvent(event: MotionEvent?): Boolean {
        try {
            return this.touchInputHandler?.onTouchEvent(event) ?: false
        } catch (e: NullPointerException) {
            Log.e(tag, "NullPointerException ignored")
        }
        return false
    }

    // Send e.g. mouse events like hover and scroll to be handled.
    fun onGenericMotionEvent(event: MotionEvent): Boolean {
        // Ignore TOOL_TYPE_FINGER events that come from the touchscreen with HOVER type action
        // which cause pointer jumping trouble in simulated touchpad for some devices.
        var toolTypeFinger = false
        if (Constants.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            toolTypeFinger = event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER
        }
        val a = event.action
        val isHoverEnter = a == MotionEvent.ACTION_HOVER_ENTER
        val isHoverExit = a == MotionEvent.ACTION_HOVER_EXIT
        val isHoverEventFromFingerOnTouchscreen =
            (a == MotionEvent.ACTION_HOVER_MOVE && event.source == InputDevice.SOURCE_TOUCHSCREEN && toolTypeFinger)
        if (!(isHoverEnter || isHoverExit || isHoverEventFromFingerOnTouchscreen)) {
            return this.touchInputHandler?.onTouchEvent(event) ?: false
        }
        return false
    }
}