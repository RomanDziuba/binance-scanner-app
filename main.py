import random
import traceback
from kivy.app import App
from kivy.uix.widget import Widget
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.gridlayout import GridLayout
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.graphics import Color, Rectangle
from kivy.clock import Clock

ROWS = 18
COLS = 10

SHAPES = [
    [[1, 1, 1, 1]],
    [[1, 1], [1, 1]],
    [[0, 1, 0], [1, 1, 1]],
    [[1, 0, 0], [1, 1, 1]],
    [[0, 0, 1], [1, 1, 1]],
    [[0, 1, 1], [1, 1, 0]],
    [[1, 1, 0], [0, 1, 1]]
]

COLORS = [
    (0, 0.8, 0.8),
    (0.9, 0.9, 0),
    (0.6, 0, 0.8),
    (0.9, 0.5, 0),
    (0, 0, 0.9),
    (0, 0.8, 0),
    (0.9, 0, 0)
]

class SafeTetrisGame(Widget):
    def init(self, **kwargs):
        super().init(**kwargs)
        self.grid = [[None for _ in range(COLS)] for _ in range(ROWS)]
        self.score = 0
        self.game_over = False
        
        self.current_shape = None
        self.current_color = None
        self.current_x = 0
        self.current_y = 0
        
        self.spawn_shape()
        Clock.schedule_interval(self.update, 0.5)

    def spawn_shape(self):
        idx = random.randint(0, len(SHAPES) - 1)
        self.current_shape = SHAPES[idx]
        self.current_color = COLORS[idx]
        self.current_x = COLS // 2 - len(self.current_shape[0]) // 2
        self.current_y = 0
        
        if self.check_collision(self.current_shape, self.current_x, self.current_y):
            self.game_over = True

    def check_collision(self, shape, offset_x, offset_y):
        for r, row in enumerate(shape):
            for c, val in enumerate(row):
                if val:
                    new_x = offset_x + c
                    new_y = offset_y + r
                    if new_x < 0 or new_x >= COLS or new_y >= ROWS:
                        return True
                    if new_y >= 0 and self.grid[new_y][new_x] is not None:
                        return True
        return False

    def merge_shape(self):
        for r, row in enumerate(self.current_shape):
            for c, val in enumerate(row):
                if val:
                    self.grid[self.current_y + r][self.current_x + c] = self.current_color
        self.clear_lines()
        self.spawn_shape()

    def clear_lines(self):
        lines_to_clear = [r for r, row in enumerate(self.grid) if all(cell is not None for cell in row)]
        for r in lines_to_clear:
            del self.grid[r]
            self.grid.insert(0, [None for _ in range(COLS)])
            self.score += 100

    def rotate_shape(self):
        if self.game_over or not self.current_shape:
            return
        rotated = list(zip(*self.current_shape[::-1]))
        if not self.check_collision(rotated, self.current_x, self.current_y):
            self.current_shape = rotated
            self.draw()

    def move(self, dx):
        if not self.game_over and self.current_shape and not self.check_collision(self.current_shape, self.current_x + dx, self.current_y):
            self.current_x += dx
            self.draw()

    def drop(self):
        if not self.game_over and self.current_shape:
            while not self.check_collision(self.current_shape, self.current_x, self.current_y + 1):
                self.current_y += 1
            self.merge_shape()
            self.draw()

    def update(self, dt):
        if self.game_over:
            return
        if not self.check_collision(self.current_shape, self.current_x, self.current_y + 1):
            self.current_y += 1
        else:
            self.merge_shape()
        self.draw()

    def draw(self):
        if self.width <= 0 or self.height <= 0:
            return
            
        self.canvas.clear()
        cell_w = self.width / COLS
        cell_h = self.height / ROWS