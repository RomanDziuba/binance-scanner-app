import threading
import time
import os
import requests
import math
from datetime import datetime
from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.gridlayout import GridLayout
from kivy.uix.scrollview import ScrollView
from kivy.uix.label import Label
from kivy.uix.button import Button
from kivy.uix.textinput import TextInput
from kivy.uix.switch import Switch
from kivy.uix.spinner import Spinner
from kivy.uix.widget import Widget
from kivy.uix.progressbar import ProgressBar
from kivy.uix.tabbedpanel import TabbedPanel, TabbedPanelItem
from kivy.clock import Clock
from kivy.graphics import Color, RoundedRectangle, Line

# =====================================================================
# ⚙️ НАСТРОЙКИ СКАНЕРА И ПАРАМЕТРОВ
# =====================================================================
CONFIG = {
    "MIN_VOLUME_USDT": 5_000_000,
    "FILTER_LOW_VOLUME": True,
    "VOLUME_SPIKE_MULTIPLIER": 2.0,
    "FILTER_RISK_REWARD": True,
    "MAX_SL_TP_RATIO": 1.0,
    "FILTER_MIN_PROFIT": True,
    "MIN_PROFIT_PCT": 1.2,
    "FILTER_MAX_PROFIT": True,
    "MAX_PROFIT_PCT": 3.0,
    "FILTER_BTC_TREND": True,
    "FILTER_LIQUIDITY_SWEEP": True,
    "SWEEP_MIN_ATR_RATIO": 0.2,
    "SWEEP_MAX_ATR_RATIO": 1.5,
    "FILTER_TAKER_RATIO": True,
    "MIN_TAKER_BUY_RATIO": 0.65,
    "FILTER_ATR_SPIKE": True,
    "MAX_ATR_MULTIPLIER": 2.5,
    "FILTER_FUNDING_RATE": True,
    "MAX_ABSOLUTE_FUNDING_RATE": 0.0005,
    "FILTER_KAMA_EFFICIENCY": True,
    "KAMA_ER_PERIOD": 10,
    "MIN_EFFICIENCY_RATIO": 0.30,
    "TIMEFRAME_MAIN": "15m",
    "TIMEFRAME_CONF": "30m",
    "SCAN_TIMEFRAME": "5m",
    "PAUSE_AFTER_SCAN_SEC": 60,
    "INTERVAL_BETWEEN_SCANS_SEC": 300,
    "SIGNAL_MODE": "Простой",
    "SEND_DETAILED_INFO": True,
    "ENABLE_TELEGRAM": True,
    "BOT_TOKEN": "8740928704:AAGBdKD81m8K9dqbwH4OviaizzCzT-BkVrI",
    "CHAT_ID": "1434233986"
}

is_scanner_running = False
scanner_thread = None
lock = threading.Lock()
LAST_ALERT_TIME = {}

BTC_TREND_CACHE = {"is_bullish": True, "is_bearish": False, "timestamp": 0}
btc_trend_lock = threading.Lock()

FUNDING_RATE_CACHE = {}
funding_rate_lock = threading.Lock()

def get_price_fmt(price):
    if price == 0: return "{:.2f}"
    if price >= 100: return "{:.2f}"
    elif price >= 1: return "{:.4f}"
    elif price >= 0.001: return "{:.6f}"
    else: return "{:.8f}"

# =====================================================================
# АНАЛОГИ PANDAS / NUMPY НА ЧИСТОМ PYTHON
# =====================================================================
def calculate_ema(values, span):
    if not values: return []
    k = 2 / (span + 1)
    ema_list = [values[0]]
    for val in values[1:]:
        ema_list.append(val * k + ema_list[-1] * (1 - k))
    return ema_list

def calculate_rolling_mean(values, window):
    res = []
    for i in range(len(values)):
        start = max(0, i - window + 1)
        chunk = values[start:i+1]
        res.append(sum(chunk) / len(chunk))
    return res

def calculate_rolling_max(values, window):
    res = []
    for i in range(len(values)):
        start = max(0, i - window + 1)
        chunk = values[start:i+1]
        res.append(max(chunk))
    return res

def calculate_rolling_min(values, window):
    res = []
    for i in range(len(values)):
        start = max(0, i - window + 1)
        chunk = values[start:i+1]
        res.append(min(chunk))
    return res

# =====================================================================
# СТИЛЬНЫЙ ИНТЕРФЕЙС КИБЕРПАНК С КРУПНЫМИ ШРИФТАМИ
# =====================================================================
class StyledButton(Button):
    def __init__(self, bg_color=(0, 0.9, 0.9, 1), **kwargs):
        super().__init__(**kwargs)
        self.background_normal = ''
        self.background_color = (0, 0, 0, 0)
        self.custom_bg = bg_color
        self.color = (1, 1, 1, 1)
        self.bold = True
        self.font_size = 18
        self.bind(pos=self.update_canvas, size=self.update_canvas)

    def update_canvas(self, *args):
        self.canvas.before.clear()
        with self.canvas.before:
            Color(*self.custom_bg)
            RoundedRectangle(pos=self.pos, size=self.size, radius=[12])

class CircularNeonTimer(Widget):
    """Большой круговой неоновый таймер"""
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.progress = 1.0
        self.bind(pos=self.update_canvas, size=self.update_canvas)

    def set_progress(self, current_sec, total_sec):
        if total_sec > 0:
            self.progress = max(0.0, min(1.0, current_sec / total_sec))
        else:
            self.progress = 0.0
        self.update_canvas()

    def update_canvas(self, *args):
        self.canvas.before.clear()
        with self.canvas.before:
            Color(0.12, 0.16, 0.22, 1)
            Line(ellipse=(self.x + 12, self.y + 12, self.width - 24, self.height - 24), width=10)
            
            Color(0, 0.9, 1, 1)
            cx = self.center_x
            cy = self.center_y
            r = min(self.width, self.height) / 2 - 12
            if r > 0:
                angle = 360 * self.progress
                Line(circle=(cx, cy, r, 0, angle), width=10)

class ScannerAppUI(TabbedPanel):
    def __init__(self, **kwargs):
        super(ScannerAppUI, self).__init__(**kwargs)
        self.do_default_tab = False
        self.background_color = (0.05, 0.07, 0.09, 1)

        # --- Таб 1: СКАНЕР ---
        tab_control = TabbedPanelItem(text='СКАНЕР')
        control_layout = BoxLayout(orientation='vertical', padding=15, spacing=12)
        
        # Информационная карточка статуса
        status_card = BoxLayout(orientation='vertical', size_hint_y=None, height=85, padding=10, spacing=6)
        with status_card.canvas.before:
            Color(0.09, 0.13, 0.18, 1)
            RoundedRectangle(pos=status_card.pos, size=status_card.size, radius=[12])
        status_card.bind(pos=self._update_rect, size=self._update_rect)

        self.status_label = Label(text='СТАТУС: ГОТОВ К РАБОТЕ', font_size=18, bold=True, color=(0, 0.9, 1, 1))
        self.info_sub_label = Label(text='Ожидание запуска системы...', font_size=14, color=(0.7, 0.8, 0.9, 1))
        status_card.add_widget(self.status_label)
        status_card.add_widget(self.info_sub_label)
        control_layout.add_widget(status_card)

        # Большой круговой таймер с крупными цифрами посередине (180x180)
        timer_container = BoxLayout(orientation='horizontal', size_hint_y=None, height=190)
        timer_container.add_widget(Widget())
        
        self.circular_timer = CircularNeonTimer(size_hint=(None, None), size=(180, 180))
        self.timer_big_label = Label(text="05:00", font_size=36, bold=True, color=(0, 1, 0.8, 1), size_hint=(None, None), size=(180, 180))
        
        timer_stack = RelativeTimerLayout()
        timer_stack.add_widget(self.circular_timer)
        timer_stack.add_widget(self.timer_big_label)
        
        timer_container.add_widget(timer_stack)
        timer_container.add_widget(Widget())
        control_layout.add_widget(timer_container)

        # Прогресс-бар сканирования
        self.progress_bar = ProgressBar(max=100, value=0, size_hint_y=None, height=18)
        control_layout.add_widget(self.progress_bar)

        # Кнопки управления (крупные, симметричные, без битых иконок)
        btn_layout = BoxLayout(size_hint_y=None, height=56, spacing=15)
        self.start_btn = StyledButton(text='ЗАПУСК', bg_color=(0, 0.8, 0.4, 1))
        self.start_btn.bind(on_press=self.start_scanner)
        self.stop_btn = StyledButton(text='СТОП', bg_color=(0.9, 0.2, 0.3, 1))
        self.stop_btn.disabled = True
        self.stop_btn.bind(on_press=self.stop_scanner)
        
        btn_layout.add_widget(self.start_btn)
        btn_layout.add_widget(self.stop_btn)
        control_layout.add_widget(btn_layout)

        # Терминал логов
        control_layout.add_widget(Label(text='ТЕРМИНАЛ СИГНАЛОВ', size_hint_y=None, height=25, bold=True, color=(0.5, 0.7, 0.9, 1), font_size=16))
        
        self.log_scroll = ScrollView()
        self.log_label = Label(text='Система DRV инициализирована.\n', size_hint_y=None, halign='left', valign='top', color=(0, 1, 0.8, 1), font_name='Roboto', font_size=15)
        self.log_label.bind(texture_size=lambda s, w: setattr(s, 'height', w[1]))
        self.log_label.bind(size=lambda s, w: setattr(s, 'text_size', (w[0], None)))
        self.log_scroll.add_widget(self.log_label)
        control_layout.add_widget(self.log_scroll)
        
        tab_control.add_widget(control_layout)
        self.add_widget(tab_control)

        # --- Таб 2: НАСТРОЙКИ ---
        tab_settings = TabbedPanelItem(text='НАСТРОЙКИ')
        settings_scroll = ScrollView()
        settings_layout = GridLayout(cols=1, spacing=14, size_hint_y=None, padding=15)
        settings_layout.bind(minimum_height=settings_layout.setter('height'))

        settings_layout.add_widget(Label(text='Режим выдачи сигналов:', size_hint_y=None, height=25, color=(0, 0.9, 1, 1), bold=True, font_size=16))
        self.signal_mode_spinner = Spinner(text=CONFIG["SIGNAL_MODE"], values=('Простой', 'Расширенный'), size_hint_y=None, height=48)
        self.signal_mode_spinner.font_size = 16
        settings_layout.add_widget(self.signal_mode_spinner)

        self.inputs = {}
        fields = [
            ("MIN_VOLUME_USDT", "Мин. объем пары (USDT):"),
            ("VOLUME_SPIKE_MULTIPLIER", "Множитель объема свечи:"),
            ("MIN_PROFIT_PCT", "Мин. потенциал профита (%):"),
            ("MAX_PROFIT_PCT", "Макс. потенциал профита (%):"),
            ("SWEEP_MIN_ATR_RATIO", "Мин. прокол ATR (Sweep):"),
            ("SWEEP_MAX_ATR_RATIO", "Макс. прокол ATR (Sweep):"),
            ("MIN_TAKER_BUY_RATIO", "Мин. доля Taker Buy (0.65 = 65%):"),
            ("MAX_ATR_MULTIPLIER", "Макс. множитель ATR Spike:"),
            ("MAX_ABSOLUTE_FUNDING_RATE", "Макс. фандинг (0.0005 = 0.05%):"),
            ("MIN_EFFICIENCY_RATIO", "Мин. коэффициент KAMA ER:"),
            ("PAUSE_AFTER_SCAN_SEC", "Пауза после скана (сек):"),
            ("INTERVAL_BETWEEN_SCANS_SEC", "Интервал между сканами (сек):")
        ]
        
        for key, label_text in fields:
            settings_layout.add_widget(Label(text=label_text, size_hint_y=None, height=24, color=(0.8, 0.9, 1, 1), font_size=15))
            # Увеличенная высота и размер шрифта для полей ввода, чтобы цифры были идеально видны
            ti = TextInput(text=str(CONFIG[key]), multiline=False, size_hint_y=None, height=48, background_color=(0.12, 0.16, 0.22, 1), foreground_color=(1, 1, 1, 1), font_size=16)
            self.inputs[key] = ti
            settings_layout.add_widget(ti)
            
        save_btn = StyledButton(text='СОХРАНИТЬ НАСТРОЙКИ', bg_color=(0, 0.6, 0.9, 1), size_hint_y=None, height=54)
        save_btn.bind(on_press=self.save_settings)
        settings_layout.add_widget(save_btn)
        
        settings_scroll.add_widget(settings_layout)
        tab_settings.add_widget(settings_scroll)
        self.add_widget(tab_settings)

        # --- Таб 3: TELEGRAM ---
        tab_tg = TabbedPanelItem(text='TELEGRAM')
        tg_layout = GridLayout(cols=1, spacing=16, padding=20, size_hint_y=None)
        tg_layout.bind(minimum_height=tg_layout.setter('height'))
        
        tg_layout.add_widget(Label(text='Отправка уведомлений:', size_hint_y=None, height=25, color=(0, 0.9, 1, 1), bold=True, font_size=16))
        self.tg_switch = Switch(active=CONFIG["ENABLE_TELEGRAM"], size_hint_y=None, height=44)
        tg_layout.add_widget(self.tg_switch)

        tg_layout.add_widget(Label(text='Полный отчет в Telegram (все фильтры):', size_hint_y=None, height=25, color=(0, 0.9, 1, 1), bold=True, font_size=16))
        self.full_report_switch = Switch(active=CONFIG["SEND_DETAILED_INFO"], size_hint_y=None, height=44)
        tg_layout.add_widget(self.full_report_switch)
        
        tg_layout.add_widget(Label(text='Bot Token:', size_hint_y=None, height=24, color=(0.8, 0.9, 1, 1), font_size=15))
        self.bot_token_input = TextInput(text=CONFIG["BOT_TOKEN"], multiline=False, size_hint_y=None, height=48, background_color=(0.12, 0.16, 0.22, 1), foreground_color=(1, 1, 1, 1), font_size=15)
        tg_layout.add_widget(self.bot_token_input)
        
        tg_layout.add_widget(Label(text='Chat ID:', size_hint_y=None, height=24, color=(0.8, 0.9, 1, 1), font_size=15))
        self.chat_id_input = TextInput(text=CONFIG["CHAT_ID"], multiline=False, size_hint_y=None, height=48, background_color=(0.12, 0.16, 0.22, 1), foreground_color=(1, 1, 1, 1), font_size=15)
        tg_layout.add_widget(self.chat_id_input)
        
        save_tg_btn = StyledButton(text='СОХРАНИТЬ TELEGRAM', bg_color=(0, 0.6, 0.9, 1), size_hint_y=None, height=54)
        save_tg_btn.bind(on_press=self.save_telegram_settings)
        tg_layout.add_widget(save_tg_btn)
        
        tab_tg.add_widget(tg_layout)
        self.add_widget(tab_tg)

    def _update_rect(self, instance, value):
        instance.canvas.before.clear()
        with instance.canvas.before:
            Color(0.09, 0.13, 0.18, 1)
            RoundedRectangle(pos=instance.pos, size=instance.size, radius=[12])

    def log(self, message):
        timestamp = datetime.now().strftime('[%H:%M:%S] ')
        Clock.schedule_once(lambda dt: self._update_logs(timestamp + message + '\n'))

    def clear_logs(self):
        Clock.schedule_once(lambda dt: setattr(self.log_label, 'text', ''))

    def update_progress(self, current, total):
        Clock.schedule_once(lambda dt: self._set_progress(current, total))

    def _set_progress(self, current, total):
        self.progress_bar.max = total
        self.progress_bar.value = current

    def update_timer_display(self, current_sec, total_sec, status_text):
        Clock.schedule_once(lambda dt: self._apply_timer(current_sec, total_sec, status_text))

    def _apply_timer(self, current_sec, total_sec, status_text):
        self.circular_timer.set_progress(current_sec, total_sec)
        m, s = divmod(max(0, current_sec), 60)
        self.timer_big_label.text = f"{m:02d}:{s:02d}"
        self.info_sub_label.text = status_text

    def play_alert_sound(self):
        try:
            print('\a')
        except Exception:
            pass

    def _update_logs(self, text):
        self.log_label.text += text

    def save_settings(self, instance):
        try:
            for key, ti in self.inputs.items():
                CONFIG[key] = float(ti.text) if '.' in ti.text else int(ti.text)
            CONFIG["SIGNAL_MODE"] = self.signal_mode_spinner.text
            self.log("Настройки успешно сохранены!")
        except ValueError:
            self.log("Ошибка: проверьте числовые поля!")

    def save_telegram_settings(self, instance):
        CONFIG["ENABLE_TELEGRAM"] = self.tg_switch.active
        CONFIG["SEND_DETAILED_INFO"] = self.full_report_switch.active
        CONFIG["BOT_TOKEN"] = self.bot_token_input.text.strip()
        CONFIG["CHAT_ID"] = self.chat_id_input.text.strip()
        self.log("Настройки Telegram сохранены!")

    def start_scanner(self, instance):
        global is_scanner_running, scanner_thread
        if is_scanner_running: return
        is_scanner_running = True
        self.status_label.text = 'СТАТУС: СКАНИРОВАНИЕ'
        self.start_btn.disabled = True
        self.stop_btn.disabled = False
        
        scanner_thread = threading.Thread(target=scanner_main_loop, args=(self,), daemon=True)
        scanner_thread.start()
        self.log("Сканер DRV запущен в фоновом режиме.")

    def stop_scanner(self, instance):
        global is_scanner_running
        is_scanner_running = False
        self.status_label.text = 'СТАТУС: ОСТАНОВЛЕН'
        self.info_sub_label.text = 'Ожидание запуска...'
        self.start_btn.disabled = False
        self.stop_btn.disabled = True
        self.update_progress(0, 100)
        self.circular_timer.set_progress(0, 1)
        self.timer_big_label.text = "00:00"
        self.log("Сканер остановлен.")

class RelativeTimerLayout(Widget):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.size_hint = (None, None)
        self.size = (180, 180)

    def add_widget(self, widget):
        widget.pos = self.pos
        return super().add_widget(widget)

    def on_pos(self, *args):
        for child in self.children:
            child.pos = self.pos

# =====================================================================
# ТОРГОВАЯ ЛОГИКА И АНАЛИТИКА (10 ФИЛЬТРОВ)
# =====================================================================
def send_telegram_alert(symbol, msg, ui_app):
    if not CONFIG["ENABLE_TELEGRAM"]:
        ui_app.play_alert_sound()
        return
    def _send():
        url = f"https://api.telegram.org/bot{CONFIG['BOT_TOKEN']}/sendMessage"
        payload = {"chat_id": CONFIG["CHAT_ID"], "text": msg, "parse_mode": "HTML", "disable_web_page_preview": True}
        try:
            res = requests.post(url, json=payload, timeout=5)
            res.raise_for_status()
            ui_app.log(f"Сигнал {symbol} отправлен в Telegram!")
        except Exception as e:
            ui_app.log(f"Ошибка Telegram ({symbol}): {e}")
    threading.Thread(target=_send, daemon=True).start()

def check_internet_connection():
    try:
        requests.get("https://fapi.binance.com/fapi/v1/ping", timeout=3)
        return True
    except Exception:
        return False

def get_active_futures_pairs():
    url = "https://fapi.binance.com/fapi/v1/ticker/24hr"
    try:
        res = requests.get(url, timeout=10)
        res.raise_for_status()
        valid_pairs = []
        for item in res.json():
            if item['symbol'].endswith('USDT'):
                if float(item['quoteVolume']) >= CONFIG["MIN_VOLUME_USDT"]:
                    valid_pairs.append(item['symbol'])
        return valid_pairs
    except Exception:
        return []

def fetch_klines(symbol, timeframe="15m", limit=200):
    url = "https://fapi.binance.com/fapi/v1/klines"
    params = {"symbol": symbol, "interval": timeframe, "limit": limit}
    try:
        res = requests.get(url, params=params, timeout=5)
        res.raise_for_status()
        candles = []
        for row in res.json():
            candles.append({
                "timestamp": datetime.fromtimestamp(int(row[0]) / 1000.0),
                "open": float(row[1]),
                "high": float(row[2]),
                "low": float(row[3]),
                "close": float(row[4]),
                "volume": float(row[5]),
                "tb_base_av": float(row[9])
            })
        return candles
    except Exception:
        return None

def update_market_context():
    candles_btc = fetch_klines("BTCUSDT", CONFIG["TIMEFRAME_CONF"], limit=50)
    if candles_btc and len(candles_btc) > 64:
        closes = [c["close"] for c in candles_btc]
        emas = calculate_ema(closes, 64)
        with btc_trend_lock:
            BTC_TREND_CACHE["is_bullish"] = closes[-2] > emas[-2]
            BTC_TREND_CACHE["is_bearish"] = closes[-2] < emas[-2]

    if CONFIG["FILTER_FUNDING_RATE"]:
        try:
            res = requests.get("https://fapi.binance.com/fapi/v1/premiumIndex", timeout=5)
            res.raise_for_status()
            new_rates = {item['symbol']: float(item.get('lastFundingRate', 0.0)) for item in res.json()}
            with funding_rate_lock:
                global FUNDING_RATE_CACHE
                FUNDING_RATE_CACHE = new_rates
        except Exception:
            pass

def calc_murrey_math(candles, base_lookback=64):
    highs = [c["high"] for c in candles]
    lows = [c["low"] for c in candles]
    closes = [c["close"] for c in candles]
    opens = [c["open"] for c in candles]

    tr_list = []
    for i in range(len(candles)):
        hl = highs[i] - lows[i]
        hc = abs(highs[i] - closes[i-1]) if i > 0 else hl
        lc = abs(lows[i] - closes[i-1]) if i > 0 else 0
        tr_list.append(max(hl, hc, lc))

    atr_list = calculate_rolling_mean(tr_list, 14)
    atr = atr_list[-1] if atr_list else (highs[-1] - lows[-1])
    if atr == 0 or atr is None: atr = highs[-1] - lows[-1]

    volatility_ratio = tr_list[-1] / (atr + 1e-8) if (atr + 1e-8) > 0 else 1.0
    adaptive_lookback = int(base_lookback * 0.75) if volatility_ratio > 1.5 else base_lookback

    smoothed_high = [(highs[i] + max(opens[i], closes[i])) / 2 for i in range(len(candles))]
    smoothed_low = [(lows[i] + min(opens[i], closes[i])) / 2 for i in range(len(candles))]

    hh = calculate_rolling_max(smoothed_high, adaptive_lookback)[-1]
    ll = calculate_rolling_min(smoothed_low, adaptive_lookback)[-1]

    range_val = hh - ll
    step = range_val / 8 if range_val > 0 else 1.0

    m_vals = {
        "+2/8": hh + 2 * step, "+1/8": hh + step, "8/8": hh, "7/8": hh - step,
        "6/8": hh - 2 * step, "5/8": hh - 3 * step, "4/8": hh - 4 * step,
        "3/8": hh - 5 * step, "2/8": hh - 6 * step, "1/8": hh - 7 * step,
        "0/8": ll, "-1/8": ll - step, "-2/8": ll - 2 * step
    }
    return m_vals, step, atr, atr_list

def calc_kama_efficiency(candles, period=10):
    closes = [c["close"] for c in candles]
    if len(closes) < period + 1: return 0.0
    change = abs(closes[-1] - closes[-1 - period])
    volatility = sum(abs(closes[i] - closes[i-1]) for i in range(len(closes) - period, len(closes)))
    if volatility == 0: return 0.0
    return change / volatility

def check_sweep(c_low, c_high, c_close, c_open, level, is_bullish, atr):
    if not CONFIG["FILTER_LIQUIDITY_SWEEP"]:
        return (c_close > level and c_open <= level) if is_bullish else (c_close < level and c_open >= level)
        
    if atr is None or atr == 0: atr = (c_high - c_low) if (c_high - c_low) > 0 else level * 0.001

    if is_bullish:
        if c_low < level and c_close > level:
            penetration_ratio = (level - c_low) / atr
            if CONFIG["SWEEP_MIN_ATR_RATIO"] <= penetration_ratio <= CONFIG["SWEEP_MAX_ATR_RATIO"]: return True
        if c_close > level and c_open <= level: return True
    else:
        if c_high > level and c_close < level:
            penetration_ratio = (c_high - level) / atr
            if CONFIG["SWEEP_MIN_ATR_RATIO"] <= penetration_ratio <= CONFIG["SWEEP_MAX_ATR_RATIO"]: return True
        if c_close < level and c_open >= level: return True
    return False

def analyze_and_format_signal(symbol, df15, m15, step15, atr15, atr_list15, df30, m30, step30, atr30):
    curr_price = df15[-1]['close']
    fmt = get_price_fmt(curr_price)
    f = lambda v: fmt.format(v)
    
    c15, c30 = df15[-2], df30[-2]
    volumes15 = [c['volume'] for c in df15]
    avg_vol15 = calculate_rolling_mean(volumes15, 20)[-2]
    is_high_vol15 = c15['volume'] > (avg_vol15 * CONFIG["VOLUME_SPIKE_MULTIPLIER"])
    
    if CONFIG["FILTER_ATR_SPIKE"]:
        atr_rolling_mean = calculate_rolling_mean(atr_list15, 20)[-2]
        if atr_rolling_mean > 0 and (atr_list15[-2] / atr_rolling_mean) > CONFIG["MAX_ATR_MULTIPLIER"]:
            return None
                    
    total_vol = c15['volume']
    taker_buy_ratio = (c15['tb_base_av'] / total_vol) if total_vol > 0 else 0.5
    current_er = calc_kama_efficiency(df15, CONFIG["KAMA_ER_PERIOD"])
    
    closes30 = [c['close'] for c in df30]
    ema30_val = calculate_ema(closes30, 64)[-2]
    
    tp_zone, sl_level, is_active_signal, signal_direction, title = None, None, False, None, ""

    if m15["3/8"] <= curr_price <= m15["5/8"]:
        if check_sweep(c15['low'], c15['high'], c15['close'], c15['open'], m15["5/8"], True, atr15) and is_high_vol15:
            is_active_signal, signal_direction = True, "bullish"
            tp_zone = (m30["7/8"], m30["8/8"]) if (c30['close'] > ema30_val) else (m15["7/8"], m15["8/8"])
            sl_level = m30["-2/8"]
            title = "ЗАКРЕПЛЕНИЕ ВЫШЕ 5/8 (ЛОНГ)"
        elif check_sweep(c15['low'], c15['high'], c15['close'], c15['open'], m15["3/8"], False, atr15) and is_high_vol15:
            is_active_signal, signal_direction = True, "bearish"
            tp_zone = (m30["0/8"], m30["1/8"]) if (c30['close'] < ema30_val) else (m15["0/8"], m15["1/8"])
            sl_level = m30["+2/8"]
            title = "ЗАКРЕПЛЕНИЕ НИЖЕ 3/8 (ШОРТ)"
    elif curr_price >= m15["8/8"]:
        if check_sweep(c15['low'], c15['high'], c15['close'], c15['open'], m15["8/8"], False, atr15):
            is_active_signal, signal_direction = True, "bearish"
            tp_zone = (m30["4/8"], m30["6/8"])
            sl_level = m30["+2/8"]
            title = "РАЗВОРОТ ОТ 8/8 (ШОРТ)"
    elif curr_price <= m15["0/8"]:
        if check_sweep(c15['low'], c15['high'], c15['close'], c15['open'], m15["0/8"], True, atr15):
            is_active_signal, signal_direction = True, "bullish"
            tp_zone = (m30["2/8"], m30["4/8"])
            sl_level = m30["-2/8"]
            title = "ОТСКОК ОТ 0/8 (ЛОНГ)"

    if not is_active_signal: return None
    if CONFIG["FILTER_LOW_VOLUME"] and not is_high_vol15: return None

    if CONFIG["FILTER_BTC_TREND"] and symbol != "BTCUSDT":
        with btc_trend_lock:
            if signal_direction == "bullish" and not BTC_TREND_CACHE["is_bullish"]: return None
            if signal_direction == "bearish" and not BTC_TREND_CACHE["is_bearish"]: return None

    if CONFIG["FILTER_TAKER_RATIO"]:
        if signal_direction == "bullish" and taker_buy_ratio < CONFIG["MIN_TAKER_BUY_RATIO"]: return None
        if signal_direction == "bearish" and taker_buy_ratio > (1.0 - CONFIG["MIN_TAKER_BUY_RATIO"]): return None

    tp_min, tp_max = min(tp_zone[0], tp_zone[1]), max(tp_zone[0], tp_zone[1])
    tp_min_pct = abs(tp_min - curr_price) / curr_price * 100
    tp_max_pct = abs(tp_max - curr_price) / curr_price * 100
    sl_pct = abs(sl_level - curr_price) / curr_price * 100

    if CONFIG["FILTER_MIN_PROFIT"] and tp_min_pct < CONFIG["MIN_PROFIT_PCT"]: return None
    if CONFIG["FILTER_MAX_PROFIT"] and tp_min_pct > CONFIG["MAX_PROFIT_PCT"]: return None
    if CONFIG["FILTER_RISK_REWARD"] and sl_pct > (tp_min_pct * CONFIG["MAX_SL_TP_RATIO"]): return None

    if CONFIG["FILTER_FUNDING_RATE"]:
        with funding_rate_lock:
            fr = FUNDING_RATE_CACHE.get(symbol, 0.0)
        if signal_direction == "bullish" and fr > CONFIG["MAX_ABSOLUTE_FUNDING_RATE"]: return None
        if signal_direction == "bearish" and fr < -CONFIG["MAX_ABSOLUTE_FUNDING_RATE"]: return None

    if CONFIG["FILTER_KAMA_EFFICIENCY"] and current_er < CONFIG["MIN_EFFICIENCY_RATIO"]: return None

    date_time_str = datetime.now().strftime('%Y-%m-%d / %H:%M:%S')
    dir_str = "ЛОНГ" if signal_direction == "bullish" else "ШОРТ"

    if CONFIG["SEND_DETAILED_INFO"]:
        current_fr = FUNDING_RATE_CACHE.get(symbol, 0.0)
        risk_reward_calc = (sl_pct / tp_min_pct) if tp_min_pct > 0 else 0
        msg = (
            f"🔔 {date_time_str} 🔔\n"
            f"<b>{symbol}</b> | {dir_str}\n"
            f"📌 {title}\n"
            f"💰 <b>ВХОД:</b> {f(curr_price)}\n"
            f"🎯 <b>ТЕЙК:</b> {f(tp_min)} - {f(tp_max)} (+{tp_min_pct:.2f}%)\n"
            f"🛡️ <b>СТОП:</b> {f(sl_level)} (-{sl_pct:.2f}%)\n\n"
            f"📋 <b>ПРОШЕДШИЕ ФИЛЬТРЫ:</b>\n"
            f"• Объём: OK (х{c15['volume']/avg_vol15:.2f})\n"
            f"• Taker Buy Ratio: {taker_buy_ratio*100:.1f}%\n"
            f"• KAMA ER: {current_er:.2f} (мин: {CONFIG['MIN_EFFICIENCY_RATIO']})\n"
            f"• Фандинг: {current_fr*100:.4f}%\n"
            f"• Соотношение SL/TP: {risk_reward_calc:.2f}x"
        )
    else:
        msg = (
            f"🔔 {date_time_str} 🔔\n"
            f"<b>{symbol}</b> | {dir_str}\n"
            f"📌 {title}\n"
            f"💰 ВХОД: {f(curr_price)}\n"
            f"🎯 ТЕЙК: {f(tp_min)} - {f(tp_max)} (+{tp_min_pct:.2f}%)\n"
            f"🛡️ СТОП: {f(sl_level)} (-{sl_pct:.2f}%)\n"
            f"Прибыль: +{tp_min_pct:.2f}% | Убыток: -{sl_pct:.2f}%"
        )
    return msg

def process_pair(symbol, ui_app):
    df15 = fetch_klines(symbol, CONFIG["TIMEFRAME_MAIN"])
    df30 = fetch_klines(symbol, CONFIG["TIMEFRAME_CONF"])
    if not df15 or len(df15) < 30 or not df30 or len(df30) < 30: return

    m15, _, atr15, atr_list15 = calc_murrey_math(df15, 64)
    m30, _, atr30, _ = calc_murrey_math(df30, 64)
    
    signal_msg = analyze_and_format_signal(symbol, df15, m15, step15 := (m15['8/8'] - m15['0/8'])/8, atr15, atr_list15, df30, m30, step30 := (m30['8/8'] - m30['0/8'])/8, atr30)
    if signal_msg:
        closed_candle_time = df15[-2]['timestamp']
        with lock:
            if LAST_ALERT_TIME.get(symbol) != closed_candle_time:
                ui_app.log(f"\n{signal_msg}\n" + "-"*35)
                send_telegram_alert(symbol, signal_msg, ui_app)
                LAST_ALERT_TIME[symbol] = closed_candle_time

def scanner_main_loop(ui_app):
    ui_app.log("Инициализация ядра DRV Scanner...")
    
    while is_scanner_running:
        try:
            if not check_internet_connection():
                ui_app.status_label.text = 'СТАТУС: НЕТ ИНТЕРНЕТА'
                ui_app.info_sub_label.text = 'Ожидание восстановления сети...'
                ui_app.log("Нет подключения к интернету. Ожидание сети...")
                while not check_internet_connection():
                    if not is_scanner_running: break
                    time.sleep(3)
                if not is_scanner_running: break
                ui_app.status_label.text = 'СТАТУС: СКАНИРОВАНИЕ'
                ui_app.log("Сеть восстановлена!")

            update_market_context()
            pairs = get_active_futures_pairs()
            if not pairs:
                time.sleep(5)
                continue
                
            total_pairs = len(pairs)
            ui_app.log(f"Сканирование {total_pairs} активных пар...")
            
            from concurrent.futures import ThreadPoolExecutor
            completed = 0
            with ThreadPoolExecutor(max_workers=8) as executor:
                futures = [executor.submit(process_pair, p, ui_app) for p in pairs]
                for f in futures:
                    if not is_scanner_running: break
                    f.result()
                    completed += 1
                    ui_app.update_progress(completed, total_pairs)
            
            if not is_scanner_running: break

            # Пауза после полного скана
            pause_sec = int(CONFIG.get("PAUSE_AFTER_SCAN_SEC", 60))
            ui_app.status_label.text = 'СТАТУС: ПАУЗА ПОСЛЕ СКАНА'
            for sec in range(pause_sec, 0, -1):
                if not is_scanner_running: break
                ui_app.update_timer_display(sec, pause_sec, "Очистка экрана через...")
                time.sleep(1)

            if not is_scanner_running: break

            # Очистка экрана
            ui_app.clear_logs()
            ui_app.update_progress(0, 100)
            ui_app.log("Экран очищен. Подготовка к новому циклу...")

            # Обратный отсчет до следующего сканирования
            interval_sec = int(CONFIG.get("INTERVAL_BETWEEN_SCANS_SEC", 300))
            ui_app.status_label.text = 'СТАТУС: ОЖИДАНИЕ ЦИКЛА'
            for sec in range(interval_sec, 0, -1):
                if not is_scanner_running: break
                ui_app.update_timer_display(sec, interval_sec, "До нового сканирования рынка")
                time.sleep(1)

            ui_app.status_label.text = 'СТАТУС: СКАНИРОВАНИЕ'
            ui_app.info_sub_label.text = 'Запуск процесса...'

        except Exception as e:
            ui_app.log(f"Ошибка в цикле: {e}")
            time.sleep(5)

class DRVScannerApp(App):
    def build(self):
        self.title = "DRV"
        return ScannerAppUI()

if __name__ == '__main__':
    DRVScannerApp().run()
