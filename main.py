import threading
import time
import os
import requests
import pandas as pd
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
from kivy.clock import Clock

# =====================================================================
# ГЛОБАЛЬНЫЕ НАСТРОЙКИ (ПО УМОЛЧАНИЮ)
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
    "TIMEFRAME_MAIN": "15m",
    "TIMEFRAME_CONF": "30m",
    "SCAN_TIMEFRAME": "5m",
    "SIGNAL_MODE": "Простой", # "Простой" или "Расширенный"
    "ENABLE_TELEGRAM": True,
    "BOT_TOKEN": "8740928704:AAGBdKD81m8K9dqbwH4OviaizzCzT-BkVrI",
    "CHAT_ID": "1434233986"
}

# Глобальные переменные управления потоком
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


class ScannerAppUI(TabbedPanel):
    def __init__(self, **kwargs):
        super(ScannerAppUI, self).__init__(**kwargs)
        self.do_default_tab = False
        
        # --- Вкладка 1: Управление и Логи ---
        tab_control = TabbedPanelItem(text='📊 Сканер')
        control_layout = BoxLayout(orientation='vertical', padding=10, spacing=10)
        
        self.status_label = Label(text='Статус: ОСТАНОВЛЕН 🛑', size_hint_y=None, height=40, font_size=18)
        control_layout.add_widget(self.status_label)
        
        btn_layout = BoxLayout(size_hint_y=None, height=50, spacing=10)
        self.start_btn = Button(text='ЗАПУСТИТЬ', background_color=(0, 0.7, 0, 1))
        self.start_btn.bind(on_press=self.start_scanner)
        self.stop_btn = Button(text='ОСТАНОВИТЬ', background_color=(0.7, 0, 0, 1), disabled=True)
        self.stop_btn.bind(on_press=self.stop_scanner)
        btn_layout.add_widget(self.start_btn)
        btn_layout.add_widget(self.stop_btn)
        control_layout.add_widget(btn_layout)
        
        control_layout.add_widget(Label(text='Логи и сигналы в реальном времени:', size_hint_y=None, height=30))
        
        self.log_scroll = ScrollView()
        self.log_label = Label(text='Приложение готово к работе.\n', size_hint_y=None, halign='left', valign='top')
        self.log_label.bind(texture_size=lambda s, w: setattr(s, 'height', w[1]))
        self.log_label.bind(size=lambda s, w: setattr(s, 'text_size', (w[0], None)))
        self.log_scroll.add_widget(self.log_label)
        control_layout.add_widget(self.log_scroll)
        
        tab_control.add_widget(control_layout)
        self.add_widget(tab_control)

        # --- Вкладка 2: Настройки и Фильтры ---
        tab_settings = TabbedPanelItem(text='⚙️ Настройки')
        settings_scroll = ScrollView()
        settings_layout = GridLayout(cols=1, spacing=10, size_hint_y=None, padding=10)
        settings_layout.bind(minimum_height=settings_layout.setter('height'))
        
        # Режим выдачи сигналов
        settings_layout.add_widget(Label(text='Режим выдачи сигналов:', size_hint_y=None, height=30))
        self.signal_mode_spinner = Spinner(text=CONFIG["SIGNAL_MODE"], values=('Простой', 'Расширенный'), size_hint_y=None, height=40)
        settings_layout.add_widget(self.signal_mode_spinner)

        # Поля ввода числовых настроек
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
            ("MAX_ABSOLUTE_FUNDING_RATE", "Макс. фандинг (0.0005 = 0.05%):")
        ]
        
        for key, label_text in fields:
            settings_layout.add_widget(Label(text=label_text, size_hint_y=None, height=25))
            ti = TextInput(text=str(CONFIG[key]), multiline=False, size_hint_y=None, height=40)
            self.inputs[key] = ti
            settings_layout.add_widget(ti)
            
        save_btn = Button(text='СОХРАНИТЬ НАСТРОЙКИ', size_hint_y=None, height=50, background_color=(0, 0.5, 0.8, 1))
        save_btn.bind(on_press=self.save_settings)
        settings_layout.add_widget(save_btn)
        
        settings_scroll.add_widget(settings_layout)
        tab_settings.add_widget(settings_scroll)
        self.add_widget(tab_settings)

        # --- Вкладка 3: Telegram ---
        tab_tg = TabbedPanelItem(text='📱 Telegram')
        tg_layout = GridLayout(cols=1, spacing=10, padding=20, size_hint_y=None)
        tg_layout.bind(minimum_height=tg_layout.setter('height'))
        
        tg_layout.add_widget(Label(text='Отправка в Telegram:', size_hint_y=None, height=30))
        self.tg_switch = Switch(active=CONFIG["ENABLE_TELEGRAM"], size_hint_y=None, height=40)
        tg_layout.add_widget(self.tg_switch)
        
        tg_layout.add_widget(Label(text='Bot Token:', size_hint_y=None, height=25))
        self.bot_token_input = TextInput(text=CONFIG["BOT_TOKEN"], multiline=False, size_hint_y=None, height=40)
        tg_layout.add_widget(self.bot_token_input)
        
        tg_layout.add_widget(Label(text='Chat ID (Канал или Группа):', size_hint_y=None, height=25))
        self.chat_id_input = TextInput(text=CONFIG["CHAT_ID"], multiline=False, size_hint_y=None, height=40)
        tg_layout.add_widget(self.chat_id_input)
        
        save_tg_btn = Button(text='СОХРАНИТЬ TELEGRAM НАСТРОЙКИ', size_hint_y=None, height=50, background_color=(0, 0.5, 0.8, 1))
        save_tg_btn.bind(on_press=self.save_telegram_settings)
        tg_layout.add_widget(save_tg_btn)
        
        tab_tg.add_widget(tg_layout)
        self.add_widget(tab_tg)

    def log(self, message):
        timestamp = datetime.now().strftime('[%H:%M:%S] ')
        Clock.schedule_once(lambda dt: self._update_logs(timestamp + message + '\n'))

    def _update_logs(self, text):
        self.log_label.text += text

    def save_settings(self, instance):
        try:
            for key, ti in self.inputs.items():
                CONFIG[key] = float(ti.text)
            CONFIG["SIGNAL_MODE"] = self.signal_mode_spinner.text
            self.log("✅ Настройки успешно обновлены!")
        except ValueError:
            self.log("❌ Ошибка: проверьте правильность введенных чисел!")

    def save_telegram_settings(self, instance):
        CONFIG["ENABLE_TELEGRAM"] = self.tg_switch.active
        CONFIG["BOT_TOKEN"] = self.bot_token_input.text.strip()
        CONFIG["CHAT_ID"] = self.chat_id_input.text.strip()
        self.log("✅ Настройки Telegram сохранены!")

    def start_scanner(self, instance):
        global is_scanner_running, scanner_thread
        if is_scanner_running:
            return
        is_scanner_running = True
        self.status_label.text = 'Статус: РАБОТАЕТ ▶️'
        self.start_btn.disabled = True
        self.stop_btn.disabled = False
        
        scanner_thread = threading.Thread(target=scanner_main_loop, args=(self.log,), daemon=True)
        scanner_thread.start()
        self.log("🚀 Сканер запущен в фоновом режиме.")

    def stop_scanner(self, instance):
        global is_scanner_running
        is_scanner_running = False
        self.status_label.text = 'Статус: ОСТАНОВЛЕН 🛑'
        self.start_btn.disabled = False
        self.stop_btn.disabled = True
        self.log("🛑 Остановка сканера...")


# =====================================================================
# ТОРГОВЫЙ СКАНЕР И ЛОГИКА
# =====================================================================

def send_telegram_alert(symbol, msg, logger_func):
    if not CONFIG["ENABLE_TELEGRAM"]:
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
            logger_func(f"✅ Сигнал {symbol} отправлен в Telegram!")
        except Exception as e:
            logger_func(f"❌ Ошибка отправки в Telegram ({symbol}): {e}")
    threading.Thread(target=_send, daemon=True).start()


def get_active_futures_pairs(logger_func):
    url = "https://fapi.binance.com/fapi/v1/ticker/24hr"
    try:
        res = requests.get(url, timeout=10)
        res.raise_for_status()
        data = res.json()
        valid_pairs = []
        for item in data:
            if item['symbol'].endswith('USDT'):
                volume_usdt = float(item['quoteVolume'])
                if volume_usdt >= CONFIG["MIN_VOLUME_USDT"]:
                    valid_pairs.append(item['symbol'])
        return valid_pairs
    except Exception as e:
        logger_func(f"Ошибка получения списка пар: {e}")
        return []


def fetch_klines(symbol, timeframe="15m", limit=200):
    url = "https://fapi.binance.com/fapi/v1/klines"
    params = {"symbol": symbol, "interval": timeframe, "limit": limit}
    try:
        res = requests.get(url, params=params, timeout=5)
        res.raise_for_status()
        df = pd.DataFrame(res.json(), columns=[
            "timestamp", "open", "high", "low", "close", "volume",
            "close_time", "quote_av", "trades", "tb_base_av", "tb_quote_av", "ignore"
        ])
        df["timestamp"] = pd.to_datetime(df["timestamp"], unit="ms")
        for col in ["open", "high", "low", "close", "volume", "tb_base_av", "tb_quote_av"]:
            df[col] = df[col].astype(float)
        return df
    except Exception:
        return None


def update_market_context(logger_func):
    df_btc = fetch_klines("BTCUSDT", CONFIG["TIMEFRAME_CONF"], limit=50)
    if df_btc is not None and not df_btc.empty:
        ema_btc = df_btc['close'].ewm(span=64, adjust=False).mean().iloc[-2]
        curr_btc = df_btc['close'].iloc[-2]
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


def calc_murrey_math(df, base_lookback=64):
    high_low = df['high'] - df['low']
    high_close = (df['high'] - df['close'].shift(1)).abs()
    low_close = (df['low'] - df['close'].shift(1)).abs()
    tr = pd.concat([high_low, high_close, low_close], axis=1).max(axis=1)
    df['atr'] = tr.rolling(window=14).mean()
    
    atr = df['atr'].iloc[-1]
    if pd.isna(atr) or atr == 0:
        atr = (df['high'].iloc[-1] - df['low'].iloc[-1])
    
    volatility_ratio = tr.iloc[-1] / (atr + 1e-8)
    adaptive_lookback = int(base_lookback * 0.75) if volatility_ratio > 1.5 else base_lookback
    
    smoothed_high = (df['high'] + df[['open', 'close']].max(axis=1)) / 2
    smoothed_low = (df['low'] + df[['open', 'close']].min(axis=1)) / 2
    
    hh = smoothed_high.rolling(window=adaptive_lookback).max().iloc[-1]
    ll = smoothed_low.rolling(window=adaptive_lookback).min().iloc[-1]
    
    range_val = hh - ll
    step = range_val / 8 if range_val > 0 else 1.0
    
    m_vals = {
        "+2/8": hh + 2 * step, "+1/8": hh + step, "8/8": hh, "7/8": hh - step,
        "6/8": hh - 2 * step, "5/8": hh - 3 * step, "4/8": hh - 4 * step,
        "3/8": hh - 5 * step, "2/8": hh - 6 * step, "1/8": hh - 7 * step,
        "0/8": ll, "-1/8": ll - step, "-2/8": ll - 2 * step
    }
    return m_vals, step, atr


def check_sweep(c_low, c_high, c_close, c_open, level, is_bullish, atr):
    if not CONFIG["FILTER_LIQUIDITY_SWEEP"]:
        return (c_close > level and c_open <= level) if is_bullish else (c_close < level and c_open >= level)
            
    if pd.isna(atr) or atr == 0:
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


def analyze_and_format_signal(symbol, df15, m15, step15, atr15, df30, m30, step30, atr30):
    fmt = get_price_fmt(df15['close'].iloc[-1])
    f = lambda v: fmt.format(v)
    curr_price = df15['close'].iloc[-1]
    c15 = df15.iloc[-2]
    c30 = df30.iloc[-2]
    
    avg_vol15 = df15['volume'].rolling(20).mean().iloc[-2]
    is_high_vol15 = c15['volume'] > (avg_vol15 * CONFIG["VOLUME_SPIKE_MULTIPLIER"])
    
    if CONFIG["FILTER_ATR_SPIKE"]:
        atr_rolling_mean = df15['atr'].rolling(20).mean().iloc[-2]
        curr_candle_atr = df15['atr'].iloc[-2]
        if not pd.isna(atr_rolling_mean) and atr_rolling_mean > 0:
            if (curr_candle_atr / atr_rolling_mean) > CONFIG["MAX_ATR_MULTIPLIER"]:
                return None
                
    taker_buy_vol = c15['tb_base_av']
    total_vol = c15['volume']
    taker_buy_ratio = (taker_buy_vol / total_vol) if total_vol > 0 else 0.5
    ema30_val = df30['close'].ewm(span=64, adjust=False).mean().iloc[-2]
    
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

    if not is_active_signal:
        return None

    if CONFIG["FILTER_LOW_VOLUME"] and not is_high_vol15:
        return None

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
    dir_str = "📈 ЛОНГ" if signal_direction == "bullish" else "📉 ШОРТ"

    # Проверка режима выдачи сигналов
    if CONFIG["SIGNAL_MODE"] == "Расширенный":
        msg = (
            f"🔔{date_time_str}🔔\n"
            f"{symbol}****{dir_str}\n"
            f"📌 Сигнал: {title}\n"
            f"💰 ВХОД: {f(curr_price)}\n"
            f"🎯 Зона Тейк-Профит: {f(tp_min)} - {f(tp_max)}\n"
            f"🛡️ Стоп-Лосс: {f(sl_level)}\n"
            f"Прибыль: +{tp_min_pct:.2f}% — +{tp_max_pct:.2f}%\n"
            f"Убыток: -{sl_pct:.2f}%\n"
            f"📊 Объем: OK | Taker Buy: {taker_buy_ratio*100:.1f}%"
        )
    else:
        # Простой формат
        msg = (
            f"🔔{date_time_str}🔔\n"
            f"{symbol}****{dir_str}\n"
            f"💰 ВХОД: {f(curr_price)}\n"
            f"🎯 Зона Тейк-Профит:\n"
            f"{f(tp_min)} - {f(tp_max)}\n"
            f"🛡️ Стоп-Лосс: {f(sl_level)}\n"
            f"Прибыль (1x): +{tp_min_pct:.2f}% — +{tp_max_pct:.2f}%\n"
            f"Убыток (1x): -{sl_pct:.2f}%"
        )
    return msg


def process_pair(symbol, logger_func):
    df15 = fetch_klines(symbol, CONFIG["TIMEFRAME_MAIN"])
    df30 = fetch_klines(symbol, CONFIG["TIMEFRAME_CONF"])
    if df15 is None or df15.empty or df30 is None or df30.empty: return

    df30['EMA'] = df30['close'].ewm(span=64, adjust=False).mean()
    m15, step15, atr15 = calc_murrey_math(df15, 64)
    m30, step30, atr30 = calc_murrey_math(df30, 64)
    
    signal_msg = analyze_and_format_signal(symbol, df15, m15, step15, atr15, df30, m30, step30, atr30)
    if signal_msg:
        closed_candle_time = df15['timestamp'].iloc[-2]
        with lock:
            if LAST_ALERT_TIME.get(symbol) != closed_candle_time:
                logger_func(f"\n{signal_msg}\n" + "-"*30)
                send_telegram_alert(symbol, signal_msg, logger_func)
                LAST_ALERT_TIME[symbol] = closed_candle_time


def scanner_main_loop(logger_func):
    logger_func("🔄 Цикл сканирования запущен...")
    while is_scanner_running:
        try:
            update_market_context(logger_func)
            pairs = get_active_futures_pairs(logger_func)
            if not pairs:
                logger_func("⚠️ Нет пар, повтор через 10 сек...")
                time.sleep(10)
                continue
                
            logger_func(f"🔎 Сканирование {len(pairs)} пар...")
            from concurrent.futures import ThreadPoolExecutor
            with ThreadPoolExecutor(max_workers=8) as executor:
                for p in pairs:
                    if not is_scanner_running: break
                    executor.submit(process_pair, p, logger_func)
            
            # Ожидание перед следующим сканом (5 минут таймфрейм)
            for _ in range(300):
                if not is_scanner_running: break
                time.sleep(1)
        except Exception as e:
            logger_func(f"❌ Ошибка в цикле: {e}")
            time.sleep(10)


class BinanceScannerApp(App):
    def build(self):
        self.title = "Binance Futures Scanner"
        return ScannerAppUI()


if __name__ == '__main__':
    BinanceScannerApp().run()