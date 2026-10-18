[app]

title = Roma
package.name = roma
package.domain = org.drv
source.dir = .
source.include_exts = py,png,jpg,jpeg,gif,kv,atlas,json,txt,csv,sqlite,db,ttf,otf,ini,cfg,yaml,pem,crt
source.exclude_exts = spec,pyc,pyo,bak,tmp,log
source.exclude_patterns = spec/*,tests/*,.git/*,.github/*,venv/*,__pycache__/*

requirements = python3,kivy,sqlite3,requests,urllib3,certifi,idna,charset-normalizer

version = 1.0
version.code = 1
orientation = portrait
fullscreen = 1

android.permissions = \
    INTERNET, \
    ACCESS_NETWORK_STATE, \
    WRITE_EXTERNAL_STORAGE, \
    READ_EXTERNAL_STORAGE

android.api = 31
android.minapi = 21
android.sdk = 31
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
