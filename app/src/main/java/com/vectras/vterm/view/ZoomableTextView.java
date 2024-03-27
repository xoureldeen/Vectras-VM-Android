package com.vectras.vterm.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ScaleGestureDetector;
import android.view.MotionEvent;
import androidx.appcompat.widget.AppCompatTextView;

public class ZoomableTextView extends AppCompatTextView {
    private ScaleGestureDetector scaleGestureDetector;
    private float mScaleFactor = 1.0f;
    private float defaultSize;

    public ZoomableTextView(Context context) {
        super(context);
        init(context);
    }

    public ZoomableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        defaultSize = getTextSize();
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
                mScaleFactor *= scaleGestureDetector.getScaleFactor();
                // Don't let the object get too small or too large.
                mScaleFactor = Math.max(0.5f, Math.min(mScaleFactor, 2.0f));
                setTextSize(mScaleFactor * defaultSize);
                return true;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        return scaleGestureDetector.onTouchEvent(event);
    }

    public void setTextSize(float size){
        super.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }
    public interface OnTextSizeChangeListener {
        void onTextSizeChange(float textSize);
    }

    private OnTextSizeChangeListener textSizeChangeListener;

    public void setOnTextSizeChangeListener(OnTextSizeChangeListener textSizeChangeListener) {
        this.textSizeChangeListener = textSizeChangeListener;
    }

    // In the code where the zoom and text size change happens:
    private void handleZoom(float scaleFactor) {
        // ... Existing zooming logic ...

        float newTextSize = this.getTextSize() * scaleFactor;
        this.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);

        // Notify the listener about the text size change
        if (textSizeChangeListener != null) {
            textSizeChangeListener.onTextSizeChange(newTextSize);
        }
    }
}
