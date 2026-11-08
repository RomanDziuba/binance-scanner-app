package com.pdfdocx.version1;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 🚀 PRO PDF TO DOCX CONVERTER ENGINE v1.0
 * Интегрировано с Apache POI (Word) & PDFBox (PDF Parser)
 * Автор: Dziuba Roman
 */
public class MainActivity extends Activity {

    private String selectedPdfPath = "";
    private String selectedPdfName = "";
    private Uri selectedPdfUri = null;

    private TextView statusTextView, infoTextView, fileLabelView;
    private Button btnSelectPdf, btnConvert, btnCopyText;
    private EditText logEditText;

    private Handler handler = new Handler(Looper.getMainLooper());
    private String extractedTextCache = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Инициализация ресурсов PDFBox для Android
        try {
            PDFBoxResourceLoader.init(getApplicationContext());
        } catch (Exception ignored) {}

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));
        rootLayout.setPadding(16, 16, 16, 16);

        ScrollView mainScroll = new ScrollView(this);
        mainScroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView authorView = new TextView(this);
        authorView.setText("✦ PDF to DOCX Pro Converter Engine ✦");
        authorView.setTextColor(Color.parseColor("#F59E0B"));
        authorView.setTextSize(14f);
        authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 5, 0, 8);
        container.addView(authorView);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 20, 20, 20);
        
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#0F172A"));
        cardBg.setCornerRadius(24f);
        cardBg.setStroke(2, Color.parseColor("#1E293B"));
        card.setBackground(cardBg);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 16);
        card.setLayoutParams(cardParams);

        TextView titleView = new TextView(this);
        titleView.setText("📄 Конвертер PDF ➡️ Word (DOCX)");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(16f);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 10);
        card.addView(titleView);

        infoTextView = new TextView(this);
        infoTextView.setText("📦 Выберите PDF файл на устройстве. Приложение извлечет текст, сохранит форматирование и создаст файл .docx в папке «Загрузки».");
        infoTextView.setTextColor(Color.parseColor("#94A3B8"));
        infoTextView.setTextSize(11f);
        infoTextView.setPadding(12, 10, 12, 10);
        
        GradientDrawable infoBg = new GradientDrawable();
        infoBg.setColor(Color.parseColor("#1E293B"));
        infoBg.setCornerRadius(12f);
        infoTextView.setBackground(infoBg);
        
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        infoParams.setMargins(0, 0, 0, 12);
        infoTextView.setLayoutParams(infoParams);
        card.addView(infoTextView);

        fileLabelView = new TextView(this);
        fileLabelView.setText("📂 Файл не выбран");
        fileLabelView.setTextColor(Color.parseColor("#FBBF24"));
        fileLabelView.setTextSize(12f);
        fileLabelView.setTypeface(Typeface.DEFAULT_BOLD);
        fileLabelView.setPadding(0, 0, 0, 10);
        card.addView(fileLabelView);

        btnSelectPdf = createStyledButton("📁 Выбрать PDF файл", "#1D4ED8");
        btnSelectPdf.setOnClickListener(v -> openPdfPicker());
        card.addView(btnSelectPdf);

        btnConvert = createStyledButton("⚙️ Конвертировать в DOCX", "#0F766E");
        btnConvert.setEnabled(false);
        btnConvert.setBackgroundColor(Color.parseColor("#334155"));
        btnConvert.setOnClickListener(v -> startPdfToDocxConversion());
        card.addView(btnConvert);

        btnCopyText = createStyledButton("📋 Скопировать извлеченный текст", "#334155");
        btnCopyText.setOnClickListener(v -> copyExtractedText());
        card.addView(btnCopyText);

        statusTextView = new TextView(this);
        statusTextView.setText("Статус: Готов к работе 🚀");
        statusTextView.setTextColor(Color.parseColor("#10B981"));
        statusTextView.setTextSize(12f);
        statusTextView.setTypeface(Typeface.DEFAULT_BOLD);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(0, 12, 0, 8);
        card.addView(statusTextView);

        TextView logLabel = new TextView(this);
        logLabel.setText("📋 Терминал и предпросмотр текста:");
        logLabel.setTextColor(Color.parseColor("#64748B"));
        logLabel.setTextSize(11f);
        logLabel.setPadding(0, 6, 0, 4);
        card.addView(logLabel);

        logEditText = new EditText(this);
        logEditText.setHint("Здесь появится извлеченный текст и отчет о конвертации...");
        logEditText.setTextSize(10f);
        logEditText.setTextColor(Color.parseColor("#E2E8F0"));
        logEditText.setHintTextColor(Color.parseColor("#475569"));
        logEditText.setBackgroundColor(Color.parseColor("#030712"));
        logEditText.setPadding(10, 10, 10, 10);
        logEditText.setGravity(Gravity.TOP);
        logEditText.setMinLines(8);
        logEditText.setMaxLines(15);
        card.addView(logEditText);

        container.addView(card);
        mainScroll.addView(container);
        rootLayout.addView(mainScroll);
        setContentView(rootLayout);
    }

    private Button createStyledButton(String text, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.parseColor("#F8FAFC"));
        btn.setTextSize(11f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        btn.setPadding(12, 12, 12, 12);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 6);
        btn.setLayoutParams(params);
        return btn;
    }

    private void openPdfPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Выберите PDF файл"), 1001);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedPdfUri = data.getData();
            selectedPdfName = getFileNameFromUri(selectedPdfUri);
            fileLabelView.setText("📄 Выбран: " + selectedPdfName);
            btnConvert.setEnabled(true);
            btnConvert.setBackgroundColor(Color.parseColor("#0F766E"));
            statusTextView.setText("Статус: Файл загружен, нажмите конвертацию 🚀");
            logEditText.setText("Выбран файл: " + selectedPdfName + "\nГотов к чтению и парсингу структуры.");
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String result = "document.pdf";
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx != -1) result = cursor.getString(idx);
                }
            } catch (Exception ignored) {}
        }
        return result;
    }

    private void startPdfToDocxConversion() {
        if (selectedPdfUri == null) {
            Toast.makeText(this, "Сначала выберите PDF файл!", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("🔄 Конвертация PDF в DOCX");
        progressDialog.setMessage("Чтение PDF, парсинг страниц и генерация Word документа...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        statusTextView.setText("Статус: ⚙️ Обработка PDF документа...");

        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedPdfUri);
                PDDocument document = PDDocument.load(inputStream);
                
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                String parsedText = stripper.getText(document);
                document.close();
                if (inputStream != null) inputStream.close();

                extractedTextCache = parsedText != null ? parsedText : "";

                // Создание DOCX файла с помощью Apache POI
                XWPFDocument docx = new XWPFDocument();
                
                // Заголовок документа
                XWPFParagraph titleParagraph = docx.createParagraph();
                titleParagraph.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun titleRun = titleParagraph.createRun();
                titleRun.setText("Конвертировано из: " + selectedPdfName);
                titleRun.setBold(true);
                titleRun.setFontSize(14);
                titleRun.setColor("1D4ED8");

                // Разделение по абзацам для сохранения структуры
                String[] paragraphs = extractedTextCache.split("\\r?\\n");
                for (String pText : paragraphs) {
                    XWPFParagraph para = docx.createParagraph();
                    XWPFRun run = para.createRun();
                    run.setText(pText);
                    run.setFontSize(11);
                    run.setFontFamily("Calibri");
                }

                // Сохранение файла в папку Загрузки (Download)
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }

                String outName = selectedPdfName.replaceAll("(?i)\\.pdf$", "") + "_converted.docx";
                File outFile = new File(downloadsDir, outName);

                FileOutputStream fos = new FileOutputStream(outFile);
                docx.write(fos);
                fos.close();
                docx.close();

                handler.post(() -> {
                    progressDialog.dismiss();
                    statusTextView.setText("Статус: ✅ Успешно конвертировано в DOCX!");
                    Toast.makeText(MainActivity.this, "Файл сохранен в папку Загрузки: " + outName, Toast.LENGTH_LONG).show();
                    logEditText.setText("=== ✅ КОНВЕРТАЦИЯ ЗАВЕРШЕНА УСПЕШНО ===\n" +
                                        "• Исходный PDF: " + selectedPdfName + "\n" +
                                        "• Созданный DOCX: " + outName + "\n" +
                                        "• Путь: Папка «Загрузки» (Download)\n\n" +
                                        "--- ПРЕДПРОСИМОТР ИЗВЛЕЧЕННОГО ТЕКСТА ---\n" +
                                        (extractedTextCache.length() > 2000 ? extractedTextCache.substring(0, 2000) + "\n[... текст обрезан для предпросмотра ...]" : extractedTextCache));
                });

            } catch (Exception e) {
                handler.post(() -> {
                    progressDialog.dismiss();
                    statusTextView.setText("Статус: ❌ Ошибка конвертации");
                    logEditText.setText("❌ Ошибка: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void copyExtractedText() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null && !extractedTextCache.isEmpty()) {
                ClipData clip = ClipData.newPlainText("PdfText", extractedTextCache);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Текст скопирован в буфер обмена!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Нет извлеченного текста для копирования", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка копирования", Toast.LENGTH_SHORT).show();
        }
    }
}