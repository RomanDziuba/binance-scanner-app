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
import org.json.JSONException;
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

    private static final int PICK_APC_FILE = 3001;

    private boolean isParamEditMode = false;
    private String currentProjectName = "Substation_IED_Config.apcmi";
    
    // Хранилище файлов внутри .apcmi контейнера
    private Map<String, String> projectFiles = new HashMap<>();
    private String activeNodeFile = "config.json";

    private LinearLayout containerLayout, propertyGridContainer;
    private ScrollView mainScroll;
    private TextView statusTextView, projectTitleView;
    private Button btnToggleView, btnOpenProject, btnSaveProject;
    private EditText rawEditorInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Инициализация демо-структуры IED в стиле PCM600
        projectFiles.put("config.json", "{\n  \"ied_name\": \"REF615_Feeder_Protection\",\n  \"station_voltage_kv\": 110,\n  \"relay_group\": \"Chint_NR2_NC2\",\n  \"trip_delay_ms\": 150,\n  \"os_version\": \"v5.2.3\",\n  \"auto_reclosing\": true\n}");
        projectFiles.put("exported.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<IED name=\"REF615\">\n    <Setting parameter=\"ZeroSequence\" value=\"Enabled\"/>\n    <Setting parameter=\"Threshold\" value=\"120A\"/>\n</IED>");
        projectFiles.put("PDSdata.dat", "LOG_EVENT: Substation bay 3 initialized.\nRELAY_STATUS: Normal\nCURRENT_LOAD_A: 110.5");
        activeNodeFile = "config.json";

        // Главный контейнер в инженерном стиле PCM600 (темный промышленный фон)
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#0A0E17"));
        rootLayout.setPadding(16, 16, 16, 16);

        // --- ВЕРХНЯЯ ПАНЕЛЬ ИНСТРУМЕНТОВ (ToolBar) ---
        LinearLayout toolBar = new LinearLayout(this);
        toolBar.setOrientation(LinearLayout.HORIZONTAL);
        toolBar.setGravity(Gravity.CENTER_VERTICAL);
        toolBar.setPadding(12, 12, 12, 12);
        
        GradientDrawable toolbarBg = new GradientDrawable();
        toolbarBg.setColor(Color.parseColor("#131B2E"));
        toolbarBg.setCornerRadius(8f);
        toolbarBg.setStroke(1, Color.parseColor("#1E293B"));
        toolBar.setBackground(toolbarBg);

        projectTitleView = new TextView(this);
        projectTitleView.setText("⚡ PCM600: IED Plant Structure");
        projectTitleView.setTextColor(Color.parseColor("#38BDF8"));
        projectTitleView.setTextSize(12f);
        projectTitleView.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        projectTitleView.setLayoutParams(titleParams);
        toolBar.addView(projectTitleView);

        btnToggleView = createStyledButton("📝 Редактор", "#1E293B", 100);
        btnToggleView.setOnClickListener(v -> toggleViewMode());
        toolBar.addView(btnToggleView);

        rootLayout.addView(toolBar);
        addSpacer(rootLayout, 10);

        // --- ПАНЕЛЬ ДЕЙСТВИЙ (Project Actions) ---
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);

        btnOpenProject = createStyledButton("📁 Открыть .apcmi", "#0F766E", 0);
        btnOpenProject.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        btnOpenProject.setOnClickListener(v -> openApcmiFilePicker());
        actionRow.addView(btnOpenProject);

        addSpacerHoriz(actionRow, 10);

        btnSaveProject = createStyledButton("💾 Сохранить .apcmi", "#1D4ED8", 0);
        btnSaveProject.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        btnSaveProject.setOnClickListener(v -> saveApcmiArchive());
        actionRow.addView(btnSaveProject);

        rootLayout.addView(actionRow);
        addSpacer(rootLayout, 10);

        // --- СТАТУС СТРОКА (Status Bar) ---
        statusTextView = new TextView(this);
        statusTextView.setText(" Проект: " + currentProjectName + " | Выбран узел: " + activeNodeFile);
        statusTextView.setTextColor(Color.parseColor("#10B981"));
        statusTextView.setTextSize(11f);
        statusTextView.setPadding(8, 6, 8, 6);
        
        GradientDrawable statusBg = new GradientDrawable();
        statusBg.setColor(Color.parseColor("#0F172A"));
        statusBg.setCornerRadius(6f);
        statusTextView.setBackground(statusBg);
        rootLayout.addView(statusTextView);
        addSpacer(rootLayout, 10);

        // --- РАБОЧАЯ ОБЛАСТЬ (Split Workspace: Tree Navigator + Property Grid) ---
        LinearLayout workspace = new LinearLayout(this);
        workspace.setOrientation(LinearLayout.VERTICAL);

        // 1. Дерево узлов проекта (Горизонтальные вкладки-узлы в стиле Plant Structure PCM600)
        HorizontalScrollView treeScroll = new HorizontalScrollView(this);
        treeScroll.setPadding(0, 0, 0, 8);
        
        containerLayout = new LinearLayout(this);
        containerLayout.setOrientation(LinearLayout.HORIZONTAL);
        treeScroll.addView(containerLayout);
        workspace.addView(treeScroll);

        // 2. Таблица параметров / Свойств (Property Grid / Parameter Settings)
        mainScroll = new ScrollView(this);
        mainScroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f
        ));

        propertyGridContainer = new LinearLayout(this);
        propertyGridContainer.setOrientation(LinearLayout.VERTICAL);
        mainScroll.addView(propertyGridContainer);
        workspace.addView(mainScroll);

        LinearLayout.LayoutParams wsParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f
        );
        workspace.setLayoutParams(wsParams);
        rootLayout.addView(workspace);

        setContentView(rootLayout);
        renderWorkspace();
    }

    private void toggleViewMode() {
        isParamEditMode = !isParamEditMode;
        if (isParamEditMode) {
            btnToggleView.setText("📊 Таблица");
            btnToggleView.setBackgroundColor(Color.parseColor("#047857"));
            statusTextView.setText(" Режим: Текстовый редактор [Raw Code]");
        } else {
            if (rawEditorInput != null && !activeNodeFile.isEmpty()) {
                projectFiles.put(activeNodeFile, rawEditorInput.getText().toString());
            }
            btnToggleView.setText("📝 Редактор");
            btnToggleView.setBackgroundColor(Color.parseColor("#1E293B"));
            statusTextView.setText(" Проект: " + currentProjectName + " | Узел: " + activeNodeFile);
        }
        renderWorkspace();
    }

    private void renderWorkspace() {
        containerLayout.removeAllViews();
        propertyGridContainer.removeAllViews();

        // Отрисовка дерева узлов IED (Plant Structure Navigator)
        for (String fileName : projectFiles.keySet()) {
            boolean isSelected = fileName.equals(activeNodeFile);
            Button nodeBtn = createStyledButton("📁 " + fileName, isSelected ? "#0F766E" : "#1E293B", 0);
            nodeBtn.setTextSize(11f);
            
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 8, 0);
            nodeBtn.setLayoutParams(lp);

            final String nodeName = fileName;
            nodeBtn.setOnClickListener(v -> {
                if (isParamEditMode && rawEditorInput != null && !activeNodeFile.isEmpty()) {
                    projectFiles.put(activeNodeFile, rawEditorInput.getText().toString());
                }
                activeNodeFile = nodeName;
                statusTextView.setText(" Проект: " + currentProjectName + " | Узел: " + activeNodeFile);
                renderWorkspace();
            });
            containerLayout.addView(nodeBtn);
        }

        // Отрисовка содержимого выбранного узла
        String content = projectFiles.get(activeNodeFile);
        if (content == null) content = "";

        if (isParamEditMode) {
            // Режим сырого редактора кода / конфигурации
            rawEditorInput = new EditText(this);
            rawEditorInput.setText(content);
            rawEditorInput.setTextColor(Color.parseColor("#E2E8F0"));
            rawEditorInput.setTextSize(12f);
            rawEditorInput.setTypeface(Typeface.MONOSPACE);
            rawEditorInput.setBackgroundColor(Color.parseColor("#0F172A"));
            rawEditorInput.setPadding(16, 16, 16, 16);
            rawEditorInput.setGravity(Gravity.TOP);
            rawEditorInput.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 850
            ));
            propertyGridContainer.addView(rawEditorInput);
        } else {
            // Режим Property Grid / Parameter Settings (в стиле таблиц параметров PCM600)
            TextView gridHeader = new TextView(this);
            gridHeader.setText("⚙️ Окно параметров и свойств узла: " + activeNodeFile);
            gridHeader.setTextColor(Color.parseColor("#FBBF24"));
            gridHeader.setTextSize(12f);
            gridHeader.setTypeface(Typeface.DEFAULT_BOLD);
            gridHeader.setPadding(4, 4, 4, 12);
            propertyGridContainer.addView(gridHeader);

            try {
                JSONObject json = new JSONObject(content);
                Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object val = json.get(key);
                    propertyGridContainer.addView(createParameterRowCard(key, String.valueOf(val)));
                }
            } catch (Exception e) {
                // Если формат не JSON, отображаем как структурированный лог/текст
                TextView textCard = new TextView(this);
                textCard.setText(content);
                textCard.setTextColor(Color.parseColor("#E2E8F0"));
                textCard.setTextSize(12f);
                textCard.setTypeface(Typeface.MONOSPACE);
                textCard.setPadding(16, 16, 16, 16);

                GradientDrawable cardBg = new GradientDrawable();
                cardBg.setColor(Color.parseColor("#0F172A"));
                cardBg.setCornerRadius(8f);
                cardBg.setStroke(1, Color.parseColor("#1E293B"));
                textCard.setBackground(cardBg);
                propertyGridContainer.addView(textCard);
            }
        }
    }

    private View createParameterRowCard(String paramName, String paramValue) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(16, 12, 16, 12);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#0F172A"));
        bg.setCornerRadius(8f);
        bg.setStroke(1, Color.parseColor("#1E293B"));
        row.setBackground(bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 6);
        row.setLayoutParams(params);

        TextView nameView = new TextView(this);
        nameView.setText(paramName);
        nameView.setTextColor(Color.parseColor("#38BDF8"));
        nameView.setTextSize(12f);
        nameView.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        nameView.setLayoutParams(nameParams);
        row.addView(nameView);

        TextView valView = new TextView(this);
        valView.setText(paramValue);
        valView.setTextColor(Color.parseColor("#F8FAFC"));
        valView.setTextSize(12f);
        valView.setTypeface(Typeface.MONOSPACE);
        row.addView(valView);

        return row;
    }

    private void openApcmiFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Выберите APCMI проект"), PICK_APC_FILE);
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
                File tempZip = new File(getCacheDir(), "import_ied.zip");
                InputStream in = getContentResolver().openInputStream(uri);
                FileOutputStream out = new FileOutputStream(tempZip);
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
                out.close();
                in.close();

                projectFiles.clear();
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
                        projectFiles.put(entry.getName(), sb.toString());
                    }
                }
                zipFile.close();
                tempZip.delete();

                if (!projectFiles.isEmpty()) {
                    activeNodeFile = projectFiles.keySet().iterator().next();
                } else {
                    projectFiles.put("config.json", "{}");
                    activeNodeFile = "config.json";
                }

                currentProjectName = uri.getLastPathSegment();
                if (currentProjectName == null) currentProjectName = "ied_config.apcmi";

                statusTextView.setText(" Проект: " + currentProjectName + " успешно загружен!");
                renderWorkspace();
                Toast.makeText(this, "IED Конфигурация открыта!", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Toast.makeText(this, "Ошибка чтения архива: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void saveApcmiArchive() {
        if (isParamEditMode && rawEditorInput != null && !activeNodeFile.isEmpty()) {
            projectFiles.put(activeNodeFile, rawEditorInput.getText().toString());
        }

        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) downloadsDir.mkdirs();

            File outFile = new File(downloadsDir, currentProjectName);
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outFile));
            
            for (Map.Entry<String, String> entry : projectFiles.entrySet()) {
                ZipEntry ze = new ZipEntry(entry.getKey());
                zos.putNextEntry(ze);
                byte[] data = entry.getValue().getBytes(StandardCharsets.UTF_8);
                zos.write(data, 0, data.length);
                zos.closeEntry();
            }
            zos.close();

            Toast.makeText(this, "💾 Проект PCM600 сохранен в Загрузки!", Toast.LENGTH_LONG).show();
            statusTextView.setText(" Сохранено: " + outFile.getAbsolutePath());
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
        btn.setPadding(10, 10, 10, 10);
        
        if (widthDp > 0) {
            float scale = getResources().getDisplayMetrics().density;
            int px = (int) (widthDp * scale + 0.5f);
            btn.setLayoutParams(new LinearLayout.LayoutParams(px, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        return btn;
    }

    private void addSpacer(LinearLayout layout, int heightDp) {
        View spacer = new View(this);
        float scale = getResources().getDisplayMetrics().density;
        int px = (int) (heightDp * scale + 0.5f);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px));
        layout.addView(spacer);
    }

    private void addSpacerHoriz(LinearLayout layout, int widthDp) {
        View spacer = new View(this);
        float scale = getResources().getDisplayMetrics().density;
        int px = (int) (widthDp * scale + 0.5f);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(px, ViewGroup.LayoutParams.MATCH_PARENT));
        layout.addView(spacer);
    }
}