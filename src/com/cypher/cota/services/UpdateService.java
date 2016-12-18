package com.cypher.cota.services;

import com.cypher.cota.utils.AlarmUtils;

import android.content.Context;
import android.content.Intent;

import android.util.Log;
import android.app.Service;
import android.os.IBinder;

public class UpdateService extends Service {
    private static final String TAG = "COTA:UpdateService";

    @Override
    public IBinder onBind(Intent arg0) {
        Log.v(TAG, "onBind: Service bound");

        return null;
    }

    public void onCreate() {
        super.onCreate();

        Log.v(TAG, "onCreate: Service starting");

        AlarmUtils.setAlarm(this, true);
    }
}
