package com.vectras.vm;

import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
//import com.google.android.gms.ads.MobileAds;
//import com.google.android.gms.ads.initialization.InitializationStatus;
//import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
//import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.vectras.vm.utils.UIUtils;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class PostActivity extends AppCompatActivity {

    private Toolbar tb;
    public static TextView postTitle;
    public static TextView postContent;
    public static TextView postDate;
    public static ImageView postThumb;
    public static String title, content, contentStr, date, thumb;

//    private InterstitialAd mInterstitialAd;
    private String TAG = "PostActivity";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.post_content);
        postTitle = findViewById(R.id.postTitle);
        postContent = findViewById(R.id.postContent);
        postDate = findViewById(R.id.postDate);
        postThumb = findViewById(R.id.postThumb);
        tb = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(tb);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        //AdView mAdView = findViewById(R.id.adView);
        //AdRequest adRequest = new AdRequest.Builder().build();
        //mAdView.loadAd(adRequest);
//        MobileAds.initialize(this, new OnInitializationCompleteListener() {
//            @Override
//            public void onInitializationComplete(InitializationStatus initializationStatus) {
//            }
//        });
        /*InterstitialAd.load(this, "ca-app-pub-3568137780412047/7745973511", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                        Log.i(TAG, "onAdLoaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.d(TAG, loadAdError.toString());
                        mInterstitialAd = null;
                    }
                });*/
        postContent.setTextIsSelectable(true);

        Glide.with(this).load(thumb).into(postThumb);
        new Thread(new Runnable() {

            public void run() {

                BufferedReader reader = null;
                final StringBuilder builder = new StringBuilder();

                try {
                    // Create a URL for the desired page
                    URL url = new URL(content); //My text file location
                    //First open the connection
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(60000); // timing out in a minute

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    //t=(TextView)findViewById(R.id.TextView1); // ideally do this in onCreate()
                    String str;
                    while ((str = in.readLine()) != null) {
                        builder.append(str);
                    }
                    in.close();
                } catch (Exception e) {
                    postContent.setText("no internet connection");
                    UIUtils.toastLong(PostActivity.this, "check your internet connection");
                    Log.d("VECTRAS", e.toString());
                }

                //since we are in background thread, to post results we have to go back to ui thread. do the following for that

                PostActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        contentStr = builder.toString(); // My TextFile has 3 lines
                        postContent.setText(Html.fromHtml(contentStr));
                    }
                });

            }
        }).start();
        postDate.setText(date);
        postTitle.setText(title);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

}
