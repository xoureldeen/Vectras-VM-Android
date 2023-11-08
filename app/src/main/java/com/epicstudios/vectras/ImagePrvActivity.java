package com.epicstudios.vectras;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.epicstudios.vectras.R;

public class ImagePrvActivity extends AppCompatActivity {
    public static String linkIv;
    public ImageView ivPrv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.iv_prv);
        ivPrv = findViewById(R.id.ivPrv);
        Glide.with(this).load(linkIv).into(ivPrv);
    }
}
