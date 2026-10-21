package com.shifripnr.app;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_PERMISSIONS = 100;

    // Модель данных шифра ПНР
    public static class PnrItem {
        public String volCode;
        public String volName;
        public String dept;
        public String sec;
        public String grp;
        public String code;
        public String altCode;
        public String unit;
        public List<String> works;

        public PnrItem(String volCode, String volName, String dept, String sec, String grp, String code, String altCode, String unit, List<String> works) {
            this.volCode = volCode;
            this.volName = volName;
            this.dept = dept;
            this.sec = sec;
            this.grp = grp;
            this.code = code;
            this.altCode = altCode;
            this.unit = unit;
            this.works = works;
        }
    }

    private List<PnrItem> allDatabase = new ArrayList<>();
    private List<PnrItem> filteredDatabase = new ArrayList<>();

    private LinearLayout containerCards;
    private TextView countInfoText;
    private EditText searchInput;
    private Spinner volSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissionsIfNeeded();
        initDatabase();
        filteredDatabase.addAll(allDatabase);

        // Главный контейнер (Темная тема)
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0F172A")); // Slate 900
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // ==================== ВЕРХНЯЯ ПАНЕЛЬ (HEADER) ====================
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setBackgroundColor(Color.parseColor("#1E293B")); // Slate 800
        header.setPadding(24, 24, 24, 24);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleLayout = new LinearLayout(this);
        titleLayout.setOrientation(LinearLayout.VERTICAL);
        titleLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView title = new TextView(this);
        title.setText("⚡ Довідник шифрів ПНР");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        titleLayout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Нормативи КНУПУ / ДСТУ Б Д.2.6 (Збірники 1–10)");
        subtitle.setTextColor(Color.parseColor("#94A3B8")); // Slate 400
        subtitle.setTextSize(12f);
        titleLayout.addView(subtitle);

        header.addView(titleLayout);

        // Кнопки экспорта
        Button btnJson = createActionButton("JSON", Color.parseColor("#3B82F6"), v -> exportJSON());
        Button btnCsv = createActionButton("CSV", Color.parseColor("#059669"), v -> exportCSV());
        header.addView(btnJson);
        header.addView(btnCsv);

        root.addView(header);

        // ==================== ПАНЕЛЬ ФИЛЬТРАЦИИ И ПОИСКА ====================
        LinearLayout filterPanel = new LinearLayout(this);
        filterPanel.setOrientation(LinearLayout.VERTICAL);
        filterPanel.setBackgroundColor(Color.parseColor("#1E293B"));
        filterPanel.setPadding(24, 0, 24, 20);

        // Поиск
        searchInput = new EditText(this);
        searchInput.setHint("🔍 Пошук за шифром (П1-1-1) або назвою робіт...");
        searchInput.setHintTextColor(Color.parseColor("#64748B"));
        searchInput.setTextColor(Color.WHITE);
        searchInput.setBackgroundColor(Color.parseColor("#0F172A"));
        searchInput.setPadding(30, 24, 30, 24);
        searchInput.setTextSize(14f);
        
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        searchParams.setMargins(0, 0, 0, 16);
        searchInput.setLayoutParams(searchParams);
        
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        filterPanel.addView(searchInput);

        // Выпадающий список збірників
        volSpinner = new Spinner(this);
        String[] volumes = {
            "📂 Всі збірники (П1 — П10)",
            "Збірник 1 (П1) — Електротехнічні пристрої",
            "Збірник 2 (П2) — АСУ ТП та КІП",
            "Збірник 3 (П3) — Вентиляція та кондиціювання",
            "Збірник 4 (П4) — Підйомно-транспортне устаткування",
            "Збірник 6 (П6) — Теплосилове устаткування",
            "Збірник 7 (П7) — Компресорні установки",
            "Збірник 8 (П8) — Парові та газові турбіни",
            "Збірник 9 (П9) — Водопостачання та каналізація",
            "Збірник 10 (П10) — Металургійне устаткування"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, volumes);
        volSpinner.setAdapter(adapter);
        volSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { applyFilters(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        filterPanel.addView(volSpinner);

        root.addView(filterPanel);

        // Сводка (Статистика)
        countInfoText = new TextView(this);
        countInfoText.setTextColor(Color.parseColor("#94A3B8"));
        countInfoText.setTextSize(12f);
        countInfoText.setPadding(24, 16, 24, 8);
        root.addView(countInfoText);

        // ==================== СПИСОК КАРТОЧЕК ====================
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f));
        
        containerCards = new LinearLayout(this);
        containerCards.setOrientation(LinearLayout.VERTICAL);
        containerCards.setPadding(24, 12, 24, 24);
        scrollView.addView(containerCards);

        root.addView(scrollView);

        // ==================== ФУТЕР С АВТОРСТВОМ ====================
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER);
        footer.setBackgroundColor(Color.parseColor("#0B0F19"));
        footer.setPadding(16, 16, 16, 16);

        TextView authorText = new TextView(this);
        authorText.setText("App engineering by Dziuba R.V • KNUПУ / ДСТУ Б Д.2.6");
        authorText.setTextColor(Color.parseColor("#64748B")); // Меткий неброский цвет
        authorText.setTextSize(10f);
        footer.addView(authorText);

        root.addView(footer);

        setContentView(root);
        renderCards();
    }

    // Фильтрация базы данных
    private void applyFilters() {
        String query = searchInput.getText().toString().trim().toLowerCase();
        int spinnerPos = volSpinner.getSelectedItemPosition();
        String selectedVolCode = "ALL";
        if (spinnerPos == 1) selectedVolCode = "П1";
        else if (spinnerPos == 2) selectedVolCode = "П2";
        else if (spinnerPos == 3) selectedVolCode = "П3";
        else if (spinnerPos == 4) selectedVolCode = "П4";
        else if (spinnerPos == 5) selectedVolCode = "П6";
        else if (spinnerPos == 6) selectedVolCode = "П7";
        else if (spinnerPos == 7) selectedVolCode = "П8";
        else if (spinnerPos == 8) selectedVolCode = "П9";
        else if (spinnerPos == 9) selectedVolCode = "П10";

        filteredDatabase.clear();
        for (PnrItem item : allDatabase) {
            boolean matchVol = selectedVolCode.equals("ALL") || item.volCode.equals(selectedVolCode);
            boolean matchQuery = query.isEmpty() ||
                    item.code.toLowerCase().contains(query) ||
                    (item.altCode != null && item.altCode.toLowerCase().contains(query)) ||
                    item.volName.toLowerCase().contains(query) ||
                    item.dept.toLowerCase().contains(query) ||
                    item.sec.toLowerCase().contains(query) ||
                    item.grp.toLowerCase().contains(query) ||
                    worksContain(item.works, query);

            if (matchVol && matchQuery) {
                filteredDatabase.add(item);
            }
        }
        renderCards();
    }

    private boolean worksContain(List<String> works, String q) {
        for (String w : works) {
            if (w.toLowerCase().contains(q)) return true;
        }
        return false;
    }

    // Отрисовка карточек
    private void renderCards() {
        containerCards.removeAllViews();
        countInfoText.setText("Відображено позицій: " + filteredDatabase.size() + " з " + allDatabase.size());

        if (filteredDatabase.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Нічого не знайдено за вашим запитом.");
            empty.setTextColor(Color.parseColor("#94A3B8"));
            empty.setPadding(24, 48, 24, 24);
            empty.setGravity(Gravity.CENTER);
            containerCards.addView(empty);
            return;
        }

        for (PnrItem item : filteredDatabase) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(Color.parseColor("#1E293B"));
            card.setPadding(20, 20, 20, 20);
            
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 16);
            card.setLayoutParams(lp);

            // Верхняя строка карточки (Шифр + Од. вим)
            LinearLayout topRow = new LinearLayout(this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView codeView = new TextView(this);
            codeView.setText(item.code);
            codeView.setTextColor(Color.parseColor("#60A5FA")); // Blue 400
            codeView.setTextSize(16f);
            codeView.setTypeface(Typeface.DEFAULT_BOLD);
            codeView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
            topRow.addView(codeView);

            if (item.altCode != null && !item.altCode.isEmpty()) {
                TextView altView = new TextView(this);
                altView.setText(" " + item.altCode + " ");
                altView.setTextColor(Color.parseColor("#F59E0B")); // Amber
                altView.setTextSize(11f);
                topRow.addView(altView);
            }

            TextView unitView = new TextView(this);
            unitView.setText(" Од: " + item.unit);
            unitView.setTextColor(Color.parseColor("#94A3B8"));
            unitView.setTextSize(12f);
            topRow.addView(unitView);

            card.addView(topRow);

            // Иерархия
            TextView pathView = new TextView(this);
            pathView.setText(item.volName + " / " + item.dept);
            pathView.setTextColor(Color.parseColor("#64748B"));
            pathView.setTextSize(11f);
            LinearLayout.LayoutParams pathLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            pathLp.setMargins(0, 4, 0, 12);
            pathView.setLayoutParams(pathLp);
            card.addView(pathView);

            // Список робіт
            LinearLayout worksBox = new LinearLayout(this);
            worksBox.setOrientation(LinearLayout.VERTICAL);
            worksBox.setBackgroundColor(Color.parseColor("#0F172A"));
            worksBox.setPadding(16, 14, 16, 14);

            for (String work : item.works) {
                TextView wView = new TextView(this);
                wView.setText("• " + work);
                wView.setTextColor(Color.parseColor("#CBD5E1")); // Slate 300
                wView.setTextSize(13f);
                LinearLayout.LayoutParams wLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                wLp.setMargins(0, 0, 0, 6);
                wView.setLayoutParams(wLp);
                worksBox.addView(wView);
            }
            card.addView(worksBox);

            containerCards.addView(card);
        }
    }

    // Экспорт в JSON
    private void exportJSON() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for (int i = 0; i < filteredDatabase.size(); i++) {
                PnrItem it = filteredDatabase.get(i);
                sb.append("  {\n");
                sb.append("    \"code\": \"").append(it.code).append("\",\n");
                sb.append("    \"vol\": \"").append(it.volName).append("\",\n");
                sb.append("    \"dept\": \"").append(it.dept).append("\",\n");
                sb.append("    \"unit\": \"").append(it.unit).append("\",\n");
                sb.append("    \"worksCount\": ").append(it.works.size()).append("\n");
                sb.append("  }").append(i < filteredDatabase.size() - 1 ? "," : "").append("\n");
            }
            sb.append("]");
            saveFileToDownloads("pnr_export.json", sb.toString());
            Toast.makeText(this, "✅ Експортовано у Downloads/pnr_export.json", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "❌ Помилка експорту", Toast.LENGTH_SHORT).show();
        }
    }

    // Экспорт в CSV
    private void exportCSV() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("\uFEFFШифр;Збірник;Відділ;Одиниця;Склад робіт\n");
            for (PnrItem it : filteredDatabase) {
                sb.append(it.code).append(";")
                  .append("\"").append(it.volName).append("\";")
                  .append("\"").append(it.dept).append("\";")
                  .append(it.unit).append(";")
                  .append("\"").append(String.join(" | ", it.works)).append("\"\n");
            }
            saveFileToDownloads("pnr_export.csv", sb.toString());
            Toast.makeText(this, "✅ Експортовано у Downloads/pnr_export.csv", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "❌ Помилка експорту", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveFileToDownloads(String fileName, String content) throws Exception {
        OutputStream fos;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            fos = getContentResolver().openOutputStream(uri);
        } else {
            java.io.File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!dir.exists()) dir.mkdirs();
            java.io.File file = new java.io.File(dir, fileName);
            fos = new java.io.FileOutputStream(file);
        }
        fos.write(content.getBytes("UTF-8"));
        fos.flush();
        fos.close();
    }

    private void requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSIONS);
            }
        }
    }

    private Button createActionButton(String text, int bgColor, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(12f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(bgColor);
        btn.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(110, 80);
        lp.setMargins(8, 0, 0, 0);
        btn.setLayoutParams(lp);
        return btn;
    }

    // Инициализация полной базы данных шифров ПНР (Збірники 1-10)[span_1](start_span)[span_1](end_span)
    private void initDatabase() {
        // Збірник 1
        List<String> w1 = new ArrayList<>();
        w1.add("Вимірювання опору ізоляції обмоток мегаомметром на 2500 В та визначення коефіцієнта абсорбції.");
        w1.add("Вимірювання опору обмоток постійному струму на всіх відгалуженнях.");
        w1.add("Визначення коефіцієнта трансформації та перевірка групи з'єднання обмоток.");
        allDatabase.add(new PnrItem("П1", "Збірник 1. Електротехнічні пристрої", "Силове електроустаткування", "Трансформатори", "Група 1", "П1-1-1", "КП1-1-1", "шт", w1));

        List<String> w2 = new ArrayList<>();
        w2.add("Випробування підвищеною напругою промислової частоти.");
        w2.add("Випробування трансформаторного масла на електричну міцність.");
        allDatabase.add(new PnrItem("П1", "Збірник 1. Електротехнічні пристрої", "Силове електроустаткування", "Трансформатори", "Група 1", "П1-1-2", "КП1-1-2", "шт", w2));

        List<String> w3 = new ArrayList<>();
        w3.add("Перевірка механічної частини вакуумного або елегазового вимикача.");
        w3.add("Вимірювання часу увімкнення/вимкнення та одночасності замикання контактів.");
        allDatabase.add(new PnrItem("П1", "Збірник 1. Електротехнічні пристрої", "Силове електроустаткування", "Вимикачі", "Група 5", "П1-5-1", "КП1-5-1", "шт", w3));

        List<String> w4 = new ArrayList<>();
        w4.add("Налаштування ножових контактів, перевірка щільності прилягання.");
        w4.add("Вимірювання перехідного опору контактних з'єднань.");
        allDatabase.add(new PnrItem("П1", "Збірник 1. Електротехнічні пристрої", "Силове електроустаткування", "Роз'єднувачі", "Група 10", "П1-10-1", "КП1-10-1", "шт", w4));

        List<String> w5 = new ArrayList<>();
        w5.add("Комплексна перевірка та регулювання механічних блокувань КРУ.");
        w5.add("Випробування підвищеною напругою опорної ізоляції.");
        allDatabase.add(new PnrItem("П1", "Збірник 1. Електротехнічні пристрої", "Силове електроустаткування", "КРУ", "Група 20", "П1-20-1", "КП1-20-1", "комірка", w5));

        List<String> w6 = new ArrayList<>();
        w6.add("Вимикач автоматичний однополюсний до 1 кВ. Вимірювання опору ізоляції.");
        w6.add("Перевірка дії розчіплювачів струмом від стороннього джерела.");
        allDatabase.add(new PnrItem("П1", "Збірник 1. Електротехнічні пристрої", "Низьковольтні апарати", "Автомати", "Група 15", "П1-15-1", "КП1-15-1", "шт", w6));

        List<String> w7 = new ArrayList<>();
        w7.add("Вимикач триполюсний автоматичний до 600 А з мікропроцесорним розчіплювачем.");
        w7.add("Налаштування електронних уставок струму й часу розчеплення (L, S, I, G).");
        allDatabase.add(new PnrItem("П1", "Збірник 1. Електротехнічні пристрої", "Низьковольтні апарати", "Автомати", "Група 15", "П1-15-7", "КП1-15-7", "шт", w7));

        List<String> w8 = new ArrayList<>();
        w8.add("Пристрій автоматичного ввімкнення резерву (АВР) на 2 вводи.");
        w8.add("Перевірка часових затримок та тестування блокувань.");
        allDatabase.add(new PnrItem("П1", "Збірник 1. Електротехнічні пристрої", "Розподільчі щити", "АВР", "Група 30", "П1-30-1", "КП1-30-1", "пристрій", w8));

        // Збірник 2 (АСУ ТП та КІП)
        List<String> w9 = new ArrayList<>();
        w9.add("Датчики тиску та розрідження (тензометричні, п'єзорезистивні).");
        w9.add("Калібрування нуля, перевірка лінійності вихідного сигналу (4-20 мА).");
        allDatabase.add(new PnrItem("П2", "Збірник 2. Автоматизовані системи керування", "Первинні перетворювачі", "Датчики тиску", "Група 1", "П2-1-1", "КП2-1-1", "шт", w9));

        List<String> w10 = new ArrayList<>();
        w10.add("Термометри опору (Pt100) та термопари.");
        w10.add("Вимірювання опору ізоляції, перевірка градуювальних таблиць.");
        allDatabase.add(new PnrItem("П2", "Збірник 2. Автоматизовані системи керування", "Первинні перетворювачі", "Температура", "Група 2", "П2-2-1", "КП2-2-1", "шт", w10));

        List<String> w11 = new ArrayList<>();
        w11.add("Контролер програмований процесорний (центральний блок CPU).");
        w11.add("Налаштування мережевих інтерфейсів (Modbus TCP, Profinet).");
        allDatabase.add(new PnrItem("П2", "Збірник 2. Автоматизовані системи керування", "Контролери", "ПЛК", "Група 10", "П2-10-1", "КП2-10-1", "контролер", w11));

        List<String> w12 = new ArrayList<>();
        w12.add("Пристрої комплексної автоматики, сигналізації та протиаварійного захисту (ПАЗ).");
        w12.add("Налаштування устави спрацьовування та затримок часу.");
        allDatabase.add(new PnrItem("П2", "Збірник 2. Автоматизовані системи керування", "Захист та блокування", "ПАЗ", "Група 13", "П2-13-6", "КП2-13-6", "шт", w12));

        // Збірник 3 (Вентиляція)
        List<String> w13 = new ArrayList<>();
        w13.add("Аеродинамічні випробування та налагодження приточної/витяжної системи.");
        w13.add("Вимірювання витрат та швидкості повітря у повітроводах.");
        allDatabase.add(new PnrItem("П3", "Збірник 3. Системи вентиляції та кондиціювання", "Вентиляційні системи", "Приточні установки", "Група 1", "П3-1-1", "КП3-1-1", "система", w13));

        // Збірник 4 (Підйомно-транспортне)
        List<String> w14 = new ArrayList<>();
        w14.add("Комплексна наладка електрообладнання мостового або козлового крана.");
        w14.add("Налаштування приводів пересування та обмежувачів вантажопідйомності.");
        allDatabase.add(new PnrItem("П4", "Збірник 4. Підйомно-транспортне устаткування", "Підйомні машини", "Крани", "Група 10", "П4-10-1", "КП4-10-1", "кран", w14));

        // Збірник 6 (Теплосилове)
        List<String> w15 = new ArrayList<>();
        w15.add("Пусконалагоджувальні роботи водогрійного або парового котла.");
        w15.add("Регулювання режимів горіння палива, балансування газ-повітря.");
        allDatabase.add(new PnrItem("П6", "Збірник 6. Теплосилове устаткування", "Котлоагрегати", "Котли", "Група 1", "П6-1-1", "КП6-1-1", "котлоагрегат", w15));

        // Збірник 7 (Компресори)
        List<String> w16 = new ArrayList<>();
        w16.add("Пусконалагоджувальні роботи компресорної установки.");
        w16.add("Налаштування автоматики мащення та охолодження.");
        allDatabase.add(new PnrItem("П7", "Збірник 7. Компресорні установки", "Компресори", "Гвинтові компресори", "Група 1", "П7-1-1", "КП7-1-1", "установка", w16));

        // Збірник 8 (Турбіни)
        List<String> w17 = new ArrayList<>();
        w17.add("Налагодження системи автоматичного регулювання та гідрозахисту парової турбіни.");
        allDatabase.add(new PnrItem("П8", "Збірник 8. Парові та газові турбіни", "Турбоагрегати", "Парові турбіни", "Група 1", "П8-1-1", "КП8-1-1", "турбіна", w17));

        // Збірник 9 (Водопостачання)
        List<String> w18 = new ArrayList<>();
        w18.add("Комплексне налагодження насосного агрегату з частотно-регульованим приводом (ЧРП).");
        allDatabase.add(new PnrItem("П9", "Збірник 9. Споруди водопостачання та каналізації", "Насосні станції", "Насоси", "Група 1", "П9-1-1", "КП9-1-1", "насос", w18));

        // Збірник 10 (Металургія)
        List<String> w19 = new ArrayList<>();
        w19.add("Налагодження електроприводів прокатного стану та системи синхронізації швидкостей.");
        allDatabase.add(new PnrItem("П10", "Збірник 10. Металургійне устаткування", "Доменне обладнання", "Прокатні стани", "Група 1", "П10-1-1", "КП10-1-1", "лінія", w19));
    }
}
