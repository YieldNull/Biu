package com.bbbbiu.biu.util;

import android.content.Context;
import android.net.wifi.WifiManager;

public class NetworkUtil {
    private static final String TAG = NetworkUtil.class.getSimpleName();

    public static boolean enableWifi(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int state = wifiManager.getWifiState();
        return !(state == WifiManager.WIFI_STATE_DISABLED || state == WifiManager.WIFI_STATE_DISABLING)
                || wifiManager.setWifiEnabled(true);
    }
}