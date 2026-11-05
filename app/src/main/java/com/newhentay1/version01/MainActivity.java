package com.newhentay1.version01;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.view.MotionEvent;
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
    private ScrollView modelsScrollView;
    private byte[] selectedImageBytes;
    
    private Handler handler = new Handler(Looper.getMainLooper());
    
    private String selectedModel = "grapefruit";
    private LinearLayout standardModelsContainer;
    private LinearLayout nsfwModelsContainer;
    
    private List<LinearLayout> modelRows = new ArrayList<LinearLayout>();
    private List<String> hordeKeys = new ArrayList<String>();
    
    private List<String> standardModelsList = new ArrayList<String>();
    private List<String> nsfwModelsList = new ArrayList<String>();

    // Управление отменой генерации
    private volatile boolean isGenerationCancelled = false;
    private String activeJobId = null;
    private String activeApiKey = null;
    private boolean isGenerating = false;
    
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

        // Панель обновления моделей
        LinearLayout topButtonsLayout = new LinearLayout(this);
        topButtonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        topButtonsLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        Button btnRefreshModels = createStyledButton("🔄 Обновить модели с сервера", "#0284C7");
        LinearLayout.LayoutParams refParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        );
        refParams.setMargins(0, 0, 0, 10);
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

        // --- РАЗДЕЛЕНИЕ МОДЕЛЕЙ НА ДВЕ СЕКЦИИ ---
        TextView labelModel = new TextView(this);
        labelModel.setText("🤖 Выберите модель ИИ (разделено по категориям):");
        labelModel.setTextColor(Color.parseColor("#F8FAFC"));
        labelModel.setTextSize(12f);
        labelModel.setPadding(0, 10, 0, 8);
        rootLayout.addView(labelModel);

        LinearLayout splitModelsLayout = new LinearLayout(this);
        splitModelsLayout.setOrientation(LinearLayout.VERTICAL);
        splitModelsLayout.setBackgroundColor(Color.parseColor("#0F172A"));
        splitModelsLayout.setPadding(10, 10, 10, 10);
        
        LinearLayout.LayoutParams splitParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 400
        );
        splitParams.setMargins(0, 0, 0, 15);
        splitModelsLayout.setLayoutParams(splitParams);

        // 1. Секция стандартных моделей
        TextView standardHeader = new TextView(this);
        standardHeader.setText("✨ Стандартные модели (General / Art / Photo)");
        standardHeader.setTextColor(Color.parseColor("#38BDF8"));
        standardHeader.setTextSize(11f);
        standardHeader.setTypeface(Typeface.DEFAULT_BOLD);
        standardHeader.setPadding(5, 5, 5, 5);
        splitModelsLayout.addView(standardHeader);

        standardModelsContainer = new LinearLayout(this);
        standardModelsContainer.setOrientation(LinearLayout.VERTICAL);
        splitModelsLayout.addView(standardModelsContainer);

        // Разделитель
        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#334155"));
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2
        );
        divParams.setMargins(0, 15, 0, 10);
        divider.setLayoutParams(divParams);
        splitModelsLayout.addView(divider);

        // 2. Секция NSFW моделей
        TextView nsfwHeader = new TextView(this);
        nsfwHeader.setText("🔞 NSFW модели (18+ / Anime / Furry / Uncensored)");
        nsfwHeader.setTextColor(Color.parseColor("#FCA5A5"));
        nsfwHeader.setTextSize(11f);
        nsfwHeader.setTypeface(Typeface.DEFAULT_BOLD);
        nsfwHeader.setPadding(5, 5, 5, 5);
        splitModelsLayout.addView(nsfwHeader);

        nsfwModelsContainer = new LinearLayout(this);
        nsfwModelsContainer.setOrientation(LinearLayout.VERTICAL);
        splitModelsLayout.addView(nsfwModelsContainer);

        // Оборачиваем в общий скролл с защитой касаний
        modelsScrollView = new ScrollView(this);
        modelsScrollView.setLayoutParams(splitParams);
        modelsScrollView.addView(splitModelsLayout);
        modelsScrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });
        rootLayout.addView(modelsScrollView);

        initDefaultModelsList();
        rebuildModelListUI();

        SharedPreferences prefsInit = getSharedPreferences("ModelPrefs", MODE_PRIVATE);
        boolean isFirstRun = prefsInit.getBoolean("is_first_run", true);
        if (isFirstRun) {
            prefsInit.edit().putBoolean("is_first_run", false).apply();
            fetchOnlineModelsAsync(false);
        }

        // Кнопка запуска генерации
        Button btnProcess = createStyledButton("🚀 Запустить генерацию", "#059669");
        btnProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGeneration();
            }
        });
        rootLayout.addView(btnProcess);

        // Кнопка экстренной остановки задачи (теперь всегда доступна!)
        Button btnStop = createStyledButton("⏹ Остановить генерацию", "#DC2626");
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopCurrentGeneration();
            }
        });
        rootLayout.addView(btnStop);

        TextView labelLogs = new TextView(this);
        labelLogs.setText("📋 Отладочные логи (можно выбирать и прокручивать):");
        labelLogs.setTextColor(Color.parseColor("#38BDF8"));
        labelLogs.setTextSize(12f);
        labelLogs.setTypeface(Typeface.DEFAULT_BOLD);
        labelLogs.setPadding(0, 10, 0, 5);
        rootLayout.addView(labelLogs);

        logScrollView = new ScrollView(this);
        LinearLayout.LayoutParams logScrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 250
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

        logScrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

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
        
        appendLog("MainActivity инициализирована. Всего моделей: " + (standardModelsList.size() + nsfwModelsList.size()));
    }

    private void initDefaultModelsList() {
        standardModelsList.clear();
        nsfwModelsList.clear();
        
        List<String> rawDefaults = new ArrayList<String>();
        rawDefaults.add("grapefruit");
        rawDefaults.add("Yiffy");
        rawDefaults.add("WAI-NSFW-illustrious-SDXL");
        rawDefaults.add("Grapefruit Hentai");
        rawDefaults.add("Nova Furry Pony");
        rawDefaults.add("526Mix-Animated");
        rawDefaults.add("Abyss OrangeMix");
        rawDefaults.add("AbyssOrangeMix-AfterDark");
        rawDefaults.add("BB95 Furry Mix v14");
        rawDefaults.add("BlenderMix Pony");
        rawDefaults.add("BweshMix");
        rawDefaults.add("CamelliaMix 2.5D");
        rawDefaults.add("Cetus-Mix");
        rawDefaults.add("Cheese Daddys Landscape Mix");
        rawDefaults.add("CyriousMix");
        rawDefaults.add("Ether Real Mix");
        rawDefaults.add("ExpMix Line");
        rawDefaults.add("GhostMix");
        rawDefaults.add("Hentai Diffusion");
        rawDefaults.add("HolyMix ILXL");
        rawDefaults.add("Lawlas's yiff mix");
        rawDefaults.add("MeinaMix");
        rawDefaults.add("MoonMix Fantasy");
        rawDefaults.add("NTR MIX IL-Noob XL");
        rawDefaults.add("Nova Furry XL");
        rawDefaults.add("Pastel Mix");
        rawDefaults.add("Rag Illustrious Mix");
        rawDefaults.add("Real Dos Mix");
        rawDefaults.add("Ri-Mix");
        rawDefaults.add("WAI-ANI-NSFW-PONYXL");
        rawDefaults.add("iCoMix Inpainting");
        rawDefaults.add("iCoMix");
        rawDefaults.add("majicMIX realistic");

        for (int i = 0; i < rawDefaults.size(); i++) {
            String m = rawDefaults.get(i);
            if (isModelNsfw(m)) {
                nsfwModelsList.add(m);
            } else {
                standardModelsList.add(m);
            }
        }
    }

    private boolean isModelNsfw(String modelName) {
        if (modelName == null) return false;
        String lower = modelName.toLowerCase();
        return lower.contains("nsfw") || 
               lower.contains("hentai") || 
               lower.contains("yiffy") || 
               lower.contains("furry") || 
               lower.contains("afterdark") || 
               lower.contains("lawlas") ||
               lower.contains("babes") ||
               lower.contains("uncensored");
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
                        
                        final List<String> fetchedAll = new ArrayList<String>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            String name = obj.optString("name", "");
                            String type = obj.optString("type", "image");
                            if (!name.isEmpty() && "image".equalsIgnoreCase(type)) {
                                fetchedAll.add(name);
                            }
                        }

                        if (!fetchedAll.isEmpty()) {
                            Collections.sort(fetchedAll);
                            
                            standardModelsList.clear();
                            nsfwModelsList.clear();

                            for (int i = 0; i < fetchedAll.size(); i++) {
                                String m = fetchedAll.get(i);
                                if (isModelNsfw(m)) {
                                    nsfwModelsList.add(m);
                                } else {
                                    standardModelsList.add(m);
                                }
                            }
                            
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (!standardModelsList.contains(selectedModel) && !nsfwModelsList.contains(selectedModel)) {
                                        if (!standardModelsList.isEmpty()) {
                                            selectedModel = standardModelsList.get(0);
                                        } else if (!nsfwModelsList.isEmpty()) {
                                            selectedModel = nsfwModelsList.get(0);
                                        }
                                    }
                                    rebuildModelListUI();
                                    int totalCount = standardModelsList.size() + nsfwModelsList.size();
                                    appendLog("✅ Успешно загружено моделей с сервера: " + totalCount);
                                    if (showToast) {
                                        Toast.makeText(MainActivity.this, "✅ Обновлено! Всего моделей: " + totalCount, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                            return;
                        }
                    }
                    throw new Exception("Пустой ответ сервера моделей.");
                } catch (Exception e) {
                    appendLog("⚠️ Не удалось загрузить модели из сети: " + e.getMessage() + ". Используем локальные списки.");
                    if (showToast) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "⚠️ Ошибка сети. Используются локальные модели.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    private void rebuildModelListUI() {
        if (standardModelsContainer == null || nsfwModelsContainer == null) return;
        standardModelsContainer.removeAllViews();
        nsfwModelsContainer.removeAllViews();
        modelRows.clear();

        // Построение стандартных моделей с нумерацией
        for (int i = 0; i < standardModelsList.size(); i++) {
            final String mName = standardModelsList.get(i);
            LinearLayout row = createModelRow(mName, i + 1, false);
            modelRows.add(row);
            standardModelsContainer.addView(row);
        }

        // Построение NSFW моделей с нумерацией
        for (int i = 0; i < nsfwModelsList.size(); i++) {
            final String mName = nsfwModelsList.get(i);
            LinearLayout row = createModelRow(mName, i + 1, true);
            modelRows.add(row);
            nsfwModelsContainer.addView(row);
        }
    }

    private LinearLayout createModelRow(final String mName, int indexNum, boolean isNsfw) {
        LinearLayout rowLayout = new LinearLayout(this);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(2, 2, 2, 2);
        rowLayout.setLayoutParams(rowParams);

        if (isNsfw) {
            rowLayout.setBackgroundColor(Color.parseColor("#33EF4444"));
        } else {
            rowLayout.setBackgroundColor(Color.TRANSPARENT);
        }

        Button mBtn = new Button(this);
        String labelDisplay = indexNum + ". " + mName + (isNsfw ? " 🔞" : "");
        mBtn.setText(labelDisplay);
        mBtn.setTextSize(11f);
        mBtn.setTypeface(Typeface.DEFAULT_BOLD);
        mBtn.setPadding(12, 10, 12, 10);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        );
        mBtn.setLayoutParams(btnParams);

        Button menuBtn = new Button(this);
        menuBtn.setText("⚙️ Меню");
        menuBtn.setTextSize(10f);
        menuBtn.setTypeface(Typeface.DEFAULT_BOLD);
        menuBtn.setPadding(8, 10, 8, 10);
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
        return rowLayout;
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
            
            // Ищем имя модели из текста кнопки (пропускаем порядковый номер и префиксы)
            String rawText = mBtn.getText().toString();
            String mName = rawText;
            if (rawText.contains(". ")) {
                mName = rawText.substring(rawText.indexOf(". ") + 2);
            }
            if (mName.contains(" 🔞")) {
                mName = mName.replace(" 🔞", "");
            }

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

    private void showModelMenuDialog(String modelName) {
        SharedPreferences prefs = getSharedPreferences("ModelPrefs", MODE_PRIVATE);
        boolean isNsfwModel = isModelNsfw(modelName);
        
        String modelDesc = "🤖 Идентификатор: " + modelName + "\n\n";
        if (isNsfwModel) {
            modelDesc += "⚠️ NSFW модель (18+). Оптимизирована для проработки анатомии и художественных эротических сцен.";
        } else {
            modelDesc += "✨ Стандартная модель. Отлично подходит для портретов, артов и фотореализма.";
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
        labelCfg.setText("🎯 CFG Scale (сила следования промпту):");
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
        labelSteps.setText("\n🔄 Steps (шаги генерации, рек. 20-30):");
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
        labelDenoising.setText("\n⚡ Denoising strength (сила изменения фото, 0.1 - 1.0):");
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

                    Toast.makeText(MainActivity.this, "✅ Настройки сохранены!", Toast.LENGTH_SHORT).show();
                    appendLog("Обновлены настройки модели " + modelName);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "❌ Ошибка в числах!", Toast.LENGTH_LONG).show();
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

        if (!hasCyrillic) return trimmed;

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
                        appendLog("Перевод: '" + trimmed + "' -> '" + translated + "'");
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
                    appendLog("✅ Ключ #" + (i+1) + " активен. Kudos: " + (int)kudos);
                }
            } catch (Exception e) {
                appendLog("⚠️ Сбой проверки ключа #" + (i+1));
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

    private void stopCurrentGeneration() {
        isGenerationCancelled = true;
        isGenerating = false;
        appendLog("⏹ Запрос на остановку генерации...");

        if (activeJobId != null && activeApiKey != null) {
            final String jobIdToCancel = activeJobId;
            final String apiKeyToCancel = activeApiKey;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Request cancelReq = new Request.Builder()
                                .url("https://stablehorde.net/api/v2/generate/status/" + jobIdToCancel)
                                .addHeader("apikey", apiKeyToCancel)
                                .addHeader("Client-Agent", "MonsterAI:1.0:android")
                                .delete()
                                .build();
                        httpClient.newCall(cancelReq).execute();
                        appendLog("⏹ Задача отменена на сервере.");
                    } catch (Exception e) {
                        appendLog("⚠️ Ошибка отмены на сервере: " + e.getMessage());
                    }
                }
            }).start();
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                statusText.setText("⏹ Генерация остановлена пользователем.");
                Toast.makeText(MainActivity.this, "⏹ Генерация остановлена.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isBitmapInvalidOrBlack(Bitmap bitmap) {
        if (bitmap == null) return true;
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w <= 0 || h <= 0) return true;

        int blackPixelsCount = 0;
        int totalChecked = 0;
        int stepX = Math.max(1, w / 20);
        int stepY = Math.max(1, h / 20);

        for (int x = 0; x < w; x += stepX) {
            for (int y = 0; y < h; y += stepY) {
                totalChecked++;
                int pixel = bitmap.getPixel(x, y);
                if (Color.red(pixel) < 10 && Color.green(pixel) < 10 && Color.blue(pixel) < 10) {
                    blackPixelsCount++;
                }
            }
        }
        return ((double) blackPixelsCount / totalChecked) > 0.95;
    }

    private void startGeneration() {
        if (selectedImageBytes == null) {
            Toast.makeText(this, "⚠️ Сначала выберите фото из галереи!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isGenerating) {
            Toast.makeText(this, "⏳ Генерация уже выполняется...", Toast.LENGTH_SHORT).show();
            return;
        }

        isGenerationCancelled = false;
        isGenerating = true;
        activeJobId = null;
        activeApiKey = null;

        final String userChangesRaw = inputChanges.getText().toString().trim();
        final boolean isNsfwActive = isModelNsfw(selectedModel);

        statusText.setText("⏳ Запуск генерации...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    appendLog("--- СТАРТ ГЕНЕРАЦИИ ---");
                    
                    String translatedChanges = translateToEnglish(userChangesRaw);
                    if (isGenerationCancelled) return;

                    List<String> workingKeys = getPrioritizedWorkingKeys();
                    if (isGenerationCancelled) return;

                    Bitmap originalBmp = BitmapFactory.decodeByteArray(selectedImageBytes, 0, selectedImageBytes.length);
                    Bitmap squareBmp = makeSquareBitmap(originalBmp);

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    squareBmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    String base64Image = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);

                    String fullPrompt = "";
                    String negativePrompt = "";

                    if (isNsfwActive) {
                        String nsfwCore = "masterpiece, best quality, highly detailed 2.5d anime illustration, beautiful detailed face, intricate clothing textures, perfect anatomy, accurate lighting, vibrant colors, expressive body, gorgeous physique, sensual curves, fully detailed skin texture, artistic nudity, uncovered body, aesthetic explicit anatomy";
                        if (!translatedChanges.isEmpty()) nsfwCore += ", " + translatedChanges;
                        negativePrompt = "(lowres, bad anatomy, bad hands:1.5, poorly drawn hands:1.5, missing fingers:1.5, extra fingers:1.5, mutated hands:1.5, disfigured, text, ugly, blurry, censored)";
                        fullPrompt = nsfwCore + " ### " + negativePrompt;
                    } else {
                        String standardCore = "masterpiece, best quality, ultra-detailed photo, sharp focus, professional studio lighting, 8k resolution, photorealistic";
                        if (!translatedChanges.isEmpty()) standardCore += ", " + translatedChanges;
                        negativePrompt = "(lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, cropped, worst quality, low quality)";
                        fullPrompt = standardCore + " ### " + negativePrompt;
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
                    
                    // Добавляем остальные модели для надежности
                    for (int i = 0; i < standardModelsList.size(); i++) {
                        String m = standardModelsList.get(i);
                        if (!modelsToTry.contains(m)) modelsToTry.add(m);
                    }
                    for (int i = 0; i < nsfwModelsList.size(); i++) {
                        String m = nsfwModelsList.get(i);
                        if (!modelsToTry.contains(m)) modelsToTry.add(m);
                    }

                    for (int mIdx = 0; mIdx < modelsToTry.size(); mIdx++) {
                        if (successGenerated || isGenerationCancelled) break;
                        final String modelName = modelsToTry.get(mIdx);

                        for (int kIdx = 0; kIdx < workingKeys.size(); kIdx++) {
                            if (successGenerated || isGenerationCancelled) break;
                            String apiKey = workingKeys.get(kIdx);
                            activeApiKey = apiKey;

                            try {
                                appendLog("👉 Пробуем модель: [" + modelName + "]");
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        statusText.setText("⏳ Генерация (модель: " + modelName + ")...");
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
                                    continue;
                                }
                                if (code == 400 || !response.isSuccessful() || response.body() == null) {
                                    continue;
                                }

                                String respStr = response.body().string();
                                JSONObject respJson = new JSONObject(respStr);
                                String jobId = respJson.optString("id", null);
                                activeJobId = jobId;

                                if (jobId == null) continue;

                                appendLog("✅ Задача принята. Job ID: " + jobId);
                                String statusUrl = "https://stablehorde.net/api/v2/generate/status/" + jobId;
                                boolean jobDone = false;
                                String imgUrl = null;

                                for (int attempt = 0; attempt < 120; attempt++) {
                                    if (isGenerationCancelled) break;
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
                                            final int waitTime = stJson.optInt("wait_time", 0);
                                            appendLog("⏳ Очередь: " + qPos + ", Ждать: " + waitTime + "с");
                                        }
                                    }
                                }

                                if (isGenerationCancelled) break;

                                if (jobDone && imgUrl != null && !imgUrl.isEmpty()) {
                                    Request imgReq = new Request.Builder().url(imgUrl).get().build();
                                    Response imgResp = httpClient.newCall(imgReq).execute();
                                    if (imgResp.isSuccessful() && imgResp.body() != null) {
                                        byte[] tempBytes = imgResp.body().bytes();
                                        Bitmap decodedBitmap = BitmapFactory.decodeByteArray(tempBytes, 0, tempBytes.length);

                                        if (isBitmapInvalidOrBlack(decodedBitmap)) {
                                            appendLog("⚠️ Модель вернула черный квадрат, пробуем другую...");
                                            continue;
                                        }

                                        resultBytes = tempBytes;
                                        successGenerated = true;
                                        break;
                                    }
                                }
                            } catch (Exception ex) {
                                // Ошибка итерации
                            }
                        }
                    }

                    if (isGenerationCancelled) {
                        isGenerating = false;
                        return;
                    }

                    if (!successGenerated || resultBytes == null) {
                        throw new RuntimeException("Все доступные модели перегружены или отклонили запрос.");
                    }

                    final Bitmap finalBmp = BitmapFactory.decodeByteArray(resultBytes, 0, resultBytes.length);
                    saveBitmapToGallery(finalBmp, "MonsterAI_" + System.currentTimeMillis() + ".jpg");

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageBitmap(finalBmp);
                            statusText.setText("✅ Успешно сгенерировано и сохранено!");
                            Toast.makeText(MainActivity.this, "Фото сохранено в галерею!", Toast.LENGTH_LONG).show();
                            appendLog("✨ Готово! Фото сохранено.");
                            isGenerating = false;
                        }
                    });

                } catch (final Exception e) {
                    isGenerating = false;
                    if (!isGenerationCancelled) {
                        appendLog("❌ ОШИБКА: " + e.getMessage());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                statusText.setText("❌ Ошибка генерации");
                                Toast.makeText(MainActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
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
        } catch (Exception e) {
            appendLog("❌ Ошибка сохранения: " + e.getMessage());
        }
    }
}