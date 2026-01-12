package com.vectras.vm;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.vectras.vm.utils.NotificationUtils;

import java.util.Map;

public class FCMService
        extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {

        if (message.getNotification() != null) {
            String title = message.getNotification().getTitle();
            String body = message.getNotification().getBody();
            String image = message.getNotification().getImageUrl() != null ? message.getNotification().getImageUrl().toString() : null;

            Map<String, String> data = message.getData();

            NotificationUtils.pushNow(this,
                    1,
                    NotificationUtils.generalChannelId,
                    title,
                    body,
                    R.drawable.ic_vectras_vm_48,
                    image,
                    -1,
                    data.get("url"),
                    null);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {

    }
}
