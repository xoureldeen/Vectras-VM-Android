package com.vectras.vm.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.vectras.vm.MainActivity;
import com.vectras.vm.R;
import com.vectras.vm.SplashActivity;

public class VerifyEmailActivity extends AppCompatActivity {

    public static VerifyEmailActivity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);
        activity = this;
        TextView emailTxt = findViewById(R.id.email);
        emailTxt.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());
        Button singoutBtn = findViewById(R.id.signout);
        singoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                MainActivity.activity.finish();
                startActivity(new Intent(activity, SplashActivity.class));
            }
        });
    }
    public void onResume() {
        super.onResume();
        if (FirebaseAuth.getInstance().getCurrentUser().isEmailVerified()) {
            finish();
        }
    }
}
