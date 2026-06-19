package com.vectras.vm.view;

import android.view.MotionEvent;
import android.view.View;

public class DynamicBubble {
    View container;
    View bubble;
    private float dX, dY, startX, startY;
    boolean isDragging;
    final int CLICK_ACTION_THRESHOLD = 10;
    Runnable onClicked;

    public DynamicBubble(View container, View bubble) {
        this.container = container;
        this.bubble = bubble;

        setup();
    }

    public void onClicked(Runnable onClicked) {
        this.onClicked = onClicked;
    }

    private void setup() {
        bubble.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {

                case MotionEvent.ACTION_DOWN:
                    // Save the View's initial position relative to your finger.
                    dX = v.getX() - event.getRawX();
                    dY = v.getY() - event.getRawY();

                    // Save the starting position to check if it's a click.
                    startX = event.getRawX();
                    startY = event.getRawY();
                    isDragging = false;
                    break;

                case MotionEvent.ACTION_MOVE:
                    // Calculate the distance moved.
                    float movedX = event.getRawX() - startX;
                    float movedY = event.getRawY() - startY;

                    // If the displacement exceeds the threshold, it is determined that pulling is occurring.
                    if (Math.abs(movedX) > CLICK_ACTION_THRESHOLD || Math.abs(movedY) > CLICK_ACTION_THRESHOLD) {
                        isDragging = true;

                        // Move the view using your finger.
                        v.setX(event.getRawX() + dX);
                        v.setY(event.getRawY() + dY);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    if (!isDragging) {
                        // Click event.
                        v.performClick();
                        if (onClicked != null) onClicked.run();
                    } else {
                        onDropped();
                    }
                    isDragging = false;
                    break;

                case MotionEvent.ACTION_CANCEL:
                    isDragging = false;
                    break;

                default:
                    return false;
            }
            return true;
        });
    }

    private void onDropped() {
        if (bubble.getX() > (float) container.getWidth() / 2) {
            bubble.animate().x(container.getWidth() - bubble.getWidth());
        } else {
            bubble.animate().x(0);
        }

        if (bubble.getY() < bubble.getHeight()) {
            bubble.animate().y(0);
        } else if (bubble.getY() > (float) container.getHeight() - bubble.getHeight()) {
            bubble.animate().y(container.getHeight() - bubble.getHeight());
        }
    }
}
