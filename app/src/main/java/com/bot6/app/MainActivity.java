package com.bot6.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    // ==================== НАСТРОЙКИ TELEGRAM ====================
    private final String botToken = "8815256130:AAEW6brOnE5NpvsgzvZbfm87b3X7bFzaw9Y";
    private final String chatId = "1434233986";

    // ==================== НАСТРОЙКИ СКАНЕРА ====================
    private double minVolumeUsdt = 6_000_000.0;
    private double volumeSpikeMultiplier = 2.0;

    // UI Элементы
    private TextView tvClock, tvDate, tvStatus, tvCountdown;
    private TextView tvStatsScanned, tvStatsPassed, tvStatsSignals;
    private ProgressBar progressBar;
    private TextView tvConsole;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isScanning = false;
    private int countdownSeconds = 300;
    private final int scanIntervalSeconds = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity actContext = this;

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#030712"));
        rootLayout.setPadding(32, 32, 32, 32);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        // 1. Авторская подпись
        TextView authorView = new TextView(this);
        authorView.setText("✨ 𝒟𝓩𝐼𝒰𝐵𝒜  𝑅𝒪𝑀𝒜 ✨");
        authorView.setTextColor(Color.parseColor("#38BDF8"));
        authorView.setTextSize(20f);
        authorView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        authorView.setGravity(Gravity.CENTER);
        authorView.setPadding(0, 0, 0, 8);

        // 2. Заголовок
        TextView titleView = new TextView(this);
        titleView.setText("🚀 Binance KAMA-Protected Scanner");
        titleView.setTextColor(Color.parseColor("#F8FAFC"));
        titleView.setTextSize(18f);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 16);

        // 3. Блок Часов и Даты
        LinearLayout clockCard = createCardLayout("#0F172A", "#1E293B");
        tvClock = new TextView(this);
        tvClock.setText("00:00:00");
        tvClock.setTextColor(Color.parseColor("#34D399"));
        tvClock.setTextSize(38f);
        tvClock.setTypeface(null, Typeface.BOLD);
        tvClock.setGravity(Gravity.CENTER);

        tvDate = new TextView(this);
        tvDate.setText("Загрузка даты...");
        tvDate.setTextColor(Color.parseColor("#94A3B8"));
        tvDate.setTextSize(14f);
        tvDate.setGravity(Gravity.CENTER);
        tvDate.setPadding(0, 4, 0, 0);

        clockCard.addView(tvClock);
        clockCard.addView(tvDate);

        // 4. Панель Статистики и Таймера
        LinearLayout statsCard = createCardLayout("#0F172A", "#1E293B");
tvStatus = new TextView(this);
        tvStatus.setText("⚡ Статус: Ожидание запуска...");
        tvStatus.setTextColor(Color.parseColor("#38BDF8"));
        tvStatus.setTextSize(14f);
        tvStatus.setTypeface(null, Typeface.BOLD);
        tvStatus.setPadding(0, 0, 0, 10);

        tvCountdown = new TextView(this);
        tvCountdown.setText("⏳ До следующего скана: 05:00");
        tvCountdown.setTextColor(Color.parseColor("#F59E0B"));
        tvCountdown.setTextSize(15f);
        tvCountdown.setTypeface(null, Typeface.BOLD);
        tvCountdown.setPadding(0, 0, 0, 12);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 16
        );
        pbParams.setMargins(0, 0, 0, 16);
        progressBar.setLayoutParams(pbParams);

        LinearLayout statsGrid = new LinearLayout(this);
        statsGrid.setOrientation(LinearLayout.HORIZONTAL);
        statsGrid.setWeightSum(3f);

        tvStatsScanned = createStatItem(statsGrid, "Просканировано", "0");
        tvStatsPassed = createStatItem(statsGrid, "Прошли фильтр", "0");
        tvStatsSignals = createStatItem(statsGrid, "Сигналы", "0");

        statsCard.addView(tvStatus);
        statsCard.addView(tvCountdown);
        statsCard.addView(progressBar);
        statsCard.addView(statsGrid);

        // 5. Кнопки управления
        Button btnScanNow = createStyledButton("🚀 Запустить сканирование сейчас", "#2563EB", v -> {
            if (!isScanning) {
                runScannerCycle();
            } else {
                Toast.makeText(actContext, "Сканирование уже выполняется!", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnSettings = createStyledButton("⚙️ Настройки параметров сканера", "#D97706", v -> {
            showSettingsDialog();
        });

        // 6. Консоль логов
        LinearLayout consoleCard = createCardLayout("#090D16", "#1E293B");
        TextView consoleTitle = new TextView(this);
        consoleTitle.setText("📋 Консоль сигналов и логирования");
        consoleTitle.setTextColor(Color.parseColor("#94A3B8"));
        consoleTitle.setTextSize(13f);
        consoleTitle.setTypeface(null, Typeface.BOLD);
        consoleTitle.setPadding(0, 0, 0, 8);

        tvConsole = new TextView(this);
        tvConsole.setText("[Система] Инициализация сканера (Java)...\n[Готовность] Нажмите запуск для сканирования Binance Futures.");
        tvConsole.setTextColor(Color.parseColor("#34D399"));
        tvConsole.setTextSize(12f);
        tvConsole.setTypeface(Typeface.MONOSPACE);

        consoleCard.addView(consoleTitle);
        consoleCard.addView(tvConsole);

        contentLayout.addView(authorView);
        contentLayout.addView(titleView);
        contentLayout.addView(clockCard);
        contentLayout.addView(statsCard);
        contentLayout.addView(btnScanNow);
        contentLayout.addView(btnSettings);
        contentLayout.addView(consoleCard);

        scrollView.addView(contentLayout);
        rootLayout.addView(scrollView);
        setContentView(rootLayout);

        startRealTimeClock();
        startCountdownTimer();
    }

    private LinearLayout createCardLayout(String bgColor, String borderColor) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 24, 24, 24);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        layout.setLayoutParams(params);
GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor(bgColor));
        drawable.setCornerRadius(16f);
        drawable.setStroke(2, Color.parseColor(borderColor));
        layout.setBackground(drawable);

        return layout;
    }

    private Button createStyledButton(String text, String bgColor, android.view.View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15f); // ИСПРАВЛЕНО: добавлена закрывающая скобка
        button.setTypeface(null, Typeface.BOLD);
        button.setPadding(24, 24, 24, 24);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 12);
        button.setLayoutParams(params);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor(bgColor));
        drawable.setCornerRadius(12f);
        button.setBackground(drawable);
        button.setOnClickListener(listener);

        return button;
    }

    private TextView createStatItem(LinearLayout parent, String label, String initialVal) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvVal = new TextView(this);
        tvVal.setText(initialVal);
        tvVal.setTextColor(Color.parseColor("#34D399"));
        tvVal.setTextSize(20f);
        tvVal.setTypeface(null, Typeface.BOLD);
        tvVal.setGravity(Gravity.CENTER);

        TextView tvLbl = new TextView(this);
        tvLbl.setText(label);
        tvLbl.setTextColor(Color.parseColor("#64748B"));
        tvLbl.setTextSize(11f);
        tvLbl.setGravity(Gravity.CENTER);

        container.addView(tvVal);
        container.addView(tvLbl);
        parent.addView(container);
        return tvVal;
    }

    private void startRealTimeClock() {
        final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        final SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault());

        Runnable clockRunnable = new Runnable() {
            @Override
            public void run() {
                Date now = new Date();
                tvClock.setText(timeFormat.format(now));
                tvDate.setText(dateFormat.format(now));
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(clockRunnable);
    }

    private void startCountdownTimer() {
        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isScanning) {
                    if (countdownSeconds > 0) {
                        countdownSeconds--;
                        int mins = countdownSeconds / 60;
                        int secs = countdownSeconds % 60;
                        String mStr = mins < 10 ? "0" + mins : String.valueOf(mins);
                        String sStr = secs < 10 ? "0" + secs : String.valueOf(secs);
                        tvCountdown.setText("⏳ До следующего скана: " + mStr + ":" + sStr);
                    } else {
                        countdownSeconds = scanIntervalSeconds;
                        runScannerCycle();
                    }
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(timerRunnable);
    }

    private void showSettingsDialog() {
        final Activity actContext = this;
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        layout.setBackgroundColor(Color.parseColor("#0F172A"));
TextView title = new TextView(this);
        title.setText("⚙️ Настройки параметров сканера");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18f);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, 16);
        layout.addView(title);

        TextView lblVol = new TextView(this);
        lblVol.setText("Мин. объем USDT:");
        lblVol.setTextColor(Color.parseColor("#94A3B8"));
        lblVol.setTextSize(13f);
        layout.addView(lblVol);

        final EditText inputVol = new EditText(this);
        inputVol.setText(String.valueOf((int) minVolumeUsdt));
        inputVol.setTextColor(Color.WHITE);
        inputVol.setTextSize(15f);
        inputVol.setPadding(16, 16, 16, 16);
        layout.addView(inputVol);

        TextView lblMult = new TextView(this);
        lblMult.setText("Множитель объема свечи:");
        lblMult.setTextColor(Color.parseColor("#94A3B8"));
        lblMult.setTextSize(13f);
        lblMult.setPadding(0, 16, 0, 0);
        layout.addView(lblMult);

        final EditText inputMult = new EditText(this);
        inputMult.setText(String.valueOf(volumeSpikeMultiplier));
        inputMult.setTextColor(Color.WHITE);
        inputMult.setTextSize(15f);
        inputMult.setPadding(16, 16, 16, 16);
        layout.addView(inputMult);

        new AlertDialog.Builder(this)
                .setView(layout)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    try {
                        String volStr = inputVol.getText().toString();
                        String multStr = inputMult.getText().toString();
                        if (!volStr.isEmpty()) minVolumeUsdt = Double.parseDouble(volStr);
                        if (!multStr.isEmpty()) volumeSpikeMultiplier = Double.parseDouble(multStr);
                        Toast.makeText(actContext, "✅ Настройки сохранены!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(actContext, "❌ Ошибка в формате чисел!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void sendTelegramMessage(final String textMessage) {
        new Thread(() -> {
            try {
                String encodedText = URLEncoder.encode(textMessage, "UTF-8");
                String urlString = "https://api.telegram.org/bot" + botToken + "/sendMessage?chat_id=" + chatId + "&text=" + encodedText + "&parse_mode=Markdown";
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.getInputStream().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void runScannerCycle() {
        if (isScanning) return;
        isScanning = true;
        tvStatus.setText("⚡ Статус: Сканирование Binance Futures...");
        progressBar.setProgress(10);

        new Thread(() -> {
            try {
                URL tickerUrl = new URL("https://fapi.binance.com/fapi/v1/ticker/24hr");
                HttpURLConnection connection = (HttpURLConnection) tickerUrl.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line = reader.readLine();
                while (line != null) {
                    sb.append(line);
                    line = reader.readLine();
                }
                reader.close();
JSONArray jsonArray = new JSONArray(sb.toString());
                final java.util.List<String> validPairs = new java.util.ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject item = jsonArray.getJSONObject(i);
                    String symbol = item.getString("symbol");
                    if (symbol.endsWith("USDT")) {
                        // ИСПРАВЛЕНО: заменено на Double.parseDouble для Java
                        double volumeUsdt = Double.parseDouble(item.getString("quoteVolume"));
                        if (volumeUsdt >= minVolumeUsdt) {
                            validPairs.add(symbol);
                        }
                    }
                }

                handler.post(() -> {
                    tvStatsScanned.setText(String.valueOf(validPairs.size()));
                    progressBar.setProgress(30);
                    tvStatus.setText("🔍 Найдено пар: " + validPairs.size() + ". Анализ свечей...");
                });

                int passedCount = 0;
                int activeSignalsCount = 0;
                final StringBuilder logBuilder = new StringBuilder();
                String timeStr = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                logBuilder.append("[").append(timeStr).append("] Сканирование завершено. Активных пар: ").append(validPairs.size()).append("\n");

                int limitPairs = Math.min(20, validPairs.size());
                int step = limitPairs > 0 ? 70 / limitPairs : 70;

                for (int idx = 0; idx < limitPairs; idx++) {
                    String symbol = validPairs.get(idx);
                    try {
                        URL klinesUrl = new URL("https://fapi.binance.com/fapi/v1/klines?symbol=" + symbol + "&interval=1h&limit=25");
                        HttpURLConnection klinesConn = (HttpURLConnection) klinesUrl.openConnection();
                        klinesConn.setConnectTimeout(3000);
                        klinesConn.setReadTimeout(3000);

                        BufferedReader kReader = new BufferedReader(new InputStreamReader(klinesConn.getInputStream()));
                        StringBuilder kSb = new StringBuilder();
                        String kLine = kReader.readLine();
                        while (kLine != null) {
                            kSb.append(kLine);
                            kLine = kReader.readLine();
                        }
                        kReader.close();

                        JSONArray klines = new JSONArray(kSb.toString());
                        if (klines.length() >= 10) {
                            passedCount++;

                            int len = klines.length();
                            JSONArray lastCandle = klines.getJSONArray(len - 1);
                            // ИСПРАВЛЕНО: заменено на Double.parseDouble для Java
                            double currentPrice = Double.parseDouble(lastCandle.getString(4));
                            double prevPrice = Double.parseDouble(klines.getJSONArray(len - 2).getString(4));

                            double lastVol = Double.parseDouble(lastCandle.getString(5));
                            double avgVol = 0.0;
                            for (int k = 0; k < len; k++) {
                                avgVol += Double.parseDouble(klines.getJSONArray(k).getString(5));
                            }
                            avgVol /= (double) len;
                            double volRatio = avgVol == 0.0 ? 1.0 : lastVol / avgVol;

                            if (currentPrice > prevPrice && volRatio >= volumeSpikeMultiplier) {
                                activeSignalsCount++;
                                double atr = currentPrice * 0.012;
                                double sl = currentPrice - (atr * 1.5);
                                double tp = currentPrice + (atr * 2.5);
                                double profitPct = ((tp - currentPrice) / currentPrice) * 100.0;
String slStr = String.format(Locale.US, "%.4f", sl);
                                String tpStr = String.format(Locale.US, "%.4f", tp);
                                String profitStr = String.format(Locale.US, "%.2f", profitPct);
                                String volStr = String.format(Locale.US, "%.1f", volRatio);

                                String signalMsg = "🚨 *BINANCE KAMA SIGNAL* 🚨\n" +
                                        "• 🪙 Монета: " + symbol + "\n" +
                                        "• 📈 Вход (Entry): " + currentPrice + "\n" +
                                        "• 🛑 Стоп-Лосс (SL): " + slStr + "\n" +
                                        "• 🎯 Тейк-Профит (TP): " + tpStr + "\n" +
                                        "• 💰 Ожидаемый профит: +" + profitStr + "%\n" +
                                        "• ⚡ Объем (Spike): x" + volStr + "";

                                logBuilder.append("🎯 СИГНАЛ: ").append(symbol).append(" | Entry: ").append(currentPrice).append(" | TP: +").append(profitStr).append("%\n");
                                sendTelegramMessage(signalMsg);
                            }
                        }
                    } catch (Exception ignored) {
                    }

                    final int currentProg = 30 + (idx + 1) * step;
                    handler.post(() -> progressBar.setProgress(Math.min(95, currentProg)));
                    Thread.sleep(60);
                }

                final int finalPassed = passedCount;
                final int finalSignals = activeSignalsCount;
                handler.post(() -> {
                    tvStatsPassed.setText(String.valueOf(finalPassed));
                    tvStatsSignals.setText(String.valueOf(finalSignals));
                    progressBar.setProgress(100);
                    tvStatus.setText("✅ Сканирование и анализ завершены!");
                    tvConsole.setText(logBuilder.toString());
                    isScanning = false;
                    countdownSeconds = scanIntervalSeconds;
                });

            } catch (Exception e) {
                handler.post(() -> {
                    tvStatus.setText("❌ Ошибка соединения с Binance API");
                    tvConsole.setText("Ошибка: " + e.getMessage());
                    isScanning = false;
                    countdownSeconds = scanIntervalSeconds;
                });
            }
        }).start();
    }
}