package com.vectras.vm.ui.login;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.vectras.vm.AppConfig;
import com.vectras.vm.R;
import com.vectras.vm.RomsManagerActivity;
import com.vectras.vm.SplashActivity;
import com.vectras.vm.utils.UIUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextView mStatusTextView;
    public String license;

    // ...
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        new Thread(new Runnable() {

            public void run() {

                BufferedReader reader = null;
                final StringBuilder builder = new StringBuilder();

                try {
                    // Create a URL for the desired page
                    URL url = new URL(AppConfig.vectrasTerms); //My text file location
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
                    UIUtils.toastLong(LoginActivity.this, "no internet connection " + e.toString());
                }

                //since we are in background thread, to post results we have to go back to ui thread. do the following for that

                runOnUiThread(new Runnable() {
                    public void run() {
                        license = builder.toString();
                    }
                });

            }
        }).start();


        MaterialButton signinButton = findViewById(R.id.signinBtn);

        MaterialButton signupButton = findViewById(R.id.signupBtn);

        MaterialButton signwithGoogleButton = findViewById(R.id.signwithGoogleBtn);

        TextInputEditText usernameEditText = findViewById(R.id.username);

        TextInputEditText passwordEditText = findViewById(R.id.password);

        mStatusTextView = findViewById(R.id.errorTxt);

        TextView ppTxt = findViewById(R.id.ppTxt);

        ppTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (license != null)
                    UIAlertLicense("Terms&Conditions", license, LoginActivity.this);
            }
        });

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
                    signinButton.setEnabled(true);
                } else {
                    signinButton.setEnabled(false);
                }
                if (passwordEditText.getText().toString().trim().length() > 0) {
                    signinButton.setEnabled(true);
                } else {
                    signinButton.setEnabled(false);
                }
            }
        };
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

        signinButton.setOnClickListener(v -> mAuth.signInWithEmailAndPassword(usernameEditText.getText().toString(), passwordEditText.getText().toString())
                .addOnCompleteListener(LoginActivity.this, (OnCompleteListener<AuthResult>) task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                    } else {

                        mStatusTextView.setText(task.getException().toString());
                        // If sign in fails, display a message to the user.
                        Toast.makeText(LoginActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                        updateUI(null);
                    }
                }));
        signupButton.setOnClickListener(v -> startActivity(new Intent(this, SignupActivity.class)));
        signwithGoogleButton.setOnClickListener(v -> startActivity(new Intent(this, GoogleSignInActivity.class)));

    }

    public static void UIAlertLicense(String title, String html, final Activity activity) {
        AlertDialog alertDialog;
        alertDialog = new AlertDialog.Builder(activity, R.style.MainDialogTheme).create();
        alertDialog.setTitle(title);
        alertDialog.setCancelable(true);

        alertDialog.setMessage(Html.fromHtml(html));

        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "I Acknowledge", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                return;
            }
        });
        alertDialog.show();
    }

    public void updateUI(Object USER) {
        if (USER != null) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser user = auth.getCurrentUser();

            if (user != null && !user.isEmailVerified()) {
                user.sendEmailVerification()
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                finish();
                                startActivity(new Intent(LoginActivity.this, SplashActivity.class));
                            }
                        });
            }
            if (user != null && user.isEmailVerified())
                startActivity(new Intent(LoginActivity.this, RomsManagerActivity.class));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUser.reload();
        }
        updateUI(currentUser);
    }
}