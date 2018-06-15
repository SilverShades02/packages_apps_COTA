package co.aoscp.cota.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class FeedbackReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case "com.aoscp.FEEDBACK":
                // TODO: Add FeedBack
                break;
        }
    }
}