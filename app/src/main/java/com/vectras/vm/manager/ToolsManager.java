package com.vectras.vm.manager;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.anbui.elephant.retrofit2utils.Retrofit2Utils;
import com.vectras.vm.AppConfig;
import com.vectras.vm.R;
import com.vectras.vm.VectrasApp;
import com.vectras.vm.setupwizard.SetupFeatureCore;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.NotificationUtils;

import java.io.File;

public class ToolsManager {
    public static void mount3dfxWrappers(Activity activity) {
        new Thread(() -> {
            if (!FileUtils.isFileExists(AppConfig.basefiledir + "3dfx-wrappers.iso"))
                SetupFeatureCore.copyAssetToFile(activity, "roms/3dfx-wrappers.iso", AppConfig.basefiledir + "3dfx-wrappers.iso");

            activity.runOnUiThread(() -> QmpSender.changeOpticalDisc(activity, AppConfig.basefiledir + "3dfx-wrappers.iso", ""));
        }).start();
    }

    public static void mountVirtIOWin(Activity activity) {
        new Thread(() -> {
            if (!FileUtils.isFileExists(AppConfig.basefiledir + "virtio-win.iso")) {
                FileUtils.delete(new File(AppConfig.basefiledir + "virtio-win.bin"));
                activity.runOnUiThread(() -> DialogUtils.twoDialog(
                        activity,
                        activity.getString(R.string.download_required),
                        activity.getString(R.string.this_tool_needs_to_be_downloaded_before_use),
                        activity.getString(R.string.ok),
                        activity.getString(R.string.cancel),
                        true,
                        R.drawable.arrow_downward_24px,
                        true,
                        () -> new Thread(() -> {

                            int notificationId = 30;

                            if (!NotificationUtils.isChannelExist(NotificationUtils.downloadChannelId, activity)) {
                                NotificationUtils.createChannel("Download", "View the file download process.",
                                        NotificationUtils.downloadChannelId, NotificationManager.IMPORTANCE_DEFAULT, activity);
                            }

                            NotificationManagerCompat manager = NotificationManagerCompat.from(VectrasApp.getContext());
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(VectrasApp.getContext(), NotificationUtils.downloadChannelId)
                                    .setSmallIcon(R.drawable.arrow_cool_down_24px)
                                    .setContentTitle(activity.getString(R.string.virtio_tools_for_windows))
                                    .setContentText("0%")
                                    .setPriority(NotificationCompat.PRIORITY_LOW)
                                    .setOngoing(true)
                                    .setOnlyAlertOnce(true);

                            builder.setProgress(100, 0, false);
                            manager.notify(notificationId, builder.build());

                            Retrofit2Utils.download(AppConfig.virtIOWinUrl, AppConfig.basefiledir + "virtio-win.bin", new Retrofit2Utils.DownloadCallback() {
                                @Override
                                public void onProgress(int percent) {
                                    builder.setProgress(100, percent, false)
                                            .setContentText(percent + "%");
                                    manager.notify(notificationId, builder.build());
                                    Log.d("DL", percent + "%");
                                }

                                @Override
                                public void onResult(boolean success, String path, Throwable error) {
                                    if (success) {
                                        FileUtils.move(AppConfig.basefiledir + "virtio-win.bin", AppConfig.basefiledir + "virtio-win.iso");

                                        if (DialogUtils.isAllowShow(activity)) {
                                            NotificationUtils.recall(activity, notificationId);

                                            activity.runOnUiThread(() -> DialogUtils.twoDialog(
                                                    activity,
                                                    activity.getString(R.string.virtio_tools_for_windows_is_now_ready_to_use),
                                                    activity.getString(R.string.do_you_want_to_insert_it_into_the_optical_drive_right_now),
                                                    activity.getString(R.string.ok),
                                                    activity.getString(R.string.cancel),
                                                    true,
                                                    R.drawable.check_24px,
                                                    true,
                                                    () -> QmpSender.changeOpticalDisc(activity, AppConfig.basefiledir + "virtio-win.iso", ""),
                                                    null,
                                                    null)
                                            );
                                        } else {
                                            Context context = VectrasApp.getContext();

                                            builder.setProgress(0, 0, false)
                                                    .setSmallIcon(R.drawable.check_24px)
                                                    .setContentText(context.getString(R.string.virtio_tools_for_windows_is_now_ready_to_use))
                                                    .setOngoing(false);

                                            manager.notify(notificationId, builder.build());
                                        }
                                    } else {
                                        FileUtils.delete(new File(AppConfig.basefiledir + "virtio-win.bin"));

                                        if (DialogUtils.isAllowShow(activity)) {
                                            activity.runOnUiThread(() -> DialogUtils.oopsDialog(activity, activity.getString(R.string.download_failed_note)));
                                        } else {
                                            Context context = VectrasApp.getContext();

                                            builder.setProgress(0, 0, false)
                                                    .setSmallIcon(R.drawable.error_96px)
                                                    .setContentText(context.getString(R.string.download_failed_note))
                                                    .setOngoing(false);

                                            manager.notify(notificationId, builder.build());
                                        }
                                    }
                                }
                            });
                        }).start(),
                        null,
                        null));
            } else {
                QmpSender.changeOpticalDisc(activity, AppConfig.basefiledir + "virtio-win.iso", "");
            }
        }).start();
    }
}
