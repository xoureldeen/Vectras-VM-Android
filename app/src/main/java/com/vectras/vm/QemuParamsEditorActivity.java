package com.vectras.vm;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;

import com.vectras.vm.databinding.ActivityQemuParamsEditorBinding;
import com.vectras.vm.utils.UIUtils;

public class QemuParamsEditorActivity extends AppCompatActivity {

    ActivityQemuParamsEditorBinding binding;
    public static String result = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.edgeToEdge(this);
        binding = ActivityQemuParamsEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UIUtils.setOnApplyWindowInsetsListener(findViewById(R.id.main));
        if (getIntent().hasExtra("content")) {
            result = getIntent().getStringExtra("content");
            binding.edittext1.setText(result);
        }

        binding.materialbutton1.setOnClickListener(v -> {
            result = binding.edittext1.getText().toString();
            finish();
        });

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            binding.edittext1.requestFocus();
            binding.edittext1.setSelection(binding.edittext1.getText().length());
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.showSoftInput(binding.edittext1, InputMethodManager.SHOW_IMPLICIT);
        }, 200);
    }
}