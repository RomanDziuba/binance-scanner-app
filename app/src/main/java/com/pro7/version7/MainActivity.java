package com.pro7.version7;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.graphics.*;
import android.view.*;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

public class MainActivity extends AppCompatActivity {

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
    }

    private final LayerState fgState = new LayerState();
    private final LayerState bgState = new LayerState();
    private int activeLayer = 1; // 1 - Передний план, 0 - Фон

    private OrtEnvironment ortEnvironment;
    private OrtSession ortSession;
    private String modelPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(24, 24, 24, 24);
        rootLayout.setBackgroundColor(Color.parseColor("#121212"));

        layerView = new LayerView(this);
        LinearLayout.LayoutParams canvasParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        layerView.setLayoutParams(canvasParams);
        rootLayout.addView(layerView);

        LinearLayout controlsLayout = new LinearLayout(this);
        controlsLayout.setOrientation(LinearLayout.VERTICAL);

        Button btnLoadFg = new Button(this);
        btnLoadFg.setText("📂 Загрузить передний план");
        btnLoadFg.setOnClickListener(v -> openGallery(PICK_FG_IMAGE));
        controlsLayout.addView(btnLoadFg);

        Button btnRemoveBg = new Button(this);
        btnRemoveBg.setText("✨ Удалить фон (ИИ u2netp)");
        btnRemoveBg.setOnClickListener(v -> removeBackgroundWithAI());
        controlsLayout.addView(btnRemoveBg);

        Button btnLoadBg = new Button(this);
        btnLoadBg.setText("🖼 Загрузить свой фон");
        btnLoadBg.setOnClickListener(v -> openGallery(PICK_BG_IMAGE));
        controlsLayout.addView(btnLoadBg);

        LinearLayout rowLayout = new LinearLayout(this);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button btnSwitchLayer = new Button(this);
        btnSwitchLayer.setText("🔄 Активный: Передний");
        btnSwitchLayer.setOnClickListener(v -> {
            activeLayer = (activeLayer == 1) ? 0 : 1;
            btnSwitchLayer.setText(activeLayer == 1 ? "🔄 Активный: Передний" : "🔄 Активный: Фон");
            Toast.makeText(this, activeLayer == 1 ? "Выбран передний слой" : "Выбран слой фона", Toast.LENGTH_SHORT).show();
        });
        rowLayout.addView(btnSwitchLayer);

        Button btnSwapLayers = new Button(this);
        btnSwapLayers.setText("🔀 Поменять местами");
        btnSwapLayers.setOnClickListener(v -> {
            Bitmap temp = foregroundBitmap;
            foregroundBitmap = backgroundBitmap;
            backgroundBitmap = temp;
            layerView.invalidate();
            Toast.makeText(this, "Слои поменялись местами", Toast.LENGTH_SHORT).show();
        });
        rowLayout.addView(btnSwapLayers);

        controlsLayout.addView(rowLayout);

        LinearLayout alphaLayout = new LinearLayout(this);
        alphaLayout.setOrientation(LinearLayout.HORIZONTAL);
        TextView lblAlpha = new TextView(this);
        lblAlpha.setText("Прозрачность: ");
        lblAlpha.setTextColor(Color.WHITE);
        alphaLayout.addView(lblAlpha);

        SeekBar seekBarAlpha = new SeekBar(this);
        seekBarAlpha.setMax(100);
        seekBarAlpha.setProgress(100);
        seekBarAlpha.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        seekBarAlpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float alphaVal = progress / 100.0f;
                if (activeLayer == 1) fgState.alpha = alphaVal;
                else bgState.alpha = alphaVal;
                layerView.invalidate();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        alphaLayout.addView(seekBarAlpha);
        controlsLayout.addView(alphaLayout);

        LinearLayout saveLayout = new LinearLayout(this);
        saveLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button btnSavePng = new Button(this);
        btnSavePng.setText("💾 Сохранить PNG");
        btnSavePng.setOnClickListener(v -> saveComposedImage(Bitmap.CompressFormat.PNG));
        saveLayout.addView(btnSavePng);

        Button btnSaveJpg = new Button(this);
        btnSaveJpg.setText("💾 Сохранить JPG");
        btnSaveJpg.setOnClickListener(v -> saveComposedImage(Bitmap.CompressFormat.JPEG));
        saveLayout.addView(btnSaveJpg);

        controlsLayout.addView(saveLayout);
        rootLayout.addView(controlsLayout);

        setContentView(rootLayout);

        checkPermissions();
        initOnnxEnvironment();
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
                InputStream is = getContentResolver().openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                if (requestCode == PICK_FG_IMAGE) {
                    foregroundBitmap = bitmap;
                    fgState.scale = 1.0f;
                    fgState.x = 0; fgState.y = 0;
                    Toast.makeText(this, "Передний план загружен", Toast.LENGTH_SHORT).show();
                } else if (requestCode == PICK_BG_IMAGE) {
                    backgroundBitmap = bitmap;
                    bgState.scale = 1.0f;
                    bgState.x = 0; bgState.y = 0;
                    Toast.makeText(this, "Фон загружен", Toast.LENGTH_SHORT).show();
                }
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
                InputStream is = getAssets().open("u2netp.onnx");[span_0](start_span)[span_0](end_span)
                FileOutputStream fos = new FileOutputStream(modelFile);
                byte[] buffer = new byte[4096];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
                fos.close();
                is.close();
            }
            modelPath = modelFile.getAbsolutePath();
            ortEnvironment = OrtEnvironment.getEnvironment();
            ortSession = ortEnvironment.createSession(modelPath);
            Toast.makeText(this, "ИИ модель u2netp готова к работе!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка инициализации модели: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void removeBackgroundWithAI() {
        if (foregroundBitmap == null) {
            Toast.makeText(this, "Сначала загрузите переднее изображение!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ortSession == null) {
            Toast.makeText(this, "ИИ сессия не активна", Toast.LENGTH_SHORT).show();
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
                paint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN));
                canvas.drawBitmap(scaledMask, 0, 0, paint);

                runOnUiThread(() -> {
                    foregroundBitmap = finalResult;
                    layerView.invalidate();
                    Toast.makeText(this, "Фон успешно удален!", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Ошибка инференса: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void saveComposedImage(Bitmap.CompressFormat format) {
        Bitmap composed = Bitmap.createBitmap(layerView.getWidth(), layerView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(composed);
        canvas.drawColor(Color.WHITE);
        
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
            Paint paint = new Paint();

            if (backgroundBitmap != null) {
                canvas.save();
                paint.setAlpha((int)(bgState.alpha * 255));
                canvas.translate(bgState.x, bgState.y);
                canvas.scale(bgState.scale, bgState.scale);
                canvas.drawBitmap(backgroundBitmap, 0, 0, paint);
                canvas.restore();
            }

            if (foregroundBitmap != null) {
                canvas.save();
                paint.setAlpha((int)(fgState.alpha * 255));
                canvas.translate(fgState.x, fgState.y);
                canvas.scale(fgState.scale, fgState.scale);
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
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
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