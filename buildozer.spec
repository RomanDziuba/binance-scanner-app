[app]

# ==============================================================================
# 1. ОСНОВНЫЕ ИДЕНТИФИКАТОРЫ И МЕТАДАННЫЕ ПРИЛОЖЕНИЯ
# ==============================================================================

# Уникальное отображаемое имя приложения, которое пользователь видит на экране смартфона
title = Cyber Ultimate Master App

# Уникальное системное имя пакета приложения (формирует корневой пакет в APK/AAB)
package.name = cyberultimatemaster

# Базовый домен разработчика для создания уникального идентификатора приложения (applicationId)
package.domain = org.cyber

# Корневая директория, содержащая исходный код проекта (точка означает текущую папку репозитория)
source.dir = .

# Расширения файлов, которые будут упакованы внутрь APK (код, ресурсы, стили, базы данных, конфигурации)
source.include_exts = py,png,jpg,jpeg,gif,kv,atlas,json,txt,csv,sqlite,db,ttf,otf,ini,cfg,yaml,pem,crt

# Файлы и маски, которые исключаются из компиляции для оптимизации веса готового пакета
source.exclude_exts = spec,pyc,pyo,bak,tmp,log

# Папки, исключаемые из сканирования компилятором
source.exclude_patterns = spec/*,tests/*,.git/*,.github/*,venv/*,pycache/*

# ==============================================================================
# 2. БИБЛИОТЕКИ И ЗАВИСИМОСТИ (PYTHON-FOR-ANDROID RECIPES)
# Включает полный стек для UI, сетевых запросов, криптографии, аналитики и работы с нативным Java API через pyjnius
# ==============================================================================

requirements = python3,kivy,plyer,pyjnius,requests,urllib3,certifi,idna,charset-normalizer,cryptography,pyopenssl,numpy,pandas,matplotlib,plotly,jinja2,pydantic,sqlite3

# Версия приложения для отображения пользователю
version = 1.0

# Числовой код версии для систем магазинов приложений (обязательно увеличивается при каждом новом релизе)
version.code = 1

# Ориентация экрана при запуске: portrait (портретная), landscape (альбомная) или all (автоповорот)
orientation = portrait

# Режим отображения системных панелей: 0 — стандартный вид со строкой состояния, 1 — полный экран (Immersive Mode)
fullscreen = 0

# ==============================================================================
# 3. МАКСИМАЛЬНЫЙ ПАКЕТ РАЗРЕШЕНИЙ (ANDROID PERMISSIONS)
# Полный перечень системных прав для интеграции платежей, Google Pay, датчиков, сети и оборудования
# ==============================================================================

android.permissions = \
    com.android.vending.BILLING, \
    INTERNET, \
    ACCESS_NETWORK_STATE, \
    ACCESS_WIFI_STATE, \
    CHANGE_WIFI_STATE, \
    CHANGE_NETWORK_STATE, \
    BLUETOOTH, \
    BLUETOOTH_ADMIN, \
    BLUETOOTH_CONNECT, \
    BLUETOOTH_SCAN, \
    BLUETOOTH_ADVERTISE, \
    NFC, \
    USB_PERMISSION, \
    ACCESS_FINE_LOCATION, \
    ACCESS_COARSE_LOCATION, \
    ACCESS_BACKGROUND_LOCATION, \
    CAMERA, \
    RECORD_AUDIO, \
    MODIFY_AUDIO_SETTINGS, \
    BODY_SENSORS, \
    USE_BIOMETRIC, \
    USE_FINGERPRINT, \
    FLASHLIGHT, \
    READ_EXTERNAL_STORAGE, \
    WRITE_EXTERNAL_STORAGE, \
    READ_MEDIA_IMAGES, \
    READ_MEDIA_VIDEO, \
    READ_MEDIA_AUDIO, \
    MANAGE_EXTERNAL_STORAGE, \
    POST_NOTIFICATIONS, \
    VIBRATE, \
    WAKE_LOCK, \
    RECEIVE_BOOT_COMPLETED, \
    FOREGROUND_SERVICE, \
    FOREGROUND_SERVICE_DATA_SYNC, \
    FOREGROUND_SERVICE_CAMERA, \
    FOREGROUND_SERVICE_MICROPHONE, \
    SYSTEM_ALERT_WINDOW, \
    REQUEST_INSTALL_PACKAGES, \
    SCHEDULE_EXACT_ALARM, \
    USE_FULL_SCREEN_INTENT, \
    READ_CONTACTS, \
    WRITE_CONTACTS, \
    READ_CALENDAR, \
    WRITE_CALENDAR, \
    READ_PHONE_STATE, \
    READ_PHONE_NUMBERS, \
    ANSWER_PHONE_CALLS, \
    CALL_PHONE

# ==============================================================================
# 4. АРХИТЕКТУРНЫЕ НАСТРОЙКИ КОМПИЛЯЦИИ И СРЕДЫ ANDROID
# Конфигурация целевых API, версий SDK, NDK и процессорных архитектур
# ==============================================================================