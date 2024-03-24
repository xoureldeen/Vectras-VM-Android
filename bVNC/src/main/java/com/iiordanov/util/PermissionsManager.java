package com.iiordanov.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.iiordanov.bVNC.Constants;
import com.iiordanov.bVNC.Utils;
import com.undatech.remoteClientUi.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by iordan on 24/06/18.
 */

public class PermissionsManager {
    private static final Map<PermissionGroups, String[]> permissionGroups;
    public static String TAG = "PermissionsManager";

    static {
        Map<PermissionGroups, String[]> temp = new HashMap<>();
        temp.put(PermissionGroups.RECORD_AUDIO, new String[]{"android.permission.RECORD_AUDIO"});
        temp.put(PermissionGroups.RECORD_AND_MODIFY_AUDIO,
                new String[]{"android.permission.RECORD_AUDIO", "android.permission.MODIFY_AUDIO_SETTINGS"});
        permissionGroups = Collections.unmodifiableMap(temp);
    }

    private static String[] retrievePermissions(Context context) {
        Log.i(TAG, "Retrieving permissions.");
        try {
            String packageName = Utils.pName(context);
            String[] requestedPermissions = context.getPackageManager().getPackageInfo(
                    packageName, PackageManager.GET_PERMISSIONS).requestedPermissions;
            android.util.Log.d(TAG, Arrays.toString(requestedPermissions));
            return requestedPermissions;
        } catch (PackageManager.NameNotFoundException e) {
            android.util.Log.e(TAG, "Failed to retrieve page info.", e);
            return new String[]{};
        }
    }

    public static void requestPermissions(Activity activity, PermissionGroups permission_group, boolean showToast) {
        String[] permissions = (String[]) permissionGroups.get(permission_group);
        if (permissions == null) {
            Log.e(TAG, "Could not find permissions for permission group: " + permission_group);
            return;
        }
        Log.i(TAG, "Requesting permissions for permission group: " + permission_group);
        for (String permission : permissions) {
            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                if (!Utils.querySharedPreferenceBoolean(activity, Constants.permissionsRequested)) {
                    Utils.setSharedPreferenceBoolean(activity, Constants.permissionsRequested, true);
                    // No explanation needed; request the permission
                    ActivityCompat.requestPermissions(activity, permissions, 0);
                } else if (showToast) {
                    Toast.makeText(activity, R.string.please_grant_permission_from_prefs, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
