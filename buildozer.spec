[app]

# Название приложения
title = Cyber Compass
package.name = cybercompass
package.domain = org.cyber

# Указываем путь к исходным файлам (текущая папка)
source.dir = .

# Исходные файлы
source.include_exts = py,png,jpg,kv,atlas
version = 1.0

# Зависимости
requirements = python3,kivy,plyer,android

# Ориентация экрана
orientation = portrait

# Разрешения Android для доступа к датчикам положения и геолокации
android.permissions = INTERNET,ACCESS_FINE_LOCATION,ACCESS_COARSE_LOCATION,BODY_SENSORS

# Настройки сборщика SDK/NDK
android.api = 33
android.minapi = 21
android.sdk = 33
android.ndk = 25b
android.archs = arm64-v8a

[buildozer]
log_level = 2
warn_on_root = 1