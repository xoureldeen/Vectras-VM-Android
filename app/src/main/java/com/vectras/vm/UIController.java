package com.vectras.vm;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class UIController {
    public static void edgeToEdge(ComponentActivity _activity) {
        EdgeToEdge.enable(_activity);
    }

    public static void setOnApplyWindowInsetsListener(View _view) {
        ViewCompat.setOnApplyWindowInsetsListener(_view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public static void simpleAnimationScale(View _view, int _duration) {
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

    public static void simpleTranslationUpToDown(View _view, int _duration) {
        ObjectAnimator ani1 = new ObjectAnimator();
        ani1.setTarget(_view);
        ani1.setPropertyName("translationY");
        ani1.setFloatValues((float)(-20), (float)(0));
        ani1.setDuration(_duration);
        ani1.setInterpolator(new LinearInterpolator());
        ani1.start();
    }
}
