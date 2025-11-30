package com.vectras.vm;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.color.MaterialColors;
import com.vectras.vm.databinding.IvPrvBinding;
import com.vectras.vm.utils.UIUtils;

public class ImagePrvActivity extends AppCompatActivity {
    IvPrvBinding binding;
    boolean isInvertedBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.edgeToEdge(this);
        binding = IvPrvBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UIUtils.setOnApplyWindowInsetsListener(findViewById(R.id.main));
        if (getIntent().hasExtra("uri")) {
            Glide.with(this).load(getIntent().getStringExtra("uri")).placeholder(R.drawable.progress_activity_24px).error(R.drawable.broken_image_24px).into(binding.ivPrv);
        }

        binding.btnChangeBackground.setOnClickListener(v -> {
            int newBackgroundColor = MaterialColors.getColor(binding.main, isInvertedBackground ? com.google.android.material.R.attr.colorSurface : com.google.android.material.R.attr.colorOnSurface);
            binding.main.setBackgroundColor(newBackgroundColor);
            UIUtils.setLightStatusBar(UIUtils.isColorLight(newBackgroundColor), ImagePrvActivity.this);
            isInvertedBackground = !isInvertedBackground;
        });
    }
}
