import os
import traceback
from kivy.app import App
from kivy.uix.label import Label
from kivy.uix.scrollview import ScrollView

class FileLoggerApp(App):
    def build(self):
        try:
            # Пробуем записать тестовый файл в общую папку Download на телефоне
            download_dir = "/sdcard/Download"
            os.makedirs(download_dir, exist_ok=True)
            
            log_file_path = os.path.join(download_dir, "roma_crash_log.txt")
            
            with open(log_file_path, "w", encoding="utf-8") as f:
                f.write("Приложение успешно инициализировано и запущено!\n")
            
            return Label(
                text="✅ Успешный запуск!\nФайл лога создан в папке Download:\nroma_crash_log.txt", 
                font_size='16sp', 
                halign='center',
                valign='middle'
            )
            
        except Exception as e:
            # Если произошел срыв, перехватываем ошибку и пишем её в файл в Download
            err_text = traceback.format_exc()
            try:
                os.makedirs("/sdcard/Download", exist_ok=True)
                with open("/sdcard/Download/roma_crash_error.txt", "w", encoding="utf-8") as f:
                    f.write(err_text)
            except Exception:
                pass
            
            # А также показываем текст ошибки на экране внутри прокручиваемой области
            root = ScrollView()
            lbl = Label(
                text=f"❌ ОШИБКА КРАША:\n\n{err_text}", 
                font_size='12sp', 
                size_hint_y=None,
                color=(1, 0, 0, 1)
            )
            lbl.bind(texture_size=lambda instance, value: setattr(instance, 'height', value[1]))
            root.add_widget(lbl)
            return root

if name == 'main':
    FileLoggerApp().run()