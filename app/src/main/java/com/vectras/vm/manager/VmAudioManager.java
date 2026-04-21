package com.vectras.vm.manager;

import android.util.Log;

import com.vectras.qemu.Config;
import com.vectras.vm.VectrasApp;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.StreamAudio;

public class VmAudioManager {
    private static final String TAG = "VmAudioManager";
    public static final StreamAudio streamAudio = new StreamAudio(VectrasApp.getContext());

    public static void stream(String vmID) {
        if (streamAudio.isPlaying()) streamAudio.stop();

        streamAudio.setFile(VmFileManager.getAudioRaw(VectrasApp.getContext(), vmID));
        streamAudio.play();

        new Thread(() -> {
            while (FileUtils.isFileExists(Config.getLocalQMPSocketPath(vmID))) {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) { return; }
            }

            Log.d(TAG, "Stoped.");
            streamAudio.stop();
        }).start();
    }
}
