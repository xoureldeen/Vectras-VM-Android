package com.vectras.vm.sound;

import android.content.Context;
import android.media.audiofx.Equalizer;
import android.util.Log;

public class SoundEffect {
    final  String TAG = "SoundEffect";

    Context context;
    public int session;
    public Equalizer equalizer;

    public SoundEffect(Context context, int session) {
        this.context = context;
        this.session = session;

         equalizer = new Equalizer(1000, session);
    }

    public AudioFrequencyData getDeviceAudioFrequencies() {
        AudioFrequencyData data = new AudioFrequencyData();

        int[] targets = { 60, 230, 910, 4000, 14000 };

        short bands = equalizer.getNumberOfBands();
        int[] hzList = new int[bands];

        for (short i = 0; i < bands; i++) {
            int center = equalizer.getCenterFreq(i);
            hzList[i] = center / 1000;
        }

        if (bands <= targets.length) {
            int[] finalIds = new int[bands];
            for (int i = 0; i < bands; i++) {
                finalIds[i] = i;
            }

            data.frequencies = hzList;
            data.ids = finalIds;

            Log.d(TAG, "Equalizer bands: " + bands);
            Log.d(TAG, "Equalizer frequencies: " + hzList[0] + ", " + hzList[1] + ", " + hzList[2] + ", " + hzList[3] + ", " + hzList[4]);
            Log.d(TAG, "Equalizer ids: " + finalIds[0] + ", " + finalIds[1] + ", " + finalIds[2] + ", " + finalIds[3] + ", " + finalIds[4]);

            return data;
        }

        int[] finalHzList = new int[5];
        int[] finalIds = new int[5];

        for (int i = 0; i < targets.length; i++) {
            int losest = Integer.MAX_VALUE;
            int bandId = 0;
            for (int ii = 0; ii < hzList.length; ii++) {
                if (Math.abs(targets[i] - hzList[ii]) < losest) {
                    losest = hzList[ii];
                    bandId = ii;
                }

                finalHzList[i] = losest;
                finalIds[i] = bandId;
            }
        }

        data.frequencies = finalHzList;
        data.ids = finalIds;

        Log.d(TAG, "Equalizer bands: " + bands);
        Log.d(TAG, "Equalizer frequencies: " + hzList[0] + ", " + hzList[1] + ", " + hzList[2] + ", " + hzList[3] + ", " + hzList[4]);
        Log.d(TAG, "Equalizer ids: " + finalIds[0] + ", " + finalIds[1] + ", " + finalIds[2] + ", " + finalIds[3] + ", " + finalIds[4]);

        return data;
    }

    public void applyEffect(float[] dBData) {
        short bands = equalizer.getNumberOfBands();

        if (bands < 2) {
            Log.d(TAG, "Equalizer bands: " + bands);
            return;
        }

        AudioFrequencyData data = getDeviceAudioFrequencies();

        equalizer.setBandLevel((short) data.ids[0], (short) dBData[0]);
        equalizer.setBandLevel((short) data.ids[1], (short) dBData[1]);
        if (bands > 2) equalizer.setBandLevel((short) data.ids[2], (short) dBData[2]);
        if (bands > 3) equalizer.setBandLevel((short) data.ids[3], (short) dBData[3]);
        if (bands > 4) equalizer.setBandLevel((short) data.ids[4], (short) dBData[4]);

        Log.d(TAG, "Equalizer dB: " + dBData[0] + ", " + dBData[1] + ", " + dBData[2] + ", " + dBData[3] + ", " + dBData[4]);
    }

    public void release() {
        if (equalizer != null) {
            equalizer.release();
        }
    }

    public void setEnabled(boolean isEnabled) {
        equalizer.setEnabled(isEnabled);
    }

    public short getMinBandLevelRange() {
        return equalizer.getBandLevelRange()[0];
    }

    public short getMaxBandLevelRange() {
        return equalizer.getBandLevelRange()[1];
    }

    public static class AudioFrequencyData {
        int[] frequencies;
        int[] ids;
    }
}
