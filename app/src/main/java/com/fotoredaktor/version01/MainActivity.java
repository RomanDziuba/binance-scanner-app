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
        modesTitle.setText("\n👇 Уникальные методы художественной обработки:");
        modesTitle.setTextColor(Color.parseColor("#94A3B8"));
        modesTitle.setTextSize(14f);
        modesTitle.setPadding(0, 10, 0, 10);
        layout.addView(modesTitle);

        // Панель с кнопками уникальных режимов
        effectsLayout = new LinearLayout(this);
        effectsLayout.setOrientation(LinearLayout.VERTICAL);
        effectsLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        addEffectButton(effectsLayout, "✏️ Настоящий Карандашный Скетч", 0, "#2563EB");
        addEffectButton(effectsLayout, "💬 Графический Комикс (Инкер)", 1, "#7C3AED");
        addEffectButton(effectsLayout, "🎨 Масляная Живопись кистью", 2, "#B45309");
        addEffectButton(effectsLayout, "📜 Старинная Гравюра (Вуудкут)", 3, "#047857");
        addEffectButton(effectsLayout, "💧 Художественная Акварель", 4, "#334155");
        addEffectButton(effectsLayout, "🧩 Мозаичный Витраж", 5, "#BE185D");
        addEffectButton(effectsLayout, "⚡ Неоновый Кибер-Чертеж", 6, "#4F46E5");

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
            statusTextView.setText("✅ Фото выбрано! Нажмите нужный художественный метод.");
            Toast.makeText(this, "Фото загружено в память!", Toast.LENGTH_SHORT).show();
        }
    }

    private void processPhotoWithStyle(int styleIndex, String styleName) {
        statusTextView.setText("Статус: 🔄 Применяется метод: " + styleName + "...");
        
        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(selectedPhotoUri);
                Bitmap original = BitmapFactory.decodeStream(is);
                if (is != null) is.close();

                if (original != null) {
                    // Ограничиваем размер для быстрой и стабильной обработки
                    int maxDim = 800;
                    int w = original.getWidth();
                    int h = original.getHeight();
                    if (w > maxDim || h > maxDim) {
                        float ratio = Math.min((float) maxDim / w, (float) maxDim / h);
                        w = Math.round(w * ratio);
                        h = Math.round(h * ratio);
                        original = Bitmap.createScaledBitmap(original, w, h, true);
                    }

                    Bitmap artBitmap = applyUniqueArtMethod(original, styleIndex);

                    // Сохранение напрямую в галерею телефона (Pictures/MonsterArtDrawings)
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
                        Toast.makeText(MainActivity.this, "🖼 Художественный рисунок сохранен в Галерею!", Toast.LENGTH_LONG).show();
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

    // Диспетчер уникальных методов рисования
    private Bitmap applyUniqueArtMethod(Bitmap src, int methodType) {
        switch (methodType) {
            case 0: return toPencilSketch(src);
            case 1: return toInkComic(src);
            case 2: return toOilPainting(src);
            case 3: return toWoodcut(src);
            case 4: return toWatercolor(src);
            case 5: return toStainedGlass(src);
            case 6: return toNeonBlueprint(src);
            default: return toPencilSketch(src);
        }
    }

    // МЕТОД 0: Настоящий Карандашный Скетч (Color Dodge алгоритм)
    private Bitmap toPencilSketch(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);

        int[] gray = new int[w * h];
        int[] inverted = new int[w * h];
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int g = (int)(0.299 * Color.red(p) + 0.587 * Color.green(p) + 0.114 * Color.blue(p));
            gray[i] = g;
            inverted[i] = 255 - g;
        }

        // Размытие инвертированного слоя (симуляция света)
        int radius = 3;
        int[] blurred = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int sum = 0, count = 0;
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
                            sum += inverted[ny * w + nx];
                            count++;
                        }
                    }
                }
                blurred[y * w + x] = sum / count;
            }
        }

        // Color Dodge смешивание
        int[] result = new int[w * h];
        for (int i = 0; i < gray.length; i++) {
            int top = gray[i];
            int bottom = blurred[i];
            int dodge = bottom >= 255 ? 255 : Math.min(255, (top * 256) / (255 - bottom));
            result[i] = Color.rgb(dodge, dodge, dodge);
        }

        Bitmap dest = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        dest.setPixels(result, 0, w, 0, 0, w, h);
        return dest;
    }

    // МЕТОД 1: Графический Комикс / Инкер (Контуры + Постеризация палитры)
    private Bitmap toInkComic(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);
        int[] result = new int[w * h];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                int p = pixels[idx];
                int r = Color.red(p);
                int g = Color.green(p);
                int b = Color.blue(p);

                r = (r / 64) * 85;
                g = (g / 64) * 85;
                b = (b / 64) * 85;

                if (x > 0 && y > 0) {
                    int pPrev = pixels[(y - 1) * w + (x - 1)];
                    if (Math.abs(r - Color.red(pPrev)) > 35) {
                        r = 0; g = 0; b = 0; // Черная тушь контура
                    }
                }
                result[idx] = Color.rgb(Math.min(255, r), Math.min(255, g), Math.min(255, b));
            }
        }
        Bitmap dest = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        dest.setPixels(result, 0, w, 0, 0, w, h);
        return dest;
    }

    // МЕТОД 2: Масляная Живопись кистью
    private Bitmap toOilPainting(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);
        int[] result = new int[w * h];
        int radius = 3;
        int levels = 8;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int[] intensityCount = new int[levels];
                int[] sumR = new int[levels], sumG = new int[levels], sumB = new int[levels];

                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
                            int p = pixels[ny * w + nx];
                            int r = Color.red(p), g = Color.green(p), b = Color.blue(p);
                            int curLevel = (r + g + b) / 3 * levels / 256;
                            if (curLevel >= levels) curLevel = levels - 1;
                            intensityCount[curLevel]++;
                            sumR[curLevel] += r; sumG[curLevel] += g; sumB[curLevel] += b;
                        }
                    }
                }

                int maxIndex = 0, maxCount = 0;
                for (int i = 0; i < levels; i++) {
                    if (intensityCount[i] > maxCount) { maxCount = intensityCount[i]; maxIndex = i; }
                }

                if (maxCount > 0) {
                    result[y * w + x] = Color.rgb(sumR[maxIndex] / maxCount, sumG[maxIndex] / maxCount, sumB[maxIndex] / maxCount);
                } else {
                    result[y * w + x] = pixels[y * w + x];
                }
            }
        }
        Bitmap dest = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        dest.setPixels(result, 0, w, 0, 0, w, h);
        return dest;
    }

    // МЕТОД 3: Старинная Гравюра (Вуудкут)
    private Bitmap toWoodcut(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);
        int[] result = new int[w * h];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = pixels[y * w + x];
                int lum = (int)(0.299 * Color.red(p) + 0.587 * Color.green(p) + 0.114 * Color.blue(p));
                if ((y % 4) < (lum * 4 / 255)) {
                    result[y * w + x] = Color.rgb(15, 15, 15);
                } else {
                    result[y * w + x] = Color.rgb(245, 240, 230);
                }
            }
        }
        Bitmap dest = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        dest.setPixels(result, 0, w, 0, 0, w, h);
        return dest;
    }

    // МЕТОД 4: Художественная Акварель
    private Bitmap toWatercolor(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap small = Bitmap.createScaledBitmap(src, w / 2, h / 2, true);
        Bitmap dest = Bitmap.createScaledBitmap(small, w, h, true);
        
        Canvas canvas = new Canvas(dest);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(1.5f); // Сочные акварельные цвета
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(dest, 0, 0, paint);
        return dest;
    }

    // МЕТОД 5: Мозаичный Витраж
    private Bitmap toStainedGlass(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int blockSize = 16;
        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);
        int[] result = new int[w * h];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int bx = Math.min(w - 1, (x / blockSize) * blockSize + blockSize / 2);
                int by = Math.min(h - 1, (y / blockSize) * blockSize + blockSize / 2);
                int centerColor = pixels[by * w + bx];
                
                if ((x % blockSize == 0) || (y % blockSize == 0)) {
                    result[y * w + x] = Color.BLACK; // Свинцовые перегородки витража
                } else {
                    result[y * w + x] = centerColor;
                }
            }
        }
        Bitmap dest = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        dest.setPixels(result, 0, w, 0, 0, w, h);
        return dest;
    }

    // МЕТОД 6: Неоновый Кибер-Чертеж
    private Bitmap toNeonBlueprint(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);
        int[] result = new int[w * h];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                int p = pixels[idx];
                int edge = 0;
                if (x > 0 && y > 0) {
                    edge = Math.abs(Color.red(p) - Color.red(pixels[(y - 1) * w + (x - 1)]));
                }
                
                if (edge > 30) {
                    result[idx] = Color.rgb(0, 255, 255); // Яркий неон
                } else {
                    result[idx] = Color.rgb(5, 10, 35);   // Темный чертежный фон
                }
            }
        }
        Bitmap dest = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        dest.setPixels(result, 0, w, 0, 0, w, h);
        return dest;
    }
}