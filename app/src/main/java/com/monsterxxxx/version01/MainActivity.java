package com.monsterxxxx.version01;

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
import android.widget.HorizontalScrollView;
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

// THEME: @android:style/Theme.Material.Light.NoActionBar
public class MainActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 101;
    
    private ImageView imageView;
    private TextView statusText;
    private EditText inputChanges;
    private TextView logConsole;
    private ScrollView logScrollView;
    private byte[] selectedImageBytes;
    private Handler handler = new Handler(Looper.getMainLocall() != null ? Looper.getMainLooper() : Looper.getMainLooper());
    
    // Модель по умолчанию
    private String selectedModel = "grapefruit";
    private List<Button> modelButtons = new ArrayList<Button>();

    // Пул ключей AI Horde
    private List<String> hordeKeys = new ArrayList<String>();
    
    // Все 33 NSFW модели из вашего списка
    private final String[] allAvailableModels = {
        "grapefruit",
        "Yiffy",
        "WAI-NSFW-illustrious-SDXL",
        "Grapefruit Hentai",
        "Nova Furry Pony",
        "526Mix-Animated",
        "Abyss OrangeMix",
        "AbyssOrangeMix-AfterDark",
        "BB95 Furry Mix v14",
        "BB95 Furry Mix",
        "BlenderMix Pony",
        "BweshMix",
        "CamelliaMix 2.5D",
        "Cetus-Mix",
        "Cheese Daddys Landscape Mix",
        "CyriousMix",
        "Ether Real Mix",
        "ExpMix Line",
        "GhostMix",
        "Hentai Diffusion",
        "HolyMix ILXL",
        "Lawlas's yiff mix",
        "MeinaMix",
        "MoonMix Fantasy",
        "NTR MIX IL-Noob XL",
        "Nova Furry XL",
        "Pastel Mix",
        "Rag Illustrious Mix",
        "Real Dos Mix",
        "Ri-Mix",
        "WAI-ANI-NSFW-PONYXL",
        "iCoMix Inpainting",
        "iCoMix",
        "majicMIX realistic"
    };
    
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
        rootLayout.setPadding(25, 25, 25, 25);

        TextView title = new TextView(this);
        title.setText("🔥 Monster AI Horde Pro Studio (33 Models)");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(17f);
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
        labelChanges.setText("✍️ Что изменить на фото (рус/укр):");
        labelChanges.setTextColor(Color.parseColor("#F8FAFC"));
        labelChanges.setTextSize(12f);
        labelChanges.setPadding(0, 10, 0, 5);
        rootLayout.addView(labelChanges);

        inputChanges = new EditText(this);
        inputChanges.setHint("Например: сделать красные волосы, добавить очки...");
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

        TextView labelModel = new TextView(this);
        labelModel.setText("🤖 Выберите модель ИИ (прокрутите для всех 33):");
        labelModel.setTextColor(Color.parseColor("#F8FAFC"));
        labelModel.setTextSize(12f);
        labelModel.setPadding(0, 10, 0, 8);
        rootLayout.addView(labelModel);

        // Горизонтальный/вертикальный скролл для выбора всех моделей
        LinearLayout modelsContainer = new LinearLayout(this);
        modelsContainer.setOrientation(LinearLayout.VERTICAL);

        ScrollView modelsScroll = new ScrollView(this);
        LinearLayout.LayoutParams mScrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 350
        );
        mScrollParams.setMargins(0, 0, 0, 15);
        modelsScroll.setLayoutParams(mScrollParams);
        modelsScroll.setBackgroundColor(Color.parseColor("#0F172A"));

        for (int i = 0; i < allAvailableModels.length; i++) {
            final String mName = allAvailableModels[i];
            Button mBtn = new Button(this);
            mBtn.setText(mName);
            mBtn.setTextSize(12f);
            mBtn.setTypeface(Typeface.DEFAULT_BOLD);
            mBtn.setPadding(15, 12, 15, 12);
            
            LinearLayout.LayoutParams mParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            mParams.setMargins(4, 4, 4, 4);
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
                    updateModelButtonsState();
                    appendLog("Выбрана модель: " + selectedModel);
                }
            });

            modelButtons.add(mBtn);
            modelsContainer.addView(mBtn);
        }
        modelsScroll.addView(modelsContainer);
        rootLayout.addView(modelsScroll);

        Button btnProcess = createStyledButton("🚀 Запустить генерацию", "#059669");
        btnProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGeneration();
            }
        });
        rootLayout.addView(btnProcess);

        // --- ТЕРМИНАЛ ЛОГОВ ДЛЯ ОТЛАДКИ ---
        TextView labelLogs = new TextView(this);
        labelLogs.setText("📋 Отладочные логи в реальном времени:");
        labelLogs.setTextColor(Color.parseColor("#38BDF8"));
        labelLogs.setTextSize(12f);
        labelLogs.setTypeface(Typeface.DEFAULT_BOLD);
        labelLogs.setPadding(0, 10, 0, 5);
        rootLayout.addView(labelLogs);

        logScrollView = new ScrollView(this);
        LinearLayout.LayoutParams logScrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 300
        );
        logScrollParams.setMargins(0, 0, 0, 10);
        logScrollView.setLayoutParams(logScrollParams);
        logScrollView.setBackgroundColor(Color.parseColor("#0B0F19"));

        logConsole = new TextView(this);
        logConsole.setText("⏳ Приложение готово к работе...\n");
        logConsole.setTextColor(Color.parseColor("#34D399"));
        logConsole.setTextSize(11f);
        logConsole.setTypeface(Typeface.MONOSPACE);
        logConsole.setPadding(15, 15, 15, 15);
        logScrollView.addView(logConsole);
        rootLayout.addView(logScrollView);

        Button btnCopyLogs = createStyledButton("📋 Скопировать логи в буфер", "#475569");
        btnCopyLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("MonsterAI Logs", logConsole.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, "✅ Логи скопированы в буфер обмена!", Toast.LENGTH_SHORT).show();
            }
        });
        rootLayout.addView(btnCopyLogs);

        mainScroll.addView(rootLayout);
        setContentView(mainScroll);
        
        appendLog("MainActivity создана успешно. Загружено моделей: " + allAvailableModels.length);
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

    private void updateModelButtonsState() {
        for (int i = 0; i < modelButtons.size(); i++) {
            Button btn = modelButtons.get(i);
            String mName = allAvailableModels[i];
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
                appendLog("Фото успешно выбрано из галереи. Размер байт: " + selectedImageBytes.length);
            } catch (Exception e) {
                appendLog("❌ Ошибка загрузки фото: " + e.getMessage());
                Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    // Точный аналог make_image_square (ImageOps.pad)
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
        canvas.drawColor(Color.BLACK); // Черные поля (pad)
        
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
                        appendLog("Перевод: '" + text + "' -> '" + translated + "'");
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
        appendLog("Проверка пула ключей AI Horde...");
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
                    appendLog("✅ Ключ #" + (i+1) + " (" + key.substring(0, 5) + "...) Активен. Kudos: " + (int)kudos);
                } else {
                    appendLog("❌ Ключ #" + (i+1) + " отклонен (Code: " + response.code() + ")");
                }
            } catch (Exception e) {
                appendLog("⚠️ Сбой сети при проверке ключа #" + (i+1));
            }
        }
        
        if (workingKeys.isEmpty()) {
            appendLog("⚠️ Валидация вернула пустоту. Используем все ключи принудительно.");
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
        progress.setMessage("Подготовка и генерация...");
        progress.setCancelable(false);
        progress.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    appendLog("--- СТАРТ НОВОЙ ГЕНЕРАЦИИ ---");
                    
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progress.setMessage("Перевод пожеланий...");
                        }
                    });
                    String translatedChanges = translateToEnglish(userChangesRaw);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progress.setMessage("Проверка ключей...");
                        }
                    });
                    List<String> workingKeys = getPrioritizedWorkingKeys();

                    // Квадратирование фото с черными полями
                    Bitmap originalBmp = BitmapFactory.decodeByteArray(selectedImageBytes, 0, selectedImageBytes.length);
                    Bitmap squareBmp = makeSquareBitmap(originalBmp);

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    squareBmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    String base64Image = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);

                    // Инъекции для img2img
                    String hardcoreInject = "masterpiece, best quality, highly detailed anime, xxx, explicit, uncensored, highly detailed nude, bare skin, extremely detailed anatomy, hyper-detailed reproductive organs, meticulously detailed genitals, intricate physiological features, flawless skin texture, hentai";
                    if (!translatedChanges.isEmpty()) {
                        hardcoreInject += ", " + translatedChanges;
                    }
                    String negativePrompt = "(lowres, bad anatomy, bad hands:1.5, poorly drawn hands:1.5, missing fingers:1.5, extra fingers:1.5, mutated hands:1.5, fused fingers:1.5, disfigured, text, ugly, blurry, twisted neck, backward head, facing away, disconnected head, broken posture)";
                    String fullPrompt = hardcoreInject + " ### " + negativePrompt;

                    // Параметры из вашего словаря БЕЗ width/height чтобы избежать черных квадратов
                    JSONObject paramsObj = new JSONObject();
                    paramsObj.put("sampler_name", "k_dpmpp_2m");
                    paramsObj.put("cfg_scale", 7.5);
                    paramsObj.put("steps", 20);
                    paramsObj.put("denoising_strength", 0.60);

                    byte[] resultBytes = null;
                    boolean successGenerated = false;

                    // Формируем список моделей: выбранная пользователем идет первой, затем остальные 32 модели
                    List<String> modelsToTry = new ArrayList<String>();
                    modelsToTry.add(selectedModel);
                    for (int i = 0; i < allAvailableModels.length; i++) {
                        String m = allAvailableModels[i];
                        if (!modelsToTry.contains(m)) {
                            modelsToTry.add(m);
                        }
                    }

                    appendLog("Всего моделей для перебора: " + modelsToTry.size());

                    for (int mIdx = 0; mIdx < modelsToTry.size(); mIdx++) {
                        if (successGenerated) break;
                        final String modelName = modelsToTry.get(mIdx);

                        for (int kIdx = 0; kIdx < workingKeys.size(); kIdx++) {
                            if (successGenerated) break;
                            String apiKey = workingKeys.get(kIdx);

                            try {
                                appendLog("👉 Пробую модель: [" + modelName + "], Ключ: " + apiKey.substring(0, 5) + "...");
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        progress.setMessage("Модель: " + modelName);
                                    }
                                });

                                JSONArray modelsArray = new JSONArray();
                                modelsArray.put(modelName);

                                JSONObject payload = new JSONObject();
                                payload.put("prompt", fullPrompt);
                                payload.put("params", paramsObj);
                                payload.put("nsfw", true);          // Обязательный параметр из ваших требований
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
                                        .addHeader("Client-Agent", "MonsterAI:1.0:android")
                                        .post(body)
                                        .build();

                                Response response = httpClient.newCall(request).execute();
                                int code = response.code();
                                
                                if (code == 401 || code == 402 || code == 403 || code == 429) {
                                    appendLog("⚠️ Ключ отклонен (HTTP " + code + "). Пробуем следующий ключ.");
                                    continue;
                                }
                                if (code == 400) {
                                    String errBody = response.body() != null ? response.body().string() : "";
                                    appendLog("⚠️ Модель " + modelName + " отклонила запрос (HTTP 400): " + errBody);
                                    break; // Модель не поддерживает параметры, переходим к следующей модели
                                }
                                if (!response.isSuccessful() || response.body() == null) {
                                    appendLog("⚠️ Ошибка ответа: HTTP " + code);
                                    continue;
                                }

                                String respStr = response.body().string();
                                JSONObject respJson = new JSONObject(respStr);
                                String jobId = respJson.optString("id", null);
                                if (jobId == null) {
                                    appendLog("⚠️ Не получен job_id от сервера");
                                    continue;
                                }

                                appendLog("✅ Запрос принят! Job ID: " + jobId + ". Ожидаем генерацию...");
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
                                            appendLog("❌ Воркер модели " + modelName + " сбросил задачу (faulted).");
                                            jobDone = true;
                                            break;
                                        }
                                        if (stJson.optBoolean("done", false)) {
                                            JSONArray gens = stJson.optJSONArray("generations");
                                            if (gens != null && gens.length() > 0) {
                                                imgUrl = gens.getJSONObject(0).optString("img", null);
                                            }
                                            jobDone = true;
                                            appendLog("🎉 Задача успешно завершена воркером!");
                                            break;
                                        } else {
                                            final int qPos = stJson.optInt("queue_position", 0);
                                            final int waitTime = stJson.optInt("wait_time", 0);
                                            appendLog("⏳ Статус [" + modelName + "] Позиция в очереди: " + qPos + ", Ждать сек: " + waitTime);
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
                                    appendLog("📥 Скачивание результата с URL: " + imgUrl);
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
                                        appendLog("✅ Фото успешно получено и весит " + resultBytes.length + " байт!");
                                        break;
                                    }
                                }
                            } catch (Exception ex) {
                                appendLog("🔴 Ошибка в процессе отправки: " + ex.getMessage());
                            }
                        }
                    }

                    if (!successGenerated || resultBytes == null) {
                        throw new RuntimeException("Все 33 модели и ключи перебраны, но генерация не удалась.");
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
                            appendLog("✨ Готово! Изображение сохранено в галерею.");
                        }
                    });

                } catch (final Exception e) {
                    appendLog("❌ КРИТИЧЕСКАЯ ОШИБКА ГЕНЕРАЦИИ: " + e.getMessage());
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
                    appendLog("Файл успешно сохранен в MediaStore: " + fileName);
                }
            }
        } catch (Exception e) {
            appendLog("❌ Ошибка записи в галерею: " + e.getMessage());
        }
    }
}