package com.vectras.vm.utils;

import android.content.Context;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class GmsChecker {
    public static boolean isAvailable(Context context) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        return googleApiAvailability.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
    }
}
