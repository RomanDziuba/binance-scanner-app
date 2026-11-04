package com.odegda.version01;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 101;
    
    private ImageView imageView;
    private TextView statusText;
    private EditText inputChanges;
    private TextView logConsole;
    private ScrollView logScrollView;
    private byte[] selectedImageBytes;
    
    private Handler handler = new Handler(Looper.getMainLooper());
    
    // Специализированная модель для закрашивания и смены элементов/одежды
    private final String targetInpaintingModel = "iCoMix Inpainting";

    private List<String> hordeKeys = new ArrayList<String>();
    
    private OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Пул ключей AI Horde
        hordeKeys.add("lJSv4pVAjwtzOYfVldIVxg");
        hordeKeys.add("OjbJD9mxPNcXGU7DfGQAXQ");
        hordeKeys.add("V0lhf56Xg982yORgPuZl6A");
        hordeKeys.add("ZGnWWME9ZIBRCWHUFGQbWg");
        hordeKeys.add("pww7e3-SPoteR7r794Mjng");
        hordeKeys.add("7IPa67lmBrwLPeWz5MGWbQ");
        hordeKeys.add("j7gxGfUrvncLoJDuVzrVWA");

        ScrollView mainScroll = new ScrollView(this);
        mainScroll.setBackgroundColor(Color.parseColor("#030712"));

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        rootLayout.setPadding(25, 25, 25, 25);

        TextView title = new TextView(this);
        title.setText("👔 Monster AI - Смена одежды (Inpainting)");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(17f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 15);
        rootLayout.addView(title);

        statusText = new TextView(this);
        statusText.setText("ℹ️ Выберите фото и опишите желаемую одежду");
        statusText.setTextColor(Color.parseColor("#94A3B8"));
        statusText.setTextSize(12f);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 0, 0, 15);
        rootLayout.addView(statusText);

        imageView = new ImageView(this);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 420
        );
        imgParams.setMargins(0, 0, 0, 15);
        imageView.setLayoutParams(imgParams);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setBackgroundColor(Color.parseColor("#0F172A"));
        rootLayout.addView(imageView);

        Button btnPick = createStyledButton("📁 Выбрать фото из галереи", "#2563EB");
        btnPick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });
        rootLayout.addView(btnPick);

        TextView labelChanges = new TextView(this);
        labelChanges.setText("✍️ Описание новой одежды (рус/укр):");
        labelChanges.setTextColor(Color.parseColor("#F8FAFC"));
        labelChanges.setTextSize(12f);
        labelChanges.setPadding(0, 10, 0, 5);
        rootLayout.addView(labelChanges);

        inputChanges = new EditText(this);
        inputChanges.setHint("Например: медицинский халат, вечернее платье, кожаная куртка...");
        inputChanges.setHintTextColor(Color.parseColor("#666666"));
        inputChanges.setTextColor(Color.parseColor("#000000"));
        inputChanges.setBackgroundColor(Color.parseColor("#FFFFFF"));
        inputChanges.setTextSize(13f);
        inputChanges.setPadding(16, 16, 16, 16);
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        editParams.setMargins(0, 0, 0, 15);
        inputChanges.setLayoutParams(editParams);
        rootLayout.addView(inputChanges);

        TextView activeModelTag = new TextView(this);
        activeModelTag.setText("🎯 Активная модель: " + targetInpaintingModel);
        activeModelTag.setTextColor(Color.parseColor("#10B981"));
        activeModelTag.setTextSize(13f);
        activeModelTag.setTypeface(Typeface.DEFAULT_BOLD);
        activeModelTag.setPadding(0, 5, 0, 15);
        rootLayout.addView(activeModelTag);

        Button btnProcess = createStyledButton("🚀 Заменить одежду", "#059669");
        btnProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startClothingGeneration();
            }
        });
        rootLayout.addView(btnProcess);

        TextView labelLogs = new TextView(this);
        labelLogs.setText("📋 Консоль отладки (выделение и прокрутка):");
        labelLogs.setTextColor(Color.parseColor("#38BDF8"));
        labelLogs.setTextSize(12f);
        labelLogs.setTypeface(Typeface.DEFAULT_BOLD);
        labelLogs.setPadding(0, 10, 0, 5);
        rootLayout.addView(labelLogs);

        logScrollView = new ScrollView(this);
        LinearLayout.LayoutParams logScrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 280
        );
        logScrollParams.setMargins(0, 0, 0, 10);
        logScrollView.setLayoutParams(logScrollParams);
        logScrollView.setBackgroundColor(Color.parseColor("#0B0F19"));

        logConsole = new TextView(this);
        logConsole.setText("⏳ Приложение готово. Использование модели Inpainting...\n");
        logConsole.setTextColor(Color.parseColor("#34D399"));
        logConsole.setTextSize(11f);
        logConsole.setTypeface(Typeface.MONOSPACE);
        logConsole.setPadding(15, 15, 15, 15);
        logConsole.setTextIsSelectable(true);
        logScrollView.addView(logConsole);
        rootLayout.addView(logScrollView);

        LinearLayout logButtonsLayout = new LinearLayout(this);
        logButtonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        logButtonsLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        Button btnCopyLogs = createStyledButton("📋 Копировать логи", "#475569");
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        );
        copyParams.setMargins(0, 0, 5, 0);
        btnCopyLogs.setLayoutParams(copyParams);
        btnCopyLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("MonsterAI Logs", logConsole.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, "✅ Логи скопированы!", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnClearLogs = createStyledButton("🧹 Очистить", "#EF4444");
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        );
        clearParams.setMargins(5, 0, 0, 0);
        btnClearLogs.setLayoutParams(clearParams);
        btnClearLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logConsole.setText("");
                appendLog("Консоль очищена.");
            }
        });

        logButtonsLayout.addView(btnCopyLogs);
        logButtonsLayout.addView(btnClearLogs);
        rootLayout.addView(logButtonsLayout);

        mainScroll.addView(rootLayout);
        setContentView(mainScroll);
        
        appendLog("Инициализация завершена. Активная модель: " + targetInpaintingModel);
    }

    private void appendLog(final String text) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                String timeStr = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                logConsole.append("[" + timeStr + "] " + text + "\n");
                logScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private Button createStyledButton(String text, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(13f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        btn.setPadding(20, 16, 20, 16);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 10);
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
            Uri uri = data.getData();
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] temp = new byte[4096];
                int nRead;
                while ((nRead = is.read(temp, 0, temp.length)) != -1) {
                    buffer.write(temp, 0, nRead);
                }
                buffer.flush();
                selectedImageBytes = buffer.toByteArray();
                if (is != null) is.close();

                Bitmap bmp = BitmapFactory.decodeByteArray(selectedImageBytes, 0, selectedImageBytes.length);
                imageView.setImageBitmap(bmp);
                statusText.setText("✅ Фото загружено!");
                appendLog("Загружено изображение (" + selectedImageBytes.length + " байт)");
            } catch (Exception e) {
                appendLog("❌ Ошибка загрузки: " + e.getMessage());
                Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private Bitmap makeSquareBitmap(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        if (width == height) {
            return source;
        }
        int maxDim = Math.max(width, height);
        if (maxDim > 1024) {
            float scale = 1024f / maxDim;
            int newW = (int)(width * scale);
            int newH = (int)(height * scale);
            source = Bitmap.createScaledBitmap(source, newW, newH, true);
            width = source.getWidth();
            height = source.getHeight();
            maxDim = 1024;
        }

        Bitmap squareBitmap = Bitmap.createBitmap(maxDim, maxDim, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(squareBitmap);
        canvas.drawColor(Color.BLACK);
        
        int left = (maxDim - width) / 2;
        int top = (maxDim - height) / 2;
        canvas.drawBitmap(source, left, top, null);
        return squareBitmap;
    }

    private String translateToEnglish(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        try {
            String encoded = URLEncoder.encode(text, "UTF-8");
            String url = "https://api.mymemory.translated.net/get?q=" + encoded + "&langpair=auto|en";
            Request request = new Request.Builder().url(url).get().build();
            Response response = httpClient.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                JSONObject json = new JSONObject(response.body().string());
                JSONObject respData = json.optJSONObject("responseData");
                if (respData != null) {
                    String translated = respData.optString("translatedText", "");
                    if (!translated.isEmpty() && !translated.toUpperCase().contains("INVALID")) {
                        appendLog("Перевод одежды: '" + text + "' -> '" + translated + "'");
                        return translated;
                    }
                }
            }
        } catch (Exception e) {
            appendLog("⚠️ Ошибка перевода: " + e.getMessage());
        }
        return text;
    }

    private List<String> getPrioritizedWorkingKeys() {
        List<KeyScore> workingKeys = new ArrayList<KeyScore>();
        appendLog("Проверка ключей AI Horde...");
        for (int i = 0; i < hordeKeys.size(); i++) {
            String key = hordeKeys.get(i);
            try {
                Request request = new Request.Builder()
                        .url("https://stablehorde.net/api/v2/find_user")
                        .addHeader("apikey", key)
                        .addHeader("Client-Agent", "MonsterAI:1.0:android")
                        .get()
                        .build();
                Response response = httpClient.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    JSONObject json = new JSONObject(response.body().string());
                    double kudos = json.optDouble("kudos", 0);
                    workingKeys.add(new KeyScore(key, kudos));
                    appendLog("✅ Ключ #" + (i+1) + " Активен (Kudos: " + (int)kudos + ")");
                } else {
                    appendLog("❌ Ключ #" + (i+1) + " отклонен");
                }
            } catch (Exception e) {
                appendLog("⚠️ Сбой связи с ключом");
            }
        }
        
        if (workingKeys.isEmpty()) {
            for (int i = 0; i < hordeKeys.size(); i++) {
                workingKeys.add(new KeyScore(hordeKeys.get(i), 100.0));
            }
        }

        Collections.sort(workingKeys, new Comparator<KeyScore>() {
            @Override
            public int compare(KeyScore a, KeyScore b) {
                return Double.compare(b.kudos, a.kudos);
            }
        });

        List<String> sortedKeys = new ArrayList<String>();
        for (int i = 0; i < workingKeys.size(); i++) {
            sortedKeys.add(workingKeys.get(i).key);
        }
        return sortedKeys;
    }

    private static class KeyScore {
        String key;
        double kudos;
        KeyScore(String key, double kudos) {
            this.key = key;
            this.kudos = kudos;
        }
    }

    private void startClothingGeneration() {
        if (selectedImageBytes == null) {
            Toast.makeText(this, "⚠️ Выберите фото!", Toast.LENGTH_SHORT).show();
            return;
        }

        final String userClothingRaw = inputChanges.getText().toString().trim();

        final ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("⏳ Смена одежды");
        progress.setMessage("Подготовка и отправка...");
        progress.setCancelable(false);
        progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    appendLog("--- СТАРТ ГЕНЕРАЦИИ ОДЕЖДЫ ---");
                    
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progress.setMessage("Перевод текста одежды...");
                        }
                    });
                    String translatedClothing = translateToEnglish(userClothingRaw);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progress.setMessage("Проверка доступности ключей...");
                        }
                    });
                    List<String> workingKeys = getPrioritizedWorkingKeys();

                    Bitmap originalBmp = BitmapFactory.decodeByteArray(selectedImageBytes, 0, selectedImageBytes.length);
                    Bitmap squareBmp = makeSquareBitmap(originalBmp);

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    squareBmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    String base64Image = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);

                    // Базовый промпт, нацеленный именно на качественную детальную одежду и сохранение лица
                    String clothingCore = "photorealistic clothing replacement, detailed outfit, realistic fabric texture, perfect fit, professional fashion rendering, high detail, sharp focus, 8k resolution";
                    if (!translatedClothing.isEmpty()) {
                        clothingCore += ", " + translatedClothing;
                    }
                    
                    String negativePrompt = "(distorted face:1.5, bad anatomy, deformed eyes, extra limbs, bad hands, blurry, low resolution, bad quality)";
                    String fullPrompt = clothingCore + " ### " + negativePrompt;

                    // Параметры настройки для Inpainting
                    JSONObject paramsObj = new JSONObject();
                    paramsObj.put("sampler_name", "k_dpmpp_2m");
                    paramsObj.put("cfg_scale", 7.5);
                    paramsObj.put("steps", 25);
                    paramsObj.put("denoising_strength", 0.55); // Оптимальная сила для смены одежды без ломки форм

                    byte[] resultBytes = null;
                    boolean successGenerated = false;

                    for (int kIdx = 0; kIdx < workingKeys.size(); kIdx++) {
                        if (successGenerated) break;
                        String apiKey = workingKeys.get(kIdx);

                        try {
                            appendLog("👉 Запрос к модели: [" + targetInpaintingModel + "]");
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progress.setMessage("Генерация через " + targetInpaintingModel);
                                }
                            });

                            JSONArray modelsArray = new JSONArray();
                            modelsArray.put(targetInpaintingModel);

                            JSONObject payload = new JSONObject();
                            payload.put("prompt", fullPrompt);
                            payload.put("params", paramsObj);
                            payload.put("nsfw", true);
                            payload.put("censor_nsfw", false);
                            payload.put("source_image", base64Image);
                            payload.put("source_processing", "inpainting"); // РЕЖИМ ИНПЕЙНТИНГА
                            payload.put("models", modelsArray);

                            RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json; charset=utf-8"));
                            Request request = new Request.Builder()
                                    .url("https://stablehorde.net/api/v2/generate/async")
                                    .addHeader("apikey", apiKey)
                                    .addHeader("accept", "application/json")
                                    .addHeader("Content-Type", "application/json")
                                    .addHeader("Client-Agent", "MonsterAI:1.0:android")
                                    .post(body)
                                    .build();

                            Response response = httpClient.newCall(request).execute();
                            int code = response.code();
                            
                            if (code == 401 || code == 402 || code == 403 || code == 429) {
                                appendLog("⚠️ Ключ отклонен (HTTP " + code + "), пробуем другой.");
                                continue;
                            }
                            if (code == 400) {
                                String errBody = response.body() != null ? response.body().string() : "";
                                appendLog("⚠️ Модель отклонила параметр: " + errBody);
                                break;
                            }
                            if (!response.isSuccessful() || response.body() == null) {
                                appendLog("⚠️ Ошибка сети: HTTP " + code);
                                continue;
                            }

                            String respStr = response.body().string();
                            JSONObject respJson = new JSONObject(respStr);
                            String jobId = respJson.optString("id", null);
                            if (jobId == null) {
                                continue;
                            }

                            appendLog("✅ Задача принята! ID: " + jobId + ". Ожидаем рендер...");
                            String statusUrl = "https://stablehorde.net/api/v2/generate/status/" + jobId;
                            boolean jobDone = false;
                            String imgUrl = null;

                            for (int attempt = 0; attempt < 120; attempt++) {
                                Thread.sleep(5000);
                                Request statusReq = new Request.Builder()
                                        .url(statusUrl)
                                        .addHeader("Client-Agent", "MonsterAI:1.0:android")
                                        .get()
                                        .build();
                                Response statRespReal = httpClient.newCall(statusReq).execute();
                                if (statRespReal.isSuccessful() && statRespReal.body() != null) {
                                    JSONObject stJson = new JSONObject(statRespReal.body().string());
                                    if (stJson.optBoolean("faulted", false)) {
                                        appendLog("❌ Задача сброшена воркером.");
                                        jobDone = true;
                                        break;
                                    }
                                    if (stJson.optBoolean("done", false)) {
                                        JSONArray gens = stJson.optJSONArray("generations");
                                        if (gens != null && gens.length() > 0) {
                                            imgUrl = gens.getJSONObject(0).optString("img", null);
                                        }
                                        jobDone = true;
                                        appendLog("🎉 Рендеринг одежды завершен!");
                                        break;
                                    } else {
                                        final int qPos = stJson.optInt("queue_position", 0);
                                        final int waitTime = stJson.optInt("wait_time", 0);
                                        appendLog("⏳ Очередь: " + qPos + ", Ждать: " + waitTime + "с");
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                progress.setMessage("Позиция в очереди: " + qPos);
                                            }
                                        });
                                    }
                                }
                            }

                            if (jobDone && imgUrl != null && !imgUrl.isEmpty()) {
                                appendLog("📥 Загрузка готового фото...");
                                Request imgReq = new Request.Builder().url(imgUrl).get().build();
                                Response imgResp = httpClient.newCall(imgReq).execute();
                                if (imgResp.isSuccessful() && imgResp.body() != null) {
                                    resultBytes = imgResp.body().bytes();
                                    successGenerated = true;
                                    appendLog("✅ Получено успешно (" + resultBytes.length + " байт)!");
                                    break;
                                }
                            }
                        } catch (Exception ex) {
                            appendLog("🔴 Ошибка выполнения: " + ex.getMessage());
                        }
                    }

                    if (!successGenerated || resultBytes == null) {
                        throw new RuntimeException("Не удалось получить результат от модели.");
                    }

                    final Bitmap finalBmp = BitmapFactory.decodeByteArray(resultBytes, 0, resultBytes.length);
                    saveBitmapToGallery(finalBmp, "MonsterAI_Cloth_" + System.currentTimeMillis() + ".jpg");

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progress.dismiss();
                            imageView.setImageBitmap(finalBmp);
                            statusText.setText("✅ Одежда успешно изменена!");
                            Toast.makeText(MainActivity.this, "Сохранено в Pictures/MonsterAI", Toast.LENGTH_LONG).show();
                            appendLog("✨ Готово! Изображение сохранено в галерею.");
                        }
                    });

                } catch (final Exception e) {
                    appendLog("❌ ОШИБКА: " + e.getMessage());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progress.dismiss();
                            statusText.setText("❌ Ошибка: " + e.getMessage());
                            Toast.makeText(MainActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
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
                    appendLog("Сохранено в файл: " + fileName);
                }
            }
        } catch (Exception e) {
            appendLog("❌ Ошибка записи файлов: " + e.getMessage());
        }
    }
}