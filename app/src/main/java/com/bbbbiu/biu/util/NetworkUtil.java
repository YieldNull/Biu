package com.bbbbiu.biu.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Collections;

public class NetworkUtil {
    private static final String TAG = NetworkUtil.class.getSimpleName();

    /**
     * 打开wifi。（已打开则直接返回）
     *
     * @param context context
     * @return 是否成功打开
     */
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


    /**
     * 检查是否连接了VPN
     *
     * @return 是否连接了VPN
     */
    public static boolean isVpnEnabled() {
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (networkInterface.isUp() && networkInterface.getName().startsWith("tun")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    /**
     * 检查设备是否联网
     *
     * @param context context
     * @return 是否联网
     */
    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo info = cm.getActiveNetworkInfo();

        return !(info == null || !info.isConnected());
    }

    /**
     * 开启或关闭数据流量，只对Android 4 有效
     *
     * @param enabled 是否开启
     * @return 是否执行成功
     */
    public static boolean setMobileDataEnabled(Context context, boolean enabled) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        try {

            final Class<?> conmanClass = Class.forName(manager.getClass()
                    .getName());
            final Field iConnectivityManagerField = conmanClass
                    .getDeclaredField("mService");
            iConnectivityManagerField.setAccessible(true);
            final Object iConnectivityManager = iConnectivityManagerField
                    .get(manager);
            final Class<?> iConnectivityManagerClass = Class
                    .forName(iConnectivityManager.getClass().getName());
            final Method setMobileDataEnabledMethod = iConnectivityManagerClass
                    .getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
            setMobileDataEnabledMethod.setAccessible(true);
            setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
            return true;

        } catch (Exception e) {
            Log.w("Mobile Data", "error turning on/off data");
            return false;
        }
    }
}