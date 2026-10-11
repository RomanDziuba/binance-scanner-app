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
from kivy.uix.tabbedpanel import TabbedPanel, TabbedPanelItem
from kivy.uix.progressbar import ProgressBar
from kivy.clock import Clock
from kivy.graphics import Color, Rectangle, RoundedRectangle
from kivy.core.audio import SoundLoader

# =====================================================================
# ГЛОБАЛЬНЫЕ НАСТРОЙКИ ПО УМОЛЧАНИЮ
# =====================================================================
CONFIG = {
    "MIN_VOLUME_USDT": 5_000_000,
    "FILTER_LOW_VOLUME": True,
    "VOLUME_SPIKE_MULTIPLIER": 1.5,
    "FILTER_RISK_REWARD": True,
    "MAX_SL_TP_DIFF_PCT": 1.4,
    "FILTER_MIN_PROFIT": True,
    "MIN_PROFIT_PCT": 0.9,
    "FILTER_BTC_TREND": True,
    "FILTER_LIQUIDITY_SWEEP": True,
    "SWEEP_MIN_ATR_RATIO": 0.1,
    "SWEEP_MAX_ATR_RATIO": 2.0,
    "FILTER_TAKER_RATIO": True,
    "MIN_TAKER_BUY_RATIO": 0.6,
    "FILTER_ATR_SPIKE": True,
    "MAX_ATR_MULTIPLIER": 2.5,
    "FILTER_FUNDING_RATE": True,
    "MAX_ABSOLUTE_FUNDING_RATE": 0.0005,
    "PAUSE_BETWEEN_SCANS_SEC": 60,  # Пауза между сканами в секундах
    "TIMEFRAME_MAIN": "15m",
    "TIMEFRAME_CONF": "30m",
    "SCAN_TIMEFRAME": "5m",
    "SIGNAL_MODE": "Простой", 
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


def play_alert_sound():
    """Воспроизведение звука сигнала"""
    try:
        from kivy.core.audio import SoundLoader
        # Стандартный бип системного динамика / генерация тона
        import sys
        if hasattr(sys, 'stdout'):
            print('\a')
    except Exception:
        pass


def get_price_fmt(price):
    if price == 0: return "{:.2f}"
    if price >= 100: return "{:.2f}"
    elif price >= 1: return "{:.4f}"
    elif price >= 0.001: return "{:.6f}"
    else: return "{:.8f}"


# =====================================================================
# МАТЕМАТИЧЕСКИЕ РАСЧЕТЫ
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
# СТИЛИЗОВАННЫЕ КОМПОНЕНТЫ ИНТЕРФЕЙСА (DARK PRO THEME)
# =====================================================================
class StyledButton(Button):
    def __init__(self, bg_color=(0.1, 0.6, 0.4, 1), **kwargs):
        super().__init__(**kwargs)
        self.background_normal = ''
        self.background_color = (0, 0, 0, 0)
        self.custom_color = bg_color
        self.font_size = '15sp'
        self.bold = True
        self.color = (1, 1, 1, 1)
        
        with self.canvas.before:
            Color(*self.custom_color)
            self.rect = RoundedRectangle(pos=self.pos, size=self.size, radius=[10])
        self.bind(pos=self.update_canvas, size=self.update_canvas)

    def update_canvas(self, *args):
        self.rect.pos = self.pos
        self.rect.size = self.size


class StyledTextInput(TextInput):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.background_normal = ''
        self.background_active = ''
        self.background_color = (0.12, 0.16, 0.22, 1)
        self.foreground_color = (0, 0.9, 1, 1)
        self.cursor_color = (0, 0.9, 1, 1)
        self.font_size = '14sp'
        self.padding = [10, 10, 10, 10]


class ScannerAppUI(TabbedPanel):
    def __init__(self, **kwargs):
        super(ScannerAppUI, self).__init__(**kwargs)
        self.do_default_tab = False
        self.background_color = (0.05, 0.07, 0.09, 1)
        self.tab_width = 110

        # --- Вкладка 1: Сканер ---
        tab_control = TabbedPanelItem(text='Сканер')
        tab_control.background_color = (0.1, 0.14, 0.18, 1)
        
        control_layout = BoxLayout(orientation='vertical', padding=15, spacing=12)
        
        # Плашка статуса
        self.status_box = BoxLayout(size_hint_y=None, height=45, padding=5)
        with self.status_box.canvas.before:
            Color(0.1, 0.13, 0.18, 1)
            self.sb_rect = RoundedRectangle(pos=self.status_box.pos, size=self.status_box.size, radius=[8])
        self.status_box.bind(pos=self._update_sb, size=self._update_sb)

        self.status_label = Label(
            text='СТАТУС: ОСТАНОВЛЕН', 
            bold=True, 
            color=(1, 0.3, 0.3, 1),
            font_size='16sp'
        )
        self.status_box.add_widget(self.status_label)
        control_layout.add_widget(self.status_box)

        # Кнопки управления
        btn_layout = BoxLayout(size_hint_y=None, height=52, spacing=12)
        self.start_btn = StyledButton(text='ЗАПУСТИТЬ', bg_color=(0, 0.8, 0.4, 1))
        self.start_btn.bind(on_press=self.start_scanner)
        
        self.stop_btn = StyledButton(text='ОСТАНОВИТЬ', bg_color=(0.9, 0.2, 0.2, 1))
        self.stop_btn.disabled = True
        self.stop_btn.bind(on_press=self.stop_scanner)
        
        btn_layout.add_widget(self.start_btn)
        btn_layout.add_widget(self.stop_btn)
        control_layout.add_widget(btn_layout)

        # Прогресс Бар
        progress_box = BoxLayout(orientation='vertical', size_hint_y=None, height=45, spacing=4)
        self.progress_label = Label(text='Прогресс сканирования: 0%', font_size='12sp', color=(0.7, 0.8, 0.9, 1))
        self.progress_bar = ProgressBar(max=100, value=0, size_hint_y=None, height=18)
        progress_box.add_widget(self.progress_label)
        progress_box.add_widget(self.progress_bar)
        control_layout.add_widget(progress_box)

        # Панель логов
        self.log_scroll = ScrollView()
        self.log_label = Label(
            text='Готов к работе.\n', 
            size_hint_y=None, 
            halign='left', 
            valign='top',
            color=(0, 0.95, 0.8, 1),
            font_size='13sp',
            markup=True
        )
        self.log_label.bind(texture_size=lambda s, w: setattr(s, 'height', w[1]))
        self.log_label.bind(size=lambda s, w: setattr(s, 'text_size', (w[0], None)))
        self.log_scroll.add_widget(self.log_label)
        control_layout.add_widget(self.log_scroll)
        
        tab_control.add_widget(control_layout)
        self.add_widget(tab_control)

        # --- Вкладка 2: Настройки ---
        tab_settings = TabbedPanelItem(text='Настройки')
        tab_settings.background_color = (0.1, 0.14, 0.18, 1)
        
        settings_scroll = ScrollView()
        settings_layout = GridLayout(cols=1, spacing=10, size_hint_y=None, padding=15)
        settings_layout.bind(minimum_height=settings_layout.setter('height'))
        
        settings_layout.add_widget(Label(text='Режим выдачи сигналов:', size_hint_y=None, height=25, color=(0.8, 0.9, 1, 1)))
        self.signal_mode_spinner = Spinner(
            text=CONFIG["SIGNAL_MODE"], 
            values=('Простой', 'Расширенный'), 
            size_hint_y=None, 
            height=42,
            background_color=(0.12, 0.18, 0.26, 1),
            color=(0, 0.9, 1, 1)
        )
        settings_layout.add_widget(self.signal_mode_spinner)

        self.inputs = {}
        fields = [
            ("MIN_VOLUME_USDT", "Мин. объем пары (USDT):"),
            ("VOLUME_SPIKE_MULTIPLIER", "Множитель объема свечи:"),
            ("MAX_SL_TP_DIFF_PCT", "Макс. разница SL/TP (%):"),
            ("MIN_PROFIT_PCT", "Мин. потенциал профита (%):"),
            ("SWEEP_MIN_ATR_RATIO", "Мин. прокол ATR (Sweep):"),
            ("SWEEP_MAX_ATR_RATIO", "Макс. прокол ATR (Sweep):"),
            ("MIN_TAKER_BUY_RATIO", "Мин. доля Taker Buy (0.6 = 60%):"),
            ("MAX_ATR_MULTIPLIER", "Макс. множитель ATR Spike:"),
            ("MAX_ABSOLUTE_FUNDING_RATE", "Макс. фандинг (0.0005 = 0.05%):"),
            ("PAUSE_BETWEEN_SCANS_SEC", "Пауза между сканами (сек):")
        ]
        
        for key, label_text in fields:
            settings_layout.add_widget(Label(text=label_text, size_hint_y=None, height=22, color=(0.7, 0.8, 0.9, 1), halign='left'))
            ti = StyledTextInput(text=str(CONFIG[key]), multiline=False, size_hint_y=None, height=40)
            self.inputs[key] = ti
            settings_layout.add_widget(ti)
            
        save_btn = StyledButton(text='СОХРАНИТЬ НАСТРОЙКИ', bg_color=(0, 0.55, 0.9, 1), size_hint_y=None, height=48)
        save_btn.bind(on_press=self.save_settings)
        settings_layout.add_widget(save_btn)
        
        settings_scroll.add_widget(settings_layout)
        tab_settings.add_widget(settings_scroll)
        self.add_widget(tab_settings)

        # --- Вкладка 3: Telegram ---
        tab_tg = TabbedPanelItem(text='Telegram')
        tab_tg.background_color = (0.1, 0.14, 0.18, 1)
        
        tg_layout = GridLayout(cols=1, spacing=12, padding=15, size_hint_y=None)
        tg_layout.bind(minimum_height=tg_layout.setter('height'))
        
        tg_layout.add_widget(Label(text='Отправка в Telegram:', size_hint_y=None, height=25, color=(0.8, 0.9, 1, 1)))
        self.tg_switch = Switch(active=CONFIG["ENABLE_TELEGRAM"], size_hint_y=None, height=38)
        tg_layout.add_widget(self.tg_switch)
        
        tg_layout.add_widget(Label(text='Bot Token:', size_hint_y=None, height=22, color=(0.7, 0.8, 0.9, 1)))
        self.bot_token_input = StyledTextInput(text=CONFIG["BOT_TOKEN"], multiline=False, size_hint_y=None, height=40)
        tg_layout.add_widget(self.bot_token_input)
        
        tg_layout.add_widget(Label(text='Chat ID (Канал или Группа):', size_hint_y=None, height=22, color=(0.7, 0.8, 0.9, 1)))
        self.chat_id_input = StyledTextInput(text=CONFIG["CHAT_ID"], multiline=False, size_hint_y=None, height=40)
        tg_layout.add_widget(self.chat_id_input)
        
        save_tg_btn = StyledButton(text='СОХРАНИТЬ TELEGRAM НАСТРОЙКИ', bg_color=(0, 0.55, 0.9, 1), size_hint_y=None, height=48)
        save_tg_btn.bind(on_press=self.save_telegram_settings)
        tg_layout.add_widget(save_tg_btn)
        
        tab_tg.add_widget(tg_layout)
        self.add_widget(tab_tg)

    def _update_sb(self, *args):
        self.sb_rect.pos = self.status_box.pos
        self.sb_rect.size = self.status_box.size

    def log(self, message):
        timestamp = datetime.now().strftime('[%H:%M:%S] ')
        Clock.schedule_once(lambda dt: self._update_logs(timestamp + message + '\n'))

    def clear_logs(self):
        Clock.schedule_once(lambda dt: setattr(self.log_label, 'text', ''))

    def set_progress(self, current, total):
        pct = int((current / total) * 100) if total > 0 else 0
        Clock.schedule_once(lambda dt: self._update_progress(current, total, pct))

    def _update_progress(self, current, total, pct):
        self.progress_bar.value = pct
        self.progress_label.text = f'Прогресс: {pct}% ({current}/{total} пар)'

    def _update_logs(self, text):
        self.log_label.text += text

    def save_settings(self, instance):
        try:
            for key, ti in self.inputs.items():
                CONFIG[key] = float(ti.text)
            CONFIG["SIGNAL_MODE"] = self.signal_mode_spinner.text
            self.log("[color=00FF88]Настройки сохранены![/color]")
        except ValueError:
            self.log("[color=FF4444]Ошибка: проверьте ввод чисел![/color]")

    def save_telegram_settings(self, instance):
        CONFIG["ENABLE_TELEGRAM"] = self.tg_switch.active
        CONFIG["BOT_TOKEN"] = self.bot_token_input.text.strip()
        CONFIG["CHAT_ID"] = self.chat_id_input.text.strip()
        self.log("[color=00FF88]Настройки Telegram сохранены![/color]")

    def start_scanner(self, instance):
        global is_scanner_running, scanner_thread
        if is_scanner_running: return
        is_scanner_running = True
        self.status_label.text = 'СТАТУС: РАБОТАЕТ'
        self.status_label.color = (0, 1, 0.5, 1)
        self.start_btn.disabled = True
        self.stop_btn.disabled = False
        
        scanner_thread = threading.Thread(target=scanner_main_loop, args=(self.log, self.clear_logs, self.set_progress), daemon=True)
        scanner_thread.start()

    def stop_scanner(self, instance):
        global is_scanner_running
        is_scanner_running = False
        self.status_label.text = 'СТАТУС: ОСТАНОВЛЕН'
        self.status_label.color = (1, 0.3, 0.3, 1)
        self.start_btn.disabled = False
        self.stop_btn.disabled = True
        self.log("[color=FF8800]Остановка сканера...[/color]")


# =====================================================================
# РАБОЧИЙ ЦИКЛ СКАНЕРА
# =====================================================================

def check_internet():
    try:
        requests.get("https://fapi.binance.com/fapi/v1/ping", timeout=3)
        return True
    except Exception:
        return False


def send_telegram_alert(symbol, msg, logger_func):
    if not CONFIG["ENABLE_TELEGRAM"]:
        # Если Telegram выключен - издаем звук и показываем в логах
        play_alert_sound()
        return

    def _send():
        url = f"https://api.telegram.org/bot{CONFIG['BOT_TOKEN']}/sendMessage"
        payload = {
            "chat_id": CONFIG["CHAT_ID"],
            "text": msg,
            "parse_mode": "HTML",
            "disable_web_page_preview": True
        }
        try:
            res = requests.post(url, json=payload, timeout=5)
            res.raise_for_status()
        except Exception as e:
            logger_func(f"[color=FF4444]Ошибка Telegram ({symbol}): {e}[/color]")
    threading.Thread(target=_send, daemon=True).start()


def get_active_futures_pairs():
    url = "https://fapi.binance.com/fapi/v1/ticker/24hr"
    res = requests.get(url, timeout=8)
    res.raise_for_status()
    data = res.json()
    valid_pairs = []
    for item in data:
        if item['symbol'].endswith('USDT'):
            volume_usdt = float(item['quoteVolume'])
            if volume_usdt >= CONFIG["MIN_VOLUME_USDT"]:
                valid_pairs.append(item['symbol'])
    return valid_pairs


def fetch_klines(symbol, timeframe="15m", limit=200):
    url = "https://fapi.binance.com/fapi/v1/klines"
    params = {"symbol": symbol, "interval": timeframe, "limit": limit}
    try:
        res = requests.get(url, params=params, timeout=5)
        res.raise_for_status()
        raw = res.json()
        candles = []
        for row in raw:
            dt = datetime.fromtimestamp(int(row[0]) / 1000.0)
            candles.append({
                "timestamp": dt,
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
    candles_btc = fetch_klines("BTCUSDT", CONFIG["TIMEFRAME_CONF"], limit=100)
    if candles_btc and len(candles_btc) > 64:
        closes = [c["close"] for c in candles_btc]
        emas = calculate_ema(closes, 64)
        ema_btc = emas[-2]
        curr_btc = closes[-2]
        with btc_trend_lock:
            BTC_TREND_CACHE["is_bullish"] = curr_btc > ema_btc
            BTC_TREND_CACHE["is_bearish"] = curr_btc < ema_btc

    if CONFIG["FILTER_FUNDING_RATE"]:
        url = "https://fapi.binance.com/fapi/v1/premiumIndex"
        try:
            res = requests.get(url, timeout=5)
            res.raise_for_status()
            data = res.json()
            new_rates = {item['symbol']: float(item.get('lastFundingRate', 0.0)) for item in data}
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
    if atr == 0 or atr is None:
        atr = highs[-1] - lows[-1]

    volatility_ratio = tr_list[-1] / (atr + 1e-8) if (atr + 1e-8) > 0 else 1.0
    adaptive_lookback = int(base_lookback * 0.75) if volatility_ratio > 1.5 else base_lookback

    smoothed_high = [(highs[i] + max(opens[i], closes[i])) / 2 for i in range(len(candles))]
    smoothed_low = [(lows[i] + min(opens[i], closes[i])) / 2 for i in range(len(candles))]

    hh_list = calculate_rolling_max(smoothed_high, adaptive_lookback)
    ll_list = calculate_rolling_min(smoothed_low, adaptive_lookback)

    hh = hh_list[-1]
    ll = ll_list[-1]

    range_val = hh - ll
    step = range_val / 8 if range_val > 0 else 1.0

    m_vals = {
        "+2/8": hh + 2 * step, "+1/8": hh + step, "8/8": hh, "7/8": hh - step,
        "6/8": hh - 2 * step, "5/8": hh - 3 * step, "4/8": hh - 4 * step,
        "3/8": hh - 5 * step, "2/8": hh - 6 * step, "1/8": hh - 7 * step,
        "0/8": ll, "-1/8": ll - step, "-2/8": ll - 2 * step
    }
    return m_vals, step, atr, atr_list, tr_list


def check_sweep(c_low, c_high, c_close, c_open, level, is_bullish, atr):
    if not CONFIG["FILTER_LIQUIDITY_SWEEP"]:
        return (c_close > level and c_open <= level) if is_bullish else (c_close < level and c_open >= level)
        
    if atr is None or atr == 0:
        atr = (c_high - c_low) if (c_high - c_low) > 0 else level * 0.001

    if is_bullish:
        if c_low < level and c_close > level:
            penetration_ratio = (level - c_low) / atr
            if CONFIG["SWEEP_MIN_ATR_RATIO"] <= penetration_ratio <= CONFIG["SWEEP_MAX_ATR_RATIO"]:
                return True
        if c_close > level and c_open <= level:
            return True
    else:
        if c_high > level and c_close < level:
            penetration_ratio = (c_high - level) / atr
            if CONFIG["SWEEP_MIN_ATR_RATIO"] <= penetration_ratio <= CONFIG["SWEEP_MAX_ATR_RATIO"]:
                return True
        if c_close < level and c_open >= level:
            return True
    return False


def analyze_and_format_signal(symbol, df15, m15, step15, atr15, atr_list15, tr_list15, df30, m30, step30, atr30, atr_list30):
    curr_price = df15[-1]['close']
    fmt = get_price_fmt(curr_price)
    f = lambda v: fmt.format(v)
    
    c15 = df15[-2]
    c30 = df30[-2]
    
    volumes15 = [c['volume'] for c in df15]
    vol_mean15 = calculate_rolling_mean(volumes15, 20)
    avg_vol15 = vol_mean15[-2] if len(vol_mean15) >= 2 else volumes15[-2]
    is_high_vol15 = c15['volume'] > (avg_vol15 * CONFIG["VOLUME_SPIKE_MULTIPLIER"])
    
    if CONFIG["FILTER_ATR_SPIKE"]:
        atr_rolling_mean_list = calculate_rolling_mean(atr_list15, 20)
        if len(atr_rolling_mean_list) >= 2:
            atr_rolling_mean = atr_rolling_mean_list[-2]
            curr_candle_atr = atr_list15[-2]
            if atr_rolling_mean > 0:
                if (curr_candle_atr / atr_rolling_mean) > CONFIG["MAX_ATR_MULTIPLIER"]:
                    return None
                    
    taker_buy_vol = c15['tb_base_av']
    total_vol = c15['volume']
    taker_buy_ratio = (taker_buy_vol / total_vol) if total_vol > 0 else 0.5
    
    closes30 = [c['close'] for c in df30]
    ema30_list = calculate_ema(closes30, 64)
    ema30_val = ema30_list[-2] if len(ema30_list) >= 2 else closes30[-2]
    
    tp_zone = None
    sl_level = None
    is_active_signal = False
    signal_direction = None
    title = ""

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
    if CONFIG["FILTER_RISK_REWARD"] and sl_pct > (tp_min_pct + CONFIG["MAX_SL_TP_DIFF_PCT"]): return None

    if CONFIG["FILTER_FUNDING_RATE"]:
        with funding_rate_lock:
            fr = FUNDING_RATE_CACHE.get(symbol, 0.0)
        if signal_direction == "bullish" and fr > CONFIG["MAX_ABSOLUTE_FUNDING_RATE"]: return None
        if signal_direction == "bearish" and fr < -CONFIG["MAX_ABSOLUTE_FUNDING_RATE"]: return None

    date_time_str = datetime.now().strftime('%Y-%m-%d / %H:%M:%S')
    dir_str = "[color=00FF88]📈 ЛОНГ[/color]" if signal_direction == "bullish" else "[color=FF4444]📉 ШОРТ[/color]"

    if CONFIG["SIGNAL_MODE"] == "Расширенный":
        msg = (
            f"⚡ [b]{symbol}[/b] | {dir_str}\n"
            f"⏰ {date_time_str}\n"
            f"📌 Сигнал: {title}\n"
            f"💰 Вход: [b]{f(curr_price)}[/b]\n"
            f"🎯 Тейк-Профит: {f(tp_min)} — {f(tp_max)}\n"
            f"🛡️ Стоп-Лосс: {f(sl_level)}\n"
            f"📊 Профит: +{tp_min_pct:.2f}%..+{tp_max_pct:.2f}% | Риск: -{sl_pct:.2f}%\n"
            f"🔥 Taker Buy: {taker_buy_ratio*100:.1f}%\n"
        )
    else:
        msg = (
            f"⚡ [b]{symbol}[/b] | {dir_str}\n"
            f"💰 Вход: [b]{f(curr_price)}[/b]\n"
            f"🎯 Тейк: {f(tp_min)} — {f(tp_max)}\n"
            f"🛡️ Стоп: {f(sl_level)}\n"
            f"📈 Профит: +{tp_min_pct:.2f}%..+{tp_max_pct:.2f}% | Убыток: -{sl_pct:.2f}%\n"
        )
    return msg


def process_pair(symbol, logger_func):
    df15 = fetch_klines(symbol, CONFIG["TIMEFRAME_MAIN"])
    df30 = fetch_klines(symbol, CONFIG["TIMEFRAME_CONF"])
    if not df15 or len(df15) < 30 or not df30 or len(df30) < 30: 
        return

    m15, step15, atr15, atr_list15, tr_list15 = calc_murrey_math(df15, 64)
    m30, step30, atr30, atr_list30, tr_list30 = calc_murrey_math(df30, 64)
    
    signal_msg = analyze_and_format_signal(
        symbol, df15, m15, step15, atr15, atr_list15, tr_list15, 
        df30, m30, step30, atr30, atr_list30
    )
    if signal_msg:
        closed_candle_time = df15[-2]['timestamp']
        with lock:
            if LAST_ALERT_TIME.get(symbol) != closed_candle_time:
                logger_func(f"\n{signal_msg}\n" + "—"*28)
                send_telegram_alert(symbol, signal_msg, logger_func)
                LAST_ALERT_TIME[symbol] = closed_candle_time


def scanner_main_loop(logger_func, clear_logs_func, progress_func):
    internet_warning_shown = False

    while is_scanner_running:
        # 1. Очистка экрана перед каждым запуском
        clear_logs_func()
        progress_func(0, 100)

        # 2. Проверка интернета без спама
        while is_scanner_running and not check_internet():
            if not internet_warning_shown:
                logger_func("[color=FF8800]⚠️ Нет подключения к интернету. Ожидание сети...[/color]")
                internet_warning_shown = True
            time.sleep(5)

        if not is_scanner_running: break
        
        if internet_warning_shown:
            logger_func("[color=00FF88]✅ Соединение восстановлено![/color]")
            internet_warning_shown = False

        try:
            logger_func("[color=00E5FF]🔍 Анализ рынка Binance Futures...[/color]")
            update_market_context()
            pairs = get_active_futures_pairs()
            
            if not pairs:
                logger_func("[color=FF8800]⚠️ Нет пар для анализа.[/color]")
                time.sleep(10)
                continue
                
            total_pairs = len(pairs)
            
            from concurrent.futures import ThreadPoolExecutor
            completed_count = 0
            
            with ThreadPoolExecutor(max_workers=8) as executor:
                futures = [executor.submit(process_pair, p, logger_func) for p in pairs]
                for f in futures:
                    if not is_scanner_running: break
                    f.result()
                    completed_count += 1
                    progress_func(completed_count, total_pairs)

            progress_func(total_pairs, total_pairs)
            logger_func("\n[color=00FF88]✅ Сканирование завершено.[/color]")
            
            # Пауза перед следующей очисткой и сканированием
            pause_sec = int(CONFIG.get("PAUSE_BETWEEN_SCANS_SEC", 60))
            logger_func(f"[color=778899]Пауза {pause_sec} сек. перед следующим обновлением...[/color]")
            
            for _ in range(pause_sec):
                if not is_scanner_running: break
                time.sleep(1)

        except Exception as e:
            logger_func(f"[color=FF4444]Ошибка в цикле: {e}[/color]")
            time.sleep(10)


class BinanceScannerApp(App):
    def build(self):
        self.title = "DRV"
        return ScannerAppUI()


if __name__ == '__main__':
    BinanceScannerApp().run()
