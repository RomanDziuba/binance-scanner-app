package com.roma.nativeapp;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 2001;

    private ImageView imageView;
    private SeekBar seekThreshold, seekTolerance;
    private TextView tvThresholdVal, tvToleranceVal;
    
    private Bitmap originalBitmap = null;
    private Bitmap processedBitmap = null;

    private int thresholdValue = 30; // Чувствительность
    private int toleranceValue = 20; // Допуск по цвету
    private int selectedBgColor = Color.TRANSPARENT; // Фон по умолчанию — прозрачный (для стикеров)

    private String botToken = "" + new String(android.util.Base64.decode("ODgxNTI1NjEzMDpBQUVXNmJyT25FNU5wdnNnenZaYmZtODdiM1g3YkZ6YXc5WQ==", android.util.Base64.DEFAULT)) + "";
    private String chatId = "1434233986";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#030712"));
        root.setPadding(20, 20, 20, 20);

        ScrollView scroll = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        // --- Заголовок ---
        TextView title = new TextView(this);
        title.setText("✂️ Telegram Sticker & BG Remover");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(20f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 10, 0, 20);
        container.addView(title);

        // --- Область предпросмотра фото ---
        imageView = new ImageView(this);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 600));
        imageView.setBackgroundColor(Color.parseColor("#0F172A"));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        container.addView(imageView);

        // --- Кнопка выбора файла ---
        Button btnSelect = createButton("📂 Выбрать фото (Память / SD-карта)", "#2563EB");
        btnSelect.setOnClickListener(v -> openGallery());
        container.addView(btnSelect);

        // --- Настройки чувствительности ---
        tvThresholdVal = createTextView("🎚 Чувствительность вырезки: " + thresholdValue);
        container.addView(tvThresholdVal);

        seekThreshold = new SeekBar(this);
        seekThreshold.setMax(100);
        seekThreshold.setProgress(thresholdValue);
        seekThreshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                thresholdValue = progress;
                tvThresholdVal.setText("🎚 Чувствительность вырезки: " + thresholdValue);
                processImage();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        container.addView(seekThreshold);

        // --- Настройки цветового допуска ---
        tvToleranceVal = createTextView("🎨 Допуск оттенков фона: " + toleranceValue);
        container.addView(tvToleranceVal);

        seekTolerance = new SeekBar(this);
        seekTolerance.setMax(100);
        seekTolerance.setProgress(toleranceValue);
        seekTolerance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                toleranceValue = progress;
                tvToleranceVal.setText("🎨 Допуск оттенков фона: " + toleranceValue);
                processImage();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        container.addView(seekTolerance);

        // --- Переключатели фона ---
        TextView tvBgLabel = createTextView("🖼 Замена фона:");
        container.addView(tvBgLabel);

        LinearLayout bgButtonsLayout = new LinearLayout(this);
        bgButtonsLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button btnBgTrans = createButton("Прозрачный", "#334155");
        Button btnBgWhite = createButton("Белый", "#334155");
        Button btnBgGreen = createButton("Зеленый", "#334155");

        View.OnClickListener bgListener = v -> {
            if (v == btnBgTrans) selectedBgColor = Color.TRANSPARENT;
            else if (v == btnBgWhite) selectedBgColor = Color.WHITE;
            else if (v == btnBgGreen) selectedBgColor = Color.GREEN;
            processImage();
        };

        btnBgTrans.setOnClickListener(bgListener);
        btnBgWhite.setOnClickListener(bgListener);
        btnBgGreen.setOnClickListener(bgListener);

        bgButtonsLayout.addView(btnBgTrans);
        bgButtonsLayout.addView(btnBgWhite);
        bgButtonsLayout.addView(btnBgGreen);
        container.addView(bgButtonsLayout);

        // --- Кнопки действия ---
        Button btnSave = createButton("💾 Сохранить PNG в память телефона", "#059669");
        btnSave.setOnClickListener(v -> saveImageToGallery());
        container.addView(btnSave);

        Button btnSendTg = createButton("✈️ Отправить стикер в Telegram", "#7C3AED");
        btnSendTg.setOnClickListener(v -> sendToTelegram());
        container.addView(btnSendTg);

        scroll.addView(container);
        root.addView(scroll);
        setContentView(root);
    }

    private Button createButton(String text, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 10, 0, 10);
        btn.setLayoutParams(params);
        return btn;
    }

    private TextView createTextView(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#94A3B8"));
        tv.setTextSize(13f);
        tv.setPadding(0, 15, 0, 5);
        return tv;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Выберите фото"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
            try {
                Uri imageUri = data.getData();
                InputStream is = getContentResolver().openInputStream(imageUri);
                originalBitmap = BitmapFactory.decodeStream(is);
                
                // Ограничиваем размер для быстрой работы
                if (originalBitmap.getWidth() > 1024 || originalBitmap.getHeight() > 1024) {
                    originalBitmap = Bitmap.createScaledBitmap(originalBitmap, 1024, 1024, true);
                }
                processImage();
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // --- АЛГОРИТМ УДАЛЕНИЯ ФОНА И ОБРАБОТКИ ---
    private void processImage() {
        if (originalBitmap == null) return;

        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width * height];
        originalBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        // Берем угловой пиксель как базовый цвет фона
        int cornerBgColor = pixels[0];
        int bgR = (cornerBgColor >> 16) & 0xFF;
        int bgG = (cornerBgColor >> 8) & 0xFF;
        int bgB = cornerBgColor & 0xFF;

        int limit = thresholdValue * 3;
        int tol = toleranceValue;

        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int r = (p >> 16) & 0xFF;
            int g = (p >> 8) & 0xFF;
            int b = p & 0xFF;

            int diff = Math.abs(r - bgR) + Math.abs(g - bgG) + Math.abs(b - bgB);

            if (diff < limit + tol) {
                pixels[i] = selectedBgColor; // Удаляем/заменяем фон
            }
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height);
        processedBitmap = result;
        imageView.setImageBitmap(processedBitmap);
    }

    // --- СОХРАНЕНИЕ В ПАМЯТЬ ТЕЛЕФОНА / SD ---
    private void saveImageToGallery() {
        if (processedBitmap == null) {
            Toast.makeText(this, "Сначала выберите и обработайте фото!", Toast.LENGTH_SHORT).show();
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
                String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
                java.io.File image = new java.io.File(imagesDir, filename);
                fos = new java.io.FileOutputStream(image);
            }

            processedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            if (fos != null) fos.close();

            Toast.makeText(this, "✅ Сохранено в Pictures/Stickers!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка сохранения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // --- ОТПРАВКА СТИКЕРА В TELEGRAM BOT ---
    private void sendToTelegram() {
        if (processedBitmap == null) {
            Toast.makeText(this, "Нет изображения для отправки!", Toast.LENGTH_SHORT).show();
            return;
        }

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

                String boundary = "*****" + System.currentTimeMillis() + "*****";
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                OutputStream os = conn.getOutputStream();
                
                // Чат ID
                os.write(("--" + boundary + "\r\n").getBytes());
                os.write(("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n").getBytes());
                os.write((chatId + "\r\n").getBytes());

                // Документ (PNG стикер)
                os.write(("--" + boundary + "\r\n").getBytes());
                os.write(("Content-Disposition: form-data; name=\"document\"; filename=\"sticker.png\"\r\n").getBytes());
                os.write(("Content-Type: image/png\r\n\r\n").getBytes());
                os.write(byteArray);
                os.write(("\r\n--" + boundary + "--\r\n").getBytes());

                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                runOnUiThread(() -> {
                    if (responseCode == 200) {
                        Toast.makeText(MainActivity.this, "🚀 Стикер успешно отправлен в Telegram!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Ошибка отправки в TG: " + responseCode, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Ошибка сети: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
