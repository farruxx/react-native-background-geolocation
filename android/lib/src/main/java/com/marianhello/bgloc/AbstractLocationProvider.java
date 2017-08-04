/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

This is a new class
*/

package com.marianhello.bgloc;

import android.location.Location;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.media.AudioManager;
import android.media.ToneGenerator;

import com.marianhello.bgloc.data.BackgroundLocation;
import com.marianhello.cordova.JSONErrorFactory;

import org.json.JSONObject;

/**
 * AbstractLocationProvider
 */
public abstract class AbstractLocationProvider implements LocationProvider {
    private static final int PERMISSION_DENIED_ERROR_CODE = 2;
    private BackgroundLocation lastBackgroundLocation;

    protected static enum Tone {
        BEEP,
        BEEP_BEEP_BEEP,
        LONG_BEEP,
        DOODLY_DOO,
        CHIRP_CHIRP_CHIRP,
        DIALTONE
    }

    ;

    protected Integer PROVIDER_ID;
    protected LocationService locationService;
    protected Location lastLocation;
    protected Config config;

    protected ToneGenerator toneGenerator;

    protected AbstractLocationProvider(LocationService locationService) {
        this.locationService = locationService;
        this.config = locationService.getConfig();
    }

    public void onCreate() {
        toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
    }

    public void onDestroy() {
        toneGenerator.release();
        toneGenerator = null;
    }

    /**
     * Register broadcast reciever
     *
     * @param receiver
     */
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        return locationService.registerReceiver(receiver, filter);
    }

    /**
     * Unregister broadcast reciever
     *
     * @param receiver
     */
    public void unregisterReceiver(BroadcastReceiver receiver) {
        locationService.unregisterReceiver(receiver);
    }

    /**
     * Handle location as recorder by provider
     *
     * @param location
     */
    public void handleLocation(Location location) {
        if(location.getAccuracy()<15) {
            BackgroundLocation backgroundLocation = new BackgroundLocation(PROVIDER_ID, location);
            if (lastBackgroundLocation != null && isFreshLocation(lastBackgroundLocation)) {
                backgroundLocation.setDeltaDistance(getDistance(backgroundLocation, lastBackgroundLocation));
                backgroundLocation.setDeltaTime(backgroundLocation.getTime() - lastBackgroundLocation.getTime());
            }
            locationService.handleLocation(backgroundLocation);
            lastBackgroundLocation = backgroundLocation;
        }
    }

    private double getDistance(BackgroundLocation loc1, BackgroundLocation loc2) {
        double lat1 = loc1.getLatitude();
        double lng1 = loc1.getLongitude();
        double lat2 = loc2.getLatitude();
        double lng2 = loc2.getLongitude();
        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters


        distance = Math.pow(distance, 2);

        return Math.sqrt(distance);
    }

    private boolean isFreshLocation(BackgroundLocation lastBackgroundLocation) {
        if (lastBackgroundLocation != null) {
            long deltaTime = System.currentTimeMillis() - lastBackgroundLocation.getTime();
            return deltaTime < 5 * 60 * 1000;//5 minutes
        }
        return false;
    }

    /**
     * Handle stationary location with radius
     *
     * @param location
     * @param radius   radius of stationary region
     */
    public void handleStationary(Location location, float radius) {
        locationService.handleStationary(new BackgroundLocation(PROVIDER_ID, location, radius));
    }

    /**
     * Handle stationary location without radius
     *
     * @param location
     */
    public void handleStationary(Location location) {
        locationService.handleStationary(new BackgroundLocation(PROVIDER_ID, location));
    }

    /**
     * Handle security exception
     *
     * @param exception
     */
    public void handleSecurityException(SecurityException exception) {
        JSONObject error = JSONErrorFactory.getJSONError(PERMISSION_DENIED_ERROR_CODE, exception.getMessage());
        locationService.handleError(error);
    }

    /**
     * Plays debug sound
     *
     * @param name tone
     */
    protected void startTone(Tone name) {
        if (toneGenerator == null) return;

        int tone = 0;
        int duration = 1000;

        switch (name) {
            case BEEP:
                tone = ToneGenerator.TONE_PROP_BEEP;
                break;
            case BEEP_BEEP_BEEP:
                tone = ToneGenerator.TONE_CDMA_CONFIRM;
                break;
            case LONG_BEEP:
                tone = ToneGenerator.TONE_CDMA_ABBR_ALERT;
                break;
            case DOODLY_DOO:
                tone = ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE;
                break;
            case CHIRP_CHIRP_CHIRP:
                tone = ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD;
                break;
            case DIALTONE:
                tone = ToneGenerator.TONE_SUP_RINGTONE;
                break;
        }

        toneGenerator.startTone(tone, duration);
    }
}
