[app]

# (str) Название вашего приложения
title = Cyber Compass

# (str) Имя пакета
package.name = cybercompass

# (str) Домен пакета
package.domain = org.cyber

# (str) Путь к исходным файлам (текущая директория)
source.dir = .

# (list) Расширения файлов, которые нужно включить в сборку
source.include_exts = py,png,jpg,kv,atlas

# (list) Зависимости приложения
requirements = python3,kivy,plyer,android

# (str) Версия приложения
version = 1.0

# (str) Ориентация экрана (portrait - портретная)
orientation = portrait

# (list) Разрешения для работы с датчиками и геолокацией
android.permissions = INTERNET,ACCESS_FINE_LOCATION,ACCESS_COARSE_LOCATION,BODY_SENSORS

# (int) Целевой API Android
android.api = 33

# (int) Минимальный поддерживаемый API
android.minapi = 21

# (int) Версия Android SDK
android.sdk = 33

# (str) Версия Android NDK
android.ndk = 25b

# (list) Архитектура процессора
android.archs = arm64-v8a

[buildozer]

# (int) Уровень логов (2 — максимальная детализация для поиска ошибок)
log_level = 2

# (int) Предупреждение при запуске от root (0 = False, 1 = True)
warn_on_root = 1