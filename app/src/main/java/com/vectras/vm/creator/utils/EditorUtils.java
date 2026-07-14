package com.vectras.vm.creator.utils;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.WindowManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;

public class EditorUtils {
    public static BottomSheetDialog getDummyDialog(Activity activity) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        if (dialog.getWindow() != null) {
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        return dialog;
    }
}
