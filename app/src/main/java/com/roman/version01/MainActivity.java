package com.roman.version01;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.view.ViewGroup;
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
    private TextView statusText, selectedModelText;
    private byte[] selectedImageBytes;
    private Handler handler = new Handler(Looper.getMainLocper() != null ? Looper.getMainLooper() : Looper.getMainLooper());
    
    // Выбранная модель по умолчанию
    private String selectedModel = "grapefruit";
    private List<Button> modelButtonList = new ArrayList<>();
    
    // Список моделей из вашего ТЗ
    private final String[] availableModels = {
        "grapefruit", "Yiffy", "WAI-NSFW-illustrious-SDXL", "Grapefruit Hentai", "Nova Furry Pony"
    };

    // Пул ключей AI Horde
    private List<String> hordeKeys = new ArrayList<String>() {{
        add("lJSv4pVAjwtzOYfVldIVxg");
        add("OjbJD9mxPNcXGU7DfGQAXQ");
        add("V0lhf56Xg982yORgPuZl6A");
        add("ZGnWWME9ZIBRCWHUFGQbWg");
        add("pww7e3-SPoteR7r794Mjng");
        add("7IPa67lmBrwLPeWz5MGWbQ");
        add("j7gxGfUrvncLoJDuVzrVWA");
    }};
    
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
        rootLayout.setPadding(20, 20, 20, 20);

        ScrollView mainScroll = new ScrollView(this);
        mainScroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("🔥 Monster AI Horde Studio v7.0");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(16f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 10);
        container.addView(title);

        statusText = new TextView(this);
        statusText.setText("ℹ️ Загрузите фото из галереи для начала работы");
        statusText.setTextColor(Color.parseColor("#94A3B8"));
        statusText.setTextSize(12f);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 0, 0, 10);
        container.addView(statusText);

        // Панель выбора модели (Кнопки)
        TextView modelLabel = new TextView(this);
        modelLabel.setText("🎨 Выберите ИИ-модель:");
        modelLabel.setTextColor(Color.parseColor("#F59E0B"));
        modelLabel.setTextSize(12f);
        modelLabel.setPadding(0, 5, 0, 5);
        container.addView(modelLabel);

        LinearLayout modelContainer = new LinearLayout(this);
        modelContainer.setOrientation(LinearLayout.VERTICAL);
        for (String mName : availableModels) {
            Button mBtn = createStyledButton(mName, mName.equals(selectedModel) ? "#0D9488" : "#1E293B");
            modelButtonList.add(mBtn);
            mBtn.setOnClickListener(v -> {
                selectedModel = mName;
                updateModelButtonColors();
                Toast.makeText(this, "Выбрана модель: " + mName, Toast.LENGTH_SHORT).show();
            });
            modelContainer.addView(mBtn);
        }
        container.addView(modelContainer);

        imageView = new ImageView(this);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 550
        );
        imgParams.setMargins(0, 10, 0, 10);
        imageView.setLayoutParams(imgParams);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setBackgroundColor(Color.parseColor("#0F172A"));
        container.addView(imageView);

        Button btnPick = createStyledButton("📁 Выбрать фото из галереи", "#2563EB");
        btnPick.setOnClickListener(v -> openGallery());
        container.addView(btnPick);

        mainScroll.addView(container);
        rootLayout.addView(mainScroll);
        setContentView(rootLayout);
    }

    private Button createStyledButton(String text, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(12f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        btn.setPadding(15, 12, 15, 12);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 6);
        btn.setLayoutParams(params);
        return btn;
    }

    private void updateModelButtonColors() {
        for (int i = 0; i < availableModels.length; i++) {
            String mName = availableModels[i];
            Button btn = modelButtonList.get(i);
            if (mName.equals(selectedModel)) {
                btn.setBackgroundColor(Color.parseColor("#0D9488")); // Выбранная модель (красивый бирюзовый)
            } else {
                btn.setBackgroundColor(Color.parseColor("#1E293B")); // Неактивные
            }
        }
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

                // Сразу после загрузки вызываем диалог ввода изменений (как в оригинале)
                showCustomChangesDialog();

            } catch (Exception e) {
                Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    // --- ДИАЛОГ ВВОДА ИЗМЕНЕНИЙ (ЧЕРНЫЙ ТЕКСТ НА БЕЛОМ ФОНЕ) ---
    private void showCustomChangesDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);
        layout.setBackgroundColor(Color.parseColor("#F8FAFC"));

        TextView dialogTitle = new TextView(this);
        dialogTitle.setText("✍️ Внесите изменения (на русском или английском):");
        dialogTitle.setTextColor(Color.parseColor("#0F172A"));
        dialogTitle.setTextSize(13f);
        dialogTitle.setTypeface(Typeface.DEFAULT_BOLD);
        dialogTitle.setPadding(0, 0, 0, 10);
        layout.addView(dialogTitle);

        final EditText inputChanges = new EditText(this);
        inputChanges.setHint("Например: сделать красные волосы, добавить очки...");
        inputChanges.setTextColor(Color.parseColor("#000000")); // Черный текст
        inputChanges.setHintTextColor(Color.parseColor("#64748B"));
        inputChanges.setBackgroundColor(Color.parseColor("#FFFFFF")); // Белый фон
        inputChanges.setTextSize(13f);
        inputChanges.setPadding(15, 15, 15, 15);
        inputChanges.setMinLines(3);
        layout.addView(inputChanges);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("⚙️ Настройка генерации")
            .setView(layout)
            .setPositiveButton("🚀 Запуск обработки", (d, w) -> {
                String rawChanges = inputChanges.getText().toString().trim();
                translateAndStartGeneration(rawChanges);
            })
            .setNeutralButton("➡️ Пропустить (Оригинал)", (d, w) -> {
                translateAndStartGeneration("");
            })
            .setNegativeButton("Отмена", null)
            .create();

        dialog.show();
    }

    // --- АВТОПЕРЕВОД ТЕКСТА НА АНГЛИЙСКИЙ (ЧЕРЕЗ БЕСПЛАТНЫЙ API) ---
    private void translateAndStartGeneration(String russianText) {
        if (russianText.isEmpty()) {
            startResilientGeneration("");
            return;
        }

        ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("🔄 Перевод промпта");
        progress.setMessage("Перевод текста на английский язык...");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            String translated = russianText;
            try {
                String encoded = URLEncoder.encode(russianText, "UTF-8");
                String urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=en&dt=t&q=" + encoded;
                Request req = new Request.Builder().url(urlStr).get().build();
                Response resp = httpClient.newCall(req).execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    String jsonStr = resp.body().string();
                    JSONArray arr = new JSONArray(jsonStr);
                    JSONArray sentences = arr.getJSONArray(0);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < sentences.length(); i++) {
                        sb.append(sentences.getJSONArray(i).getString(0));
                    }
                    if (sb.length() > 0) {
                        translated = sb.toString();
                    }
                }
            } catch (Exception ignored) {}

            final String finalEnPrompt = translated;
            handler.post(() -> {
                progress.dismiss();
                startResilientGeneration(finalEnPrompt);
            });
        }).start();
    }

    // --- ОСНОВНАЯ ГЕНЕРАЦИЯ С AI HORDE И СОХРАНЕНИЕМ ПРОПОРЦИЙ ---
    private void startResilientGeneration(String customChanges) {
        if (selectedImageBytes == null) {
            Toast.makeText(this, "⚠️ Сначала выберите фото!", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("⏳ AI Horde Processing");
        progress.setMessage("Проверка ключей и отправка задачи...");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            try {
                // Проверяем валидность ключей
                List<String> workingKeys = new ArrayList<>();
                for (String key : hordeKeys) {
                    try {
                        Request req = new Request.Builder()
                                .url("https://stablehorde.net/api/v2/find_user")
                                .addHeader("apikey", key)
                                .get()
                                .build();
                        Response resp = httpClient.newCall(req).execute();
                        if (resp.isSuccessful() && resp.body() != null) {
                            JSONObject json = new JSONObject(resp.body().string());
                            if (json.optDouble("kudos", 0) > 0) {
                                workingKeys.add(key);
                            }
                        }
                    } catch (Exception ignored) {}
                }

                if (workingKeys.isEmpty()) {
                    workingKeys.add("0000000000"); // Анонимный ключ если все исчерпаны
                }

                String base64Image = Base64.encodeToString(selectedImageBytes, Base64.NO_WRAP);
                
                // Базовые инжекты + перевод пользователя
                String hardcoreInject = "nude:1.5, masterpiece, best quality, highly detailed 3d pixar anime:1.2, explicit, nsfw:1.5, uncensored:1.5, flawless skin, perfect lighting, beautiful face";
                if (!customChanges.isEmpty()) {
                    hardcoreInject += ", " + customChanges;
                }
                
                String negativePrompt = "(deformed face, ugly, lowres, bad anatomy)";
                String fullPrompt = hardcoreInject + " ### " + negativePrompt;

                JSONObject paramsObj = new JSONObject();
                paramsObj.put("sampler_name", "k_dpmpp_2m");
                paramsObj.put("cfg_scale", 7.8);
                paramsObj.put("steps", 35);
                paramsObj.put("denoising_strength", 0.44);

                JSONArray postProc = new JSONArray();
                postProc.put("GFPGAN");
                paramsObj.put("post_processing", postProc);

                byte[] resultBytes = null;
                boolean success = false;

                for (String apiKey : workingKeys) {
                    if (success) break;
                    try {
                        JSONArray modelsArray = new JSONArray();
                        modelsArray.put(selectedModel);

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
                        if (!response.isSuccessful() || response.body() == null) continue;

                        JSONObject respJson = new JSONObject(response.body().string());
                        String jobId = respJson.optString("id", null);
                        if (jobId == null) continue;

                        String statusUrl = "https://stablehorde.net/api/v2/generate/status/" + jobId;
                        String imgUrl = null;

                        for (int attempt = 0; attempt < 120; attempt++) {
                            Thread.sleep(5000);
                            Request statusReq = new Request.Builder().url(statusUrl).get().build();
                            Response statResp = httpClient.newCall(statusReq).execute();
                            if (statResp.isSuccessful() && statResp.body() != null) {
                                JSONObject stJson = new JSONObject(statResp.body().string());
                                if (stJson.optBoolean("faulted", false)) break;
                                if (stJson.optBoolean("done", false)) {
                                    JSONArray gens = stJson.optJSONArray("generations");
                                    if (gens != null && gens.length() > 0) {
                                        imgUrl = gens.getJSONObject(0).optString("img", null);
                                    }
                                    break;
                                } else {
                                    int qPos = stJson.optInt("queue_position", 0);
                                    handler.post(() -> progress.setMessage("Очередь [" + selectedModel + "], позиция: " + qPos));
                                }
                            }
                        }

                        if (imgUrl != null && !imgUrl.isEmpty()) {
                            handler.post(() -> progress.setMessage("Скачивание результата..."));
                            Request imgReq = new Request.Builder().url(imgUrl).get().build();
                            Response imgResp = httpClient.newCall(imgReq).execute();
                            if (imgResp.isSuccessful() && imgResp.body() != null) {
                                resultBytes = imgResp.body().bytes();
                                success = true;
                                break;
                            }
                        }
                    } catch (Exception ignored) {}
                }

                if (!success || resultBytes == null) {
                    throw new RuntimeException("Не удалось получить результат от AI Horde. Попробуйте другую модель.");
                }

                // Декодируем и сохраняем с сохранением оригинальных пропорций (без принудительного квадрата)
                Bitmap finalBmp = BitmapFactory.decodeByteArray(resultBytes, 0, resultBytes.length);
                saveBitmapToGallery(finalBmp, "MonsterAI_" + System.currentTimeMillis() + ".jpg");

                handler.post(() -> {
                    progress.dismiss();
                    imageView.setImageBitmap(finalBmp);
                    statusText.setText("✅ Готово! Сохранено в галерею.");
                    Toast.makeText(MainActivity.this, "Фото сохранено в Pictures/MonsterAI", Toast.LENGTH_LONG).show();
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