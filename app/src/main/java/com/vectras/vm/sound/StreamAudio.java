package com.vectras.vm.sound;

import android.app.Activity;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

import com.vectras.vm.utils.FileUtils;

import java.io.RandomAccessFile;

public class StreamAudio {
    private final String TAG = "StreamAudio";
    private Context context;
    private boolean isPlay;
    private String filePath = "";
    private int sampleRate = 48000;
    public boolean isDestroyed;
    private StreamAudio cross;
    AudioTrack audioTrack;
    private SoundEffect soundEffect;

    public StreamAudio(Context context) {
        this.context = context;
    }

    public boolean isPlaying() {
        return isPlay;
    }

    public void stop() {
        if (audioTrack != null) audioTrack.stop();
        isPlay = false;
    }

    public void play() {
        if (!filePath.isEmpty()) streamFromFile();
    }

    public void setFile(String path) {
        filePath = path;
    }

    public String getFile() {
        return filePath;
    }

    public void setCross(StreamAudio streamAudio) {
        cross = streamAudio;
    }

    public void setMinimumSampleRate() {
        sampleRate = 44100;
    }

    public void setHighSampleRate() {
        sampleRate = 48000;
    }

    float volume = 1f;

    public void setVolume(float volume) {
        this.volume = volume / 100;
        if (audioTrack != null) audioTrack.setVolume(this.volume);
    }

    float nextPlayVolume = -1;

    public void setNextPlayVolume(float volume) {
        this.volume = volume / 100;
    }

    public float getVolume() {
        return volume * 100;
    }


    public void release() {
        isPlay = false;
        releaseSoundEffect();
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

            if (minBuf <= 0) {
                isPlay = false;
                Log.d(TAG, "Failed to get min buffer size");
                return;
            }

            audioTrack = new AudioTrack.Builder()
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

            audioTrack.setVolume(nextPlayVolume < 0 ? volume : nextPlayVolume);

            nextPlayVolume = -1;

            applyEffect(audioTrack);

            audioTrack.play();

            if (cross != null && !cross.isDestroyed && cross.isPlaying()) cross.stop();

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

            if (cross != null && !cross.isDestroyed && !cross.isPlaying()) cross.play();
        }).start();
    }

    float[] currentdBData = new float[5];

    public void applyEffect(AudioTrack audioTrack) {
        if (soundEffect != null && soundEffect.equalizer.getNumberOfBands() < 2) return;

        AudioSettingsData audioSettingsData = new AudioSettingsData(context);

        if (!audioSettingsData.isEqualizerEnabled()) {
            releaseSoundEffect();
            return;
        }

        if (soundEffect == null || soundEffect.session != audioTrack.getAudioSessionId())
            soundEffect = new SoundEffect(context, audioTrack.getAudioSessionId());

        if (soundEffect.equalizer.getNumberOfBands() < 2) return;

        float[] dBData = new float[5];

        //Bass -> Mid -> Treble
        dBData[0] = audioSettingsData.getLowBass();
        dBData[1] = audioSettingsData.getBass();
        dBData[2] = audioSettingsData.getMid();
        dBData[3] = audioSettingsData.getTreble();
        dBData[4] = audioSettingsData.getUpperTreble();

        if (
                currentdBData[0] == dBData[0] &&
                        currentdBData[1] == dBData[1] &&
                        currentdBData[2] == dBData[2] &&
                        currentdBData[3] == dBData[3] &&
                        currentdBData[4] == dBData[4]) {
            return;
        }

        this.currentdBData = dBData;

        // Convert dB to mB
        for (int i = 0; i < dBData.length; i++) {
            dBData[i] = dBData[i] * 100;
        }

        soundEffect.applyEffect(dBData);

        soundEffect.setEnabled(audioSettingsData.isEqualizerEnabled());

        Log.d(TAG, "Equalizer enabled: " + audioSettingsData.isEqualizerEnabled());
    }

    public void releaseSoundEffect() {
        if (soundEffect != null) {
            soundEffect.setEnabled(false);
            soundEffect.release();
            soundEffect = null;
        }
    }

    private boolean isContextDestroyed(Context context) {
        if (context instanceof Activity activity) {
            isDestroyed = activity.isDestroyed() || activity.isFinishing();
            return isDestroyed;
        }
        return false;
    }
}
