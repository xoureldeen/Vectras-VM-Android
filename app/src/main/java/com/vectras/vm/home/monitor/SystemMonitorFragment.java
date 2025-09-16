package com.vectras.vm.home.monitor;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.os.Build.VERSION.SDK_INT;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.transition.MaterialFadeThrough;
import com.vectras.qemu.Config;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.R;
import com.vectras.vm.VMManager;
import com.vectras.vm.databinding.FragmentHomeSystemMonitorBinding;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vterm.Terminal;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SystemMonitorFragment extends Fragment {
    final String TAG = "SystemMonitorFragment";
    FragmentHomeSystemMonitorBinding binding;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    boolean isStopUpdateMonitor = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialFadeThrough());
        setReturnTransition(new MaterialFadeThrough());
        setExitTransition(new MaterialFadeThrough());
        setReenterTransition(new MaterialFadeThrough());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentHomeSystemMonitorBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        startMonitor();
        getQemuInfo();
        initialize();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(isStopUpdateMonitor) {
            startMonitor();
            getQemuInfo();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopMonitor();
    }

    private void initialize() {
        binding.btStopqemu.setOnClickListener(v -> VMManager.requestKillAllQemuProcess(requireActivity(), () -> {
            ScheduledExecutorService executorUpdate = Executors.newSingleThreadScheduledExecutor();
            executorUpdate.schedule(() -> {
                if (getContext() != null) {
                    requireActivity().runOnUiThread(this::getQemuInfo);
                }
            }, 500, TimeUnit.MILLISECONDS);
        }));

        binding.btStopvmvnc.setOnClickListener(v -> {
            if (Config.currentVNCServervmID.isEmpty()) {
                binding.btStopvmvnc.setVisibility(View.GONE);
            } else {
                DialogUtils.threeDialog(
                        requireActivity(),
                        getString(R.string.shutdown),
                        getString(R.string.shutdown_or_reset_content),
                        getString(R.string.shutdown),
                        getString(R.string.cancel),
                        getString(R.string.reset),
                        true,
                        R.drawable.power_settings_new_24px,
                        true,
                        () -> {
                            Config.vmID = Config.currentVNCServervmID;
                            VMManager.shutdownCurrentVM();
                            Config.currentVNCServervmID = "";
                            binding.btStopvmvnc.setVisibility(View.GONE);
                            getQemuInfo();
                        },
                        null,
                        () -> {
                            Config.vmID = Config.currentVNCServervmID;
                            VMManager.resetCurrentVM();
                        },
                        null);
            }
        });
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable monitorTask = new Runnable() {
        @Override
        public void run() {
            if (isStopUpdateMonitor) return;
            updateSystemMonitor();
            handler.postDelayed(this, 1000);
        }
    };

    private void startMonitor() {
        isStopUpdateMonitor = false;
        handler.post(monitorTask);
    }

    private void stopMonitor() {
        isStopUpdateMonitor = true;
        handler.removeCallbacks(monitorTask);
    }

    @SuppressLint("SetTextI18n")
    private void updateSystemMonitor() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) requireActivity().getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);

        double totalMemory = mi.totalMem * Math.pow(10, -9);
        double freeMemory = mi.availMem * Math.pow(10, -9);
        double usedMemory = totalMemory - freeMemory;
        double percentageOfRam = (100 / totalMemory) * usedMemory;

        binding.tvTotalram.setText(getString(R.string.total_memory) + " " + String.format(Locale.US, "%.1f", totalMemory) + " GB");
        binding.tvUsedram.setText(getString(R.string.used_memory)  + " " + String.format(Locale.US, "%.1f", usedMemory) + " GB");
        binding.tvPercentageofram.setText((int) percentageOfRam + "%");
        if (SDK_INT >= Build.VERSION_CODES.N) {
            binding.cpiMemory.setProgress((int) percentageOfRam, true);
        } else {
            binding.cpiMemory.setProgress((int) percentageOfRam);
        }


        StatFs externalStatFs = new StatFs( Environment.getExternalStorageDirectory().getAbsolutePath() );

        double totalStorage = (externalStatFs.getBlockCountLong() * externalStatFs.getBlockSizeLong()) * Math.pow(10, -9);
        double freeStorage = (externalStatFs.getAvailableBlocksLong() * externalStatFs.getBlockSizeLong()) * Math.pow(10, -9);
        double usedStorage = totalStorage - freeStorage;
        double percentageOfStorage = (100 / totalStorage) * usedStorage;

        binding.tvTotalstorage.setText(getString(R.string.total_memory) + " " + String.format(Locale.US, "%.1f", totalStorage) + " GB");
        binding.tvUsedstorage.setText(getString(R.string.used_memory)  + " " + String.format(Locale.US, "%.1f", usedStorage) + " GB");
        binding.tvPercentageofstorage.setText((int) percentageOfStorage + "%");
        if (SDK_INT >= Build.VERSION_CODES.N) {
            binding.cpiStorage.setProgress((int) percentageOfStorage, true);
        } else {
            binding.cpiStorage.setProgress((int) percentageOfStorage);
        }
    }

    @SuppressLint("SetTextI18n")
    private void getQemuInfo() {
        String currentArch = MainSettingsManager.getArch(requireActivity());
        String command = "qemu-system-x86_64 --version";
        new Terminal(requireActivity()).extractQemuVersion(command, false, requireActivity(), (output, errors) -> {
            if (errors.isEmpty()) {
                binding.tvQemuversion.setText(getString(R.string.version) + " " + (output.equals("8.2.1") ? output + " - 3dfx" : getString(R.string.unknow)) + ".");
            } else {
                Log.e(TAG, "Errors: " + errors);
            }
        });

        binding.tvQemuarch.setText(getString(R.string.arch) + " " + currentArch + ".");

        executor.execute(() -> {
            String result = Terminal.executeShellCommandWithResult("ps -e", requireActivity());
            requireActivity().runOnUiThread(() -> {
                if (!result.isEmpty()) {
                    switch (currentArch) {
                        case "X86_64" ->
                                binding.tvQemustatus.setText(getString(R.string.status_qemu) + " " + (result.contains("qemu-system-x86_64 -qmp") ? getString(R.string.running) : getString(R.string.stopped)) + ".");
                        case "I386" ->
                                binding.tvQemustatus.setText(getString(R.string.status_qemu) + " " + (result.contains("qemu-system-i386 -qmp") ? getString(R.string.running) : getString(R.string.stopped)) + ".");
                        case "ARM64" ->
                                binding.tvQemustatus.setText(getString(R.string.status_qemu) + " " + (result.contains("qemu-system-aarch64 -qmp") ? getString(R.string.running) : getString(R.string.stopped)) + ".");
                        case "PPC" ->
                                binding.tvQemustatus.setText(getString(R.string.status_qemu) + " " + (result.contains("qemu-system-ppc -qmp") ? getString(R.string.running) : getString(R.string.stopped)) + ".");
                        default -> binding.tvQemustatus.setText(getString(R.string.status_qemu) + " " + getString(R.string.stopped) + ".");
                    }

                    if (result.contains("qemu-system") && result.contains("-qmp")) {
                        binding.btStopqemu.setVisibility(View.VISIBLE);
                    } else {
                        binding.btStopqemu.setVisibility(View.GONE);
                    }
                } else {
                    binding.tvQemustatus.setText(getString(R.string.status_qemu) + " " + getString(R.string.stopped) + ".");
                    binding.btStopqemu.setVisibility(View.GONE);
                    Log.i(TAG, "Errors: " + result);
                }

                getVNCServerStatus(result);
            });
        });
    }

    @SuppressLint("SetTextI18n")
    private void getVNCServerStatus(String resultCommand) {
        binding.tvVncport.setText(getString(R.string.port_qemu) + " " + (Integer.parseInt(MainSettingsManager.getVncExternalDisplay(requireActivity())) + 5900) + ".");

        if (resultCommand.contains(Config.defaultVNCHost + ":" + Config.defaultVNCPort)) {
            binding.tvVncstatus.setText(getString(R.string.status_qemu) + " " + getString(R.string.running) + ".");
            binding.btStopvmvnc.setVisibility(View.VISIBLE);
        } else {
            binding.tvVncstatus.setText(getString(R.string.status_qemu) + " " + getString(R.string.stopped) + ".");
            binding.btStopvmvnc.setVisibility(View.GONE);
        }
    }
}