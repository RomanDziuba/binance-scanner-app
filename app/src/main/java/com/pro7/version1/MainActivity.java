package com.pro7.version1;

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

    // Элементы управления для синхронизации
    private SeekBar seekBarAlpha, seekBarRotation;
    private Button btnSwitchLayer;

    // История для кнопки "Отменить" (Undo)
    private static class HistorySnapshot {
        Bitmap fgBitmap, bgBitmap;
        LayerState fgS = new LayerState();
        LayerState bgS = new LayerState();
    }
    private final List<HistorySnapshot> historyStack = new ArrayList<>();

    // ONNX Runtime
    private OrtEnvironment ortEnvironment;
    private OrtSession ortSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(16, 16, 16, 16);
        rootLayout.setBackgroundColor(Color.parseColor("#121212"));

        // Холст
        layerView = new LayerView(this);
        LinearLayout.LayoutParams canvasParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        layerView.setLayoutParams(canvasParams);
        rootLayout.addView(layerView);

        LinearLayout controlsLayout = new LinearLayout(this);
        controlsLayout.setOrientation(LinearLayout.VERTICAL);

        // Кнопки загрузки и ИИ
        Button btnLoadFg = new Button(this);
        btnLoadFg.setText("📂 Загрузить передний план");
        btnLoadFg.setOnClickListener(v -> openGallery(PICK_FG_IMAGE));
        controlsLayout.addView(btnLoadFg);

        Button btnRemoveBg = new Button(this);
        btnRemoveBg.setText("✨ Удалить фон (ИИ u2netp)");
        btnRemoveBg.setOnClickListener(v -> {
            saveStateToHistory();
            removeBackgroundWithAI();
        });
        controlsLayout.addView(btnRemoveBg);

        Button btnLoadBg = new Button(this);
        btnLoadBg.setText("🖼 Загрузить свой фон");
        btnLoadBg.setOnClickListener(v -> openGallery(PICK_BG_IMAGE));
        controlsLayout.addView(btnLoadBg);

        // Управление слоями (Переключение и Смена мест)
        LinearLayout rowLayout = new LinearLayout(this);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);

        btnSwitchLayer = new Button(this);
        btnSwitchLayer.setText("🔄 Активный: Передний");
        btnSwitchLayer.setOnClickListener(v -> {
            activeLayer = (activeLayer == 1) ? 0 : 1;
            btnSwitchLayer.setText(activeLayer == 1 ? "🔄 Активный: Передний" : "🔄 Активный: Фон");
            // Синхронизируем ползунки под выбранный слой
            syncUIWithActiveLayer();
            Toast.makeText(this, activeLayer == 1 ? "Выбран передний слой" : "Выбран слой фона", Toast.LENGTH_SHORT).show();
        });
        rowLayout.addView(btnSwitchLayer, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        Button btnSwapLayers = new Button(this);
        btnSwapLayers.setText("🔀 Поменять местами");
        btnSwapLayers.setOnClickListener(v -> {
            saveStateToHistory();
            // Меняем местами не только картинки, но и их состояния (позицию, масштаб, альфа, поворот)
            Bitmap tempBmp = foregroundBitmap;
            foregroundBitmap = backgroundBitmap;
            backgroundBitmap = tempBmp;

            LayerState tempState = new LayerState();
            tempState.set(fgState);
            fgState.set(bgState);
            bgState.set(tempState);

            layerView.invalidate();
            Toast.makeText(this, "Слои поменялись местами", Toast.LENGTH_SHORT).show();
        });
        rowLayout.addView(btnSwapLayers, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        controlsLayout.addView(rowLayout);

        // Кнопка удаления активного слоя
        Button btnDeleteLayer = new Button(this);
        btnDeleteLayer.setText("🗑 Удалить активный слой");
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
        controlsLayout.addView(btnDeleteLayer);

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

        // Ползунок поворота (-180 до 180 градусов)
        LinearLayout rotLayout = new LinearLayout(this);
        rotLayout.setOrientation(LinearLayout.HORIZONTAL);
        TextView lblRot = new TextView(this);
        lblRot.setText("Поворот: ");
        lblRot.setTextColor(Color.WHITE);
        rotLayout.addView(lblRot);

        seekBarRotation = new SeekBar(this);
        seekBarRotation.setMax(360);
        seekBarRotation.setProgress(180); // 180 соответствет 0 градусов
        seekBarRotation.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        seekBarRotation.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float rotVal = progress - 180.0f;
                    if (activeLayer == 1) fgState.rotation = rotVal;
                    else bgState.rotation = rotVal;
                    layerView.invalidate();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        rotLayout.addView(seekBarRotation);
        controlsLayout.addView(rotLayout);

        // Кнопки Отменить и Сохранить
        LinearLayout actionLayout = new LinearLayout(this);
        actionLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button btnUndo = new Button(this);
        btnUndo.setText("↩️ Отменить");
        btnUndo.setOnClickListener(v -> undoLastAction());
        actionLayout.addView(btnUndo, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        Button btnSavePng = new Button(this);
        btnSavePng.setText("💾 PNG");
        btnSavePng.setOnClickListener(v -> saveComposedImage(Bitmap.CompressFormat.PNG));
        actionLayout.addView(btnSavePng, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        Button btnSaveJpg = new Button(this);
        btnSaveJpg.setText("💾 JPG");
        btnSaveJpg.setOnClickListener(v -> saveComposedImage(Bitmap.CompressFormat.JPEG));
        actionLayout.addView(btnSaveJpg, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        controlsLayout.addView(actionLayout);
        rootLayout.addView(controlsLayout);

        setContentView(rootLayout);

        checkPermissions();
        initOnnxEnvironment();
    }

    private void syncUIWithActiveLayer() {
        if (activeLayer == 1) {
            seekBarAlpha.setProgress((int)(fgState.alpha * 100));
            seekBarRotation.setProgress((int)(fgState.rotation + 180));
        } else {
            seekBarAlpha.setProgress((int)(bgState.alpha * 100));
            seekBarRotation.setProgress((int)(bgState.rotation + 180));
        }
    }

    private void saveStateToHistory() {
        HistorySnapshot snap = new HistorySnapshot();
        snap.fgBitmap = foregroundBitmap;
        snap.bgBitmap = backgroundBitmap;
        snap.fgS.set(fgState);
        snap.bgS.set(bgState);
        historyStack.add(snap);
        if (historyStack.size() > 15) { // Ограничение памяти
            historyStack.remove(0);
        }
    }

    private void undoLastAction() {
        if (historyStack.isEmpty()) {
            Toast.makeText(this, "Нечего отменять", Toast.LENGTH_SHORT).show();
            return;
        }
        HistorySnapshot snap = historyStack.remove(historyStack.size() - 1);
        foregroundBitmap = snap.fgBitmap;
        backgroundBitmap = snap.bgBitmap;
        fgState.set(snap.fgS);
        bgState.set(snap.bgS);
        syncUIWithActiveLayer();
        layerView.invalidate();
        Toast.makeText(this, "Действие отменено", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(this, "Передний план загружен", Toast.LENGTH_SHORT).show();
                } else if (requestCode == PICK_BG_IMAGE) {
                    backgroundBitmap = bitmap;
                    bgState.scale = 1.0f;
                    bgState.x = 0; bgState.y = 0; bgState.rotation = 0; bgState.alpha = 1.0f;
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
                InputStream is = getAssets().open("u2netp.onnx"); // Копируем модель из активов
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
            canvas.drawColor(Color.WHITE); // Подложка для JPG
        } else {
            canvas.drawColor(Color.TRANSPARENT); // Прозрачность для PNG
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

            // Отрисовка фона
            if (backgroundBitmap != null) {
                canvas.save();
                paint.setAlpha((int)(bgState.alpha * 255));
                canvas.translate(bgState.x, bgState.y);
                canvas.scale(bgState.scale, bgState.scale);
                canvas.rotate(bgState.rotation, backgroundBitmap.getWidth() / 2f, backgroundBitmap.getHeight() / 2f);
                canvas.drawBitmap(backgroundBitmap, 0, 0, paint);
                canvas.restore();
            }

            // Отрисовка переднего плана
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
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    saveStateToHistory(); // Сохраняем состояние перед перемещением
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (!scaleDetector.isInProgress()) {
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
                    }
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    break;
            }
            return true;
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