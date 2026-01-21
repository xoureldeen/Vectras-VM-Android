package com.vectras.vm;

import static android.content.Intent.ACTION_VIEW;
import static android.view.View.GONE;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.creator.VMCreatorSelector;
import com.vectras.vm.databinding.ActivityMinitoolsBinding;
import com.vectras.vm.main.MainActivity;
import com.vectras.vm.setupwizard.SetupWizard2Activity;
import com.vectras.vm.utils.CommandUtils;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.ListUtils;
import com.vectras.vm.utils.PackageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class Minitools extends AppCompatActivity {
    private final String TAG = "Minitools";
    private ActivityMinitoolsBinding binding;
    private final ArrayList<HashMap<String, Object>> mirrorlist = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMinitoolsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
//        UIUtils.setOnApplyWindowInsetsListener(findViewById(R.id.main));

        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        binding.toolbar.setTitle(getString(R.string.mini_tools));

        binding.setupsoundfortermux.setOnClickListener(v -> {
            if (PackageUtils.isInstalled("com.termux", getApplicationContext())) {
                DialogUtils.twoDialog(Minitools.this, getString(R.string.setup_sound), getResources().getString(R.string.setup_sound_guide_content), getString(R.string.start_setup), getString(R.string.cancel), true, R.drawable.volume_up_24px, true,
                        () -> {
                            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("Setup", "curl -o setup.sh https://raw.githubusercontent.com/AnBui2004/termux/refs/heads/main/installpulseaudio.sh && chmod +rwx setup.sh && ./setup.sh && rm setup.sh");
                            clipboard.setPrimaryClip(clip);
                            Intent intent = getPackageManager()
                                    .getLaunchIntentForPackage("com.termux");

                            if (intent != null) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.copied), Toast.LENGTH_LONG).show();
                        }, null, null);
            } else {
                DialogUtils.twoDialog(Minitools.this, getString(R.string.termux_is_not_installed), getResources().getString(R.string.you_need_to_install_termux), getString(R.string.install), getString(R.string.cancel), true, R.drawable.arrow_downward_24px, true,
                        () -> {
                            Intent intent = new Intent();
                            intent.setAction(ACTION_VIEW);
                            intent.setData(Uri.parse("https://github.com/termux/termux-app/releases"));
                            startActivity(intent);
                        }, null, null);
            }

        });

        binding.mirror.setOnClickListener(v -> VMCreatorSelector.showDialog(Minitools.this, mirrorlist, MainSettingsManager.getSelectedMirror(this), ((position, name, value) -> {
            MainSettingsManager.setSelectedMirror(Minitools.this, position);
            CommandUtils.run(value + "&& exit", false, Minitools.this);
        }), getString(R.string.mirrors)));

        binding.cleanup.setOnClickListener(v -> DialogUtils.twoDialog(Minitools.this, getResources().getString(R.string.clean_up), getResources().getString(R.string.clean_up_content), getResources().getString(R.string.clean_up), getResources().getString(R.string.cancel), true, R.drawable.cleaning_services_24px, true,
                this::cleanUp, null, null));

        binding.restore.setOnClickListener(v -> DialogUtils.twoDialog(Minitools.this, getResources().getString(R.string.restore), getResources().getString(R.string.restore_content), getResources().getString(R.string.continuetext), getResources().getString(R.string.cancel), true, R.drawable.settings_backup_restore_24px, true,
                () -> {
                    int result = VMManager.restoreAll();
                    DialogUtils.oneDialog(Minitools.this, getString(R.string.done), getString(R.string.restored) + " " + result + ".", R.drawable.settings_backup_restore_24px);
                    binding.restore.setVisibility(GONE);
                }, null, null));

        binding.deleteallvm.setOnClickListener(v -> DialogUtils.twoDialog(Minitools.this, getResources().getString(R.string.delete_all_vm), getResources().getString(R.string.delete_all_vm_content), getResources().getString(R.string.delete_all), getResources().getString(R.string.cancel), true, R.drawable.delete_24px, true,
                this::eraserAllVM, null, null));

        binding.deleteall.setOnClickListener(v -> DialogUtils.twoDialog(Minitools.this, getResources().getString(R.string.delete_all), getResources().getString(R.string.delete_all_content), getResources().getString(R.string.delete_all), getResources().getString(R.string.cancel), true, R.drawable.delete_forever_24px, true,
                this::eraserData, null, null));

        binding.reinstallsystem.setOnClickListener(v -> DialogUtils.twoDialog(Minitools.this, getResources().getString(R.string.reinstall_system), getResources().getString(R.string.reinstall_system_content), getResources().getString(R.string.continuetext), getResources().getString(R.string.cancel), true, R.drawable.system_update_24px, true,
                this::eraserSystem, null, null));

        ListUtils.setupMirrorListForListmap(mirrorlist);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return switch (item.getItemId()) {
            case 0 -> true;
            case android.R.id.home -> {
                finish();
                yield true;
            }
            default -> super.onOptionsItemSelected(item);
        };
    }

    private void cleanUp() {
        View progressView = LayoutInflater.from(this).inflate(R.layout.dialog_progress_style, null);
        TextView progress_text = progressView.findViewById(R.id.progress_text);
        progress_text.setText(getString(R.string.just_a_moment));
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(this, R.style.CenteredDialogTheme)
                .setView(progressView)
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            int result = VMManager.cleanUp();

            runOnUiThread(() -> {
                progressDialog.dismiss();
                DialogUtils.twoDialog(
                        this,
                        getString(R.string.done),
                        result + " " + getString(R.string.items_have_been_cleared),
                        getString(R.string.show_recycle_bin),
                        getString(R.string.close),
                        true,
                        R.drawable.cleaning_services_24px,
                        true,
                        () -> {
                            FileUtils.createDirectory(AppConfig.recyclebin);
                            FileUtils.openFolder(this, AppConfig.recyclebin);
                        },
                        null,
                        null);
                binding.restore.setVisibility(GONE);
                binding.cleanup.setVisibility(GONE);
            });
        }).start();
    }

    private void eraserAllVM() {
        View progressView = LayoutInflater.from(this).inflate(R.layout.dialog_progress_style, null);
        TextView progress_text = progressView.findViewById(R.id.progress_text);
        progress_text.setText(getString(R.string.just_a_moment));
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(this, R.style.CenteredDialogTheme)
                .setView(progressView)
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            VMManager.killallqemuprocesses(this);
            FileUtils.delete(new File(AppConfig.vmFolder));
            FileUtils.delete(new File(AppConfig.recyclebin));
            FileUtils.delete(new File(AppConfig.romsdatajson));
            File vDir = new File(AppConfig.maindirpath);
            if (!vDir.mkdirs()) Log.e(TAG, "Unable to create folder: " + AppConfig.maindirpath);
            FileUtils.writeToFile(AppConfig.maindirpath, "roms-data.json", "[]");

            runOnUiThread(() -> {
                progressDialog.dismiss();
                binding.cleanup.setVisibility(GONE);
                binding.restore.setVisibility(GONE);
                binding.deleteallvm.setVisibility(GONE);
                Toast.makeText(this, getResources().getString(R.string.done), Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    private void eraserData() {
        View progressView = LayoutInflater.from(this).inflate(R.layout.dialog_progress_style, null);
        TextView progress_text = progressView.findViewById(R.id.progress_text);
        progress_text.setText(getString(R.string.just_a_moment));
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(this, R.style.CenteredDialogTheme)
                .setView(progressView)
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            VMManager.killallqemuprocesses(this);
            FileUtils.delete(new File(AppConfig.maindirpath));
            File vDir = new File(AppConfig.maindirpath);
            if (!vDir.mkdirs()) Log.e(TAG, "Unable to create folder: " + AppConfig.maindirpath);
            FileUtils.writeToFile(AppConfig.maindirpath, "roms-data.json", "[]");

           runOnUiThread(() -> {
                progressDialog.dismiss();
                binding.cleanup.setVisibility(GONE);
                binding.restore.setVisibility(GONE);
                binding.deleteallvm.setVisibility(GONE);
                binding.deleteall.setVisibility(GONE);
                Toast.makeText(this, getResources().getString(R.string.done), Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    private void eraserSystem() {
        View progressView = LayoutInflater.from(this).inflate(R.layout.dialog_progress_style, null);
        TextView progress_text = progressView.findViewById(R.id.progress_text);
        progress_text.setText(getString(R.string.just_a_moment));
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(this, R.style.CenteredDialogTheme)
                .setView(progressView)
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            MainActivity.isActivate = false;
            AppConfig.needreinstallsystem = true;
            VMManager.killallqemuprocesses(this);
            FileUtils.delete(new File(getFilesDir().getAbsolutePath() + "/data"));
            FileUtils.delete(new File(getFilesDir().getAbsolutePath() + "/distro"));
            FileUtils.delete(new File(getFilesDir().getAbsolutePath() + "/usr"));

            runOnUiThread(() -> {
                progressDialog.dismiss();
                Intent intent = new Intent();
                intent.setClass(this, SetupWizard2Activity.class);
                startActivity(intent);
                finishAffinity();
            });
        }).start();
    }
}