package com.sabbir.smartmode;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;

public class IndoorDetectionService extends Service {

    private static final String CHANNEL_ID = "smart_mode_channel";

    private LocationManager locationManager;
    private NotificationManager notificationManager;

    private int updateCount = 0;
    private Location lastLocation;

    @Override
    public void onCreate() {
        super.onCreate();

        locationManager = (LocationManager)
            getSystemService(Context.LOCATION_SERVICE);

        notificationManager = (NotificationManager)
            getSystemService(Context.NOTIFICATION_SERVICE);

        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "SABBIR SMART MODE GPS",
                NotificationManager.IMPORTANCE_LOW
            );

            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("SABBIR SMART MODE")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .build();
        }

        return new Notification.Builder(this)
            .setContentTitle("SABBIR SMART MODE")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startForeground(
            101,
            createNotification("GPS তথ্যের অপেক্ষায়...")
        );

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

            Location previous = locationManager.getLastKnownLocation(
                LocationManager.GPS_PROVIDER
            );

            if (previous != null) {
                showGpsInfo(previous);
            }

        } catch (SecurityException | IllegalArgumentException e) {
            updateNotification("GPS চালু করা যায়নি");
        }

        return START_STICKY;
    }

    private final LocationListener locationListener =
        new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            showGpsInfo(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
            updateNotification("ফোনের GPS বন্ধ আছে");
        }

        @Override
        public void onProviderEnabled(String provider) {
            updateNotification("GPS চালু হয়েছে");
        }

        @Override
        public void onStatusChanged(
            String provider, int status, Bundle extras
        ) {
        }
    };

    private void showGpsInfo(Location location) {

        updateCount++;
        lastLocation = location;

        float accuracy = location.getAccuracy();

        String message =
            "GPS পাওয়া গেছে | Accuracy: "
            + accuracy + " মিটার | আপডেট: "
            + updateCount;

        updateNotification(message);

        // এই সংস্করণে GPS তথ্য পরীক্ষা করা হচ্ছে।
        // এখনো স্বয়ংক্রিয় সাইলেন্ট চালু করা হচ্ছে না।
    }

    private void updateNotification(String message) {
        notificationManager.notify(
            101,
            createNotification(message)
        );
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
