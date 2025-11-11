package com.vectras.vm;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vectras.qemu.MainSettingsManager;
import com.vectras.vm.databinding.ActivityRomInfoBinding;
import com.vectras.vm.utils.DialogUtils;
import com.vectras.vm.utils.FileUtils;
import com.vectras.vm.utils.ImageUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RomInfo extends AppCompatActivity {

    final String TAG = "RomInfo";
    ActivityRomInfoBinding binding;
    public static boolean isFinishNow = false;
    private String currentViews = "0";
    private boolean isAnBuiID = false;
    private String contentID = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityRomInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.btnDownload.setOnClickListener(v -> {
            Intent openurl = new Intent();
            openurl.setAction(Intent.ACTION_VIEW);
            openurl.setData(Uri.parse(getIntent().getStringExtra("getrom")));
            startActivity(openurl);
        });

        binding.btnPick.setOnClickListener(v -> romPicker.launch("*/*"));

        if (getIntent().hasExtra("title")) {
            binding.textName.setText(getIntent().getStringExtra("title"));
        }
        if (getIntent().hasExtra("shortdesc")) {
            binding.textSize.setText(getIntent().getStringExtra("shortdesc"));
        }
        if (getIntent().hasExtra("desc")) {
            binding.descTxt.setText(getIntent().getStringExtra("desc"));
        }

        if (getIntent().hasExtra("icon")) {
            Glide.with(this).load(getIntent().getStringExtra("icon")).placeholder(R.drawable.ic_computer_180dp_with_padding).error(R.drawable.ic_computer_180dp_with_padding).into(binding.ivIcon);
        }

        if (getIntent().hasExtra("id") &&
                !Objects.requireNonNull(getIntent().getStringExtra("id")).isEmpty()) {

            isAnBuiID = true;
            contentID = getIntent().getStringExtra("id");

        } else if (getIntent().hasExtra("vecid") &&
                !Objects.requireNonNull(getIntent().getStringExtra("vecid")).isEmpty()) {

            contentID = getIntent().getStringExtra("vecid");
        }

        initialize();
    }

    public void onResume() {
        super.onResume();
        if (isFinishNow)
            finish();
        isFinishNow = false;
    }

    private final ActivityResultLauncher<String> romPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {

                if (uri == null) return;

                String filePath;
                try {
                    File selectedFilePath = new File(getPath(uri));
                    filePath = selectedFilePath.getPath();
                } catch (Exception e) {
                    filePath = "";
                }

                String selectedFileName = FileUtils.getFileNameFromUri(this, uri);
                if (selectedFileName.equals(getIntent().getStringExtra("filename")) ||
                        (selectedFileName.endsWith(".cvbi.zip") && selectedFileName.equals(getIntent().getStringExtra("filename") + ".zip"))) {
                    Intent intent = new Intent();

                    if (getIntent().hasExtra("icon") &&
                            !Objects.requireNonNull(getIntent().getStringExtra("icon")).isEmpty() &&
                            (!selectedFileName.endsWith(".cvbi")
                                    || !selectedFileName.endsWith(".cvbi.zip"))) {

                        Glide.with(this)
                                .asBitmap()
                                .load(getIntent().getStringExtra("icon"))
                                .into(new CustomTarget<Bitmap>() {
                                    @Override
                                    public void onResourceReady(@NonNull Bitmap resource,
                                                                @Nullable Transition<? super Bitmap> transition) {
                                        ImageUtils.saveBitmapToPNGFile(resource, Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath(), "thumbnail.png");
                                        intent.putExtra("romicon", getExternalCacheDir().getAbsolutePath() + "/thumbnail.png");
                                    }

                                    @Override
                                    public void onLoadCleared(@Nullable Drawable placeholder) {
                                        intent.putExtra("romicon", "");
                                    }
                                });
                    } else {
                        intent.putExtra("romicon", "");
                    }

                    intent.setClass(getApplicationContext(), VMCreatorActivity.class);
                    intent.putExtra("addromnow", "");
                    intent.putExtra("romname", getIntent().getStringExtra("title"));
                    intent.putExtra("romfilename", getIntent().getStringExtra("filename"));
                    intent.putExtra("finalromfilename", getIntent().getStringExtra("finalromfilename"));
                    intent.putExtra("rompath", filePath);
                    intent.putExtra("romuri", uri.toString());
                    if (Objects.requireNonNull(getIntent().getStringExtra("extra")).contains(selectedFileName)) {
                        intent.putExtra("addtodrive", "");
                        intent.putExtra("romextra", getIntent().getStringExtra("extra"));
                    } else {
                        intent.putExtra("addtodrive", "1");
                        intent.putExtra("romextra", getIntent().getStringExtra("extra"));
                    }
                    switch (Objects.requireNonNull(getIntent().getStringExtra("arch"))) {
                        case "X86_64":
                            MainSettingsManager.setArch(this, "X86_64");
                            break;
                        case "i386":
                            MainSettingsManager.setArch(this, "I386");
                            break;
                        case "ARM64":
                            MainSettingsManager.setArch(this, "ARM64");
                            break;
                        case "PowerPC":
                            MainSettingsManager.setArch(this, "PPC");
                            break;
                    }
                    startActivity(intent);
                } else {
                    DialogUtils.oneDialog(RomInfo.this,
                            getString(R.string.problem_has_been_detected),
                            getString(R.string.please_select) + " " + getIntent().getStringExtra("filename"),
                            getString(R.string.ok),
                            true, R.drawable.warning_48px,
                            true,
                            null,
                            null);
                }
            });

    public String getPath(Uri uri) {
        return FileUtils.getPath(RomInfo.this, uri);
    }

    private void initialize() {
        int currentVerifyIcon = R.drawable.verified_user_24px;
        String currentVerifyText = getString(R.string.verified);
        String currentVerifyContent = getString(R.string.verified_content);

        if (getIntent().hasExtra("verified")) {
            if (!getIntent().getBooleanExtra("verified", false)) {
                binding.ivVerified.setImageResource(R.drawable.gpp_maybe_24px);
                binding.tvVerified.setText(getString(R.string.not_verified));

                currentVerifyIcon = R.drawable.gpp_maybe_24px;
                currentVerifyText = getString(R.string.not_verified);
                currentVerifyContent = getString(R.string.not_verified_content);
            }
        }

        if (getIntent().hasExtra("creator")) {
            binding.tvCreator.setText(getIntent().getStringExtra("creator"));
        }

        binding.tvArch.setText(getArchName(Objects.requireNonNull(getIntent().getStringExtra("arch"))));

        if (getIntent().hasExtra("size")) {
            binding.tvSize.setText(getIntent().getStringExtra("size"));
        }

        if (getIntent().hasExtra("filename")) {
            binding.tvFilename.setText(getIntent().getStringExtra("filename"));
        }

        binding.lnViews.setOnClickListener((v -> DialogUtils.oneDialog(
                RomInfo.this,
                getString(R.string.views),
                currentViews + ".",
                getString(R.string.ok),
                true,
                R.drawable.show_chart_24px,
                true,
                null,
                null)));

        String finalCurrentVerifyText = currentVerifyText;
        String finalCurrentVerifyContent = currentVerifyContent;
        int finalCurrentVerifyIcon = currentVerifyIcon;
        binding.lnVerified.setOnClickListener((v -> DialogUtils.oneDialog(
                RomInfo.this,
                finalCurrentVerifyText,
                finalCurrentVerifyContent,
                getString(R.string.ok),
                true,
                finalCurrentVerifyIcon,
                true,
                null,
                null)));

        binding.lnCreator.setOnClickListener((v -> DialogUtils.oneDialog(
                RomInfo.this,
                getString(R.string.who_created_this_rom),
                getIntent().getStringExtra("creator") + ".",
                getString(R.string.ok),
                true,
                R.drawable.account_circle_24px,
                true,
                null,
                null)));

        binding.lnArch.setOnClickListener((v -> DialogUtils.oneDialog(
                RomInfo.this,
                getString(R.string.architecture),
                getArchName(Objects.requireNonNull(getIntent().getStringExtra("arch"))) + ".",
                getString(R.string.ok),
                true,
                R.drawable.devices_other_24px,
                true,
                null,
                null)));

        binding.lnSize.setOnClickListener((v -> DialogUtils.oneDialog(
                RomInfo.this,
                getString(R.string.sizetext),
                getIntent().getStringExtra("size") + ".",
                getString(R.string.ok),
                true,
                R.drawable.hard_drive_24px,
                true,
                null,
                null)));

        binding.lnFilename.setOnClickListener((v -> DialogUtils.oneDialog(
                RomInfo.this,
                getString(R.string.file_name),
                getIntent().getStringExtra("filename") + ".",
                getString(R.string.ok),
                true,
                R.drawable.file_copy_24px,
                true,
                null,
                null)));

        binding.btnLike.setOnClickListener(v -> sendLikeUpdate(contentID));

        if (!contentID.isEmpty()) {
            RequestNetwork net = new RequestNetwork(this);
            RequestNetwork.RequestListener _net_request_listener = new RequestNetwork.RequestListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onResponse(String tag, String response, HashMap<String, Object> responseHeaders) {
                    Log.i(TAG, response);
                    if (!response.isEmpty()) {
                        HashMap<String, Object> map = new Gson().fromJson(
                                response, new TypeToken<HashMap<String, Object>>() {
                                }.getType()
                        );

                        binding.btnLike.setVisibility(View.VISIBLE);
                        String likeContent = getString(R.string.like);
                        boolean isLiked = MainSettingsManager.getLikes(RomInfo.this).contains("," + getIntent().getStringExtra("id"));
                        if (map.containsKey("likes")) {
                            likeContent = (Objects.requireNonNull(map.get("likes")).toString().isEmpty() || (((Double) Objects.requireNonNull(map.get("likes"))).intValue() == 0) ? getString(R.string.like) : finalNumberOfInteractionsFormat(((Double) Objects.requireNonNull(map.get("likes"))).intValue()));
                        } else {
                            if (isLiked) likeContent = getString(R.string.liked);
                        }
                        if (isLiked) {
                            binding.btnLike.setIcon(ContextCompat.getDrawable(RomInfo.this, R.drawable.thumb_up_filled_24px));
                        }
                        binding.btnLike.setText(likeContent);

                        binding.lnAllViews.setVisibility(View.VISIBLE);
                        String viewsContent;
                        if (map.containsKey("views")) {
                            if (Objects.requireNonNull(map.get("views")).toString().isEmpty()) {
                                currentViews = "1";
                                viewsContent = "1 " + getString(R.string.unit_of_view);
                            } else {
                                currentViews = finalNumberOfInteractionsFormat(((Double) Objects.requireNonNull(map.get("views"))).intValue());
                                viewsContent = currentViews + " " + getString(((Double) Objects.requireNonNull(map.get("views"))).intValue() > 1 ? R.string.unit_of_views : R.string.unit_of_view);
                            }
                        } else {
                            viewsContent = "1 " + getString(R.string.unit_of_view);
                        }
                        binding.tvViews.setText(viewsContent);

                    } else {
                        binding.lnAllViews.setVisibility(View.GONE);
                        binding.btnLike.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onErrorResponse(String tag, String message) {
                    if (tag.equals("contentinfo")) {
                        binding.lnAllViews.setVisibility(View.GONE);
                        binding.btnLike.setVisibility(View.GONE);
                    }
                }
            };

            String urlToGetInfo = "https://go.anbui.ovh/egg/contentinfo?id=" + contentID + (isAnBuiID ? "" : "&app=vectrasvm");
            net.startRequestNetwork(RequestNetworkController.GET,urlToGetInfo,"contentinfo", _net_request_listener);
            Log.i(TAG, "urlToGetInfo: " + urlToGetInfo);

            sendViewUpdate(contentID);
        }
    }

    private void sendLikeUpdate(String id) {
        String addlikecount;

        if (MainSettingsManager.getLikes(this).contains("," + id)) {
            MainSettingsManager.setLikes(this, MainSettingsManager.getLikes(this).replace("," + id, ""));
            binding.btnLike.setIcon(ContextCompat.getDrawable(RomInfo.this, R.drawable.thumb_up_24px));
            addlikecount = "-1";
        } else {
            MainSettingsManager.setLikes(this, MainSettingsManager.getLikes(this) + "," + id);
            binding.btnLike.setIcon(ContextCompat.getDrawable(RomInfo.this, R.drawable.thumb_up_filled_24px));
            addlikecount = "1";
        }

        binding.btnLike.setText(addlikecount.equals("1") ? R.string.liked : R.string.like);

        String json = "{"
                + "\"id\":\"" + id + "\","
                + "\"addcount\":" + addlikecount
                + (isAnBuiID ? "" : ",\"app\":\"vectrasvm\"")
                + "}";

        RequestBody body = RequestBody.create(
                json, MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url("https://go.anbui.ovh/egg/updatelike?app=verctrasvm")
                .post(body)
                .build();

        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "sendLikeUpdate: ", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String resBody = response.body().string();
                    if (!resBody.isEmpty()) {
                        HashMap<String, Object> map = new Gson().fromJson(
                                resBody, new TypeToken<HashMap<String, Object>>(){}.getType()
                        );

                        if (map.containsKey("count")) {
                            runOnUiThread(() -> {
                                binding.btnLike.setVisibility(View.VISIBLE);
                                binding.btnLike.setText((Objects.requireNonNull(map.get("count")).toString().isEmpty() || (((Double) Objects.requireNonNull(map.get("count"))).intValue() == 0) ? getString(R.string.like) : finalNumberOfInteractionsFormat(((Double) Objects.requireNonNull(map.get("count"))).intValue())));
                            });
                        } else {
                            runOnUiThread(() -> binding.btnLike.setVisibility(View.GONE));
                        }
                    } else {
                        runOnUiThread(() -> binding.btnLike.setVisibility(View.GONE));
                    }
                } else {
                    runOnUiThread(() -> binding.btnLike.setVisibility(View.GONE));
                }
            }
        });
    }

    private void sendViewUpdate(String id) {

        if (MainSettingsManager.getViews(this).contains("," + id)) {
            return;
        } else {
            MainSettingsManager.setViews(this, MainSettingsManager.getViews(this) + "," + id);
        }

        String json = "{"
                + "\"id\":\"" + id + "\""
                + (isAnBuiID ? "" : ",\"app\":\"vectrasvm\"")
                + "}";

        RequestBody body = RequestBody.create(
                json, MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url("https://go.anbui.ovh/egg/updateview?app=vectrasvm")
                .post(body)
                .build();

        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "sendViewUpdate: ", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String resBody = response.body().string();
                    if (!resBody.isEmpty()) {
                        HashMap<String, Object> map = new Gson().fromJson(
                                resBody, new TypeToken<HashMap<String, Object>>(){}.getType()
                        );

                        if (map.containsKey("count")) {
                            String viewsContent;

                            if (Objects.requireNonNull(map.get("count")).toString().isEmpty()) {
                                currentViews = "1";
                                viewsContent = "1 " + getString(R.string.unit_of_view);
                            } else {
                                currentViews = finalNumberOfInteractionsFormat(((Double) Objects.requireNonNull(map.get("count"))).intValue());
                                viewsContent = currentViews + " " + getString(((Double) Objects.requireNonNull(map.get("count"))).intValue() > 1 ? R.string.unit_of_views : R.string.unit_of_view);
                            }

                            runOnUiThread(() -> {
                                binding.lnAllViews.setVisibility(View.VISIBLE);
                                binding.tvViews.setText(viewsContent);
                            });
                        } else {
                            runOnUiThread(() ->binding.lnAllViews.setVisibility(View.GONE));
                        }
                    } else {
                        runOnUiThread(() -> binding.lnAllViews.setVisibility(View.GONE));
                    }
                } else {
                    runOnUiThread(() -> binding.lnAllViews.setVisibility(View.GONE));
                }
            }
        });
    }

    @NonNull
    private String getArchName(String arch) {
        return switch (arch) {
            case "X86_64" -> getString(R.string.x86_64);
            case "i386" -> getString(R.string.i386_qemu);
            case "ARM64" -> getString(R.string.arm64_qemu);
            case "PowerPC" -> getString(R.string.powerpc_qemu);
            default -> getString(R.string.unknow);
        };
    }

    public static String finalNumberOfInteractionsFormat(int number) {
        if (number >= 1_000_000) {
            return String.format(Locale.US, "%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format(Locale.US, "%.1fK", number / 1_000.0);
        } else {
            return String.valueOf(number);
        }
    }

}