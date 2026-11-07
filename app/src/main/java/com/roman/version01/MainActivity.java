package com.roman.version01;

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
import android.view.ViewGroup;
import android.widget.*;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

        // Настраиваем поле вывода с поддержкой выделения текста (setTextIsSelectable)
        logTextView = new TextView(this);
        logTextView.setText("Выберите таблицу для автоматического чтения в приложении...\n(Вы можете прокручивать текст по горизонтали и вертикали, а также выделять нужные фрагменты)");
        logTextView.setTextSize(12f);
        logTextView.setTextColor(Color.parseColor("#E2E8F0"));
        logTextView.setTypeface(Typeface.MONOSPACE);
        logTextView.setBackgroundColor(Color.parseColor("#0F172A"));
        logTextView.setPadding(20, 20, 20, 20);
        logTextView.setTextIsSelectable(true); // РАЗРЕШЕНИЕ ВЫДЕЛЕНИЯ И КОПИРОВАНИЯ ТЕКСТА
        logTextView.setHorizontallyScrolling(true); // Разрешаем скролл по горизонтали

        // Двумерный скроллинг: HorizontalScrollView (вправо/влево) + ScrollView (вверх/вниз)
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
            processAnySpreadsheet(data.getData());
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

    private void processAnySpreadsheet(Uri uri) {
        String fileName = getFileName(uri);
        statusTextView.setText("Статус: 🔄 Чтение файла в приложении...");

        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            try {
                File tempFile = new File(getCacheDir(), "universal_temp_file");
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

                if (fileName.endsWith(".xlsx") || isZipFile(tempFile)) {
                    parseXlsxFile(tempFile, sb);
                } else if (fileName.endsWith(".csv") || fileName.endsWith(".txt")) {
                    parseCsvFile(tempFile, sb);
                } else {
                    parseLegacyXlsFile(tempFile, sb);
                }

                tempFile.delete();

            } catch (Exception e) {
                sb.append("❌ Ошибка при чтении: ").append(e.getMessage());
            }

            final String output = sb.toString();
            new Handler(Looper.getMainLooper()).post(() -> {
                logTextView.setText(output);
                statusTextView.setText("Статус: ✅ Файл успешно открыт в приложении!");
            });
        }).start();
    }

    private boolean isZipFile(File file) {
        try (InputStream in = new FileInputStream(file)) {
            byte[] sig = new byte[4];
            if (in.read(sig) == 4) {
                return sig[0] == 0x50 && sig[1] == 0x4B && sig[2] == 0x03 && sig[3] == 0x04;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void parseXlsxFile(File file, StringBuilder sb) throws Exception {
        ZipFile zip = new ZipFile(file);
        List<String> sharedStrings = new ArrayList<>();
        ZipEntry stringsEntry = zip.getEntry("xl/sharedStrings.xml");
        if (stringsEntry != null) {
            try (InputStream sis = zip.getInputStream(stringsEntry)) {
                DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = db.parse(sis);
                NodeList tNodes = doc.getElementsByTagName("t");
                for (int i = 0; i < tNodes.getLength(); i++) {
                    sharedStrings.add(tNodes.item(i).getTextContent());
                }
            }
        }

        ZipEntry sheetEntry = zip.getEntry("xl/worksheets/sheet1.xml");
        if (sheetEntry == null) {
            for (Enumeration<? extends ZipEntry> en = zip.entries(); en.hasMoreElements();) {
                ZipEntry entry = en.nextElement();
                if (entry.getName().startsWith("xl/worksheets/sheet")) {
                    sheetEntry = entry;
                    break;
                }
            }
        }

        if (sheetEntry != null) {
            try (InputStream shIn = zip.getInputStream(sheetEntry)) {
                DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document sheetDoc = db.parse(shIn);
                NodeList rowList = sheetDoc.getElementsByTagName("row");
                sb.append("📊 Таблица XLSX | Всего строк: ").append(rowList.getLength()).append("\n\n");

                for (int r = 0; r < Math.min(rowList.getLength(), 400); r++) {
                    org.w3c.dom.Element row = (org.w3c.dom.Element) rowList.item(r);
                    NodeList cells = row.getElementsByTagName("c");
                    sb.append(String.format("[%3d] ", (r + 1)));
                    
                    for (int c = 0; c < cells.getLength(); c++) {
                        org.w3c.dom.Element cell = (org.w3c.dom.Element) cells.item(c);
                        String type = cell.getAttribute("t");
                        NodeList vList = cell.getElementsByTagName("v");
                        
                        if (vList.getLength() > 0) {
                            String val = vList.item(0).getTextContent();
                            if ("s".equals(type)) {
                                int sIdx = Integer.parseInt(val);
                                if (sIdx >= 0 && sIdx < sharedStrings.size()) {
                                    val = sharedStrings.get(sIdx);
                                }
                            }
                            sb.append(val).append("\t | \t");
                        }
                    }
                    sb.append("\n");
                }
            }
        } else {
            sb.append("⚠️ Листы таблицы не обнаружены.");
        }
        zip.close();
    }

    private void parseCsvFile(File file, StringBuilder sb) throws Exception {
        sb.append("📊 Таблица CSV / TXT\n\n");
        try (BufferedReader br = new BufferedReader(new java.io.InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            int count = 0;
            while ((line = br.readLine()) != null && count < 400) {
                sb.append(line.replace(",", "\t | \t")).append("\n");
                count++;
            }
        }
    }

    private void parseLegacyXlsFile(File file, StringBuilder sb) {
        sb.append("📊 Таблица XLS (Legacy Binary Format)\n\n");
        try (InputStream in = new FileInputStream(file)) {
            byte[] data = new byte[(int) Math.min(file.length(), 1024 * 1024)];
            int read = in.read(data);
            
            StringBuilder currentWord = new StringBuilder();
            int cellCount = 0;

            for (int i = 0; i < read; i++) {
                int b = data[i] & 0xFF;
                // Отсекаем мусорные символы и маски формата ($, #, 0.00, ÿ), оставляя чистый текст и числа
                if ((b >= 32 && b <= 126 && b != '#' && b != '$' && b != '@') || (b >= 192 && b <= 255) || b == 9) {
                    currentWord.append((char) b);
                } else {
                    if (currentWord.length() >= 3) {
                        String word = currentWord.toString().trim();
                        if (!word.isEmpty() && !word.contains("0.00") && !word.contains("yy")) {
                            sb.append(word).append("\t | \t");
                            cellCount++;
                            if (cellCount >= 8) {
                                sb.append("\n");
                                cellCount = 0;
                            }
                        }
                    }
                    currentWord.setLength(0);
                }
            }
        } catch (Exception e) {
            sb.append("Ошибка чтения бинарного файла: ").append(e.getMessage());
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
                    processAnySpreadsheet(Uri.fromFile(selectedFile));
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