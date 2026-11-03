package com.roman.version3;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

// THEME: @android:style/Theme.Material.Light.NoActionBar
public class MainActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 101;
    
    private ImageView imageView;
    private TextView statusText;
    private Uri selectedImageUri;
    private Bitmap currentBitmap;
    private Handler handler = new Handler(Looper.getMainLooper());
    
    // OkHttp клиент уже встроен в зависимости вашего компилятора!
    private OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Главный интерфейс приложения
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));
        rootLayout.setPadding(30, 30, 30, 30);

        // Заголовок
        TextView title = new TextView(this);
        title.setText("🎨 Monster AI Photo Studio");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(18f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 20);
        rootLayout.addView(title);

        // Информационный статус
        statusText = new TextView(this);
        statusText.setText("ℹ️ Выберите фото из галереи телефона");
        statusText.setTextColor(Color.parseColor("#94A3B8"));
        statusText.setTextSize(12f);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 0, 0, 20);
        rootLayout.addView(statusText);

        // Область предпросмотра фото
        imageView = new ImageView(this);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 650
        );
        imgParams.setMargins(0, 0, 0, 20);
        imageView.setLayoutParams(imgParams);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setBackgroundColor(Color.parseColor("#0F172A"));
        rootLayout.addView(imageView);

        // Кнопка выбора фото из галереи
        Button btnPick = createStyledButton("📁 Выбрать фото из галереи", "#2563EB");
        btnPick.setOnClickListener(v -> openGallery());
        rootLayout.addView(btnPick);

        // Кнопка запуска обработки и сохранения в галерею
        Button btnProcess = createStyledButton("✨ Обработать и сохранить в галерею", "#059669");
        btnProcess.setOnClickListener(v -> processAndSavePhoto());
        rootLayout.addView(btnProcess);

        setContentView(rootLayout);
    }

    private Button createStyledButton(String text, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(14f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        btn.setPadding(20, 20, 20, 20);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 15);
        btn.setLayoutParams(params);
        return btn;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Выберите фото"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            try {
                InputStream is = getContentResolver().openInputStream(selectedImageUri);
                currentBitmap = BitmapFactory.decodeStream(is);
                if (is != null) is.close();
                
                imageView.setImageBitmap(currentBitmap);
                statusText.setText("✅ Фото загружено! Нажмите кнопку обработки.");
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void processAndSavePhoto() {
        if (currentBitmap == null) {
            Toast.makeText(this, "⚠️ Сначала выберите фото из галереи!", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("🎨 Обработка изображения");
        progress.setMessage("Применение нейросети / фильтров...");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            try {
                // 💡 Здесь задействуется ваша логика обработки 
                // (можно использовать OkHttp для запроса к AI Horde или локальный ONNX Runtime)
                Thread.sleep(2000); // Имитация работы алгоритма

                // Пример результата (здесь можно подставить обработанный Bitmap)
                Bitmap processedBitmap = currentBitmap.copy(currentBitmap.getConfig(), true);

                // Автоматическое сохранение готового фото в галерею телефона (в папку Pictures/MonsterAI)
                saveBitmapToGallery(processedBitmap, "MonsterAI_" + System.currentTimeMillis() + ".jpg");

                handler.post(() -> {
                    progress.dismiss();
                    imageView.setImageBitmap(processedBitmap);
                    statusText.setText("✅ Готово! Фото сохранено в галерею телефона.");
                    Toast.makeText(MainActivity.this, "Фото успешно сохранено в галерею!", Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                handler.post(() -> {
                    progress.dismiss();
                    statusText.setText("❌ Ошибка: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void saveBitmapToGallery(Bitmap bitmap, String fileName) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MonsterAI");

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                }
            }
        } catch (Exception ignored) {}
    }
}