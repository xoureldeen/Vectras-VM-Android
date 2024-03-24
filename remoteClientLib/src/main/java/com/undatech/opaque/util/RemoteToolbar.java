package com.undatech.opaque.util;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

public class RemoteToolbar extends Toolbar {
    private static final String TAG = "RemoteToolbar";

    public RemoteToolbar(Context context) {
        super(context);
    }

    public RemoteToolbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RemoteToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setPositionToMakeVisible(
            int XCoor, int YCoor, int rootRight, int rootBottom, int resetPositionX, int resetPositionY
    ) {
        if (XCoor > rootRight || YCoor > rootBottom) {
            this.setX(resetPositionX);
            this.setY(resetPositionY);
        } else {
            this.setX(XCoor);
            this.setY(YCoor);
        }
    }
}
