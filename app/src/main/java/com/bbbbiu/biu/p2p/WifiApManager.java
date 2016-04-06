package com.bbbbiu.biu.p2p;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 使用反射机制使{@link WifiManager}中隐藏的关于WifiAp的方法可见。
 * <p>
 * 增加功能：
 * 1.恢复开热点之前wifi状态
 * 2.HTC开热点时的API reflection
 */
public class WifiApManager {
    private static final String TAG = WifiApManager.class.getSimpleName();

    public static final String AP_SSID = "bbbbiu.com";
    
    public static final int WIFI_AP_STATE_DISABLING = 10;
    public static final int WIFI_AP_STATE_DISABLED = 11;
    public static final int WIFI_AP_STATE_ENABLING = 12;
    public static final int WIFI_AP_STATE_ENABLED = 13;
    public static final int WIFI_AP_STATE_FAILED = 14;


    private final WifiManager mWifiManager;
    private Context context;

    private int mWifiState;

    private boolean isHtc = false;

    public WifiApManager(Context context) {
        this.context = context;
        mWifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);

        mWifiState = mWifiManager.getWifiState();

        // check whether this is a HTC device
        try {
            Field field = WifiConfiguration.class.getDeclaredField("mWifiApProfile");
            isHtc = field != null;
        } catch (Exception ignore) {
        }
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

            if (isHtc) {
                setupHtcWifiConfiguration(wifiConfig);
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
            WifiConfiguration configuration = (WifiConfiguration) method.invoke(mWifiManager);

            if (isHtc) {
                configuration = getHtcWifiApConfiguration(configuration);
            }
            return configuration;

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
            if (isHtc) {
                setupHtcWifiConfiguration(wifiConfig);
            }
            Method method = mWifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
            return (Boolean) method.invoke(mWifiManager, wifiConfig);
        } catch (Exception e) {
            Log.i(TAG, "setWifiApConfiguration " + e.toString());
            return false;
        }
    }

    /**
     * 设置HTC AP。默认设置为无密码
     *
     * @param config WifiConfiguration
     */
    private void setupHtcWifiConfiguration(WifiConfiguration config) {
        try {
            Object mWifiApProfileValue = getFieldValue(config, "mWifiApProfile");

            if (mWifiApProfileValue != null) {
                setFieldValue(mWifiApProfileValue, "SSID", config.SSID);
                setFieldValue(mWifiApProfileValue, "BSSID", config.BSSID);
                setFieldValue(mWifiApProfileValue, "secureType", "open");
                setFieldValue(mWifiApProfileValue, "dhcpEnable", 1);
            }
        } catch (Exception e) {
            Log.i(TAG, "setupHtcWifiConfiguration " + e.toString());
        }
    }

    /**
     * 获取HTC AP
     *
     * @param config WifiConfiguration
     * @return
     */
    private WifiConfiguration getHtcWifiApConfiguration(WifiConfiguration config) {
        try {
            Object mWifiApProfileValue = getFieldValue(config, "mWifiApProfile");
            if (mWifiApProfileValue != null) {
                config.SSID = (String) getFieldValue(mWifiApProfileValue, "SSID");
            }
        } catch (Exception e) {
            Log.i(TAG, "getHtcWifiApConfiguration " + e.toString());
        }
        return config;
    }

    /**
     * 利用反射机制为 设置object 对应propertyName 的值为 value
     *
     * @param object
     * @param propertyName
     * @param value
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    private void setFieldValue(Object object, String propertyName, Object value)
            throws IllegalAccessException, NoSuchFieldException {
        Field field = object.getClass().getDeclaredField(propertyName);
        field.setAccessible(true);
        field.set(object, value);
    }

    /**
     * 利用反射机制为 设置object 对应propertyName 的值
     *
     * @param object
     * @param propertyName
     * @return
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    private Object getFieldValue(Object object, String propertyName)
            throws IllegalAccessException, NoSuchFieldException {
        Field field = object.getClass().getDeclaredField(propertyName);
        field.setAccessible(true);
        return field.get(object);
    }
}
