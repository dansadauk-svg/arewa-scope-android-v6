package com.arewascope.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SplashActivity extends Activity {
    private static final int SPLASH_DELAY_MS = 800;
    private static final String BRAND_ORANGE = "#F76103";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor(BRAND_ORANGE));
        getWindow().setNavigationBarColor(Color.parseColor(BRAND_ORANGE));
        buildSplashScreen();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
            if (getIntent() != null) {
                if (getIntent().hasExtra("url")) {
                    mainIntent.putExtra("url", getIntent().getStringExtra("url"));
                }
                if (getIntent().hasExtra("link")) {
                    mainIntent.putExtra("link", getIntent().getStringExtra("link"));
                }
            }
            startActivity(mainIntent);
            finish();
        }, SPLASH_DELAY_MS);
    }

    private void buildSplashScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.parseColor(BRAND_ORANGE));
        root.setPadding(dp(28), dp(28), dp(28), dp(28));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.splash_logo);
        logo.setAdjustViewBounds(true);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(190), dp(190));
        logoParams.setMargins(0, 0, 0, dp(18));
        root.addView(logo, logoParams);

        TextView title = new TextView(this);
        title.setText("Arewa Scope");
        title.setTextColor(Color.WHITE);
        title.setTextSize(27);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        root.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView subtitle = new TextView(this);
        subtitle.setText("Northern Nigeria News & Updates");
        subtitle.setTextColor(Color.parseColor("#FFF3EA"));
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.setMargins(0, dp(6), 0, dp(22));
        root.addView(subtitle, subParams);

        ProgressBar loader = new ProgressBar(this, null, android.R.attr.progressBarStyleSmall);
        root.addView(loader, new LinearLayout.LayoutParams(dp(32), dp(32)));

        setContentView(root);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
