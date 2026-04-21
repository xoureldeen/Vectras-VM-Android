package com.vectras.vm.utils;

import android.app.Activity;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

import java.io.RandomAccessFile;

public class StreamAudio {
    private final String TAG = "StreamAudio";
    private Context context;
    private boolean isPlay;
    private String filePath = "";
    private int sampleRate = 48000;

    public StreamAudio(Context context) {
        this.context = context;
    }

    public boolean isPlaying() {
        return isPlay;
    }

    public void stop() {
        isPlay = false;
    }

    public void play() {
        if (!filePath.isEmpty()) streamFromFile();
    }

    public void setFile(String path) {
        filePath = path;
    }

    public void setMinimumSampleRate() {
        sampleRate = 44100;
    }

    public void setHighSampleRate() {
        sampleRate = 48000;
    }


    public void streamFromFile() {
        if (isPlay) return;

        isPlay = true;

        new Thread(() -> {
            Log.d(TAG, "Play: " + filePath);

            while (isPlay && !FileUtils.isFileExists(filePath)) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    isPlay = false;
                    Log.d(TAG, "Failed to wait for: " + filePath);
                    return;
                }
            }

            if (!isPlay) return;

            Log.d(TAG, "Preparing to play: " + filePath);

            int minBuf = AudioTrack.getMinBufferSize(sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT);

            AudioTrack audioTrack = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .build())
                    .setBufferSizeInBytes(minBuf * 4)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();

            audioTrack.play();

            try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
                byte[] buffer = new byte[minBuf];
                long lastPos = raf.length();

                Log.d(TAG, "Playing: " + filePath);

                while (isPlay && !isContextDestroyed(context)) {
                    long fileSize = raf.length();

                    if (fileSize > lastPos) {
                        raf.seek(lastPos);
                        int bytesRead = raf.read(buffer);
                        if (bytesRead > 0) {
                            audioTrack.write(buffer, 0, bytesRead);
                            lastPos += bytesRead;
                        }
                    } else {
                        Thread.sleep(10);
                    }
                }
            } catch (Exception e) {
                isPlay = false;
                Log.e(TAG, e.getMessage());
            }
        }).start();
    }

    private boolean isContextDestroyed(Context context) {
        if (context instanceof Activity activity) {
            return activity.isDestroyed() || activity.isFinishing();
        }
        return false;
    }
}
