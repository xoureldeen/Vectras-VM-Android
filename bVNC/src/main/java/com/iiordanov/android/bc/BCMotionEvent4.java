/**
 * Copyright (c) 2010 Michael A. MacDonald
 */
package com.iiordanov.android.bc;

import android.view.MotionEvent;

/**
 * Pre-sdk 5 version; add fake multi-touch sensing later?
 *
 * @author Michael A. MacDonald
 */
class BCMotionEvent4 implements IBCMotionEvent {

    /* (non-Javadoc)
     * @see com.iiordanov.android.bc.IBCMotionEvent#getPointerCount(android.view.MotionEvent)
     */
    @Override
    public int getPointerCount(MotionEvent evt) {
        return 1;
    }

}
