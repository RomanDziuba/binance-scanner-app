extends Node2D

const CELL_SIZE = 36
const BOARD_WIDTH = 10
const BOARD_HEIGHT = 20
const BOARD_OFFSET_X = 40
const BOARD_OFFSET_Y = 140

var board = []
var current_piece = []
var current_pos = Vector2i(0, 0)
var current_color = Color.WHITE
var score = 0
var game_over = false
var drop_timer = 0.0
var drop_interval = 0.5

const SHAPES = [
    [],
    [[0,0],[1,0],[2,0],[3,0]], # I
    [[0,0],[0,1],[1,1],[2,1]], # J
    [[2,0],[0,1],[1,1],[2,1]], # L
    [[1,0],[2,0],[1,1],[2,1]], # O
    [[1,0],[2,0],[0,1],[1,1]], # S
    [[1,0],[0,1],[1,1],[2,1]], # T
    [[0,0],[1,0],[1,1],[2,1]]  # Z
]

const COLORS = [
    Color.TRANSPARENT,
    Color.CYAN,
    Color.BLUE,
    Color.ORANGE,
    Color.YELLOW,
    Color.GREEN,
    Color.PURPLE,
    Color.RED
]

func _ready():
    init_board()
    spawn_piece()
    set_process(true)
    set_process_input(true)

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
        score += lines_cleared * 100 * lines_cleared

func rotate_piece():
    var rotated = []
    for block in current_piece:
        var rx = -block[1] + 1
        var ry = block[0]
        rotated.append([rx, ry])
    if not check_collision(current_pos, rotated):
        current_piece = rotated

func _process(delta):
    if game_over:
        return
    drop_timer += delta
    if drop_timer >= drop_interval:
        drop_timer = 0.0
        move_down()
    queue_redraw()

func move_down():
    current_pos.y += 1
    if check_collision(current_pos, current_piece):
        current_pos.y -= 1
        merge_piece()
        clear_lines()
        spawn_piece()

func _input(event):
    if game_over:
        if (event is InputEventMouseButton and event.pressed) or (event is InputEventScreenTouch and event.pressed):
            init_board()
            score = 0
            game_over = false
            spawn_piece()
        return

    if event is InputEventScreenTouch and event.pressed:
        var pos = event.position
        if pos.x < 240:
            current_pos.x -= 1
            if check_collision(current_pos, current_piece):
                current_pos.x += 1
        elif pos.x > 480:
            current_pos.x += 1
            if check_collision(current_pos, current_piece):
                current_pos.x -= 1
        else:
            rotate_piece()
    elif event is InputEventKey and event.pressed:
        if event.keycode == KEY_LEFT:
            current_pos.x -= 1
            if check_collision(current_pos, current_piece):
                current_pos.x += 1
        elif event.keycode == KEY_RIGHT:
            current_pos.x += 1
            if check_collision(current_pos, current_piece):
                current_pos.x -= 1
        elif event.keycode == KEY_DOWN:
            move_down()
        elif event.keycode == KEY_UP:
            rotate_piece()

func _draw():
    draw_rect(Rect2(0, 0, 720, 1280), Color(0.08, 0.08, 0.12))
    
    draw_rect(Rect2(BOARD_OFFSET_X - 4, BOARD_OFFSET_Y - 4, BOARD_WIDTH * CELL_SIZE + 8, BOARD_HEIGHT * CELL_SIZE + 8), Color(0.2, 0.2, 0.3), false, 4.0)
    
    for y in range(BOARD_HEIGHT):
        for x in range(BOARD_WIDTH):
            var color_idx = board[y][x]
            if color_idx > 0:
                var rect = Rect2(BOARD_OFFSET_X + x * CELL_SIZE, BOARD_OFFSET_Y + y * CELL_SIZE, CELL_SIZE - 2, CELL_SIZE - 2)
                draw_rect(rect, COLORS[color_idx])

    if not game_over:
        for block in current_piece:
            var px = current_pos.x + block[0]
            var py = current_pos.y + block[1]
            if py >= 0:
                var rect = Rect2(BOARD_OFFSET_X + px * CELL_SIZE, BOARD_OFFSET_Y + py * CELL_SIZE, CELL_SIZE - 2, CELL_SIZE - 2)
                draw_rect(rect, current_color)

    draw_string(ThemeDB.fallback_font, Vector2(40, 90), "СЧЕТ: " + str(score), HORIZONTAL_ALIGNMENT_LEFT, -1, 32, Color.WHITE)
    
    if game_over:
        draw_rect(Rect2(60, 450, 600, 250), Color(0.1, 0.1, 0.1, 0.9))
        draw_string(ThemeDB.fallback_font, Vector2(120, 540), "ИГРА ОКОНЧЕНА", HORIZONTAL_ALIGNMENT_CENTER, -1, 48, Color.RED)
        draw_string(ThemeDB.fallback_font, Vector2(140, 610), "Тапните для сброса", HORIZONTAL_ALIGNMENT_CENTER, -1, 28, Color.YELLOW)
