package com.roman.version1;

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
import android.util.Base64;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

// THEME: @android:style/Theme.Material.Light.NoActionBar
public class MainActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 101;
    
    private ImageView imageView;
    private TextView statusText;
    private Uri selectedImageUri;
    private byte[] selectedImageBytes;
    private Handler handler = new Handler(Looper.getMainLooper());
    
    // --- ПУЛ КЛЮЧЕЙ AI HORDE (из вашего скрипта) ---
    private List<String> hordeKeys = new ArrayList<String>() {{
        add("lJSv4pVAjwtzOYfVldIVxg");
        add("OjbJD9mxPNcXGU7DfGQAXQ");
        add("V0lhf56Xg982yORgPuZl6A");
        add("ZGnWWME9ZIBRCWHUFGQbWg");
        add("pww7e3-SPoteR7r794Mjng");
        add("7IPa67lmBrwLPeWz5MGWbQ");
        add("j7gxGfUrvncLoJDuVzrVWA");
    }};
    
    // OkHttp клиент с расширенными таймаутами для работы с ИИ-серверами
    private OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));
        rootLayout.setPadding(30, 30, 30, 30);

        TextView title = new TextView(this);
        title.setText("🔥 Monster AI Horde Engine Ultra");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(18f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 15);
        rootLayout.addView(title);

        statusText = new TextView(this);
        statusText.setText("ℹ️ Выберите фото из галереи. Авто-проверка ключей активна.");
        statusText.setTextColor(Color.parseColor("#94A3B8"));
        statusText.setTextSize(12f);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 0, 0, 15);
        rootLayout.addView(statusText);

        imageView = new ImageView(this);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 600
        );
        imgParams.setMargins(0, 0, 0, 15);
        imageView.setLayoutParams(imgParams);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setBackgroundColor(Color.parseColor("#0F172A"));
        rootLayout.addView(imageView);

        Button btnPick = createStyledButton("📁 Выбрать фото из галереи", "#2563EB");
        btnPick.setOnClickListener(v -> openGallery());
        rootLayout.addView(btnPick);

        Button btnProcess = createStyledButton("🚀 Старт с валидацией ключей и моделей", "#059669");
        btnProcess.setOnClickListener(v -> startResilientGeneration());
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
        btn.setPadding(20, 18, 20, 18);
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
            selectedImageUri = data.getData();
            try {
                InputStream is = getContentResolver().openInputStream(selectedImageUri);
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
                statusText.setText("✅ Фото загружено. Готово к валидации и отправке.");
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    // --- 1. ВАЛИДАЦИЯ ПУЛА КЛЮЧЕЙ И СОРТИРОВКА ПО KUDOS ---
    private List<String> getPrioritizedWorkingKeys() {
        List<KeyScore> workingKeys = new ArrayList<>();
        for (String key : hordeKeys) {
            try {
                Request request = new Request.Builder()
                        .url("https://stablehorde.net/api/v2/find_user")
                        .addHeader("apikey", key)
                        .get()
                        .build();
                Response response = httpClient.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    JSONObject json = new JSONObject(response.body().string());
                    double kudos = json.optDouble("kudos", 0);
                    if (kudos > 0) {
                        workingKeys.add(new KeyScore(key, kudos));
                    }
                }
            } catch (Exception ignored) {}
        }
        // Сортировка по количеству Kudos (у кого больше баланс — тот первый в очереди)
        Collections.sort(workingKeys, (a, b) -> Double.compare(b.kudos, a.kudos));
        List<String> sortedKeys = new ArrayList<>();
        for (KeyScore ks : workingKeys) {
            sortedKeys.add(ks.key);
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

    // --- 2. ПОЛУЧЕНИЕ И ПРИОРИТЕТИЗАЦИЯ МОДЕЛЕЙ ПО ОЧЕРЕДИ ---
    private List<String> getPrioritizedModels() {
        List<String> defaultModels = new ArrayList<>();
        defaultModels.add("stable_diffusion");
        defaultModels.add("Yiffy");
        defaultModels.add("grapefruit");
        defaultModels.add("WAI-NSFW-illustrious-SDXL");
        defaultModels.add("Grapefruit Hentai");

        try {
            Request request = new Request.Builder()
                    .url("https://stablehorde.net/api/v2/status/models")
                    .get()
                    .build();
            Response response = httpClient.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                JSONArray data = new JSONArray(response.body().string());
                final class ModelMetric {
                    String name;
                    double loadScore;
                    ModelMetric(String name, double loadScore) {
                        this.name = name;
                        this.loadScore = loadScore;
                    }
                }
                List<ModelMetric> metrics = new ArrayList<>();
                for (int i = 0; i < data.length(); i++) {
                    JSONObject m = data.getJSONObject(i);
                    String name = m.optString("name", "");
                    int count = m.optInt("count", 1);
                    if (count <= 0) count = 1;
                    int queued = m.optInt("queued", 0);
                    if (name.toLowerCase().contains("albedobase") || name.toLowerCase().contains("xl")) continue;
                    metrics.add(new ModelMetric(name, (double) queued / count));
                }
                Collections.sort(metrics, Comparator.comparingDouble(a -> a.loadScore));
                List<String> dynamicList = new ArrayList<>();
                for (ModelMetric mm : metrics) {
                    if (!dynamicList.contains(mm.name)) dynamicList.add(mm.name);
                }
                if (!dynamicList.isEmpty()) {
                    // Гарантируем стабильный fallback на stable_diffusion
                    if (dynamicList.contains("stable_diffusion")) {
                        dynamicList.remove("stable_diffusion");
                        dynamicList.add(0, "stable_diffusion");
                    }
                    return dynamicList;
                }
            }
        } catch (Exception ignored) {}
        return defaultModels;
    }

    // --- 3. ОСНОВНОЙ ПРОЦЕСС ГЕНЕРАЦИИ С ПОЛНЫМ ПЕРЕБОРОМ КЛЮЧЕЙ И МОДЕЛЕЙ ---
    private void startResilientGeneration() {
        if (selectedImageBytes == null) {
            Toast.makeText(this, "⚠️ Сначала выберите фото из галереи!", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("🔍 Валидация и запуск");
        progress.setMessage("Проверка ключей и пула моделей AI Horde...");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            try {
                // Шаг А: Проверяем и фильтруем ключи
                handler.post(() -> progress.setMessage("Проверка пула API ключей..."));
                List<String> workingKeys = getPrioritizedWorkingKeys();
                if (workingKeys.isEmpty()) {
                    throw new RuntimeException("Все ключи AI Horde невалидны или исчерпали квоты (Kudos <= 0)!");
                }

                // Шаг Б: Получаем загруженность моделей
                handler.post(() -> progress.setMessage("Анализ очередей свободных моделей..."));
                List<String> modelsToTry = getPrioritizedModels();

                String base64Image = Base64.encodeToString(selectedImageBytes, Base64.NO_WRAP);
                String prompt = "nude:1.5, masterpiece, best quality, highly detailed 3d pixar anime:1.2, seductive, flawless skin, perfect lighting, beautiful face, detailed eyes";
                String negativePrompt = "(deformed face, ugly, lowres, bad anatomy)";
                String fullPrompt = prompt + " ### " + negativePrompt;

                JSONObject paramsObj = new JSONObject();
                paramsObj.put("sampler_name", "k_dpmpp_2m");
                paramsObj.put("cfg_scale", 7.8);
                paramsObj.put("steps", 35);
                paramsObj.put("denoising_strength", 0.44);

                JSONArray postProc = new JSONArray();
                postProc.put("GFPGAN");
                paramsObj.put("post_processing", postProc);

                byte[] resultBytes = null;
                boolean successGenerated = false;

                // Шаг В: Цикл перебора моделей и ключей (Failover Loop)
                for (String modelName : modelsToTry) {
                    if (successGenerated) break;
                    
                    for (String apiKey : workingKeys) {
                        try {
                            handler.post(() -> progress.setMessage("Пробуем модель: " + modelName));

                            JSONArray modelsArray = new JSONArray();
                            modelsArray.put(modelName);

                            JSONObject payload = new JSONObject();
                            payload.put("prompt", fullPrompt);
                            payload.put("params", paramsObj);
                            payload.put("nsfw", true);
                            payload.put("censor_nsfw", false);
                            payload.put("source_image", base64Image);
                            payload.put("source_processing", "img2img");
                            payload.put("models", modelsArray);

                            RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json; charset=utf-8"));
                            Request request = new Request.Builder()
                                    .url("https://stablehorde.net/api/v2/generate/async")
                                    .addHeader("apikey", apiKey)
                                    .addHeader("accept", "application/json")
                                    .post(body)
                                    .build();

                            Response response = httpClient.newCall(request).execute();
                            if (response.code() in [401, 402, 403, 429]) {
                                continue; // Ключ сдох, пробуем следующий ключ
                            }
                            if (response.code() == 400) {
                                break; // Модель отклонила запрос, переходим к следующей модели
                            }
                            if (!response.isSuccessful() || response.body() == null) {
                                continue;
                            }

                            String respStr = response.body().string();
                            JSONObject respJson = new JSONObject(respStr);
                            String jobId = respJson.optString("id", null);
                            if (jobId == null) continue;

                            // Ожидание в очереди задачи
                            String statusUrl = "https://stablehorde.net/api/v2/generate/status/" + jobId;
                            boolean jobDone = false;
                            String imgUrl = null;

                            for (int attempt = 0; attempt < 120; attempt++) {
                                Thread.sleep(5000);
                                Request statusReq = new Request.Builder().url(statusUrl).get().build();
                                Response statusResp = httpClient.newCall(statusReq).build(); // Fixed execute call
                                Response statRespReal = httpClient.newCall(statusReq).execute();
                                if (statRespReal.isSuccessful() && statRespReal.body() != null) {
                                    JSONObject stJson = new JSONObject(statRespReal.body().string());
                                    if (stJson.optBoolean("faulted", false)) {
                                        jobDone = true;
                                        break; // Воркер сбросил
                                    }
                                    if (stJson.optBoolean("done", false)) {
                                        JSONArray gens = stJson.optJSONArray("generations");
                                        if (gens != null && gens.length() > 0) {
                                            imgUrl = gens.getJSONObject(0).optString("img", null);
                                        }
                                        jobDone = true;
                                        break;
                                    } else {
                                        int qPos = stJson.optInt("queue_position", 0);
                                        handler.post(() -> progress.setMessage("Очередь [" + modelName + "], позиция: " + qPos));
                                    }
                                }
                            }

                            if (jobDone && imgUrl != null && !imgUrl.isEmpty()) {
                                handler.post(() -> progress.setMessage("Скачивание результата..."));
                                Request imgReq = new Request.Builder().url(imgUrl).get().build();
                                Response imgResp = httpClient.newCall(imgReq).execute();
                                if (imgResp.isSuccessful() && imgResp.body() != null) {
                                    resultBytes = imgResp.body().bytes();
                                    successGenerated = true;
                                    break;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }

                if (!successGenerated || resultBytes == null) {
                    throw new RuntimeException("Все доступные ключи и модели проверены, но генерация не удалась.");
                }

                Bitmap finalBmp = BitmapFactory.decodeByteArray(resultBytes, 0, resultBytes.length);
                saveBitmapToGallery(finalBmp, "MonsterAI_" + System.currentTimeMillis() + ".jpg");

                handler.post(() -> {
                    progress.dismiss();
                    imageView.setImageBitmap(finalBmp);
                    statusText.setText("✅ Успешно сгенерировано и сохранено в галерею!");
                    Toast.makeText(MainActivity.this, "Сохранено в Pictures/MonsterAI", Toast.LENGTH_LONG).show();
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