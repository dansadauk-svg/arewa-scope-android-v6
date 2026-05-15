package com.arewascope.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends Activity {
    private static final String TAG = "ArewaScope";
    private static final String SITE_URL = "https://arewascope.com.ng/";
    private static final String SITE_HOST = "arewascope.com.ng";
    private static final String FCM_TOPIC = "news";
    public static final String NOTIFICATION_CHANNEL_ID = "arewa_news";
    public static final String NOTIFICATION_CHANNEL_NAME = "Arewa Scope News";
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 2001;

    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout offlineView;
    private ValueCallback<Uri[]> uploadCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#F76103"));
        getWindow().setNavigationBarColor(Color.parseColor("#F76103"));
        createNotificationChannel();
        requestNotificationPermissionIfNeeded();
        subscribeToNewsTopic();
        buildLayout();
        setupWebView();

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            String initialUrl = extractSafeUrlFromIntent(getIntent());
            loadUrl(initialUrl == null ? SITE_URL : initialUrl);
        }
    }

    private void buildLayout() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);
        root.setFitsSystemWindows(true);

        webView = new WebView(this);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        root.addView(webView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setVisibility(View.GONE);
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4));
        progressParams.gravity = Gravity.TOP;
        root.addView(progressBar, progressParams);

        offlineView = new LinearLayout(this);
        offlineView.setOrientation(LinearLayout.VERTICAL);
        offlineView.setGravity(Gravity.CENTER);
        offlineView.setPadding(dp(24), dp(24), dp(24), dp(24));
        offlineView.setBackgroundColor(Color.WHITE);
        offlineView.setVisibility(View.GONE);

        TextView title = new TextView(this);
        title.setText("No internet connection");
        title.setTextSize(22);
        title.setTextColor(Color.parseColor("#0B1220"));
        title.setGravity(Gravity.CENTER);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        offlineView.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Please check your connection and try again.");
        subtitle.setTextSize(15);
        subtitle.setTextColor(Color.parseColor("#334155"));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(10), 0, dp(18));
        offlineView.addView(subtitle);

        Button retry = new Button(this);
        retry.setText("Retry");
        retry.setAllCaps(false);
        retry.setOnClickListener(v -> loadUrl(webView.getUrl() == null ? SITE_URL : webView.getUrl()));
        offlineView.addView(retry);

        root.addView(offlineView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadsImagesAutomatically(true);
        settings.setCacheMode(isOnline() ? WebSettings.LOAD_DEFAULT : WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setUserAgentString(settings.getUserAgentString() + " ArewaScopeAndroidApp/1.6");

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url);
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                offlineView.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                offlineView.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (uploadCallback != null) {
                    uploadCallback.onReceiveValue(null);
                }
                uploadCallback = filePathCallback;
                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (ActivityNotFoundException e) {
                    uploadCallback = null;
                    Toast.makeText(MainActivity.this, "No file picker found", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });
    }

    private boolean handleUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return true;
        }
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();

        if ("http".equals(scheme) || "https".equals(scheme)) {
            if (SITE_HOST.equals(host) || ("www." + SITE_HOST).equals(host)) {
                return false;
            }
            openOutside(url);
            return true;
        }

        if ("tel".equals(scheme) || "mailto".equals(scheme) || "whatsapp".equals(scheme) || "intent".equals(scheme)) {
            openOutside(url);
            return true;
        }

        return false;
    }

    private void openOutside(String url) {
        try {
            Intent intent = url.startsWith("intent:") ? Intent.parseUri(url, Intent.URI_INTENT_SCHEME) : new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open this link", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUrl(String url) {
        if (isOnline()) {
            offlineView.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            webView.loadUrl(url);
        } else {
            webView.setVisibility(View.GONE);
            offlineView.setVisibility(View.VISIBLE);
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = cm == null ? null : cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    private void subscribeToNewsTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic(FCM_TOPIC)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Subscribed to FCM topic: " + FCM_TOPIC);
                    } else {
                        Log.w(TAG, "FCM topic subscription failed", task.getException());
                    }
                });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null && manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        NOTIFICATION_CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Latest Arewa Scope news updates");
                manager.createNotificationChannel(channel);
            }
        }
    }

    private String extractSafeUrlFromIntent(Intent intent) {
        if (intent == null) {
            return null;
        }
        String url = intent.getStringExtra("url");
        if (url == null) {
            url = intent.getStringExtra("link");
        }
        if (url == null) {
            return null;
        }
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
        if (("http".equals(scheme) || "https".equals(scheme)) && (SITE_HOST.equals(host) || ("www." + SITE_HOST).equals(host))) {
            return url;
        }
        return SITE_URL;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String url = extractSafeUrlFromIntent(intent);
        if (url != null && webView != null) {
            loadUrl(url);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && uploadCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                results = new Uri[]{data.getData()};
            }
            uploadCallback.onReceiveValue(results);
            uploadCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            webView.saveState(outState);
        }
    }

    @Override
    protected void onDestroy() {
        if (uploadCallback != null) {
            uploadCallback.onReceiveValue(null);
            uploadCallback = null;
        }
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
