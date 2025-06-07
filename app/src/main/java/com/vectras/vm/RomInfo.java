package com.vectras.vm;

import static android.content.Intent.ACTION_OPEN_DOCUMENT;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.UIUtils;

import java.io.File;
import java.util.Objects;

public class RomInfo extends AppCompatActivity {

    public static boolean isFinishNow = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_rom_info);
        UIUtils.setOnApplyWindowInsetsListener(findViewById(R.id.main));

        ImageView ivIcon;
        TextView textName;
        TextView textSize;
        Button btn_download;
        Button btn_pick;
        TextView descTxt;

        ivIcon = findViewById(R.id.ivIcon);
        textName = findViewById(R.id.textName);
        textSize = findViewById(R.id.textSize);
        btn_download = findViewById(R.id.btn_download);
        btn_pick = findViewById(R.id.btn_pick);
        descTxt = findViewById(R.id.descTxt);

        btn_download.setOnClickListener(v -> {
            Intent openurl = new Intent();
            openurl.setAction(Intent.ACTION_VIEW);
            openurl.setData(Uri.parse(getIntent().getStringExtra("getrom")));
            startActivity(openurl);
        });

        btn_pick.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS);
                startActivityForResult(intent, 0);
            }
        });

        if (getIntent().hasExtra("title")) {
            textName.setText(getIntent().getStringExtra("title"));
        }
        if (getIntent().hasExtra("shortdesc")) {
            textSize.setText(getIntent().getStringExtra("shortdesc"));
        }
        if (getIntent().hasExtra("desc")) {
            descTxt.setText(getIntent().getStringExtra("desc"));
        }

        if (getIntent().hasExtra("icon")) {
            Glide.with(this).load(getIntent().getStringExtra("icon")).into(ivIcon);
        }

//        btn_pick.setText(getString(R.string.select) + " " + getIntent().getStringExtra("filename"));
    }

    public void onResume() {
        super.onResume();
        if (isFinishNow)
            finish();
        isFinishNow = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == RESULT_OK) {
            Uri content_describer = data.getData();
            File selectedFilePath = new File(getPath(content_describer));
            if (selectedFilePath.getName().equals(getIntent().getStringExtra("filename")) || (selectedFilePath.getName().endsWith(".cvbi.zip") && selectedFilePath.getName().equals(getIntent().getStringExtra("filename") + ".zip"))) {
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), CustomRomActivity.class);
                intent.putExtra("addromnow", "");
                intent.putExtra("romname", getIntent().getStringExtra("title"));
                intent.putExtra("romfilename", getIntent().getStringExtra("filename"));
                intent.putExtra("finalromfilename", getIntent().getStringExtra("finalromfilename"));
                intent.putExtra("rompath", selectedFilePath.getPath());
                if (Objects.requireNonNull(getIntent().getStringExtra("extra")).contains(selectedFilePath.getName())) {
                    intent.putExtra("addtodrive", "");
                    intent.putExtra("romextra", getIntent().getStringExtra("extra"));
                } else {
                    intent.putExtra("addtodrive", "1");
                    intent.putExtra("romextra", getIntent().getStringExtra("extra"));
                }
                intent.putExtra("romicon", AppConfig.maindirpath + "icons/" + getIntent().getStringExtra("filename") + ".png");
                switch (Objects.requireNonNull(getIntent().getStringExtra("arch"))) {
                    case "X86_64":
                        MainSettingsManager.setArch(this, "X86_64");
                        break;
                    case "i386":
                        MainSettingsManager.setArch(this, "I386");
                        break;
                    case "ARM64":
                        MainSettingsManager.setArch(this, "ARM64");
                        break;
                    case "PowerPC":
                        MainSettingsManager.setArch(this, "PPC");
                        break;
                }
                startActivity(intent);
            } else {
                UIUtils.oneDialog(getString(R.string.problem_has_been_detected), getString(R.string.please_select) + " " + getIntent().getStringExtra("filename"), true, false, this);
            }

        }
    }

    public String getPath(Uri uri) {
        return FileUtils.getPath(RomInfo.this, uri);
    }
}