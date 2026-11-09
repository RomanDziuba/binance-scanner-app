package com.apcmi.version1;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

// THEME: @android:style/Theme.Material.Light.NoActionBar
public class MainActivity extends Activity {

    private static final int PICK_APC_FILE = 2001;

    private boolean isEditMode = false;
    private String currentFileName = "config.apcmi";
    
    // Карта файлов внутри архива: Имя файла -> Текст
    private Map<String, String> archiveFiles = new HashMap<>();
    private String activeInternalFile = "";

    private LinearLayout containerLayout;
    private ScrollView mainScroll;
    private TextView statusTextView, titleView;
    private Button btnToggleMode, btnOpenFile, btnSaveFile;
    private EditText codeEditorInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Демо-данные на случай первого запуска
        archiveFiles.put("config.json", "{\n  \"config_name\": \"Default APCMI Config\",\n  \"version\": \"1.0.0\",\n  \"author\": \"Dziuba Roman\"\n}");
        archiveFiles.put("exported.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n    <setting name=\"mode\">pro</setting>\n</resources>");
        activeInternalFile = "config.json";

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));
        rootLayout.setPadding(24, 24, 24, 24);

        // Верхняя панель
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, 0, 0, 16);

        titleView = new TextView(this);
        titleView.setText("📂 APCMI Configurator (ZIP Engine)");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(13f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        titleView.setLayoutParams(titleParams);
        topBar.addView(titleView);

        btnToggleMode = createStyledButton("✏️ Редактор", "#1E293B", 100);
        btnToggleMode.setOnClickListener(v -> toggleMode());
        topBar.addView(btnToggleMode);

        rootLayout.addView(topBar);

        // Панель кнопок действий
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, 0, 0, 16);

        btnOpenFile = createStyledButton("📁 Открыть .apcmi", "#0F766E", 0);
        btnOpenFile.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        btnOpenFile.setOnClickListener(v -> openFilePicker());
        actionRow.addView(btnOpenFile);

        addSpacer(actionRow, 12);

        btnSaveFile = createStyledButton("💾 Сохранить архив", "#1D4ED8", 0);
        btnSaveFile.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        btnSaveFile.setOnClickListener(v -> saveFileToStorage());
        actionRow.addView(btnSaveFile);

        rootLayout.addView(actionRow);

        statusTextView = new TextView(this);
        statusTextView.setText("Файл: " + currentFileName + " | Активный файл: " + activeInternalFile);
        statusTextView.setTextColor(Color.parseColor("#10B981"));
        statusTextView.setTextSize(11f);
        statusTextView.setPadding(0, 0, 0, 12);
        rootLayout.addView(statusTextView);

        // Область контента
        mainScroll = new ScrollView(this);
        mainScroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f
        ));

        containerLayout = new LinearLayout(this);
        containerLayout.setOrientation(LinearLayout.VERTICAL);
        mainScroll.addView(containerLayout);
        rootLayout.addView(mainScroll);

        setContentView(rootLayout);
        renderContent();
    }

    private void toggleMode() {
        isEditMode = !isEditMode;
        if (isEditMode) {
            btnToggleMode.setText("👁️ Просмотр");
            btnToggleMode.setBackgroundColor(Color.parseColor("#047857"));
            statusTextView.setText("Режим: Редактирование файла [" + activeInternalFile + "]");
        } else {
            if (codeEditorInput != null && !activeInternalFile.isEmpty()) {
                archiveFiles.put(activeInternalFile, codeEditorInput.getText().toString());
            }
            btnToggleMode.setText("✏️ Редактор");
            btnToggleMode.setBackgroundColor(Color.parseColor("#1E293B"));
            statusTextView.setText("Режим: Просмотр архива [" + currentFileName + "]");
        }
        renderContent();
    }

    private void renderContent() {
        containerLayout.removeAllViews();

        if (isEditMode) {
            TextView label = new TextView(this);
            label.setText("📝 Редактирование файла: " + activeInternalFile);
            label.setTextColor(Color.parseColor("#38BDF8"));
            label.setTextSize(12f);
            label.setPadding(0, 0, 0, 8);
            containerLayout.addView(label);

            codeEditorInput = new EditText(this);
            codeEditorInput.setText(archiveFiles.get(activeInternalFile) != null ? archiveFiles.get(activeInternalFile) : "");
            codeEditorInput.setTextColor(Color.parseColor("#E2E8F0"));
            codeEditorInput.setTextSize(12f);
            codeEditorInput.setTypeface(Typeface.MONOSPACE);
            codeEditorInput.setBackgroundColor(Color.parseColor("#0F172A"));
            codeEditorInput.setPadding(20, 20, 20, 20);
            codeEditorInput.setGravity(Gravity.TOP);
            codeEditorInput.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 800
            ));
            containerLayout.addView(codeEditorInput);
        } else {
            TextView header = new TextView(this);
            header.setText("📦 Файлы внутри .apcmi архива (Нажмите для выбора):");
            header.setTextColor(Color.parseColor("#FBBF24"));
            header.setTextSize(12f);
            header.setPadding(0, 0, 0, 10);
            containerLayout.addView(header);

            for (String fileName : archiveFiles.keySet()) {
                Button fileItemBtn = createStyledButton("📄 " + fileName + (fileName.equals(activeInternalFile) ? " (Выбран)" : ""), 
                    fileName.equals(activeInternalFile) ? "#0F766E" : "#1E293B", 0);
                fileItemBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                ((LinearLayout.LayoutParams) fileItemBtn.getLayoutParams()).setMargins(0, 0, 0, 8);
                
                final String fName = fileName;
                fileItemBtn.setOnClickListener(v -> {
                    activeInternalFile = fName;
                    renderContent();
                });
                containerLayout.addView(fileItemBtn);
            }

            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
            divider.setBackgroundColor(Color.parseColor("#334155"));
            ((LinearLayout.LayoutParams) divider.getLayoutParams()).setMargins(0, 15, 0, 15);
            containerLayout.addView(divider);

            TextView contentHeader = new TextView(this);
            contentHeader.setText("🔍 Просмотр содержимого: " + activeInternalFile);
            contentHeader.setTextColor(Color.parseColor("#38BDF8"));
            contentHeader.setTextSize(12f);
            contentHeader.setPadding(0, 0, 0, 8);
            containerLayout.addView(contentHeader);

            String content = archiveFiles.get(activeInternalFile);
            if (content != null) {
                try {
                    JSONObject json = new JSONObject(content);
                    Iterator<String> keys = json.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        Object val = json.get(key);
                        containerLayout.addView(createParameterCard(key, String.valueOf(val)));
                    }
                } catch (Exception e) {
                    TextView textCard = new TextView(this);
                    textCard.setText(content);
                    textCard.setTextColor(Color.parseColor("#E2E8F0"));
                    textCard.setTextSize(12f);
                    textCard.setPadding(16, 16, 16, 16);

                    GradientDrawable bg = new GradientDrawable();
                    bg.setColor(Color.parseColor("#0F172A"));
                    bg.setCornerRadius(16f);
                    bg.setStroke(1, Color.parseColor("#334155"));
                    textCard.setBackground(bg);
                    containerLayout.addView(textCard);
                }
            }
        }
    }

    private View createParameterCard(String key, String value) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 12, 16, 12);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#0F172A"));
        bg.setCornerRadius(16f);
        bg.setStroke(1, Color.parseColor("#1E293B"));
        card.setBackground(bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 8);
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
        valView.setTextSize(13f);
        valView.setPadding(0, 4, 0, 0);
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
            Toast.makeText(this, "Файловый менеджер недоступен", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                File tempZip = new File(getCacheDir(), "temp_import.zip");
                InputStream in = getContentResolver().openInputStream(uri);
                FileOutputStream out = new FileOutputStream(tempZip);
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
                out.close();
                in.close();

                archiveFiles.clear();
                ZipFile zipFile = new ZipFile(tempZip);
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory()) {
                        InputStream zis = zipFile.getInputStream(entry);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                        reader.close();
                        archiveFiles.put(entry.getName(), sb.toString());
                    }
                }
                zipFile.close();
                tempZip.delete();

                if (!archiveFiles.isEmpty()) {
                    activeInternalFile = archiveFiles.keySet().iterator().next();
                } else {
                    archiveFiles.put("config.json", "{}");
                    activeInternalFile = "config.json";
                }

                currentFileName = uri.getLastPathSegment();
                if (currentFileName == null) currentFileName = "config.apcmi";

                statusTextView.setText("Файл: " + currentFileName + " успешно распакован!");
                renderContent();
                Toast.makeText(this, "Архив .apcmi открыт!", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Toast.makeText(this, "Ошибка распаковки: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void saveFileToStorage() {
        if (isEditMode && codeEditorInput != null && !activeInternalFile.isEmpty()) {
            archiveFiles.put(activeInternalFile, codeEditorInput.getText().toString());
        }

        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) downloadsDir.mkdirs();

            File outFile = new File(downloadsDir, currentFileName);
            
            // Упаковываем обратно в валидный ZIP (.apcmi)
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outFile));
            for (Map.Entry<String, String> entry : archiveFiles.entrySet()) {
                ZipEntry ze = new ZipEntry(entry.getKey());
                zos.putNextEntry(ze);
                byte[] data = entry.getValue().getBytes(StandardCharsets.UTF_8);
                zos.write(data, 0, data.length);
                zos.closeEntry();
            }
            zos.close();

            Toast.makeText(this, "💾 Архив .apcmi сохранен в Загрузки!", Toast.LENGTH_LONG).show();
            statusTextView.setText("Сохранено: " + outFile.getAbsolutePath());
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка сохранения: " + e.getMessage(), Toast.LENGTH_LONG).show();
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