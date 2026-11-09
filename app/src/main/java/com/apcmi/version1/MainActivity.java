package com.apcmi.version1;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

// THEME: @android:style/Theme.Material.Light.NoActionBar
public class MainActivity extends Activity {

    private static final int PICK_APC_FILE = 3001;
    private static final int PERMISSION_REQUEST_CODE = 1002;

    private boolean isParamEditMode = false;
    private String currentProjectName = "REX615_Substation.apcmi";
    
    // Хранилище файлов проекта
    private Map<String, String> projectFiles = new LinkedHashMap<>();
    private String activeNodeFile = "";

    // Структура параметров для режима наладки (Parameter Setting / Tuning)
    private Map<String, Map<String, String>> parsedParameters = new LinkedHashMap<>();

    private LinearLayout containerLayout, propertyGridContainer;
    private ScrollView mainScroll;
    private TextView statusTextView, projectTitleView;
    private Button btnToggleView, btnOpenProject, btnSaveProject, btnScanPhone;
    private EditText rawEditorInput;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkStoragePermissions();

        // Демо-структура блоков IED для PCM600
        projectFiles.put("exported.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<IED name=\"REX615\">\n    <Setting parameter=\"ZeroSequence\" value=\"Enabled\"/>\n    <Setting parameter=\"Threshold\" value=\"120A\"/>\n</IED>");
        projectFiles.put("PDSdata.dat", "FunctionGroup: TPGAPC1\n  - PickupDelay: 150ms\n  - OperateValue: 5.2A\n  - Status: Active\nFunctionGroup: PHHPTOC1\n  - CurrentTrip: 120A\n  - TimeMultiplier: 0.1s");
        activeNodeFile = "PDSdata.dat";

        // Главный контейнер PCM600
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#0A0E17"));
        rootLayout.setPadding(16, 16, 16, 16);

        // --- ВЕРХНЯЯ ПАНЕЛЬ ИНСТРУМЕНТОВ ---
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
        projectTitleView.setText("⚡ PCM600: IED Parameter Setting & Tuning");
        projectTitleView.setTextColor(Color.parseColor("#38BDF8"));
        projectTitleView.setTextSize(12f);
        projectTitleView.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        projectTitleView.setLayoutParams(titleParams);
        toolBar.addView(projectTitleView);

        btnToggleView = createStyledButton("📝 Raw Код", "#1E293B", 100);
        btnToggleView.setOnClickListener(v -> toggleViewMode());
        toolBar.addView(btnToggleView);

        rootLayout.addView(toolBar);
        addSpacer(rootLayout, 10);

        // --- ПАНЕЛЬ КНОПОК ---
        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);

        btnOpenProject = createStyledButton("📁 Открыть", "#0F766E", 0);
        btnOpenProject.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        btnOpenProject.setOnClickListener(v -> openApcmiFilePicker());
        actionRow.addView(btnOpenProject);

        addSpacerHoriz(actionRow, 8);

        btnSaveProject = createStyledButton("💾 Сохранить", "#1D4ED8", 0);
        btnSaveProject.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        btnSaveProject.setOnClickListener(v -> saveApcmiArchive());
        actionRow.addView(btnSaveProject);

        addSpacerHoriz(actionRow, 8);

        btnScanPhone = createStyledButton("🔍 Сканировать", "#B45309", 0);
        btnScanPhone.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        btnScanPhone.setOnClickListener(v -> scanPhoneForProjects());
        actionRow.addView(btnScanPhone);

        rootLayout.addView(actionRow);
        addSpacer(rootLayout, 10);

        // --- СТАТУС СТРОКА ---
        statusTextView = new TextView(this);
        statusTextView.setText(" Проект: " + currentProjectName + " | Блок: " + activeNodeFile);
        statusTextView.setTextColor(Color.parseColor("#10B981"));
        statusTextView.setTextSize(11f);
        statusTextView.setPadding(8, 6, 8, 6);
        
        GradientDrawable statusBg = new GradientDrawable();
        statusBg.setColor(Color.parseColor("#0F172A"));
        statusBg.setCornerRadius(6f);
        statusTextView.setBackground(statusBg);
        rootLayout.addView(statusTextView);
        addSpacer(rootLayout, 10);

        // --- РАБОЧАЯ ОБЛАСТЬ ---
        LinearLayout workspace = new LinearLayout(this);
        workspace.setOrientation(LinearLayout.VERTICAL);

        // Дерево узлов проекта (Вкладки)
        HorizontalScrollView treeScroll = new HorizontalScrollView(this);
        treeScroll.setPadding(0, 0, 0, 8);
        
        containerLayout = new LinearLayout(this);
        containerLayout.setOrientation(LinearLayout.HORIZONTAL);
        treeScroll.addView(containerLayout);
        workspace.addView(treeScroll);

        // Таблица параметров / Property Grid (Интерфейс наладки PCM600)
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
        parseActiveNodeParameters();
        renderWorkspace();
    }

    private void checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, PERMISSION_REQUEST_CODE);
            }
        }
    }

    private void scanPhoneForProjects() {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("🔍 Сканирование памяти");
        progress.setMessage("Поиск файлов проектов (.apcmi, .zip, .cff, .dat) на устройстве...");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            List<File> foundFiles = new ArrayList<>();
            try {
                File storageDir = Environment.getExternalStorageDirectory();
                recursiveSearch(storageDir, foundFiles);
            } catch (Exception ignored) {}

            handler.post(() -> {
                progress.dismiss();
                showFoundFilesDialog(foundFiles);
            });
        }).start();
    }

    private void recursiveSearch(File dir, List<File> result) {
        if (dir == null || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                String name = f.getName().toLowerCase();
                if (!name.startsWith(".") && !name.equals("android") && !name.equals("data")) {
                    recursiveSearch(f, result);
                }
            } else {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".apcmi") || name.endsWith(".zip") || name.endsWith(".cff") || name.endsWith(".dat") || name.endsWith(".ldf")) {
                    result.add(f);
                }
            }
        }
    }

    private void showFoundFilesDialog(List<File> files) {
        if (files.isEmpty()) {
            Toast.makeText(this, "Файлы проектов не найдены на устройстве", Toast.LENGTH_LONG).show();
            return;
        }

        List<String> displayNames = new ArrayList<>();
        for (File f : files) {
            displayNames.add(f.getName() + " (" + (f.length() / 1024) + " КБ)\nПуть: " + f.getParent());
        }

        final CharSequence[] items = displayNames.toArray(new CharSequence[0]);
        new AlertDialog.Builder(this)
            .setTitle("📁 Найденные проекты PCM600 (" + files.size() + ")")
            .setItems(items, (dialog, which) -> {
                File selected = files.get(which);
                loadArchiveFileSafely(selected);
            })
            .setNegativeButton("Закрыть", null)
            .show();
    }

    private void loadArchiveFileSafely(File file) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("📂 Загрузка IED");
        progress.setMessage("Парсинг структуры и параметров защиты...");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            try {
                Map<String, String> tempMap = new LinkedHashMap<>();
                String lowerName = file.getName().toLowerCase();

                if (lowerName.endsWith(".apcmi") || lowerName.endsWith(".zip")) {
                    ZipFile zipFile = new ZipFile(file);
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        if (!entry.isDirectory()) {
                            try (InputStream zis = zipFile.getInputStream(entry)) {
                                String content = readTextStreamSmart(zis);
                                tempMap.put(entry.getName(), content);
                            } catch (Exception e) {
                                tempMap.put(entry.getName(), "[Бинарный блок данных IED]");
                            }
                        }
                    }
                    zipFile.close();
                } else {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        String content = readTextStreamSmart(fis);
                        tempMap.put(file.getName(), content);
                    } catch (Exception e) {
                        tempMap.put(file.getName(), "[Бинарный файл]");
                    }
                }

                handler.post(() -> {
                    progress.dismiss();
                    if (!tempMap.isEmpty()) {
                        projectFiles = tempMap;
                        activeNodeFile = projectFiles.keySet().iterator().next();
                        currentProjectName = file.getName();
                        parseActiveNodeParameters();
                        statusTextView.setText(" Проект: " + currentProjectName + " успешно загружен!");
                        renderWorkspace();
                        Toast.makeText(this, "Проект PCM600 открыт!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Архив пуст", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                handler.post(() -> {
                    progress.dismiss();
                    Toast.makeText(this, "Ошибка чтения: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // Умный ридер с автоматическим определением UTF-8 / UTF-16 и фильтрацией бинарного мусора
    private String readTextStreamSmart(InputStream is) {
        try {
            byte[] bytes = readAllBytes(is);
            if (bytes.length < 2) return "";

            // Проверка на UTF-16 (например, XML от ABB)
            if ((bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) || (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF)) {
                return new String(bytes, StandardCharsets.UTF_16);
            }

            String textUtf8 = new String(bytes, StandardCharsets.UTF_8);
            if (textUtf8.contains("<?xml") || textUtf8.contains("<TemplateInformation>") || textUtf8.contains("FunctionGroup")) {
                return textUtf8;
            }

            // Если файл текстовый / лог
            if (isTextReadable(textUtf8)) {
                return textUtf8;
            }

            // Если это бинарный или неизвестный DAT/LDF файл, извлекаем понятные строки и параметры для наладки
            StringBuilder extracted = new StringBuilder();
            extracted.append("--- PCM600 IED Binary / Parameter Block ---\n");
            
            String currentGroup = "General_IED_Settings";
            extracted.append("FunctionGroup: ").append(currentGroup).append("\n");

            int paramId = 1;
            for (int i = 0; i < bytes.length - 3; i++) {
                if (bytes[i] >= 32 && bytes[i] <= 126 && bytes[i+1] >= 32 && bytes[i+1] <= 126) {
                    int start = i;
                    while (i < bytes.length && bytes[i] >= 32 && bytes[i] <= 126) {
                        i++;
                    }
                    String token = new String(bytes, start, i - start, StandardCharsets.US_ASCII).trim();
                    if (token.length() > 3 && token.length() < 30 && !token.contains("")) {
                        if (token.matches(".*[A-Z].*") && (token.contains("PC") || token.contains("PT") || token.contains("TRIP") || token.contains("AR") || token.contains("OC"))) {
                            currentGroup = token;
                            extracted.append("\nFunctionGroup: ").append(currentGroup).append("\n");
                        } else {
                            extracted.append("  - Param_").append(paramId++).append(": ").append(token).append("\n");
                        }
                    }
                }
            }
            return extracted.toString();
        } catch (Exception e) {
            return "[Ошибка чтения потока данных]";
        }
    }

    private byte[] readAllBytes(InputStream inputStream) throws java.io.IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    private boolean isTextReadable(String s) {
        int printable = 0;
        int total = Math.min(s.length(), 500);
        for (int i = 0; i < total; i++) {
            char c = s.charAt(i);
            if (c >= 32 || c == '\n' || c == '\r' || c == '\t') printable++;
        }
        return (double) printable / total > 0.85;
    }

    private void parseActiveNodeParameters() {
        parsedParameters.clear();
        String content = projectFiles.get(activeNodeFile);
        if (content == null) return;

        String currentGroup = "General Settings";
        Map<String, String> groupParams = new LinkedHashMap<>();
        parsedParameters.put(currentGroup, groupParams);

        String[] lines = content.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("FunctionGroup:") || trimmed.startsWith("<") && trimmed.contains("name=")) {
                currentGroup = trimmed.replace("FunctionGroup:", "").replace("<", "").trim();
                groupParams = new LinkedHashMap<>();
                parsedParameters.put(currentGroup, groupParams);
            } else if (trimmed.contains(":") || trimmed.contains("=")) {
                String[] parts = trimmed.contains(":") ? trimmed.split(":", 2) : trimmed.split("=", 2);
                if (parts.length == 2) {
                    String k = parts[0].replace("-", "").replace("<Setting parameter=", "").replace("\"", "").trim();
                    String v = parts[1].replace("/>", "").replace("\"", "").trim();
                    groupParams.put(k, v);
                }
            }
        }

        // Если парсер не нашел групп, создаем стандартные уставки для удобной наладки
        if (groupParams.isEmpty()) {
            groupParams.put("IED_Device_Status", "Active / Normal");
            groupParams.put("Configuration_Version", "v5.2.3");
            groupParams.put("Communication_Protocol", "IEC61850");
            parsedParameters.put("IED Core Settings", groupParams);
        }
    }

    private void toggleViewMode() {
        isParamEditMode = !isParamEditMode;
        if (isParamEditMode) {
            btnToggleView.setText("⚙️ Уставки");
            btnToggleView.setBackgroundColor(Color.parseColor("#047857"));
            statusTextView.setText(" Режим: Сырой редактор кода [Raw Editor]");
        } else {
            if (rawEditorInput != null && !activeNodeFile.isEmpty()) {
                projectFiles.put(activeNodeFile, rawEditorInput.getText().toString());
                parseActiveNodeParameters();
            }
            btnToggleView.setText("📝 Raw Код");
            btnToggleView.setBackgroundColor(Color.parseColor("#1E293B"));
            statusTextView.setText(" Проект: " + currentProjectName + " | Блок: " + activeNodeFile);
        }
        renderWorkspace();
    }

    private void renderWorkspace() {
        containerLayout.removeAllViews();
        propertyGridContainer.removeAllViews();

        // Рендер дерева узлов IED (Вкладки файлов)
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
                parseActiveNodeParameters();
                statusTextView.setText(" Проект: " + currentProjectName + " | Блок: " + activeNodeFile);
                renderWorkspace();
            });
            containerLayout.addView(nodeBtn);
        }

        String content = projectFiles.get(activeNodeFile);
        if (content == null) content = "";

        if (isParamEditMode) {
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
            // Окно Параметров и Наладки блоков (PCM600 Parameter Setting Grid)
            TextView gridHeader = new TextView(this);
            gridHeader.setText("⚙️ Таблица уставок и наладки блоков: " + activeNodeFile);
            gridHeader.setTextColor(Color.parseColor("#FBBF24"));
            gridHeader.setTextSize(12f);
            gridHeader.setTypeface(Typeface.DEFAULT_BOLD);
            gridHeader.setPadding(4, 4, 4, 12);
            propertyGridContainer.addView(gridHeader);

            for (Map.Entry<String, Map<String, String>> groupEntry : parsedParameters.entrySet()) {
                String groupName = groupEntry.getKey();
                Map<String, String> params = groupEntry.getValue();

                // Заголовок группы функции
                TextView groupHeader = new TextView(this);
                groupHeader.setText("📌 [Функциональный блок]: " + groupName);
                groupHeader.setTextColor(Color.parseColor("#38BDF8"));
                groupHeader.setTextSize(11f);
                groupHeader.setTypeface(Typeface.DEFAULT_BOLD);
                groupHeader.setPadding(4, 12, 4, 6);
                propertyGridContainer.addView(groupHeader);

                for (Map.Entry<String, String> paramEntry : params.entrySet()) {
                    propertyGridContainer.addView(createParameterTuningCard(groupName, paramEntry.getKey(), paramEntry.getValue()));
                }
            }
        }
    }

    private View createParameterTuningCard(String group, String paramKey, String paramVal) {
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
        nameView.setText(paramKey);
        nameView.setTextColor(Color.parseColor("#94A3B8"));
        nameView.setTextSize(12f);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        nameView.setLayoutParams(nameParams);
        row.addView(nameView);

        // Интерактивное поле ввода для наладки уставки (как в Parameter Setting PCM600)
        final EditText valEdit = new EditText(this);
        valEdit.setText(paramVal);
        valEdit.setTextColor(Color.parseColor("#38BDF8"));
        valEdit.setTextSize(12f);
        valEdit.setTypeface(Typeface.MONOSPACE);
        valEdit.setBackgroundColor(Color.parseColor("#131B2E"));
        valEdit.setPadding(12, 8, 12, 8);
        
        LinearLayout.LayoutParams valParams = new LinearLayout.LayoutParams(250, ViewGroup.LayoutParams.WRAP_CONTENT);
        valEdit.setLayoutParams(valParams);

        valEdit.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                // Сохранение отрегулированного значения уставки
                parsedParameters.get(group).put(paramKey, valEdit.getText().toString());
                updateRawContentFromParameters();
            }
        });

        row.addView(valEdit);
        return row;
    }

    private void updateRawContentFromParameters() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<String, String>> g : parsedParameters.entrySet()) {
            sb.append("FunctionGroup: ").append(g.getKey()).append("\n");
            for (Map.Entry<String, String> p : g.getValue().entrySet()) {
                sb.append("  - ").append(p.getKey()).append(": ").append(p.getValue()).append("\n");
            }
        }
        projectFiles.put(activeNodeFile, sb.toString());
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
                InputStream in = getContentResolver().openInputStream(uri);
                File tempZip = new File(getCacheDir(), "import_ied.zip");
                FileOutputStream out = new FileOutputStream(tempZip);
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
                out.close();
                in.close();

                loadArchiveFileSafely(tempZip);
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка открытия: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void saveApcmiArchive() {
        if (isParamEditMode && rawEditorInput != null && !activeNodeFile.isEmpty()) {
            projectFiles.put(activeNodeFile, rawEditorInput.getText().toString());
        } else {
            updateRawContentFromParameters();
        }

        ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("💾 Сохранение IED");
        progress.setMessage("Упаковка параметров и архива .apcmi...");
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
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

                handler.post(() -> {
                    progress.dismiss();
                    Toast.makeText(this, "💾 Проект PCM600 сохранен в Загрузки!", Toast.LENGTH_LONG).show();
                    statusTextView.setText(" Сохранено: " + outFile.getAbsolutePath());
                });
            } catch (Exception e) {
                handler.post(() -> {
                    progress.dismiss();
                    Toast.makeText(this, "Ошибка сохранения: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private Button createStyledButton(String text, String colorHex, int widthDp) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.parseColor("#F8FAFC"));
        btn.setTextSize(11f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        btn.setPadding(8, 8, 8, 8);
        
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