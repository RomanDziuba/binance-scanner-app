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

        Button btnPickSingle = new Button(this);
        btnPickSingle.setText("📂 Выбрать фото из галереи");
        btnPickSingle.setTextColor(Color.WHITE);
        btnPickSingle.setTextSize(13f);
        btnPickSingle.setTypeface(Typeface.DEFAULT_BOLD);
        btnPickSingle.setBackgroundColor(Color.parseColor("#0D9488"));
        btnPickSingle.setPadding(20, 20, 20, 20);
        btnPickSingle.setOnClickListener(v -> openGalleryPicker());
        layout.addView(btnPickSingle);

        TextView modesTitle = new TextView(this);
        modesTitle.setText("\n👇 Режимы компьютерного зрения и графики:");
        modesTitle.setTextColor(Color.parseColor("#94A3B8"));
        modesTitle.setTextSize(14f);
        modesTitle.setPadding(0, 10, 0, 10);
        layout.addView(modesTitle);

        effectsLayout = new LinearLayout(this);
        effectsLayout.setOrientation(LinearLayout.VERTICAL);
        effectsLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        addEffectButton(effectsLayout, "✏️ Художественный Скетч (XDoG)", 0, "#2563EB");
        addEffectButton(effectsLayout, "💬 Стиль Комикс (Cel-Shading)", 1, "#7C3AED");
        addEffectButton(effectsLayout, "🎨 Масляная Живопись (Кувахара)", 2, "#B45309");
        addEffectButton(effectsLayout, "📜 Гравюра тушью (Woodcut)", 3, "#047857");
        addEffectButton(effectsLayout, "💧 Живая Акварель", 4, "#334155");
        addEffectButton(effectsLayout, "🧩 Художественный Витраж", 5, "#BE185D");
        addEffectButton(effectsLayout, "⚡ Неоновый Кибер-Арт", 6, "#4F46E5");

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
            statusTextView.setText("✅ Фото загружено! Выберите стиль обработки.");
            Toast.makeText(this, "Фото готово к трансформации!", Toast.LENGTH_SHORT).show();
        }
    }

    private void processPhotoWithStyle(int styleIndex, String styleName) {
        statusTextView.setText("Статус: 🔄 Рендеринг (" + styleName + ")...");
        
        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(selectedPhotoUri);
                Bitmap original = BitmapFactory.decodeStream(is);
                if (is != null) is.close();

                if (original != null) {
                    // Оптимальный размер для качественного компьютерного зрения
                    int maxDim = 700;
                    int w = original.getWidth();
                    int h = original.getHeight();
                    if (w > maxDim || h > maxDim) {
                        float ratio = Math.min((float) maxDim / w, (float) maxDim / h);
                        w = Math.round(w * ratio);
                        h = Math.round(h * ratio);
                        original = Bitmap.createScaledBitmap(original, w, h, true);
                    }

                    Bitmap artBitmap = applyVisionStyle(original, styleIndex);

                    ContentValues values = new ContentValues();
                    String fileName = "Art_" + System.currentTimeMillis() + ".jpg";
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MonsterArtDrawings");

                    Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    if (imageUri != null) {
                        try (OutputStream fos = getContentResolver().openOutputStream(imageUri)) {
                            if (fos != null) {
                                artBitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos);
                            }
                        }
                    }

                    runOnUiThread(() -> {
                        statusTextView.setText("Готово! Сохранено в Галерею (Pictures/MonsterArtDrawings)");
                        Toast.makeText(MainActivity.this, "🖼 Рисунок сохранен в галерею!", Toast.LENGTH_LONG).show();
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

    private Bitmap applyVisionStyle(Bitmap src, int type) {
        switch (type) {
            case 0: return visionPencilSketch(src);
            case 1: return visionComicCelShading(src);
            case 2: return visionOilKuwahara(src);
            case 3: return visionWoodcut(src);
            case 4: return visionWatercolor(src);
            case 5: return visionStainedGlass(src);
            case 6: return visionNeonCyber(src);
            default: return visionPencilSketch(src);
        }
    }

    // 1. НАСТОЯЩИЙ КАРАНДАШНЫЙ СКЕТЧ (Разница Гауссианов / DoG)
    private Bitmap visionPencilSketch(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);

        int[] gray = new int[w * h];
        for (int i = 0; i < pixels.length; i++) {
            gray[i] = (int)(0.299 * Color.red(pixels[i]) + 0.587 * Color.green(pixels[i]) + 0.114 * Color.blue(pixels[i]));
        }

        int[] blur1 = boxBlur(gray, w, h, 2);
        int[] blur2 = boxBlur(gray, w, h, 6);

        int[] result = new int[w * h];
        for (int i = 0; i < gray.length; i++) {
            int diff = blur1[i] - (int)(1.15 * blur2[i]);
            int val = diff > 0 ? 255 : Math.max(0, 255 + diff * 4);
            // Добавляем текстуру белой бумаги
            if (val > 240) val = 255;
            result[i] = Color.rgb(val, val, val);
        }

        Bitmap dest = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        dest.setPixels(result, 0, w, 0, 0, w, h);
        return dest;
    }

    // 2. СТИЛЬ КОМИКС (Cel-Shading + Четкий контур)
    private Bitmap visionComicCelShading(Bitmap src) {
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

                // Квантование цветов под палитру комикса (4 уровня на канал)
                r = ((r + 32) / 64) * 64;
                g = ((g + 32) / 64) * 64;
                b = ((b + 32) / 64) * 64;

                // Детекция краев для чернильной обводки
                if (x > 0 && y > 0) {
                    int pPrev = pixels[(y - 1) * w + (x - 1)];
                    int diff = Math.abs(r - Color.red(pPrev)) + Math.abs(g - Color.green(pPrev));
                    if (diff > 50) {
                        r = 10; g = 10; b = 10; // Черная линия комикса
                    }
                }
                result[idx] = Color.rgb(Math.min(255, r), Math.min(255, g), Math.min(255, b));
            }
        }
        Bitmap dest = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        dest.setPixels(result, 0, w, 0, 0, w, h);
        return dest;
    }

    // 3. МАСЛЯНАЯ ЖИВОПИСЬ (Фильтр Кувахары для кистевых мазков)
    private Bitmap visionOilKuwahara(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);
        int[] result = new int[w * h];
        int radius = 4;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int[] meanR = new int[4], meanG = new int[4], meanB = new int[4];
                int[] count = new int[4];
                long[] sumSqR = new long[4], sumSqG = new long[4], sumSqB = new long[4];

                // 4 квадранта для поиска однородности мазка
                int[][] xOffsets = {{-radius, 0}, {0, radius}, {-radius, 0}, {0, radius}};
                int[][] yOffsets = {{-radius, -radius}, {-radius, -radius}, {0, 0}, {0, 0}};

                for (int q = 0; q < 4; q++) {
                    for (int dy = yOffsets[q][0]; dy <= yOffsets[q][1]; dy++) {
                        for (int dx = xOffsets[q][0]; dx <= xOffsets[q][1]; dx++) {
                            int nx = x + dx, ny = y + dy;
                            if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
                                int p = pixels[ny * w + nx];
                                int red = Color.red(p), green = Color.green(p), blue = Color.blue(p);
                                meanR[q] += red; meanG[q] += green; meanB[q] += blue;
                                sumSqR[q] += red * red; sumSqG[q] += green * green; sumSqB[q] += blue * blue;
                                count[q]++;
                            }
                        }
                    }
                }

                int minVariance = Integer.MAX_VALUE;
                int bestQ = 0;
                for (int q = 0; q < 4; q++) {
                    if (count[q] == 0) continue;
                    long vr = sumSqR[q] - (long)meanR[q] * meanR[q] / count[q];
                    long vg = sumSqG[q] - (long)meanG[q] * meanG[q] / count[q];
                    long vb = sumSqB[q] - (long)meanB[q] * meanB[q] / count[q];
                    int variance = (int)(vr + vg + vb);
                    if (variance < minVariance) {
                        minVariance = variance;
                        bestQ = q;
                    }
                }

                int c = count[bestQ];
                if (c > 0) {
                    result[y * w + x] = Color.rgb(meanR[bestQ] / c, meanG[bestQ] / c, meanB[bestQ] / c);
                } else {
                    result[y * w + x] = pixels[y * w + x];
                }
            }
        }
        Bitmap dest = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        dest.setPixels(result, 0, w, 0, 0, w, h);
        return dest;
    }

    // 4. СТАРИННАЯ ГРАВЮРА (Woodcut)
    private Bitmap visionWoodcut(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);
        int[] result = new int[w * h];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = pixels[y * w + x];
                int lum = (int)(0.299 * Color.red(p) + 0.587 * Color.green(p) + 0.114 * Color.blue(p));
                // Имитация резца по дереву: диагональная штриховка
                if (((x + y) % 6) < (lum * 6 / 255)) {
                    result[y * w + x] = Color.rgb(240, 235, 220); // Светлые участки
                } else {
                    result[y * w + x] = Color.rgb(20, 20, 20);   // Черная резьба
                }
            }
        }
        Bitmap dest = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        dest.setPixels(result, 0, w, 0, 0, w, h);
        return dest;
    }

    // 5. ЖИВАЯ АКВАРЕЛЬ
    private Bitmap visionWatercolor(Bitmap src) {
        Bitmap oil = visionOilKuwahara(src);
        int w = oil.getWidth();
        int h = oil.getHeight();
        Bitmap dest = Bitmap.createScaledBitmap(oil, w / 2, h / 2, true);
        dest = Bitmap.createScaledBitmap(dest, w, h, true);
        
        Canvas canvas = new Canvas(dest);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix(new float[] {
            1.2f, 0, 0, 0, 10,
            0, 1.2f, 0, 0, 10,
            0, 0, 1.2f, 0, 10,
            0, 0, 0, 1, 0
        });
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(dest, 0, 0, paint);
        return dest;
    }

    // 6. ХУДОЖЕСТВЕННЫЙ ВИТРАЖ
    private Bitmap visionStainedGlass(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int cell = 12;
        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);
        int[] result = new int[w * h];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int cx = Math.min(w - 1, (x / cell) * cell + cell / 2);
                int cy = Math.min(h - 1, (y / cell) * cell + cell / 2);
                int baseColor = pixels[cy * w + cx];

                if (x % cell == 0 || y % cell == 0 || (x % cell == y % cell)) {
                    result[y * w + x] = Color.BLACK; // Свинцовый контур витража
                } else {
                    result[y * w + x] = baseColor;
                }
            }
        }
        Bitmap dest = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        dest.setPixels(result, 0, w, 0, 0, w, h);
        return dest;
    }

    // 7. НЕОНОВЫЙ КИБЕР-АРТ
    private Bitmap visionNeonCyber(Bitmap src) {
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
                
                if (edge > 25) {
                    result[idx] = Color.rgb(255, 0, 128); // Неоново-розовый контур
                } else {
                    int r = Color.red(p) / 4;
                    int g = Color.green(p) / 4;
                    int b = Color.blue(p) / 2 + 100;
                    result[idx] = Color.rgb(r, g, b); // Кибернетический синий фон
                }
            }
        }
        Bitmap dest = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        dest.setPixels(result, 0, w, 0, 0, w, h);
        return dest;
    }

    // Вспомогательный метод быстрого размытия для скетча
    private int[] boxBlur(int[] src, int w, int h, int r) {
        int[] dst = new int[src.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int sum = 0, count = 0;
                for (int dy = -r; dy <= r; dy++) {
                    for (int dx = -r; dx <= r; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
                            sum += src[ny * w + nx];
                            count++;
                        }
                    }
                }
                dst[y * w + x] = sum / count;
            }
        }
        return dst;
    }
}