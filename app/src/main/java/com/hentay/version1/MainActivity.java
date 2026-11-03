package com.hentay.version1;

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
    
    // Ключ AI Horde из вашего скрипта (можно заменить на любой рабочий)
    private String hordeApiKey = "lJSv4pVAjwtzOYfVldIVxg";
    
    // OkHttp клиент для связи с серверами AI Horde
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
        title.setText("🎨 AI Horde Img2Img Studio");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(18f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 15);
        rootLayout.addView(title);

        statusText = new TextView(this);
        statusText.setText("ℹ️ Выберите фото из галереи для обработки нейросетью");
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

        Button btnProcess = createStyledButton("🚀 Запустить обработку в AI Horde", "#059669");
        btnProcess.setOnClickListener(v -> startHordeProcessing());
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
                statusText.setText("✅ Фото загружено! Нажмите кнопку запуска.");
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startHordeProcessing() {
        if (selectedImageBytes == null) {
            Toast.makeText(this, "⚠️ Сначала выберите фото из галереи!", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("⏳ Связь с AI Horde");
        progress.setMessage("Отправка задачи на сервер...");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            try {
                // 1. Конвертируем выбранное фото в Base64
                String base64Image = Base64.encodeToString(selectedImageBytes, Base64.NO_WRAP);

                // 2. Промпты (аналогично вашему Python скрипту)
                String prompt = "nude:1.5, masterpiece, best quality, highly detailed 3d pixar anime:1.2, seductive, flawless skin, perfect lighting, beautiful face, detailed eyes";
                String negativePrompt = "(deformed face, ugly, lowres, bad anatomy)";
                String fullPrompt = prompt + " ### " + negativePrompt;

                JSONObject paramsObj = new JSONObject();
                paramsObj.put("sampler_name", "k_dpmpp_2m");
                paramsObj.put("cfg_scale", 7.8);
                paramsObj.put("steps", 35);
                paramsObj.put("denoising_strength", 0.44);

                JSONArray modelsArray = new JSONArray();
                modelsArray.put("grapefruit");

                JSONObject payload = new JSONObject();
                payload.put("prompt", fullPrompt);
                payload.put("params", paramsObj);
                payload.put("nsfw", true);
                payload.put("censor_nsfw", false);
                payload.put("source_image", base64Image);
                payload.put("source_processing", "img2img");
                payload.put("models", modelsArray);

                // 3. Отправка POST запроса на генерацию (async)
                RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url("https://stablehorde.net/api/v2/generate/async")
                        .addHeader("apikey", hordeApiKey)
                        .addHeader("accept", "application/json")
                        .post(body)
                        .build();

                Response response = httpClient.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Ошибка отправки: " + response.code());
                }

                String respStr = response.body().string();
                JSONObject respJson = new JSONObject(respStr);
                String jobId = respJson.optString("id", null);
                if (jobId == null || jobId.isEmpty()) {
                    throw new RuntimeException("Не получен Job ID от сервера");
                }

                // 4. Цикл ожидания в очереди (как в вашем Python-скрипте)
                String statusUrl = "https://stablehorde.net/api/v2/generate/status/" + jobId;
                String imgUrl = null;

                for (int i = 0; i < 120; i++) { // Проверяем каждые 5 секунд (до 10 минут)
                    Thread.sleep(5000);

                    Request statusReq = new Request.Builder()
                            .url(statusUrl)
                            .addHeader("accept", "application/json")
                            .get()
                            .build();

                    Response statusResp = httpClient.newCall(statusReq).execute();
                    if (statusResp.isSuccessful() && statusResp.body() != null) {
                        String statusBody = statusResp.body().string();
                        JSONObject statusJson = new JSONObject(statusBody);

                        boolean done = statusJson.optBoolean("done", false);
                        boolean faulted = statusJson.optBoolean("faulted", false);

                        if (faulted) {
                            throw new RuntimeException("Воркер AI Horde сбросил задачу (Faulted)");
                        }

                        if (done) {
                            JSONArray gens = statusJson.optJSONArray("generations");
                            if (gens != null && gens.length() > 0) {
                                imgUrl = gens.getJSONObject(0).optString("img", null);
                            }
                            break;
                        } else {
                            int queuePos = statusJson.optInt("queue_position", 0);
                            handler.post(() -> progress.setMessage("Место в очереди ИИ: " + queuePos));
                        }
                    }
                }

                if (imgUrl == null || imgUrl.isEmpty()) {
                    throw new RuntimeException("Истекло время ожидания или не удалось получить картинку");
                }

                // 5. Скачивание готовой картинки с сервера
                handler.post(() -> progress.setMessage("Скачивание готового результата..."));
                Request imgReq = new Request.Builder().url(imgUrl).get().build();
                Response imgResp = httpClient.newCall(imgReq).execute();
                if (!imgResp.isSuccessful() || imgResp.body() == null) {
                    throw new RuntimeException("Ошибка скачивания результата");
                }

                byte[] resultBytes = imgResp.body().bytes();
                Bitmap finalBmp = BitmapFactory.decodeByteArray(resultBytes, 0, resultBytes.length);

                // 6. Автоматическое сохранение в галерею телефона
                saveBitmapToGallery(finalBmp, "MonsterAI_" + System.currentTimeMillis() + ".jpg");

                handler.post(() -> {
                    progress.dismiss();
                    imageView.setImageBitmap(finalBmp);
                    statusText.setText("✅ Готово! Результат сохранен в галерею.");
                    Toast.makeText(MainActivity.this, "Сохранено в папку Pictures/MonsterAI", Toast.LENGTH_LONG).show();
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