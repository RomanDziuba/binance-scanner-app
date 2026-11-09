package com.apcmi.version1;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// THEME: @android:style/Theme.Material.Light.NoActionBar
public class MainActivity extends Activity {

    private static final int PICK_APC_FILE = 2001;
    private static final int SAVE_APC_FILE = 2002;

    private boolean isEditMode = false;
    private String currentFileName = "sample.apcmi";
    private String rawFileContent = "{\n  \"config_name\": \"Default APCMI Config\",\n  \"version\": \"1.0.0\",\n  \"author\": \"Dziuba Roman\",\n  \"debug_mode\": true,\n  \"timeout_ms\": 5000,\n  \"server_url\": \"https://api.apcmi.com/v1\"\n}";

    private LinearLayout containerLayout;
    private ScrollView mainScroll;
    private TextView statusTextView, titleView;
    private Button btnToggleMode, btnOpenFile, btnSaveFile, btnShareFile;
    private EditText codeEditorInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Корневой макет
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));
        rootLayout.setPadding(24, 24, 24, 24);

        // Верхняя панель управления
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, 0, 0, 16);

        titleView = new TextView(this);
        titleView.setText("📂 APCMI Configurator");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(15f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        titleView.setLayoutParams(titleParams);
        topBar.addView(titleView);

        btnToggleMode = createStyledButton("✏️ Редактор", "#1E293B", 110);
        btnToggleMode.setOnClickListener(v -> toggleMode());
        topBar.addView(btnToggleMode);

        rootLayout.addView(topBar);

        // Панель быстрых действий (Открыть / Сохранить / Поделиться)
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, 0, 0, 16);

        btnOpenFile = createStyledButton("📁 Открыть", "#0F766E", 0);
        btnOpenFile.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        btnOpenFile.setOnClickListener(v -> openFilePicker());
        actionRow.addView(btnOpenFile);

        addSpacer(actionRow, 12);

        btnSaveFile = createStyledButton("💾 Сохранить", "#1D4ED8", 0);
        btnSaveFile.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        btnSaveFile.setOnClickListener(v -> saveFileToStorage());
        actionRow.addView(btnSaveFile);

        addSpacer(actionRow, 12);

        btnShareFile = createStyledButton("📤", "#334155", 80);
        btnShareFile.setOnClickListener(v -> shareConfiguration());
        actionRow.addView(btnShareFile);

        rootLayout.addView(actionRow);

        // Статус строка
        statusTextView = new TextView(this);
        statusTextView.setText("Файл: " + currentFileName + " | Режим: Просмотр 👁️");
        statusTextView.setTextColor(Color.parseColor("#10B981"));
        statusTextView.setTextSize(11f);
        statusTextView.setPadding(0, 0, 0, 12);
        rootLayout.addView(statusTextView);

        // Контейнер содержимого (Просмотр / Редактирование)
        mainScroll = new ScrollView(this);
        mainScroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f
        ));

        containerLayout = new LinearLayout(this);
        containerLayout.setOrientation(LinearLayout.VERTICAL);
        mainScroll.addView(containerLayout);
        rootLayout.addView(mainScroll);

        setContentView(rootLayout);

        // Отрисовка начального экрана
        renderContent();
    }

    private void toggleMode() {
        isEditMode = !isEditMode;
        if (isEditMode) {
            btnToggleMode.setText("👁️ Просмотр");
            btnToggleMode.setBackgroundColor(Color.parseColor("#047857"));
            statusTextView.setText("Файл: " + currentFileName + " | Режим: Редактирование ✍️");
        } else {
            // Если выходим из режима редактирования, сохраняем изменения из редактора в память
            if (codeEditorInput != null) {
                rawFileContent = codeEditorInput.getText().toString();
            }
            btnToggleMode.setText("✏️ Редактор");
            btnToggleMode.setBackgroundColor(Color.parseColor("#1E293B"));
            statusTextView.setText("Файл: " + currentFileName + " | Режим: Просмотр 👁️");
        }
        renderContent();
    }

    private void renderContent() {
        containerLayout.removeAllViews();

        if (isEditMode) {
            // Режим редактирования: текстовый редактор с моноширинным шрифтом
            codeEditorInput = new EditText(this);
            codeEditorInput.setText(rawFileContent);
            codeEditorInput.setTextColor(Color.parseColor("#E2E8F0"));
            codeEditorInput.setTextSize(12f);
            codeEditorInput.setTypeface(Typeface.MONOSPACE);
            codeEditorInput.setBackgroundColor(Color.parseColor("#0F172A"));
            codeEditorInput.setPadding(20, 20, 20, 20);
            codeEditorInput.setGravity(Gravity.TOP);
            codeEditorInput.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            ));
            containerLayout.addView(codeEditorInput);
        } else {
            // Режим просмотра: красивый парсинг .apcmi / JSON в визуальные карточки
            try {
                JSONObject json = new JSONObject(rawFileContent);
                Iterator<String> keys = json.keys();
                
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object value = json.get(key);
                    containerLayout.addView(createParameterCard(key, String.valueOf(value)));
                }
            } catch (JSONException e) {
                // Если файл не JSON, выводим как обычный текст в блоке
                TextView errorCard = new TextView(this);
                errorCard.setText("⚠️ Файл не является валидным JSON-форматом APCMI.\nОтображение в сыром виде:\n\n" + rawFileContent);
                errorCard.setTextColor(Color.parseColor("#FBBF24"));
                errorCard.setTextSize(12f);
                errorCard.setPadding(20, 20, 20, 20);
                
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(Color.parseColor("#0F172A"));
                bg.setCornerRadius(16f);
                bg.setStroke(1, Color.parseColor("#334155"));
                errorCard.setBackground(bg);
                
                containerLayout.addView(errorCard);
            }
        }
    }

    private View createParameterCard(String key, String value) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 16, 20, 16);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#0F172A"));
        bg.setCornerRadius(16f);
        bg.setStroke(1, Color.parseColor("#1E293B"));
        card.setBackground(bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 12);
        card.setLayoutParams(params);

        TextView keyView = new TextView(this);
        keyView.setText("🔑 " + key.toUpperCase());
        keyView.setTextColor(Color.parseColor("#38BDF8"));
        keyView.setTextSize(11f);
        keyView.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(keyView);

        TextView valView = new TextView(this);
        valView.setText(value);
        valView.setTextColor(Color.parseColor("#F8FAFC"));
        valView.setTextSize(14f);
        valView.setPadding(0, 6, 0, 0);
        card.addView(valView);

        return card;
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Выберите APCMI файл"), PICK_APC_FILE);
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть файловый менеджер", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();

                rawFileContent = sb.toString();
                currentFileName = uri.getLastPathSegment();
                if (currentFileName == null) currentFileName = "config.apcmi";

                statusTextView.setText("Файл: " + currentFileName + " успешно загружен!");
                renderContent();
                Toast.makeText(this, "Файл открыт!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка чтения файла: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void saveFileToStorage() {
        if (isEditMode && codeEditorInput != null) {
            rawFileContent = codeEditorInput.getText().toString();
        }

        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) downloadsDir.mkdirs();

            File outFile = new File(downloadsDir, currentFileName);
            FileOutputStream fos = new FileOutputStream(outFile);
            fos.write(rawFileContent.getBytes(StandardCharsets.UTF_8));
            fos.close();

            Toast.makeText(this, "💾 Файл сохранен в Загрузки: " + outFile.getName(), Toast.LENGTH_LONG).show();
            statusTextView.setText("Файл сохранен: " + outFile.getAbsolutePath());
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка сохранения: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void shareConfiguration() {
        if (isEditMode && codeEditorInput != null) {
            rawFileContent = codeEditorInput.getText().toString();
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "APCMI Config: " + currentFileName);
        shareIntent.putExtra(Intent.EXTRA_TEXT, rawFileContent);
        try {
            startActivity(Intent.createChooser(shareIntent, "Поделиться конфигурацией"));
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка отправки", Toast.LENGTH_SHORT).show();
        }
    }

    private Button createStyledButton(String text, String colorHex, int widthDp) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.parseColor("#F8FAFC"));
        btn.setTextSize(11f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        btn.setPadding(12, 12, 12, 12);
        
        if (widthDp > 0) {
            float scale = getResources().getDisplayMetrics().density;
            int px = (int) (widthDp * scale + 0.5f);
            btn.setLayoutParams(new LinearLayout.LayoutParams(px, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        return btn;
    }

    private void addSpacer(LinearLayout layout, int widthDp) {
        View spacer = new View(this);
        float scale = getResources().getDisplayMetrics().density;
        int px = (int) (widthDp * scale + 0.5f);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(px, ViewGroup.LayoutParams.MATCH_PARENT));
        layout.addView(spacer);
    }
}