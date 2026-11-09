package com.sabbir.smartmode;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.Context;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.JavascriptInterface;

public class MainActivity extends Activity {

    private WebView webView;
    private NotificationManager notificationManager;
    private SharedPreferences preferences;

    private static final String PREFS = "SmartModeSettings";
    private static final String KEY_OLD_FILTER = "old_filter";
    private static final String KEY_DND_ACTIVE = "dnd_active";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        notificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);

        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);

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
            return notificationManager.isNotificationPolicyAccessGranted();
        }

        @JavascriptInterface
        public boolean setDnd(boolean enabled) {

            if (!notificationManager.isNotificationPolicyAccessGranted()) {
                return false;
            }

            try {
                if (enabled) {

                    boolean alreadyActive =
                        preferences.getBoolean(KEY_DND_ACTIVE, false);

                    if (!alreadyActive) {
                        int oldFilter =
                            notificationManager.getCurrentInterruptionFilter();

                        preferences.edit()
                            .putInt(KEY_OLD_FILTER, oldFilter)
                            .putBoolean(KEY_DND_ACTIVE, true)
                            .apply();
                    }

                    notificationManager.setInterruptionFilter(
                        NotificationManager.INTERRUPTION_FILTER_NONE
                    );

                } else {

                    boolean wasActive =
                        preferences.getBoolean(KEY_DND_ACTIVE, false);

                    if (wasActive) {
                        int oldFilter = preferences.getInt(
                            KEY_OLD_FILTER,
                            NotificationManager.INTERRUPTION_FILTER_ALL
                        );

                        notificationManager.setInterruptionFilter(oldFilter);

                        preferences.edit()
                            .putBoolean(KEY_DND_ACTIVE, false)
                            .apply();
                    }
                }

                return true;

            } catch (Exception e) {
                return false;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (webView != null) {
            webView.evaluateJavascript(
                "if(typeof updateDndStatus === 'function'){updateDndStatus();}",
                null
            );
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
