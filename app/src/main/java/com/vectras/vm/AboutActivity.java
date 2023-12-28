package com.vectras.vm;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.vectras.vm.utils.UIUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.vectras.vm.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AboutActivity extends AppCompatActivity implements View.OnClickListener{

    Button btn_osl, btn_clog, btn_youtube, btn_github,btn_telegram;
    String appInfo;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setTitle(getString(R.string.app_name));
        //btn
        btn_telegram = (Button) findViewById(R.id.btn_telegram);
        btn_youtube = (Button) findViewById(R.id.btn_youtube);
        btn_github = (Button) findViewById(R.id.btn_github);
        btn_osl = (Button) findViewById(R.id.btn_osl);
        btn_clog = (Button) findViewById(R.id.btn_changelog);
        //onclicklistener
        btn_telegram.setOnClickListener(this);
        btn_github.setOnClickListener(this);
        btn_youtube.setOnClickListener(this);
        btn_osl.setOnClickListener(this);
        btn_clog.setOnClickListener(this);

        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        new Thread(new Runnable(){

            public void run(){

                BufferedReader reader = null;
                final StringBuilder builder = new StringBuilder();

                try {
                    // Create a URL for the desired page
                    URL url = new URL(AppConfig.vectrasInfo); //My text file location
                    //First open the connection
                    HttpURLConnection conn=(HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(60000); // timing out in a minute

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    //t=(TextView)findViewById(R.id.TextView1); // ideally do this in onCreate()
                    String str;
                    while ((str = in.readLine()) != null) {
                        builder.append(str);
                    }
                    in.close();
                } catch (Exception e) {
                    UIUtils.toastLong(AboutActivity.this, "check your internet connection");
                    Log.d("VECTRAS",e.toString());
                }

                //since we are in background thread, to post results we have to go back to ui thread. do the following for that

                runOnUiThread(new Runnable(){
                    public void run(){
                        appInfo = builder.toString(); // My TextFile has 3 lines

                    }
                });

            }
        }).start();
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
                i.putExtra(Intent.EXTRA_EMAIL  , new String[]{"noureldeenelsayed856@gmail.com"});
                i.putExtra(Intent.EXTRA_SUBJECT, "Vectras User: " + Build.BRAND);
                i.putExtra(Intent.EXTRA_TEXT   , "Device Model: \n" + Build.MODEL + "\n");
                try {
                    startActivity(Intent.createChooser(i, "Send mail..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    Snackbar.make(view, "There are no email clients installed.", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }

            }
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
                String tw = "https://www.youtube.com/@XOURELDEEN";
                Intent w = new Intent(Intent.ACTION_VIEW);
                w.setData(Uri.parse(tw));
                startActivity(w);
            } else if (id == GT) {
                String gt = AppConfig.vectrasRepo;
                Intent g = new Intent(Intent.ACTION_VIEW);
                g.setData(Uri.parse(gt));
                startActivity(g);
            } else if (id == CL) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this, R.style.MainDialogTheme);
                alertDialogBuilder.setTitle("Changelog");
                alertDialogBuilder
                        .setMessage(getString(R.string.app_version))
                        .setCancelable(true)
                        .setIcon(R.mipmap.ic_launcher)
                        .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            } else if (id == OSL) {
                AlertDialog.Builder alertDialogOSL = new AlertDialog.Builder(this, R.style.MainDialogTheme);
                alertDialogOSL.setTitle("APP INFO");
                alertDialogOSL
                        .setMessage(appInfo)
                        .setCancelable(true)
                        .setIcon(R.drawable.round_info_24)
                        .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                AlertDialog alertDialogosl = alertDialogOSL.create();
                alertDialogosl.show();
            }
    }
}
