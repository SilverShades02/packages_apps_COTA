package com.cypher.cota.receivers;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cypher.cota.activities.SystemActivity;

public class DownloadReceiver extends BroadcastReceiver {
    public static final String CHECK_DOWNLOADS_FINISHED = "com.cypher.cota.Utils.CHECK_DOWNLOADS_FINISHED";
    public static final String CHECK_DOWNLOADS_ID = "com.cypher.cota.Utils.CHECK_DOWNLOADS_ID";

    @Override
    public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
        Intent i = new Intent(context, SystemActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra(CHECK_DOWNLOADS_FINISHED, true);
        i.putExtra(CHECK_DOWNLOADS_ID, id);
        context.startActivity(i);
    }

}