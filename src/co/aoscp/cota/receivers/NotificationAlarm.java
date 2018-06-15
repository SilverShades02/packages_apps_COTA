package co.aoscp.cota.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import co.aoscp.cota.updater.RomUpdater;
import co.aoscp.cota.utils.NetworkUtils;

public class NotificationAlarm extends BroadcastReceiver {

    private RomUpdater mRomUpdater;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mRomUpdater == null) {
            mRomUpdater = new RomUpdater(context, true);
        }
        if (NetworkUtils.isNetworkAvailable(context)) {
            mRomUpdater.check();
        }
    }
}