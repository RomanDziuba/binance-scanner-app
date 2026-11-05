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
    
    private ImageView imageView;
    private TextView statusText;
    private EditText inputChanges;
    private TextView logConsole;
    private ScrollView logScrollView;
    private byte[] selectedImageBytes;
    
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

    // Элементы управления отменой генерации
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
        
        // Ключи доступа AI Horde
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
        labelModel.setText("🤖 Активная модель ИИ:");
        labelModel.setTextColor(Color.parseColor("#F8FAFC"));
        labelModel.setTextSize(12f);
        labelModel.setPadding(0, 10, 0, 5);
        rootLayout.addView(labelModel);

        // Кнопка-селектор моделей с отображением текущей модели и количества
        btnModelSelector = createStyledButton("🤖 Загрузка моделей...", "#1E293B");
        btnModelSelector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAdvancedModelSelectionDialog();
            }
        });
        rootLayout.addView(btnModelSelector);

        // Панель прогресса и статуса (не блокирует интерфейс, кнопка стоп всегда доступна)
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

        // Кнопка запуска генерации
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

        // Восстановление данных при повороте или первичный запуск
        if (savedInstanceState != null) {
            selectedModel = savedInstanceState.getString("selectedModel", "grapefruit");
            inputChanges.setText(savedInstanceState.getString("inputText", ""));
            logConsole.setText(savedInstanceState.getString("logText", ""));
            selectedImageBytes = savedInstanceState.getByteArray("imageBytes");
            if (selectedImageBytes != null) {
                Bitmap bmp = BitmapFactory.decodeByteArray(selectedImageBytes, 0, selectedImageBytes.length);
                imageView.setImageBitmap(bmp);
            }
            ArrayList<String> savedModels = savedInstanceState.getStringArrayList("allModels");
            if (savedModels != null && !savedModels.isEmpty()) {
                allAvailableModels = savedModels;
                updateModelButtonLabel();
            } else {
                fetchOnlineModelsAsync(false);
            }
        } else {
            fetchOnlineModelsAsync(false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("selectedModel", selectedModel);
        outState.putString("inputText", inputChanges.getText().toString());
        outState.putString("logText", logConsole.getText().toString());
        outState.putStringArrayList("allModels", new ArrayList<String>(allAvailableModels));
        if (selectedImageBytes != null) {
            outState.putByteArray("imageBytes", selectedImageBytes);
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

    private void updateModelButtonLabel() {
        if (btnModelSelector != null) {
            boolean isNsfw = isModelNsfw(selectedModel);
            btnModelSelector.setText("🤖 Активная: " + selectedModel + (isNsfw ? " 🔞" : "") + " [Всего в базе: " + allAvailableModels.size() + "]");
        }
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
                    appendLog("⚠️ Не удалось загрузить модели из сети: " + e.getMessage());
                    if (showToast) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "⚠️ Ошибка сети при обновлении моделей.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    // Продвинутое меню выбора моделей: два отдельных скроллируемых блока + кнопка обновления
    private void showAdvancedModelSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        ScrollView dialogScroll = new ScrollView(this);
        LinearLayout mainDialogLayout = new LinearLayout(this);
        mainDialogLayout.setOrientation(LinearLayout.VERTICAL);
        mainDialogLayout.setPadding(25, 25, 25, 25);
        mainDialogLayout.setBackgroundColor(Color.parseColor("#0F172A"));
        dialogScroll.addView(mainDialogLayout);

        TextView dialogTitle = new TextView(this);
        dialogTitle.setText("🤖 Каталог моделей (Всего: " + allAvailableModels.size() + ")");
        dialogTitle.setTextColor(Color.parseColor("#38BDF8"));
        dialogTitle.setTextSize(16f);
        dialogTitle.setTypeface(Typeface.DEFAULT_BOLD);
        dialogTitle.setPadding(0, 0, 0, 15);
        mainDialogLayout.addView(dialogTitle);

        // Кнопка обновления моделей прямо в меню
        Button btnRefreshInDialog = createStyledButton("🔄 Обновить список моделей с сервера", "#0284C7");
        btnRefreshInDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchOnlineModelsAsync(true);
            }
        });
        mainDialogLayout.addView(btnRefreshInDialog);

        // Разделение моделей на Обычные и NSFW
        List<String> normalModels = new ArrayList<String>();
        List<String> nsfwModels = new ArrayList<String>();

        for (int i = 0; i < allAvailableModels.size(); i++) {
            String m = allAvailableModels.get(i);
            if (isModelNsfw(m)) {
                nsfwModels.add(m);
            } else {
                normalModels.add(m);
            }
        }

        // Блок 1: Обычные модели
        TextView headerNormal = new TextView(this);
        headerNormal.setText("\n🟢 Стандартные модели (" + normalModels.size() + "):");
        headerNormal.setTextColor(Color.parseColor("#34D399"));
        headerNormal.setTextSize(13f);
        headerNormal.setTypeface(Typeface.DEFAULT_BOLD);
        mainDialogLayout.addView(headerNormal);

        LinearLayout normalContainer = new LinearLayout(this);
        normalContainer.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < normalModels.size(); i++) {
            final String modelName = normalModels.get(i);
            final int indexNum = i + 1;
            Button mBtn = new Button(this);
            mBtn.setText(indexNum + ". " + modelName);
            mBtn.setTextSize(12f);
            mBtn.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            mBtn.setPadding(15, 12, 15, 12);
            
            if (modelName.equals(selectedModel)) {
                mBtn.setBackgroundColor(Color.parseColor("#059669"));
                mBtn.setTextColor(Color.WHITE);
            } else {
                mBtn.setBackgroundColor(Color.parseColor("#1E293B"));
                mBtn.setTextColor(Color.parseColor("#CBD5E1"));
            }
            
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            bp.setMargins(0, 3, 0, 3);
            mBtn.setLayoutParams(bp);
            
            final AlertDialog dialog = builder.create(); // объявление для закрытия
            mBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedModel = modelName;
                    updateModelButtonLabel();
                    appendLog("Выбрана модель: " + selectedModel);
                    // Закрыть диалог
                }
            });
            // Исправленный лисенер с закрытием
            final AlertDialog[] holder = new AlertDialog[1];
            mBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedModel = modelName;
                    updateModelButtonLabel();
                    appendLog("Выбрана модель: " + selectedModel);
                    if (holder[0] != null) holder[0].dismiss();
                }
            });
            normalContainer.addView(mBtn);
        }
        mainDialogLayout.addView(normalContainer);

        // Блок 2: NSFW модели
        TextView headerNsfw = new TextView(this);
        headerNsfw.setText("\n🔞 NSFW модели (" + nsfwModels.size() + "):");
        headerNsfw.setTextColor(Color.parseColor("#FCA5A5"));
        headerNsfw.setTextSize(13f);
        headerNsfw.setTypeface(Typeface.DEFAULT_BOLD);
        mainDialogLayout.addView(headerNsfw);

        LinearLayout nsfwContainer = new LinearLayout(this);
        nsfwContainer.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < nsfwModels.size(); i++) {
            final String modelName = nsfwModels.get(i);
            final int indexNum = i + 1;
            Button mBtn = new Button(this);
            mBtn.setText(indexNum + ". " + modelName + " 🔞");
            mBtn.setTextSize(12f);
            mBtn.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            mBtn.setPadding(15, 12, 15, 12);
            
            if (modelName.equals(selectedModel)) {
                mBtn.setBackgroundColor(Color.parseColor("#059669"));
                mBtn.setTextColor(Color.WHITE);
            } else {
                mBtn.setBackgroundColor(Color.parseColor("#44EF4444"));
                mBtn.setTextColor(Color.parseColor("#FCA5A5"));
            }
            
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            bp.setMargins(0, 3, 0, 3);
            mBtn.setLayoutParams(bp);
            
            final AlertDialog[] holder = new AlertDialog[1];
            mBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedModel = modelName;
                    updateModelButtonLabel();
                    appendLog("Выбрана NSFW модель: " + selectedModel);
                    if (holder[0] != null) holder[0].dismiss();
                }
            });
            nsfwContainer.addView(mBtn);
        }
        mainDialogLayout.addView(nsfwContainer);

        builder.setView(dialogScroll);
        builder.setPositiveButton("Закрыть", null);
        AlertDialog dialogInstance = builder.create();
        // Запись ссылки для закрытия из кнопок моделей
        // (внутренние лисенеры уже используют dismiss через холдер или контекст)
        dialogInstance.show();
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
                progressStatusLayout.setVisibility(View.GONE);
                btnStartGen.setVisibility(View.VISIBLE);
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

        // Перевод интерфейса в режим обработки (кнопка Стоп активна и видна)
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
                            progressStatusText.setText("Перевод пожеланий...");
                        }
                    });
                    
                    String translatedChanges = translateToEnglish(userChangesRaw);
                    if (isGenerationCancelled) return;

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressStatusText.setText("Проверка ключей доступа...");
                        }
                    });
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
                        appendLog("🔥 Активен NSFW-промпт высочайшего качества.");
                    } else {
                        String standardCore = "masterpiece, best quality, ultra-detailed photo, sharp focus, professional studio lighting, 8k resolution, photorealistic";
                        if (!translatedChanges.isEmpty()) standardCore += ", " + translatedChanges;
                        negativePrompt = "(lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, cropped, worst quality, low quality)";
                        fullPrompt = standardCore + " ### " + negativePrompt;
                        appendLog("✨ Активен стандартный фото-промпт.");
                    }

                    JSONObject paramsObj = new JSONObject();
                    paramsObj.put("sampler_name", "k_dpmpp_2m");
                    paramsObj.put("cfg_scale", 7.5);
                    paramsObj.put("steps", 25);
                    paramsObj.put("denoising_strength", 0.60);

                    byte[] resultBytes = null;
                    boolean successGenerated = false;

                    List<String> modelsToTry = new ArrayList<String>();
                    modelsToTry.add(selectedModel);
                    for (int i = 0; i < allAvailableModels.size(); i++) {
                        String m = allAvailableModels.get(i);
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
                                appendLog("👉 Модель: [" + modelName + "], Ключ: " + apiKey.substring(0, 5) + "...");
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressStatusText.setText("Модель: " + modelName);
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
                                    if (isGenerationCancelled) break;
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
                                                    progressStatusText.setText("Очередь: " + qPos + " | Ждать: " + waitTime + "с");
                                                }
                                            });
                                        }
                                    }
                                }

                                if (isGenerationCancelled) break;

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

                    if (isGenerationCancelled) return;

                    if (!successGenerated || resultBytes == null) {
                        throw new RuntimeException("Все модели перегружены или вернули пустой результат.");
                    }

                    final Bitmap finalBmp = BitmapFactory.decodeByteArray(resultBytes, 0, resultBytes.length);
                    saveBitmapToGallery(finalBmp, "MonsterAI_" + System.currentTimeMillis() + ".jpg");

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressStatusLayout.setVisibility(View.GONE);
                            btnStartGen.setVisibility(View.VISIBLE);
                            imageView.setImageBitmap(finalBmp);
                            statusText.setText("✅ Успешно сгенерировано и сохранено!");
                            Toast.makeText(MainActivity.this, "Фото сохранено в Pictures/MonsterAI", Toast.LENGTH_LONG).show();
                            appendLog("✨ Готово! Фото сохранено в галерею.");
                        }
                    });

                } catch (final Exception e) {
                    if (!isGenerationCancelled) {
                        appendLog("❌ ОШИБКА: " + e.getMessage());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                progressStatusLayout.setVisibility(View.GONE);
                                btnStartGen.setVisibility(View.VISIBLE);
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