from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.label import Label
from kivy.clock import Clock
from kivy.utils import get_color_from_hex
from kivy.core.window import Window
import datetime

# Установка стильного темного фона приложения
Window.clearcolor = get_color_from_hex('#141414')

class CyberClockApp(App):
    def build(self):
        # Главный вертикальный контейнер с отступами
        layout = BoxLayout(orientation='vertical', padding=40, spacing=20)
        
        # Заголовок меню
        self.title_label = Label(
            text='[b]CYBER UNIVERSAL APP[/b]',
            markup=True,
            font_size='24sp',
            color=get_color_from_hex('#00FFCC'),
            size_hint=(1, 0.2),
            halign='center'
        )
        self.title_label.bind(size=self.title_label.setter('text_size'))
        
        # Метка для отображения времени (крупный шрифт)
        self.time_label = Label(
            text='00:00:00',
            font_size='52sp',
            bold=True,
            color=get_color_from_hex('#FFFFFF'),
            size_hint=(1, 0.3),
            halign='center'
        )
        self.time_label.bind(size=self.time_label.setter('text_size'))
        
        # Метка для отображения текущей даты
        self.date_label = Label(
            text='Загрузка даты...',
            font_size='18sp',
            color=get_color_from_hex('#AAAAAA'),
            size_hint=(1, 0.2),
            halign='center'
        )
        self.date_label.bind(size=self.date_label.setter('text_size'))

        # Добавляем все элементы на экран
        layout.add_widget(self.title_label)
        layout.add_widget(self.time_label)
        layout.add_widget(self.date_label)

        # Запускаем обновление времени каждую секунду
        Clock.schedule_interval(self.update_datetime, 1)
        self.update_datetime(0) # Первичный вызов для мгновенного отображения

        return layout

    def update_datetime(self, dt):
        now = datetime.datetime.now()
        
        # Обновляем время
        self.time_label.text = now.strftime('%H:%M:%S')
        
        # Словари для красивого вывода даты на русском языке
        days = ['Понедельник', 'Вторник', 'Среда', 'Четверг', 'Пятница', 'Суббота', 'Воскресенье']
        months = ['января', 'февраля', 'марта', 'апреля', 'мая', 'июня', 'июля', 'августа', 'сентября', 'октября', 'ноября', 'декабря']
        
        day_name = days[now.weekday()]
        month_name = months[now.month - 1]
        
        # Форматируем строку даты
        self.date_label.text = f"{day_name}, {now.day} {month_name} {now.year}"

if name == 'main':
    CyberClockApp().run()