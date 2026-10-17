import math
from kivy.app import App
from kivy.clock import Clock
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.label import Label
from kivy.uix.button import Button
from kivy.graphics import Color, Rectangle
from kivy.utils import get_color_from_hex

# Проверка платформы для подключения датчиков через plyer
try:
    from plyer import compass
    PLATFORM_ANDROID = True
except ImportError:
    PLATFORM_ANDROID = False

# Запрос разрешений на Android для доступа к датчикам/геолокации
def request_android_permissions():
    if PLATFORM_ANDROID:
        try:
            from android.permissions import request_permissions, Permission
            request_permissions([
                Permission.ACCESS_FINE_LOCATION,
                Permission.ACCESS_COARSE_LOCATION
            ])
        except Exception as e:
            print(f"Ошибка запроса разрешений: {e}")

class CyberCompassWidget(BoxLayout):
    def init(self, **kwargs):
        super().init(**kwargs)
        self.orientation = 'vertical'
        self.padding = 30
        self.spacing = 20

        # Темный киберпанк-фон
        with self.canvas.before:
            Color(*get_color_from_hex('#0b0e14'))
            self.bg = Rectangle(pos=self.pos, size=self.size)
        self.bind(pos=self.update_bg, size=self.update_bg)

        # Заголовок
        self.title_label = Label(
            text="[b]CYBER COMPASS[/b]",
            markup=True,
            font_size=28,
            size_hint=(1, 0.15),
            color=get_color_from_hex('#00ffcc') # Неоновый циан
        )
        self.add_widget(self.title_label)

        # Инфо-лейбл для градусов
        self.info_label = Label(
            text="Нажмите кнопку для старта",
            font_size=22,
            size_hint=(1, 0.15),
            color=get_color_from_hex('#ffffff')
        )
        self.add_widget(self.info_label)

        # Главный индикатор направления
        self.compass_display = Label(
            text="—",
            font_size=65,
            bold=True,
            size_hint=(1, 0.5),
            color=get_color_from_hex('#ff0055') # Неоновый розовый
        )
        self.add_widget(self.compass_display)

        # Кнопка управления датчиком
        self.toggle_btn = Button(
            text="АКТИВИРОВАТЬ ДАТЧИК",
            size_hint=(1, 0.2),
            background_color=get_color_from_hex('#00ffcc'),
            color=(0, 0, 0, 1)
        )
        self.toggle_btn.bind(on_press=self.toggle_compass)
        self.add_widget(self.toggle_btn)

        self.is_active = False
        self.azimuth = 0.0

    def update_bg(self, *args):
        self.bg.pos = self.pos
        self.bg.size = self.size

    def toggle_compass(self, instance):
        if not self.is_active:
            # Запрашиваем разрешения при активации
            request_android_permissions()
            
            if PLATFORM_ANDROID:
                try:
                    compass.enable()
                    self.is_active = True
                    self.toggle_btn.text = "ОСТАНОВИТЬ КОМПАС"
                    Clock.schedule_interval(self.update_sensor, 0.2)
                except Exception as e:
                    self.info_label.text = "Ошибка датчика компаса!"
            else:
                # Режим симуляции для отладки на ПК
                self.is_active = True
                self.toggle_btn.text = "ОСТАНОВИТЬ (Симуляция ПК)"
                self.sim_angle = 0
                Clock.schedule_interval(self.simulate_sensor, 0.1)
        else:
            if PLATFORM_ANDROID:
                try:
                    compass.disable()
                except:
                    pass
            Clock.unschedule(self.update_sensor)
            Clock.unschedule(self.simulate_sensor)
            self.is_active = False
            self.toggle_btn.text = "АКТИВИРОВАТЬ ДАТЧИК"
            self.info_label.text = "Компас отключен"