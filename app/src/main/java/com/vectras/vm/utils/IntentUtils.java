package com.vectras.vm.utils;

import static android.content.Intent.ACTION_VIEW;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.vectras.vm.AppConfig;
import com.vectras.vm.R;

public class IntentUtils {
    public static boolean openTelegramLink(Context context) {
        return openUrl(context, AppConfig.telegramLink, true);
    }

    public static boolean openUrl(Context context, String url, boolean isShowErrorDialog) {
        boolean result = openUrl(context, url);
        if (isShowErrorDialog && !result) {
            showErrorDialog(context);
        }
        return result;
    }

    public static boolean openUrl(Context context, String url) {
        Intent intent = new Intent(ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse(url));

        PackageManager packagemanager = context.getPackageManager();
        if (intent.resolveActivity(packagemanager) != null) {
            context.startActivity(intent);
            return true;
        } else {
            return false;
        }
    }

    public static void launchPlayStoreVersion(Context context) {
        openApp(context, "com.vectrasllc.vm");
    }

    public static void openApp(Context context, String packageName) {
        openApp(context, packageName, true, true);
    }

    public static boolean openApp(Context context, String packageName, boolean isOpenAsNewTask, boolean isOpenPlayStore) {
        PackageManager pm = context.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(packageName);

        if (intent != null) {
            if (isOpenAsNewTask) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else if (isOpenPlayStore) {
            if (!openUrl(context, "https://play.google.com/store/apps/details?id=" + packageName)) showErrorDialog(context);
        } else {
            showErrorDialog(context);
            return false;
        }

        return true;
    }

    public static void showErrorDialog(Context context) {
        if (!DialogUtils.isAllowShow(context)) return;

        DialogUtils.oopsDialog(
                context,
                context.getString(R.string.there_is_no_app_to_perform_this_action)
        );
    }
}
