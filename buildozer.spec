[app]

title = DRV
package.name = drv
package.domain = org.drv
source.dir = .
source.include_exts = py,png,jpg,kv,atlas
version = 1.1

requirements = python3,kivy,pyjnius,requests,certifi,urllib3,idna,charset-normalizer

orientation = portrait
fullscreen = 0

android.permissions = INTERNET,ACCESS_NETWORK_STATE,WAKE_LOCK,FOREGROUND_SERVICE
android.api = 33
android.minapi = 21
android.accept_sdk_license = True
android.archs = arm64-v8a
android.ndk = 25b
[buildozer]
log_level = 2
warn_on_root = 1
