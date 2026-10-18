[app]

# Название приложения (можете менять под каждый проект)
title = Cyber Universal App
package.name = cyberuniversal
package.domain = org.cyber

# Директория с исходниками
source.dir = .
source.include_exts = py,png,jpg,kv,atlas,json,txt

# Универсальный набор требований для большинства приложений (UI, датчики, сеть, API, парсеры)
requirements = python3,kivy,plyer,requests,urllib3,certifi,idna,charset-normalizer,android

version = 1.0
orientation = portrait

# МАКСИМАЛЬНЫЙ набор разрешений современного телефона под любые задачи:
# (Интернет, сеть, геолокация, камера, хранилище, вибрация, блокировка сна, Bluetooth, датчики тела)
android.permissions = INTERNET,ACCESS_NETWORK_STATE,ACCESS_FINE_LOCATION,ACCESS_COARSE_LOCATION,CAMERA,WRITE_EXTERNAL_STORAGE,READ_EXTERNAL_STORAGE,VIBRATE,WAKE_LOCK,BLUETOOTH,BLUETOOTH_ADMIN,BODY_SENSORS

# Поддержка современных версий Android и архитектур (включая старые и новые процессоры)
android.api = 33
android.minapi = 21
android.sdk = 33
android.ndk = 25b
android.archs = arm64-v8a, armeabi-v7a

# Автоматическое определение ориентации и иконки (если появятся)
icon.filename = 
presplash.filename = 

[buildozer]
log_level = 2
warn_on_root = 1