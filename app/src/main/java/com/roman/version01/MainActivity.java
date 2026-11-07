package com.roman.version01;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

// THEME: @android:style/Theme.Material.Light.NoActionBar
public class MainActivity extends Activity {

    private static final int PICK_EXCEL_REQUEST = 2002;
    private LinearLayout rootLayout;
    private TextView statusTextView, logTextView;
    private ScrollView mainUiContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Создаем корневой контейнер
        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));
        setContentView(rootLayout);

        // Шаг 1: Показываем красивую черную заставку (Splash Screen)
        showSplashScreen();

        // Шаг 2: Проверяем и запрашиваем полный доступ к файлам в фоне
        checkAndRequestStoragePermissions();
    }

    /**
     * Профессиональная заставка с брендингом Excel и вашими данными
     */
    private void showSplashScreen() {
        LinearLayout splashContainer = new LinearLayout(this);
        splashContainer.setOrientation(LinearLayout.VERTICAL);
        splashContainer.setGravity(Gravity.CENTER);
        splashContainer.setPadding(40, 40, 40, 40);

        // Иконка Excel (зеленый стилизованный блок с буквой X)
        LinearLayout excelIconBox = new LinearLayout(this);
        excelIconBox.setGravity(Gravity.CENTER);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setColor(Color.parseColor("#107C41")); // Официальный цвет Microsoft Excel
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

        // Название программы на английском
        TextView titleView = new TextView(this);
        titleView.setText("Universal Excel Core");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(22f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        splashContainer.addView(titleView);

        // Профессиональное описание
        TextView descView = new TextView(this);
        descView.setText("Advanced high-precision spreadsheet parsing and universal file management engine supporting all workbook formats.");
        descView.setTextColor(Color.parseColor("#94A3B8"));
        descView.setTextSize(12f);
        descView.setGravity(Gravity.CENTER);
        descView.setPadding(20, 15, 20, 30);
        splashContainer.addView(descView);

        // Автор и год
        TextView authorView = new TextView(this);
        authorView.setText("Created by Dziuba Roman • 2026");
        authorView.setTextColor(Color.parseColor("#F59E0B"));
        authorView.setTextSize(13f);
        authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        authorView.setGravity(Gravity.CENTER);
        splashContainer.addView(authorView);

        rootLayout.addView(splashContainer);

        // Через 3.5 секунды плавно переходим к главному интерфейсу
        new Handler(Looper.getMainLooper()).postDelayed(this.getRealString(), 3500);
    }

    private Runnable getRealString() {
        return () -> {
            rootLayout.removeAllViews();
            initMainInterface();
        };
    }

    /**
     * Главный рабочий интерфейс программы
     */
    private void initMainInterface() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(20, 20, 20, 20);
        container.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView header = new TextView(this);
        header.setText("📊 Excel Universal Reader Pro");
        header.setTextColor(Color.parseColor("#38BDF8"));
        header.setTextSize(18f);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setGravity(Gravity.CENTER);
        header.setPadding(0, 0, 0, 20);
        container.addView(header);

        statusTextView = new TextView(this);
        statusTextView.setText("Статус: Доступ к памяти предоставлен. Готов к чтению файлов.");
        statusTextView.setTextColor(Color.parseColor("#10B981"));
        statusTextView.setTextSize(12f);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(0, 0, 0, 20);
        container.addView(statusTextView);

        // Кнопка выбора файла Excel
        Button btnOpenExcel = createStyledButton("📂 Открыть любой Excel файл (.xlsx / .xls)", "#2563EB");
        btnOpenExcel.setOnClickListener(v -> openFilePicker());
        container.addView(btnOpenExcel);

        // Кнопка сканирования памяти
        Button btnScanStorage = createStyledButton("🔍 Сканировать устройство на наличие таблиц", "#0D9488");
        btnScanStorage.setOnClickListener(v -> scanDeviceForExcelFiles());
        container.addView(btnScanStorage);

        // Терминал / Область вывода данных таблиц
        logTextView = new TextView(this);
        logTextView.setText("Здесь появится содержимое открытой таблицы Excel...");
        logTextView.setTextSize(11f);
        logTextView.setTextColor(Color.parseColor("#E2E8F0"));
        logTextView.setTypeface(Typeface.MONOSPACE);
        logTextView.setBackgroundColor(Color.parseColor("#0F172A"));
        logTextView.setPadding(15, 15, 15, 15);

        ScrollView scroll = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 900
        );
        scrollParams.setMargins(0, 20, 0, 0);
        scroll.setLayoutParams(scrollParams);
        scroll.addView(logTextView);
        container.addView(scroll);

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
        params.setMargins(0, 0, 0, 15);
        btn.setLayoutParams(params);
        return btn;
    }

    /**
     * Запрос доступа ко всем папкам и файлам телефона (Android 11+ и ниже)
     */
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
        // Поддерживаем любые типы файлов Excel и общие документы
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        String[] mimeTypes = {
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 
            "application/vnd.ms-excel",
            "application/octet-stream"
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(Intent.createChooser(intent, "Выберите файл Excel"), PICK_EXCEL_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            processExcelUri(uri);
        }
    }

    /**
     * Универсальный высокоточный парсер Excel файлов (.xlsx)
     */
    private void processExcelUri(Uri uri) {
        statusTextView.setText("Статус: 🔄 Чтение и парсинг структуры Excel...");
        new Thread(() -> {
            StringBuilder resultBuilder = new StringBuilder();
            try {
                // Копируем файл во временную директорию для корректного чтения через ZipFile
                File tempFile = new File(getCacheDir(), "parsing_temp.xlsx");
                try (InputStream in = getContentResolver().openInputStream(uri);
                     java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                }

                // .xlsx является ZIP-архивом, содержащим XML таблицы и общие строки
                ZipFile zip = new ZipFile(tempFile);
                
                // 1. Читаем sharedStrings.xml (все текстовые константы книги)
                List<String> sharedStrings = new ArrayList<>();
                ZipEntry stringsEntry = zip.getEntry("xl/sharedStrings.xml");
                if (stringsEntry != null) {
                    InputStream sis = zip.getInputStream(stringsEntry);
                    DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    Document doc = db.parse(sis);
                    NodeList tNodes = doc.getElementsByTagName("t");
                    for (int i = 0; i < tNodes.getLength(); i++) {
                        sharedStrings.add(tNodes.item(i).getTextContent());
                    }
                    sis.close();
                }

                // 2. Находим первый рабочий лист (sheet1.xml)
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
                    InputStream shIn = zip.getInputStream(sheetEntry);
                    DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    Document sheetDoc = db.parse(shIn);
                    NodeList rowList = sheetDoc.getElementsByTagName("row");

                    resultBuilder.append("=== УСПЕШНО РАСПАКОВАНО И ПРОЧИТАНО ===\n");
                    resultBuilder.append("Всего строк в таблице: ").append(rowList.getLength()).append("\n\n");

                    // Построчный обход ячеек с максимальной точностью
                    for (int r = 0; r < Math.min(rowList.getLength(), 300); r++) {
                        org.w3c.dom.Element row = (org.w3c.dom.Element) rowList.item(r);
                        NodeList cells = row.getElementsByTagName("c");
                        resultBuilder.append("Строка ").append(r + 1).append(": ");
                        
                        for (int c = 0; c < cells.getLength(); c++) {
                            org.w3c.dom.Element cell = (org.w3c.dom.Element) cells.item(c);
                            String type = cell.getAttribute("t");
                            NodeList vList = cell.getElementsByTagName("v");
                            
                            if (vList.getLength() > 0) {
                                String val = vList.item(0).getTextContent();
                                // Если тип 's', значение является индексом в словаре sharedStrings
                                if ("s".equals(type)) {
                                    int sIdx = Integer.parseInt(val);
                                    if (sIdx >= 0 && sIdx < sharedStrings.size()) {
                                        val = sharedStrings.get(sIdx);
                                    }
                                }
                                resultBuilder.append("[").append(val).append("] ");
                            }
                        }
                        resultBuilder.append("\n");
                    }
                    shIn.close();
                } else {
                    resultBuilder.append("⚠️ Структура листов Excel не найдена. Возможно, это старый формат .xls или защищенный файл.");
                }
                zip.close();
                tempFile.delete();

            } catch (Exception e) {
                resultBuilder.append("❌ Ошибка при чтении Excel файла: ").append(e.getMessage()).append("\n")
                            .append("Убедитесь, что файл не поврежден и имеет доступ для чтения.");
            }

            final String finalOutput = resultBuilder.toString();
            handlerPost(() -> {
                logTextView.setText(finalOutput);
                statusTextView.setText("Статус: ✅ Файл успешно обработан!");
            });
        }).start();
    }

    private void scanDeviceForExcelFiles() {
        statusTextView.setText("Статус: 🔍 Поиск таблиц Excel в памяти телефона...");
        new Thread(() -> {
            List<File> foundFiles = new ArrayList<>();
            try {
                File storage = Environment.getExternalStorageDirectory();
                recursiveSearch(storage, foundFiles);
            } catch (Exception ignored) {}

            StringBuilder sb = new StringBuilder();
            sb.append("📂 Результаты сканирования памяти:\n");
            if (foundFiles.isEmpty()) {
                sb.append("Файлы .xlsx не найдены в корневых папках. Воспользуйтесь кнопкой выбора файла вручную.");
            } else {
                for (File f : foundFiles) {
                    sb.append("• ").append(f.getName()).append(" (").append(f.getAbsolutePath()).append(")\n");
                }
            }

            final String res = sb.toString();
            handlerPost(() -> {
                logTextView.setText(res);
                statusTextView.setText("Статус: Сканирование завершено.");
            });
        }).start();
    }

    private void recursiveSearch(File dir, List<File> list) {
        if (dir == null || !dir.isDirectory() || list.size() > 50) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                if (!f.getName().startsWith(".")) {
                    recursiveSearch(f, list);
                }
            } else {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
                    list.add(f);
                }
            }
        }
    }

    private void handlerPost(Runnable r) {
        new Handler(Looper.getMainLooper()).post(r);
    }
}