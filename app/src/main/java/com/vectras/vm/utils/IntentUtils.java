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
            DialogUtils.oneDialog(
                    context,
                    context.getString(R.string.oops),
                    context.getString(R.string.there_is_no_app_to_perform_this_action),
                    R.drawable.error_96px
            );
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
}
