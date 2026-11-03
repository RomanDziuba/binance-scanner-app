package com.fon3.version3;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
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

    private static final int PICK_IMAGE_1 = 1;
    private static final int PICK_IMAGE_2 = 2;
    private static final int PERMISSION_CODE = 100;

    private LayerView layerView;
    private Bitmap bitmap1 = null;
    private Bitmap bitmap2 = null;
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

    private final LayerState state1 = new LayerState();
    private final LayerState state2 = new LayerState();
    private int activeLayer = 1;

    private SeekBar seekBarAlpha, seekBarHalo, seekBarDensity, seekBarSmooth;

    private float[][][][] cachedRawOutput1 = null;
    private Bitmap sourceForAI1 = null;
    private float[][][][] cachedRawOutput2 = null;
    private Bitmap sourceForAI2 = null;

    private static class HistorySnapshot {
        Bitmap bmp1, bmp2;
        LayerState s1 = new LayerState();
        LayerState s2 = new LayerState();
        int activeLayer;
        float[][][][] raw1, raw2;
        Bitmap src1, src2;
    }
    private final List<HistorySnapshot> undoStack = new ArrayList<>();
    private final List<HistorySnapshot> redoStack = new ArrayList<>();

    private OrtEnvironment ortEnvironment;
    private OrtSession ortSession;

    private interface OnProgressChangedListener {
        void onProgress(int progress);
        void onStartTracking();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(8, 8, 8, 8);
        rootLayout.setBackgroundColor(Color.parseColor("#121212"));

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
        tvAngleInfo.setTextSize(14);
        tvAngleInfo.setPadding(12, 12, 12, 12);
        FrameLayout.LayoutParams tvParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        tvParams.gravity = Gravity.TOP | Gravity.END;
        tvAngleInfo.setLayoutParams(tvParams);
        container.addView(tvAngleInfo);

        rootLayout.addView(container);

        LinearLayout controlsLayout = new LinearLayout(this);
        controlsLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout loadRow = new LinearLayout(this);
        loadRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnLoad1 = new Button(this);
        btnLoad1.setText("📂 Фотка 1");
        btnLoad1.setPadding(4, 16, 4, 16);
        btnLoad1.setOnClickListener(v -> openGallery(PICK_IMAGE_1));
        loadRow.addView(btnLoad1, new LinearLayout.LayoutParams(0, 120, 0.5f));

        Button btnLoad2 = new Button(this);
        btnLoad2.setText("📂 Фотка 2");
        btnLoad2.setPadding(4, 16, 4, 16);
        btnLoad2.setOnClickListener(v -> openGallery(PICK_IMAGE_2));
        loadRow.addView(btnLoad2, new LinearLayout.LayoutParams(0, 120, 0.5f));

        controlsLayout.addView(loadRow);

        Button btnRemoveBg = new Button(this);
        btnRemoveBg.setText("✨ Удалить фон");
        btnRemoveBg.setPadding(12, 16, 12, 16);
        btnRemoveBg.setOnClickListener(v -> {
            saveStateToHistory();
            removeBackgroundWithAI();
        });
        controlsLayout.addView(btnRemoveBg, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 120));

        LinearLayout rowLayerLayout = new LinearLayout(this);
        rowLayerLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button btnSwapLayers = new Button(this);
        btnSwapLayers.setText("🔀 Поменять местами");
        btnSwapLayers.setPadding(8, 16, 8, 16);
        btnSwapLayers.setOnClickListener(v -> {
            saveStateToHistory();
            Bitmap tempBmp = bitmap1;
            bitmap1 = bitmap2;
            bitmap2 = tempBmp;

            LayerState tempState = new LayerState();
            tempState.set(state1);
            state1.set(state2);
            state2.set(tempState);

            float[][][][] tempRaw = cachedRawOutput1;
            cachedRawOutput1 = cachedRawOutput2;
            cachedRawOutput2 = tempRaw;

            Bitmap tempSrc = sourceForAI1;
            sourceForAI1 = sourceForAI2;
            sourceForAI2 = tempSrc;

            activeLayer = 1;
            syncUIWithActiveLayer();
            layerView.invalidate();
            Toast.makeText(this, "Слои поменялись местами. Верхний активен.", Toast.LENGTH_SHORT).show();
        });
        rowLayerLayout.addView(btnSwapLayers, new LinearLayout.LayoutParams(0, 120, 0.75f));

        Button btnDeleteLayer = new Button(this);
        btnDeleteLayer.setText("🗑 Уд.");
        btnDeleteLayer.setTextColor(Color.RED);
        btnDeleteLayer.setPadding(4, 16, 4, 16);
        btnDeleteLayer.setOnClickListener(v -> {
            saveStateToHistory();
            if (activeLayer == 1) {
                bitmap1 = null;
                cachedRawOutput1 = null;
                sourceForAI1 = null;
                Toast.makeText(this, "Фотка 1 удалена", Toast.LENGTH_SHORT).show();
            } else {
                bitmap2 = null;
                cachedRawOutput2 = null;
                sourceForAI2 = null;
                Toast.makeText(this, "Фотка 2 удалена", Toast.LENGTH_SHORT).show();
            }
            layerView.invalidate();
        });
        rowLayerLayout.addView(btnDeleteLayer, new LinearLayout.LayoutParams(0, 120, 0.25f));
        controlsLayout.addView(rowLayerLayout);

        // Широкие интерактивные ползунки
        controlsLayout.addView(createWideSeekBarLayout("Прозрачность:", seekBarAlpha = createInteractiveSeekBar(100, 100, new OnProgressChangedListener() {
            @Override public void onProgress(int progress) {
                float val = progress / 100.0f;
                if (activeLayer == 1) state1.alpha = val;
                else state2.alpha = val;
                layerView.invalidate();
            }
            @Override public void onStartTracking() { saveStateToHistory(); }
        })));

        controlsLayout.addView(createWideSeekBarLayout("Очистка ореолов (Мощная):", seekBarHalo = createInteractiveSeekBar(100, 50, new OnProgressChangedListener() {
            @Override public void onProgress(int progress) { applyInteractiveMaskSettings(); }
            @Override public void onStartTracking() { saveStateToHistory(); }
        })));

        controlsLayout.addView(createWideSeekBarLayout("Плотность краев:", seekBarDensity = createInteractiveSeekBar(100, 50, new OnProgressChangedListener() {
            @Override public void onProgress(int progress) { applyInteractiveMaskSettings(); }
            @Override public void onStartTracking() { saveStateToHistory(); }
        })));

        controlsLayout.addView(createWideSeekBarLayout("Сглаживание краев:", seekBarSmooth = createInteractiveSeekBar(100, 20, new OnProgressChangedListener() {
            @Override public void onProgress(int progress) { applyInteractiveMaskSettings(); }
            @Override public void onStartTracking() { saveStateToHistory(); }
        })));

        LinearLayout actionLayout = new LinearLayout(this);
        actionLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button btnUndo = new Button(this);
        btnUndo.setText("↩️ Отмена");
        btnUndo.setPadding(4, 16, 4, 16);
        btnUndo.setOnClickListener(v -> undoAction());
        actionLayout.addView(btnUndo, new LinearLayout.LayoutParams(0, 120, 0.25f));

        Button btnRedo = new Button(this);
        btnRedo.setText("🔄 Повтор");
        btnRedo.setPadding(4, 16, 4, 16);
        btnRedo.setOnClickListener(v -> redoAction());
        actionLayout.addView(btnRedo, new LinearLayout.LayoutParams(0, 120, 0.25f));

        Button btnSavePng = new Button(this);
        btnSavePng.setText("💾 PNG");
        btnSavePng.setPadding(4, 16, 4, 16);
        btnSavePng.setOnClickListener(v -> saveComposedImage(Bitmap.CompressFormat.PNG));
        actionLayout.addView(btnSavePng, new LinearLayout.LayoutParams(0, 120, 0.25f));

        Button btnSaveJpg = new Button(this);
        btnSaveJpg.setText("💾 JPG");
        btnSaveJpg.setPadding(4, 16, 4, 16);
        btnSaveJpg.setOnClickListener(v -> saveComposedImage(Bitmap.CompressFormat.JPEG));
        actionLayout.addView(btnSaveJpg, new LinearLayout.LayoutParams(0, 120, 0.25f));

        controlsLayout.addView(actionLayout);

        TextView tvFooter = new TextView(this);
        tvFooter.setText("Created by Dziuba Roman 2026");
        tvFooter.setTextColor(Color.GRAY);
        tvFooter.setTextSize(12);
        tvFooter.setGravity(Gravity.CENTER);
        tvFooter.setPadding(4, 8, 4, 4);
        controlsLayout.addView(tvFooter);

        rootLayout.addView(controlsLayout);
        setContentView(rootLayout);

        checkPermissions();
        initOnnxEnvironment();
    }

    private LinearLayout createWideSeekBarLayout(String labelText, SeekBar seekBar) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(4, 2, 4, 2);

        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextColor(Color.WHITE);
        label.setTextSize(13);
        layout.addView(label, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        seekBar.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.addView(seekBar);
        return layout;
    }

    private SeekBar createInteractiveSeekBar(int max, int initialProgress, OnProgressChangedListener listener) {
        SeekBar sb = new SeekBar(this);
        sb.setMax(max);
        sb.setProgress(initialProgress);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    listener.onProgress(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { listener.onStartTracking(); }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        return sb;
    }

    private void syncUIWithActiveLayer() {
        if (activeLayer == 1) {
            seekBarAlpha.setProgress((int)(state1.alpha * 100));
            updateAngleDisplay(state1.rotation);
        } else {
            seekBarAlpha.setProgress((int)(state2.alpha * 100));
            updateAngleDisplay(state2.rotation);
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
        snap.bmp1 = bitmap1;
        snap.bmp2 = bitmap2;
        snap.s1.set(state1);
        snap.s2.set(state2);
        snap.activeLayer = activeLayer;
        snap.raw1 = cachedRawOutput1;
        snap.raw2 = cachedRawOutput2;
        snap.src1 = sourceForAI1;
        snap.src2 = sourceForAI2;
        undoStack.add(snap);
        redoStack.clear();
        if (undoStack.size() > 20) undoStack.remove(0);
    }

    private void undoAction() {
        if (undoStack.isEmpty()) {
            Toast.makeText(this, "Нечего отменять", Toast.LENGTH_SHORT).show();
            return;
        }
        HistorySnapshot currentSnap = new HistorySnapshot();
        currentSnap.bmp1 = bitmap1;
        currentSnap.bmp2 = bitmap2;
        currentSnap.s1.set(state1);
        currentSnap.s2.set(state2);
        currentSnap.activeLayer = activeLayer;
        currentSnap.raw1 = cachedRawOutput1;
        currentSnap.raw2 = cachedRawOutput2;
        currentSnap.src1 = sourceForAI1;
        currentSnap.src2 = sourceForAI2;
        redoStack.add(currentSnap);

        HistorySnapshot snap = undoStack.remove(undoStack.size() - 1);
        bitmap1 = snap.bmp1;
        bitmap2 = snap.bmp2;
        state1.set(snap.s1);
        state2.set(snap.s2);
        activeLayer = snap.activeLayer;
        cachedRawOutput1 = snap.raw1;
        cachedRawOutput2 = snap.raw2;
        sourceForAI1 = snap.src1;
        sourceForAI2 = snap.src2;
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
        currentSnap.bmp1 = bitmap1;
        currentSnap.bmp2 = bitmap2;
        currentSnap.s1.set(state1);
        currentSnap.s2.set(state2);
        currentSnap.activeLayer = activeLayer;
        currentSnap.raw1 = cachedRawOutput1;
        currentSnap.raw2 = cachedRawOutput2;
        currentSnap.src1 = sourceForAI1;
        currentSnap.src2 = sourceForAI2;
        undoStack.add(currentSnap);

        HistorySnapshot snap = redoStack.remove(redoStack.size() - 1);
        bitmap1 = snap.bmp1;
        bitmap2 = snap.bmp2;
        state1.set(snap.s1);
        state2.set(snap.s2);
        activeLayer = snap.activeLayer;
        cachedRawOutput1 = snap.raw1;
        cachedRawOutput2 = snap.raw2;
        sourceForAI1 = snap.src1;
        sourceForAI2 = snap.src2;
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
                if (requestCode == PICK_IMAGE_1) {
                    bitmap1 = bitmap;
                    sourceForAI1 = bitmap;
                    cachedRawOutput1 = null;
                    state1.scale = 1.0f;
                    state1.x = 0; state1.y = 0; state1.rotation = 0; state1.alpha = 1.0f;
                    activeLayer = 1;
                    Toast.makeText(this, "Фотка 1 загружена и активна", Toast.LENGTH_SHORT).show();
                } else if (requestCode == PICK_IMAGE_2) {
                    bitmap2 = bitmap;
                    sourceForAI2 = bitmap;
                    cachedRawOutput2 = null;
                    state2.scale = 1.0f;
                    state2.x = 0; state2.y = 0; state2.rotation = 0; state2.alpha = 1.0f;
                    activeLayer = 2;
                    Toast.makeText(this, "Фотка 2 загружена и активна", Toast.LENGTH_SHORT).show();
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
        Bitmap targetBmp = (activeLayer == 1) ? bitmap1 : bitmap2;
        if (targetBmp == null) {
            Toast.makeText(this, "Сначала загрузите активное изображение!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ortSession == null) {
            Toast.makeText(this, "ИИ не инициализирован", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Анализ и мощное удаление фона...", Toast.LENGTH_SHORT).show();

        if (activeLayer == 1) sourceForAI1 = bitmap1;
        else sourceForAI2 = bitmap2;

        new Thread(() -> {
            try {
                int imgSize = 320;
                Bitmap resized = Bitmap.createScaledBitmap(targetBmp, imgSize, imgSize, true);
                
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

                if (activeLayer == 1) cachedRawOutput1 = rawOutput;
                else cachedRawOutput2 = rawOutput;

                Bitmap finalResult = generateMaskedBitmap(targetBmp, rawOutput);

                runOnUiThread(() -> {
                    if (activeLayer == 1) bitmap1 = finalResult;
                    else bitmap2 = finalResult;
                    layerView.invalidate();
                    Toast.makeText(this, "Фон очищен и сглажен!", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Ошибка ИИ: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void applyInteractiveMaskSettings() {
        Bitmap srcBmp = (activeLayer == 1) ? sourceForAI1 : sourceForAI2;
        float[][][][] raw = (activeLayer == 1) ? cachedRawOutput1 : cachedRawOutput2;

        if (srcBmp == null || raw == null) return;

        Bitmap finalResult = generateMaskedBitmap(srcBmp, raw);
        if (activeLayer == 1) bitmap1 = finalResult;
        else bitmap2 = finalResult;
        layerView.invalidate();
    }

    private Bitmap generateMaskedBitmap(Bitmap source, float[][][][] rawOutput) {
        int imgSize = 320;
        Bitmap maskBitmap = Bitmap.createBitmap(imgSize, imgSize, Bitmap.Config.ARGB_8888);

        float haloVal = seekBarHalo != null ? seekBarHalo.getProgress() / 100.0f : 0.5f;
        float densityVal = seekBarDensity != null ? seekBarDensity.getProgress() / 100.0f : 0.5f;
        float smoothVal = seekBarSmooth != null ? seekBarSmooth.getProgress() / 100.0f : 0.2f;

        float lowThreshold = haloVal * 0.85f; 
        float highThreshold = 1.0f - (densityVal * 0.7f);

        for (int y = 0; y < imgSize; y++) {
            for (int x = 0; x < imgSize; x++) {
                float val = rawOutput[0][0][y][x];

                if (val < lowThreshold) {
                    val = 0.0f;
                } else if (val > highThreshold) {
                    val = 1.0f;
                } else {
                    if (highThreshold > lowThreshold) {
                        val = (val - lowThreshold) / (highThreshold - lowThreshold);
                    }
                    val = (float) Math.pow(Math.max(0f, Math.min(1f, val)), 0.4f);
                }

                int alpha = (int) (val * 255);
                maskBitmap.setPixel(x, y, Color.argb(alpha, 255, 255, 255));
            }
        }

        Bitmap scaledMask = Bitmap.createScaledBitmap(maskBitmap, source.getWidth(), source.getHeight(), true);
        Bitmap finalResult = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(finalResult);
        
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        
        if (smoothVal > 0.01f) {
            float blurRadius = smoothVal * 30.0f; 
            paint.setMaskFilter(new BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL));
        }

        canvas.drawBitmap(source, 0, 0, new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawBitmap(scaledMask, 0, 0, paint);

        return finalResult;
    }

    private void saveComposedImage(Bitmap.CompressFormat format) {
        Bitmap composed = Bitmap.createBitmap(layerView.getWidth(), layerView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(composed);
        if (format == Bitmap.CompressFormat.JPEG) {
            canvas.drawColor(Color.WHITE);
        } else {
            canvas.drawColor(Color.TRANSPARENT);
        }
        
        layerView.drawLayers(canvas, true);

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

        private float initialTouchAngle = 0f;
        private float startRotation = 0f;

        public LayerView(android.content.Context context) {
            super(context);
            scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    float scaleFactor = detector.getScaleFactor();
                    float focusX = detector.getFocusX();
                    float focusY = detector.getFocusY();

                    if (activeLayer == 1 && bitmap1 != null) {
                        float oldScale = state1.scale;
                        state1.scale *= scaleFactor;
                        state1.scale = Math.max(0.1f, Math.min(state1.scale, 10.0f));
                        state1.x = focusX - (focusX - state1.x) * (state1.scale / oldScale);
                        state1.y = focusY - (focusY - state1.y) * (state1.scale / oldScale);
                    } else if (activeLayer == 2 && bitmap2 != null) {
                        float oldScale = state2.scale;
                        state2.scale *= scaleFactor;
                        state2.scale = Math.max(0.1f, Math.min(state2.scale, 10.0f));
                        state2.x = focusX - (focusX - state2.x) * (state2.scale / oldScale);
                        state2.y = focusY - (focusY - state2.y) * (state2.scale / oldScale);
                    }
                    invalidate();
                    return true;
                }
            });
        }

        public void drawLayers(Canvas canvas, boolean isSaving) {
            Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

            if (bitmap2 != null) {
                canvas.save();
                paint.setAlpha((int)(state2.alpha * 255));
                canvas.translate(state2.x, state2.y);
                canvas.scale(state2.scale, state2.scale);
                canvas.rotate(state2.rotation, bitmap2.getWidth() / 2f, bitmap2.getHeight() / 2f);
                canvas.drawBitmap(bitmap2, 0, 0, paint);
                canvas.restore();

                if (!isSaving && activeLayer == 2) {
                    drawSelectionBorder(canvas, bitmap2, state2);
                }
            }

            if (bitmap1 != null) {
                canvas.save();
                paint.setAlpha((int)(state1.alpha * 255));
                canvas.translate(state1.x, state1.y);
                canvas.scale(state1.scale, state1.scale);
                canvas.rotate(state1.rotation, bitmap1.getWidth() / 2f, bitmap1.getHeight() / 2f);
                canvas.drawBitmap(bitmap1, 0, 0, paint);
                canvas.restore();

                if (!isSaving && activeLayer == 1) {
                    drawSelectionBorder(canvas, bitmap1, state1);
                }
            }
        }

        private void drawSelectionBorder(Canvas canvas, Bitmap bmp, LayerState state) {
            canvas.save();
            canvas.translate(state.x, state.y);
            canvas.scale(state.scale, state.scale);
            canvas.rotate(state.rotation, bmp.getWidth() / 2f, bmp.getHeight() / 2f);

            Paint borderPaint = new Paint();
            borderPaint.setColor(Color.YELLOW);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(3f / state.scale);
            canvas.drawRect(0, 0, bmp.getWidth(), bmp.getHeight(), borderPaint);
            canvas.restore();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            drawLayers(canvas, false);
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

                float touchX = event.getX();
                float touchY = event.getY();
                if (isTouchInsideBitmap(touchX, touchY, bitmap2, state2) && !isTouchInsideBitmap(touchX, touchY, bitmap1, state1)) {
                    activeLayer = 2;
                    syncUIWithActiveLayer();
                    invalidate();
                } else if (isTouchInsideBitmap(touchX, touchY, bitmap1, state1)) {
                    activeLayer = 1;
                    syncUIWithActiveLayer();
                    invalidate();
                }

            } else if (action == MotionEvent.ACTION_POINTER_DOWN && pointerCount == 2) {
                isScalingOrRotating = true;
                initialTouchAngle = getAngle(event);
                startRotation = (activeLayer == 1) ? state1.rotation : state2.rotation;
                lastFocusX = scaleDetector.getFocusX();
                lastFocusY = scaleDetector.getFocusY();
            } else if (action == MotionEvent.ACTION_MOVE) {
                if (pointerCount >= 2) {
                    float currentAngle = getAngle(event);
                    float deltaAngle = currentAngle - initialTouchAngle;
                    float newRotation = startRotation + deltaAngle;

                    float focusX = scaleDetector.getFocusX();
                    float focusY = scaleDetector.getFocusY();
                    float dxf = focusX - lastFocusX;
                    float dyf = focusY - lastFocusY;

                    if (activeLayer == 1) {
                        state1.rotation = newRotation;
                        state1.x += dxf;
                        state1.y += dyf;
                        updateAngleDisplay(state1.rotation);
                    } else {
                        state2.rotation = newRotation;
                        state2.x += dxf;
                        state2.y += dyf;
                        updateAngleDisplay(state2.rotation);
                    }

                    lastFocusX = focusX;
                    lastFocusY = focusY;
                    invalidate();
                } else if (!isScalingOrRotating && !scaleDetector.isInProgress()) {
                    float dx = event.getX() - lastTouchX;
                    float dy = event.getY() - lastTouchY;
                    if (activeLayer == 1) {
                        state1.x += dx;
                        state1.y += dy;
                    } else {
                        state2.x += dx;
                        state2.y += dy;
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

        private boolean isTouchInsideBitmap(float tx, float ty, Bitmap bmp, LayerState state) {
            if (bmp == null) return false;
            float localX = (tx - state.x) / state.scale;
            float localY = (ty - state.y) / state.scale;
            return localX >= 0 && localX <= bmp.getWidth() && localY >= 0 && localY <= bmp.getHeight();
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