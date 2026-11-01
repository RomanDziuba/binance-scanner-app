package com.fotoredaktor.version01;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.widget.*;
import android.graphics.*;
import android.view.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

// THEME: @android:style/Theme.Material.Light.NoActionBar
public class MainActivity extends Activity {

    private TextView statusTextView;
    private Button startBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Создаем простой и стильный интерфейс управления
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.parseColor("#030712"));
        layout.setPadding(40, 40, 40, 40);

        TextView title = new TextView(this);
        title.setText("🎨 Monster Photo Art Studio");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(22f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 20);
        layout.addView(title);

        statusTextView = new TextView(this);
        statusTextView.setText("Статус: Готов к превращению фото в картины 🚀");
        statusTextView.setTextColor(Color.parseColor("#10B981"));
        statusTextView.setTextSize(14f);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(0, 0, 0, 40);
        layout.addView(statusTextView);

        startBtn = new Button(this);
        startBtn.setText("🚀 Начать художественную обработку");
        startBtn.setTextColor(Color.WHITE);
        startBtn.setTextSize(14f);
        startBtn.setTypeface(Typeface.DEFAULT_BOLD);
        startBtn.setBackgroundColor(Color.parseColor("#2563EB"));
        startBtn.setPadding(30, 30, 30, 30);
        
        startBtn.setOnClickListener(v -> {
            startBtn.setEnabled(false);
            statusTextView.setText("Статус: 🔄 Сканирование папок и рисование...");
            new Thread(() -> {
                processAllPhonePhotosToArt();
            }).start();
        });
        
        layout.addView(startBtn);
        setContentView(layout);
    }

    // 1. Главный метод поиска всех фото и генерации рисунков
    private void processAllPhonePhotosToArt() {
        File rootDir = Environment.getExternalStorageDirectory();
        List<File> imageFiles = new ArrayList<>();
        scanFolderRecursive(rootDir, imageFiles);

        File outputDir = new File(Environment.getExternalStorageDirectory(), "MonsterArtDrawings");
        if (!outputDir.exists()) outputDir.mkdirs();

        int totalProcessed = 0;
        for (File imgFile : imageFiles) {
            try {
                Bitmap original = decodeSampledBitmapFromFile(imgFile.getAbsolutePath(), 800, 800);
                if (original != null) {
                    int styleIndex = totalProcessed % 7;
                    Bitmap artisticCopy = applyArtisticFilter(original, styleIndex);

                    File outFile = new File(outputDir, "Art_" + styleIndex + "_" + imgFile.getName());
                    FileOutputStream fos = new FileOutputStream(outFile);
                    artisticCopy.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                    fos.flush();
                    fos.close();
                    
                    totalProcessed++;
                }
            } catch (Exception ignored) {}
        }
        
        final int finalCount = totalProcessed;
        runOnUiThread(() -> {
            statusTextView.setText("Готово! Создано рисунков: " + finalCount + "\nПапка: MonsterArtDrawings");
            startBtn.setEnabled(true);
            Toast.makeText(MainActivity.this, "Обработка завершена!", Toast.LENGTH_LONG).show();
        });
    }

    // 2. Рекурсивный обход всех папок телефона в поисках картинок
    private void scanFolderRecursive(File dir, List<File> resultList) {
        if (dir == null || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                if (!f.getName().startsWith(".")) {
                    scanFolderRecursive(f, resultList);
                }
            } else {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")) {
                    resultList.add(f);
                }
            }
        }
    }

    // 3. Менеджер стилей (7 видов «рисования»)
    private Bitmap applyArtisticFilter(Bitmap src, int styleType) {
        switch (styleType) {
            case 0: return toPencilSketch(src);     // Карандашный набросок
            case 1: return toComicBook(src);        // Стиль комикс / Гравюра
            case 2: return toSepiaVintage(src);     // Художественная сепия / Ретро
            case 3: return toPosterize(src);        // Мультяшная постеризация
            case 4: return toNoirContrast(src);     // Жесткий нуар (черно-белый контур)
            case 5: return toPixelArt(src);         // Мозаичный пиксель-арт
            case 6: return toNeonCyberpunk(src);    // Неоновый киберпанк
            default: return toPencilSketch(src);
        }
    }

    // --- АЛГОРИТМЫ ФИЛЬТРОВ ---

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

    private Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}