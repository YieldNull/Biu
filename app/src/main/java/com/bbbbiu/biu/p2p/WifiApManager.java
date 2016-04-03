package com.bbbbiu.biu.p2p;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * 使用反射机制使{@link WifiManager}中隐藏的关于WifiAp的方法可见。
 * <p/>
 * 增加恢复之前wifi状态的功能
 */
public class WifiApManager {
    public static final int WIFI_AP_STATE_DISABLING = 10;
    public static final int WIFI_AP_STATE_DISABLED = 11;
    public static final int WIFI_AP_STATE_ENABLING = 12;
    public static final int WIFI_AP_STATE_ENABLED = 13;
    public static final int WIFI_AP_STATE_FAILED = 14;


    private final WifiManager mWifiManager;
    private Context context;

    private int mWifiState;

    public WifiApManager(Context context) {
        this.context = context;
        mWifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);

        mWifiState = mWifiManager.getWifiState();
    }

    /**
     * 恢复开启热点之前的状态
     */
    protected void restoreWifiState() {
        if (mWifiState == WifiManager.WIFI_STATE_ENABLED || mWifiState == WifiManager.WIFI_STATE_ENABLING) {
            mWifiManager.setWifiEnabled(true);
        }
    }

    /**
     * Start AccessPoint mode with the specified
     * configuration. If the radio is already running in
     * AP mode, update the new configuration
     * Note that starting in access point mode disables station
     * mode operation
     *
     * @param wifiConfig SSID, security and channel details as part of WifiConfiguration
     * @return {@code true} if the operation succeeds, {@code false} otherwise
     */
    public boolean setWifiApEnabled(WifiConfiguration wifiConfig, boolean enabled) {
        try {

            if (enabled) {
                mWifiManager.setWifiEnabled(false); // 首先关闭Wifi连接
            } else {
                restoreWifiState();
            }

            // reflect
            Method method = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            return (Boolean) method.invoke(mWifiManager, wifiConfig, enabled);
        } catch (Exception e) {
            Log.e(getClass().toString(), "", e);
            return false;
        }
    }

    /**
     * Gets the Wi-Fi enabled state.
     *
     * @return One of {@link #WIFI_AP_STATE_DISABLED},
     * {@link #WIFI_AP_STATE_DISABLING}, {@link #WIFI_AP_STATE_ENABLED},
     * {@link #WIFI_AP_STATE_ENABLING}, {@link #WIFI_AP_STATE_FAILED}
     * @see #isWifiApEnabled()
     */
    public int getWifiApState() {
        try {
            Method method = mWifiManager.getClass().getMethod("getWifiApState");

            return ((Integer) method.invoke(mWifiManager));

        } catch (Exception e) {
            Log.e(this.getClass().toString(), "", e);
            return WIFI_AP_STATE_FAILED;
        }
    }

    /**
     * Return whether Wi-Fi AP is enabled or disabled.
     *
     * @return {@code true} if Wi-Fi AP is enabled
     * @see #getWifiApState()
     */
    public boolean isWifiApEnabled() {
        return getWifiApState() == WIFI_AP_STATE_ENABLED;
    }

    /**
     * Gets the Wi-Fi AP Configuration.
     *
     * @return AP details in {@link WifiConfiguration}
     */
    public WifiConfiguration getWifiApConfiguration() {
        try {
            Method method = mWifiManager.getClass().getMethod("getWifiApConfiguration");
            return (WifiConfiguration) method.invoke(mWifiManager);
        } catch (Exception e) {
            Log.e(getClass().toString(), "", e);
            return null;
        }
    }

    /**
     * Sets the Wi-Fi AP Configuration.
     *
     * @return {@code true} if the operation succeeded, {@code false} otherwise
     */
    public boolean setWifiApConfiguration(WifiConfiguration wifiConfig) {
        try {
            Method method = mWifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
            return (Boolean) method.invoke(mWifiManager, wifiConfig);
        } catch (Exception e) {
            Log.e(getClass().toString(), "", e);
            return false;
        }
    }
}
