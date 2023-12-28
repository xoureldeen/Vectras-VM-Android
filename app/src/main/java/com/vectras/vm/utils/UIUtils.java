package com.vectras.vm.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.Gravity;
import android.webkit.WebView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.vectras.vm.R;
import com.vectras.vm.AppConfig;
import com.vectras.vm.logger.VectrasStatus;

import java.io.IOException;

public class UIUtils {

	public static void toastLong(final Context activity, final String errStr) {
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {

				Toast toast = Toast.makeText(activity, errStr, Toast.LENGTH_LONG);
				toast.show();
                VectrasStatus.logInfo("<font color='yellow'>[I] "+errStr+"</font>");

			}
		});

	}

	public static void showHints(Activity activity) {

	}
    
    public static void UIAlertHtml(String title, String html, Activity activity) {

        AlertDialog alertDialog;
        alertDialog = new AlertDialog.Builder(activity).create();
        alertDialog.setTitle(title);
        WebView webview = new WebView(activity);
        webview.loadData(html, "text/html", "UTF-8");
        alertDialog.setView(webview);
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });
        alertDialog.show();
    }
}
