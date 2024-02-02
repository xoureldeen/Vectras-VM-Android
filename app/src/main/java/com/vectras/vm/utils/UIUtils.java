package com.vectras.vm.utils;

import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.vectras.qemu.Config;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.qemu.utils.FileUtils;
import com.vectras.vm.MainActivity;
import com.vectras.vm.R;
import com.vectras.vm.logger.VectrasStatus;

import java.io.IOException;
import java.util.Scanner;

public class UIUtils {

    private static final String TAG = "UIUtils";

    public static Spannable formatAndroidLog(String contents) {

        Scanner scanner = null;
        Spannable formattedString = new SpannableString(contents);
        if(contents.length()==0)
            return formattedString;

        try {
            scanner = new Scanner(contents);
            int counter = 0;
            ForegroundColorSpan colorSpan = null;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                //FIXME: some devices don't have standard format for the log
                if (line.startsWith("E/") || line.contains(" E ")) {
                    colorSpan = new ForegroundColorSpan(Color.rgb(255, 22, 22));
                } else if (line.startsWith("W/") || line.contains(" W ")) {
                    colorSpan = new ForegroundColorSpan(Color.rgb(22, 44, 255));
                } else {
                    colorSpan = null;
                }
                if (colorSpan!= null) {
                    formattedString.setSpan(colorSpan, counter, counter + line.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                counter += line.length()+1;
            }

        }catch (Exception ex) {
            Log.e(TAG, "Could not format vectras log: " + ex.getMessage());
        } finally {
            if(scanner!=null) {
                try {
                    scanner.close();
                } catch (Exception ex) {
                    if(Config.debug)
                        ex.printStackTrace();
                }
            }

        }
        return formattedString;
    }
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

    public static void showFileNotSupported(Activity context){
        UIAlert(context, "Error", "File path is not supported. Make sure you choose a file/directory from your internal storage or external sd card. Root and Download Directories are not supported.");
    }


    public static boolean onKeyboard(Activity activity, boolean toggle, View view) {
        // Prevent crashes from activating mouse when machine is paused
        if (MainActivity.vmexecutor.paused == 1)
            return !toggle;

        InputMethodManager inputMgr = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);

        //XXX: we need to get the focused view to make this always work
        //inputMgr.toggleSoftInput(0, 0);


//        View view = activity.getCurrentFocus();
        if (toggle || !Config.enableToggleKeyboard){
            if(view!=null) {
                view.requestFocus();
                inputMgr.showSoftInput(view, InputMethodManager.SHOW_FORCED);
            }
        } else {
            if (view != null) {
                inputMgr.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }

        return !toggle;
    }

    public static void hideKeyboard(Activity activity, View view) {
        InputMethodManager inputMgr = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (view != null) {
            inputMgr.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void toastShortTop(final Activity activity, final String errStr) {
        UIUtils.toast(activity, errStr, Gravity.TOP | Gravity.CENTER, Toast.LENGTH_SHORT);
    }

    public static void toast(final Context context, final String errStr, final int gravity, final int length) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if(context instanceof Activity && ((Activity) context).isFinishing()) {
                    return ;
                }
                Toast toast = Toast.makeText(context, errStr, length);
                toast.setGravity(gravity, 0, 0);
                toast.show();

            }
        });

    }

    public static void toastShort(final Context context, final String errStr) {
        toast(context, errStr, Gravity.CENTER | Gravity.CENTER, Toast.LENGTH_SHORT);

    }

    public static void setOrientation(Activity activity) {
        int orientation = MainSettingsManager.getOrientationSetting(activity);
        switch (orientation) {
            case 0:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                break;
            case 1:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case 2:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
            case 3:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case 4:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                break;
        }
    }


    public static void onChangeLog(Activity activity) {
        PackageInfo pInfo = null;

        try {
            pInfo = activity.getPackageManager().getPackageInfo(activity.getClass().getPackage().getName(),
                    PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        com.vectras.qemu.utils.FileUtils fileutils = new com.vectras.qemu.utils.FileUtils();
        try {
            UIUtils.UIAlert(activity,"CHANGELOG", fileutils.LoadFile(activity, "CHANGELOG", false),
                    0, false, "OK", null, null, null, null, null);
        } catch (IOException e) {

            e.printStackTrace();
        }
    }



    public static void showHints(Activity activity) {


        UIUtils.toastShortTop(activity, "Press Volume Down for Right Click");

        UIUtils.toastShortTop(activity, "Press Volume Up for Left Click");

        UIUtils.toastShortTop(activity, "Press Back Button for Hide/Show Controls UI");


    }

    public static boolean isLandscapeOrientation(Activity activity)
    {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point screenSize = new Point();
        display.getSize(screenSize);
        if(screenSize.x < screenSize.y)
            return false;
        return true;
    }

    private static void openURL(Activity activity, String url) {
        try {
            Intent fileIntent = new Intent(Intent.ACTION_VIEW);
            fileIntent.setData(Uri.parse(url));
            activity.startActivity(fileIntent);
        }catch (Exception ex) {
            UIUtils.toastShort(activity, "Could not open url");
        }
    }

    public static void UIAlert(Activity activity, String title, String body) {

        AlertDialog alertDialog;
        alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
        alertDialog.setTitle(title);
        TextView textView = new TextView(activity);
        textView.setPadding(20,20,20,20);
        textView.setText(body);
        ScrollView view = new ScrollView(activity);
        view.addView(textView);
        alertDialog.setView(view);
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });
        alertDialog.show();
    }

    public static void UIAlert(Activity activity, String title, String body, int textSize, boolean cancelable,
                               String button1title, DialogInterface.OnClickListener button1Listener,
                               String button2title, DialogInterface.OnClickListener button2Listener,
                               String button3title, DialogInterface.OnClickListener button3Listener
    ) {

        AlertDialog alertDialog;
        alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
        alertDialog.setTitle(title);
        alertDialog.setCanceledOnTouchOutside(cancelable);
        TextView textView = new TextView(activity);
        textView.setPadding(20,20,20,20);
        textView.setText(body);
        if(textSize>0)
            textView.setTextSize(textSize);
        textView.setBackgroundColor(Color.WHITE);
        textView.setTextColor(Color.BLACK);
        ScrollView view = new ScrollView(activity);
        view.addView(textView);
        alertDialog.setView(view);
        if(button1title!=null)
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, button1title, button1Listener);
        if(button2title!=null)
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, button2title, button2Listener);
        if(button3title!=null)
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, button3title, button3Listener);
        alertDialog.show();
    }

    public static void UIAlertLog(final Activity activity, String title, Spannable body) {

        AlertDialog alertDialog;
        alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
        alertDialog.setTitle(title);
        TextView textView = new TextView(activity);
        textView.setPadding(20,20,20,20);
        textView.setText(body);
        textView.setBackgroundColor(Color.BLACK);
        textView.setTextSize(12);
        textView.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        textView.setSingleLine(false);
        ScrollView view = new ScrollView(activity);
        view.addView(textView);
        alertDialog.setView(view);
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });
        alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Copy To", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                ClipboardManager clipboardManager = (ClipboardManager)
                        activity.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("nonsense_data",
                        body);
                clipboardManager.setPrimaryClip(clipData);
                UIUtils.toastShort(activity, "Copied to clipboard successfully!");
                return;
            }
        });
        alertDialog.show();
    }

    public static void UIAlertHtml(String title, String body, Activity activity) {

        AlertDialog alertDialog;
        alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
        alertDialog.setTitle(title);

        try {
            WebView webview = new WebView(activity);
            webview.loadData(body, "text/html", "UTF-8");
            alertDialog.setView(webview);
        } catch (Exception ex) {
            TextView textView = new TextView(activity);
            textView.setText(body);
            alertDialog.setView(textView);
        }

        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });
        alertDialog.show();
    }

    public static void promptShowLog(final Activity activity) {

        final AlertDialog alertDialog;
        alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
        alertDialog.setTitle("Show log");
        TextView stateView = new TextView(activity);
        stateView.setText("Something happened during last run, do you want to see the log?");
        stateView.setPadding(20, 20, 20, 20);
        alertDialog.setView(stateView);

        // alertDialog.setMessage(body);
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        FileUtils.viewVectrasLog(activity);
                    }
                });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {


                    }
                });
        alertDialog.show();

    }


}
