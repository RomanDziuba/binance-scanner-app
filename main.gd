extends Node2D

const CELL_SIZE = 36
const BOARD_WIDTH = 10
const BOARD_HEIGHT = 20
const BOARD_OFFSET_X = 40
const BOARD_OFFSET_Y = 160

var board = []
var current_piece = []
var current_pos = Vector2i(0, 0)
var current_color = Color.WHITE
var score = 0
var lines_total = 0
var level = 1
var game_over = false
var drop_timer = 0.0
var drop_interval = 0.5

# Аудио плееры для звуков
var sfx_move: AudioStreamPlayer
var sfx_rotate: AudioStreamPlayer
var sfx_drop: AudioStreamPlayer
var sfx_clear: AudioStreamPlayer

const SHAPES = [
    [],
    [[0,0],[1,0],[2,0],[3,0]], # I (Голубой)
    [[0,0],[0,1],[1,1],[2,1]], # J (Синий)
    [[2,0],[0,1],[1,1],[2,1]], # L (Оранжевый)
    [[1,0],[2,0],[1,1],[2,1]], # O (Желтый)
    [[1,0],[2,0],[0,1],[1,1]], # S (Зеленый)
    [[1,0],[0,1],[1,1],[2,1]], # T (Фиолетовый)
    [[0,0],[1,0],[1,1],[2,1]]  # Z (Красный)
]

const COLORS = [
    Color.TRANSPARENT,
    Color.CYAN,
    Color.DODGER_BLUE,
    Color.ORANGE,
    Color.YELLOW,
    Color.GREEN_YELLOW,
    Color.VIOLET,
    Color.CRIMSON
]

func _ready():
    setup_audio()
    init_board()
    spawn_piece()
    set_process(true)
    set_process_input(true)

func setup_audio():
    # Создаем аудиоплееры программно для звуковых эффектов
    sfx_move = AudioStreamPlayer.new()
    sfx_rotate = AudioStreamPlayer.new()
    sfx_drop = AudioStreamPlayer.new()
    sfx_clear = AudioStreamPlayer.new()
    
    add_child(sfx_move)
    add_child(sfx_rotate)
    add_child(sfx_drop)
    add_child(sfx_clear)
    
    # Безопасная загрузка звуков (если файлы добавлены в проект)
    if ResourceLoader.exists("res://move.wav"):
        sfx_move.stream = load("res://move.wav")
    if ResourceLoader.exists("res://rotate.wav"):
        sfx_rotate.stream = load("res://rotate.wav")
    if ResourceLoader.exists("res://drop.wav"):
        sfx_drop.stream = load("res://drop.wav")
    if ResourceLoader.exists("res://clear.wav"):
        sfx_clear.stream = load("res://clear.wav")

func play_sound(player: AudioStreamPlayer):
    if player and player.stream:
        player.play()

func init_board():
    board = []
    for y in range(BOARD_HEIGHT):
        var row = []
        for x in range(BOARD_WIDTH):
            row.append(0)
        board.append(row)

func spawn_piece():
    var shape_idx = randi() % 7 + 1
    current_piece = SHAPES[shape_idx].duplicate(true)
    current_color = COLORS[shape_idx]
    current_pos = Vector2i(BOARD_WIDTH / 2 - 2, 0)
    
    if check_collision(current_pos, current_piece):
        game_over = true

func check_collision(pos, piece):
    for block in piece:
        var px = pos.x + block[0]
        var py = pos.y + block[1]
        if px < 0 or px >= BOARD_WIDTH or py >= BOARD_HEIGHT:
            return true
        if py >= 0 and board[py][px] != 0:
            return true
    return false

func merge_piece():
    for block in current_piece:
        var px = current_pos.x + block[0]
        var py = current_pos.y + block[1]
        if py >= 0:
            for i in range(1, COLORS.size()):
                if COLORS[i] == current_color:
                    board[py][px] = i
                    break
    play_sound(sfx_drop)

func clear_lines():
    var lines_cleared = 0
    var y = BOARD_HEIGHT - 1
    while y >= 0:
        var full = true
        for x in range(BOARD_WIDTH):
            if board[y][x] == 0:
                full = false
                break
        if full:
            board.remove_at(y)
            var new_row = []
            for x in range(BOARD_WIDTH):
                new_row.append(0)
            board.insert(0, new_row)
            lines_cleared += 1
        else:
            y -= 1
            
    if lines_cleared > 0:
        lines_total += lines_cleared
        score += lines_cleared * 100 * level
        level = 1 + lines_total / 10
        drop_interval = max(0.1, 0.5 - (level - 1) * 0.05)
        play_sound(sfx_clear)