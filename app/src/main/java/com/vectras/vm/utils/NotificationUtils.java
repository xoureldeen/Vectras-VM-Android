package com.vectras.vm.utils;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.vectras.vm.SplashActivity;

import java.util.Objects;

public class NotificationUtils {
    private static final String TAG = "NotificationUtils";
    public static final int NO_ICON = -1;

    public static String generalChannelId = "general";

    public static void createAllChannel(Context context) {
        createChannel("General", "Receive new notifications.",
                "general", NotificationManager.IMPORTANCE_DEFAULT, context);
    }

    @SuppressLint("MissingPermission")
    public static void pushNow(
            Context context,
            int notificationID,
            String channelID,
            String title,
            String content,
            int icon,
            String largeIconURL,
            int largeIconRes,
            String url,
            Class<?> activityClass) {

        if (!isPermissionGranted(context)) return;
        if (!isChannelExist(channelID, context)) createAllChannel(context);
        if (!isChannelEnabled(channelID, context)) return;

        Intent intent;
        if (url != null && !url.isEmpty()) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            intent = new Intent(context, Objects.requireNonNullElse(activityClass, SplashActivity.class));
        }
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        //If multiple notifications are sent, the PendingIntent will be overwritten. It is recommended to use notificationID.
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationID, intent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder mbuilder = new NotificationCompat.Builder(context, channelID)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
//                .setColor(ContextCompat.getColor(context, R.color.colorAccent));

        if ((largeIconURL != null && !largeIconURL.isEmpty()) || largeIconRes != NO_ICON) {
            Glide.with(context)
                    .asBitmap()
                    .load(largeIconURL == null || largeIconURL.isEmpty() ? largeIconRes : largeIconURL)
                    .apply(RequestOptions.circleCropTransform())
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            mbuilder.setLargeIcon(resource);
                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                            notificationManager.notify(notificationID, mbuilder.build());
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                            notificationManager.notify(notificationID, mbuilder.build());
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            // Handle when the resource is cleared
                        }
                    });
        } else {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(notificationID, mbuilder.build());
        }
    }

    public static void clearAll(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    public static boolean isChannelEnabled(String channelId, Context context) {
        if (Build.VERSION.SDK_INT < 26) return true;

        try {
            if (channelId != null && !channelId.isEmpty()) {
                NotificationManager manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                NotificationChannel channel = manager.getNotificationChannel(channelId);
                return channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error: ", e);
        }
        return false;
    }

    public static boolean isChannelExist(String id, Context _context) {
        if (Build.VERSION.SDK_INT < 26) return true;

        try {
            NotificationManager checkChannelAvailable = (NotificationManager) _context.getSystemService(NOTIFICATION_SERVICE);
            if (checkChannelAvailable.getNotificationChannel(id) != null) {
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error: ", e);
        }
        return false;
    }

    public static void createChannel(String title, String description, String id, int importance, Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            if (!isChannelExist(id, context)) {
                NotificationChannel channel = new NotificationChannel(id, title, importance);
                channel.setDescription(description);
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                notificationManager.createNotificationChannel(channel);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error: ", e);
        }
        }
    }

    public static boolean isPermissionGranted(Context context) {
        if (Build.VERSION.SDK_INT >= 33) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static void requestPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= 33 && !isPermissionGranted(activity))
            ActivityCompat.requestPermissions(activity, new String[] {Manifest.permission.POST_NOTIFICATIONS}, 1000);
    }
}
