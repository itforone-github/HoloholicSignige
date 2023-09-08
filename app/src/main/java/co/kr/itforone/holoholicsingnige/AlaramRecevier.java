package co.kr.itforone.holoholicsingnige;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import common.Common;

public class AlaramRecevier extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        String urls[] = context.getResources().getStringArray(R.array.urlArr);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            MainActivity.MAIN_WEB_VIEW.loadUrl(urls[1]+ Common.getMyDeviceId(context));
        }
    }
}
