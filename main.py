import sys
import time
import requests
from dataclasses import dataclass, field
from typing import List, Optional

from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.gridlayout import GridLayout
from kivy.uix.label import Label
from kivy.uix.button import Button
from kivy.uix.scrollview import ScrollView
from kivy.uix.widget import Widget
from kivy.graphics import Color, Rectangle, Line
from kivy.clock import Clock


# ==================== МОДЕЛИ ДАННЫХ (ЧИСТЫЙ PYTHON) ====================
@dataclass
class TickerData:
    symbol: str
    price: float
    volume_24h: float
    price_change_percent: float

    @classmethod
    def from_dict(cls, data: dict) -> "TickerData":
        return cls(
            symbol=data.get("symbol", "UNKNOWN"),
            price=float(data.get("lastPrice", 0.0)),
            volume_24h=float(data.get("quoteVolume", 0.0)),
            price_change_percent=float(data.get("priceChangePercent", 0.0))
        )


# ==================== МОДУЛЬ ОТРИСОВКИ ГРАФИКА (KIVY CANVAS) ====================
class MiniCandleChart(Widget):
    """
    Легковесный модуль графиков на Kivy Canvas без использования matplotlib/plotly
    """
    def init(self, **kwargs):
        super().init(**kwargs)
        self.prices: List[float] = [100.0, 102.5, 101.0, 105.0, 104.0, 108.0, 107.5, 112.0]
        self.bind(pos=self.update_chart, size=self.update_chart)

    def set_prices(self, price_list: List[float]):
        if price_list:
            self.prices = price_list
            self.update_chart()

    def update_chart(self, *args):
        self.canvas.clear()
        if not self.prices or len(self.prices) < 2:
            return

        with self.canvas:
            # Фон графика
            Color(0.1, 0.1, 0.12, 1)
            Rectangle(pos=self.pos, size=self.size)

            min_p = min(self.prices)
            max_p = max(self.prices)
            p_range = max_p - min_p if max_p != min_p else 1.0

            padding = 10
            width = self.width - (padding * 2)
            height = self.height - (padding * 2)

            points = []
            step = width / (len(self.prices) - 1)

            for idx, price in enumerate(self.prices):
                x = self.x + padding + (idx * step)
                y = self.y + padding + ((price - min_p) / p_range * height)
                points.extend([x, y])

            # Отрисовка линии тренда
            if self.prices[-1] >= self.prices[0]:
                Color(0.15, 0.8, 0.4, 1)  # Зеленая свеча/тренд
            else:
                Color(0.9, 0.25, 0.25, 1) # Красная свеча/тренд

            Line(points=points, width=2)


# ==================== ГЛАВНЫЙ ИНТЕРФЕЙС ПРИЛОЖЕНИЯ ====================
class MainScreen(BoxLayout):
    def init(self, **kwargs):
        super().init(**kwargs)
        self.orientation = "vertical"
        self.padding = 12
        self.spacing = 10

        # 1. Заголовок
        header = Label(
            text="Cyber Ultimate Master — Binance Scanner",
            font_size="20sp",
            bold=True,
            size_hint_y=None,
            height=40
        )
        self.add_widget(header)

        # 2. График цен (Canvas)
        self.chart = MiniCandleChart(size_hint_y=0.35)
        self.add_widget(self.chart)

        # 3. Панель кнопок управления
        btn_layout = BoxLayout(size_hint_y=None, height=48, spacing=8)
        
        self.btn_refresh = Button(text="🔄 Обновить Binance", background_color=(0.2, 0.6, 1.0, 1))
        self.btn_refresh.bind(on_press=self.fetch_binance_data)
        
        self.btn_clear = Button(text="🧹 Очистить", background_color=(0.8, 0.3, 0.3, 1))
        self.btn_clear.bind(on_press=self.clear_list)

        btn_layout.add_widget(self.btn_refresh)
        btn_layout.add_widget(self.btn_clear)
        self.add_widget(btn_layout)