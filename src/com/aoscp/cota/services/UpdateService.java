package com.aoscp.cota.services;


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

import com.aoscp.cota.R;
import com.aoscp.cota.activities.SystemActivity;
import com.aoscp.cota.updater.Updater;
import com.aoscp.cota.utils.AlarmUtils;

import java.io.Serializable;

public class UpdateService extends Service {
    private static final String TAG = "COTA:UpdateService";
	
    public static final String FILES_INFO = "com.aoscp.cota.Utils.FILES_INFO";
    public static final int NOTIFICATION_UPDATE = 122303235;
    public static final int NOTIFICATION_INSTALL = 122303246;
    private static Updater.PackageInfo[] sPackageInfosRom = new Updater.PackageInfo[0];
	
    private HandlerThread handlerThread;
    private Handler handler;
	
    private static NotificationManager notificationManager = null;
	
    public static void start(Context context) {
        start(context, null);
    }
	
	private static void start(Context context, String action) {
        Intent i = new Intent(context, UpdateService.class);
        i.setAction(action);
        context.startService(i);
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
		
	notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
	AlarmUtils.setAlarm(this, true);
    }
	
    @Override
    public void onDestroy() {
        handlerThread.quitSafely();

        super.onDestroy();
    }
	
    public static void startNotificationUpdate(Context context, Updater.PackageInfo[] infosRom) {
        Resources resources = context.getResources();
	int color = resources.getColor(R.color.colorPrimary);

        if (infosRom != null) {
            sPackageInfosRom = infosRom;
        } else {
            infosRom = sPackageInfosRom;
        }

        Intent intent = new Intent(context, SystemActivity.class);
        NotificationInfo fileInfo = new NotificationInfo();
        fileInfo.mNotificationId = NOTIFICATION_UPDATE;
        fileInfo.mPackageInfosRom = infosRom;
        intent.putExtra(FILES_INFO, fileInfo);
        PendingIntent pIntent = PendingIntent.getActivity(context, NOTIFICATION_UPDATE, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
		.setColor(color)
                .setContentText(resources.getString(R.string.update_found_notif))
                .setSmallIcon(R.drawable.ic_update_notification)
                .setContentIntent(pIntent)
                .setOngoing(true);
				
	builder.setContentTitle(resources.getString(R.string.update_label) + " "
                + infosRom[0].getVersion().toString());
        notificationManager.notify(NOTIFICATION_UPDATE, builder.build());
    }
	
    public static void stopNotificationUpdate() {
        notificationManager.cancel(NOTIFICATION_UPDATE);
    }
	
    public static void startNotificationInstall(Context context) {
        Resources resources = context.getResources();

        Intent intent = new Intent(context, SystemActivity.class);
        NotificationInfo fileInfo = new NotificationInfo();
        fileInfo.mNotificationId = NOTIFICATION_INSTALL;
        intent.putExtra(FILES_INFO, fileInfo);
        PendingIntent pIntent = PendingIntent.getActivity(context, NOTIFICATION_INSTALL, intent,
		                        PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentText(resources.getString(R.string.update_downloaded_notif))
                .setSmallIcon(R.drawable.ic_update_notification)
                .setContentIntent(pIntent)
                .setOngoing(true);
				
	builder.setContentTitle(resources.getString(R.string.downloaded_complete));
	notificationManager.notify(NOTIFICATION_INSTALL, builder.build());
    }
	
    public static void stopNotificationInstall() {
        notificationManager.cancel(NOTIFICATION_INSTALL);
    }
	
    public static class NotificationInfo implements Serializable {
        public int mNotificationId;
        public Updater.PackageInfo[] mPackageInfosRom;
    }
}
