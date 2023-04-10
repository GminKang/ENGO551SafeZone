package com.example.safezone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {


    private final String TAG = "GeofenceBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        String message = intent.getStringExtra("data");
        String filter = intent.getAction();
        Log.d(TAG, "Intent filter: " + filter);
        Log.d(TAG, "Intent received."+intent+message);
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }

    }
    public static IntentFilter getIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.safezone.transition");
        return intentFilter;
    }
}
