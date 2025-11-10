package com.vectras.vm.view;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;

import com.bumptech.glide.Glide;
import com.vectras.vm.R;
import com.vectras.vm.model.GithubUser;
import com.vectras.vm.network.GithubApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class GithubUserView extends LinearLayout {
    private final String TAG = "GithubUserView";
    private LinearLayout mainView;
    private ImageView profileImage;
    private TextView userName;
    private TextView userDescription;
    private ImageButton githubProfile;
    private static final String BASE_URL = "https://api.github.com/";
    private String thisUserNameGitHub = "";

    public GithubUserView(Context context) {
        super(context);
        init(context);
    }

    public GithubUserView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GithubUserView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.github_user_view, this, true);

        mainView = findViewById(R.id.main);
        profileImage = findViewById(R.id.profile_image);
        userName = findViewById(R.id.user_name);
        userDescription = findViewById(R.id.user_description);
        githubProfile = findViewById(R.id.githubProfile);

        mainView.setOnClickListener(v -> openGithubProfile(context));
    }

    public void setUsername(String username) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        GithubApiService service = retrofit.create(GithubApiService.class);
        Call<GithubUser> call = service.getUser(username);
        call.enqueue(new Callback<GithubUser>() {
            @Override
            public void onResponse(Call<GithubUser> call, Response<GithubUser> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GithubUser user = response.body();
                    thisUserNameGitHub = user.getLogin();
                    userName.setText(!user.getLogin().isEmpty() ? user.getLogin() : getContext().getString(R.string.unknow));
                    userDescription.setText(!user.getBio().isEmpty() ? user.getBio() : getContext().getString(R.string.unknow));
                    if (!user.getAvatarUrl().isEmpty()) {
                        Glide.with(getContext()).load(user.getAvatarUrl()).placeholder(R.drawable.account_circle_24px).error(R.drawable.account_circle_24px).into(profileImage);
                    } else {
                        profileImage.setImageResource(R.drawable.account_circle_24px);
                    }
                } else {
                    userName.setText(getContext().getString(R.string.unknow));
                    userDescription.setText(getContext().getString(R.string.unknow));
                    profileImage.setImageResource(R.drawable.account_circle_24px);
                }
            }

            @Override
            public void onFailure(Call<GithubUser> call, Throwable t) {
                userName.setText(getContext().getString(R.string.unknow));
                userDescription.setText(getContext().getString(R.string.unknow));
                profileImage.setImageResource(R.drawable.account_circle_24px);
            }
        });
    }

    private void openGithubProfile(Context context) {
        String url = "https://github.com/" + thisUserNameGitHub;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(intent);
    }
}