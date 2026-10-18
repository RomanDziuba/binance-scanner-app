import os
import traceback
from kivy.app import App
from kivy.uix.label import Label

class DiagnosticApp(App):
    def build(self):
        try:
            # Пробуем записать файл в папку данных приложения
            log_path = os.path.join(self.user_data_dir, "crash_log.txt")
            with open(log_path, "w", encoding="utf-8") as f:
                f.write("Инициализация прошла успешно!\n")
            
            return Label(text="Успешный запуск на Xiaomi!", font_size='22sp')
        except Exception as e:
            err = traceback.format_exc()
            return Label(text=f"Ошибка:\n{err}", font_size='14sp')

if name == 'main':
    DiagnosticApp().run()