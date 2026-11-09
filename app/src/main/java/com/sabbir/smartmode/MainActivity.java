
package com.sabbir.smartmode;

import android.app.Activity;
import android.os.Bundle;
import android.Manifest;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;
    private static final int LOCATION_REQUEST = 100;

    private NotificationManager nm;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        nm = (NotificationManager)
            getSystemService(Context.NOTIFICATION_SERVICE);

        prefs = getSharedPreferences("SmartModeSettings", MODE_PRIVATE);

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
                startActivity(new Intent(
                    Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
                ));
            });
        }

        @JavascriptInterface
        public boolean isDndAccessGranted() {
            return nm.isNotificationPolicyAccessGranted();
        }

        @JavascriptInterface
        public boolean setDnd(boolean enabled) {
            if (!nm.isNotificationPolicyAccessGranted()) {
                return false;
            }

            try {
                boolean active = prefs.getBoolean("dnd_active", false);

                if (enabled && !active) {
                    int oldFilter = nm.getCurrentInterruptionFilter();

                    prefs.edit()
                        .putInt("old_filter", oldFilter)
                        .putBoolean("dnd_active", true)
                        .apply();

                    nm.setInterruptionFilter(
                        NotificationManager.INTERRUPTION_FILTER_NONE
                    );

                } else if (!enabled && active) {
                    int oldFilter = prefs.getInt(
                        "old_filter",
                        NotificationManager.INTERRUPTION_FILTER_ALL
                    );

                    nm.setInterruptionFilter(oldFilter);

                    prefs.edit()
                        .putBoolean("dnd_active", false)
                        .apply();
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

                    requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    }, LOCATION_REQUEST);

                    return;
                }

                if (!nm.isNotificationPolicyAccessGranted()) {
                    Toast.makeText(
                        MainActivity.this,
                        "আগে DND অনুমতি দিন",
                        Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                Intent service = new Intent(
                    MainActivity.this,
                    IndoorDetectionService.class
                );

                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    startForegroundService(service);
                } else {
                    startService(service);
                }

                Toast.makeText(
                    MainActivity.this,
                    "পর্যবেক্ষণ চালুর অনুরোধ পাঠানো হয়েছে",
                    Toast.LENGTH_SHORT
                ).show();
            });
        }

        @JavascriptInterface
        public void stopMonitoring() {
            stopService(new Intent(
                MainActivity.this,
                IndoorDetectionService.class
            ));

            Toast.makeText(
                MainActivity.this,
                "পর্যবেক্ষণ বন্ধ করা হয়েছে",
                Toast.LENGTH_SHORT
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
