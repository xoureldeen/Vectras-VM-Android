package com.undatech.opaque;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.RectF;
import android.widget.ImageView;

public interface AbstractDrawableData {
    void doneWaiting();

    void setCursorRect(int x, int y, int w, int h, int hX, int hY);

    void moveCursorRect(int x, int y);

    void setSoftCursor(int[] newSoftCursorPixels);

    RectF getCursorRect();

    boolean isNotInitSoftCursor();

    float getMinimumScale();

    boolean widthRatioLessThanHeightRatio();

    void prepareFullUpdateRequest(boolean incremental);

    boolean validDraw(int x, int y, int w, int h);

    int offset(int x, int y);

    void updateBitmap(int x, int y, int w, int h);

    void updateBitmap(Bitmap b, int x, int y, int w, int h);

    void setImageDrawable(ImageView v);

    void updateView(ImageView v);

    void copyRect(int sx, int sy, int dx, int dy, int w, int h);

    void fillRect(int x, int y, int w, int h, int pix);

    void imageRect(int x, int y, int w, int h, int[] pix);

    void drawRect(int x, int y, int w, int h, Paint paint);

    void scrollChanged(int newx, int newy);

    void frameBufferSizeChanged(int width, int height);

    void syncScroll();

    void dispose();

    int fbWidth();

    int fbHeight();

    int bmWidth();

    int bmHeight();

    int getXoffset();

    int getYoffset();

    int[] getBitmapPixels();

    int getBitmapWidth();
    int getBitmapHeight();

    int getFramebufferWidth();
    int getFramebufferHeight();

    Bitmap getMbitmap();

    Paint getPaint();
}
