package co.aoscp.cota.services;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.app.Service;
import android.os.IBinder;
import android.os.Handler;
import android.os.HandlerThread;

import co.aoscp.cota.R;
import co.aoscp.cota.UpdateSystem;
import co.aoscp.cota.updater.Updater;
import co.aoscp.cota.updater.Updater.PackageInfo;
import co.aoscp.cota.utils.AlarmUtils;

import java.io.Serializable;

public class UpdateService extends Service {
    private static final String TAG = "COTA:UpdateService";

    public static final String FILES_INFO = "co.aoscp.cota.Utils.FILES_INFO";
    public static final int NOTIFICATION_UPDATE = 122303235;
    public static final int NOTIFICATION_INSTALL = 122303246;

    private static final String UPDATE_NOTIF_CHANNEL = "SYSUPDATE";

    private HandlerThread handlerThread;
    private Handler handler;

    private static Context mContext;
    private static NotificationManager mNoMan;

    private static NotificationManager notificationManager = null;

    public static void start(Context context) {
        start(context, null);
    }

    private static void start(Context context, String action) {
        Intent i = new Intent(context, UpdateService.class);
        i.setAction(action);
        context.startService(i);
        final NotificationChannel updateChannel = new NotificationChannel(
                UPDATE_NOTIF_CHANNEL,
                context.getString(R.string.update_system_notification_channel),
                NotificationManager.IMPORTANCE_HIGH);
        updateChannel.setBlockableSystem(true);
        updateChannel.enableLights(true);
        updateChannel.enableVibration(true);
        mNoMan.createNotificationChannel(updateChannel);
        mContext = context;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        Log.v(TAG, "onBind: Service bound");

        return null;
    }

    public void onCreate() {
        super.onCreate();

        Log.v(TAG, "onCreate: Service starting");

        handlerThread = new HandlerThread("COTA System Update Service");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        AlarmUtils.setAlarm(this, true);
        mNoMan = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public void onDestroy() {
        handlerThread.quitSafely();
        super.onDestroy();
    }

    public static void showUpdateAvailableNotification(Context context, PackageInfo[] info) {
        if (info == null) return;
        Resources resources = context.getResources();
        final int color = resources.getColor(R.color.colorPrimary);

        Intent intent = new Intent(context, UpdateSystem.class);
        NotificationInfo fileInfo = new NotificationInfo();
        fileInfo.mNotificationId = NOTIFICATION_UPDATE;
        fileInfo.mPackageInfosRom = info;
        intent.putExtra(FILES_INFO, fileInfo);
        PendingIntent pIntent = PendingIntent.getActivity(context, NOTIFICATION_UPDATE, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

        final Notification.Builder nb =
                new Notification.Builder(context, UPDATE_NOTIF_CHANNEL)
                        .setSmallIcon(R.drawable.ic_update_notification)
                        .setShowWhen(false)
                        .setContentTitle(String.format(mContext.getResources().getString(
                                R.string.update_system_notification_update_available),
                                info[0].getVersion().toString()))
                        .setContentText(resources.getString(R.string.update_system_notification_update_available_desc))
                        .setOnlyAlertOnce(false)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setOngoing(true)
                        .setContentIntent(pIntent)
                        .setColor(color);

        final Notification n = nb.build();
        mNoMan.notify(NOTIFICATION_UPDATE, n);
    }

    public static void stopNotificationInstall() {
        mNoMan.cancel(NOTIFICATION_INSTALL);
    }

    public static void cancelNotifications() {
        mNoMan.cancel(NOTIFICATION_INSTALL);
        mNoMan.cancel(NOTIFICATION_UPDATE);
    }

    public static class NotificationInfo implements Serializable {
        public int mNotificationId;
        public Updater.PackageInfo[] mPackageInfosRom;
    }
}
