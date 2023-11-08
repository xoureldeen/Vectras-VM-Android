package com.epicstudios.vectras.utils;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import com.epicstudios.vectras.Config;

public class KeyboardUtils {
	private static final String TAG = "KeyboardUtils";

	public static boolean showKeyboard(Activity activity, View view) {

		InputMethodManager inputMgr = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (view != null) {
			view.requestFocus();
			inputMgr.showSoftInput(view, InputMethodManager.SHOW_FORCED);

		} else {
			if (view != null) {
				inputMgr.hideSoftInputFromWindow(view.getWindowToken(), 0);
			}
			return true;
		}
		return true;
	}

	public static void hideKeyboard(Activity activity, View view) {
		InputMethodManager inputMgr = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
		if (view != null) {
			inputMgr.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}
}
