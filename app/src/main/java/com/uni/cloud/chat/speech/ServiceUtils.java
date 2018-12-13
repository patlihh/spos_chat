package com.uni.cloud.chat.speech;


import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

public class ServiceUtils {
    private static final String TAG = "LoginActivity";
    /**
     * 判断服务是否开启
     *
     * @return
     */
    public static boolean isServiceRunning(Context context, String ServiceName) {
        if (("").equals(ServiceName) || ServiceName == null)
            return false;
        ActivityManager myManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager
                .getRunningServices(30);
        for (int i = 0; i < runningService.size(); i++) {
            if (runningService.get(i).service.getClassName().toString()
                    .equals(ServiceName)) {
                Log.d(TAG, "mSpeechService is exist!" );
                return true;
            }
        }
        Log.d(TAG, "mSpeechService is not exist!" );
        return false;
    }
}