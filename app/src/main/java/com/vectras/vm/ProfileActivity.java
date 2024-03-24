package com.vectras.vm;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ProfileActivity extends AppCompatActivity {

    private ProfileActivity activity;
    public TextInputEditText profileUsername;
    public MaterialButton saveBtn;
    FirebaseAuth mAuth;
    FirebaseUser mCurrentUser;
    private DatabaseReference newUser;
    public Uri profileUri;

    public ImageView profilePic;

    public ProgressBar loadingPb;

    private Uri imgUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        activity = this;
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser();
        newUser = FirebaseDatabase.getInstance().getReference().child(mCurrentUser.getUid());
        newUser.child("email").setValue(mCurrentUser.getEmail());
        String name = mCurrentUser.getDisplayName();
        String email = mCurrentUser.getEmail();
        Uri picture = mCurrentUser.getPhotoUrl();
        profilePic = findViewById(R.id.profilePic);
        profileUsername = findViewById(R.id.profileName);
        saveBtn = findViewById(R.id.saveBtn);
        loadingPb = findViewById(R.id.loadingPb);
        //Glide.with(activity).load(picture).error(R.drawable.person_24).into(profilePic);
        profileUsername.setText(name);
        if (profileUsername.getText().toString().equals(mCurrentUser.getDisplayName())) {
            saveBtn.setEnabled(false);
        } else {
            saveBtn.setEnabled(true);
        }
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
                if (profileUsername.getText().toString().trim().length() > 0) {
                    if (profileUsername.getText().toString().equals(mCurrentUser.getDisplayName())) {
                        saveBtn.setEnabled(false);
                    } else {
                        saveBtn.setEnabled(true);
                    }
                } else {
                    saveBtn.setEnabled(false);
                    profileUsername.setError("username can't be empty!");
                }
            }
        };
        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_PICK);
                i.setType("image/*");
                startActivityForResult(i, 1009);
            }
        });
        profileUsername.addTextChangedListener(afterTextChangedListener);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                loadingPb.setVisibility(View.VISIBLE);

                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(String.valueOf(profileUsername.getText()))
                        .setPhotoUri(imgUri)
                        .build();

                mCurrentUser.updateProfile(profileUpdates)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    mCurrentUser.sendEmailVerification()
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    saveBtn.setEnabled(false);
                                                    loadingPb.setVisibility(View.GONE);
                                                    View rootView = findViewById(R.id.main_layout);
                                                    Snackbar.make(rootView, "Updated Successfully!", 3000).show();
                                                }
                                            });

                                }
                            }
                        });

            }
        });

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1009 && resultCode == RESULT_OK) {
            imgUri = data.getData();
            Glide.with(activity).load(imgUri).into(profilePic);
            saveBtn.setEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (profileUsername.getText().toString().equals(mCurrentUser.getDisplayName())) {
            saveBtn.setEnabled(false);
        } else {
            saveBtn.setEnabled(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (profileUsername.getText().toString().equals(mCurrentUser.getDisplayName())) {
            saveBtn.setEnabled(false);
        } else {
            saveBtn.setEnabled(true);
        }
    }
}