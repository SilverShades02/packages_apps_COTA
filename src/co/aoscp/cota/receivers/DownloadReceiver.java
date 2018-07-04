package co.aoscp.cota.receivers;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import co.aoscp.cota.UpdateNotification;

public class DownloadReceiver extends BroadcastReceiver {

    public static final String CHECK_DOWNLOADS_FINISHED = "co.aoscp.cota.Utils.CHECK_DOWNLOADS_FINISHED";
    public static final String CHECK_DOWNLOADS_ID = "co.aoscp.cota.Utils.CHECK_DOWNLOADS_ID";

    private UpdateNotification mUpdateNotification;

    @Override
    public void onReceive(Context context, Intent intent) {
        mUpdateNotification = new UpdateNotification(context);
        mUpdateNotification.showInstall();
    }

}