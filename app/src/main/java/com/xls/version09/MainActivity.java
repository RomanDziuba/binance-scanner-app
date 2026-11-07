package com.xls.version09;

import android.view.ViewGroup;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import org.apache.poi.ss.usermodel.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

// THEME: @android:style/Theme.Material.Light.NoActionBar
public class MainActivity extends Activity {

    private static final int PICK_EXCEL_REQUEST = 2002;
    private LinearLayout rootLayout;
    private TextView statusTextView, logTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));
        setContentView(rootLayout);

        showSplashScreen();
        checkAndRequestStoragePermissions();
    }

    private void showSplashScreen() {
        LinearLayout splashContainer = new LinearLayout(this);
        splashContainer.setOrientation(LinearLayout.VERTICAL);
        splashContainer.setGravity(Gravity.CENTER);
        splashContainer.setPadding(40, 40, 40, 40);

        LinearLayout excelIconBox = new LinearLayout(this);
        excelIconBox.setGravity(Gravity.CENTER);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setColor(Color.parseColor("#107C41"));
        iconBg.setCornerRadius(32f);
        excelIconBox.setBackground(iconBg);
        
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(160, 160);
        iconParams.setMargins(0, 0, 0, 30);
        excelIconBox.setLayoutParams(iconParams);

        TextView iconText = new TextView(this);
        iconText.setText("XLS");
        iconText.setTextColor(Color.WHITE);
        iconText.setTextSize(26f);
        iconText.setTypeface(Typeface.DEFAULT_BOLD);
        excelIconBox.addView(iconText);
        splashContainer.addView(excelIconBox);

        TextView titleView = new TextView(this);
        titleView.setText("Universal Excel Reader Pro");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(22f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        splashContainer.addView(titleView);

        TextView descView = new TextView(this);
        descView.setText("Advanced high-precision spreadsheet parsing and universal file management engine supporting all workbook formats.");
        descView.setTextColor(Color.parseColor("#94A3B8"));
        descView.setTextSize(12f);
        descView.setGravity(Gravity.CENTER);
        descView.setPadding(20, 15, 20, 30);
        splashContainer.addView(descView);

        TextView authorView = new TextView(this);
        authorView.setText("Created by Dziuba Roman • 2026");
        authorView.setTextColor(Color.parseColor("#F59E0B"));
        authorView.setTextSize(13f);
        authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        authorView.setGravity(Gravity.CENTER);
        splashContainer.addView(authorView);

        rootLayout.addView(splashContainer);

        new Handler(Looper.getMainLooper()).postDelayed(this::initMainInterface, 3500);
    }

    private void initMainInterface() {
        rootLayout.removeAllViews();
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(20, 20, 20, 20);
        container.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView header = new TextView(this);
        header.setText("📊 Universal Excel Core Reader");
        header.setTextColor(Color.parseColor("#38BDF8"));
        header.setTextSize(18f);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setGravity(Gravity.CENTER);
        header.setPadding(0, 0, 0, 15);
        container.addView(header);

        statusTextView = new TextView(this);
        statusTextView.setText("Статус: Доступ к хранилищу активен. Готов к работе.");
        statusTextView.setTextColor(Color.parseColor("#10B981"));
        statusTextView.setTextSize(12f);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(0, 0, 0, 15);
        container.addView(statusTextView);

        Button btnOpenExcel = createStyledButton("📂 Открыть файл Excel через проводник", "#2563EB");
        btnOpenExcel.setOnClickListener(v -> openFilePicker());
        container.addView(btnOpenExcel);

        Button btnScanStorage = createStyledButton("🔍 Сканировать устройство (Свежие таблицы)", "#0D9488");
        btnScanStorage.setOnClickListener(v -> scanAndShowExcelFilesDialog());
        container.addView(btnScanStorage);

        Button btnExit = createStyledButton("🚪 Закрыть приложение", "#B91C1C");
        btnExit.setOnClickListener(v -> finishAffinity());
        container.addView(btnExit);

        // Настраиваем поле вывода с поддержкой выделения и двумерного скролла
        logTextView = new TextView(this);
        logTextView.setText("Выберите таблицу для чтения...\n(Доступен скролл вправо/влево, вверх/вниз и выделение текста)");
        logTextView.setTextSize(12f);
        logTextView.setTextColor(Color.parseColor("#E2E8F0"));
        logTextView.setTypeface(Typeface.MONOSPACE);
        logTextView.setBackgroundColor(Color.parseColor("#0F172A"));
        logTextView.setPadding(20, 20, 20, 20);
        logTextView.setTextIsSelectable(true);
        logTextView.setHorizontallyScrolling(true);

        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);
        ScrollView verticalScrollView = new ScrollView(this);

        verticalScrollView.addView(logTextView);
        horizontalScrollView.addView(verticalScrollView);

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 800
        );
        scrollParams.setMargins(0, 15, 0, 0);
        horizontalScrollView.setLayoutParams(scrollParams);
        container.addView(horizontalScrollView);

        ScrollView mainScroll = new ScrollView(this);
        mainScroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
        ));
        mainScroll.addView(container);
        rootLayout.addView(mainScroll);
    }

    private Button createStyledButton(String text, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(12f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        btn.setPadding(15, 15, 15, 15);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 12);
        btn.setLayoutParams(params);
        return btn;
    }

    private void checkAndRequestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }
            }
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Выберите таблицу Excel"), PICK_EXCEL_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            processExcelFileWithPoi(data.getData());
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx != -1) result = cursor.getString(idx);
                }
            } catch (Exception ignored) {}
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result != null ? result.toLowerCase() : "unknown.xlsx";
    }

    /**
     * Профессиональный парсинг любых версий Excel через Apache POI
     */
    private void processExcelFileWithPoi(Uri uri) {
        String fileName = getFileName(uri);
        statusTextView.setText("Статус: 🔄 Чтение и форматирование таблицы...");

        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            try {
                File tempFile = new File(getCacheDir(), "poi_temp_file.xls");
                try (InputStream in = getContentResolver().openInputStream(uri);
                     FileOutputStream out = new FileOutputStream(tempFile)) {
                    if (in == null) throw new Exception("Не удалось открыть поток файла");
                    byte[] buffer = new byte[16384];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    out.flush();
                }

                sb.append("📂 Файл: ").append(fileName).append("\n");
                sb.append("📦 Размер: ").append(tempFile.length()).append(" байт\n");
                sb.append("==================================================\n\n");

                if (fileName.endsWith(".csv")) {
                    parseCsvFile(tempFile, sb);
                } else {
                    // Используем Apache POI для чтения .xls и .xlsx файлов любой версии
                    try (InputStream fileIn = new FileInputStream(tempFile);
                         Workbook workbook = WorkbookFactory.create(fileIn)) {
                        
                        Sheet sheet = workbook.getSheetAt(0); // Берем первый лист
                        sb.append("📊 Лист: ").append(sheet.getSheetName()).append(" | Строк: ").append(sheet.getPhysicalNumberOfRows()).append("\n\n");

                        DataFormatter formatter = new DataFormatter();
                        int rowCount = 0;

                        for (Row row : sheet) {
                            if (rowCount > 500) {
                                sb.append("\n... (показано первые 500 строк для оптимизации) ...");
                                break;
                            }
                            sb.append(String.format("[%3d] ", (row.getRowNum() + 1)));

                            for (Cell cell : row) {
                                String val = formatter.formatCellValue(cell);
                                sb.append(val).append("\t | \t");
                            }
                            sb.append("\n");
                            rowCount++;
                        }
                    }
                }

                tempFile.delete();

            } catch (Exception e) {
                sb.append("❌ Ошибка при чтении таблицы: ").append(e.getMessage());
            }

            final String output = sb.toString();
            new Handler(Looper.getMainLooper()).post(() -> {
                logTextView.setText(output);
                statusTextView.setText("Статус: ✅ Таблица успешно загружена!");
            });
        }).start();
    }

    private void parseCsvFile(File file, StringBuilder sb) throws Exception {
        sb.append("📊 Таблица CSV\n\n");
        try (BufferedReader br = new BufferedReader(new java.io.InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            int count = 0;
            while ((line = br.readLine()) != null && count < 500) {
                sb.append(line.replace(",", "\t | \t")).append("\n");
                count++;
            }
        }
    }

    private void scanAndShowExcelFilesDialog() {
        statusTextView.setText("Статус: 🔍 Поиск таблиц на устройстве...");
        new Thread(() -> {
            List<File> foundFiles = new ArrayList<>();
            try {
                recursiveSearch(Environment.getExternalStorageDirectory(), foundFiles);
            } catch (Exception ignored) {}

            // Сортировка по новизне: самые свежие первыми
            Collections.sort(foundFiles, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            List<String> displayList = new ArrayList<>();
            for (File f : foundFiles) {
                String dateStr = sdf.format(new Date(f.lastModified()));
                displayList.add("📄 " + f.getName() + "\n📅 " + dateStr + " | 📦 " + (f.length() / 1024) + " КБ");
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                statusTextView.setText("Статус: Найдено таблиц: " + foundFiles.size());
                if (foundFiles.isEmpty()) {
                    Toast.makeText(this, "Таблицы не найдены на устройстве", Toast.LENGTH_SHORT).show();
                    return;
                }

                LinearLayout dialogLayout = new LinearLayout(this);
                dialogLayout.setOrientation(LinearLayout.VERTICAL);
                dialogLayout.setPadding(15, 15, 15, 15);
                dialogLayout.setBackgroundColor(Color.parseColor("#090D16"));

                ListView listView = new ListView(this);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, displayList) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);
                        TextView tv = view.findViewById(android.R.id.text1);
                        if (tv != null) {
                            tv.setTextColor(Color.parseColor("#E2E8F0"));
                            tv.setTextSize(11f);
                            tv.setPadding(12, 10, 12, 10);
                        }
                        view.setBackgroundColor(Color.parseColor("#0F172A"));
                        return view;
                    }
                };
                listView.setAdapter(adapter);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 900
                );
                listView.setLayoutParams(params);
                dialogLayout.addView(listView);

                AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("📁 Найденные таблицы (от свежих к старым)")
                    .setView(dialogLayout)
                    .setNegativeButton("Закрыть", null)
                    .create();

                listView.setOnItemClickListener((parent, view, position, id) -> {
                    File selectedFile = foundFiles.get(position);
                    dialog.dismiss();
                    processExcelFileWithPoi(Uri.fromFile(selectedFile));
                });

                dialog.show();
            });
        }).start();
    }

    private void recursiveSearch(File dir, List<File> list) {
        if (dir == null || !dir.isDirectory() || list.size() > 150) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory() && !f.getName().startsWith(".")) {
                recursiveSearch(f, list);
            } else if (f.isFile()) {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".xlsx") || name.endsWith(".xls") || name.endsWith(".csv")) {
                    list.add(f);
                }
            }
        }
    }
}