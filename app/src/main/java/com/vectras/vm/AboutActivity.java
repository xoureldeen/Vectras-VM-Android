package com.vectras.vm;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.vectras.vm.adapters.GithubUserAdapter;
import com.vectras.vm.utils.CommandUtils;
import com.vectras.vm.utils.UIUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AboutActivity extends AppCompatActivity implements View.OnClickListener{
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Button btn_osl, btn_clog;
    ImageButton btn_discord, btn_youtube, btn_github, btn_telegram, btn_instagram, btn_facebook;

    String appInfo;

    public String TAG = "AboutActivity";
//    private InterstitialAd mInterstitialAd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.edgeToEdge(this);
        setContentView(R.layout.activity_about);
//        UIUtils.setOnApplyWindowInsetsListener(findViewById(R.id.main));
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setTitle(getResources().getString(R.string.about));
        //btn
        btn_telegram = findViewById(R.id.btn_telegram);
        btn_youtube = findViewById(R.id.btn_youtube);
        btn_github = findViewById(R.id.btn_github);
        btn_instagram = findViewById(R.id.btn_instagram);
        btn_facebook = findViewById(R.id.btn_facebook);
        btn_discord = findViewById(R.id.btn_discord);
        btn_osl = findViewById(R.id.btn_osl);
        btn_clog = findViewById(R.id.btn_changelog);
        //onclicklistener
        btn_telegram.setOnClickListener(this);
        btn_github.setOnClickListener(this);
        btn_youtube.setOnClickListener(this);
        btn_instagram.setOnClickListener(this);
        btn_facebook.setOnClickListener(this);
        btn_discord.setOnClickListener(this);
        btn_osl.setOnClickListener(this);
        btn_clog.setOnClickListener(this);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL  , new String[]{"anbuigo2004@gmail.com"});
            i.putExtra(Intent.EXTRA_SUBJECT, "Vectras User: " + Build.BRAND);
            i.putExtra(Intent.EXTRA_TEXT   , "Device Model: \n" + Build.MODEL + "\n");
            try {
                startActivity(Intent.createChooser(i, "Send mail..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Snackbar.make(view, "There are no email clients installed.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }

        });

        RecyclerView recyclerView = findViewById(R.id.github_users_recycler_view);
        String[] usernames = {"vectras-team", "xoureldeen", "ahmedbarakat2007", "anbui2004"};

        GithubUserAdapter adapter = new GithubUserAdapter(this, usernames);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        TextView qemuVersion = findViewById(R.id.qemuVersion);
        executor.execute(() -> {
            String qemuVersionName = CommandUtils.getQemuVersionName();
            runOnUiThread(() -> {
                if (!qemuVersionName.isEmpty()) qemuVersion.setText(qemuVersionName); else getString(R.string.unknow);
            });
        });

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()== android.R.id.home){
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
    public static final int TG = R.id.btn_telegram;
    public static final int YT = R.id.btn_youtube;
    public static final int GT = R.id.btn_github;
    public static final int IG = R.id.btn_instagram;
    public static final int FB = R.id.btn_facebook;
    public static final int DD = R.id.btn_discord;
    public static final int CL = R.id.btn_changelog;
    public static final int OSL = R.id.btn_osl;
    @Override
    public void onClick(View v) {
        int id = v.getId();
            if (id == TG) {
                String tg = "https://t.me/vectras_os";
                Intent f = new Intent(Intent.ACTION_VIEW);
                f.setData(Uri.parse(tg));
                startActivity(f);
            } else if (id == YT) {
                String tw = "https://youtube.com/@xoureldeen";
                Intent w = new Intent(Intent.ACTION_VIEW);
                w.setData(Uri.parse(tw));
                startActivity(w);
            } else if (id == GT) {
                String gt = AppConfig.vectrasRepo;
                Intent g = new Intent(Intent.ACTION_VIEW);
                g.setData(Uri.parse(gt));
                startActivity(g);
            } else if (id == IG) {
                String ig = "https://vectras.vercel.app";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(ig));
                startActivity(i);
            } else if (id == DD) {
                String dd = "https://discord.gg/t8TACrKSk7";
                Intent f = new Intent(Intent.ACTION_VIEW);
                f.setData(Uri.parse(dd));
                startActivity(f);
            } else if (id == FB) {
                String fb = AppConfig.vectrasWebsite + "community.html";
                Intent f = new Intent(Intent.ACTION_VIEW);
                f.setData(Uri.parse(fb));
                startActivity(f);
            } else if (id == CL) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this, R.style.MainDialogTheme);
                alertDialogBuilder.setTitle("Changelog");
                alertDialogBuilder
                        .setMessage(getString(R.string.app_version))
                        .setCancelable(true)
                        .setIcon(R.mipmap.ic_launcher)
                        .setNegativeButton("OK", (dialog, which) -> dialog.dismiss());
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            } else if (id == OSL) {
                AlertDialog.Builder alertDialogOSL = new AlertDialog.Builder(this, R.style.MainDialogTheme);
                alertDialogOSL.setTitle(getResources().getString(R.string.info));
                alertDialogOSL
                        .setMessage(appInfo)
                        .setCancelable(true)
                        .setIcon(R.drawable.round_info_24)
                        .setNegativeButton("OK", (dialog, which) -> dialog.dismiss());
                AlertDialog alertDialogosl = alertDialogOSL.create();
                alertDialogosl.show();
            }
    }
}
