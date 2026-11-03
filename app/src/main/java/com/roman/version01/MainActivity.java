package com.roman.version01;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
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
    private EditText inputChanges;
    private byte[] selectedImageBytes;
    private Handler handler = new Handler(Looper.getMainLooper());
    
    private String selectedModel = "grapefruit";
    private List<Button> modelButtons = new ArrayList<Button>();

    // Пул ключей AI Horde
    private List<String> hordeKeys = new ArrayList<String>();
    
    private OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Заполнение пула ключей
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
        rootLayout.setPadding(30, 30, 30, 30);

        TextView title = new TextView(this);
        title.setText("🔥 Monster AI Horde Pro Studio");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(18f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 15);
        rootLayout.addView(title);

        statusText = new TextView(this);
        statusText.setText("ℹ️ Выберите фото и укажите пожелания для изменения");
        statusText.setTextColor(Color.parseColor("#94A3B8"));
        statusText.setTextSize(12f);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 0, 0, 15);
        rootLayout.addView(statusText);

        imageView = new ImageView(this);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 500
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
        labelChanges.setText("✍️ Что изменить на фото (рус/укр):");
        labelChanges.setTextColor(Color.parseColor("#F8FAFC"));
        labelChanges.setTextSize(12f);
        labelChanges.setPadding(0, 10, 0, 5);
        rootLayout.addView(labelChanges);

        inputChanges = new EditText(this);
        inputChanges.setHint("Например: сделать красные волосы, добавить очки...");
        inputChanges.setHintTextColor(Color.parseColor("#666666"));
        inputChanges.setTextColor(Color.parseColor("#000000")); // Черный текст
        inputChanges.setBackgroundColor(Color.parseColor("#FFFFFF")); // Белый фон
        inputChanges.setTextSize(13f);
        inputChanges.setPadding(16, 16, 16, 16);
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        editParams.setMargins(0, 0, 0, 15);
        inputChanges.setLayoutParams(editParams);
        rootLayout.addView(inputChanges);

        TextView labelModel = new TextView(this);
        labelModel.setText("🤖 Выберите модель ИИ:");
        labelModel.setTextColor(Color.parseColor("#F8FAFC"));
        labelModel.setTextSize(12f);
        labelModel.setPadding(0, 10, 0, 8);
        rootLayout.addView(labelModel);

        final String[] modelsList = {
            "grapefruit", 
            "Yiffy", 
            "WAI-NSFW-illustrious-SDXL", 
            "Grapefruit Hentai", 
            "Nova Furry Pony"
        };

        LinearLayout modelsContainer = new LinearLayout(this);
        modelsContainer.setOrientation(LinearLayout.VERTICAL);
        
        for (int i = 0; i < modelsList.length; i++) {
            final String mName = modelsList[i];
            Button mBtn = new Button(this);
            mBtn.setText(mName);
            mBtn.setTextSize(12f);
            mBtn.setTypeface(Typeface.DEFAULT_BOLD);
            mBtn.setPadding(15, 12, 15, 12);
            
            LinearLayout.LayoutParams mParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            mParams.setMargins(0, 0, 0, 6);
            mBtn.setLayoutParams(mParams);

            if (mName.equals(selectedModel)) {
                mBtn.setBackgroundColor(Color.parseColor("#059669"));
                mBtn.setTextColor(Color.WHITE);
            } else {
                mBtn.setBackgroundColor(Color.parseColor("#1E293B"));
                mBtn.setTextColor(Color.parseColor("#CBD5E1"));
            }

            mBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedModel = mName;
                    updateModelButtonsState(modelsList);
                }
            });

            modelButtons.add(mBtn);
            modelsContainer.addView(mBtn);
        }
        rootLayout.addView(modelsContainer);

        Button btnProcess = createStyledButton("🚀 Запустить генерацию", "#059669");
        btnProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGeneration();
            }
        });
        rootLayout.addView(btnProcess);

        mainScroll.addView(rootLayout);
        setContentView(mainScroll);
    }

    private void updateModelButtonsState(String[] modelsList) {
        for (int i = 0; i < modelButtons.size(); i++) {
            Button btn = modelButtons.get(i);
            String mName = modelsList[i];
            if (mName.equals(selectedModel)) {
                btn.setBackgroundColor(Color.parseColor("#059669"));
                btn.setTextColor(Color.WHITE);
            } else {
                btn.setBackgroundColor(Color.parseColor("#1E293B"));
                btn.setTextColor(Color.parseColor("#CBD5E1"));
            }
        }
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
        params.setMargins(0, 0, 0, 12);
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
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    // Создание квадрата с черными полями для сохранения пропорций
    private Bitmap makeSquareBitmap(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        if (width == height) {
            return source;
        }
        int maxDim = Math.max(width, height);
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
                        return translated;
                    }
                }
            }
        } catch (Exception ignored) {}
        return text;
    }

    // Валидация ключей с обязательным заголовком Client-Agent
    private List<String> getPrioritizedWorkingKeys() {
        List<KeyScore> workingKeys = new ArrayList<KeyScore>();
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
                }
            } catch (Exception ignored) {}
        }
        
        // Гарантированный fallback на все ключи, если проверка сети временно недоступна
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

    private void startGeneration() {
        if (selectedImageBytes == null) {
            Toast.makeText(this, "⚠️ Сначала выберите фото из галереи!", Toast.LENGTH_SHORT).show();
            return;
        }

        final String userChangesRaw = inputChanges.getText().toString().trim();

        final ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("⏳ Обработка задачи");
        progress.setMessage("Подготовка фото и проверка ключей AI Horde...");
        progress.setCancelable(false);
        progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progress.setMessage("Перевод пожеланий на английский...");
                        }
                    });
                    String translatedChanges = translateToEnglish(userChangesRaw);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progress.setMessage("Проверка пула API ключей...");
                        }
                    });
                    List<String> workingKeys = getPrioritizedWorkingKeys();
                    if (workingKeys.isEmpty()) {
                        workingKeys = hordeKeys;
                    }

                    // Квадратирование изображения (черные полосы по краям)
                    Bitmap originalBmp = BitmapFactory.decodeByteArray(selectedImageBytes, 0, selectedImageBytes.length);
                    Bitmap squareBmp = makeSquareBitmap(originalBmp);
                    int squareSize = squareBmp.getWidth();

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    squareBmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    String base64Image = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);

                    String hardcoreInject = "nude:1.5, masterpiece, best quality, highly detailed 3d pixar anime:1.2, seductive, flawless skin, perfect lighting, beautiful face, detailed eyes";
                    if (!translatedChanges.isEmpty()) {
                        hardcoreInject += ", " + translatedChanges;
                    }
                    String negativePrompt = "(lowres, bad anatomy, bad hands:1.5, poorly drawn hands:1.5, missing fingers:1.5, extra fingers:1.5, mutated hands:1.5, fused fingers:1.5, disfigured, text, ugly, blurry, twisted neck, backward head, facing away, disconnected head, broken posture)";
                    String fullPrompt = hardcoreInject + " ### " + negativePrompt;

                    JSONObject paramsObj = new JSONObject();
                    paramsObj.put("sampler_name", "k_dpmpp_2m");
                    paramsObj.put("cfg_scale", 7.8);
                    paramsObj.put("steps", 35);
                    paramsObj.put("denoising_strength", 0.44);
                    paramsObj.put("width", squareSize);
                    paramsObj.put("height", squareSize);

                    byte[] resultBytes = null;
                    boolean successGenerated = false;

                    List<String> modelsToTry = new ArrayList<String>();
                    modelsToTry.add(selectedModel);
                    String[] allModels = {"grapefruit", "Yiffy", "WAI-NSFW-illustrious-SDXL", "Grapefruit Hentai", "Nova Furry Pony"};
                    for (int i = 0; i < allModels.length; i++) {
                        String m = allModels[i];
                        if (!modelsToTry.contains(m)) modelsToTry.add(m);
                    }

                    for (int mIdx = 0; mIdx < modelsToTry.size(); mIdx++) {
                        if (successGenerated) break;
                        final String modelName = modelsToTry.get(mIdx);

                        for (int kIdx = 0; kIdx < workingKeys.size(); kIdx++) {
                            if (successGenerated) break;
                            String apiKey = workingKeys.get(kIdx);

                            try {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        progress.setMessage("Модель [" + modelName + "]. Запрос...");
                                    }
                                });

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
                                        .addHeader("Content-Type", "application/json")
                                        .addHeader("Client-Agent", "MonsterAI:1.0:android") // Обязательный заголовок AI Horde
                                        .post(body)
                                        .build();

                                Response response = httpClient.newCall(request).execute();
                                int code = response.code();
                                if (code == 401 || code == 402 || code == 403 || code == 429) {
                                    continue;
                                }
                                if (code == 400) {
                                    break;
                                }
                                if (!response.isSuccessful() || response.body() == null) {
                                    continue;
                                }

                                String respStr = response.body().string();
                                JSONObject respJson = new JSONObject(respStr);
                                String jobId = respJson.optString("id", null);
                                if (jobId == null) continue;

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
                                            jobDone = true;
                                            break;
                                        }
                                        if (stJson.optBoolean("done", false)) {
                                            JSONArray gens = stJson.optJSONArray("generations");
                                            if (gens != null && gens.length() > 0) {
                                                imgUrl = gens.getJSONObject(0).optString("img", null);
                                            }
                                            jobDone = true;
                                            break;
                                        } else {
                                            final int qPos = stJson.optInt("queue_position", 0);
                                            handler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    progress.setMessage("Очередь [" + modelName + "], позиция: " + qPos);
                                                }
                                            });
                                        }
                                    }
                                }

                                if (jobDone && imgUrl != null && !imgUrl.isEmpty()) {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            progress.setMessage("Скачивание результата...");
                                        }
                                    });
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
                        throw new RuntimeException("Все модели перегружены или отклонили запрос.");
                    }

                    final Bitmap finalBmp = BitmapFactory.decodeByteArray(resultBytes, 0, resultBytes.length);
                    saveBitmapToGallery(finalBmp, "MonsterAI_" + System.currentTimeMillis() + ".jpg");

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progress.dismiss();
                            imageView.setImageBitmap(finalBmp);
                            statusText.setText("✅ Успешно сгенерировано и сохранено!");
                            Toast.makeText(MainActivity.this, "Фото сохранено в Pictures/MonsterAI", Toast.LENGTH_LONG).show();
                        }
                    });

                } catch (final Exception e) {
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
                }
            }
        } catch (Exception ignored) {}
    }
}