[app]

# (str) Название вашего приложения
title = Cyber Compass

# (str) Имя пакета (без пробелов и спецсимволов)
package.name = cybercompass

# (str) Домен пакета
package.domain = org.cyber

# (str) Путь к исходным файлам приложения (текущая директория)
source.dir = .

# (list) Расширения файлов, которые будут упакованы в APK
source.include_exts = py,png,jpg,kv,atlas

# (list) Зависимости приложения (только поддерживаемые модули python-for-android)
requirements = kivy,plyer,android

# (str) Версия приложения
version = 1.0

# (str) Ориентация экрана
orientation = portrait

# (list) Разрешения Android для работы с датчиками и сетью
android.permissions = INTERNET,ACCESS_FINE_LOCATION,ACCESS_COARSE_LOCATION,BODY_SENSORS

# (int) Версии API, SDK и NDK (проверенная стабильная связка)
android.api = 31
android.minapi = 21
android.sdk = 31
android.ndk = 25b

# (list) Целевая архитектура процессора мобильного устройства
android.archs = arm64-v8a

[buildozer]

# (int) Уровень логирования (2 — максимальная детализация для отслеживания сборки)
log_level = 2

# (int) Разрешить сборку под учетной записью root (нужно для контейнеров GitHub Actions)
warn_on_root = 1