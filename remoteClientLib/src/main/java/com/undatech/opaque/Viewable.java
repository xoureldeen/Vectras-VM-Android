package com.undatech.opaque;

import android.content.Context;
import android.graphics.Bitmap;

public interface Viewable {
    void waitUntilInflated();

    int getDesiredWidth();

    int getDesiredHeight();

    void reallocateDrawable(int width, int height);

    Bitmap getBitmap();

    void reDraw(int x, int y, int width, int height);

    void reDraw(float x, float y, float width, float height);

    void setMousePointerPosition(int x, int y);

    void mouseMode(boolean relative);

    boolean isAbleToPan();

    void setImageDrawable(AbstractDrawableData drawable);

    Context getContext();

    void invalidateMousePosition();

    boolean getMouseFollowPan();

    int getAbsX();

    int getAbsY();

    int getVisibleDesktopWidth();

    int getVisibleDesktopHeight();

    int getImageWidth();

    int getImageHeight();

    int getWidth();

    int getHeight();

    boolean isUseFull();

    void syncScroll();

    void doneWaiting();

    void softCursorMove(int updateRectX, int updateRectY);

    //void updateFBSize(int width, int height);

    void displayShortToastMessage(final CharSequence message);

    void postDrawableSetter();

    int framebufferWidth();

    int framebufferHeight();

    void prepareFullUpdateRequest(boolean incremental);

    int getXoffset();

    int getYoffset();

    int bmWidth();

    int bmHeight();

    int getRemoteWidth(int width, int height);

    int getRemoteHeight(int width, int height);

    void writeScreenshotToFile(String filePath, int dstWidth);

    void disposeDrawable();
}
