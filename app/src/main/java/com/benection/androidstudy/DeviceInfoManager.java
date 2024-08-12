package com.benection.androidstudy;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

public class DeviceInfoManager {

    public static DeviceInfo getDeviceInfo(Context context) {
        String deviceUuid = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        String deviceModel = Build.MODEL;
        String systemName = "Android";
        String systemVersion = Build.VERSION.RELEASE;

        return new DeviceInfo(deviceUuid, deviceModel, systemName, systemVersion);
    }
}
