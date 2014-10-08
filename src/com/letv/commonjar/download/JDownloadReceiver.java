package com.letv.commonjar.download;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import com.letv.commonjar.CLog;

public class JDownloadReceiver extends BroadcastReceiver {

    private static final String TAG = CLog.makeTag(JDownloadReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            CLog.d(TAG, "Received broadcast intent for " + Intent.ACTION_BOOT_COMPLETED);
            startService(context);
        } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
            CLog.d(TAG, "Received broadcast intent for " + Intent.ACTION_MEDIA_MOUNTED);
            // startService(context);
        } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            /*
             * NetworkInfo info = (NetworkInfo)
             * intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO); if (info != null
             * && info.isConnected()) { startService(context); }
             */
        }
    }

    private void startService(Context context) {
        context.startService(new Intent(context, JDownloadService.class));
    }
}
