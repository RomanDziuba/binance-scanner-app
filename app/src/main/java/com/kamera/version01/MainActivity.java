package com.kamera.version01;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.graphics.*;
import android.view.*;

import android.graphics.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

// Добавь эти методы в свой класс MainActivity.java

// 1. Главный метод поиска всех фото и генерации рисунков
private void processAllPhonePhotosToArt() {
    File rootDir = android.os.Environment.getExternalStorageDirectory();
    List<File> imageFiles = new ArrayList<>();
    scanFolderRecursive(rootDir, imageFiles);

    // Создаем папку для результатов в памяти телефона
    File outputDir = new File(android.os.Environment.getExternalStorageDirectory(), "MonsterArtDrawings");
    if (!outputDir.exists()) outputDir.mkdirs();

    int totalProcessed = 0;
    for (File imgFile : imageFiles) {
        try {
            // Ограничиваем размер для избежания OutOfMemoryError на больших фото
            Bitmap original = decodeSampledBitmapFromFile(imgFile.getAbsolutePath(), 800, 800);
            if (original != null) {
                // Применяем разные стили (например, чередуем 7 стилей)
                int styleIndex = totalProcessed % 7;
                Bitmap artisticCopy = applyArtisticFilter(original, styleIndex);

                // Сохраняем нарисованную копию
                File outFile = new File(outputDir, "Art_" + styleIndex + "_" + imgFile.getName());
                FileOutputStream fos = new FileOutputStream(outFile);
                artisticCopy.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.flush();
                fos.close();
                
                totalProcessed++;
            }
        } catch (Exception ignored) {}
    }
    
    // Вывод уведомления через твой лог/статус
    if (statusTextView != null) {
        statusTextView.post(() -> statusTextView.setText("Готово! Обработано рисунков: " + totalProcessed));
    }
}

// 2. Рекурсивный обход всех папок телефона в поисках картинок
private void scanFolderRecursive(File dir, List<File> resultList) {
    if (dir == null || !dir.isDirectory()) return;
    File[] files = dir.listFiles();
    if (files == null) return;

    for (File f : files) {
        if (f.isDirectory()) {
            // Пропускаем скрытые системные папки для скорости
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

// Стиль 0: Карандашный скетч (обесцвечивание + инверсия + контраст)
private Bitmap toPencilSketch(Bitmap src) {
    int w = src.getWidth();
    int h = src.getHeight();
    Bitmap dest = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    
    Canvas canvas = new Canvas(dest);
    Paint paint = new Paint();
    
    ColorMatrix cm = new ColorMatrix();
    cm.setSaturation(0); // Черно-белое
    ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
    paint.setColorFilter(f);
    
    canvas.drawBitmap(src, 0, 0, paint);
    // Дополнительная обработка пикселей для эффекта штриховки
    return invertAndContrast(dest);
}

// Стиль 1: Комикс (выделение контуров и уменьшение палитры)
private Bitmap toComicBook(Bitmap src) {
    Bitmap bmp = toPosterize(src);
    int w = bmp.getWidth();
    int h = bmp.getHeight();
    Bitmap dest = Bitmap.createBitmap(w, h, bmp.getConfig());
    
    for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
            int pixel = bmp.getPixel(x, y);
            int r = Color.red(pixel);
            int g = Color.green(pixel);
            int b = Color.blue(pixel);
            // Усиление границ и сочных цветов
            r = Math.min(255, r * 3 / 2);
            g = Math.min(255, g * 3 / 2);
            b = Math.min(255, b * 3 / 2);
            dest.setPixel(x, y, Color.rgb(r, g, b));
        }
    }
    return dest;
}

// Стиль 2: Художественная сепия
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

// Стиль 3: Постеризация (эффект картины/анимации)
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

// Стиль 4: Жесткий нуар (высококонтрастная графика)
private Bitmap toNoirContrast(Bitmap src) {
    int w = src.getWidth();
    int h = src.getHeight();
    Bitmap dest = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
            int p = src.getPixel(x, y);
            int lum = (int)(0.299 * Color.red(p) + 0.587 * Color.green(p) + 0.114 * Color.blue(p));
            int val = lum > 120 ? 255 : 0; // Жесткий порог
            dest.setPixel(x, y, Color.rgb(val, val, val));
        }
    }
    return dest;
}

// Стиль 5: Пиксель-арт / Мозаика
private Bitmap toPixelArt(Bitmap src) {
    int pixelSize = 12; // Размер пиксельного блока
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

// Стиль 6: Неоновый киберпанк (инверсия каналов с ярким свечением)
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

// Вспомогательный метод для инверсии (для скетча)
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

// Безопасная загрузка картинок во избежание переполнения памяти
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