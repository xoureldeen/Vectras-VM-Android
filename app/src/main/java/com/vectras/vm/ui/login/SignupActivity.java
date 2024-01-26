package com.vectras.vm.ui.login;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.vectras.vm.R;
import com.vectras.vm.RomsManagerActivity;
import com.vectras.vm.SplashActivity;

public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextView mStatusTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        MaterialButton signupButton = findViewById(R.id.signupBtn);

        TextInputEditText fullNameEditText = findViewById(R.id.fullName);

        TextInputEditText usernameEditText = findViewById(R.id.username);

        TextInputEditText passwordEditText = findViewById(R.id.password);

        mStatusTextView = findViewById(R.id.errorTxt);

// Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (usernameEditText.getText().toString().trim().length() > 0) {
                    signupButton.setEnabled(true);
                } else {
                    signupButton.setEnabled(false);
                }
                if (passwordEditText.getText().toString().trim().length() > 0) {
                    signupButton.setEnabled(true);
                } else {
                    signupButton.setEnabled(false);
                }
                if (fullNameEditText.getText().toString().trim().length() > 0) {
                    signupButton.setEnabled(true);
                } else {
                    signupButton.setEnabled(false);
                }
            }
        };
        fullNameEditText.addTextChangedListener(afterTextChangedListener);
        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {

                }
                return false;
            }
        });
        signupButton.setOnClickListener(v -> mAuth.createUserWithEmailAndPassword(usernameEditText.getText().toString(), passwordEditText.getText().toString())
                .addOnCompleteListener(SignupActivity.this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("SIGNUP", "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("SIGNUP", "createUserWithEmail:failure", task.getException());

                        mStatusTextView.setText(task.getException().toString());

                        Toast.makeText(SignupActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                        updateUI(null);
                    }
                }));
    }
    public void updateUI(Object USER) {
        if (USER != null) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser user = auth.getCurrentUser();

            if (user != null && !user.isEmailVerified()) {
                TextInputEditText fullNameEditText = findViewById(R.id.fullName);
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(String.valueOf(fullNameEditText.getText()))
                        .build();

                user.updateProfile(profileUpdates)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    user.sendEmailVerification()
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    finish();
                                                    startActivity(new Intent(SignupActivity.this, SplashActivity.class));
                                                }
                                            });

                                }
                            }
                        });
            }
        }
    }
}