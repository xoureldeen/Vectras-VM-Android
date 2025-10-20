// only needed to capture kill of app, here used to get rid off all notifications
package au.com.darkside.xdemo;

import android.app.Service;
import android.app.NotificationManager;
import android.app.Notification;
import android.content.Intent;
import android.os.IBinder;

import android.content.Context;

public class XServerService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancelAll();
    }
}