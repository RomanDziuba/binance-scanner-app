[app]

title = Cyber Compass
package.name = cybercompass
package.domain = org.cyber
source.dir = .
source.include_exts = py,png,jpg,kv,atlas

requirements = python3,kivy,plyer,android

version = 1.0
orientation = portrait
android.permissions = INTERNET,ACCESS_FINE_LOCATION,ACCESS_COARSE_LOCATION,BODY_SENSORS

android.api = 33
android.minapi = 21
android.sdk = 33
android.ndk = 25b
android.archs = arm64-v8a

[buildozer]
log_level = 2
warn_on_root = 1