package com.binancebot.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String PREF_NAME = "BinanceBotPrefs";

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

    private String botToken = "8815256130:AAEW6brOnE5NpvsgzvZbfm87b3X7bFzaw9Y";
    private String chatId = "1434233986";

    private boolean btcIsBullish = true;
    private boolean btcIsBearish = false;
    private final Map<String, Double> fundingRateCache = new HashMap<>();
    private final Map<String, Long> lastAlertTime = new HashMap<>();

    private TextView timeDisplay;
    private TextView scannerStatus;
    private ProgressBar customProgressBar;
    private TextView scanProgressText;
    private boolean isScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadSettings();

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        mainLayout.setBackgroundColor(Color.parseColor("#0b0f19"));
        mainLayout.setPadding(30, 30, 30, 30);

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

        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.VERTICAL);
        buttonLayout.setGravity(Gravity.CENTER);
        buttonLayout.setPadding(0, 15, 0, 0);

        Button btnRunScan = createStyledButton("🚀 Запустить полный сканер рынка");
        btnRunScan.setOnClickListener(v -> {
            if (!isScanning) {
                new BinanceScannerTask().execute();
            } else {
                Toast.makeText(MainActivity.this, "Сканирование уже выполняется!", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnSettings = createStyledButton("⚙️ Настройки и тонкие параметры");
        btnSettings.setOnClickListener(v -> showSettingsDialog());

        buttonLayout.addView(btnRunScan);
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 15));
        buttonLayout.addView(spacer);
        buttonLayout.addView(btnSettings);

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
        btnBg.setColor(Color.parseColor("#334155"));
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

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚙️ Тонкие настройки торгового бота");

        String[] options = {
            "📱 Telegram Token: " + (botToken.length() > 8 ? botToken.substring(0, 6) + "..." : botToken),
            "💬 Telegram Chat ID: " + chatId,
            "📋 Расширенная информация в сигнале: " + (sendDetailedInfo ? "ВКЛ" : "ВЫКЛ"),
            "💰 Мин. объем пары (USDT): " + (long)minVolumeUsdt,
            "📊 Фильтр объема свечи: " + (filterLowVolume ? "ВКЛ (х" + volumeSpikeMultiplier + ")" : "ВЫКЛ"),
            "⚖️ Фильтр Риск/Прибыль (R/R): " + (filterRiskReward ? "ВКЛ (max " + maxSlTpRatio + ")" : "ВЫКЛ"),
            "🎯 Мин. потенциал профита (%): " + (filterMinProfit ? minProfitPct + "%" : "ВЫКЛ"),
            "🚀 Макс. потенциал профита (%): " + (filterMaxProfit ? maxProfitPct + "%" : "ВЫКЛ"),
            "📈 Фильтр тренда BTC: " + (filterBtcTrend ? "ВКЛ" : "ВЫКЛ"),
            "🌊 Ликвидность ATR Sweep: " + (filterLiquiditySweep ? "ВКЛ (" + sweepMinAtrRatio + "-" + sweepMaxAtrRatio + ")" : "ВЫКЛ"),
            "📊 Фильтр Taker Ratio (Дельта): " + (filterTakerRatio ? "ВКЛ (>=" + (minTakerBuyRatio*100) + "%)" : "ВЫКЛ"),
            "⚡ Защита ATR Spike: " + (filterAtrSpike ? "ВКЛ (max x" + maxAtrMultiplier + ")" : "ВЫКЛ"),
            "💰 Фильтр Funding Rate: " + (filterFundingRate ? "ВКЛ (max " + (maxAbsoluteFundingRate*100) + "%)" : "ВЫКЛ"),
            "📉 KAMA Efficiency Ratio: " + (filterKamaEfficiency ? "ВКЛ (min " + minEfficiencyRatio + ")" : "ВЫКЛ"),
            "🛡️ Защита KAMA Trend Guard: " + (filterKamaTrendGuard ? "ВКЛ" : "ВЫКЛ")
        };

        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    showEditDialog("Telegram Bot Token", botToken, val -> { botToken = val; saveSettings(); });
                    break;
                case 1:
                    showEditDialog("Telegram Chat ID", chatId, val -> { chatId = val; saveSettings(); });
                    break;
                case 2:
                    sendDetailedInfo = !sendDetailedInfo;
                    saveSettings();
                    break;
                case 3:
                    showEditDialog("Мин. суточный объем (USDT)", String.valueOf((long)minVolumeUsdt), val -> {
                        try { minVolumeUsdt = Double.parseDouble(val); saveSettings(); } catch(Exception ignored){}
                    });
                    break;
                case 4:
                    showNumericEditDialog("Множитель объема свечи", String.valueOf(volumeSpikeMultiplier), val -> {
                        volumeSpikeMultiplier = val; filterLowVolume = true; saveSettings();
                    }, "Отключить фильтр объема", () -> { filterLowVolume = false; saveSettings(); });
                    break;
                case 5:
                    showNumericEditDialog("Макс. соотношение SL/TP", String.valueOf(maxSlTpRatio), val -> {
                        maxSlTpRatio = val; filterRiskReward = true; saveSettings();
                    }, "Отключить фильтр R/R", () -> { filterRiskReward = false; saveSettings(); });
                    break;
                case 6:
                    showNumericEditDialog("Мин. потенциал профита (%)", String.valueOf(minProfitPct), val -> {
                        minProfitPct = val; filterMinProfit = true; saveSettings();
                    }, "Отключить мин. профит", () -> { filterMinProfit = false; saveSettings(); });
                    break;
                case 7:
                    showNumericEditDialog("Макс. потенциал профита (%)", String.valueOf(maxProfitPct), val -> {
                        maxProfitPct = val; filterMaxProfit = true; saveSettings();
                    }, "Отключить макс. профит", () -> { filterMaxProfit = false; saveSettings(); });
                    break;
                case 8:
                    filterBtcTrend = !filterBtcTrend;
                    saveSettings();
                    break;
                case 9:
                    filterLiquiditySweep = !filterLiquiditySweep;
                    saveSettings();
                    break;
                case 10:
                    showNumericEditDialog("Мин. Taker Buy Ratio (например 0.65)", String.valueOf(minTakerBuyRatio), val -> {
                        minTakerBuyRatio = val; filterTakerRatio = true; saveSettings();
                    }, "Отключить Taker Ratio", () -> { filterTakerRatio = false; saveSettings(); });
                    break;
                case 11:
                    showNumericEditDialog("Макс. ATR Spike множитель", String.valueOf(maxAtrMultiplier), val -> {
                        maxAtrMultiplier = val; filterAtrSpike = true; saveSettings();
                    }, "Отключить ATR Spike", () -> { filterAtrSpike = false; saveSettings(); });
                    break;
                case 12:
                    showNumericEditDialog("Макс. Funding Rate (например 0.0005)", String.valueOf(maxAbsoluteFundingRate), val -> {
                        maxAbsoluteFundingRate = val; filterFundingRate = true; saveSettings();
                    }, "Отключить Funding Rate", () -> { filterFundingRate = false; saveSettings(); });
                    break;
                case 13:
                    showNumericEditDialog("Мин. KAMA Efficiency Ratio", String.valueOf(minEfficiencyRatio), val -> {
                        minEfficiencyRatio = val; filterKamaEfficiency = true; saveSettings();
                    }, "Отключить KAMA Efficiency", () -> { filterKamaEfficiency = false; saveSettings(); });
                    break;
                case 14:
                    filterKamaTrendGuard = !filterKamaTrendGuard;
                    saveSettings();
                    break;
            }
            Toast.makeText(this, "Параметры обновлены!", Toast.LENGTH_SHORT).show();
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

    private void showNumericEditDialog(String title, String defaultValue, OnDoubleSavedListener listener, String disableText, Runnable disableRunnable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        final EditText input = new EditText(this);
        input.setText(defaultValue);
        builder.setView(input);
        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            try {
                listener.onSaved(Double.parseDouble(input.getText().toString().trim()));
            } catch(Exception ignored){}
        });
        builder.setNeutralButton(disableText, (dialog, which) -> disableRunnable.run());
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    interface OnValueSavedListener { void onSaved(String value); }
    interface OnDoubleSavedListener { void onSaved(double value); }

    private void saveSettings() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("botToken", botToken);
        editor.putString("chatId", chatId);
        editor.putBoolean("sendDetailedInfo", sendDetailedInfo);
        editor.putFloat("minVolumeUsdt", (float) minVolumeUsdt);
        editor.putBoolean("filterLowVolume", filterLowVolume);
        editor.putFloat("volumeSpikeMultiplier", (float) volumeSpikeMultiplier);
        editor.putBoolean("filterRiskReward", filterRiskReward);
        editor.putFloat("maxSlTpRatio", (float) maxSlTpRatio);
        editor.putBoolean("filterMinProfit", filterMinProfit);
        editor.putFloat("minProfitPct", (float) minProfitPct);
        editor.putBoolean("filterMaxProfit", filterMaxProfit);
        editor.putFloat("maxProfitPct", (float) maxProfitPct);
        editor.putBoolean("filterBtcTrend", filterBtcTrend);
        editor.putBoolean("filterLiquiditySweep", filterLiquiditySweep);
        editor.putFloat("sweepMinAtrRatio", (float) sweepMinAtrRatio);
        editor.putFloat("sweepMaxAtrRatio", (float) sweepMaxAtrRatio);
        editor.putBoolean("filterTakerRatio", filterTakerRatio);
        editor.putFloat("minTakerBuyRatio", (float) minTakerBuyRatio);
        editor.putBoolean("filterAtrSpike", filterAtrSpike);
        editor.putFloat("maxAtrMultiplier", (float) maxAtrMultiplier);
        editor.putBoolean("filterFundingRate", filterFundingRate);
        editor.putFloat("maxAbsoluteFundingRate", (float) maxAbsoluteFundingRate);
        editor.putBoolean("filterKamaEfficiency", filterKamaEfficiency);
        editor.putFloat("minEfficiencyRatio", (float) minEfficiencyRatio);
        editor.putBoolean("filterKamaTrendGuard", filterKamaTrendGuard);
        editor.apply();
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        botToken = prefs.getString("botToken", botToken);
        chatId = prefs.getString("chatId", chatId);
        sendDetailedInfo = prefs.getBoolean("sendDetailedInfo", sendDetailedInfo);
        minVolumeUsdt = prefs.getFloat("minVolumeUsdt", (float) minVolumeUsdt);
        filterLowVolume = prefs.getBoolean("filterLowVolume", filterLowVolume);
        volumeSpikeMultiplier = prefs.getFloat("volumeSpikeMultiplier", (float) volumeSpikeMultiplier);
        filterRiskReward = prefs.getBoolean("filterRiskReward", filterRiskReward);
        maxSlTpRatio = prefs.getFloat("maxSlTpRatio", (float) maxSlTpRatio);
        filterMinProfit = prefs.getBoolean("filterMinProfit", filterMinProfit);
        minProfitPct = prefs.getFloat("minProfitPct", (float) minProfitPct);
        filterMaxProfit = prefs.getBoolean("filterMaxProfit", filterMaxProfit);
        maxProfitPct = prefs.getFloat("maxProfitPct", (float) maxProfitPct);
        filterBtcTrend = prefs.getBoolean("filterBtcTrend", filterBtcTrend);
        filterLiquiditySweep = prefs.getBoolean("filterLiquiditySweep", filterLiquiditySweep);
        sweepMinAtrRatio = prefs.getFloat("sweepMinAtrRatio", (float) sweepMinAtrRatio);
        sweepMaxAtrRatio = prefs.getFloat("sweepMaxAtrRatio", (float) sweepMaxAtrRatio);
        filterTakerRatio = prefs.getBoolean("filterTakerRatio", filterTakerRatio);
        minTakerBuyRatio = prefs.getFloat("minTakerBuyRatio", (float) minTakerBuyRatio);
        filterAtrSpike = prefs.getBoolean("filterAtrSpike", filterAtrSpike);
        maxAtrMultiplier = prefs.getFloat("maxAtrMultiplier", (float) maxAtrMultiplier);
        filterFundingRate = prefs.getBoolean("filterFundingRate", filterFundingRate);
        maxAbsoluteFundingRate = prefs.getFloat("maxAbsoluteFundingRate", (float) maxAbsoluteFundingRate);
        filterKamaEfficiency = prefs.getBoolean("filterKamaEfficiency", filterKamaEfficiency);
        minEfficiencyRatio = prefs.getFloat("minEfficiencyRatio", (float) minEfficiencyRatio);
        filterKamaTrendGuard = prefs.getBoolean("filterKamaTrendGuard", filterKamaTrendGuard);
    }

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
            } catch (Exception ignored) {}
        }).start();
    }

    private class BinanceScannerTask extends AsyncTask<Void, Integer, String> {

        @Override
        protected void onPreExecute() {
            isScanning = true;
            scannerStatus.setText("🚀 Запуск сканирования Binance Futures...");
            customProgressBar.setProgress(5);
            scanProgressText.setText("Прогресс: 5% (Получение списка пар)");
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                if (filterFundingRate) {
                    try {
                        URL url = new URL("https://fapi.binance.com/fapi/v1/premiumIndex");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = r.readLine()) != null) sb.append(line);
                        JSONArray arr = new JSONArray(sb.toString());
                        synchronized (fundingRateCache) {
                            fundingRateCache.clear();
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject obj = arr.getJSONObject(i);
                                fundingRateCache.put(obj.getString("symbol"), obj.optDouble("lastFundingRate", 0.0));
                            }
                        }
                    } catch (Exception ignored) {}
                }

                publishProgress(15);

                if (filterBtcTrend) {
                    try {
                        List<Candle> btcKlines = fetchKlines("BTCUSDT", "30m", 50);
                        if (btcKlines != null && btcKlines.size() > 10) {
                            double emaBtc = calculateEma(btcKlines, 64);
                            double currBtc = btcKlines.get(btcKlines.size() - 2).close;
                            btcIsBullish = currBtc > emaBtc;
                            btcIsBearish = currBtc < emaBtc;
                        }
                    } catch (Exception ignored) {}
                }

                publishProgress(25);

                URL url = new URL("https://fapi.binance.com/fapi/v1/ticker/24hr");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                JSONArray jsonArray = new JSONArray(response.toString());
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

                publishProgress(40);
                int totalPairs = validPairs.size();

                ExecutorService executor = Executors.newFixedThreadPool(4);
                for (String symbol : validPairs) {
                    executor.submit(() -> {
                        try {
                            String signalMsg = analyzePair(symbol);
                            if (signalMsg != null) {
                                sendTelegramAlert(symbol, signalMsg);
                            }
                        } catch (Exception ignored) {}
                    });
                }

                for (int p = 40; p <= 95; p += 5) {
                    Thread.sleep(120);
                    publishProgress(p);
                }

                executor.shutdown();
                while (!executor.isTerminated()) {
                    Thread.sleep(100);
                }

                publishProgress(100);
                return "Сканирование завершено. Проверено пар: " + totalPairs;
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

    private static class Candle {
        long timestamp;
        double open, high, low, close, volume, tbBaseAv;
        double atr;
    }

    private List<Candle> fetchKlines(String symbol, String timeframe, int limit) {
        try {
            String urlStr = "https://fapi.binance.com/fapi/v1/klines?symbol=" + symbol + "&interval=" + timeframe + "&limit=" + limit;
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();

            JSONArray arr = new JSONArray(sb.toString());
            List<Candle> list = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONArray c = arr.getJSONArray(i);
                Candle candle = new Candle();
                candle.timestamp = c.getLong(0);
                candle.open = c.getDouble(1);
                candle.high = c.getDouble(2);
                candle.low = c.getDouble(3);
                candle.close = c.getDouble(4);
                candle.volume = c.getDouble(5);
                candle.tbBaseAv = c.getDouble(9);
                list.add(candle);
            }
            calculateAtr(list);
            return list;
        } catch (Exception e) {
            return null;
        }
    }

    private void calculateAtr(List<Candle> list) {
        for (int i = 0; i < list.size(); i++) {
            Candle c = list.get(i);
            double hl = c.high - c.low;
            double hc = i > 0 ? Math.abs(c.high - list.get(i - 1).close) : 0;
            double lc = i > 0 ? Math.abs(c.low - list.get(i - 1).close) : 0;
            double tr = Math.max(hl, Math.max(hc, lc));
            
            if (i >= 13) {
                double sum = 0;
                for (int j = i - 13; j <= i; j++) sum += list.get(j).high - list.get(j).low;
                c.atr = sum / 14.0;
            } else {
                c.atr = hl;
            }
        }
    }

    private double calculateEma(List<Candle> list, int period) {
        double k = 2.0 / (period + 1);
        double ema = list.get(0).close;
        for (int i = 1; i < list.size(); i++) {
            ema = (list.get(i).close * k) + (ema * (1 - k));
        }
        return ema;
    }

    private String getPriceFormat(double price) {
        if (price >= 100) return "%.2f";
        if (price >= 1) return "%.4f";
        if (price >= 0.001) return "%.6f";
        return "%.8f";
    }

    private String analyzePair(String symbol) {
        List<Candle> df15 = fetchKlines(symbol, "15m", 200);
        List<Candle> df30 = fetchKlines(symbol, "30m", 200);
        if (df15 == null || df15.size() < 70 || df30 == null || df30.size() < 70) return null;

        Candle c15 = df15.get(df15.size() - 2);
        Candle c30 = df30.get(df30.size() - 2);
        double currPrice = df15.get(df15.size() - 1).close;

        double sumVol = 0;
        for (int i = df15.size() - 22; i < df15.size() - 2; i++) sumVol += df15.get(i).volume;
        double avgVol15 = sumVol / 20.0;
        boolean isHighVol = c15.volume > (avgVol15 * volumeSpikeMultiplier);

        if (filterLowVolume && !isHighVol) return null;

        if (filterAtrSpike) {
            double sumAtr = 0;
            for (int i = df15.size() - 22; i < df15.size() - 2; i++) sumAtr += df15.get(i).atr;
            double atrMean = sumAtr / 20.0;
            if (atrMean > 0 && (c15.atr / atrMean) > maxAtrMultiplier) return null;
        }

        double takerRatio = c15.volume > 0 ? (c15.tbBaseAv / c15.volume) : 0.5;

        double hh = -1, ll = Double.MAX_VALUE;
        for (int i = df15.size() - 64; i < df15.size(); i++) {
            if (df15.get(i).high > hh) hh = df15.get(i).high;
            if (df15.get(i).low < ll) ll = df15.get(i).low;
        }
        double range = hh - ll;
        double step = range / 8.0;
        if (step <= 0) step = 1.0;

        double m3 = ll + 3 * step;
        double m5 = ll + 5 * step;
        double m0 = ll;
        double m8 = hh;

        String signalDir = null;
        double tpMin = 0, tpMax = 0, slLevel = 0;

        if (currPrice >= m3 && currPrice <= m5) {
            if (c15.low < m5 && c15.close > m5) {
                signalDir = "bullish";
                tpMin = m8 - step; tpMax = m8; slLevel = ll - 2 * step;
            } else if (c15.high > m3 && c15.close < m3) {
                signalDir = "bearish";
                tpMin = m0; tpMax = m0 + step; slLevel = hh + 2 * step;
            }
        } else if (currPrice >= m8) {
            if (c15.high > m8 && c15.close < m8) {
                signalDir = "bearish";
                tpMin = m5; tpMax = m5 + step; slLevel = m8 + 2 * step;
            }
        } else if (currPrice <= m0) {
            if (c15.low < m0 && c15.close > m0) {
                signalDir = "bullish";
                tpMin = m3; tpMax = m3 + step; slLevel = m0 - 2 * step;
            }
        }

        if (signalDir == null) return null;

        if (filterBtcTrend && !symbol.equals("BTCUSDT")) {
            if (signalDir.equals("bullish") && !btcIsBullish) return null;
            if (signalDir.equals("bearish") && !btcIsBearish) return null;
        }

        if (filterTakerRatio) {
            if (signalDir.equals("bullish") && takerRatio < minTakerBuyRatio) return null;
            if (signalDir.equals("bearish") && takerRatio > (1.0 - minTakerBuyRatio)) return null;
        }

        double tpMinPct = Math.abs(tpMin - currPrice) / currPrice * 100.0;
        double tpMaxPct = Math.abs(tpMax - currPrice) / currPrice * 100.0;
        double slPct = Math.abs(slLevel - currPrice) / currPrice * 100.0;

        if (filterMinProfit && tpMinPct < minProfitPct) return null;
        if (filterMaxProfit && tpMinPct > maxProfitPct) return null;
        if (filterRiskReward && slPct > (tpMinPct * maxSlTpRatio)) return null;

        if (filterFundingRate) {
            double fr = 0.0;
            synchronized (fundingRateCache) { fr = fundingRateCache.getOrDefault(symbol, 0.0); }
            if (signalDir.equals("bullish") && fr > maxAbsoluteFundingRate) return null;
            if (signalDir.equals("bearish") && fr < -maxAbsoluteFundingRate) return null;
        }

        long candleTime = c15.timestamp;
        synchronized (lastAlertTime) {
            if (lastAlertTime.containsKey(symbol) && lastAlertTime.get(symbol) == candleTime) return null;
            lastAlertTime.put(symbol, candleTime);
        }

        String fmt = getPriceFormat(currPrice);
        String dirStr = signalDir.equals("bullish") ? "📈 ЛОНГ" : "📉 ШОРТ";
        String dateTimeStr = new SimpleDateFormat("yyyy-MM-dd / HH:mm:ss", Locale.getDefault()).format(new Date());

        String details = "";
        if (sendDetailedInfo) {
            details = "\n\n📋 ДЕТАЛИ СИГНАЛА:\n" +
                      "• Объем свечи: " + String.format(Locale.US, "%.0f", c15.volume) + " USDT\n" +
                      "• Taker Buy Ratio: " + String.format(Locale.US, "%.1f", takerRatio * 100) + "%\n" +
                      "• Риск/Профит (SL/TP): " + String.format(Locale.US, "%.2f", slPct) + "% / " + String.format(Locale.US, "%.2f", tpMinPct) + "%";
        }

        return String.format(Locale.US,
                "🔔%s🔔\n%s****%s\n💰 ВХОД: " + fmt + "\n🎯 Зона Тейк-Профит:\n" + fmt + " - " + fmt + "\n🛡️ Стоп-Лосс: " + fmt + "\nПрибыль: +%.2f%% — +%.2f%%\nУбыток: -%.2f%%%s",
                dateTimeStr, symbol, dirStr, currPrice, tpMin, tpMax, slLevel, tpMinPct, tpMaxPct, slPct, details);
    }
}
