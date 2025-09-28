package com.vectras.vm.settings;

import static android.content.Intent.ACTION_VIEW;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.AppConfig;
import com.vectras.vm.R;
import com.vectras.vm.RequestNetwork;
import com.vectras.vm.RequestNetworkController;
import com.vectras.vm.databinding.ActivityUpdaterBinding;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.PackageUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Objects;

public class UpdaterActivity extends AppCompatActivity {

    ActivityUpdaterBinding binding;
    String downloadUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_external_vnc_settings);
        binding = ActivityUpdaterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        initialize();
    }

    private void initialize() {
        binding.bnUpdate.setOnClickListener(v -> {
            if (downloadUrl.isEmpty()) {
                finish();
            } else {
                startActivity(new Intent(ACTION_VIEW, Uri.parse(downloadUrl)));
            }
        });

        check();
    }

    private void whenUpToDate() {
        binding.lpiProgressbar.setVisibility(View.GONE);
        binding.collapsingToolbarLayout.setTitle(getText(R.string.you_are_up_to_date));
        binding.collapsingToolbarLayout.setSubtitle(PackageUtils.getThisVersionName(getApplicationContext()));
        binding.bnUpdate.setText(getString(R.string.close));
    }

    private void checkAgain() {
        binding.lpiProgressbar.setVisibility(View.VISIBLE);
        binding.mcvWhatsnew.setVisibility(View.GONE);
        binding.lnBottombar.setVisibility(View.GONE);
        binding.collapsingToolbarLayout.setTitle(getText(R.string.checking_for_updates));
        binding.collapsingToolbarLayout.setSubtitle(getText(R.string.just_a_sec));
        binding.bnUpdate.setText(getString(R.string.update));
        downloadUrl = "";
        check();
    }

    private void check() {
        int versionCode = PackageUtils.getThisVersionCode(getApplicationContext());
        String versionName = PackageUtils.getThisVersionName(getApplicationContext());

        RequestNetwork requestNetwork = new RequestNetwork(this);
        RequestNetwork.RequestListener requestNetworkListener = new RequestNetwork.RequestListener() {
            @Override
            public void onResponse(String tag, String response, HashMap<String, Object> responseHeaders) {
                binding.lpiProgressbar.setVisibility(View.GONE);
                binding.lnBottombar.setVisibility(View.VISIBLE);

                if (!response.isEmpty()) {
                    try {
                        final JSONObject obj = new JSONObject(response);
                        String versionNameonUpdate;
                        int versionCodeonUpdate;
                        String whatsnew;
                        String size;
                        String url;

                        if (MainSettingsManager.getcheckforupdatesfromthebetachannel(UpdaterActivity.this)) {
                            versionNameonUpdate = obj.getString("versionNameBeta");
                            versionCodeonUpdate = obj.getInt("versionCodeBeta");
                            whatsnew = obj.getString("MessageBeta");
                            size = obj.getString("sizeBeta");
                            url = obj.getString("urlBeta");
                        } else {
                            versionNameonUpdate = obj.getString("versionName");
                            versionCodeonUpdate = obj.getInt("versionCode");
                            whatsnew = obj.getString("Message");
                            size = obj.getString("size");
                            url = obj.getString("url");
                        }

                        if (versionCode < versionCodeonUpdate || !versionNameonUpdate.equals(versionName)) {
                            binding.collapsingToolbarLayout.setTitle(getText(R.string.new_update_available));
                            binding.collapsingToolbarLayout.setSubtitle(getString(R.string.whats_new));
                            binding.mcvWhatsnew.setVisibility(View.VISIBLE);
                            binding.tvWhatsnewcontent.setMovementMethod(LinkMovementMethod.getInstance());
                            binding.tvWhatsnewcontent.setText(Html.fromHtml(whatsnew + "<br><br>Update size:<br>" + size));
                            downloadUrl = url;
                        } else {
                            whenUpToDate();
                        }

                        if (obj.getString("versionNameBetas").contains(versionName)
                                && !MainSettingsManager.getcheckforupdatesfromthebetachannel(UpdaterActivity.this)
                                && !MainSettingsManager.getDontShowAgainJoinBetaUpdateChannelDialog(UpdaterActivity.this)) {
                            Activity activity = UpdaterActivity.this;

                            DialogUtils.threeDialog(activity,
                                    activity.getResources().getString(R.string.you_are_using_beta_version),
                                    activity.getResources().getString(R.string.switch_to_check_for_updates_on_the_Beta_channel_now),
                                    activity.getResources().getString(R.string.ok),
                                    activity.getResources().getString(R.string.cancel),
                                    activity.getResources().getString(R.string.dont_show_again),
                                    true,
                                    R.drawable.science_24px,
                                    true,
                                    () -> {
                                        MainSettingsManager.setcheckforupdatesfromthebetachannel(activity, true);
                                        checkAgain();
                                    },
                                    null,
                                    () -> MainSettingsManager.setDontShowAgainJoinBetaUpdateChannelDialog(activity, true),
                                    null);
                        }
                    } catch (JSONException e) {
                        whenUpToDate();
                    }
                } else {
                    whenUpToDate();
                }
            }

            @Override
            public void onErrorResponse(String tag, String message) {
                whenUpToDate();
            }
        };

        requestNetwork.startRequestNetwork(RequestNetworkController.GET,AppConfig.updateJson,"checkupdate",requestNetworkListener);
    }
}