package com.vectras.vm.utils;

import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class SimpleAnimations {
    public static void scale(View _view, int _duration) {
        ObjectAnimator ani1 = new ObjectAnimator();
        ani1.setTarget(_view);
        ani1.setPropertyName("scaleX");
        ani1.setFloatValues((float)(1.1), (float)(1));
        ani1.setDuration(_duration);
        ani1.setInterpolator(new LinearInterpolator());
        ani1.start();

        ObjectAnimator ani2 = new ObjectAnimator();
        ani2.setTarget(_view);
        ani2.setPropertyName("scaleY");
        ani2.setFloatValues((float)(1.1), (float)(1));
        ani2.setDuration(_duration);
        ani2.setInterpolator(new LinearInterpolator());
        ani2.start();
    }

    public static void translationUpToDown(View _view, int _duration) {
        ObjectAnimator ani1 = new ObjectAnimator();
        ani1.setTarget(_view);
        ani1.setPropertyName("translationY");
        ani1.setFloatValues((float)(-20), (float)(0));
        ani1.setDuration(_duration);
        ani1.setInterpolator(new LinearInterpolator());
        ani1.start();
    }
}
