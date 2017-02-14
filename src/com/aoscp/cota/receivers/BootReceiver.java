package com.aoscp.cota.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.aoscp.cota.App;
import com.aoscp.cota.utils.AlarmUtils;
import com.aoscp.cota.utils.DeviceInfoUtils;
import com.aoscp.cota.utils.PreferenceUtils;

import org.piwik.sdk.PiwikApplication;
import org.piwik.sdk.Tracker;
import org.piwik.sdk.TrackHelper;
import android.util.Log;

import java.util.HashMap;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "COTA:BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "onReceive intent received " + intent.toString());

        AlarmUtils.setAlarm(context, true);

        if (!PreferenceUtils.getPreference(context, PreferenceUtils.PROPERTY_FIRST_BOOT, false)) {
            Log.v(TAG, "onReceive:First boot, recording version");

            //App app = (App)context.getApplicationContext();
            App app = App.getApplication();

            HashMap<String, String> segmentation = new HashMap<>();
            segmentation.put("device", DeviceInfoUtils.getDevice());
            segmentation.put("version", DeviceInfoUtils.getExplicitVersion());
            TrackHelper.track().screen("First Boot").variable(0, "Device", DeviceInfoUtils.getDevice()).variable(1, "Version", DeviceInfoUtils.getExplicitVersion()).with(app.getTracker());
            PreferenceUtils.setPreference(context, PreferenceUtils.PROPERTY_FIRST_BOOT, true);
        }
    }
}
