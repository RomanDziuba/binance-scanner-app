package com.fotoredaktor.version01;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

// THEME: @android:style/Theme.Material.Light.NoActionBar
public class MainActivity extends Activity {

    private static final int PICK_SINGLE_PHOTO = 2001;
    private TextView statusTextView;
    private Button btnPickSingle, btnScanAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.parseColor("#030712"));
        layout.setPadding(30, 30, 30, 30);

        // ✦ Красивый логотип автора ✦
        TextView logoView = new TextView(this);
        logoView.setText("✦ Created by Dziuba Roman 2026 ✦");
        logoView.setTextColor(Color.parseColor("#F59E0B"));
        logoView.setTextSize(15f);
        logoView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        logoView.setGravity(Gravity.CENTER);
        logoView.setPadding(0, 0, 0, 20);
        layout.addView(logoView);

        TextView title = new TextView(this);
        title.setText("🎨 Monster Photo Art Studio");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(20f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 20);
        layout.addView(title);

        statusTextView = new TextView(this);
        statusTextView.setText("Статус: Готов к работе 🚀");
        statusTextView.setTextColor(Color.parseColor("#10B981"));
        statusTextView.setTextSize(13f);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(0, 0, 0, 30);
        layout.addView(statusTextView);

        // Кнопка 1: Выбрать конкретное фото вручную из любой папки
        btnPickSingle = new Button(this);
        btnPickSingle.setText("📂 Выбрать фото вручную");
        btnPickSingle.setTextColor(Color.WHITE);
        btnPickSingle.setTextSize(13f);
        btnPickSingle.setTypeface(Typeface.DEFAULT_BOLD);
        btnPickSingle.setBackgroundColor(Color.parseColor("#0D9488"));
        btnPickSingle.setPadding(20, 20, 20, 20);
        btnPickSingle.setOnClickListener(v -> openGalleryPicker());
        layout.addView(btnPickSingle);

        // Отступ
        Space space = new Space(this);
        space.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20));
        layout.addView(space);

        // Кнопка 2: Автоматически обработать все фото через MediaStore
        btnScanAll = new Button(this);
        btnScanAll.setText("🚀 Обработать все фото телефона");
        btnScanAll.setTextColor(Color.WHITE);
        btnScanAll.setTextSize(13f);
        btnScanAll.setTypeface(Typeface.DEFAULT_BOLD);
        btnScanAll.setBackgroundColor(Color.parseColor("#2563EB"));
        btnScanAll.setPadding(20, 20, 20, 20);
        btnScanAll.setOnClickListener(v -> {
            btnScanAll.setEnabled(false);
            btnPickSingle.setEnabled(false);
            statusTextView.setText("Статус: 🔄 Сканирование через MediaStore...");
            new Thread(this::processAllPhotosViaMediaStore).start();
        });
        layout.addView(btnScanAll);

        setContentView(layout);
    }

    // Открытие системного файлового менеджера/галереи для выбора фото
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
            Uri uri = data.getData();
            statusTextView.setText("Статус: 🎨 Обработка выбранного фото...");
            new Thread(() -> {
                try {
                    InputStream is = getContentResolver().openInputStream(uri);
                    Bitmap original = BitmapFactory.decodeStream(is);
                    if (is != null) is.close();

                    if (original != null) {
                        // Применяем случайный художественный стиль
                        int styleIndex = (int)(Math.random() * 7);
                        Bitmap artBitmap = applyArtisticFilter(original, styleIndex);

                        // Сохраняем результат в папку MonsterArtDrawings
                        File outputDir = new File(Environment.getExternalStorageDirectory(), "MonsterArtDrawings");
                        if (!outputDir.exists()) outputDir.mkdirs();
                        
                        File outFile = new File(outputDir, "Art_Single_" + System.currentTimeMillis() + ".jpg");
                        FileOutputStream fos = new FileOutputStream(outFile);
                        artBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                        fos.flush();
                        fos.close();

                        runOnUiThread(() -> {
                            statusTextView.setText("Готово! Сохранено в: MonsterArtDrawings");
                            Toast.makeText(MainActivity.this, "Фото успешно перерисовано!", Toast.LENGTH_LONG).show();
                        });
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> statusTextView.setText("Ошибка обработки: " + e.getMessage()));
                }
            }).start();
        }
    }

    // Надежное сканирование всех фотографий устройства через MediaStore API
    private void processAllPhotosViaMediaStore() {
        File outputDir = new File(Environment.getExternalStorageDirectory(), "MonsterArtDrawings");
        if (!outputDir.exists()) outputDir.mkdirs();

        int processedCount = 0;
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media.DATA};

        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                do {
                    String filePath = cursor.getString(columnIdx);
                    if (filePath != null) {
                        File imgFile = new File(filePath);
                        if (imgFile.exists()) {
                            try {
                                Bitmap original = decodeSampledBitmapFromFile(imgFile.getAbsolutePath(), 800, 800);
                                if (original != null) {
                                    int styleIndex = processedCount % 7;
                                    Bitmap artisticCopy = applyArtisticFilter(original, styleIndex);

                                    File outFile = new File(outputDir, "Art_" + styleIndex + "_" + imgFile.getName());
                                    FileOutputStream fos = new FileOutputStream(outFile);
                                    artisticCopy.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                                    fos.flush();
                                    fos.close();
                                    
                                    processedCount++;
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        final int finalCount = processedCount;
        runOnUiThread(() -> {
            statusTextView.setText("Готово! Обработано рисунков: " + finalCount);
            btnScanAll.setEnabled(true);
            btnPickSingle.setEnabled(true);
            Toast.makeText(MainActivity.this, "Массовая обработка завершена!", Toast.LENGTH_LONG).show();
        });
    }

    // Менеджер художественных стилей (7 вариантов)
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