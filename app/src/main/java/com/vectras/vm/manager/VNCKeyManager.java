package com.vectras.vm.manager;

import android.androidVNC.VncCanvas;
import android.util.Log;
import android.view.View;

import com.vectras.vm.R;

public class VNCKeyManager {
    final String TAG = "VNCKeyManager";

    public static final int TRACK_TAG = 99;
    public static final int DEL_KEY_CODE = 0xFFFF;
    public static final int SHIFT_LEFT_KEY_CODE = 0xFFE1;
    public static final int CTRL_LEFT_KEY_CODE = 0xFFE3;
    public static final int ALT_LEFT_KEY_CODE = 0xFFE9;
    public static final int WIN_LEFT_KEY_CODE = 0xFFEB;

    public int holdMarksCollector = 0;

    VncCanvas vncCanvas;

    View shiftButtonView;
    View ctrlButtonView;
    View altButtonView;
    View winButtonView;

    public VNCKeyManager(VncCanvas vncCanvas) {
        this.vncCanvas = vncCanvas;
    }

    public void setButtons(View shiftButtonView, View ctrlButtonView, View altButtonView, View winButtonView) {
        this.shiftButtonView = shiftButtonView;
        this.ctrlButtonView = ctrlButtonView;
        this.altButtonView = altButtonView;
        this.winButtonView = winButtonView;
    }

    public void reset() {
        Log.d(TAG, "reset");

        isHoldWinKey = false;
        isHoldAltKey = false;
        isHoldCtrlKey = false;
        isHoldShiftKey = false;

        updateButton(shiftButtonView, false);
        updateButton(ctrlButtonView, false);
        updateButton(altButtonView, false);
        updateButton(winButtonView, false);

        holdMarksCollector = 0;

        if (vncCanvas == null || !vncCanvas.isAttachedToWindow() || vncCanvas.rfb == null) return;

        vncCanvas.metaStateCollector = 0;

        vncCanvas.sendMetaKey1Up(SHIFT_LEFT_KEY_CODE, holdMarksCollector);
        vncCanvas.sendMetaKey1Up(CTRL_LEFT_KEY_CODE, holdMarksCollector);
        vncCanvas.sendMetaKey1Up(ALT_LEFT_KEY_CODE, holdMarksCollector);
        vncCanvas.sendMetaKey1Up(WIN_LEFT_KEY_CODE, holdMarksCollector);
    }

    public void updateButton(View button, boolean isDown) {
        if (button != null)
            button.setBackgroundResource(isDown ? R.drawable.controls_button2 : R.drawable.controls_button1);
    }

    boolean isPauseCheckKeyChanged = false;

    public void onKeyChanged(int keyCode, boolean isDown, int flags) {
        if (isPauseCheckKeyChanged && isDown) {
            isPauseCheckKeyChanged = false;
            return;
        }

        if (
                keyCode != SHIFT_LEFT_KEY_CODE &&
                        keyCode != CTRL_LEFT_KEY_CODE &&
                        keyCode != ALT_LEFT_KEY_CODE &&
                        keyCode != WIN_LEFT_KEY_CODE
        ) {
            reset();
        }

        isPauseCheckKeyChanged = false;
    }

    private void onChange() {
        isPauseCheckKeyChanged = true;
        if (vncCanvas != null && vncCanvas.isAttachedToWindow()) vncCanvas.metaStateCollector = holdMarksCollector;
    }

    // SHIFT
    boolean isHoldShiftKey;

    public void shiftKey() {
        if (isHoldShiftKey) {
            holdMarksCollector &= ~VncCanvas.SHIFT_MASK;
            vncCanvas.sendMetaKey1Up(SHIFT_LEFT_KEY_CODE, holdMarksCollector);
        } else {
            holdMarksCollector |= VncCanvas.SHIFT_MASK;
            vncCanvas.sendMetaKey1Down(SHIFT_LEFT_KEY_CODE, holdMarksCollector);
        }

        isHoldShiftKey = !isHoldShiftKey;

        updateButton(shiftButtonView, isHoldShiftKey);

        onChange();
    }

    // CTRL
    boolean isHoldCtrlKey;

    public void ctrlKey() {
        if (isHoldCtrlKey) {
            holdMarksCollector &= ~VncCanvas.CTRL_MASK;
            vncCanvas.sendMetaKey1Up(CTRL_LEFT_KEY_CODE, holdMarksCollector);
        } else {
            holdMarksCollector |= VncCanvas.CTRL_MASK;
            vncCanvas.sendMetaKey1Down(CTRL_LEFT_KEY_CODE, holdMarksCollector);
        }

        isHoldCtrlKey = !isHoldCtrlKey;

        updateButton(ctrlButtonView, isHoldCtrlKey);

        onChange();
    }

    // CTRL
    boolean isHoldAltKey;

    public void altKey() {
        if (isHoldAltKey) {
            holdMarksCollector &= ~VncCanvas.ALT_MASK;
            vncCanvas.sendMetaKey1Up(ALT_LEFT_KEY_CODE, holdMarksCollector);
        } else {
            holdMarksCollector |= VncCanvas.ALT_MASK;
            vncCanvas.sendMetaKey1Down(ALT_LEFT_KEY_CODE, holdMarksCollector);
        }

        isHoldAltKey = !isHoldAltKey;

        updateButton(altButtonView, isHoldAltKey);

        onChange();
    }

    // WIN
    boolean isHoldWinKey;

    public void winKey() {
        if (isHoldWinKey) {
            holdMarksCollector &= ~VncCanvas.META_MASK;
            vncCanvas.sendMetaKey1Up(WIN_LEFT_KEY_CODE, holdMarksCollector);
        } else {
            holdMarksCollector |= VncCanvas.META_MASK;
            vncCanvas.sendMetaKey1Down(WIN_LEFT_KEY_CODE, holdMarksCollector);
        }

        isHoldWinKey = !isHoldWinKey;

        updateButton(winButtonView, isHoldWinKey);

        onChange();
    }

    // DELETE
    public void deleteKey() {
        vncCanvas.sendMetaKey1(DEL_KEY_CODE, holdMarksCollector);

        onChange();
    }
}
