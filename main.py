import os
import traceback
from kivy.app import App
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.label import Label
from kivy.uix.textinput import TextInput

class LogWriterApp(App):
    def build(self):
        layout = BoxLayout(orientation='vertical', padding=20, spacing=10)
        
        # Определяем путь для сохранения лога в папку данных приложения
        # Обычно это: /data/user/0/roma/files/error_log.txt или аналогичная
        try:
            log_dir = self.user_data_dir
            os.makedirs(log_dir, exist_ok=True)
            log_path = os.path.join(log_dir, "error_log.txt")
            
            # Пишем тестовый лог
            log_content = "Приложение успешно инициализировано и запустилось!\n"
            with open(log_path, "w", encoding="utf-8") as f:
                f.write(log_content)
                
            status_text = f"ЛОГ УСПЕШНО ЗАПИСАН!\nПуть к файлу:\n{log_path}"
        except Exception as e:
            status_text = f"Ошибка записи лога:\n{str(e)}\n\n{traceback.format_exc()}"
        
        # Выводим информацию на экран
        lbl = Label(text="Статус диагностики запуска:", font_size='18sp', size_hint_y=0.2)
        layout.add_widget(lbl)
        
        txt_input = TextInput(text=status_text, readonly=True, font_size='14sp')
        layout.add_widget(txt_input)
        
        return layout

if name == 'main':
    LogWriterApp().run()