package com.vectras.vm;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.vectras.vm.utils.UIUtils;

public class EditActivity extends AppCompatActivity {

    private EditText editcontent;
    public static String result = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.edgeToEdge(this);
        setContentView(R.layout.activity_edit);
        UIUtils.setOnApplyWindowInsetsListener(findViewById(R.id.main));
        Button buttondone = findViewById(R.id.materialbutton1);
        editcontent = findViewById(R.id.edittext1);
        if (getIntent().hasExtra("content")) {
            result = getIntent().getStringExtra("content");
            editcontent.setText(result);
        }

        buttondone.setOnClickListener(v -> {
            result = editcontent.getText().toString();
            finish();
        });

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            editcontent.requestFocus();
            editcontent.setSelection(editcontent.getText().length());
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.showSoftInput(editcontent, InputMethodManager.SHOW_IMPLICIT);
        }, 200);
    }
}