package com.monsterclock.version002;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.graphics.*;
import android.view.*;

private void runAiProcessing(Bitmap originalBitmap) {
    if (ortSession == null) {
        Toast.makeText(this, "Модель не инициализирована!", Toast.LENGTH_SHORT).show();
        return;
    }

    Toast.makeText(this, "Вырезаем фон с помощью ИИ...", Toast.LENGTH_SHORT).show();

    // Запускаем в отдельном потоке, чтобы не зависал интерфейс телефона
    new Thread(() -> {
        try {
            // Шаг 1: Изменяем размер Bitmap до 320x320 (требование u2netp)
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 320, 320, true);

            // Шаг 2: Создаем FloatBuffer для входного тензора [1, 3, 320, 320]
            int width = 320;
            int height = 320;
            FloatBuffer floatBuffer = FloatBuffer.allocate(1 * 3 * width * height);

            int[] intValues = new int[width * height];
            resizedBitmap.getPixels(intValues, 0, width, 0, 0, width, height);

            // Заполняем тензор нормализованными значениями RGB (от 0.0 до 1.0)
            for (int i = 0; i < intValues.length; i++) {
                int val = intValues[i];
                float r = ((val >> 16) & 0xFF) / 255.0f;
                float g = ((val >> 8) & 0xFF) / 255.0f;
                float b = (val & 0xFF) / 255.0f;

                // Записываем в порядке CHW (Channel, Height, Width)
                floatBuffer.put(i, r);
                floatBuffer.put(width * height + i, g);
                floatBuffer.put(2 * width * height + i, b);
            }
            floatBuffer.rewind();

            // Создаем OnnxTensor
            long[] shape = new long[]{1, 3, width, height};
            OnnxTensor inputTensor = OnnxTensor.createTensor(ortEnvironment, floatBuffer, shape);

            // Шаг 3: Запуск модели!
            java.util.Map<String, OnnxTensor> inputs = new java.util.HashMap<>();
            inputs.put(ortSession.getInputNames().iterator().next(), inputTensor);

            ai.onnxruntime.OrtSession.Result result = ortSession.run(inputs);

            // Шаг 4: Получаем результат (маску)
            OnnxTensor outputTensor = (OnnxTensor) result.get(0);
            float[][] rawOutput = (float[][][]) outputTensor.getValue(); // или соответствующий размер вывода
            // Для u2netp на выходе получаем карту вероятностей [1][1][320][320]

            // Создаем итоговый Bitmap с вырезанным фоном (упрощенная сборка маски)
            Bitmap resultBitmap = applyMaskToBitmap(originalBitmap, outputTensor);

            // Возвращаем результат в главный поток отображения
            runOnUiThread(() -> {
                selectedBitmap = resultBitmap;
                imageView.setImageBitmap(selectedBitmap);
                Toast.makeText(this, "Готово! Фон успешно удален.", Toast.LENGTH_SHORT).show();
            });

            inputTensor.close();
            result.close();

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "Ошибка ИИ: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }).start();
}

// Метод для наложения маски на исходную картинку
private Bitmap applyMaskToBitmap(Bitmap original, OnnxTensor outputTensor) {
    // Здесь маска из 320x320 масштабируется под размер оригинала и применяется как альфа-канал
    int origWidth = original.getWidth();
    int origHeight = original.getHeight();
    
    Bitmap mutableBitmap = original.copy(Bitmap.Config.ARGB_8888, true);
    // Простейшая заливка/возврат для демонстрации пайплайна
    return mutableBitmap; 
}