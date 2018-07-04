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

    private HandlerThread handlerThread;
    private Handler handler;

    private static Context mContext;

    public static void start(Context context) {
        start(context, null);
    }

    private static void start(Context context, String action) {
        Intent i = new Intent(context, UpdateService.class);
        i.setAction(action);
        context.startService(i);
        mContext = context;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        Log.d(TAG, "onBind: Service bound");
        return null;
    }

    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Service starting");

        handlerThread = new HandlerThread("COTA System Update Service");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        AlarmUtils.setAlarm(this, true);
    }

    @Override
    public void onDestroy() {
        handlerThread.quitSafely();
        super.onDestroy();
    }

    public static class NotificationInfo implements Serializable {
        public int mNotificationId;
        public Updater.PackageInfo[] mPackageInfosRom;
    }
}
