package com.fon2.version1;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

public class MainActivity extends Activity {

    private static final int PICK_FG_IMAGE = 1;
    private static final int PICK_BG_IMAGE = 2;
    private static final int PERMISSION_CODE = 100;

    private LayerView layerView;
    private Bitmap foregroundBitmap = null;
    private Bitmap backgroundBitmap = null;
    private TextView tvAngleInfo;

    private static class LayerState {
        float x = 0, y = 0;
        float scale = 1.0f;
        float alpha = 1.0f;
        float rotation = 0.0f;

        public void set(LayerState other) {
            this.x = other.x;
            this.y = other.y;
            this.scale = other.scale;
            this.alpha = other.alpha;
            this.rotation = other.rotation;
        }
    }

    private final LayerState fgState = new LayerState();
    private final LayerState bgState = new LayerState();
    private int activeLayer = 1; // 1 - Передний план, 0 - Фон

    private SeekBar seekBarAlpha;

    // История для Отменить / Повторить
    private static class HistorySnapshot {
        Bitmap fgBitmap, bgBitmap;
        LayerState fgS = new LayerState();
        LayerState bgS = new LayerState();
    }
    private final List<HistorySnapshot> undoStack = new ArrayList<>();
    private final List<HistorySnapshot> redoStack = new ArrayList<>();

    private OrtEnvironment ortEnvironment;
    private OrtSession ortSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(12, 12, 12, 12);
        rootLayout.setBackgroundColor(Color.parseColor("#121212"));

        // Холст с текстом информации об угле поворота поверх
        FrameLayout container = new FrameLayout(this);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        container.setLayoutParams(containerParams);

        layerView = new LayerView(this);
        layerView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        container.addView(layerView);

        tvAngleInfo = new TextView(this);
        tvAngleInfo.setTextColor(Color.YELLOW);
        tvAngleInfo.setTextSize(16);
        tvAngleInfo.setPadding(16, 16, 16, 16);
        FrameLayout.LayoutParams tvParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        tvParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        tvAngleInfo.setLayoutParams(tvParams);
        container.addView(tvAngleInfo);

        rootLayout.addView(container);

        LinearLayout controlsLayout = new LinearLayout(this);
        controlsLayout.setOrientation(LinearLayout.VERTICAL);

        // Кнопки загрузки и ИИ (компактные высоты ~40dp)
        Button btnLoadFg = new Button(this);
        btnLoadFg.setText("📂 Загрузить передний план");
        btnLoadFg.setOnClickListener(v -> openGallery(PICK_FG_IMAGE));
        controlsLayout.addView(btnLoadFg, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 110));

        Button btnRemoveBg = new Button(this);
        btnRemoveBg.setText("✨ Удалить фон");
        btnRemoveBg.setOnClickListener(v -> {
            saveStateToHistory();
            removeBackgroundWithAI();
        });
        controlsLayout.addView(btnRemoveBg, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 110));

        Button btnLoadBg = new Button(this);
        btnLoadBg.setText("🖼 Загрузить свой фон");
        btnLoadBg.setOnClickListener(v -> openGallery(PICK_BG_IMAGE));
        controlsLayout.addView(btnLoadBg, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 110));

        // Панель: Поменять местами + Компактная кнопка удаления слоя в 2 раза меньше
        LinearLayout rowLayerLayout = new LinearLayout(this);
        rowLayerLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button btnSwapLayers = new Button(this);
        btnSwapLayers.setText("🔀 Поменять местами (Верхний активен)");
        btnSwapLayers.setOnClickListener(v -> {
            saveStateToHistory();
            Bitmap tempBmp = foregroundBitmap;
            foregroundBitmap = backgroundBitmap;
            backgroundBitmap = tempBmp;

            LayerState tempState = new LayerState();
            tempState.set(fgState);
            fgState.set(bgState);
            bgState.set(tempState);

            activeLayer = 1; // Верхний слой всегда активен по умолчанию
            syncUIWithActiveLayer();
            layerView.invalidate();
            Toast.makeText(this, "Слои поменялись местами. Верхний активен.", Toast.LENGTH_SHORT).show();
        });
        rowLayerLayout.addView(btnSwapLayers, new LinearLayout.LayoutParams(0, 110, 0.75f));

        Button btnDeleteLayer = new Button(this);
        btnDeleteLayer.setText("🗑 Уд.слой");
        btnDeleteLayer.setTextColor(Color.RED);
        btnDeleteLayer.setOnClickListener(v -> {
            saveStateToHistory();
            if (activeLayer == 1) {
                foregroundBitmap = null;
                Toast.makeText(this, "Передний слой удален", Toast.LENGTH_SHORT).show();
            } else {
                backgroundBitmap = null;
                Toast.makeText(this, "Слой фона удален", Toast.LENGTH_SHORT).show();
            }
            layerView.invalidate();
        });
        rowLayerLayout.addView(btnDeleteLayer, new LinearLayout.LayoutParams(0, 110, 0.25f));
        controlsLayout.addView(rowLayerLayout);

        // Ползунок прозрачности
        LinearLayout alphaLayout = new LinearLayout(this);
        alphaLayout.setOrientation(LinearLayout.HORIZONTAL);
        TextView lblAlpha = new TextView(this);
        lblAlpha.setText("Прозрачность: ");
        lblAlpha.setTextColor(Color.WHITE);
        alphaLayout.addView(lblAlpha);

        seekBarAlpha = new SeekBar(this);
        seekBarAlpha.setMax(100);
        seekBarAlpha.setProgress(100);
        seekBarAlpha.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        seekBarAlpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float alphaVal = progress / 100.0f;
                    if (activeLayer == 1) fgState.alpha = alphaVal;
                    else bgState.alpha = alphaVal;
                    layerView.invalidate();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        alphaLayout.addView(seekBarAlpha);
        controlsLayout.addView(alphaLayout);

        // Панель истории и сохранения: Отменить / Повторить / PNG / JPG
        LinearLayout actionLayout = new LinearLayout(this);
        actionLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button btnUndo = new Button(this);
        btnUndo.setText("↩️ Отменить");
        btnUndo.setOnClickListener(v -> undoAction());
        actionLayout.addView(btnUndo, new LinearLayout.LayoutParams(0, 110, 0.25f));

        Button btnRedo = new Button(this);
        btnRedo.setText("🔄 Повторить");
        btnRedo.setOnClickListener(v -> redoAction());
        actionLayout.addView(btnRedo, new LinearLayout.LayoutParams(0, 110, 0.25f));

        Button btnSavePng = new Button(this);
        btnSavePng.setText("💾 PNG");
        btnSavePng.setOnClickListener(v -> saveComposedImage(Bitmap.CompressFormat.PNG));
        actionLayout.addView(btnSavePng, new LinearLayout.LayoutParams(0, 110, 0.25f));

        Button btnSaveJpg = new Button(this);
        btnSaveJpg.setText("💾 JPG");
        btnSaveJpg.setOnClickListener(v -> saveComposedImage(Bitmap.CompressFormat.JPEG));
        actionLayout.addView(btnSaveJpg, new LinearLayout.LayoutParams(0, 110, 0.25f));

        controlsLayout.addView(actionLayout);
        rootLayout.addView(controlsLayout);

        setContentView(rootLayout);

        checkPermissions();
        initOnnxEnvironment();
    }

    private void syncUIWithActiveLayer() {
        if (activeLayer == 1) {
            seekBarAlpha.setProgress((int)(fgState.alpha * 100));
            updateAngleDisplay(fgState.rotation);
        } else {
            seekBarAlpha.setProgress((int)(bgState.alpha * 100));
            updateAngleDisplay(bgState.rotation);
        }
    }

    private void updateAngleDisplay(float angle) {
        int rounded = Math.round(angle);
        if (rounded == 0) {
            tvAngleInfo.setText("");
        } else {
            tvAngleInfo.setText("📐 " + rounded + "°");
        }
    }

    private void saveStateToHistory() {
        HistorySnapshot snap = new HistorySnapshot();
        snap.fgBitmap = foregroundBitmap;
        snap.bgBitmap = backgroundBitmap;
        snap.fgS.set(fgState);
        snap.bgS.set(bgState);
        undoStack.add(snap);
        redoStack.clear(); // Очищаем redo при новом действии
        if (undoStack.size() > 20) undoStack.remove(0);
    }

    private void undoAction() {
        if (undoStack.isEmpty()) {
            Toast.MakedTextShort(this, "Нечего отменять");
            return;
        }
        HistorySnapshot currentSnap = new HistorySnapshot();
        currentSnap.fgBitmap = foregroundBitmap;
        currentSnap.bgBitmap = backgroundBitmap;
        currentSnap.fgS.set(fgState);
        currentSnap.bgS.set(bgState);
        redoStack.add(currentSnap);

        HistorySnapshot snap = undoStack.remove(undoStack.size() - 1);
        foregroundBitmap = snap.fgBitmap;
        backgroundBitmap = snap.bgBitmap;
        fgState.set(snap.fgS);
        bgState.set(snap.bgS);
        syncUIWithActiveLayer();
        layerView.invalidate();
        Toast.makeText(this, "Отменено", Toast.LENGTH_SHORT).show();
    }

    private void redoAction() {
        if (redoStack.isEmpty()) {
            Toast.makeText(this, "Нечего повторять", Toast.LENGTH_SHORT).show();
            return;
        }
        HistorySnapshot currentSnap = new HistorySnapshot();
        currentSnap.fgBitmap = foregroundBitmap;
        currentSnap.bgBitmap = backgroundBitmap;
        currentSnap.fgS.set(fgState);
        currentSnap.bgS.set(bgState);
        undoStack.add(currentSnap);

        HistorySnapshot snap = redoStack.remove(redoStack.size() - 1);
        foregroundBitmap = snap.fgBitmap;
        backgroundBitmap = snap.bgBitmap;
        fgState.set(snap.fgS);
        bgState.set(snap.bgS);
        syncUIWithActiveLayer();
        layerView.invalidate();
        Toast.makeText(this, "Повторено", Toast.LENGTH_SHORT).show();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_CODE);
        }
    }

    private void openGallery(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                saveStateToHistory();
                InputStream is = getContentResolver().openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                if (requestCode == PICK_FG_IMAGE) {
                    foregroundBitmap = bitmap;
                    fgState.scale = 1.0f;
                    fgState.x = 0; fgState.y = 0; fgState.rotation = 0; fgState.alpha = 1.0f;
                    activeLayer = 1;
                    Toast.makeText(this, "Передний план загружен", Toast.LENGTH_SHORT).show();
                } else if (requestCode == PICK_BG_IMAGE) {
                    backgroundBitmap = bitmap;
                    bgState.scale = 1.0f;
                    bgState.x = 0; bgState.y = 0; bgState.rotation = 0; bgState.alpha = 1.0f;
                    activeLayer = 0;
                    Toast.makeText(this, "Фон загружен", Toast.LENGTH_SHORT).show();
                }
                syncUIWithActiveLayer();
                layerView.invalidate();
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initOnnxEnvironment() {
        try {
            File modelFile = new File(getFilesDir(), "u2netp.onnx");
            if (!modelFile.exists()) {
                InputStream is = getAssets().open("u2netp.onnx");
                FileOutputStream fos = new FileOutputStream(modelFile);
                byte[] buffer = new byte[4096];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
                fos.close();
                is.close();
            }
            ortEnvironment = OrtEnvironment.getEnvironment();
            ortSession = ortEnvironment.createSession(modelFile.getAbsolutePath());
            Toast.makeText(this, "ИИ модель u2netp готова к работе!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка модели: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void removeBackgroundWithAI() {
        if (foregroundBitmap == null) {
            Toast.makeText(this, "Загрузите переднее изображение!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ortSession == null) {
            Toast.makeText(this, "ИИ не инициализирован", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Удаление фона нейросетью...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                int imgSize = 320;
                Bitmap resized = Bitmap.createScaledBitmap(foregroundBitmap, imgSize, imgSize, true);
                
                float[] inputArray = new float[1 * 3 * imgSize * imgSize];
                int pixelIndex = 0;
                for (int y = 0; y < imgSize; y++) {
                    for (int x = 0; x < imgSize; x++) {
                        int pixel = resized.getPixel(x, y);
                        float r = ((pixel >> 16) & 0xFF) / 255.0f;
                        float g = ((pixel >> 8) & 0xFF) / 255.0f;
                        float b = (pixel & 0xFF) / 255.0f;
                        
                        inputArray[pixelIndex] = (r - 0.485f) / 0.229f;
                        inputArray[pixelIndex + imgSize * imgSize] = (g - 0.456f) / 0.224f;
                        inputArray[pixelIndex + 2 * imgSize * imgSize] = (b - 0.406f) / 0.225f;
                        pixelIndex++;
                    }
                }

                long[] shape = {1, 3, imgSize, imgSize};
                OnnxTensor tensor = OnnxTensor.createTensor(ortEnvironment, FloatBuffer.wrap(inputArray), shape);
                Map<String, OnnxTensor> inputs = new HashMap<>();
                inputs.put(ortSession.getInputNames().iterator().next(), tensor);

                OrtSession.Result result = ortSession.run(inputs);
                OnnxTensor outputTensor = (OnnxTensor) result.get(0);
                float[][][][] rawOutput = (float[][][][]) outputTensor.getValue();

                Bitmap maskBitmap = Bitmap.createBitmap(imgSize, imgSize, Bitmap.Config.ARGB_8888);
                for (int y = 0; y < imgSize; y++) {
                    for (int x = 0; x < imgSize; x++) {
                        float val = rawOutput[0][0][y][x];
                        int alpha = (int) (Math.min(Math.max(val, 0.0f), 1.0f) * 255);
                        maskBitmap.setPixel(x, y, Color.argb(alpha, 255, 255, 255));
                    }
                }

                Bitmap finalResult = Bitmap.createBitmap(foregroundBitmap.getWidth(), foregroundBitmap.getHeight(), Bitmap.Config.ARGB_8888);
                Bitmap scaledMask = Bitmap.createScaledBitmap(maskBitmap, foregroundBitmap.getWidth(), foregroundBitmap.getHeight(), true);
                
                Canvas canvas = new Canvas(finalResult);
                Paint paint = new Paint();
                canvas.drawBitmap(foregroundBitmap, 0, 0, paint);
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
                canvas.drawBitmap(scaledMask, 0, 0, paint);

                runOnUiThread(() -> {
                    foregroundBitmap = finalResult;
                    layerView.invalidate();
                    Toast.makeText(this, "Фон успешно удален!", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Ошибка ИИ: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void saveComposedImage(Bitmap.CompressFormat format) {
        Bitmap composed = Bitmap.createBitmap(layerView.getWidth(), layerView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(composed);
        if (format == Bitmap.CompressFormat.JPEG) {
            canvas.drawColor(Color.WHITE);
        } else {
            canvas.drawColor(Color.TRANSPARENT);
        }
        
        layerView.drawLayers(canvas);

        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String ext = (format == Bitmap.CompressFormat.PNG) ? ".png" : ".jpg";
        File file = new File(dir, "Editor_" + System.currentTimeMillis() + ext);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            composed.compress(format, 100, fos);
            fos.flush();
            fos.close();
            Toast.makeText(this, "Сохранено: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка сохранения: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private class LayerView extends View {
        private float lastTouchX, lastTouchY;
        private final ScaleGestureDetector scaleDetector;
        private float lastFocusX, lastFocusY;
        private boolean isScalingOrRotating = false;

        // Переменные для расчета угла поворота двумя пальцами
        private float initialTouchAngle = 0f;
        private float startRotation = 0f;

        public LayerView(android.content.Context context) {
            super(context);
            scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    float scaleFactor = detector.getScaleFactor();
                    if (activeLayer == 1) {
                        fgState.scale *= scaleFactor;
                        fgState.scale = Math.max(0.1f, Math.min(fgState.scale, 10.0f));
                    } else {
                        bgState.scale *= scaleFactor;
                        bgState.scale = Math.max(0.1f, Math.min(bgState.scale, 10.0f));
                    }
                    invalidate();
                    return true;
                }
            });
        }

        public void drawLayers(Canvas canvas) {
            Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

            if (backgroundBitmap != null) {
                canvas.save();
                paint.setAlpha((int)(bgState.alpha * 255));
                canvas.translate(bgState.x, bgState.y);
                canvas.scale(bgState.scale, bgState.scale);
                canvas.rotate(bgState.rotation, backgroundBitmap.getWidth() / 2f, backgroundBitmap.getHeight() / 2f);
                canvas.drawBitmap(backgroundBitmap, 0, 0, paint);
                canvas.restore();
            }

            if (foregroundBitmap != null) {
                canvas.save();
                paint.setAlpha((int)(fgState.alpha * 255));
                canvas.translate(fgState.x, fgState.y);
                canvas.scale(fgState.scale, fgState.scale);
                canvas.rotate(fgState.rotation, foregroundBitmap.getWidth() / 2f, foregroundBitmap.getHeight() / 2f);
                canvas.drawBitmap(foregroundBitmap, 0, 0, paint);
                canvas.restore();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            drawLayers(canvas);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            scaleDetector.onTouchEvent(event);

            int action = event.getAction() & MotionEvent.ACTION_MASK;
            int pointerCount = event.getPointerCount();

            if (action == MotionEvent.ACTION_DOWN) {
                saveStateToHistory();
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                isScalingOrRotating = false;
            } else if (action == MotionEvent.ACTION_POINTER_DOWN && pointerCount == 2) {
                isScalingOrRotating = true;
                initialTouchAngle = getAngle(event);
                startRotation = (activeLayer == 1) ? fgState.rotation : bgState.rotation;
                lastFocusX = scaleDetector.getFocusX();
                lastFocusY = scaleDetector.getFocusY();
            } else if (action == MotionEvent.ACTION_MOVE) {
                if (pointerCount >= 2) {
                    // Мультитач: масштабирование и вращение двумя пальцами без скачков
                    float currentAngle = getAngle(event);
                    float deltaAngle = currentAngle - initialTouchAngle;
                    float newRotation = startRotation + deltaAngle;

                    float focusX = scaleDetector.getFocusX();
                    float focusY = scaleDetector.getFocusY();
                    float dxf = focusX - lastFocusX;
                    float dyf = focusY - lastFocusY;

                    if (activeLayer == 1) {
                        fgState.rotation = newRotation;
                        fgState.x += dxf;
                        fgState.y += dyf;
                        updateAngleDisplay(fgState.rotation);
                    } else {
                        bgState.rotation = newRotation;
                        bgState.x += dxf;
                        bgState.y += dyf;
                        updateAngleDisplay(bgState.rotation);
                    }

                    lastFocusX = focusX;
                    lastFocusY = focusY;
                    invalidate();
                } else if (!isScalingOrRotating && !scaleDetector.isInProgress()) {
                    // Обычное перемещение одним пальцем
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;
                    if (activeLayer == 1) {
                        fgState.x += dx;
                        fgState.y += dy;
                    } else {
                        bgState.x += dx;
                        bgState.y += dy;
                    }
                    invalidate();
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                }
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
                if (pointerCount < 2) {
                    isScalingOrRotating = false;
                }
                if (pointerCount == 1) {
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                }
            }
            return true;
        }

        private float getAngle(MotionEvent event) {
            float dx = event.getX(1) - event.getX(0);
            float dy = event.getY(1) - event.getY(0);
            return (float) Math.toDegrees(Math.atan2(dy, dx));
        }
    }

    private class FrameLayout extends android.widget.FrameLayout {
        public FrameLayout(android.content.Context context) {
            super(context);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (ortSession != null) ortSession.close();
            if (ortEnvironment != null) ortEnvironment.close();
        } catch (Exception ignored) {}
    }
}