package com.xls.version1;

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
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
    private TextView statusTextView;
    private TableLayout tableLayout;
    private Button btnSaveExcel;
    
    // Хранилище данных и оригинальных цветов ячеек
    private List<List<String>> currentTableData = new ArrayList<>();
    private List<List<Integer>> currentTableColors = new ArrayList<>();
    private String activeFileName = "edited_table.xlsx";

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
        iconText.setText("😛");
        iconText.setTextColor(Color.WHITE);
        iconText.setTextSize(26f);
        iconText.setTypeface(Typeface.DEFAULT_BOLD);
        excelIconBox.addView(iconText);
        splashContainer.addView(excelIconBox);

        TextView titleView = new TextView(this);
        titleView.setText("Universal Excel Reader v_1 🇺🇦");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(22f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        splashContainer.addView(titleView);

        TextView descView = new TextView(this);
        descView.setText("Advanced spreadsheet parsing, formula evaluation & original color rendering.");
        descView.setTextColor(Color.parseColor("#94A3B8"));
        descView.setTextSize(12f);
        descView.setGravity(Gravity.CENTER);
        descView.setPadding(20, 15, 20, 30);
        splashContainer.addView(descView);

        TextView authorView = new TextView(this);
        authorView.setText("Created by Dziuba Roman • 2026 🇺🇦");
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
        header.setText("🧾 Universal Excel Reader & Editor v1 🇺🇦");
        header.setTextColor(Color.parseColor("#38BDF8"));
        header.setTextSize(18f);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setGravity(Gravity.CENTER);
        header.setPadding(0, 0, 0, 15);
        container.addView(header);

        statusTextView = new TextView(this);
        statusTextView.setText("Статус: Готовий до роботи!🚀");
        statusTextView.setTextColor(Color.parseColor("#10B981"));
        statusTextView.setTextSize(12f);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(0, 0, 0, 15);
        container.addView(statusTextView);

        Button btnOpenExcel = createStyledButton("📂 Відкрити файл Excel", "#2563EB");
        btnOpenExcel.setOnClickListener(v -> openFilePicker());
        container.addView(btnOpenExcel);

        Button btnScanStorage = createStyledButton("🔍 Сканувати свіжі таблиці", "#0D9488");
        btnScanStorage.setOnClickListener(v -> scanAndShowExcelFilesDialog());
        container.addView(btnScanStorage);

        btnSaveExcel = createStyledButton("💾 Зберегти зміни в Excel (Downloads)", "#D97706");
        btnSaveExcel.setVisibility(View.GONE);
        btnSaveExcel.setOnClickListener(v -> saveModifiedExcelFile());
        container.addView(btnSaveExcel);

        Button btnExit = createStyledButton("🚪 Закрити додаток", "#B91C1C");
        btnExit.setOnClickListener(v -> finishAffinity());
        container.addView(btnExit);

        // Таблица с увеличенной высотой (1700) для удобного обзора
        tableLayout = new TableLayout(this);
        tableLayout.setPadding(5, 5, 5, 5);

        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);
        ScrollView verticalScrollView = new ScrollView(this);

        verticalScrollView.addView(tableLayout);
        horizontalScrollView.addView(verticalScrollView);

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1700
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
        
        renderInitialPlaceholder();
    }

    private void renderInitialPlaceholder() {
        tableLayout.removeAllViews();
        TableRow row = new TableRow(this);
        TextView tv = new TextView(this);
        tv.setText(" 👉 Відкрийте  файл Excel 👈");
        tv.setTextColor(Color.parseColor("#94A3B8"));
        tv.setTextSize(12f);
        tv.setPadding(20, 20, 20, 20);
        row.addView(tv);
        tableLayout.addView(row);
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
        startActivityForResult(Intent.createChooser(intent, "Виберіть таблицю Excel 📑"), PICK_EXCEL_REQUEST);
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
        return result != null ? result : "table.xlsx";
    }

    /**
     * Безопасное чтение Excel с защитой от отсутствия AWT в Android и поддержкой лимита 600 строк
     */
    private void processExcelFileWithPoi(Uri uri) {
        activeFileName = getFileName(uri);
        statusTextView.setText("Статус: 🔄 Читання та встановлення кольорів 🌈");

        new Thread(() -> {
            currentTableData.clear();
            currentTableColors.clear();
            try {
                File tempFile = new File(getCacheDir(), "poi_temp_file.xls");
                try (InputStream in = getContentResolver().openInputStream(uri);
                     FileOutputStream out = new FileOutputStream(tempFile)) {
                    if (in == null) throw new Exception("Не вдалося відкрити  файл 😔");
                    byte[] buffer = new byte[16384];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    out.flush();
                }

                if (activeFileName.toLowerCase().endsWith(".csv")) {
                    parseCsvFile(tempFile);
                } else {
                    try (InputStream fileIn = new FileInputStream(tempFile);
                         Workbook workbook = WorkbookFactory.create(fileIn)) {
                        
                        Sheet sheet = workbook.getSheetAt(0);
                        DataFormatter formatter = new DataFormatter();
                        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                        
                        int rowCount = 0;

                        for (Row row : sheet) {
                            if (rowCount > 600) break; // Лимит 600 строк
                            List<String> rowCells = new ArrayList<>();
                            List<Integer> rowCellColors = new ArrayList<>();
                            
                            int maxCells = Math.max(row.getLastCellNum(), 1);
                            boolean isRowEmpty = true;

                            for (int c = 0; c < maxCells; c++) {
                                Cell cell = row.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                                String val = formatter.formatCellValue(cell, evaluator);
                                rowCells.add(val);
                                
                                if (val != null && !val.trim().isEmpty()) {
                                    isRowEmpty = false;
                                }

                                // Безопасное извлечение цвета заливки (только для XSSF/xlsx во избежание сбоев AWT в Android)
                                int cellColor = (rowCount == 0) ? Color.parseColor("#1E293B") : Color.parseColor("#0F172A");
                                try {
                                    if (workbook instanceof XSSFWorkbook) {
                                        CellStyle style = cell.getCellStyle();
                                        if (style != null) {
                                            org.apache.poi.ss.usermodel.Color poiColor = style.getFillForegroundColorColor();
                                            if (poiColor instanceof XSSFColor) {
                                                byte[] rgb = ((XSSFColor) poiColor).getRGB();
                                                if (rgb != null && rgb.length >= 3) {
                                                    int r = rgb[0] & 0xFF;
                                                    int g = rgb[1] & 0xFF;
                                                    int b = rgb[2] & 0xFF;
                                                    if (!(r > 245 && g > 245 && b > 245) && !(r < 10 && g < 10 && b < 10)) {
                                                        cellColor = Color.rgb(r, g, b);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (Throwable ignored) {}
                                
                                rowCellColors.add(cellColor);
                            }
                            
                            // Пропускаем полностью пустые строки
                            if (!isRowEmpty) {
                                currentTableData.add(rowCells);
                                currentTableColors.add(rowCellColors);
                                rowCount++;
                            }
                        }
                    }
                }

            } catch (Throwable e) {
                List<String> errRow = new ArrayList<>();
                errRow.add("❌ Помилка читання: " + e.getMessage());
                currentTableData.add(errRow);
                List<Integer> errColor = new ArrayList<>();
                errColor.add(Color.parseColor("#991B1B"));
                currentTableColors.add(errColor);
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                buildTableUI();
                btnSaveExcel.setVisibility(View.VISIBLE);
                statusTextView.setText("Статус: ✅ Завантажено !👍");
            });
        }).start();
    }

    private void parseCsvFile(File file) throws Exception {
        try (BufferedReader br = new BufferedReader(new java.io.InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            int count = 0;
            while ((line = br.readLine()) != null && count < 600) {
                String[] parts = line.split("[,;]");
                currentTableData.add(Arrays.asList(parts));
                List<Integer> rowColors = new ArrayList<>();
                for (String p : parts) rowColors.add(Color.parseColor("#0F172A"));
                currentTableColors.add(rowColors);
                count++;
            }
        }
    }

    private void buildTableUI() {
        tableLayout.removeAllViews();

        for (int r = 0; r < currentTableData.size(); r++) {
            List<String> rowData = currentTableData.get(r);
            List<Integer> rowColors = currentTableColors.get(r);
            TableRow tableRow = new TableRow(this);

            // Нумерация строк
            TextView indexTv = createCellView(String.valueOf(r + 1), true, r == 0, Color.parseColor("#1E293B"));
            tableRow.addView(indexTv);

            for (int c = 0; c < rowData.size(); c++) {
                final int rowIndex = r;
                final int colIndex = c;
                String cellValue = rowData.get(c);
                int cellBgColor = (c < rowColors.size()) ? rowColors.get(c) : Color.parseColor("#0F172A");

                TextView cellTv = createCellView(cellValue, false, r == 0, cellBgColor);
                cellTv.setOnClickListener(v -> showEditCellDialog(rowIndex, colIndex));
                
                tableRow.addView(cellTv);
            }
            tableLayout.addView(tableRow);
        }
    }

    private TextView createCellView(String text, boolean isHeaderIndex, boolean isTopHeaderRow, int customBgColor) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(11f);
        
        // Автоматически настраиваем цвет текста (темный для светлых фонов, белый для темных)
        int textColor = Color.parseColor("#E2E8F0");
        if (isTopHeaderRow || isHeaderIndex) {
            textColor = Color.parseColor("#38BDF8");
        } else if (customBgColor != Color.parseColor("#0F172A") && customBgColor != Color.parseColor("#1E293B")) {
            int red = Color.red(customBgColor);
            int green = Color.green(customBgColor);
            int blue = Color.blue(customBgColor);
            double luminance = (0.299 * red + 0.587 * green + 0.114 * blue);
            if (luminance > 130) {
                textColor = Color.parseColor("#0F172A"); // Темный текст на цветном/светлом фоне
            } else {
                textColor = Color.parseColor("#FFFFFF"); // Белый текст на темном фоне
            }
        }
        tv.setTextColor(textColor);

        if (isTopHeaderRow || isHeaderIndex) {
            tv.setTypeface(Typeface.DEFAULT_BOLD);
        }
        tv.setPadding(16, 12, 16, 12);
        tv.setMaxWidth(400);
        tv.setSingleLine(false);

        GradientDrawable cellBg = new GradientDrawable();
        cellBg.setColor(customBgColor);
        cellBg.setStroke(1, Color.parseColor("#334155"));
        tv.setBackground(cellBg);

        return tv;
    }

    private void showEditCellDialog(int rowIndex, int colIndex) {
        String currentValue = currentTableData.get(rowIndex).get(colIndex);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        final EditText input = new EditText(this);
        input.setText(currentValue);
        input.setTextColor(Color.parseColor("#0F172A"));
        input.setBackgroundColor(Color.parseColor("#E2E8F0"));
        input.setTextSize(13f);
        input.setPadding(20, 20, 20, 20);
        layout.addView(input);

        new AlertDialog.Builder(this)
            .setTitle("✏️ Редагувати  комірку [" + (rowIndex + 1) + ", " + (colIndex + 1) + "]")
            .setView(layout)
            .setPositiveButton("Зберегти", (dialog, which) -> {
                String newValue = input.getText().toString();
                currentTableData.get(rowIndex).set(colIndex, newValue);
                buildTableUI();
                Toast.makeText(this, "Комірка оновлена!👌", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Відміна🙅", null)
            .show();
    }

    private void saveModifiedExcelFile() {
        statusTextView.setText("Статус: 💾 Збереження файла...");
        new Thread(() -> {
            try {
                File tempFile = new File(getCacheDir(), "poi_temp_file.xls");
                Workbook workbook;
                
                if (tempFile.exists()) {
                    InputStream fileIn = new FileInputStream(tempFile);
                    workbook = WorkbookFactory.create(fileIn);
                    fileIn.close();
                } else {
                    workbook = new XSSFWorkbook();
                }

                Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : workbook.createSheet("Sheet1");

                for (int r = 0; r < currentTableData.size(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) {
                        row = sheet.createRow(r);
                    }
                    List<String> rowData = currentTableData.get(r);
                    for (int c = 0; c < rowData.size(); c++) {
                        Cell cell = row.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        cell.setCellValue(rowData.get(c));
                    }
                }

                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) downloadsDir.mkdirs();

                File outFile = new File(downloadsDir, "edited_" + activeFileName.replaceAll("\\.[^.]+$", "") + ".xlsx");
                
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    workbook.write(fos);
                }
                workbook.close();

                new Handler(Looper.getMainLooper()).post(() -> {
                    statusTextView.setText("Статус: ✅ Успішне збереження!🔥");
                    Toast.makeText(this, "Файл збережен до  Загрузок 👌: " + outFile.getName(), Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    statusTextView.setText("Статус: ❌ Помилка збереження 🤷");
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void scanAndShowExcelFilesDialog() {
        statusTextView.setText("Статус: 🔍 Пошук таблиць на пристрої...👀");
        new Thread(() -> {
            List<File> foundFiles = new ArrayList<>();
            try {
                recursiveSearch(Environment.getExternalStorageDirectory(), foundFiles);
            } catch (Exception ignored) {}

            Collections.sort(foundFiles, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            List<String> displayList = new ArrayList<>();
            for (File f : foundFiles) {
                String dateStr = sdf.format(new Date(f.lastModified()));
                displayList.add("📄 " + f.getName() + "\n📅 " + dateStr + " | 📦 " + (f.length() / 1024) + " КБ");
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                statusTextView.setText("Статус: Знайдено таблиць: 👉" + foundFiles.size());
                if (foundFiles.isEmpty()) {
                    Toast.makeText(this, "Таблиці не знайдені на пристрої 👀", Toast.LENGTH_SHORT).show();
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
                    .setTitle("📁 Знайдені таблиці")
                    .setView(dialogLayout)
                    .setNegativeButton("Закрити 🙅", null)
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