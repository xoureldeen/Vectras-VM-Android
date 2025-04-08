package com.vectras.vm;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class EditActivity extends AppCompatActivity {

    private Button buttondone;
    private EditText editcontent;
    public static String result = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIController.edgeToEdge(this);
        setContentView(R.layout.activity_edit);
        UIController.setOnApplyWindowInsetsListener(findViewById(R.id.main));
        buttondone = findViewById(R.id.materialbutton1);
        editcontent = findViewById(R.id.edittext1);
        if (getIntent().hasExtra("content")) {
            result = getIntent().getStringExtra("content");
            editcontent.setText(result);
        }

        buttondone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                result = editcontent.getText().toString();
                finish();
            }
        });

        editcontent.requestFocus();
    }
}