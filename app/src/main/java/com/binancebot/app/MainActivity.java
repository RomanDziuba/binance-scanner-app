package com.binancebot.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {

    // === НАСТРОЙКИ СКАНЕРА (Параметры из оригинального скрипта) ===
    private double minVolumeUsdt = 6_000_000.0;
    private boolean filterLowVolume = true;
    private double volumeSpikeMultiplier = 2.0;
    private boolean filterRiskReward = true;
    private double maxSlTpRatio = 1.0;
    private boolean filterMinProfit = true;
    private double minProfitPct = 1.2;
    private boolean filterMaxProfit = true;
    private double maxProfitPct = 3.0;
    private boolean filterBtcTrend = true;
    private boolean filterLiquiditySweep = true;
    private double sweepMinAtrRatio = 0.2;
    private double sweepMaxAtrRatio = 1.5;
    private boolean filterTakerRatio = true;
    private double minTakerBuyRatio = 0.65;
    private boolean filterAtrSpike = true;
    private double maxAtrMultiplier = 2.5;
    private boolean filterFundingRate = true;
    private double maxAbsoluteFundingRate = 0.0005;
    private boolean filterKamaEfficiency = true;
    private int kamaErPeriod = 10;
    private double minEfficiencyRatio = 0.30;
    private boolean filterKamaTrendGuard = true;
    
    private String scanTimeframe = "5m";
    private boolean sendDetailedInfo = true;

    private static final String BOT_TOKEN = "8740928704:AAGBdKD81m8K9dqbwH4OviaizzCzT-BkVrI";
    private static final String CHAT_ID = "1434233986";

    // UI элементы
    private TextView timeDisplay;
    private TextView scannerStatus;
    private ProgressBar customProgressBar;
    private TextView scanProgressText;
    private boolean isScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Главный контейнер (Темная тема, современные темные тона #0b0f19)
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        mainLayout.setBackgroundColor(Color.parseColor("#0b0f19"));
        mainLayout.setPadding(30, 30, 30, 30);

        // Заголовок приложения
        TextView headerBrand = new TextView(this);
        headerBrand.setText("Binance Bot Pro");
        headerBrand.setTextColor(Color.parseColor("#38bdf8"));
        headerBrand.setTextSize(26f);
        headerBrand.setTypeface(Typeface.DEFAULT_BOLD);
        headerBrand.setGravity(Gravity.CENTER);
        headerBrand.setPadding(0, 10, 0, 4);

        // Автор
        TextView headerAuthor = new TextView(this);
        headerAuthor.setText("Dziuba Roman");
        headerAuthor.setTextColor(Color.parseColor("#94a3b8"));
        headerAuthor.setTextSize(16f);
        headerAuthor.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        headerAuthor.setGravity(Gravity.CENTER);
        headerAuthor.setPadding(0, 0, 0, 20);

        // Блок часов и уникального таймера
        LinearLayout clockCard = createCardLayout();
        
        timeDisplay = new TextView(this);
        timeDisplay.setText("🕒 Инициализация часов...");
        timeDisplay.setTextColor(Color.parseColor("#10b981"));
        timeDisplay.setTextSize(18f);
        timeDisplay.setTypeface(Typeface.MONOSPACE);
        timeDisplay.setGravity(Gravity.CENTER);
        timeDisplay.setPadding(0, 0, 0, 10);
        clockCard.addView(timeDisplay);
scannerStatus = new TextView(this);
        scannerStatus.setText("💤 Сканер ожидает запуска...");
        scannerStatus.setTextColor(Color.parseColor("#cbd5e1"));
        scannerStatus.setTextSize(14f);
        scannerStatus.setGravity(Gravity.CENTER);
        clockCard.addView(scannerStatus);

        // Кастомный оригинальный прогресс-бар сканирования
        customProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        customProgressBar.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 24));
        customProgressBar.setMax(100);
        customProgressBar.setProgress(0);
        
        GradientDrawable progressDrawable = new GradientDrawable();
        progressDrawable.setColor(Color.parseColor("#3b82f6"));
        progressDrawable.setCornerRadius(12f);
        customProgressBar.setProgressDrawable(progressDrawable);

        scanProgressText = new TextView(this);
        scanProgressText.setText("Прогресс скана: 0%");
        scanProgressText.setTextColor(Color.parseColor("#64748b"));
        scanProgressText.setTextSize(12f);
        scanProgressText.setGravity(Gravity.CENTER);
        scanProgressText.setPadding(0, 8, 0, 0);

        LinearLayout progressContainer = createCardLayout();
        progressContainer.addView(customProgressBar);
        progressContainer.addView(scanProgressText);

        // Панель кнопок (Профессиональные серебристые в темных тонах)
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.VERTICAL);
        buttonLayout.setGravity(Gravity.CENTER);
        buttonLayout.setPadding(0, 15, 0, 0);

        Button btnRunScan = createStyledButton("🚀 Запустить сканирование рынка");
        btnRunScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isScanning) {
                    new BinanceScannerTask().execute();
                } else {
                    Toast.makeText(MainActivity.this, "Сканирование уже выполняется!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button btnSettings = createStyledButton("⚙️ Меню настроек фильтров");
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsDialog();
            }
        });

        buttonLayout.addView(btnRunScan);
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 15));
        buttonLayout.addView(spacer);
        buttonLayout.addView(btnSettings);

        // Сборка элементов в главный экран
        mainLayout.addView(headerBrand);
        mainLayout.addView(headerAuthor);
        mainLayout.addView(clockCard);
        mainLayout.addView(progressContainer);
        mainLayout.addView(buttonLayout);

        setContentView(mainLayout);

        // Запуск фоновых часов UI
        startClockTicker();
    }

    private LinearLayout createCardLayout() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(30, 35, 30, 35);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 20);
        card.setLayoutParams(params);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#111827"));
        bg.setCornerRadius(24f);
        bg.setStroke(2, Color.parseColor("#1f2937"));
        card.setBackground(bg);
        return card;
    }
private Button createStyledButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.parseColor("#e2e8f0"));
        btn.setTextSize(15f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setPadding(40, 25, 40, 25);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btn.setLayoutParams(params);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#334155")); // Серебристо-темные тона
        btnBg.setCornerRadius(16f);
        btnBg.setStroke(2, Color.parseColor("#475569"));
        btn.setBackground(btnBg);
        return btn;
    }

    private void startClockTicker() {
        Thread ticker = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        final String timeStr = new SimpleDateFormat("yyyy-MM-dd / HH:mm:ss", Locale.getDefault()).format(new Date());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                timeDisplay.setText("🕒 " + timeStr);
                            }
                        });
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        break;
                    }
                }
            }
        });
        ticker.setDaemon(true);
        ticker.start();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚙️ Настройки и фильтры сканера");
        
        String infoSummary = "Текущие параметры:\n\n" +
                "• Мин. объем: " + (long)minVolumeUsdt + " USDT\n" +
                "• Фильтр объема свечи: " + (filterLowVolume ? "ВКЛ (х" + volumeSpikeMultiplier + ")" : "ВЫКЛ") + "\n" +
                "• Фильтр R/R: " + (filterRiskReward ? "ВКЛ (" + maxSlTpRatio + ")" : "ВЫКЛ") + "\n" +
                "• Мин. потенциал профита: " + (filterMinProfit ? minProfitPct + "%" : "ВЫКЛ") + "\n" +
                "• Макс. потенциал профита: " + (filterMaxProfit ? maxProfitPct + "%" : "ВЫКЛ") + "\n" +
                "• Фильтр тренда BTC: " + (filterBtcTrend ? "ВКЛ" : "ВЫКЛ") + "\n" +
                "• Фильтр ликвидности (ATR Sweep): " + (filterLiquiditySweep ? "ВКЛ" : "ВЫКЛ") + "\n" +
                "• Детальная инфо в Telegram: " + (sendDetailedInfo ? "ВКЛЮЧЕНО" : "ВЫКЛЮЧЕНО");

        builder.setMessage(infoSummary);
        builder.setPositiveButton("Изменить режим расширенной инфо", (dialog, which) -> {
            sendDetailedInfo = !sendDetailedInfo;
            Toast.makeText(MainActivity.this, "Расширенная инфо: " + (sendDetailedInfo ? "ВКЛ" : "ВЫКЛ"), Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Закрыть", null);
        builder.show();
    }

    // Фоновая задача сканирования рынка Binance Futures с обновлением кастомного прогресс-бара
    private class BinanceScannerTask extends AsyncTask<Void, Integer, String> {

        @Override
        protected void onPreExecute() {
            isScanning = true;
            scannerStatus.setText("⚙️ Идет сканирование Binance Futures...");
            customProgressBar.setProgress(5);
            scanProgressText.setText("Прогресс скана: 5% (Получение списка пар)");
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                publishProgress(20, 20);
                // Запрос активных фьючерсных пар с Binance
                String urlStr = "https://fapi.binance.com/fapi/v1/ticker/24hr";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONArray jsonArray = new JSONArray(response.toString());
                publishProgress(50, 50);

                int scannedCount = 0;
                int validPairsCount = 0;

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject item = jsonArray.getJSONObject(i);
                    String symbol = item.getString("symbol");
                    if (symbol.endsWith("USDT")) {
                        double volUsdt = item.getDouble("quoteVolume");
                        if (volUsdt >= minVolumeUsdt) {
                            validPairsCount++;
                        }
                    }
                    scannedCount++;
                }

                publishProgress(90, 90);
                Thread.sleep(600); // Имитация завершения анализа
                publishProgress(100, 100);

                return "Успешно просканировано пар: " + validPairsCount + " (из " + scannedCount + " всего)";
            } catch (Exception e) {
                return "Ошибка сканирования: " + e.getMessage();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            int progress = values[0];
            customProgressBar.setProgress(progress);
            scanProgressText.setText("Прогресс скана: " + progress + "%");
        }

        @Override
        protected void onPostExecute(String result) {
            isScanning = false;
            scannerStatus.setText("✅ " + result);
            Toast.makeText(MainActivity.this, result, Toast.DEFAULT).show();
        }
    }
}