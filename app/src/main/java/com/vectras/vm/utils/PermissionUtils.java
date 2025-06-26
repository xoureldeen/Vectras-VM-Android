package com.vectras.vm.utils;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.vectras.vm.R;

public class PermissionUtils {
    public static boolean storagepermission(Activity activity, boolean request) {
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            if (request) {
                DialogUtils.oneDialog(activity, activity.getString(R.string.allow_permissions), activity.getString(R.string.you_need_to_grant_permission_to_access_the_storage_before_use), activity.getString(R.string.allow), true, R.drawable.folder_24px, false,
                        () -> {
                            if (activity.shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                                activity.startActivity(intent);
                                Toast.makeText(activity, activity.getResources().getString(R.string.find_and_allow_access_to_storage_in_settings), Toast.LENGTH_LONG).show();
                            } else {
                                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
                            }
                        }, null);
            }
            return false;
        }
    }
}
