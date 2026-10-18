extends Node2D

func _ready():
    print("Godot приложение успешно запущено на Xiaomi!")
    
    # Создаем простой текст на экране для проверки
    var label = Label.new()
    label.text = "Успешный запуск Godot на Xiaomi!"
    label.position = Vector2(50, 200)
    # Увеличиваем шрифт
    label.add_theme_font_size_override("font_size", 32)
    add_child(label)
