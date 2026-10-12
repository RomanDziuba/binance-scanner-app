[app]

# Название вашего приложения
title = DRV

# Имя пакета
package.name = drv

# Домен приложения
package.domain = org.drv

# Исходная директория
source.dir = .

# Расширения файлов для включения в APK
source.include_exts = py,png,jpg,kv,atlas

# Версия приложения
version = 1.1

# Необходимые чистые Python-библиотеки (без тяжелых pandas/numpy)
requirements = python3,kivy,pyjnius,requests,certifi,urllib3,idna,charset-normalizer

# Ориентация экрана (только портретная)
orientation = portrait
fullscreen = 0

# Разрешения для работы с сетью
android.permissions = INTERNET,ACCESS_NETWORK_STATE

# Версии Android API
android.api = 33
android.minapi = 21

# Автоматическое принятие лицензий SDK
android.accept_sdk_license = True

# Архитектура процессора
android.archs = arm64-v8a

[buildozer]
# Уровень логирования
log_level = 2
warn_on_root = 1
