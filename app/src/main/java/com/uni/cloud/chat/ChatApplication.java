package com.uni.cloud.chat;

import android.app.ActivityManager;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
//import android.support.multidex.MultiDex;
import android.util.Log;

import com.uni.cloud.chat.speech.SpeechService;
import com.uni.cloud.chat.speech.User;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//import com.uni.cloud.android.business.InitBusiness;

/**
 * 作者：叶应是叶
 * 时间：2017/11/29 20:57
 * 说明：
 */
public class ChatApplication extends Application {

    public static String identifier = "";
    private static final String TAG = "ChatApplication";

    private static String ower_id;
    private static String ower_password;

    private String ower_name;
    private String mac;

    public static ArrayList<User> mUsers = new ArrayList<>();
    public boolean isLogin = false;



    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate..." );
        mac = GetWifiMac();
        Log.d(TAG, "onCreate() mac="+mac);

        Log.d(TAG, "onCreate() startService SpeechService");
        Intent Netintent = new Intent(this, SpeechService.class);
        startService(Netintent);

        IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK);
        filter.addAction("com.ut.active.result");
        MyBroadcastReceiver receiver = new MyBroadcastReceiver();
        registerReceiver(receiver, filter);

//        InitBusiness.init(this);
    }

    public  String GetWifiMac()
    {
//        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
//        WifiInfo info = wifi.getConnectionInfo();
//        return info.getMacAddress();
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:",b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
        }
        return "02:00:00:00:00:00";

    }

    public static String getLocalVersionName(Context ctx) {
        String localVersion = "";
        try {
            PackageInfo packageInfo = ctx.getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0);
            localVersion = packageInfo.versionName;
            Log.d("TAG", "当前版本名称：" + localVersion);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return localVersion;
    }

    public String getOwer_id() {
        return ower_id;
    }

    public void setOwer_id(String ower_id) {
        this.ower_id = ower_id;
    }

    public String getOwer_password() {
        return ower_password;
    }

    public void setOwer_password(String ower_password) {
        this.ower_password = ower_password;
    }

    public String getOwer_name() {
        return ower_name;
    }

    public void setOwer_name(String ower_name) {
        this.ower_name = ower_name;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    @Override
    public void onTerminate() {
        Log.d(TAG, "onTerminate..." );
        super.onTerminate();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
   //     MultiDex.install(this);
    }
    public class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "ACTION_TIME_TICK broadcase");
            if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
                Log.d(TAG, "CheckAndStartService");
//                log("CheckAndStartService");
                CheckAndStartService();
            }else if (intent.getAction().equals("com.ut.active.result")) {
//                if (UTSS.GetResult() != CmdBase.CMD_SET_SUCCESS
//                        &&UTSS.GetResult() != CmdBase.CMD_SET_FAIL
//                        &&UTSS.GetResult() != CmdBase.CMD_NET_FAIL) {
//
//                    android.os.Process.killProcess(android.os.Process.myPid()); // 结束进程之前可以把你程序的注销或者退出代码放在这段代码之前

//                }
            }

        }

    }


    public void CheckAndStartService() {
        boolean isServiceRunning = false;
        ActivityManager manager = (ActivityManager) this
                .getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager
                .getRunningServices(Integer.MAX_VALUE)) {
            if ("com.uni.cloud.chat.speech.SpeechService".equals(service.service
                    .getClassName()))
            // Service的类名
            {
                Log.d(TAG, "Service is Running ");
                isServiceRunning = true;
            }

        }

        if (!isServiceRunning) {
            Log.d(TAG, "Service is restarting ");
            //            log("Service is restarting ");
            Intent i = new Intent(this, SpeechService.class);
            startService(i);
        }

    }

}
