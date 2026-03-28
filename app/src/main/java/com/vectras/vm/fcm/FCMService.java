package com.vectras.vm.fcm;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.vectras.vm.R;
import com.vectras.vm.utils.NotificationUtils;

import java.util.Map;
import java.util.Objects;

public class FCMService
        extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Map<String, String> data = message.getData();

        if (data.get("targetVersions") != null
                && !Objects.requireNonNull(data.get("targetVersions")).isEmpty()
                && !Objects.requireNonNull(data.get("targetVersions")).contains(getString(R.string.app_version))) {
            return;
        }

        NotificationUtils.pushNow(this,
                1,
                NotificationUtils.generalChannelId,
                data.get("title") != null ? data.get("title") : getString(R.string.new_notification),
                data.get("message") != null ? data.get("message") : getString(R.string.tap_to_view),
                R.drawable.ic_vectras_vm_48,
                data.get("image") != null ? data.get("image") : null,
                -1,
                data.get("url") != null ? data.get("url") : null,
                null);
    }

    @Override
    public void onNewToken(@NonNull String token) {

    }
}
