package com.vectras.vm;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.transition.TransitionManager;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.vectras.vm.databinding.ActivityWebViewBinding;
import com.vectras.vm.utils.UIUtils;

import java.util.Objects;

public class WebViewActivity extends AppCompatActivity {
    ActivityWebViewBinding binding;
    String oringialDomain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UIUtils.edgeToEdge(this);
        binding = ActivityWebViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UIUtils.setOnApplyWindowInsetsListener(binding.main);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        binding.webview.getSettings().setJavaScriptEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);
        binding.webview.getSettings().setBuiltInZoomControls(true);
        binding.webview.getSettings().setDisplayZoomControls(false);
        binding.webview.getSettings().setAllowFileAccess(true);
        binding.webview.getSettings().setDatabaseEnabled(true);
        binding.webview.getSettings().setDomStorageEnabled(true);

        if (getIntent().hasExtra("url") && getIntent().getStringExtra("url") != null) {
            binding.webview.loadUrl(Objects.requireNonNull(getIntent().getStringExtra("url")));
            oringialDomain= Uri.parse(getIntent().getStringExtra("url")).getAuthority();
        } else {
            binding.webview.loadUrl(AppConfig.vectrasWebsite);
            oringialDomain = Uri.parse(AppConfig.vectrasWebsite).getAuthority();
        }

        binding.webview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                String scheme = url.getScheme();
                String domain = url.getAuthority();

                if (domain != null && !domain.equals(oringialDomain)) {
                    Intent openInBrowser = new Intent();
                    openInBrowser.setAction(Intent.ACTION_VIEW);
                    openInBrowser.setData(url);
                    startActivity(openInBrowser);
                    //Do not load this URL in the webview, return true.
                    return true;
                }

                if ("file".equals(scheme) || "http".equals(scheme) || "https".equals(scheme)) {
                    return false;
                }
                return true;
            }
        });

        binding.webview.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollY > oldScrollY) {
                if (binding.btnClose.getVisibility() == View.VISIBLE) {
                    TransitionManager.beginDelayedTransition(binding.main);
                    binding.btnClose.setVisibility(View.GONE);
                }
            } else if (scrollY < oldScrollY) {
                if (binding.btnClose.getVisibility() == View.GONE) {
                    TransitionManager.beginDelayedTransition(binding.main);
                    binding.btnClose.setVisibility(View.VISIBLE);
                }
            }
        });

        binding.btnClose.setOnClickListener(v -> finish());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.webview.canGoBack()) {
                    binding.webview.goBack();
                } else {
                    finish();
                }
            }
        });
    }
}