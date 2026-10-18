[app]

title = Cyber Ultimate Master App
package.name = cyberultimatemaster
package.domain = org.cyber
source.dir = .
source.include_exts = py,png,jpg,jpeg,gif,kv,atlas,json,txt,csv,sqlite,db,ttf,otf,ini,cfg,yaml,pem,crt
source.exclude_exts = spec,pyc,pyo,bak,tmp,log
source.exclude_patterns = spec/*,tests/*,.git/*,.github/*,venv/*,__pycache__/*

requirements = python3,kivy,requests,sqlite3

version = 1.0
version.code = 1
orientation = portrait
fullscreen = 0

android.permissions = \
    com.android.vending.BILLING, \
    INTERNET, \
    ACCESS_NETWORK_STATE, \
    ACCESS_WIFI_STATE, \
    CHANGE_WIFI_STATE, \
    CHANGE_NETWORK_STATE, \
    BLUETOOTH, \
    BLUETOOTH_ADMIN, \
    BLUETOOTH_CONNECT, \
    BLUETOOTH_SCAN, \
    BLUETOOTH_ADVERTISE, \
    NFC, \
    USB_PERMISSION, \
    ACCESS_FINE_LOCATION, \
    ACCESS_COARSE_LOCATION, \
    ACCESS_BACKGROUND_LOCATION, \
    CAMERA, \
    RECORD_AUDIO, \
    MODIFY_AUDIO_SETTINGS, \
    BODY_SENSORS, \
    USE_BIOMETRIC, \
    USE_FINGERPRINT, \
    FLASHLIGHT, \
    READ_EXTERNAL_STORAGE, \
    WRITE_EXTERNAL_STORAGE, \
    READ_MEDIA_IMAGES, \
    READ_MEDIA_VIDEO, \
    READ_MEDIA_AUDIO, \
    MANAGE_EXTERNAL_STORAGE, \
    POST_NOTIFICATIONS, \
    VIBRATE, \
    WAKE_LOCK, \
    RECEIVE_BOOT_COMPLETED, \
    FOREGROUND_SERVICE, \
    FOREGROUND_SERVICE_DATA_SYNC, \
    FOREGROUND_SERVICE_CAMERA, \
    FOREGROUND_SERVICE_MICROPHONE, \
    SYSTEM_ALERT_WINDOW, \
    REQUEST_INSTALL_PACKAGES, \
    SCHEDULE_EXACT_ALARM, \
    USE_FULL_SCREEN_INTENT, \
    READ_CONTACTS, \
    WRITE_CONTACTS, \
    READ_CALENDAR, \
    WRITE_CALENDAR, \
    READ_PHONE_STATE, \
    READ_PHONE_NUMBERS, \
    ANSWER_PHONE_CALLS, \
    CALL_PHONE

android.api = 34
android.minapi = 24
android.sdk = 34
android.ndk = 25b
android.archs = arm64-v8a
android.androidx = True

icon.filename = 
presplash.filename = 
android.presplash_color = #141414
android.manifest.application_attributes = android:usesCleartextTraffic="true"

[buildozer]
log_level = 2
warn_on_root = 1
build_dir = .buildozer
bin_dir = bin
