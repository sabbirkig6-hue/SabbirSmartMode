package com.sabbir.smartmode;

import android.app.Activity;
import android.os.Bundle;
import android.Manifest;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.app.NotificationManager;
import android.provider.Settings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;

public class MainActivity extends Activity {

    private WebView webView;
    private static final int LOCATION_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setGeolocationEnabled(true);

        webView.addJavascriptInterface(new PhoneControl(), "PhoneControl");

        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(
                String origin,
                GeolocationPermissions.Callback callback
            ) {
                boolean granted =
                    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;

                callback.invoke(origin, granted, false);
            }
        });

        setContentView(webView);

        if (checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_REQUEST
            );
        }

        webView.loadUrl("file:///android_asset/index.html");
    }

    public class PhoneControl {

        @JavascriptInterface
        public void requestDndAccess() {
            runOnUiThread(() -> {
                Intent intent = new Intent(
                    Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
                );
                startActivity(intent);
            });
        }

        @JavascriptInterface
        public boolean isDndAccessGranted() {
            NotificationManager nm =
                (NotificationManager) getSystemService(
                    Context.NOTIFICATION_SERVICE
                );

            return nm.isNotificationPolicyAccessGranted();
        }

        @JavascriptInterface
        public boolean setDnd(boolean enabled) {
            NotificationManager nm =
                (NotificationManager) getSystemService(
                    Context.NOTIFICATION_SERVICE
                );

            if (!nm.isNotificationPolicyAccessGranted()) {
                return false;
            }

            try {
                nm.setInterruptionFilter(
                    enabled
                    ? NotificationManager.INTERRUPTION_FILTER_NONE
                    : NotificationManager.INTERRUPTION_FILTER_ALL
                );
                return true;
            } catch (Exception e) {
                return false;
            }
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
            }
