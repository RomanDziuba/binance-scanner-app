package com.roma.nativeapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.graphics.*;
import android.view.*;

private String generateManifest() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
               "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
               "    <uses-permission android:name=\"android.permission.INTERNET\" />\n" +
               "    <uses-permission android:name=\"android.permission.ACCESS_NETWORK_STATE\" />\n" +
               "    <uses-permission android:name=\"android.permission.ACCESS_WIFI_STATE\" />\n" +
               "    <uses-permission android:name=\"android.permission.CHANGE_WIFI_STATE\" />\n" +
               "    <uses-permission android:name=\"android.permission.CHANGE_WIFI_MULTICAST_STATE\" />\n" +
               "    <uses-permission android:name=\"android.permission.RECORD_AUDIO\" />\n" +
               "    <uses-permission android:name=\"android.permission.MODIFY_AUDIO_SETTINGS\" />\n" +
               "    <uses-permission android:name=\"android.permission.CAMERA\" />\n" +
               "    <uses-permission android:name=\"android.permission.FLASHLIGHT\" />\n" +
               "    <uses-permission android:name=\"android.permission.BLUETOOTH\" />\n" +
               "    <uses-permission android:name=\"android.permission.BLUETOOTH_ADMIN\" />\n" +
               "    <uses-permission android:name=\"android.permission.BLUETOOTH_CONNECT\" />\n" +
               "    <uses-permission android:name=\"android.permission.BLUETOOTH_SCAN\" />\n" +
               "    <uses-permission android:name=\"android.permission.ACCESS_FINE_LOCATION\" />\n" +
               "    <uses-permission android:name=\"android.permission.ACCESS_COARSE_LOCATION\" />\n" +
               "    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE\" />\n" +
               "    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE_MICROPHONE\" />\n" +
               "    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE_CAMERA\" />\n" +
               "    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE_LOCATION\" />\n" +
               "    <uses-permission android:name=\"android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE\" />\n" +
               "    <uses-permission android:name=\"android.permission.POST_NOTIFICATIONS\" />\n" +
               "    <uses-permission android:name=\"android.permission.WAKE_LOCK\" />\n" +
               "    <uses-permission android:name=\"android.permission.VIBRATE\" />\n" +
               "    <uses-permission android:name=\"android.permission.RECEIVE_BOOT_COMPLETED\" />\n" +
               "    <uses-permission android:name=\"com.android.vending.BILLING\" />\n" +
               "    <uses-permission android:name=\"android.permission.READ_EXTERNAL_STORAGE\" android:maxSdkVersion=\"32\" />\n" +
               "    <uses-permission android:name=\"android.permission.WRITE_EXTERNAL_STORAGE\" android:maxSdkVersion=\"32\" />\n" +
               "    <uses-permission android:name=\"android.permission.READ_MEDIA_AUDIO\" />\n" +
               "    <uses-permission android:name=\"android.permission.READ_MEDIA_IMAGES\" />\n" +
               "    <uses-permission android:name=\"android.permission.READ_MEDIA_VIDEO\" />\n" +
               "    <application android:allowBackup=\"true\" android:icon=\"@mipmap/ic_launcher\" android:label=\"" + appTitle + "\" android:supportsRtl=\"true\" android:theme=\"@android:style/Theme.Material.Light.NoActionBar\">\n" +
               "        <activity android:name=\".MainActivity\" android:exported=\"true\">\n" +
               "            <intent-filter>\n" +
               "                <action android:name=\"android.intent.action.MAIN\" />\n" +
               "                <category android:name=\"android.intent.category.LAUNCHER\" />\n" +
               "            </intent-filter>\n" +
               "        </activity>\n" +
               "        <service android:name=\".MainActivity$WalkieService\" android:foregroundServiceType=\"microphone\" android:exported=\"false\" />\n" +
               "    </application>\n" +
               "</manifest>";
    }
