package com.newhentay1.version01;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
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
import android.text.InputType;
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
    
    private String selectedModel = "grapefruit";
    private LinearLayout modelsContainer;
    private List<LinearLayout> modelRows = new ArrayList<LinearLayout>();

    private List<String> hordeKeys = new ArrayList<String>();
    
    // Динамический список моделей (заполняется из сети или дефолтного набора)
    private List<String> allAvailableModels = new ArrayList<String>();
    
    private OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

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

        // Панель управления моделями сверху
        LinearLayout topButtonsLayout = new LinearLayout(this);
        topButtonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        topButtonsLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        Button btnRefreshModels = createStyledButton("🔄 Обновить модели", "#0284C7");
        LinearLayout.LayoutParams refParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        );
        refParams.setMargins(0, 0, 5, 10);
        btnRefreshModels.setLayoutParams(refParams);
        btnRefreshModels.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchOnlineModelsAsync(true);
            }
        });
        topButtonsLayout.addView(btnRefreshModels);
        rootLayout.addView(topButtonsLayout);

        statusText = new TextView(this);
        statusText.setText("ℹ️ Выберите фото и укажите пожелания");
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
        labelModel.setText("🤖 Выберите модель ИИ и настройте параметры:");
        labelModel.setTextColor(Color.parseColor("#F8FAFC"));
        labelModel.setTextSize(12f);
        labelModel.setPadding(0, 10, 0, 8);
        rootLayout.addView(labelModel);

        modelsContainer = new LinearLayout(this);
        modelsContainer.setOrientation(LinearLayout.VERTICAL);

        ScrollView modelsScroll = new ScrollView(this);
        LinearLayout.LayoutParams mScrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 350
        );
        mScrollParams.setMargins(0, 0, 0, 15);
        modelsScroll.setLayoutParams(mScrollParams);
        modelsScroll.setBackgroundColor(Color.parseColor("#0F172A"));
        modelsScroll.addView(modelsContainer);
        rootLayout.addView(modelsScroll);

        // Инициализация дефолтного списка моделей
        initDefaultModelsList();
        rebuildModelListUI();

        // Автоматическое обновление списка при первом запуске
        SharedPreferences prefsInit = getSharedPreferences("ModelPrefs", MODE_PRIVATE);
        boolean isFirstRun = prefsInit.getBoolean("is_first_run", true);
        if (isFirstRun) {
            prefsInit.edit().putBoolean("is_first_run", false).apply();
            fetchOnlineModelsAsync(false);
        }

        Button btnProcess = createStyledButton("🚀 Запустить генерацию", "#059669");
        btnProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGeneration();
            }
        });
        rootLayout.addView(btnProcess);

        TextView labelLogs = new TextView(this);
        labelLogs.setText("📋 Отладочные логи (можно выбирать и прокручивать):");
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
        logConsole.setText("⏳ Приложение готово к работе...\n");
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
        
        appendLog("MainActivity инициализирована. Моделей доступно: " + allAvailableModels.size());
    }

    private void initDefaultModelsList() {
        allAvailableModels.clear();
        allAvailableModels.add("grapefruit");
        allAvailableModels.add("Yiffy");
        allAvailableModels.add("WAI-NSFW-illustrious-SDXL");
        allAvailableModels.add("Grapefruit Hentai");
        allAvailableModels.add("Nova Furry Pony");
        allAvailableModels.add("526Mix-Animated");
        allAvailableModels.add("Abyss OrangeMix");
        allAvailableModels.add("AbyssOrangeMix-AfterDark");
        allAvailableModels.add("BB95 Furry Mix v14");
        allAvailableModels.add("BlenderMix Pony");
        allAvailableModels.add("BweshMix");
        allAvailableModels.add("CamelliaMix 2.5D");
        allAvailableModels.add("Cetus-Mix");
        allAvailableModels.add("Cheese Daddys Landscape Mix");
        allAvailableModels.add("CyriousMix");
        allAvailableModels.add("Ether Real Mix");
        allAvailableModels.add("ExpMix Line");
        allAvailableModels.add("GhostMix");
        allAvailableModels.add("Hentai Diffusion");
        allAvailableModels.add("HolyMix ILXL");
        allAvailableModels.add("Lawlas's yiff mix");
        allAvailableModels.add("MeinaMix");
        allAvailableModels.add("MoonMix Fantasy");
        allAvailableModels.add("NTR MIX IL-Noob XL");
        allAvailableModels.add("Nova Furry XL");
        allAvailableModels.add("Pastel Mix");
        allAvailableModels.add("Rag Illustrious Mix");
        allAvailableModels.add("Real Dos Mix");
        allAvailableModels.add("Ri-Mix");
        allAvailableModels.add("WAI-ANI-NSFW-PONYXL");
        allAvailableModels.add("iCoMix Inpainting");
        allAvailableModels.add("iCoMix");
        allAvailableModels.add("majicMIX realistic");
    }

    private boolean isModelNsfw(String modelName) {
        if (modelName == null) return false;
        String lower = modelName.toLowerCase();
        return lower.contains("nsfw") || 
               lower.contains("hentai") || 
               lower.contains("yiffy") || 
               lower.contains("furry") || 
               lower.contains("afterdark") || 
               lower.contains("lawlas");
    }

    private void fetchOnlineModelsAsync(final boolean showToast) {
        appendLog("🌐 Запрос актуального списка моделей с сервера Stable Horde...");
        if (showToast) {
            Toast.makeText(this, "🔄 Обновление списка моделей...", Toast.LENGTH_SHORT).show();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Request request = new Request.Builder()
                            .url("https://stablehorde.net/api/v2/status/models")
                            .addHeader("accept", "application/json")
                            .addHeader("Client-Agent", "MonsterAI:1.0:android")
                            .get()
                            .build();

                    Response response = httpClient.newCall(request).execute();
                    if (response.isSuccessful() && response.body() != null) {
                        String jsonStr = response.body().string();
                        JSONArray jsonArray = new JSONArray(jsonStr);
                        
                        final List<String> fetchedModels = new ArrayList<String>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            String name = obj.optString("name", "");
                            String type = obj.optString("type", "image");
                            if (!name.isEmpty() && "image".equalsIgnoreCase(type)) {
                                fetchedModels.add(name);
                            }
                        }

                        if (!fetchedModels.isEmpty()) {
                            Collections.sort(fetchedModels);
                            
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    allAvailableModels = fetchedModels;
                                    if (!allAvailableModels.contains(selectedModel) && !allAvailableModels.isEmpty()) {
                                        selectedModel = allAvailableModels.get(0);
                                    }
                                    rebuildModelListUI();
                                    appendLog("✅ Успешно загружено моделей с сервера: " + allAvailableModels.size());
                                    if (showToast) {
                                        Toast.makeText(MainActivity.this, "✅ Список моделей обновлен (" + allAvailableModels.size() + ")", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                            return;
                        }
                    }
                    throw new Exception("Пустой ответ сервера моделей.");
                } catch (Exception e) {
                    appendLog("⚠️ Не удалось загрузить модели из сети: " + e.getMessage() + ". Используем локальный каталог.");
                    if (showToast) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "⚠️ Ошибка сети. Используется локальный список.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    private void rebuildModelListUI() {
        if (modelsContainer == null) return;
        modelsContainer.removeAllViews();
        modelRows.clear();

        for (int i = 0; i < allAvailableModels.size(); i++) {
            final String mName = allAvailableModels.get(i);
            boolean isNsfw = isModelNsfw(mName);
            
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowParams.setMargins(4, 4, 4, 4);
            rowLayout.setLayoutParams(rowParams);

            // NSFW модели выделяются полупрозрачным красным фоном (ARGB: 44EF4444)
            if (isNsfw) {
                rowLayout.setBackgroundColor(Color.parseColor("#44EF4444"));
            } else {
                rowLayout.setBackgroundColor(Color.TRANSPARENT);
            }

            Button mBtn = new Button(this);
            String labelDisplay = mName + (isNsfw ? " 🔞 [NSFW]" : "");
            mBtn.setText(labelDisplay);
            mBtn.setTextSize(12f);
            mBtn.setTypeface(Typeface.DEFAULT_BOLD);
            mBtn.setPadding(15, 12, 15, 12);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
            );
            mBtn.setLayoutParams(btnParams);

            Button menuBtn = new Button(this);
            menuBtn.setText("⚙️ Меню");
            menuBtn.setTextSize(11f);
            menuBtn.setTypeface(Typeface.DEFAULT_BOLD);
            menuBtn.setPadding(10, 12, 10, 12);
            menuBtn.setBackgroundColor(Color.parseColor("#334155"));
            menuBtn.setTextColor(Color.parseColor("#F8FAFC"));
            LinearLayout.LayoutParams menuParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            );
            menuParams.setMargins(6, 0, 0, 0);
            menuBtn.setLayoutParams(menuParams);

            if (mName.equals(selectedModel)) {
                mBtn.setBackgroundColor(Color.parseColor("#059669"));
                mBtn.setTextColor(Color.WHITE);
            } else {
                mBtn.setBackgroundColor(Color.parseColor("#1E293B"));
                mBtn.setTextColor(isNsfw ? Color.parseColor("#FCA5A5") : Color.parseColor("#CBD5E1"));
            }

            mBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedModel = mName;
                    updateModelButtonsState();
                    appendLog("Выбрана модель: " + selectedModel + (isModelNsfw(selectedModel) ? " [NSFW режим]" : ""));
                }
            });

            menuBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showModelMenuDialog(mName);
                }
            });

            rowLayout.addView(mBtn);
            rowLayout.addView(menuBtn);

            modelRows.add(rowLayout);
            modelsContainer.addView(rowLayout);
        }
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
        for (int i = 0; i < modelRows.size(); i++) {
            LinearLayout row = modelRows.get(i);
            Button mBtn = (Button) row.getChildAt(0);
            String mName = allAvailableModels.get(i);
            boolean isNsfw = isModelNsfw(mName);
            
            if (mName.equals(selectedModel)) {
                mBtn.setBackgroundColor(Color.parseColor("#059669"));
                mBtn.setTextColor(Color.WHITE);
            } else {
                mBtn.setBackgroundColor(Color.parseColor("#1E293B"));
                mBtn.setTextColor(isNsfw ? Color.parseColor("#FCA5A5") : Color.parseColor("#CBD5E1"));
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
                appendLog("Фото загружено. Байтов: " + selectedImageBytes.length);
            } catch (Exception e) {
                appendLog("❌ Ошибка загрузки фото: " + e.getMessage());
                Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    // --- МЕНЮ МОДЕЛИ И ПОНЯТНЫЕ НАСТРОЙКИ ---
    private void showModelMenuDialog(String modelName) {
        SharedPreferences prefs = getSharedPreferences("ModelPrefs", MODE_PRIVATE);
        boolean isNsfwModel = isModelNsfw(modelName);
        
        String modelDesc = "🤖 Системный идентификатор: " + modelName + "\n\n";
        if (isNsfwModel) {
            modelDesc += "⚠️ Данная модель относится к категории NSFW (18+). Оптимизирована для создания эротического контента, проработки анатомии и наготы с высоким качеством.";
        } else {
            modelDesc += "✨ Стандартная качественная модель общего назначения. Идеальна для портретов высокого разрешения, артов, пейзажей и художественной обработки.";
        }

        float savedCfg = prefs.getFloat(modelName + "_cfg", 7.5f);
        int savedSteps = prefs.getInt(modelName + "_steps", 20);
        float savedDenoising = prefs.getFloat(modelName + "_denoising", 0.60f);

        ScrollView dialogScroll = new ScrollView(this);
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(30, 30, 30, 30);
        dialogLayout.setBackgroundColor(Color.parseColor("#0F172A"));
        dialogScroll.addView(dialogLayout);

        TextView titleView = new TextView(this);
        titleView.setText("⚙️ Меню модели: " + modelName);
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(16f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setPadding(0, 0, 0, 10);
        dialogLayout.addView(titleView);

        TextView descView = new TextView(this);
        descView.setText(modelDesc);
        descView.setTextColor(Color.parseColor("#CBD5E1"));
        descView.setTextSize(12f);
        descView.setPadding(0, 0, 0, 20);
        dialogLayout.addView(descView);

        TextView labelCfg = new TextView(this);
        labelCfg.setText("🎯 Сила совпадения с промптом (CFG Scale):\nВыше значение — точнее следование вашему запросу.");
        labelCfg.setTextColor(Color.parseColor("#F8FAFC"));
        labelCfg.setTextSize(11f);
        dialogLayout.addView(labelCfg);

        final EditText inputCfg = new EditText(this);
        inputCfg.setText(String.valueOf(savedCfg));
        inputCfg.setTextColor(Color.BLACK);
        inputCfg.setBackgroundColor(Color.WHITE);
        inputCfg.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        dialogLayout.addView(inputCfg);

        TextView labelSteps = new TextView(this);
        labelSteps.setText("\n🔄 Количество шагов генерации (Steps):\nБольше шагов — выше детализация (рекомендуется 20-30).");
        labelSteps.setTextColor(Color.parseColor("#F8FAFC"));
        labelSteps.setTextSize(11f);
        dialogLayout.addView(labelSteps);

        final EditText inputSteps = new EditText(this);
        inputSteps.setText(String.valueOf(savedSteps));
        inputSteps.setTextColor(Color.BLACK);
        inputSteps.setBackgroundColor(Color.WHITE);
        inputSteps.setInputType(InputType.TYPE_CLASS_NUMBER);
        dialogLayout.addView(inputSteps);

        TextView labelDenoising = new TextView(this);
        labelDenoising.setText("\n⚡ Сила изменения исходного фото (Denoising, от 0.1 до 1.0):\nБлиже к 1.0 — полная переработка изображения, ближе к 0.1 — сохранение оригинала.");
        labelDenoising.setTextColor(Color.parseColor("#F8FAFC"));
        labelDenoising.setTextSize(11f);
        dialogLayout.addView(labelDenoising);

        final EditText inputDenoising = new EditText(this);
        inputDenoising.setText(String.valueOf(savedDenoising));
        inputDenoising.setTextColor(Color.BLACK);
        inputDenoising.setBackgroundColor(Color.WHITE);
        inputDenoising.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        dialogLayout.addView(inputDenoising);

        Button saveBtn = createStyledButton("💾 Сохранить настройки", "#059669");
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    float cfg = Float.parseFloat(inputCfg.getText().toString().trim());
                    int steps = Integer.parseInt(inputSteps.getText().toString().trim());
                    float denoising = Float.parseFloat(inputDenoising.getText().toString().trim());

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putFloat(modelName + "_cfg", cfg);
                    editor.putInt(modelName + "_steps", steps);
                    editor.putFloat(modelName + "_denoising", denoising);
                    editor.apply();

                    Toast.makeText(MainActivity.this, "✅ Настройки для " + modelName + " сохранены!", Toast.LENGTH_SHORT).show();
                    appendLog("Обновлены настройки модели " + modelName + " [CFG: " + cfg + ", Steps: " + steps + ", Denoising: " + denoising + "]");
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "❌ Ошибка в числах! Проверьте ввод.", Toast.LENGTH_LONG).show();
                }
            }
        });
        dialogLayout.addView(saveBtn);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogScroll);
        builder.setPositiveButton("Закрыть", null);
        builder.show();
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

    // Интеллектуальный перевод: ру/укр переводит на английский, английский оставляет как есть
    private String translateToEnglish(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        String trimmed = text.trim();
        
        boolean hasCyrillic = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if ((c >= '\u0400' && c <= '\u04FF') || (c >= '\u0500' && c <= '\u052F')) {
                hasCyrillic = true;
                break;
            }
        }

        if (!hasCyrillic) {
            return trimmed;
        }

        try {
            String encoded = URLEncoder.encode(trimmed, "UTF-8");
            String url = "https://api.mymemory.translated.net/get?q=" + encoded + "&langpair=auto|en";
            Request request = new Request.Builder().url(url).get().build();
            Response response = httpClient.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                JSONObject json = new JSONObject(response.body().string());
                JSONObject respData = json.optJSONObject("responseData");
                if (respData != null) {
                    String translated = respData.optString("translatedText", "").trim();
                    if (!translated.isEmpty() && !translated.toUpperCase().contains("INVALID")) {
                        appendLog("Перевод пожеланий: '" + trimmed + "' -> '" + translated + "'");
                        return translated;
                    }
                }
            }
        } catch (Exception e) {
            appendLog("⚠️ Ошибка перевода: " + e.getMessage());
        }
        return trimmed;
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
            appendLog("⚠️ Валидация вернула пустоту. Используем дефолтные ключи.");
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
        final boolean isNsfwActive = isModelNsfw(selectedModel);

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
                            progress.setMessage("Анализ и перевод пожеланий...");
                        }
                    });
                    
                    String translatedChanges = translateToEnglish(userChangesRaw);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progress.setMessage("Проверка ключей доступа...");
                        }
                    });
                    List<String> workingKeys = getPrioritizedWorkingKeys();

                    Bitmap originalBmp = BitmapFactory.decodeByteArray(selectedImageBytes, 0, selectedImageBytes.length);
                    Bitmap squareBmp = makeSquareBitmap(originalBmp);

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    squareBmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    String base64Image = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);

                    // Сборка базовых промптов (через запятую и пробел)
                    String fullPrompt = "";
                    String negativePrompt = "";

                    if (isNsfwActive) {
                        String nsfwCore = "masterpiece, best quality, highly detailed 2.5d anime illustration, beautiful detailed face, intricate clothing textures, perfect anatomy, accurate lighting, vibrant colors, expressive body, gorgeous physique, sensual curves, fully detailed skin texture, artistic nudity, uncovered body, aesthetic explicit anatomy";
                        if (!translatedChanges.isEmpty()) {
                            nsfwCore += ", " + translatedChanges;
                        }
                        negativePrompt = "(lowres, bad anatomy, bad hands:1.5, poorly drawn hands:1.5, missing fingers:1.5, extra fingers:1.5, mutated hands:1.5, fused fingers:1.5, disfigured, text, ugly, blurry, deformed, censored, mosaic, bar censor)";
                        fullPrompt = nsfwCore + " ### " + negativePrompt;
                        appendLog("🔥 Активен специализированный NSFW-промпт высочайшего качества.");
                    } else {
                        String standardCore = "masterpiece, best quality, ultra-detailed photo, sharp focus, professional studio lighting, 8k resolution, photorealistic, flawless composition";
                        if (!translatedChanges.isEmpty()) {
                            standardCore += ", " + translatedChanges;
                        }
                        negativePrompt = "(lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, fewer digits, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry)";
                        fullPrompt = standardCore + " ### " + negativePrompt;
                        appendLog("✨ Активен высококачественный фото-промпт для обычной модели.");
                    }

                    SharedPreferences prefs = getSharedPreferences("ModelPrefs", MODE_PRIVATE);
                    float cfg = prefs.getFloat(selectedModel + "_cfg", 7.5f);
                    int steps = prefs.getInt(selectedModel + "_steps", 20);
                    float denoising = prefs.getFloat(selectedModel + "_denoising", 0.60f);

                    JSONObject paramsObj = new JSONObject();
                    paramsObj.put("sampler_name", "k_dpmpp_2m");
                    paramsObj.put("cfg_scale", (double) cfg);
                    paramsObj.put("steps", steps);
                    paramsObj.put("denoising_strength", (double) denoising);

                    byte[] resultBytes = null;
                    boolean successGenerated = false;

                    List<String> modelsToTry = new ArrayList<String>();
                    modelsToTry.add(selectedModel);
                    for (int i = 0; i < allAvailableModels.size(); i++) {
                        String m = allAvailableModels.get(i);
                        if (!modelsToTry.contains(m)) {
                            modelsToTry.add(m);
                        }
                    }

                    appendLog("Моделей в очереди перебора: " + modelsToTry.size());

                    for (int mIdx = 0; mIdx < modelsToTry.size(); mIdx++) {
                        if (successGenerated) break;
                        final String modelName = modelsToTry.get(mIdx);

                        for (int kIdx = 0; kIdx < workingKeys.size(); kIdx++) {
                            if (successGenerated) break;
                            String apiKey = workingKeys.get(kIdx);

                            try {
                                appendLog("👉 Модель: [" + modelName + "], Ключ: " + apiKey.substring(0, 5) + "...");
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
                                
                                if (code == 401 || code == 402 || code == 403 || code == 429) {
                                    appendLog("⚠️ Ключ отклонен (HTTP " + code + "). Смена ключа.");
                                    continue;
                                }
                                if (code == 400) {
                                    String errBody = response.body() != null ? response.body().string() : "";
                                    appendLog("⚠️ Модель " + modelName + " отклонила запрос (HTTP 400): " + errBody);
                                    break;
                                }
                                if (!response.isSuccessful() || response.body() == null) {
                                    appendLog("⚠️ Ошибка сервера: HTTP " + code);
                                    continue;
                                }

                                String respStr = response.body().string();
                                JSONObject respJson = new JSONObject(respStr);
                                String jobId = respJson.optString("id", null);
                                if (jobId == null) {
                                    appendLog("⚠️ ID задачи не получен");
                                    continue;
                                }

                                appendLog("✅ Задача принята. Job ID: " + jobId + ". Ожидаем воркер...");
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
                                            appendLog("❌ Воркер " + modelName + " сбросил задачу (faulted).");
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
                                            final int waitTime = stJson.optInt("wait_time", 0);
                                            appendLog("⏳ Статус [" + modelName + "] Очередь: " + qPos + ", Ждать: " + waitTime + "с");
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
                                    appendLog("📥 Скачивание готового изображения...");
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
                                        appendLog("✅ Фото успешно получено (" + resultBytes.length + " байт)!");
                                        break;
                                    }
                                }
                            } catch (Exception ex) {
                                appendLog("🔴 Ошибка запроса: " + ex.getMessage());
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
                            Toast.makeText(MainActivity.this, "Фото сохранено в Pictures/MonsterAI", Toast.LENGTH_LONG).show();
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
                    appendLog("Сохранено в MediaStore: " + fileName);
                }
            }
        } catch (Exception e) {
            appendLog("❌ Ошибка сохранения в галерею: " + e.getMessage());
        }
    }
}