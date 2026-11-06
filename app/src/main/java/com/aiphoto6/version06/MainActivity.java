package com.aiphoto6.version06;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
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
import android.widget.ProgressBar;
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
    private static final String CHANNEL_ID = "monster_ai_notifications";
    
    private ImageView imageView;
    private TextView statusText;
    private EditText inputChanges;
    private TextView logConsole;
    private ScrollView logScrollView;
    private byte[] selectedImageBytes;
    private Uri selectedImageUri;
    
    private Handler handler = new Handler(Looper.getMainLooper());
    
    private String selectedModel = "grapefruit";
    private List<String> hordeKeys = new ArrayList<String>();
    private List<String> allAvailableModels = new ArrayList<String>();

    private Button btnModelSelector;
    private LinearLayout progressStatusLayout;
    private TextView progressStatusText;
    private ProgressBar progressBar;
    private Button btnStopGen;
    private Button btnStartGen;

    private volatile boolean isGenerationCancelled = false;
    private String activeJobId = null;
    private String activeApiKey = null;
    
    private OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createNotificationChannel();
        checkNotificationPermission();
        
        SharedPreferences prefs = getSharedPreferences("ModelPrefs", MODE_PRIVATE);
        selectedModel = prefs.getString("active_selected_model", "grapefruit");

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
        labelModel.setText("🤖 Активная модель ИИ (нажмите для выбора и настроек):");
        labelModel.setTextColor(Color.parseColor("#F8FAFC"));
        labelModel.setTextSize(12f);
        labelModel.setPadding(0, 10, 0, 5);
        rootLayout.addView(labelModel);

        btnModelSelector = createStyledButton("🤖 Загрузка моделей...", "#1E293B");
        btnModelSelector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isFinishing()) {
                    showAdvancedModelSelectionDialog();
                }
            }
        });
        rootLayout.addView(btnModelSelector);

        progressStatusLayout = new LinearLayout(this);
        progressStatusLayout.setOrientation(LinearLayout.VERTICAL);
        progressStatusLayout.setBackgroundColor(Color.parseColor("#1E293B"));
        progressStatusLayout.setPadding(20, 20, 20, 20);
        progressStatusLayout.setVisibility(View.GONE);
        LinearLayout.LayoutParams pLayoutParam = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        pLayoutParam.setMargins(0, 0, 0, 10);
        progressStatusLayout.setLayoutParams(pLayoutParam);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        progressStatusLayout.addView(progressBar);

        progressStatusText = new TextView(this);
        progressStatusText.setText("Генерация...");
        progressStatusText.setTextColor(Color.parseColor("#38BDF8"));
        progressStatusText.setTextSize(12f);
        progressStatusText.setPadding(0, 10, 0, 10);
        progressStatusLayout.addView(progressStatusText);

        btnStopGen = createStyledButton("⏹ Экстренная остановка (сохранить фото)", "#DC2626");
        btnStopGen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopCurrentGeneration();
            }
        });
        progressStatusLayout.addView(btnStopGen);
        rootLayout.addView(progressStatusLayout);

        btnStartGen = createStyledButton("🚀 Запустить генерацию", "#059669");
        btnStartGen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGeneration();
            }
        });
        rootLayout.addView(btnStartGen);

        TextView labelLogs = new TextView(this);
        labelLogs.setText("📋 Отладочные логи:");
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

        Button btnCopyLogs = createStyledButton("📋 Копировать", "#475569");
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
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
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
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

        if (savedInstanceState != null) {
            String restoredModel = savedInstanceState.getString("selectedModel", null);
            if (restoredModel != null) {
                selectedModel = restoredModel;
            }
            inputChanges.setText(savedInstanceState.getString("inputText", ""));
            logConsole.setText(savedInstanceState.getString("logText", ""));
            
            String uriStr = savedInstanceState.getString("selectedImageUri", null);
            if (uriStr != null) {
                selectedImageUri = Uri.parse(uriStr);
                loadImageFromUri(selectedImageUri);
            }

            ArrayList<String> savedModels = savedInstanceState.getStringArrayList("allModels");
            if (savedModels != null && !savedModels.isEmpty()) {
                allAvailableModels = savedModels;
                updateModelButtonLabel();
            } else {
                fetchOnlineModelsAsync(false);
            }
        } else {
            updateModelButtonLabel();
            fetchOnlineModelsAsync(false);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Monster AI Notifications";
            String description = "Уведомления о завершении генерации изображений";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 102);
            }
        }
    }

    private void showGenerationCompleteNotification() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                return; 
            }
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); // <-- ИСПРАВЛЕНО ЗДЕСЬ
        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 31) {
            pendingIntentFlags |= PendingIntent.FLAG_MUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("🔥 Monster AI Horde")
                    .setContentText("Генерация завершена! Фото сохранено в галерею.")
                    .setSmallIcon(android.R.drawable.ic_menu_camera)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            notificationManager.notify(1, builder.build());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("selectedModel", selectedModel);
        outState.putString("inputText", inputChanges.getText().toString());
        outState.putString("logText", logConsole.getText().toString());
        outState.putStringArrayList("allModels", new ArrayList<String>(allAvailableModels));
        if (selectedImageUri != null) {
            outState.putString("selectedImageUri", selectedImageUri.toString());
        }
    }

    private void loadImageFromUri(Uri uri) {
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
            appendLog("⚠️ Не удалось восстановить фото по Uri: " + e.getMessage());
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
               lower.contains("sex");
    }

    private String getModelDescription(String modelName) {
        String lower = modelName.toLowerCase();
        if (lower.contains("nsfw") || lower.contains("hentai") || lower.contains("yiffy") || lower.contains("lawlas")) {
            return "🔞 Специализированная модель для создания эротического контента, аниме-иллюстраций и проработки сложной анатомии с высокой детализацией кожи и тел.";
        } else if (lower.contains("furry") || lower.contains("pony")) {
            return "🦊🐾 Модель, оптимизированная под стилизованную графику, фурри-арты и персонажей в стиле Pony/Animation с яркими палитрами.";
        } else if (lower.contains("realistic") || lower.contains("real") || lower.contains("dos")) {
            return "📸 Фотореалистичная модель. Отлично подходит для создания реалистичных портретов, природных пейзажей, правильного освещения и студийной фотографии.";
        } else if (lower.contains("anime") || lower.contains("mix") || lower.contains("grapefruit") || lower.contains("meinamix") || lower.contains("ghostmix")) {
            return "🎨 Художественная 2.5D / аниме модель общего назначения. Универсальна для создания качественных персонажей, концепт-артов и стилизованных портретов.";
        } else {
            return "🤖 Сбалансированная нейросетевая модель общей категории для генерации качественных изображений по вашему текстовому запросу и исходному фото.";
        }
    }

    private void updateModelButtonLabel() {
        if (btnModelSelector != null) {
            boolean isNsfw = isModelNsfw(selectedModel);
            btnModelSelector.setText("🤖 Активная: " + selectedModel + (isNsfw ? " 🔞" : "") + " [Моделей в базе: " + allAvailableModels.size() + "]");
            SharedPreferences.Editor editor = getSharedPreferences("ModelPrefs", MODE_PRIVATE).edit();
            editor.putString("active_selected_model", selectedModel);
            editor.apply();
        }
    }

    private void fetchOnlineModelsAsync(final boolean showToast) {
        appendLog("🌐 Запрос актуального списка моделей с сервера Stable Horde...");
        if (showToast && !isFinishing()) {
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
                    if (isFinishing()) return;

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
                                    if (isFinishing()) return;
                                    allAvailableModels = fetchedModels;
                                    if (!allAvailableModels.contains(selectedModel) && !allAvailableModels.isEmpty()) {
                                        selectedModel = allAvailableModels.get(0);
                                    }
                                    updateModelButtonLabel();
                                    appendLog("✅ Успешно загружено активных моделей с сервера: " + allAvailableModels.size());
                                    if (showToast) {
                                        Toast.makeText(MainActivity.this, "✅ Список обновлен (" + allAvailableModels.size() + " моделей)", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                            return;
                        }
                    }
                    throw new Exception("Пустой ответ сервера моделей.");
                } catch (Exception e) {
                    if (isFinishing()) return;
                    appendLog("⚠️ Не удалось загрузить модели из сети: " + e.getMessage());
                    if (showToast) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (isFinishing()) return;
                                Toast.makeText(MainActivity.this, "⚠️ Ошибка сети при обновлении моделей.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    private void showModelMenuDialog(final String modelName) {
        if (isFinishing()) return;
        SharedPreferences prefs = getSharedPreferences("ModelPrefs", MODE_PRIVATE);

        float savedCfg = prefs.getFloat(modelName + "_cfg", 7.5f);
        int savedSteps = prefs.getInt(modelName + "_steps", 25);
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
        descView.setText("📖 Описание модели:\n" + getModelDescription(modelName) + "\n");
        descView.setTextColor(Color.parseColor("#CBD5E1"));
        descView.setTextSize(12f);
        descView.setPadding(0, 0, 0, 15);
        dialogLayout.addView(descView);

        TextView labelCfg = new TextView(this);
        labelCfg.setText("🎯 Сила совпадения с промптом (CFG Scale):\nВыше значение — строже следование вашему запросу (рекомендуется 7-8.5).");
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
        labelSteps.setText("\n🔄 Шаги генерации (Steps):\nКоличество итераций прорисовки (рекомендуется 20-30).");
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
        labelDenoising.setText("\n⚡ Сила изменения исходного фото (Denoising, 0.1 - 1.0):\nБлиже к 1.0 — полная переработка под запрос, ближе к 0.1 — сохранение оригинала.");
        labelDenoising.setTextColor(Color.parseColor("#F8FAFC"));
        labelDenoising.setTextSize(11f);
        dialogLayout.addView(labelDenoising);

        final EditText inputDenoising = new EditText(this);
        inputDenoising.setText(String.valueOf(savedDenoising));
        inputDenoising.setTextColor(Color.BLACK);
        inputDenoising.setBackgroundColor(Color.WHITE);
        inputDenoising.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        dialogLayout.addView(inputDenoising);

        Button saveBtn = createStyledButton("💾 Сохранить настройки модели", "#059669");
        final AlertDialog.Builder cfgBuilder = new AlertDialog.Builder(this);
        cfgBuilder.setView(dialogScroll);
        cfgBuilder.setPositiveButton("Закрыть", null);
        final AlertDialog cfgDialog = cfgBuilder.create();

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
                    cfgDialog.dismiss();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "❌ Ошибка в числах! Проверьте ввод.", Toast.LENGTH_LONG).show();
                }
            }
        });
        dialogLayout.addView(saveBtn);

        cfgDialog.show();
    }

    private void addModelCategorySection(LinearLayout parentLayout, String headerText, String headerColorHex, List<String> modelList, boolean isNsfwSection, final AlertDialog[] dialogHolder) {
        if (modelList.isEmpty()) return;

        TextView header = new TextView(this);
        header.setText("\n" + headerText + " (" + modelList.size() + "):");
        header.setTextColor(Color.parseColor(headerColorHex));
        header.setTextSize(13f);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        parentLayout.addView(header);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < modelList.size(); i++) {
            final String modelName = modelList.get(i);
            final int indexNum = i + 1;

            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rParams.setMargins(0, 3, 0, 3);
            rowLayout.setLayoutParams(rParams);

            Button mBtn = new Button(this);
            boolean isSelected = modelName.equals(selectedModel);
            mBtn.setText(indexNum + ". " + modelName + (isNsfwSection ? " 🔞" : "") + (isSelected ? " ✅ [АКТИВНА]" : ""));
            mBtn.setTextSize(12f);
            mBtn.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            mBtn.setPadding(15, 12, 15, 12);
            
            if (isSelected) {
                mBtn.setBackgroundColor(Color.parseColor("#059669"));
                mBtn.setTextColor(Color.WHITE);
            } else {
                if (isNsfwSection) {
                    mBtn.setBackgroundColor(Color.parseColor("#44EF4444"));
                    mBtn.setTextColor(Color.parseColor("#FCA5A5"));
                } else {
                    mBtn.setBackgroundColor(Color.parseColor("#1E293B"));
                    mBtn.setTextColor(Color.parseColor("#CBD5E1"));
                }
            }
            
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            mBtn.setLayoutParams(bp);
            mBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedModel = modelName;
                    updateModelButtonLabel();
                    appendLog("Выбрана модель: " + selectedModel);
                    if (dialogHolder[0] != null) dialogHolder[0].dismiss();
                }
            });

            Button settingsBtn = new Button(this);
            settingsBtn.setText("⚙️ Меню");
            settingsBtn.setTextSize(11f);
            settingsBtn.setBackgroundColor(Color.parseColor("#334155"));
            settingsBtn.setTextColor(Color.parseColor("#F8FAFC"));
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            sp.setMargins(5, 0, 0, 0);
            settingsBtn.setLayoutParams(sp);
            settingsBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showModelMenuDialog(modelName);
                }
            });

            rowLayout.addView(mBtn);
            rowLayout.addView(settingsBtn);
            container.addView(rowLayout);
        }
        parentLayout.addView(container);
    }

    private void showAdvancedModelSelectionDialog() {
        if (isFinishing()) return;
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        ScrollView dialogScroll = new ScrollView(this);
        LinearLayout mainDialogLayout = new LinearLayout(this);
        mainDialogLayout.setOrientation(LinearLayout.VERTICAL);
        mainDialogLayout.setPadding(25, 25, 25, 25);
        mainDialogLayout.setBackgroundColor(Color.parseColor("#0F172A"));
        dialogScroll.addView(mainDialogLayout);

        TextView dialogTitle = new TextView(this);
        dialogTitle.setText("🤖 Каталог моделей (Всего активных: " + allAvailableModels.size() + ")");
        dialogTitle.setTextColor(Color.parseColor("#38BDF8"));
        dialogTitle.setTextSize(16f);
        dialogTitle.setTypeface(Typeface.DEFAULT_BOLD);
        dialogTitle.setPadding(0, 0, 0, 15);
        mainDialogLayout.addView(dialogTitle);

        Button btnRefreshInDialog = createStyledButton("🔄 Обновить список с сервера", "#0284C7");
        btnRefreshInDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchOnlineModelsAsync(true);
            }
        });
        mainDialogLayout.addView(btnRefreshInDialog);

        List<String> realisticModels = new ArrayList<String>();
        List<String> animeModels = new ArrayList<String>();
        List<String> furryModels = new ArrayList<String>();
        List<String> otherNormalModels = new ArrayList<String>();
        List<String> nsfwModels = new ArrayList<String>();

        for (int i = 0; i < allAvailableModels.size(); i++) {
            String m = allAvailableModels.get(i);
            String lower = m.toLowerCase();
            if (isModelNsfw(m)) {
                nsfwModels.add(m);
            } else if (lower.contains("furry") || lower.contains("pony")) {
                furryModels.add(m);
            } else if (lower.contains("realistic") || lower.contains("real") || lower.contains("dos")) {
                realisticModels.add(m);
            } else if (lower.contains("anime") || lower.contains("mix") || lower.contains("grapefruit") || lower.contains("meinamix") || lower.contains("ghostmix")) {
                animeModels.add(m);
            } else {
                otherNormalModels.add(m);
            }
        }

        final AlertDialog[] dialogHolder = new AlertDialog[1];

        addModelCategorySection(mainDialogLayout, "📸 Фотореалистичные модели", "#38BDF8", realisticModels, false, dialogHolder);
        addModelCategorySection(mainDialogLayout, "🎨 Аниме / 2.5D модели", "#34D399", animeModels, false, dialogHolder);
        addModelCategorySection(mainDialogLayout, "🦊🐾 Фурри / Pony модели", "#FBBF24", furryModels, false, dialogHolder);
        addModelCategorySection(mainDialogLayout, "🤖 Общие модели", "#94A3B8", otherNormalModels, false, dialogHolder);
        addModelCategorySection(mainDialogLayout, "🔞 NSFW модели", "#FCA5A5", nsfwModels, true, dialogHolder);

        builder.setView(dialogScroll);
        builder.setPositiveButton("Закрыть", null);
        dialogHolder[0] = builder.create();
        dialogHolder[0].show();
    }

    private void appendLog(final String text) {
        if (isFinishing()) return;
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) return;
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
                statusText.setText("✅ Фото загружено!");
                appendLog("Фото загружено. Байтов: " + selectedImageBytes.length);
            } catch (Exception e) {
                appendLog("❌ Ошибка загрузки фото: " + e.getMessage());
                Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private Bitmap makeSquareBitmap(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        if (width == height) return source;
        int maxDim = Math.max(width, height);
        if (maxDim > 1024) {
            float scale = 1024f / maxDim;
            source = Bitmap.createScaledBitmap(source, (int)(width * scale), (int)(height * scale), true);
            width = source.getWidth();
            height = source.getHeight();
            maxDim = 1024;
        }

        Bitmap squareBitmap = Bitmap.createBitmap(maxDim, maxDim, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(squareBitmap);
        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(source, (maxDim - width) / 2, (maxDim - height) / 2, null);
        return squareBitmap;
    }

    private String translateToEnglish(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        String trimmed = text.trim();
        boolean hasCyrillic = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if ((c >= '\u0400' && c <= '\u04FF')) {
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
            if (isFinishing()) break;
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
                    appendLog("✅ Ключ #" + (i+1) + " Активен. Kudos: " + (int)kudos);
                }
            } catch (Exception e) {
                // пропускаем неактивные
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
        appendLog("⏹ Запрос на экстренную остановку генерации...");

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
                        // ignore
                    }
                }
            }).start();
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) return;
                progressStatusLayout.setVisibility(View.GONE);
                btnStartGen.setVisibility(View.VISIBLE);
                updateModelButtonLabel();
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

        isGenerationCancelled = false;
        activeJobId = null;
        activeApiKey = null;

        final String userChangesRaw = inputChanges.getText().toString().trim();
        final boolean isNsfwActive = isModelNsfw(selectedModel);

        btnStartGen.setVisibility(View.GONE);
        progressStatusLayout.setVisibility(View.VISIBLE);
        progressStatusText.setText("Подготовка задачи...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    appendLog("--- СТАРТ ГЕНЕРАЦИИ ---");
                    
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (isFinishing()) return;
                            progressStatusText.setText("Перевод пожеланий...");
                        }
                    });
                    
                    String translatedChanges = translateToEnglish(userChangesRaw);
                    if (isGenerationCancelled || isFinishing()) return;

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (isFinishing()) return;
                            progressStatusText.setText("Проверка ключей доступа...");
                        }
                    });
                    List<String> workingKeys = getPrioritizedWorkingKeys();
                    if (isGenerationCancelled || isFinishing()) return;

                    Bitmap originalBmp = BitmapFactory.decodeByteArray(selectedImageBytes, 0, selectedImageBytes.length);
                    Bitmap squareBmp = makeSquareBitmap(originalBmp);

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    squareBmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    String base64Image = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);

                    String fullPrompt = "";
                    String negativePrompt = "";

                    if (isNsfwActive) {
                        String baseStyle = "masterpiece, best quality, 3d render:1.3, cgi, Unreal Engine 5 render, octane render, extremely detailed 3d character, raw studio lighting, subsurface scattering skin texture, flawless erotic body shape, detailed nipples, wet skin sheen, exquisite curves, full nudity, unmasked intimate details, anatomical accuracy, explicit sensual pose, highly detailed genitalia, soft volumetric lighting, dramatic cinematic shadows, 8k resolution";
                        String nsfwCore = "";
                        
                        if (!translatedChanges.isEmpty()) {
                            nsfwCore = translatedChanges + ", " + baseStyle;
                            appendLog("🔥 Промпт пользователя перенесен в начало для высшего приоритета.");
                        } else {
                            nsfwCore = baseStyle;
                        }

                        negativePrompt = "(lowres, bad anatomy:1.4, bad hands:1.5, poorly drawn hands:1.5, missing fingers:1.5, extra fingers:1.5, mutated hands:1.5, disfigured, text, ugly, blurry, censored, mosaic, pixelated, bar censor, black bars)";
                        fullPrompt = nsfwCore + " ### " + negativePrompt;
                        appendLog("🔥 Активен максимально откровенный NSFW-промпт высочайшего качества.");
                    } else {
                        String baseStyle = "masterpiece, best quality, ultra-detailed photo, sharp focus, professional studio lighting, 8k resolution, photorealistic";
                        String standardCore = "";
                        
                        if (!translatedChanges.isEmpty()) {
                            standardCore = translatedChanges + ", " + baseStyle;
                            appendLog("✨ Промпт пользователя перенесен в начало для стандартного режима.");
                        } else {
                            standardCore = baseStyle;
                        }

                        negativePrompt = "(lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, cropped, worst quality, low quality)";
                        fullPrompt = standardCore + " ### " + negativePrompt;
                        appendLog("✨ Активен стандартный фото-промпт.");
                    }

                    SharedPreferences prefs = getSharedPreferences("ModelPrefs", MODE_PRIVATE);
                    float cfg = prefs.getFloat(selectedModel + "_cfg", 7.5f);
                    int steps = prefs.getInt(selectedModel + "_steps", 25);
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
                        if (!modelsToTry.contains(m)) modelsToTry.add(m);
                    }

                    for (int mIdx = 0; mIdx < modelsToTry.size(); mIdx++) {
                        if (successGenerated || isGenerationCancelled || isFinishing()) break;
                        final String modelName = modelsToTry.get(mIdx);

                        for (int kIdx = 0; kIdx < workingKeys.size(); kIdx++) {
                            if (successGenerated || isGenerationCancelled || isFinishing()) break;
                            String apiKey = workingKeys.get(kIdx);
                            activeApiKey = apiKey;

                            try {
                                appendLog("👉 Модель: [" + modelName + "], Ключ: " + apiKey.substring(0, 5) + "...");
                                
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (isFinishing()) return;
                                        progressStatusText.setText("Модель: " + modelName);
                                        if (btnModelSelector != null) {
                                            btnModelSelector.setText("🔄 Пробуем: " + modelName + " 🔞");
                                        }
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
                                
                                if (code == 401 || code == 402 || code == 403 || code == 429) continue;
                                if (code == 400 || !response.isSuccessful() || response.body() == null) continue;

                                String respStr = response.body().string();
                                JSONObject respJson = new JSONObject(respStr);
                                String jobId = respJson.optString("id", null);
                                activeJobId = jobId;

                                if (jobId == null) continue;

                                appendLog("✅ Задача принята. ID: " + jobId.substring(0, 6) + "... Ожидаем воркер...");
                                boolean jobDone = false;
                                String imgUrl = null;

                                for (int attempt = 0; attempt < 120; attempt++) {
                                    if (isGenerationCancelled || isFinishing()) break;
                                    Thread.sleep(5000);

                                    Request statusReq = new Request.Builder()
                                            .url("https://stablehorde.net/api/v2/generate/status/" + jobId)
                                            .addHeader("Client-Agent", "MonsterAI:1.0:android")
                                            .get()
                                            .build();
                                    Response statRespReal = httpClient.newCall(statusReq).execute();
                                    if (statRespReal.isSuccessful() && statRespReal.body() != null) {
                                        JSONObject stJson = new JSONObject(statRespReal.body().string());
                                        if (stJson.optBoolean("faulted", false)) break;
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
                                            handler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (isFinishing()) return;
                                                    progressStatusText.setText("Очередь: " + qPos + " | Ждать: " + waitTime + "с");
                                                }
                                            });
                                        }
                                    }
                                }

                                if (isGenerationCancelled || isFinishing()) break;

                                if (jobDone && imgUrl != null && !imgUrl.isEmpty()) {
                                    appendLog("📥 Скачивание готового изображения...");
                                    Request imgReq = new Request.Builder().url(imgUrl).get().build();
                                    Response imgResp = httpClient.newCall(imgReq).execute();
                                    if (imgResp.isSuccessful() && imgResp.body() != null) {
                                        byte[] tempBytes = imgResp.body().bytes();
                                        Bitmap decodedBitmap = BitmapFactory.decodeByteArray(tempBytes, 0, tempBytes.length);

                                        if (isBitmapInvalidOrBlack(decodedBitmap)) {
                                            appendLog("⚠️ Модель вернула пустой квадрат. Пробуем дальше...");
                                            continue;
                                        }

                                        resultBytes = tempBytes;
                                        successGenerated = true;
                                        break;
                                    }
                                }
                            } catch (Exception ex) {
                                appendLog("🔴 Ошибка: " + ex.getMessage());
                            }
                        }
                    }

                    if (isGenerationCancelled || isFinishing()) return;

                    if (!successGenerated || resultBytes == null) {
                        throw new RuntimeException("Все модели перегружены или вернули пустой результат.");
                    }

                    final Bitmap finalBmp = BitmapFactory.decodeByteArray(resultBytes, 0, resultBytes.length);
                    saveBitmapToGallery(finalBmp, "MonsterAI_" + System.currentTimeMillis() + ".jpg");

                    showGenerationCompleteNotification();

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (isFinishing()) return;
                            progressStatusLayout.setVisibility(View.GONE);
                            btnStartGen.setVisibility(View.VISIBLE);
                            updateModelButtonLabel();
                            imageView.setImageBitmap(finalBmp);
                            statusText.setText("✅ Успешно сгенерировано и сохранено!");
                            Toast.makeText(MainActivity.this, "Фото сохранено в Pictures/MonsterAI", Toast.LENGTH_LONG).show();
                            appendLog("✨ Готово! Фото сохранено в галерею.");
                        }
                    });

                } catch (final Exception e) {
                    if (!isGenerationCancelled && !isFinishing()) {
                        appendLog("❌ ОШИБКА: " + e.getMessage());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (isFinishing()) return;
                                progressStatusLayout.setVisibility(View.GONE);
                                btnStartGen.setVisibility(View.VISIBLE);
                                updateModelButtonLabel();
                                statusText.setText("❌ Ошибка: " + e.getMessage());
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
                    appendLog("Сохранено в Галерею: " + fileName);
                }
            }
        } catch (Exception e) {
            appendLog("❌ Ошибка сохранения: " + e.getMessage());
        }
    }
}