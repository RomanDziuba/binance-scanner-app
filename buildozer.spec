[app]
title = title = DRV
package.name = drv
package.domain = org.drv
requirements = python3,kivy,pyjnius,requests,certifi,urllib3,idna,charset-normalizer
package.name = binancescanner
package.domain = org.scanner
source.dir = .
source.include_exts = py,png,jpg,kv,atlas
version = 1.1

requirements = python3,kivy,pyjnius,requests,certifi,urllib3,idna,charset-normalizer

orientation = portrait
fullscreen = 0

android.permissions = INTERNET,ACCESS_NETWORK_STATE
android.api = 33
android.minapi = 21
#android.ndk = 25b
android.accept_sdk_license = True
android.archs = arm64-v8a

[buildozer]
log_level = 2
warn_on_root = 1
