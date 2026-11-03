package com.hentay2.version02;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.text.TextUtils;
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
import java.util.Iterator;
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
    private static final String PREFS_NAME = "MonsterAI_Prefs";
    
    private ImageView imageView;
    private TextView statusText;
    private EditText inputChanges;
    private TextView logConsole;
    private ScrollView logScrollView;
    private byte[] selectedImageBytes;
    
    private Handler handler = new Handler(Looper.getMainLooper());
    
    private String selectedModel = "grapefruit";
    private List<ModelRowHolder> modelRows = new ArrayList<ModelRowHolder>();

    // Список моделей по умолчанию (будет динамически обновляться из GitHub)
    private List<String> availableModels = new ArrayList<String>();
    private JSONObject modelReferenceData = new JSONObject();

    // Ключи AI Horde
    private List<String> hordeKeys = new ArrayList<String>();
    
    private OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private static class ModelRowHolder {
        String modelName;
        Button btnSelect;
        Button btnSettings;
        ModelRowHolder(String name, Button sel, Button set) {
            this.modelName = name;
            this.btnSelect = sel;
            this.btnSettings = set;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        hordeKeys.add("lJSv4pVAjwtzOYfVldIVxg");
        hordeKeys.add("OjbJD9mxPNcXGU7DfGQAXQ");
        hordeKeys.add("V0lhf56Xg982yORgPuZl6A");
        hordeKeys.add("ZGnWWME9ZIBRCWHUFGQbWg");
        hordeKeys.add("pww7e3-SPoteR7r794Mjng");
        hordeKeys.add("7IPa67lmBrwLPeWz5MGWbQ");
        hordeKeys.add("j7gxGfUrvncLoJDuVzrVWA");

        // Базовый список на случай отсутствия интернета при старте
        String[] fallbackModels = {
            "grapefruit", "Yiffy", "WAI-NSFW-illustrious-SDXL", 
            "Grapefruit Hentai", "Nova Furry Pony", "AbyssOrangeMix-AfterDark", "MeinaMix", "GhostMix"
        };
        for(String m : fallbackModels) availableModels.add(m);

        ScrollView mainScroll = new ScrollView(this);
        mainScroll.setBackgroundColor(Color.parseColor("#030712"));

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        rootLayout.setPadding(25, 25, 25, 25);

        TextView title = new TextView(this);
        title.setText("🔥 Monster AI Horde Pro Studio");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(17f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 15);
        rootLayout.addView(title);

        statusText = new TextView(this);
        statusText.setText("ℹ️ Загрузка моделей с серверов AI Horde...");
        statusText.setTextColor(Color.parseColor("#94A3B8"));
        statusText.setTextSize(12f);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 0, 0, 15);
        rootLayout.addView(statusText);

        imageView = new ImageView(this);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 400
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
        labelModel.setText("🤖 Выберите модель (⚙️ - Настройки и описание):");
        labelModel.setTextColor(Color.parseColor("#F8FAFC"));
        labelModel.setTextSize(12f);
        labelModel.setPadding(0, 10, 0, 8);
        rootLayout.addView(labelModel);

        final LinearLayout modelsContainer = new LinearLayout(this);
        modelsContainer.setOrientation(LinearLayout.VERTICAL);

        final ScrollView modelsScroll = new ScrollView(this);
        LinearLayout.LayoutParams mScrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 320
        );
        mScrollParams.setMargins(0, 0, 0, 15);
        modelsScroll.setLayoutParams(mScrollParams);
        modelsScroll.setBackgroundColor(Color.parseColor("#0F172A"));
        modelsScroll.addView(modelsContainer);
        rootLayout.addView(modelsScroll);

        // Построение списка моделей в UI
        rebuildModelsUI(modelsContainer);

        Button btnProcess = createStyledButton("🚀 Запустить генерацию", "#059669");
        btnProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGeneration();
            }
        });
        rootLayout.addView(btnProcess);

        TextView labelLogs = new TextView(this);
        labelLogs.setText("📋 Отладочные логи (можно выделять текст):");
        labelLogs.setTextColor(Color.parseColor("#38BDF8"));
        labelLogs.setTextSize(12f);
        labelLogs.setTypeface(Typeface.DEFAULT_BOLD);
        labelLogs.setPadding(0, 10, 0, 5);
        rootLayout.addView(labelLogs);

        logScrollView = new ScrollView(this);
        LinearLayout.LayoutParams logScrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 260
        );
        logScrollParams.setMargins(0, 0, 0, 10);
        logScrollView.setLayoutParams(logScrollParams);
        logScrollView.setBackgroundColor(Color.parseColor("#0B0F19"));

        logConsole = new TextView(this);
        logConsole.setText("⏳ Инициализация фоновых задач...\n");
        logConsole.setTextColor(Color.parseColor("#34D399"));
        logConsole.setTextSize(11f);
        logConsole.setTypeface(Typeface.MONOSPACE);
        logConsole.setPadding(15, 15, 15, 15);
        logConsole.setTextIsSelectable(true); // Разрешает выделение и копирование пальцем из консоли!
        logScrollView.addView(logConsole);
        rootLayout.addView(logScrollView);

        LinearLayout logButtonsLayout = new LinearLayout(this);
        logButtonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        Button btnCopyLogs = createStyledButton("📋 Копировать логи", "#475569");
        LinearLayout.LayoutParams btnCpParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        btnCpParams.setMargins(0, 0, 5, 10);
        btnCopyLogs.setLayoutParams(btnCpParams);
        btnCopyLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("MonsterAI Logs", logConsole.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, "✅ Логи скопированы!", Toast.LENGTH_SHORT).show();
            }
        });
        logButtonsLayout.addView(btnCopyLogs);

        Button btnClearLogs = createStyledButton("🧹 Очистить консоль", "#991B1B");
        LinearLayout.LayoutParams btnClParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        btnClParams.setMargins(5, 0, 0, 10);
        btnClearLogs.setLayoutParams(btnClParams);
        btnClearLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logConsole.setText("");
                appendLog("Консоль очищена.");
            }
        });
        logButtonsLayout.addView(btnClearLogs);

        rootLayout.addView(logButtonsLayout);
        mainScroll.addView(rootLayout);
        setContentView(mainScroll);

        // Запуск загрузки актуальных NSFW моделей с серверов AI Horde в фоне
        fetchHordeNsfwModelsAsync(modelsContainer);
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
        btn.setPadding(15, 14, 15, 14);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 8);
        btn.setLayoutParams(params);
        return btn;
    }

    private void rebuildModelsUI(final LinearLayout container) {
        container.removeAllViews();
        modelRows.clear();

        for (int i = 0; i < availableModels.size(); i++) {
            final String mName = availableModels.get(i);

            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowParams.setMargins(0, 0, 0, 6);
            rowLayout.setLayoutParams(rowParams);

            Button mBtn = new Button(this);
            mBtn.setText(mName);
            mBtn.setTextSize(12f);
            mBtn.setTypeface(Typeface.DEFAULT_BOLD);
            mBtn.setPadding(12, 10, 12, 10);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.80f);
            mBtn.setLayoutParams(btnParams);

            Button setBtn = new Button(this);
            setBtn.setText("⚙️ Меню");
            setBtn.setTextSize(11f);
            setBtn.setPadding(8, 10, 8, 10);
            LinearLayout.LayoutParams setParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.20f);
            setParams.setMargins(6, 0, 0, 0);
            setBtn.setLayoutParams(setParams);
            setBtn.setBackgroundColor(Color.parseColor("#334155"));
            setBtn.setTextColor(Color.parseColor("#38BDF8"));

            if (mName.equals(selectedModel)) {
                mBtn.setBackgroundColor(Color.parseColor("#059669"));
                mBtn.setTextColor(Color.WHITE);
            } else {
                mBtn.setBackgroundColor(Color.parseColor("#1E293B"));
                mBtn.setTextColor(Color.parseColor("#CBD5E1"));
            }

            final ModelRowHolder holder = new ModelRowHolder(mName, mBtn, setBtn);
            modelRows.add(holder);

            mBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedModel = mName;
                    updateModelButtonsState();
                    appendLog("Выбрана активная модель: " + selectedModel);
                }
            });

            setBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showModelSettingsDialog(mName);
                }
            });

            rowLayout.addView(mBtn);
            rowLayout.addView(setBtn);
            container.addView(rowLayout);
        }
    }

    private void updateModelButtonsState() {
        for (int i = 0; i < modelRows.size(); i++) {
            ModelRowHolder h = modelRows.get(i);
            if (h.modelName.equals(selectedModel)) {
                h.btnSelect.setBackgroundColor(Color.parseColor("#059669"));
                h.btnSelect.setTextColor(Color.WHITE);
            } else {
                h.btnSelect.setBackgroundColor(Color.parseColor("#1E293B"));
                h.btnSelect.setTextColor(Color.parseColor("#CBD5E1"));
            }
        }
    }

    // --- ДИАЛОГ НАСТРОЕК МОДЕЛИ И ЕГО СОХРАНЕНИЕ ---
    private void showModelSettingsDialog(final String modelName) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Читаем сохраненные настройки или значения по умолчанию
        float defCfg = prefs.getFloat(modelName + "_cfg", 7.5f);
        int defSteps = prefs.getInt(modelName + "_steps", 20);
        float defDenoise = prefs.getFloat(modelName + "_denoise", 0.60f);
        String defPrompt = prefs.getString(modelName + "_prompt", "masterpiece, best quality, highly detailed anime, xxx, explicit, uncensored, highly detailed nude, bare skin, extremely detailed anatomy, hyper-detailed reproductive organs, meticulously detailed genitals, intricate physiological features, flawless skin texture, hentai");

        // Ищем описание из базы GitHub
        String rawDesc = "Описание отсутствует в базе.";
        try {
            if (modelReferenceData.has(modelName)) {
                JSONObject obj = modelReferenceData.getJSONObject(modelName);
                rawDesc = obj.optString("description", "Описание отсутствует.");
            }
        } catch (Exception ignored) {}

        // Перевод базового описания на русский язык через бесплатный API
        final String englishDesc = stripHtmlTags(rawDesc);

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(30, 20, 30, 20);
        dialogLayout.setBackgroundColor(Color.parseColor("#0F172A"));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("🛠 Настройки модели: " + modelName);
        tvTitle.setTextColor(Color.parseColor("#38BDF8"));
        tvTitle.setTextSize(15f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setPadding(0, 0, 0, 10);
        dialogLayout.addView(tvTitle);

        final TextView tvDesc = new TextView(this);
        tvDesc.setText("📝 Описание (перевод): Загрузка...");
        tvDesc.setTextColor(Color.parseColor("#94A3B8"));
        tvDesc.setTextSize(12f);
        tvDesc.setPadding(0, 0, 0, 15);
        dialogLayout.addView(tvDesc);

        // Асинхронный перевод описания на русский
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String translated = translateToEnglishOrRussian(englishDesc, "ru");
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        tvDesc.setText("📝 Описание: " + translated);
                    }
                });
            }
        }).start();

        // Поля ввода для параметров
        TextView lblCfg = new TextView(this);
        lblCfg.setText("⚡ Сила промпта (CFG Scale, от 1 до 20):");
        lblCfg.setTextColor(Color.parseColor("#F8FAFC"));
        lblCfg.setTextSize(12f);
        dialogLayout.addView(lblCfg);

        final EditText inputCfg = new EditText(this);
        inputCfg.setText(String.valueOf(defCfg));
        inputCfg.setTextColor(Color.BLACK);
        inputCfg.setBackgroundColor(Color.WHITE);
        inputCfg.setTextSize(13f);
        inputCfg.setPadding(12, 12, 12, 12);
        dialogLayout.addView(inputCfg);

        TextView lblSteps = new TextView(this);
        lblSteps.setText("🔄 Шаги генерации (Steps, от 10 до 50):");
        lblSteps.setTextColor(Color.parseColor("#F8FAFC"));
        lblSteps.setTextSize(12f);
        lblSteps.setPadding(0, 10, 0, 0);
        dialogLayout.addView(lblSteps);

        final EditText inputSteps = new EditText(this);
        inputSteps.setText(String.valueOf(defSteps));
        inputSteps.setTextColor(Color.BLACK);
        inputSteps.setBackgroundColor(Color.WHITE);
        inputSteps.setTextSize(13f);
        inputSteps.setPadding(12, 12, 12, 12);
        dialogLayout.addView(inputSteps);

        TextView lblDenoise = new TextView(this);
        lblDenoise.setText("🎨 Сила изменения фото (Denoising, от 0.1 до 0.9):");
        lblDenoise.setTextColor(Color.parseColor("#F8FAFC"));
        lblDenoise.setTextSize(12f);
        lblDenoise.setPadding(0, 10, 0, 0);
        dialogLayout.addView(lblDenoise);

        final EditText inputDenoise = new EditText(this);
        inputDenoise.setText(String.valueOf(defDenoise));
        inputDenoise.setTextColor(Color.BLACK);
        inputDenoise.setBackgroundColor(Color.WHITE);
        inputDenoise.setTextSize(13f);
        inputDenoise.setPadding(12, 12, 12, 12);
        dialogLayout.addView(inputDenoise);

        TextView lblPrompt = new TextView(this);
        lblPrompt.setText("✍️ Базовый NSFW промпт для этой модели:");
        lblPrompt.setTextColor(Color.parseColor("#F8FAFC"));
        lblPrompt.setTextSize(12f);
        lblPrompt.setPadding(0, 10, 0, 0);
        dialogLayout.addView(lblPrompt);

        final EditText inputPrompt = new EditText(this);
        inputPrompt.setText(defPrompt);
        inputPrompt.setTextColor(Color.BLACK);
        inputPrompt.setBackgroundColor(Color.WHITE);
        inputPrompt.setTextSize(12f);
        inputPrompt.setPadding(12, 12, 12, 12);
        dialogLayout.addView(inputPrompt);

        ScrollView scrollDlg = new ScrollView(this);
        scrollDlg.addView(dialogLayout);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(scrollDlg);
        builder.setPositiveButton("💾 Сохранить", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    float newCfg = Float.parseFloat(inputCfg.getText().toString().trim());
                    int newSteps = Integer.parseInt(inputSteps.getText().toString().trim());
                    float newDenoise = Float.parseFloat(inputDenoise.getText().toString().trim());
                    String newPrompt = inputPrompt.getText().toString().trim();

                    SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                    editor.putFloat(modelName + "_cfg", newCfg);
                    editor.putInt(modelName + "_steps", newSteps);
                    editor.putFloat(modelName + "_denoise", newDenoise);
                    editor.putString(modelName + "_prompt", newPrompt);
                    editor.apply();

                    appendLog("⚙️ Настройки для модели [" + modelName + "] успешно сохранены!");
                    Toast.makeText(MainActivity.this, "✅ Настройки сохранены навсегда!", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "❌ Ошибка в числах: проверьте запятые и точки!", Toast.LENGTH_LONG).show();
                }
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    // --- ФЕТЧ И АВТОМАТИЧЕСКАЯ ЗАГРУЗКА NSFW МОДЕЛЕЙ ИЗ GITHUB ---
    private void fetchHordeNsfwModelsAsync(final LinearLayout modelsContainer) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    appendLog("🌐 Загрузка базы моделей с GitHub (stable_diffusion.json)...");
                    String refUrl = "https://raw.githubusercontent.com/db0/AI-Horde-image-model-reference/main/stable_diffusion.json";
                    Request reqRef = new Request.Builder().url(refUrl).get().build();
                    Response respRef = httpClient.newCall(reqRef).build();
                    if (respRef.isSuccessful() && respRef.body() != null) {
                        modelReferenceData = new JSONObject(respRef.body().string());
                        appendLog("✅ База описаний моделей загружена успешно.");
                    }

                    String validKey = getValidHordeKeyInternal();
                    appendLog("🌐 Запрос активных воркеров AI Horde (/status/models)...");
                    
                    Request reqModels = new Request.Builder()
                            .url("https://stablehorde.net/api/v2/status/models")
                            .addHeader("apikey", validKey)
                            .addHeader("Client-Agent", "MonsterAI:1.0:android")
                            .get()
                            .build();
                    Response respModels = httpClient.newCall(reqModels).build();
                    
                    if (respModels.isSuccessful() && respModels.body() != null) {
                        JSONArray activeModels = new JSONArray(respModels.body().string());
                        
                        String[] keywords = {
                            'nsfw', 'hentai', 'xxx', 'r34', 'rule34', 'erotic', 'adult', 
                            'porn', 'nude', 'smut', 'uncensored', 'grapefruit', 'abyss', 
                            'basil', 'blood', 'mix', 'yiffy', 'furry'
                        };

                        List<String> foundNsfw = new ArrayList<String>();

                        for (int i = 0; i < activeModels.length(); i++) {
                            JSONObject mObj = activeModels.getJSONObject(i);
                            String name = mObj.optString("name", "");
                            int count = mObj.optInt("count", 0);
                            
                            if (count > 0 && !name.isEmpty()) {
                                String lower = name.toLowerCase();
                                boolean match = false;
                                for (String kw : keywords) {
                                    if (lower.contains(kw)) {
                                        match = true;
                                        break;
                                    }
                                }
                                if (match && !foundNsfw.contains(name)) {
                                    foundNsfw.add(name);
                                }
                            }
                        }

                        if (!foundNsfw.isEmpty()) {
                            Collections.sort(foundNsfw);
                            availableModels = foundNsfw;
                            appendLog("🔞 Найдено активных NSFW моделей онлайн: " + availableModels.size());
                            
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    rebuildModelsUI(modelsContainer);
                                    statusText.setText("✅ Найдено активных NSFW моделей: " + availableModels.size());
                                }
                            });
                        } else {
                            appendLog("⚠️ Не найдено активных NSFW моделей, используем стандартный пул.");
                        }
                    }
                } catch (Exception e) {
                    appendLog("⚠️ Не удалось загрузить модели с сервера: " + e.getMessage());
                }
            }
        }).start();
    }

    private String getValidHordeKeyInternal() {
        for (String key : hordeKeys) {
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
                    String username = json.optString("username", "Unknown");
                    double kudos = json.optdouble("kudos", 0);
                    appendLog("✅ Рабочий ключ AI Horde! Пользователь: " + username + " (Kudos: " + (int)kudos + ")");
                    return key;
                }
            } catch (Exception ignored) {}
        }
        appendLog("⚠️ Все ключи недействительны. Используем анонимный токен.");
        return "0000000000";
    }

    private String stripHtmlTags(String text) {
        if (text == null) return "";
        return text.replaceAll("<.*?>", "");
    }

    private String translateToEnglishOrRussian(String text, String targetLang) {
        if (text == null || text.trim().isEmpty()) return "";
        try {
            String encoded = URLEncoder.encode(text, "UTF-8");
            String url = "https://api.mymemory.translated.net/get?q=" + encoded + "&langpair=auto|" + targetLang;
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
                appendLog("Фото выбрано. Байтов: " + selectedImageBytes.length);
            } catch (Exception e) {
                appendLog("❌ Ошибка загрузки фото: " + e.getMessage());
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
                    double kudos = json.optdouble("kudos", 0);
                    workingKeys.add(new KeyScore(key, kudos));
                }
            } catch (Exception ignored) {}
        }
        
        if (workingKeys.isEmpty()) {
            workingKeys.add(new KeyScore("0000000000", 0.0));
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
                    appendLog("--- СТАРТ ГЕНЕРАЦИИ (Модель: " + selectedModel + ") ---");
                    
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progress.setMessage("Перевод пожеланий на английский...");
                        }
                    });
                    
                    String translatedChanges = "";
                    if (!userChangesRaw.isEmpty()) {
                        translatedChanges = translateToEnglishOrRussian(userChangesRaw, "en");
                        appendLog("📝 Перевод пожеланий: '" + userChangesRaw + "' -> '" + translatedChanges + "'");
                    }

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progress.setMessage("Проверка ключей...");
                        }
                    });
                    List<String> workingKeys = getPrioritizedWorkingKeys();

                    Bitmap originalBmp = BitmapFactory.decodeByteArray(selectedImageBytes, 0, selectedImageBytes.length);
                    Bitmap squareBmp = makeSquareBitmap(originalBmp);

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    squareBmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    String base64Image = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);

                    // Читаем сохраненные пользователем настройки для выбранной модели
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                    float cfg = prefs.getFloat(selectedModel + "_cfg", 7.5f);
                    int steps = prefs.getInt(selectedModel + "_steps", 20);
                    float denoise = prefs.getFloat(selectedModel + "_denoise", 0.60f);
                    String basePrompt = prefs.getString(selectedModel + "_prompt", "masterpiece, best quality, highly detailed anime, xxx, explicit, uncensored, highly detailed nude, bare skin, extremely detailed anatomy, hyper-detailed reproductive organs, meticulously detailed genitals, intricate physiological features, flawless skin texture, hentai");

                    // Точное слияние пользовательского запроса с базовым промптом модели для лучшего понимания нейросетью
                    String fullPrompt = basePrompt;
                    if (!translatedChanges.isEmpty()) {
                        fullPrompt = basePrompt + ", " + translatedChanges;
                    }
                    
                    String negativePrompt = "(lowres, bad anatomy, bad hands:1.5, poorly drawn hands:1.5, missing fingers:1.5, extra fingers:1.5, mutated hands:1.5, fused fingers:1.5, disfigured, text, ugly, blurry, twisted neck, backward head, facing away, disconnected head, broken posture)";
                    String finalPayloadPrompt = fullPrompt + " ### " + negativePrompt;

                    JSONObject paramsObj = new JSONObject();
                    paramsObj.put("sampler_name", "k_dpmpp_2m");
                    paramsObj.put("cfg_scale", (double)cfg);
                    paramsObj.put("steps", steps);
                    paramsObj.put("denoising_strength", (double)denoise);

                    byte[] resultBytes = null;
                    boolean successGenerated = false;

                    List<String> modelsToTry = new ArrayList<String>();
                    modelsToTry.add(selectedModel);
                    for (int i = 0; i < availableModels.size(); i++) {
                        String m = availableModels.get(i);
                        if (!modelsToTry.contains(m)) modelsToTry.add(m);
                    }

                    for (int mIdx = 0; mIdx < modelsToTry.size(); mIdx++) {
                        if (successGenerated) break;
                        final String modelName = modelsToTry.get(mIdx);

                        for (int kIdx = 0; kIdx < workingKeys.size(); kIdx++) {
                            if (successGenerated) break;
                            String apiKey = workingKeys.get(kIdx);

                            try {
                                appendLog("👉 Запрос к модели: [" + modelName + "]");
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        progress.setMessage("Модель: " + modelName);
                                    }
                                });

                                JSONArray modelsArray = new JSONArray();
                                modelsArray.put(modelName);

                                JSONObject payload = new JSONObject();
                                payload.put("prompt", finalPayloadPrompt);
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
                                        .addHeader("Client-Agent", "MonsterAI:1.0:android")
                                        .post(body)
                                        .build();

                                Response response = httpClient.newCall(request).execute();
                                int code = response.code();
                                
                                if (code == 400) {
                                    String err = response.body() != null ? response.body().string() : "";
                                    appendLog("⚠️ Модель " + modelName + " отклонила запрос (400): " + err);
                                    break;
                                }
                                if (!response.isSuccessful() || response.body() == null) {
                                    continue;
                                }

                                String respStr = response.body().string();
                                JSONObject respJson = new JSONObject(respStr);
                                String jobId = respJson.optString("id", null);
                                if (jobId == null) continue;

                                appendLog("✅ Задача принята! Job ID: " + jobId);
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
                                            appendLog("❌ Воркер сбросил задачу (faulted).");
                                            jobDone = true;
                                            break;
                                        }
                                        if (stJson.optBoolean("done", false)) {
                                            JSONArray gens = stJson.optJSONArray("generations");
                                            if (gens != null && gens.length() > 0) {
                                                imgUrl = gens.getJSONObject(0).optString("img", null);
                                            }
                                            jobDone = true;
                                            appendLog("🎉 Генерация завершена воркером!");
                                            break;
                                        } else {
                                            final int qPos = stJson.optInt("queue_position", 0);
                                            final int waitSec = stJson.optInt("wait_time", 0);
                                            appendLog("⏳ В очереди [" + modelName + "], позиция: " + qPos + ", ожидание: " + waitSec + "с");
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
                                    appendLog("📥 Скачивание результата...");
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            progress.setMessage("Скачивание...");
                                        }
                                    });
                                    Request imgReq = new Request.Builder().url(imgUrl).get().build();
                                    Response imgResp = httpClient.newCall(imgReq).execute();
                                    if (imgResp.isSuccessful() && imgResp.body() != null) {
                                        resultBytes = imgResp.body().bytes();
                                        successGenerated = true;
                                        appendLog("✅ Фото успешно скачано (" + resultBytes.length + " байт).");
                                        break;
                                    }
                                }
                            } catch (Exception ex) {
                                appendLog("🔴 Ошибка: " + ex.getMessage());
                            }
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
                            Toast.makeText(MainActivity.this, "Сохранено в Pictures/MonsterAI", Toast.LENGTH_LONG).show();
                            appendLog("✨ Готово! Фото сохранено в галерею устройства.");
                        }
                    });

                } catch (final Exception e) {
                    appendLog("❌ КРИТИЧЕСКАЯ ОШИБКА: " + e.getMessage());
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
                    appendLog("Файл записан в MediaStore: " + fileName);
                }
            }
        } catch (Exception e) {
            appendLog("❌ Ошибка сохранения в галерею: " + e.getMessage());
        }
    }
}