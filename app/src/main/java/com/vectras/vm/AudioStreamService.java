package com.vectras.vm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.termux.app.TermuxService;
import com.vectras.qemu.Config;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class AudioStreamService extends Service {

    private static final String CHANNEL_ID = "AudioStreamServiceChannel";
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int PORT = 4713;

    private AudioTrack audioTrack;
    private boolean isPlaying = true;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Audio Streaming")
                .setContentText("Receiving audio stream...")
                .setSmallIcon(R.drawable.volume_up_24px)
                .build();
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int minBufSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
                audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT,
                        minBufSize,
                        AudioTrack.MODE_STREAM
                );

                audioTrack.play();

                new Thread(() -> {
                    try {
                    LocalSocket socket = new LocalSocket();
                    LocalSocketAddress address = new LocalSocketAddress(TermuxService.FILES_PATH + "/run/pulse/native",
                            LocalSocketAddress.Namespace.FILESYSTEM);

                        socket.connect(address);
                        InputStream in = socket.getInputStream();
                        byte[] buffer = new byte[4096];
                        int bytesRead;

                        while ((bytesRead = in.read(buffer)) != -1) {
                            // If raw PCM data
                            audioTrack.write(buffer, 0, bytesRead);
                        }

                        in.close();
                        socket.close();
                        Log.e("LocalSocket", "Running...");
                    } catch (Exception e) {
                        Log.e("LocalSocket", "Connection error", e);
                    }
                }).start();
            }
        }).start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isPlaying = false;
        audioTrack.stop();
        audioTrack.release();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Audio Stream Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
