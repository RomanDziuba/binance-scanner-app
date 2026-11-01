package com.fotoredaktor.version01;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.InputStream;
import java.io.OutputStream;

// THEME: @android:style/Theme.Material.Light.NoActionBar
public class MainActivity extends Activity {

    private static final int PICK_SINGLE_PHOTO = 2001;
    private TextView statusTextView;
    private Uri selectedPhotoUri = null;
    private LinearLayout effectsLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView mainScroll = new ScrollView(this);
        mainScroll.setBackgroundColor(Color.parseColor("#030712"));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(30, 30, 30, 30);

        // ✦ Красивый логотип автора ✦
        TextView logoView = new TextView(this);
        logoView.setText("✦ Created by Dziuba Roman 2026 ✦");
        logoView.setTextColor(Color.parseColor("#F59E0B"));
        logoView.setTextSize(15f);
        logoView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        logoView.setGravity(Gravity.CENTER);
        logoView.setPadding(0, 0, 0, 15);
        layout.addView(logoView);

        TextView title = new TextView(this);
        title.setText("🎨 Monster Photo Art Studio");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(20f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 15);
        layout.addView(title);

        statusTextView = new TextView(this);
        statusTextView.setText("Статус: Сначала выберите фото 📂");
        statusTextView.setTextColor(Color.parseColor("#10B981"));
        statusTextView.setTextSize(13f);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(0, 0, 0, 20);
        layout.addView(statusTextView);

        // Кнопка выбора фото
        Button btnPickSingle = new Button(this);
        btnPickSingle.setText("📂 Выбрать фото из галереи");
        btnPickSingle.setTextColor(Color.WHITE);
        btnPickSingle.setTextSize(13f);
        btnPickSingle.setTypeface(Typeface.DEFAULT_BOLD);
        btnPickSingle.setBackgroundColor(Color.parseColor("#0D9488"));
        btnPickSingle.setPadding(20, 20, 20, 20);
        btnPickSingle.setOnClickListener(v -> openGalleryPicker());
        layout.addView(btnPickSingle);

        // Заголовок секции режимов
        TextView modesTitle = new TextView(this);
        modesTitle.setText("\n👇 Выберите режим обработки:");
        modesTitle.setTextColor(Color.parseColor("#94A3B8"));
        modesTitle.setTextSize(14f);
        modesTitle.setPadding(0, 10, 0, 10);
        layout.addView(modesTitle);

        // Панель с кнопками всех 7 режимов
        effectsLayout = new LinearLayout(this);
        effectsLayout.setOrientation(LinearLayout.VERTICAL);
        effectsLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        // Добавляем кнопки для каждого режима
        addEffectButton(effectsLayout, "✏️ Карандашный скетч", 0, "#2563EB");
        addEffectButton(effectsLayout, "💬 Стиль комикс", 1, "#7C3AED");
        addEffectButton(effectsLayout, "📜 Ретро-сепия", 2, "#B45309");
        addEffectButton(effectsLayout, "🎨 Постеризация (Мультяшный)", 3, "#047857");
        addEffectButton(effectsLayout, "🎞 Жесткий нуар", 4, "#334155");
        addEffectButton(effectsLayout, "🧩 Пиксель-арт (Мозаика)", 5, "#BE185D");
        addEffectButton(effectsLayout, "⚡ Неоновый киберпанк", 6, "#4F46E5");

        layout.addView(effectsLayout);
        mainScroll.addView(layout);
        setContentView(mainScroll);
    }

    private void addEffectButton(LinearLayout parent, String text, int styleIndex, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(12f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        btn.setPadding(15, 15, 15, 15);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 6, 0, 6);
        btn.setLayoutParams(params);

        btn.setOnClickListener(v -> {
            if (selectedPhotoUri == null) {
                Toast.makeText(this, "⚠️ Сначала выберите фото кнопкой выше!", Toast.LENGTH_SHORT).show();
                statusTextView.setText("Ошибка: Фото не выбрано!");
                return;
            }
            processPhotoWithStyle(styleIndex, text);
        });

        parent.addView(btn);
    }

    private void openGalleryPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Выберите фотографию"), PICK_SINGLE_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PICK_SINGLE_PHOTO && data != null && data.getData() != null) {
            selectedPhotoUri = data.getData();
            statusTextView.setText("✅ Фото выбрано! Нажмите нужный режим ниже.");
            Toast.makeText(this, "Фото загружено в память!", Toast.LENGTH_SHORT).show();
        }
    }

    private void processPhotoWithStyle(int styleIndex, String styleName) {
        statusTextView.setText("Статус: 🔄 Применяется " + styleName + "...");
        
        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(selectedPhotoUri);
                Bitmap original = BitmapFactory.decodeStream(is);
                if (is != null) is.close();

                if (original != null) {
                    Bitmap artBitmap = applyArtisticFilter(original, styleIndex);

                    // Сохранение напрямую в публичную галерею (Pictures/MonsterArtDrawings)
                    ContentValues values = new ContentValues();
                    String fileName = "Art_" + System.currentTimeMillis() + ".jpg";
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MonsterArtDrawings");

                    Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    if (imageUri != null) {
                        try (OutputStream fos = getContentResolver().openOutputStream(imageUri)) {
                            if (fos != null) {
                                artBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                            }
                        }
                    }

                    runOnUiThread(() -> {
                        statusTextView.setText("Готово! Сохранено в Галерею (Pictures/MonsterArtDrawings)");
                        Toast.makeText(MainActivity.this, "🖼 Рисунок сохранен в Галерею!", Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    statusTextView.setText("Ошибка: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // Менеджер художественных стилей (7 режимов)
    private Bitmap applyArtisticFilter(Bitmap src, int styleType) {
        switch (styleType) {
            case 0: return toPencilSketch(src);
            case 1: return toComicBook(src);
            case 2: return toSepiaVintage(src);
            case 3: return toPosterize(src);
            case 4: return toNoirContrast(src);
            case 5: return toPixelArt(src);
            case 6: return toNeonCyberpunk(src);
            default: return toPencilSketch(src);
        }
    }

    private Bitmap toPencilSketch(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap dest = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(dest);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(src, 0, 0, paint);
        return invertAndContrast(dest);
    }

    private Bitmap toComicBook(Bitmap src) {
        Bitmap bmp = toPosterize(src);
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        Bitmap dest = Bitmap.createBitmap(w, h, bmp.getConfig());
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = bmp.getPixel(x, y);
                int r = Math.min(255, Color.red(pixel) * 3 / 2);
                int g = Math.min(255, Color.green(pixel) * 3 / 2);
                int b = Math.min(255, Color.blue(pixel) * 3 / 2);
                dest.setPixel(x, y, Color.rgb(r, g, b));
            }
        }
        return dest;
    }

    private Bitmap toSepiaVintage(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap dest = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(dest);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix(new float[] {
            0.393f, 0.769f, 0.189f, 0, 0,
            0.349f, 0.686f, 0.168f, 0, 0,
            0.272f, 0.534f, 0.131f, 0, 0,
            0, 0, 0, 1, 0
        });
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(src, 0, 0, paint);
        return dest;
    }

    private Bitmap toPosterize(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap dest = Bitmap.createBitmap(w, h, src.getConfig());
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = src.getPixel(x, y);
                int r = (Color.red(p) / 64) * 64;
                int g = (Color.green(p) / 64) * 64;
                int b = (Color.blue(p) / 64) * 64;
                dest.setPixel(x, y, Color.rgb(r, g, b));
            }
        }
        return dest;
    }

    private Bitmap toNoirContrast(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap dest = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = src.getPixel(x, y);
                int lum = (int)(0.299 * Color.red(p) + 0.587 * Color.green(p) + 0.114 * Color.blue(p));
                int val = lum > 120 ? 255 : 0;
                dest.setPixel(x, y, Color.rgb(val, val, val));
            }
        }
        return dest;
    }

    private Bitmap toPixelArt(Bitmap src) {
        int pixelSize = 12;
        int w = src.getWidth();
        int h.getHeight(); // wait, h is int, h is height
        int h = src.getHeight();
        Bitmap dest = Bitmap.createBitmap(w, h, src.getConfig());
        for (int y = 0; y < h; y += pixelSize) {
            for (int x = 0; x < w; x += pixelSize) {
                int color = src.getPixel(Math.min(x, w - 1), Math.min(y, h - 1));
                for (int dy = 0; dy < pixelSize && (y + dy) < h; dy++) {
                    for (int dx = 0; dx < pixelSize && (x + dx) < w; dx++) {
                        dest.setPixel(x + dx, y + dy, color);
                    }
                }
            }
        }
        return dest;
    }

    private Bitmap toNeonCyberpunk(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap dest = Bitmap.createBitmap(w, h, src.getConfig());
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = src.getPixel(x, y);
                int r = 255 - Color.blue(p);
                int g = 255 - Color.red(p);
                int b = 255 - Color.green(p);
                dest.setPixel(x, y, Color.rgb(r, g, b));
            }
        }
        return dest;
    }

    private Bitmap invertAndContrast(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap dest = Bitmap.createBitmap(w, h, src.getConfig());
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = src.getPixel(x, y);
                int r = 255 - Color.red(p);
                int g = 255 - Color.green(p);
                int b = 255 - Color.blue(p);
                dest.setPixel(x, y, Color.rgb(r, g, b));
            }
        }
        return dest;
    }
}