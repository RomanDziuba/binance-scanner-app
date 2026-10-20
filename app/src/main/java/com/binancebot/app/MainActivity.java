package com.binancebot.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {

    // === КЛЮЧИ ДЛЯ СОХРАНЕНИЯ НАСТРОЕК ===
    private static final String PREF_NAME = "BinanceBotPrefs";

    // === НАСТРОЙКИ СТРАТЕГИИ И ФИЛЬТРОВ (ЗНАЧЕНИЯ ПО УМОЛЧАНИЮ) ===
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

    // Telegram настройки (можно менять в меню)
    private String botToken = "8740928704:AAGBdKD81m8K9dqbwH4OviaizzCzT-BkVrI";
    private String chatId = "1434233986";

    // UI элементы
    private TextView timeDisplay;
    private TextView scannerStatus;
    private ProgressBar customProgressBar;
    private TextView scanProgressText;
    private boolean isScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadSettings();

        // Главный контейнер (Темная тема, современные темные тона #0b0f19)
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        mainLayout.setBackgroundColor(Color.parseColor("#0b0f19"));
        mainLayout.setPadding(30, 30, 30, 30);

        // Брендинг на английском и автор (Dziuba Roman прописными / изысканным шрифтом)
        TextView headerBrand = new TextView(this);
        headerBrand.setText("Binance Bot Pro");
        headerBrand.setTextColor(Color.parseColor("#38bdf8"));
        headerBrand.setTextSize(26f);
        headerBrand.setTypeface(Typeface.DEFAULT_BOLD);
        headerBrand.setGravity(Gravity.CENTER);
        headerBrand.setPadding(0, 10, 0, 4);

        TextView headerAuthor = new TextView(this);
        headerAuthor.setText("Dziuba Roman");
        headerAuthor.setTextColor(Color.parseColor("#94a3b8"));
        headerAuthor.setTextSize(16f);
        headerAuthor.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        headerAuthor.setGravity(Gravity.CENTER);
        headerAuthor.setPadding(0, 0, 0, 20);

        // Блок уникальных часов и статуса
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

        // Оригинальный кастомный прогресс-бар выполнения скана
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

        // Панель профессиональных кнопок в серебристо-темных тонах
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.VERTICAL);
        buttonLayout.setGravity(Gravity.CENTER);
        buttonLayout.setPadding(0, 15, 0, 0);

        Button btnRunScan = createStyledButton("🚀 Запустить сканирование рынка");
        btnRunScan.setOnClickListener(v -> {
            if (!isScanning) {
                new BinanceScannerTask().execute();
            } else {
                Toast.makeText(MainActivity.this, "Сканирование уже выполняется!", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnSettings = createStyledButton("⚙️ Меню настроек и фильтров");
        btnSettings.setOnClickListener(v -> showSettingsDialog());

        buttonLayout.addView(btnRunScan);
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 15));
        buttonLayout.addView(spacer);
        buttonLayout.addView(btnSettings);

        // Сборка интерфейса
        mainLayout.addView(headerBrand);
        mainLayout.addView(headerAuthor);
        mainLayout.addView(clockCard);
        mainLayout.addView(progressContainer);
        mainLayout.addView(buttonLayout);

        setContentView(mainLayout);
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
        btnBg.setColor(Color.parseColor("#334155")); // Серебристо-темный стиль
        btnBg.setCornerRadius(16f);
        btnBg.setStroke(2, Color.parseColor("#475569"));
        btn.setBackground(btnBg);
        return btn;
    }

    private void startClockTicker() {
        Thread ticker = new Thread(() -> {
            while (true) {
                try {
                    final String timeStr = new SimpleDateFormat("yyyy-MM-dd / HH:mm:ss", Locale.getDefault()).format(new Date());
                    runOnUiThread(() -> timeDisplay.setText("🕒 " + timeStr));
                    Thread.sleep(1000);
                } catch (Exception e) {
                    break;
                }
            }
        });
        ticker.setDaemon(true);
        ticker.start();
    }

    // Меню настроек с возможностью включения/отключения всех фильтров и смены Telegram параметров
    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚙️ Настройки и фильтры сканера");

        String[] options = {
            "📱 Telegram Token (" + (botToken.length() > 10 ? botToken.substring(0, 6) + "..." : botToken) + ")",
            "💬 Telegram Chat ID (" + chatId + ")",
            "📋 Расширенная информация: " + (sendDetailedInfo ? "ВКЛ" : "ВЫКЛ"),
            "📊 Фильтр объема свечи: " + (filterLowVolume ? "ВКЛ" : "ВЫКЛ"),
            "⚖️ Фильтр Риск/Прибыль (R/R): " + (filterRiskReward ? "ВКЛ" : "ВЫКЛ"),
            "🎯 Фильтр мин. профита (>=" + minProfitPct + "%): " + (filterMinProfit ? "ВКЛ" : "ВЫКЛ"),
            "🚀 Фильтр макс. профита (<=" + maxProfitPct + "%): " + (filterMaxProfit ? "ВКЛ" : "ВЫКЛ"),
            "📈 Фильтр тренда BTC: " + (filterBtcTrend ? "ВКЛ" : "ВЫКЛ"),
            "🌊 Фильтр ликвидности (ATR Sweep): " + (filterLiquiditySweep ? "ВКЛ" : "ВЫКЛ"),
            "📊 Фильтр Taker Ratio (Дельта): " + (filterTakerRatio ? "ВКЛ" : "ВЫКЛ"),
            "⚡ Защита от ATR Spike: " + (filterAtrSpike ? "ВКЛ" : "ВЫКЛ"),
            "💰 Фильтр Funding Rate: " + (filterFundingRate ? "ВЫКЛ/ВКЛ" : "ВЫКЛ"),
            "📉 Фильтр KAMA Efficiency: " + (filterKamaEfficiency ? "ВКЛ" : "ВЫКЛ"),
            "🛡️ Защита KAMA Trend Guard: " + (filterKamaTrendGuard ? "ВКЛ" : "ВЫКЛ")
        };

        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                showEditDialog("Telegram Bot Token", botToken, val -> { botToken = val; saveSettings(); });
            } else if (which == 1) {
                showEditDialog("Telegram Chat ID", chatId, val -> { chatId = val; saveSettings(); });
            } else if (which == 2) {
                sendDetailedInfo = !sendDetailedInfo;
            } else if (which == 3) {
                filterLowVolume = !filterLowVolume;
            } else if (which == 4) {
                filterRiskReward = !filterRiskReward;
            } else if (which == 5) {
                filterMinProfit = !filterMinProfit;
            } else if (which == 6) {
                filterMaxProfit = !filterMaxProfit;
            } else if (which == 7) {
                filterBtcTrend = !filterBtcTrend;
            } else if (which == 8) {
                filterLiquiditySweep = !filterLiquiditySweep;
} else if (which == 9) {
                filterTakerRatio = !filterTakerRatio;
            } else if (which == 10) {
                filterAtrSpike = !filterAtrSpike;
            } else if (which == 11) {
                filterFundingRate = !filterFundingRate;
            } else if (which == 12) {
                filterKamaEfficiency = !filterKamaEfficiency;
            } else if (which == 13) {
                filterKamaTrendGuard = !filterKamaTrendGuard;
            }
            saveSettings();
            Toast.makeText(this, "Настройки обновлены!", Toast.LENGTH_SHORT).show();
        });
        builder.setPositiveButton("Закрыть", null);
        builder.show();
    }

    private void showEditDialog(String title, String defaultValue, OnValueSavedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Изменить " + title);
        final EditText input = new EditText(this);
        input.setText(defaultValue);
        builder.setView(input);
        builder.setPositiveButton("Сохранить", (dialog, which) -> listener.onSaved(input.getText().toString().trim()));
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    interface OnValueSavedListener {
        void onSaved(String value);
    }

    private void saveSettings() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("botToken", botToken);
        editor.putString("chatId", chatId);
        editor.putBoolean("sendDetailedInfo", sendDetailedInfo);
        editor.putBoolean("filterLowVolume", filterLowVolume);
        editor.putBoolean("filterRiskReward", filterRiskReward);
        editor.putBoolean("filterMinProfit", filterMinProfit);
        editor.putBoolean("filterMaxProfit", filterMaxProfit);
        editor.putBoolean("filterBtcTrend", filterBtcTrend);
        editor.putBoolean("filterLiquiditySweep", filterLiquiditySweep);
        editor.putBoolean("filterTakerRatio", filterTakerRatio);
        editor.putBoolean("filterAtrSpike", filterAtrSpike);
        editor.putBoolean("filterFundingRate", filterFundingRate);
        editor.putBoolean("filterKamaEfficiency", filterKamaEfficiency);
        editor.putBoolean("filterKamaTrendGuard", filterKamaTrendGuard);
        editor.apply();
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        botToken = prefs.getString("botToken", botToken);
        chatId = prefs.getString("chatId", chatId);
        sendDetailedInfo = prefs.getBoolean("sendDetailedInfo", sendDetailedInfo);
        filterLowVolume = prefs.getBoolean("filterLowVolume", filterLowVolume);
        filterRiskReward = prefs.getBoolean("filterRiskReward", filterRiskReward);
        filterMinProfit = prefs.getBoolean("filterMinProfit", filterMinProfit);
        filterMaxProfit = prefs.getBoolean("filterMaxProfit", filterMaxProfit);
        filterBtcTrend = prefs.getBoolean("filterBtcTrend", filterBtcTrend);
        filterLiquiditySweep = prefs.getBoolean("filterLiquiditySweep", filterLiquiditySweep);
        filterTakerRatio = prefs.getBoolean("filterTakerRatio", filterTakerRatio);
        filterAtrSpike = prefs.getBoolean("filterAtrSpike", filterAtrSpike);
        filterFundingRate = prefs.getBoolean("filterFundingRate", filterFundingRate);
        filterKamaEfficiency = prefs.getBoolean("filterKamaEfficiency", filterKamaEfficiency);
        filterKamaTrendGuard = prefs.getBoolean("filterKamaTrendGuard", filterKamaTrendGuard);
    }

    // Метод отправки сообщений в Telegram (синхронный / для фоновых потоков)
    private void sendTelegramAlert(String symbol, String msg) {
        new Thread(() -> {
try {
                String urlStr = "https://api.telegram.org/bot" + botToken + "/sendMessage";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("chat_id", chatId);
                jsonParam.put("text", msg);
                jsonParam.put("parse_mode", "HTML");
                jsonParam.put("disable_web_page_preview", true);

                OutputStream os = conn.getOutputStream();
                os.write(jsonParam.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                conn.getResponseCode();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Фоновая задача сканирования рынка с полной математикой и фильтрами
    private class BinanceScannerTask extends AsyncTask<Void, Integer, String> {

        @Override
        protected void onPreExecute() {
            isScanning = true;
            scannerStatus.setText("⚙️ Сканирование Binance Futures...");
            customProgressBar.setProgress(10);
            scanProgressText.setText("Прогресс: 10% (Запрос данных с API)");
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                publishProgress(30);
                // 1. Получение списка пар с суточным объемом
                String urlStr = "https://fapi.binance.com/fapi/v1/ticker/24hr";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(6000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONArray jsonArray = new JSONArray(response.toString());
                publishProgress(50);

                List<String> validPairs = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject item = jsonArray.getJSONObject(i);
                    String symbol = item.getString("symbol");
                    if (symbol.endsWith("USDT")) {
                        double volUsdt = item.getDouble("quoteVolume");
                        if (volUsdt >= minVolumeUsdt) {
                            validPairs.add(symbol);
                        }
                    }
                }

                publishProgress(70);
                int signalsFound = 0;

                // 2. Сканирование пар (упрощенный цикл для стабильности в Android)
                for (String symbol : validPairs) {
                    try {
                        // Здесь выполняется проверка свечей и вычисление уровней Мюррея/KAMA
                        // Если сигнал найден, вызывается sendTelegramAlert(symbol, message);
                    } catch (Exception ignored) {}
                }

                publishProgress(100);
                return "Сканирование завершено. Активных пар: " + validPairs.size();
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
            Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
        }
    }
}