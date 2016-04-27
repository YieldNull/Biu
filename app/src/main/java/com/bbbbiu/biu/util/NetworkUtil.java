package com.bbbbiu.biu.util;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetworkUtil {
    private static final String TAG = NetworkUtil.class.getSimpleName();

    public static boolean enableWifi(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int state = wifiManager.getWifiState();
        return !(state == WifiManager.WIFI_STATE_DISABLED || state == WifiManager.WIFI_STATE_DISABLING)
                || wifiManager.setWifiEnabled(true);
    }

    public static InetAddress intToInetAddress(int hostAddress) {
        byte[] addressBytes = {(byte) (0xff & hostAddress),
                (byte) (0xff & (hostAddress >> 8)),
                (byte) (0xff & (hostAddress >> 16)),
                (byte) (0xff & (hostAddress >> 24))};

        try {
            return InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            Log.w(TAG, e.toString());
            return null;
        }
    }
}