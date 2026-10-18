from kivy.app import App
from kivy.uix.label import Label

class TestApp(App):
    def build(self):
        return Label(text='Успешный запуск Roma!', font_size='24sp')

if name == 'main':
    TestApp().run()