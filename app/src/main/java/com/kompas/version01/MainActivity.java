package com.kompas.version01;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_PERMISSIONS = 100;
    private static final int REQUEST_CODE_OPEN_IMAGE = 101;

    // Внутреннее разрешение холста
    private static final int CANVAS_W = 800;
    private static final int CANVAS_H = 600;

    // Модель слоя
    public static class Layer {
        public Bitmap bitmap;
        public Canvas canvas;
        public boolean visible = true;

        public Layer(int w, int h) {
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmap);
            canvas.drawColor(Color.TRANSPARENT);
        }
    }

    private List<Layer> layers = new ArrayList<>();
    private int activeLayerIndex = 0;

    private String currentTool = "brush";
    private int brushColor = Color.BLACK;
    private int brushSize = 5;
    private int brushOpacity = 255; // 0 - 255

    private boolean isSymmetry = false;
    private boolean isStabilized = false;

    private PaintView paintView;
    private LinearLayout layersListLayout;
    private TextView sizeValText, opacValText;

    // История для Undo / Redo
    private static class HistoryState {
        List<Bitmap> bitmaps;
        int activeIndex;
    }
    private Stack<HistoryState> undoStack = new Stack<>();
    private Stack<HistoryState> redoStack = new Stack<>();
    private Button btnUndo, btnRedo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissionsIfNeeded();

        // Главный контейнер (Темная тема в стиле редактора)
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#1e1e1e"));
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // ==================== ВЕРХНЯЯ ПАНЕЛЬ ====================
        HorizontalScrollView topBarScroll = new HorizontalScrollView(this);
        topBarScroll.setBackgroundColor(Color.parseColor("#2d2d2d"));
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setPadding(10, 10, 10, 10);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBarScroll.addView(topBar);

        // Выбор цвета
        final ColorPickerButton colorPicker = new ColorPickerButton(this);
        topBar.addView(colorPicker);

        // Размер кисти
        LinearLayout sizeGroup = createControlGroup("Размер:", sizeValText = new TextView(this), 1, 100, 5, new OnProgressChanged() {
            @Override
            public void onProgress(int val) {
                brushSize = val;
                sizeValText.setText(String.valueOf(val));
            }
        });
        topBar.addView(sizeGroup);

        // Прозрачность
        LinearLayout opacGroup = createControlGroup("Прозр.%:", opacValText = new TextView(this), 1, 100, 100, new OnProgressChanged() {
            @Override
            public void onProgress(int val) {
                brushOpacity = (int) (val * 255.0 / 100.0);
                opacValText.setText(String.valueOf(val));
            }
        });
        topBar.addView(opacGroup);

        // Чекбокс Симметрия
        CheckBox symCheck = new CheckBox(this);
        symCheck.setText("🪞 Симметрия");
        symCheck.setTextColor(Color.WHITE);
        symCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isSymmetry = isChecked;
            paintView.invalidate();
        });
        topBar.addView(symCheck);

        // Чекбокс Стабилизация
        CheckBox stabCheck = new CheckBox(this);
        stabCheck.setText("📈 Стаб.");
        stabCheck.setTextColor(Color.WHITE);
        stabCheck.setOnCheckedChangeListener((buttonView, isChecked) -> isStabilized = isChecked);
        topBar.addView(stabCheck);

        // Кнопки Undo / Redo
        btnUndo = createButton("↩", v -> undo());
        btnRedo = createButton("↪", v -> redo());
        btnUndo.setEnabled(false);
        btnRedo.setEnabled(false);
        topBar.addView(btnUndo);
        topBar.addView(btnRedo);

        // Кнопки Открыть / Сохранить
        Button btnOpen = createButton("📂 Открыть", v -> openImageFromGallery());
        Button btnSave = createButton("💾 Сохранить", v -> saveArtworkToPhone());
        topBar.addView(btnOpen);
        topBar.addView(btnSave);

        rootLayout.addView(topBarScroll);

        // ==================== ОСНОВНАЯ РАБОЧАЯ ОБЛАСТЬ ====================
        LinearLayout mainArea = new LinearLayout(this);
        mainArea.setOrientation(LinearLayout.HORIZONTAL);
        mainArea.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f));

        // 1. Левая панель инструментов
        ScrollView toolsScroll = new ScrollView(this);
        toolsScroll.setBackgroundColor(Color.parseColor("#2d2d2d"));
        LinearLayout toolsPanel = new LinearLayout(this);
        toolsPanel.setOrientation(LinearLayout.VERTICAL);
        toolsPanel.setPadding(8, 8, 8, 8);
        toolsScroll.addView(toolsPanel);

        String[][] toolsData = {
            {"brush", "🖌️", "Кисть"},
            {"eraser", "🧽", "Ластик"},
            {"bucket", "🪣", "Заливка"},
            {"circle", "⭕", "Точный круг"},
            {"airbrush", "💨", "Пульверизатор"},
            {"marker", "🖊️", "Маркер"},
            {"calligraphy", "✒️", "Каллиграфия"},
            {"pencil", "✏️", "Карандаш"},
            {"highlighter", "🖍️", "Выделитель"},
            {"dotted", "➖", "Пунктир"},
            {"neon", "🌟", "Неон"},
            {"pixel", "🧊", "Пиксель"}
        };

        final List<Button> toolButtons = new ArrayList<>();
        for (String[] t : toolsData) {
            final Button tBtn = new Button(this);
            tBtn.setText(t[1]);
            tBtn.setTag(t[0]);
            tBtn.setOnClickListener(v -> {
                for (Button b : toolButtons) b.setBackgroundColor(Color.parseColor("#444444"));
                tBtn.setBackgroundColor(Color.parseColor("#5865F2"));
                currentTool = (String) v.getTag();
                if (currentTool.equals("highlighter")) {
                    colorPicker.setColor(Color.YELLOW);
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(110, 110);
            lp.setMargins(0, 4, 0, 4);
            tBtn.setLayoutParams(lp);
            toolsPanel.addView(tBtn);
            toolButtons.add(tBtn);
            if (t[0].equals("brush")) tBtn.setBackgroundColor(Color.parseColor("#5865F2"));
        }
        mainArea.addView(toolsScroll);

        // 2. Центр: Холст
        FrameLayout workspace = new FrameLayout(this);
        workspace.setBackgroundColor(Color.parseColor("#252525"));
        LinearLayout.LayoutParams wsParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
        workspace.setLayoutParams(wsParams);

        paintView = new PaintView(this);
        workspace.addView(paintView);
        mainArea.addView(workspace);

        // 3. Правая панель слоев
        LinearLayout layersPanel = new LinearLayout(this);
        layersPanel.setOrientation(LinearLayout.VERTICAL);
        layersPanel.setBackgroundColor(Color.parseColor("#2d2d2d"));
        layersPanel.setLayoutParams(new LinearLayout.LayoutParams(260, LinearLayout.LayoutParams.MATCH_PARENT));

        // Шапка слоев
        LinearLayout layersHeader = new LinearLayout(this);
        layersHeader.setOrientation(LinearLayout.HORIZONTAL);
        layersHeader.setPadding(10, 10, 10, 10);
        layersHeader.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView layersTitle = new TextView(this);
        layersTitle.setText("Слои");
        layersTitle.setTextColor(Color.WHITE);
        layersTitle.setTextSize(16f);
        layersTitle.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        layersHeader.addView(layersTitle);

        Button btnAddLayer = createButton("+ Слой", v -> {
            saveStateToUndo();
            layers.add(new Layer(CANVAS_W, CANVAS_H));
            activeLayerIndex = layers.size() - 1;
            renderLayersUI();
            paintView.invalidate();
        });
        layersHeader.addView(btnAddLayer);
        layersPanel.addView(layersHeader);

        // Список слоев
        ScrollView layersScroll = new ScrollView(this);
        layersListLayout = new LinearLayout(this);
        layersListLayout.setOrientation(LinearLayout.VERTICAL);
        layersListLayout.setPadding(8, 8, 8, 8);
        layersScroll.addView(layersListLayout);
        layersPanel.addView(layersScroll);

        // Кнопки очистки/удаления слоя
        LinearLayout layerActions = new LinearLayout(this);
        layerActions.setOrientation(LinearLayout.HORIZONTAL);
        layerActions.setPadding(8, 8, 8, 8);
        
        Button btnClearLayer = createButton("🧹 Очистить", v -> {
            if (!layers.isEmpty()) {
                saveStateToUndo();
                layers.get(activeLayerIndex).canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                paintView.invalidate();
            }
        });
        Button btnDelLayer = createButton("🗑️", v -> {
            if (layers.size() > 1) {
                saveStateToUndo();
                layers.remove(activeLayerIndex);
                if (activeLayerIndex >= layers.size()) activeLayerIndex = layers.size() - 1;
                renderLayersUI();
                paintView.invalidate();
            } else {
                layers.get(0).canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                paintView.invalidate();
            }
        });
        layerActions.addView(btnClearLayer);
        layerActions.addView(btnDelLayer);
        layersPanel.addView(layerActions);

        mainArea.addView(layersPanel);
        rootLayout.addView(mainArea);

        setContentView(rootLayout);

        // Создаем первый стартовый слой
        layers.add(new Layer(CANVAS_W, CANVAS_H));
        renderLayersUI();
    }

    // ==================== КАСТОМНЫЙ ВИД РИСОВАНИЯ ====================
    private class PaintView extends View {
        private float lastX, lastY, startX, startY;
        private float smoothX, smoothY;
        private boolean isDrawing = false;
        private Bitmap previewBitmap;
        private Canvas previewCanvas;

        public PaintView(android.content.Context context) {
            super(context);
            previewBitmap = Bitmap.createBitmap(CANVAS_W, CANVAS_H, Bitmap.Config.ARGB_8888);
            previewCanvas = new Canvas(previewBitmap);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            float scaleX = (float) getWidth() / CANVAS_W;
            float scaleY = (float) getHeight() / CANVAS_H;
            float scale = Math.min(scaleX, scaleY);
            
            float offsetX = (getWidth() - CANVAS_W * scale) / 2f;
            float offsetY = (getHeight() - CANVAS_H * scale) / 2f;

            canvas.save();
            canvas.translate(offsetX, offsetY);
            canvas.scale(scale, scale);

            Paint bgPaint = new Paint();
            bgPaint.setColor(Color.WHITE);
            canvas.drawRect(0, 0, CANVAS_W, CANVAS_H, bgPaint);

            for (Layer layer : layers) {
                if (layer.visible) {
                    canvas.drawBitmap(layer.bitmap, 0, 0, null);
                }
            }

            canvas.drawBitmap(previewBitmap, 0, 0, null);
            canvas.restore();

            if (isSymmetry) {
                Paint linePaint = new Paint();
                linePaint.setColor(Color.RED);
                linePaint.setStrokeWidth(2f);
                linePaint.setStyle(Paint.Style.STROKE);
                float symScreenX = offsetX + (CANVAS_W / 2f) * scale;
                canvas.drawLine(symScreenX, offsetY, symScreenX, offsetY + CANVAS_H * scale, linePaint);
            }
        }

        private float[] getCanvasCoords(float screenX, float screenY) {
            float scaleX = (float) getWidth() / CANVAS_W;
            float scaleY = (float) getHeight() / CANVAS_H;
            float scale = Math.min(scaleX, scaleY);
            
            float offsetX = (getWidth() - CANVAS_W * scale) / 2f;
            float offsetY = (getHeight() - CANVAS_H * scale) / 2f;

            float x = (screenX - offsetX) / scale;
            float y = (screenY - offsetY) / scale;
            return new float[]{x, y};
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (layers.isEmpty() || !layers.get(activeLayerIndex).visible) return true;

            float[] coords = getCanvasCoords(event.getX(), event.getY());
            float x = coords[0];
            float y = coords[1];

            Layer activeLayer = layers.get(activeLayerIndex);
            Canvas canvas = activeLayer.canvas;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    saveStateToUndo();
                    isDrawing = true;
                    startX = x; startY = y;
                    lastX = x; lastY = y;
                    smoothX = x; smoothY = y;

                    if (currentTool.equals("bucket")) {
                        floodFill(activeLayer.bitmap, (int)x, (int)y, brushColor);
                        if (isSymmetry) floodFill(activeLayer.bitmap, (int)(CANVAS_W - x), (int)y, brushColor);
                        invalidate();
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (!isDrawing) break;

                    if (isStabilized) {
                        smoothX += (x - smoothX) * 0.2f;
                        smoothY += (y - smoothY) * 0.2f;
                        x = smoothX;
                        y = smoothY;
                    }

                    if (currentTool.equals("circle")) {
                        previewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        Paint p = createToolPaint();
                        drawShapeCircle(previewCanvas, startX, startY, x, y, p);
                        if (isSymmetry) drawShapeCircle(previewCanvas, CANVAS_W - startX, startY, CANVAS_W - x, y, p);
                    } else if (currentTool.equals("airbrush")) {
                        drawAirbrush(canvas, x, y);
                        if (isSymmetry) drawAirbrush(canvas, CANVAS_W - x, y);
                    } else {
                        Paint p = createToolPaint();
                        drawToolPath(canvas, lastX, lastY, x, y, p);
                        if (isSymmetry) drawToolPath(canvas, CANVAS_W - lastX, lastY, CANVAS_W - x, y, p);
                    }

                    lastX = x; lastY = y;
                    invalidate();
                    break;

                case MotionEvent.ACTION_UP:
                    if (!isDrawing) break;
                    isDrawing = false;

                    if (currentTool.equals("circle")) {
                        previewCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        Paint p = createToolPaint();
                        drawShapeCircle(canvas, startX, startY, x, y, p);
                        if (isSymmetry) drawShapeCircle(canvas, CANVAS_W - startX, startY, CANVAS_W - x, y, p);
                    }
                    invalidate();
                    break;
            }
            return true;
        }
    }

    // ==================== НАСТРОЙКА ИНСТРУМЕНТОВ ====================
    private Paint createToolPaint() {
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setStrokeWidth(brushSize);
        p.setColor(brushColor);
        p.setAlpha(brushOpacity);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);

        switch (currentTool) {
            case "eraser":
                p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                p.setAlpha(255);
                break;
            case "marker":
            case "highlighter":
                p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
                if (currentTool.equals("highlighter")) {
                    p.setAlpha((int)(brushOpacity * 0.4));
                    p.setStrokeWidth(brushSize * 2);
                }
                break;
            case "pencil":
                p.setStrokeWidth(Math.max(1, brushSize * 0.5f));
                break;
            case "dotted":
                p.setPathEffect(new DashPathEffect(new float[]{brushSize * 2, brushSize * 2}, 0));
                break;
            case "neon":
                p.setMaskFilter(new BlurMaskFilter(brushSize, BlurMaskFilter.Blur.NORMAL));
                p.setColor(Color.WHITE);
                break;
            case "pixel":
                p.setAntiAlias(false);
                p.setStrokeCap(Paint.Cap.SQUARE);
                break;
        }
        return p;
    }

    private void drawToolPath(Canvas canvas, float x1, float y1, float x2, float y2, Paint paint) {
        if (currentTool.equals("calligraphy")) {
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(x2 - brushSize/2f, y2 - brushSize/2f, x2 + brushSize/2f, y2 + brushSize/2f, paint);
        } else {
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(x1, y1, x2, y2, paint);
        }
    }

    private void drawShapeCircle(Canvas canvas, float x1, float y1, float x2, float y2, Paint paint) {
        paint.setStyle(Paint.Style.STROKE);
        float radius = (float) Math.hypot(x2 - x1, y2 - y1);
        canvas.drawCircle(x1, y1, radius, paint);
    }

    private void drawAirbrush(Canvas canvas, float cx, float cy) {
        Paint p = new Paint();
        p.setColor(brushColor);
        p.setAlpha((int)(brushOpacity * 0.3));
        int density = brushSize * 2;
        for (int i = 0; i < density; i++) {
            double r = Math.random() * brushSize;
            double angle = Math.random() * Math.PI * 2;
            float px = (float)(cx + r * Math.cos(angle));
            float py = (float)(cy + r * Math.sin(angle));
            canvas.drawPoint(px, py, p);
        }
    }

    private void floodFill(Bitmap bmp, int x, int y, int newColor) {
        if (x < 0 || x >= bmp.getWidth() || y < 0 || y >= bmp.getHeight()) return;
        int targetColor = bmp.getPixel(x, y);
        if (targetColor == newColor) return;

        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int[] pixels = new int[w * h];
        bmp.getPixels(pixels, 0, w, 0, 0, w, h);

        int target = targetColor;
        List<Point> queue = new ArrayList<>();
        queue.add(new Point(x, y));

        while (!queue.isEmpty()) {
            Point pt = queue.remove(queue.size() - 1);
            int px = pt.x;
            int py = pt.y;

            if (px < 0 || px >= w || py < 0 || py >= h) continue;
            int idx = py * w + px;
            if (pixels[idx] != target) continue;

            int lx = px;
            while (lx >= 0 && pixels[py * w + lx] == target) lx--;
            lx++;
            int rx = px;
            while (rx < w && pixels[py * w + rx] == target) rx++;
            rx--;

            for (int i = lx; i <= rx; i++) {
                pixels[py * w + i] = newColor;
                if (py > 0 && pixels[(py - 1) * w + i] == target) queue.add(new Point(i, py - 1));
                if (py < h - 1 && pixels[(py + 1) * w + i] == target) queue.add(new Point(i, py + 1));
            }
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h);
    }

    private void renderLayersUI() {
        layersListLayout.removeAllViews();
        for (int i = layers.size() - 1; i >= 0; i--) {
            final int index = i;
            Layer layer = layers.get(i);

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setPadding(10, 10, 10, 10);
            item.setGravity(Gravity.CENTER_VERTICAL);
            if (index == activeLayerIndex) {
                item.setBackgroundColor(Color.parseColor("#383a40"));
            }

            TextView visBtn = new TextView(this);
            visBtn.setText(layer.visible ? "👁" : "✖");
            visBtn.setTextColor(Color.WHITE);
            visBtn.setTextSize(16f);
            visBtn.setPadding(0, 0, 15, 0);
            visBtn.setOnClickListener(v -> {
                layer.visible = !layer.visible;
                renderLayersUI();
                paintView.invalidate();
            });
            item.addView(visBtn);

            TextView nameView = new TextView(this);
            nameView.setText("Слой " + (index + 1));
            nameView.setTextColor(Color.WHITE);
            nameView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
            item.addView(nameView);

            item.setOnClickListener(v -> {
                activeLayerIndex = index;
                renderLayersUI();
            });

            layersListLayout.addView(item);
        }
    }

    private void saveStateToUndo() {
        HistoryState state = new HistoryState();
        state.bitmaps = new ArrayList<>();
        for (Layer l : layers) {
            state.bitmaps.add(l.bitmap.copy(Bitmap.Config.ARGB_8888, true));
        }
        state.activeIndex = activeLayerIndex;
        undoStack.push(state);
        if (undoStack.size() > 20) undoStack.remove(0);
        redoStack.clear();
        updateUndoRedoButtons();
    }

    private void undo() {
        if (undoStack.isEmpty()) return;
        HistoryState currentState = new HistoryState();
        currentState.bitmaps = new ArrayList<>();
        for (Layer l : layers) currentState.bitmaps.add(l.bitmap.copy(Bitmap.Config.ARGB_8888, true));
        currentState.activeIndex = activeLayerIndex;
        redoStack.push(currentState);

        HistoryState prev = undoStack.pop();
        restoreState(prev);
        updateUndoRedoButtons();
    }

    private void redo() {
        if (redoStack.isEmpty()) return;
        HistoryState currentState = new HistoryState();
        currentState.bitmaps = new ArrayList<>();
        for (Layer l : layers) currentState.bitmaps.add(l.bitmap.copy(Bitmap.Config.ARGB_8888, true));
        currentState.activeIndex = activeLayerIndex;
        undoStack.push(currentState);

        HistoryState next = redoStack.pop();
        restoreState(next);
        updateUndoRedoButtons();
    }

    private void restoreState(HistoryState state) {
        layers.clear();
        for (Bitmap b : state.bitmaps) {
            Layer l = new Layer(CANVAS_W, CANVAS_H);
            l.canvas.drawBitmap(b, 0, 0, null);
            layers.add(l);
        }
        activeLayerIndex = Math.min(state.activeIndex, layers.size() - 1);
        renderLayersUI();
        paintView.invalidate();
    }

    private void updateUndoRedoButtons() {
        btnUndo.setEnabled(!undoStack.isEmpty());
        btnRedo.setEnabled(!redoStack.isEmpty());
    }

    private void saveArtworkToPhone() {
        try {
            Bitmap resultBitmap = Bitmap.createBitmap(CANVAS_W, CANVAS_H, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(resultBitmap);
            canvas.drawColor(Color.WHITE);

            for (Layer l : layers) {
                if (l.visible) canvas.drawBitmap(l.bitmap, 0, 0, null);
            }

            OutputStream fos;
            String fileName = "PaintPro_" + System.currentTimeMillis() + ".png";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PaintPro");
                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                fos = getContentResolver().openOutputStream(uri);
            } else {
                java.io.File dir = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "PaintPro");
                if (!dir.exists()) dir.mkdirs();
                java.io.File file = new java.io.File(dir, fileName);
                fos = new java.io.FileOutputStream(file);
            }

            resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            Toast.makeText(this, "✅ Успешно сохранено в Галерею (Pictures/PaintPro)!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "❌ Ошибка сохранения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_OPEN_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_IMAGE && resultCode == RESULT_OK && data != null) {
            try {
                Uri imageUri = data.getData();
                Bitmap img = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                if (img != null) {
                    saveStateToUndo();
                    Layer newLayer = new Layer(CANVAS_W, CANVAS_H);
                    float scale = Math.min((float)CANVAS_W / img.getWidth(), (float)CANVAS_H / img.getHeight());
                    float left = (CANVAS_W - img.getWidth() * scale) / 2f;
                    float top = (CANVAS_H - img.getHeight() * scale) / 2f;
                    newLayer.canvas.drawBitmap(img, null, new RectF(left, top, left + img.getWidth() * scale, top + img.getHeight() * scale), null);
                    
                    layers.add(newLayer);
                    activeLayerIndex = layers.size() - 1;
                    renderLayersUI();
                    paintView.invalidate();
                    Toast.makeText(this, "📂 Изображение загружено на новый слой!", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "❌ Не удалось открыть файл", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSIONS);
            }
        }
    }

    private interface OnProgressChanged {
        void onProgress(int val);
    }

    private LinearLayout createControlGroup(String labelText, final TextView valView, int min, int max, int defaultVal, final OnProgressChanged callback) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.HORIZONTAL);
        group.setGravity(Gravity.CENTER_VERTICAL);
        group.setPadding(6, 2, 6, 2);

        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextColor(Color.WHITE);
        label.setTextSize(12f);
        group.addView(label);

        valView.setText(String.valueOf(defaultVal));
        valView.setTextColor(Color.parseColor("#38BDF8"));
        valView.setTextSize(12f);
        valView.setPadding(4, 0, 4, 0);
        group.addView(valView);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setLayoutParams(new LinearLayout.LayoutParams(120, LinearLayout.LayoutParams.WRAP_CONTENT));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            seekBar.setMin(min);
        }
        seekBar.setMax(max);
        seekBar.setProgress(defaultVal);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                int p = Math.max(min, progress);
                callback.onProgress(p);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
        group.addView(seekBar);
        return group;
    }

    private Button createButton(String text, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(Color.parseColor("#444444"));
        btn.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 80);
        lp.setMargins(4, 0, 4, 0);
        btn.setLayoutParams(lp);
        return btn;
    }

    private class ColorPickerButton extends Button {
        public ColorPickerButton(android.content.Context context) {
            super(context);
            setColor(Color.BLACK);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(80, 80);
            lp.setMargins(4, 0, 4, 0);
            setLayoutParams(lp);
            setOnClickListener(v -> showColorPickerDialog());
        }

        public void setColor(int color) {
            brushColor = color;
            setBackgroundColor(color);
        }

        private void showColorPickerDialog() {
            final String[] colorsNames = {"Черный", "Белый", "Красный", "Зеленый", "Синий", "Желтый", "Оранжевый", "Фиолетовый", "Розовый"};
            // Чистый массив без Color.PURPLE
            final int[] colorsVals = {Color.BLACK, Color.WHITE, Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.rgb(255,140,0), Color.rgb(128, 0, 128), Color.MAGENTA};

            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Выберите цвет кисти");
            builder.setItems(colorsNames, (dialog, which) -> setColor(colorsVals[which]));
            builder.show();
        }
    }
}