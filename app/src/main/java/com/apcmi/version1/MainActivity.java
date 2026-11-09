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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

// THEME: @android:style/Theme.Material.Light.NoActionBar
public class MainActivity extends Activity {

    private static final int PICK_APC_FILE = 3001;
    private static final int PERMISSION_REQUEST_CODE = 1002;

    private boolean isParamEditMode = false;
    private String currentProjectName = "Substation_IED_Config.apcmi";
    
    // Хранилище файлов проекта
    private Map<String, String> projectFiles = new LinkedHashMap<>();
    private String activeNodeFile = "";

    private LinearLayout containerLayout, propertyGridContainer;
    private ScrollView mainScroll;
    private TextView statusTextView, projectTitleView;
    private Button btnToggleView, btnOpenProject, btnSaveProject, btnScanPhone;
    private EditText rawEditorInput;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Проверка и запрос разрешений на чтение памяти для сканирования телефона
        checkStoragePermissions();

        // Демо-данные
        projectFiles.put("config.json", "{\n  \"ied_name\": \"REF615_Feeder_Protection\",\n  \"station_voltage_kv\": 110,\n  \"relay_group\": \"Chint_NR2_NC2\",\n  \"trip_delay_ms\": 150\n}");
        activeNodeFile = "config.json";

        // Главный контейнер в стиле PCM600
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

        // --- ПАНЕЛЬ ДЕЙСТВИЙ И СКАНИРОВАНИЯ ---
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
        statusTextView.setText(" Проект: " + currentProjectName + " | Узел: " + activeNodeFile);
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

        // Таблица параметров / Свойств (Property Grid)
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
        progress.setMessage("Поиск файлов проектов (.apcmi, .zip, .cff) на устройстве...");
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
                // Пропускаем системные папки для скорости
                String name = f.getName().toLowerCase();
                if (!name.startsWith(".") && !name.equals("android") && !name.equals("data")) {
                    recursiveSearch(f, result);
                }
            } else {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".apcmi") || name.endsWith(".zip") || name.endsWith(".cff") || name.endsWith(".dat")) {
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
            .setTitle("📁 Найденные файлы (" + files.size() + ")")
            .setItems(items, (dialog, which) -> {
                File selected = files.get(which);
                loadArchiveFileSafely(selected);
            })
            .setNegativeButton("Закрыть", null)
            .show();
    }

    private void loadArchiveFileSafely(File file) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("📂 Загрузка проекта");
        progress.setMessage("Чтение и парсинг структуры IED...");
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
                                BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
                                StringBuilder sb = new StringBuilder();
                                String line;
                                int lineCount = 0;
                                // Защита от зависания на огромных файлах: читаем до 5000 строк
                                while ((line = reader.readLine()) != null && lineCount < 5000) {
                                    sb.append(line).append("\n");
                                    lineCount++;
                                }
                                tempMap.put(entry.getName(), sb.toString());
                            } catch (Exception e) {
                                tempMap.put(entry.getName(), "[Бинарный или защищенный файл данных]");
                            }
                        }
                    }
                    zipFile.close();
                } else {
                    // Одиночный файл (.cff, .dat, .xml)
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        int lineCount = 0;
                        while ((line = reader.readLine()) != null && lineCount < 5000) {
                            sb.append(line).append("\n");
                            lineCount++;
                        }
                        tempMap.put(file.getName(), sb.toString());
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
                        statusTextView.setText(" Проект: " + currentProjectName + " успешно загружен!");
                        renderWorkspace();
                        Toast.makeText(this, "Проект успешно открыт!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Архив пуст", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                handler.post(() -> {
                    progress.dismiss();
                    Toast.makeText(this, "Ошибка чтения файла: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
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

        // Отрисовка дерева узлов IED (Вкладки) без лагов
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

        // Отрисовка содержимого
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
            TextView gridHeader = new TextView(this);
            gridHeader.setText("⚙️ Параметры узла: " + activeNodeFile);
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
                // Если не JSON, выводим отформатированный текст без зависаний
                TextView textCard = new TextView(this);
                textCard.setText(content);
                textCard.setTextColor(Color.parseColor("#E2E8F0"));
                textCard.setTextSize(11f);
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
        }

        ProgressDialog progress = new ProgressDialog(this);
        progress.setTitle("💾 Сохранение");
        progress.setMessage("Упаковка проекта в .apcmi архив...");
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
                    Toast.makeText(this, "💾 Проект успешно сохранен в Загрузки!", Toast.LENGTH_LONG).show();
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