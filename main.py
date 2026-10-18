from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.label import Label
from kivy.uix.button import Button
from kivy.clock import Clock
from kivy.utils import platform

# Безопасный импорт датчика компаса из plyer
try:
    from plyer import compass
    COMPASS_AVAILABLE = True
except ImportError:
    COMPASS_AVAILABLE = False


class CompassRoot(BoxLayout):
    def init(self, **kwargs):
        super(CompassRoot, self).init(**kwargs)
        self.orientation = 'vertical'
        self.padding = 30
        self.spacing = 20

        # Заголовок
        self.title_label = Label(
            text="[b]CYBER COMPASS[/b]",
            markup=True,
            font_size='28sp',
            size_hint_y=None,
            height='60dp',
            halign='center'
        )
        self.add_widget(self.title_label)

        # Индикатор градусов и направления
        self.info_label = Label(
            text="Инициализация компаса...",
            font_size='22sp',
            halign='center',
            valign='middle'
        )
        self.add_widget(self.info_label)

        # Кнопка управления датчиком
        self.toggle_btn = Button(
            text="Включить датчик",
            font_size='18sp',
            size_hint_y=None,
            height='50dp'
        )
        self.toggle_btn.bind(on_press=self.toggle_sensor)
        self.add_widget(self.toggle_btn)

        self.sensor_active = False

    def toggle_sensor(self, instance):
        if not COMPASS_AVAILABLE:
            self.info_label.text = "Ошибка: модуль plyer.compass недоступен."
            return

        if not self.sensor_active:
            try:
                if platform == 'android':
                    # Запрос разрешений и включение датчика на Android
                    from android.permissions import request_permissions, Permission
                    request_permissions([Permission.ACCESS_FINE_LOCATION, Permission.ACCESS_COARSE_LOCATION])
                
                compass.enable()
                Clock.schedule_interval(self.update_compass, 0.2)
                self.sensor_active = True
                self.toggle_btn.text = "Выключить датчик"
                self.info_label.text = "Ожидание данных датчика..."
            except Exception as e:
                self.info_label.text = f"Ошибка запуска: {str(e)}"
        else:
            try:
                compass.disable()
                Clock.unschedule(self.update_compass)
                self.sensor_active = False
                self.toggle_btn.text = "Включить датчик"
                self.info_label.text = "Датчик выключен."
            except Exception as e:
                self.info_label.text = f"Ошибка остановки: {str(e)}"

    def update_compass(self, dt):
        try:
            heading = compass.get_orientation()
            if heading and isinstance(heading, (list, tuple)) and heading[0] is not None:
                azimuth = heading[0]
                direction = self.get_cardinal_direction(azimuth)
                self.info_label.text = f"Азимут: {azimuth:.1f}°\nНаправление: {direction}"
            else:
                self.info_label.text = "Калибровка датчика...\nПовращайте устройство."
        except Exception as e:
            self.info_label.text = f"Ошибка чтения: {str(e)}"

    def get_cardinal_direction(self, azimuth):
        dirs = ["С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ"]
        index = int((azimuth + 22.5) / 45) % 8
        return dirs[index]


class CyberCompassApp(App):
    def build(self):
        return CompassRoot()

    def on_stop(self):
        if COMPASS_AVAILABLE and self.root.sensor_active:
            try:
                compass.disable()
            except:
                pass


if name == 'main':
    CyberCompassApp().run()