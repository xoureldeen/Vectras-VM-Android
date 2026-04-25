package com.vectras.vm.creator;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.Menu;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.R;
import com.vectras.vm.databinding.ActivitySetArchBinding;
import com.vectras.vm.main.MainActivity;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.IntentUtils;
import com.vectras.vm.utils.UIUtils;

import java.io.File;

public class SetArchActivity extends AppCompatActivity implements View.OnClickListener {

    SetArchActivity activity;
    ActivitySetArchBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.edgeToEdge(this);
        binding = ActivitySetArchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
//        UIUtils.setOnApplyWindowInsetsListener(findViewById(R.id.main));
        activity = this;
        binding.archi386.setOnClickListener(this);
        binding.archx8664.setOnClickListener(this);
        binding.archarm64.setOnClickListener(this);
        binding.archppc.setOnClickListener(this);
        binding.webBtn.setOnClickListener(this);
        binding.buttongetcm.setOnClickListener(this);
        binding.bntimport.setOnClickListener(this);

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.roms_store) {
                MainActivity.isOpenRomStore = true;
                finish();
                return true;
            }
            return false;
        });

//        if (PackageUtils.isInstalled("com.anbui.cqcm.app", this)) {
//            binding.buttongetcm.setText(getResources().getString(R.string.open));
//        }

        binding.bntimport.setOnDragListener((v, event) -> {
            Log.i("Drag", "onDrag: " + event.getAction());
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    ClipDescription description = event.getClipDescription();
                    if (description != null) {
                        Log.d("DRAG", "MIME: " + description.getMimeType(0));
                        return true; // Accept to go to event DragEvent.ACTION_DROP
                    }
                    return false;

                case DragEvent.ACTION_DROP:
                    ClipData clipData = event.getClipData();
                    if (clipData != null && clipData.getItemCount() > 0) {
                        Uri uri = clipData.getItemAt(0).getUri();
                        String filePath = FileUtils.getFilePathFromUri(getApplicationContext(), uri);

                        File file = new File(filePath);

                        Intent intent = new Intent();
                        intent.setClass(getApplicationContext(), VMCreatorActivity.class);
                        intent.putExtra("addromnow", "");
                        intent.putExtra("romextra", "");
                        intent.putExtra("romname", "");
                        intent.putExtra("romicon", "");
                        intent.putExtra("rompath", filePath);
                        intent.putExtra("romfilename", file.getName());
                        startActivity(intent);
                        finish();
                    }
                    return true;
            }
            return true;
        });

    }

    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.archi386) {
            MainSettingsManager.setArchI386(this);

            startActivity(new Intent(activity, VMCreatorActivity.class));
            finish();
        } else if (id == R.id.archx86_64) {
            MainSettingsManager.setArchX86_64(this);

            startActivity(new Intent(activity, VMCreatorActivity.class));
            finish();
        } else if (id == R.id.archarm64) {
            MainSettingsManager.setArchArm64(this);

            startActivity(new Intent(activity, VMCreatorActivity.class));
            finish();
        } else if (id == R.id.archppc) {
            MainSettingsManager.setArchPpc(this);

            startActivity(new Intent(activity, VMCreatorActivity.class));
            finish();
        } else if (id == R.id.webBtn) {
            IntentUtils.openUrl(this, "https://www.qemu.org/");
        } else if (id == R.id.buttongetcm) {
            IntentUtils.openApp(this, "com.anbui.cqcm.app");
        } else if (id == R.id.bntimport) {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), VMCreatorActivity.class);
            intent.putExtra("importcvbinow", "");
            startActivity(intent);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.set_arch_toolbar_menu, menu);
        return true;
    }
}