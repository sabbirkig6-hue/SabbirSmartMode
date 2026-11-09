
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
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;
    private static final int LOCATION_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        webView.addJavascriptInterface(new PhoneControl(), "PhoneControl");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        setContentView(webView);
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
                if (enabled) {
                    return true;
                }

                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public void startMonitoring() {
            runOnUiThread(() -> {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                        new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        LOCATION_REQUEST
                    );
                    return;
                }

                NotificationManager nm =
                    (NotificationManager) getSystemService(
                        Context.NOTIFICATION_SERVICE
                    );

                if (!nm.isNotificationPolicyAccessGranted()) {
                    Toast.makeText(
                        MainActivity.this,
                        "আগে DND অনুমতি দিন",
                        Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                Intent serviceIntent = new Intent(
                    MainActivity.this,
                    IndoorDetectionService.class
                );

                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }

                Toast.makeText(
                    MainActivity.this,
                    "GPS পর্যবেক্ষণ চালু হয়েছে",
                    Toast.LENGTH_LONG
                ).show();
            });
        }

        @JavascriptInterface
        public void stopMonitoring() {
            Intent serviceIntent = new Intent(
                MainActivity.this,
                IndoorDetectionService.class
            );
            stopService(serviceIntent);

            Toast.makeText(
                MainActivity.this,
                "GPS পর্যবেক্ষণ বন্ধ হয়েছে",
                Toast.LENGTH_LONG
            ).show();
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
