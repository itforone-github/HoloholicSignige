package common;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import android.provider.Settings;

import androidx.annotation.RequiresApi;

/**
 * Created by 투덜이2 on 2017-07-14.
 */

public class Common {
    static final String PREF = "HOLOHOLIC";
    public static String TOKEN = "";
    public static String logout = "NO";

    /*
    public static String getMyNumber(Activity act){
        TelephonyManager manager =(TelephonyManager)act.getSystemService(Context.TELEPHONY_SERVICE);
        return manager.getLine1Number();
    }*/
    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static String getMyDeviceId(Context mContext) {
        String android_id = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        return android_id;
    }
    public static void savePref(Context context, String key, String value){
        SharedPreferences pref=context.getSharedPreferences(PREF, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor= pref.edit();
        editor.putString(key,value);
        editor.commit();
    }
    public static String getPref(Context context, String key, String def){
        SharedPreferences pref=context.getSharedPreferences(PREF, Activity.MODE_PRIVATE);
        String value;
        try {
            value = pref.getString(key, def);
        }catch(Exception e){
            value=def;
        }
        return value;
    }
    public static boolean getPref(Context context, String key, boolean def){
        SharedPreferences pref=context.getSharedPreferences(PREF, Activity.MODE_PRIVATE);
        boolean value;
        try {
            value = pref.getBoolean(key, def);
        }catch(Exception e){
            value=def;
        }
        return value;
    }




}
