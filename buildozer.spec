[app]

title = Cyber Compass
package.name = cybercompass
package.domain = org.cyber
source.dir = .
source.include_exts = py,png,jpg,kv,atlas

requirements = kivy,plyer,android

version = 1.0
orientation = portrait
android.permissions = INTERNET,ACCESS_FINE_LOCATION,ACCESS_COARSE_LOCATION,BODY_SENSORS

android.api = 31
android.minapi = 21
android.sdk = 31
# Убираем лишнюю букву r, чтобы ссылка сформировалась корректно (android-ndk-r25b)
android.ndk = 25b
android.archs = arm64-v8a

[buildozer]
log_level = 2
warn_on_root = 1