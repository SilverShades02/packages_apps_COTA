package com.cypher.cota;

import android.app.Application;

import org.piwik.sdk.DownloadTracker;
import org.piwik.sdk.PiwikApplication;
import org.piwik.sdk.TrackHelper;

public class App extends PiwikApplication {
    private static App sApplication;

    @Override
    public String getTrackerUrl() {
        return "http://tracker.cypheros.co/";
    }

    @Override
    public Integer getSiteId() {
        return 2;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sApplication = this;
    }

    public static App getApplication() {
        return sApplication;
    }
}
