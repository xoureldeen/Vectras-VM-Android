package com.vectras.vm;

import static android.content.Intent.ACTION_OPEN_DOCUMENT;
import static android.content.Intent.ACTION_VIEW;
import static android.view.View.GONE;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.BaseAdapter;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.termux.app.TermuxService;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.utils.CommandUtils;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.ListUtils;
import com.vectras.vm.utils.PackageUtils;
import com.vectras.vm.utils.UIUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class Minitools extends AppCompatActivity {

    private ArrayList<HashMap<String, String>> listmapForSelectMirrors = new ArrayList<>();
    private Spinner spinnerselectmirror;
    private String selectedMirrorCommand = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_minitools);
//        UIUtils.setOnApplyWindowInsetsListener(findViewById(R.id.main));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setTitle(getString(R.string.mini_tools));

        LinearLayout setupsoundfortermux = findViewById(R.id.setupsoundfortermux);
        LinearLayout cleanup = findViewById(R.id.cleanup);
        LinearLayout restore = findViewById(R.id.restore);
        LinearLayout deleteallvm = findViewById(R.id.deleteallvm);
        LinearLayout reinstallsystem = findViewById(R.id.reinstallsystem);
        LinearLayout deleteall = findViewById(R.id.deleteall);
        spinnerselectmirror = findViewById(R.id.spinnerselectmirror);

        setupsoundfortermux.setOnClickListener(v -> {
            if (PackageUtils.isInstalled("com.termux", getApplicationContext())) {
                DialogUtils.twoDialog(Minitools.this, getString(R.string.setup_sound), getResources().getString(R.string.setup_sound_guide_content), getString(R.string.start_setup), getString(R.string.cancel), true, R.drawable.volume_up_24px, true,
                        () -> {
                            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            ClipData clip = ClipData.newPlainText("Setup", "curl -o setup.sh https://raw.githubusercontent.com/AnBui2004/termux/refs/heads/main/installpulseaudio.sh && chmod +rwx setup.sh && ./setup.sh && rm setup.sh");
                            clipboard.setPrimaryClip(clip);
                            Intent intent = new Intent();
                            intent.setAction(ACTION_VIEW);
                            intent.setData(Uri.parse("android-app://com.termux"));
                            startActivity(intent);
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

        cleanup.setOnClickListener(v -> {
            DialogUtils.twoDialog(Minitools.this, getResources().getString(R.string.clean_up), getResources().getString(R.string.clean_up_content), getResources().getString(R.string.clean_up), getResources().getString(R.string.cancel), true, R.drawable.cleaning_services_24px, true,
                    () -> {
                        VMManager.cleanUp();
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.done), Toast.LENGTH_LONG).show();
                        restore.setVisibility(GONE);
                        cleanup.setVisibility(GONE);
                    }, null, null);
        });

        restore.setOnClickListener(v -> {
            DialogUtils.twoDialog(Minitools.this, getResources().getString(R.string.restore), getResources().getString(R.string.restore_content), getResources().getString(R.string.continuetext), getResources().getString(R.string.cancel), true, R.drawable.settings_backup_restore_24px, true,
                    () -> {
                        VMManager.restoreVMs();
                        UIUtils.oneDialog(getResources().getString(R.string.done), getResources().getString(R.string.restored) + " " + String.valueOf(VMManager.restoredVMs) + ".", true, false, Minitools.this);
                        restore.setVisibility(GONE);
                    }, null, null);
        });

        deleteallvm.setOnClickListener(v -> {
            DialogUtils.twoDialog(Minitools.this, getResources().getString(R.string.delete_all_vm), getResources().getString(R.string.delete_all_vm_content), getResources().getString(R.string.delete_all), getResources().getString(R.string.cancel), true, R.drawable.delete_24px, true,
                    () -> {
                        VMManager.killallqemuprocesses(getApplicationContext());
                        FileUtils.deleteDirectory(AppConfig.vmFolder);
                        FileUtils.deleteDirectory(AppConfig.recyclebin);
                        FileUtils.deleteDirectory(AppConfig.romsdatajson);
                        File vDir = new File(AppConfig.maindirpath);
                        vDir.mkdirs();
                        FileUtils.writeToFile(AppConfig.maindirpath, "roms-data.json", "[]");
                        cleanup.setVisibility(GONE);
                        restore.setVisibility(GONE);
                        deleteallvm.setVisibility(GONE);
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.done), Toast.LENGTH_LONG).show();
                    }, null, null);
        });

        deleteall.setOnClickListener(v -> {
            DialogUtils.twoDialog(Minitools.this, getResources().getString(R.string.delete_all), getResources().getString(R.string.delete_all_content), getResources().getString(R.string.delete_all), getResources().getString(R.string.cancel), true, R.drawable.delete_forever_24px, true,
                    () -> {
                        VMManager.killallqemuprocesses(getApplicationContext());
                        FileUtils.deleteDirectory(AppConfig.maindirpath);
                        File vDir = new File(AppConfig.maindirpath);
                        vDir.mkdirs();
                        FileUtils.writeToFile(AppConfig.maindirpath, "roms-data.json", "[]");
                        cleanup.setVisibility(GONE);
                        restore.setVisibility(GONE);
                        deleteallvm.setVisibility(GONE);
                        deleteall.setVisibility(GONE);
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.done), Toast.LENGTH_LONG).show();
                    }, null, null);
        });

        reinstallsystem.setOnClickListener(v -> {
            DialogUtils.twoDialog(Minitools.this, getResources().getString(R.string.reinstall_system), getResources().getString(R.string.reinstall_system_content), getResources().getString(R.string.continuetext), getResources().getString(R.string.cancel), true, R.drawable.system_update_24px, true,
                    () -> {
                        MainActivity.isActivate = false;
                        AppConfig.needreinstallsystem = true;
                        VMManager.killallqemuprocesses(Minitools.this);
                        FileUtils.deleteDirectory(getFilesDir().getAbsolutePath() + "/data");
                        FileUtils.deleteDirectory(getFilesDir().getAbsolutePath() + "/distro");
                        FileUtils.deleteDirectory(getFilesDir().getAbsolutePath() + "/usr");
                        Intent intent = new Intent();
                        intent.setClass(Minitools.this, SetupQemuActivity.class);
                        startActivity(intent);
                        finish();
                    }, null, null);
        });

        spinnerselectmirror.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMirrorCommand = listmapForSelectMirrors.get(position).get("mirror");
                MainSettingsManager.setSelectedMirror(Minitools.this, position);
                CommandUtils.run(selectedMirrorCommand + "&& exit", false, Minitools.this);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        setupSpiner();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case 0:
                return true;
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupSpiner() {
        ListUtils.setupMirrorListForListmap(listmapForSelectMirrors);

        spinnerselectmirror.setAdapter(new SpinnerSelectMirrorAdapter(listmapForSelectMirrors));
        spinnerselectmirror.setSelection(MainSettingsManager.getSelectedMirror(Minitools.this));
    }

    public class SpinnerSelectMirrorAdapter extends BaseAdapter {

        ArrayList<HashMap<String, String>> _data;

        public SpinnerSelectMirrorAdapter(ArrayList<HashMap<String, String>> _arr) {
            _data = _arr;
        }

        @Override
        public int getCount() {
            return _data.size();
        }

        @Override
        public HashMap<String, String> getItem(int _index) {
            return _data.get(_index);
        }

        @Override
        public long getItemId(int _index) {
            return _index;
        }

        @Override
        public View getView(final int _position, View _v, ViewGroup _container) {
            LayoutInflater _inflater = getLayoutInflater();
            View _view = _v;
            if (_view == null) {
                _view = _inflater.inflate(R.layout.simple_layout_for_spiner, null);
            }

            final TextView textViewLocation = _view.findViewById(R.id.textViewLocation);

            textViewLocation.setText(_data.get((int) _position).get("location"));

            return _view;
        }
    }

    public void extractLoaderApk() {
        String apkLoaderAssetPath = "bootstrap/loader.apk";
        String apkLoaderextractedFilePath = TermuxService.PREFIX_PATH + "/libexec/termux-x11/loader.apk";

        FileUtils.deleteDirectory(apkLoaderextractedFilePath);
        if (copyAssetToFile(apkLoaderAssetPath, apkLoaderextractedFilePath)) {
            FileUtils.copyAFile(TermuxService.PREFIX_PATH + "/libexec/termux-x11/loader.apk", AppConfig.maindirpath);
        }
    }

    private boolean copyAssetToFile(String assetPath, String outputPath) {
        try (InputStream in = getAssets().open(assetPath);
             OutputStream out = new FileOutputStream(outputPath)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}