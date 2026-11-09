
package com.sabbir.smartmode;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.content.pm.PackageManager;
import android.Manifest;

public class IndoorDetectionService extends Service {

    private static final String CHANNEL_ID = "smart_mode_channel";
    private LocationManager locationManager;

    private int weakCount = 0;
    private int strongCount = 0;

    private SharedPreferences preferences;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();

        preferences = getSharedPreferences(
            "SmartModeSettings", MODE_PRIVATE
        );

        notificationManager = (NotificationManager)
            getSystemService(Context.NOTIFICATION_SERVICE);

        locationManager = (LocationManager)
            getSystemService(Context.LOCATION_SERVICE);

        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "SABBIR SMART MODE",
                NotificationManager.IMPORTANCE_LOW
            );

            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("SABBIR SMART MODE চালু")
                .setContentText("GPS সংকেত পর্যবেক্ষণ চলছে")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .build();
        }

        return new Notification.Builder(this)
            .setContentTitle("SABBIR SMART MODE চালু")
            .setContentText("GPS সংকেত পর্যবেক্ষণ চলছে")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startForeground(101, createNotification());

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000,
                0,
                locationListener
            );
        } catch (Exception e) {
            stopSelf();
        }

        return START_STICKY;
    }

    private final LocationListener locationListener =
        new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {

            float accuracy = location.getAccuracy();

            if (accuracy >= 50) {
                weakCount++;
                strongCount = 0;

                if (weakCount >= 3) {
                    setSilent(true);
                }

            } else if (accuracy <= 25) {
                strongCount++;
                weakCount = 0;

                if (strongCount >= 3) {
                    setSilent(false);
                }
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(
            String provider, int status, Bundle extras
        ) {
        }
    };

    private void setSilent(boolean enabled) {

        if (!notificationManager.isNotificationPolicyAccessGranted()) {
            return;
        }

        try {
            boolean active = preferences.getBoolean(
                "dnd_active", false
            );

            if (enabled && !active) {

                int oldFilter =
                    notificationManager.getCurrentInterruptionFilter();

                preferences.edit()
                    .putInt("old_filter", oldFilter)
                    .putBoolean("dnd_active", true)
                    .apply();

                notificationManager.setInterruptionFilter(
                    NotificationManager.INTERRUPTION_FILTER_NONE
                );

            } else if (!enabled && active) {

                int oldFilter = preferences.getInt(
                    "old_filter",
                    NotificationManager.INTERRUPTION_FILTER_ALL
                );

                notificationManager.setInterruptionFilter(oldFilter);

                preferences.edit()
                    .putBoolean("dnd_active", false)
                    .apply();
            }

        } catch (Exception e) {
        }
    }

    @Override
    public void onDestroy() {
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
        }
