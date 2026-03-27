package com.vectras.vm.fcm;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;

public class FCMManager {
    private static final String TAG = "FCMManager";

    public static void subscribe() {
        FirebaseMessaging.getInstance()
                .subscribeToTopic("vectrasvmandroidgithub")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Subscribed: vectrasvmandroidgithub");
                    } else {
                        Log.e(TAG, "Subscription failed: vectrasvmandroidgithub.", task.getException());
                    }
                });
    }

    public static void unSubscribe() {
        FirebaseMessaging.getInstance()
                .unsubscribeFromTopic("vectrasvmandroidgithub")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Unsubscribed: vectrasvmandroidgithub");
                    } else {
                        Log.e(TAG, "Cancellation failed: vectrasvmandroidgithub.", task.getException());
                    }
                });
    }
}
