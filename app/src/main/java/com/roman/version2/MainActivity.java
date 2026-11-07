package com.roman.version2;

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
    
    // Внутреннее хранилище данных таблицы для отображения и редактирования
    private List<List<String>> currentTableData = new ArrayList<>();
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
        iconText.setText("😜");
        iconText.setTextColor(Color.WHITE);
        iconText.setTextSize(26f);
        iconText.setTypeface(Typeface.DEFAULT_BOLD);
        excelIconBox.addView(iconText);
        splashContainer.addView(excelIconBox);

        TextView titleView = new TextView(this);
        titleView.setText("Universal Excel Reader ");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(22f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        splashContainer.addView(titleView);

        TextView descView = new TextView(this);
        descView.setText("Advanced spreadsheet parsing, formula evaluation engine & interactive grid editing.");
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
        header.setText("🧾 Universal Excel Reader & Editor ");
        header.setTextColor(Color.parseColor("#38BDF8"));
        header.setTextSize(18f);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setGravity(Gravity.CENTER);
        header.setPadding(0, 0, 0, 15);
        container.addView(header);

        statusTextView = new TextView(this);
        statusTextView.setText("Статус: Готов к работе. Нажмите ячейку для редактирования.");
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

        btnSaveExcel = createStyledButton("💾 Сохранить изменения в Excel (Downloads)", "#D97706");
        btnSaveExcel.setVisibility(View.GONE); // Появляется после загрузки файла
        btnSaveExcel.setOnClickListener(v -> saveModifiedExcelFile());
        container.addView(btnSaveExcel);

        Button btnExit = createStyledButton("🚪 Закрыть приложение", "#B91C1C");
        btnExit.setOnClickListener(v -> finishAffinity());
        container.addView(btnExit);

        // Инициализация графической сетки табличного интерфейса
        tableLayout = new TableLayout(this);
        tableLayout.setPadding(5, 5, 5, 5);

        HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);
        ScrollView verticalScrollView = new ScrollView(this);

        verticalScrollView.addView(tableLayout);
        horizontalScrollView.addView(verticalScrollView);

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1200
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
        
        // Показать начальную подсказку в таблице
        renderInitialPlaceholder();
    }

    private void renderInitialPlaceholder() {
        tableLayout.removeAllViews();
        TableRow row = new TableRow(this);
        TextView tv = new TextView(this);
        tv.setText("   Здесь появится таблица. Откройте файл Excel.   ");
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
        return result != null ? result : "table.xlsx";
    }

    /**
     * Профессиональный парсинг Excel с вычислением формул через FormulaEvaluator
     */
    private void processExcelFileWithPoi(Uri uri) {
        activeFileName = getFileName(uri);
        statusTextView.setText("Статус: 🔄 Чтение и вычисление формул...");

        new Thread(() -> {
            currentTableData.clear();
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

                if (activeFileName.toLowerCase().endsWith(".csv")) {
                    parseCsvFile(tempFile);
                } else {
                    try (InputStream fileIn = new FileInputStream(tempFile);
                         Workbook workbook = WorkbookFactory.create(fileIn)) {
                        
                        Sheet sheet = workbook.getSheetAt(0);
                        DataFormatter formatter = new DataFormatter();
                        
                        // Инициализируем оценщик формул для автоматического расчета на лету
                        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                        
                        int rowCount = 0;

                        for (Row row : sheet) {
                            if (rowCount > 600) break; // Лимит 300 строк для высокой производительности UI
                            List<String> rowCells = new ArrayList<>();
                            
                            int maxCells = Math.max(row.getLastCellNum(), 1);
                            for (int c = 0; c < maxCells; c++) {
                                Cell cell = row.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                                // Передаем evaluator, чтобы формулы вычислялись и превращались в значения
                                rowCells.add(formatter.formatCellValue(cell, evaluator));
                            }
                            currentTableData.add(rowCells);
                            rowCount++;
                        }
                    }
                }
                tempFile.delete();

            } catch (Exception e) {
                List<String> errRow = new ArrayList<>();
                errRow.add("❌ Ошибка чтения: " + e.getMessage());
                currentTableData.add(errRow);
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                buildTableUI();
                btnSaveExcel.setVisibility(View.VISIBLE);
                statusTextView.setText("Статус: ✅ Загружено! Формулы вычислены.");
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
                count++;
            }
        }
    }

    /**
     * Динамическое построение визуальной сетки таблицы из данных в памяти
     */
    private void buildTableUI() {
        tableLayout.removeAllViews();

        for (int r = 0; r < currentTableData.size(); r++) {
            List<String> rowData = currentTableData.get(r);
            TableRow tableRow = new TableRow(this);

            // Нумерация строк (первая колонка)
            TextView indexTv = createCellView(String.valueOf(r + 1), true, r == 0);
            tableRow.addView(indexTv);

            for (int c = 0; c < rowData.size(); c++) {
                final int rowIndex = r;
                final int colIndex = c;
                String cellValue = rowData.get(c);

                TextView cellTv = createCellView(cellValue, false, r == 0);
                
                // Клик по ячейке для редактирования
                cellTv.setOnClickListener(v -> showEditCellDialog(rowIndex, colIndex));
                
                tableRow.addView(cellTv);
            }
            tableLayout.addView(tableRow);
        }
    }

    private TextView createCellView(String text, boolean isHeaderIndex, boolean isTopHeaderRow) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(11f);
        tv.setTextColor(isTopHeaderRow || isHeaderIndex ? Color.parseColor("#38BDF8") : Color.parseColor("#E2E8F0"));
        if (isTopHeaderRow || isHeaderIndex) {
            tv.setTypeface(Typeface.DEFAULT_BOLD);
        }
        tv.setPadding(16, 12, 16, 12);
        tv.setMaxWidth(400);
        tv.setSingleLine(false);

        GradientDrawable cellBg = new GradientDrawable();
        cellBg.setColor(isTopHeaderRow ? Color.parseColor("#1E293B") : Color.parseColor("#0F172A"));
        cellBg.setStroke(1, Color.parseColor("#334155"));
        tv.setBackground(cellBg);

        return tv;
    }

    /**
     * Диалоговое окно для редактирования содержимого конкретной ячейки
     */
    private void showEditCellDialog(int rowIndex, int colIndex) {
        String currentValue = currentTableData.get(rowIndex).get(colIndex);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        final EditText input = new EditText(this);
        input.setText(currentValue);
        input.setTextColor(Color.WHITE);
        input.setTextSize(13f);
        layout.addView(input);

        new AlertDialog.Builder(this)
            .setTitle("✏️ Редактировать ячейку [" + (rowIndex + 1) + ", " + (colIndex + 1) + "]")
            .setView(layout)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                String newValue = input.getText().toString();
                // Обновляем данные в оперативной памяти
                currentTableData.get(rowIndex).set(colIndex, newValue);
                // Перестраиваем таблицу на экране
                buildTableUI();
                Toast.makeText(this, "Ячейка обновлена!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    /**
     * Сохранение измененной таблицы обратно в Excel (.xlsx) в папку Загрузки
     */
    private void saveModifiedExcelFile() {
        statusTextView.setText("Статус: 💾 Сохранение файла...");
        new Thread(() -> {
            try {
                Workbook workbook = new XSSFWorkbook();
                Sheet sheet = workbook.createSheet("Edited Sheet");

                for (int r = 0; r < currentTableData.size(); r++) {
                    Row row = sheet.createRow(r);
                    List<String> rowData = currentTableData.get(r);
                    for (int c = 0; c < rowData.size(); c++) {
                        Cell cell = row.createCell(c);
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
                    statusTextView.setText("Статус: ✅ Успешно сохранено!");
                    Toast.makeText(this, "Файл сохранен в Загрузки: " + outFile.getName(), Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    statusTextView.setText("Статус: ❌ Ошибка сохранения");
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void scanAndShowExcelFilesDialog() {
        statusTextView.setText("Статус: 🔍 Поиск таблиц на устройстве...");
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
                    .setTitle("📁 Найденные таблицы")
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