package com.roma.nativeapp;

import android.app.Activity;
import android.graphics.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private TextView priceTextView, statusTextView;
    private CandleStickView chartView;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRunning = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Главный контейнер (Темный стиль терминала)
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#030712"));
        root.setPadding(24, 24, 24, 24);

        // Заголовок
        TextView title = new TextView(this);
        title.setText("📈 ETH / USDT (Binance Futures 1m)");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(16f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, 8);
        root.addView(title);

        // Текущая цена
        priceTextView = new TextView(this);
        priceTextView.setText("Цена: Загрузка...");
        priceTextView.setTextColor(Color.parseColor("#10B981"));
        priceTextView.setTextSize(22f);
        priceTextView.setTypeface(Typeface.MONOSPACE);
        priceTextView.setPadding(0, 0, 0, 12);
        root.addView(priceTextView);

        // График свечей (Custom View)
        chartView = new CandleStickView(this);
        LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        );
        chartView.setLayoutParams(chartParams);
        root.addView(chartView);

        // Статус строка
        statusTextView = new TextView(this);
        statusTextView.setText("Статус: Подключение к Binance Futures API...");
        statusTextView.setTextColor(Color.parseColor("#64748B"));
        statusTextView.setTextSize(11f);
        statusTextView.setPadding(0, 12, 0, 0);
        root.addView(statusTextView);

        setContentView(root);
        startDataFetching();
    }

    private void startDataFetching() {
        new Thread(() -> {
            while (isRunning) {
                try {
                    // 1. Запрос текущей цены фьючерса ETHUSDT
                    URL priceUrl = new URL("https://fapi.binance.com/fapi/v1/ticker/price?symbol=ETHUSDT");
                    HttpURLConnection pConn = (HttpURLConnection) priceUrl.openConnection();
                    pConn.setRequestMethod("GET");
                    BufferedReader pReader = new BufferedReader(new InputStreamReader(pConn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder pSb = new StringBuilder();
                    String line;
                    while ((line = pReader.readLine()) != null) pSb.append(line);
                    pReader.close();
                    
                    JSONObject pJson = new JSONObject(pSb.toString());
                    final String currentPrice = pJson.optString("price", "0.00");

                    // 2. Запрос минутных свечей (Klines)
                    URL klinesUrl = new URL("https://fapi.binance.com/fapi/v1/klines?symbol=ETHUSDT&interval=1m&limit=30");
                    HttpURLConnection kConn = (HttpURLConnection) klinesUrl.openConnection();
                    kConn.setRequestMethod("GET");
                    BufferedReader kReader = new BufferedReader(new InputStreamReader(kConn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder kSb = new StringBuilder();
                    while ((line = kReader.readLine()) != null) kSb.append(line);
                    kReader.close();

                    JSONArray klinesArray = new JSONArray(kSb.toString());
                    List<Candle> candles = new ArrayList<>();

                    for (int i = 0; i < klinesArray.length(); i++) {
                        JSONArray k = klinesArray.getJSONArray(i);
                        float open = (float) k.getDouble(1);
                        float high = (float) k.getDouble(2);
                        float low = (float) k.getDouble(3);
                        float close = (float) k.getDouble(4);
                        candles.add(new Candle(open, high, low, close));
                    }

                    // Обновляем UI в главном потоке
                    handler.post(() -> {
                        priceTextView.setText("⚡ " + currentPrice + " USDT");
                        chartView.updateCandles(candles);
                        statusTextView.setText("Статус: Данные обновлены успешно (" + candles.size() + " свечей)");
                    });

                } catch (Exception e) {
                    handler.post(() -> statusTextView.setText("Ошибка сети: " + e.getMessage()));
                }

                try {
                    Thread.sleep(5000); // Обновление каждые 5 секунд
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;
    }

    // Структура данных свечи
    public static class Candle {
        float open, high, low, close;
        public Candle(float o, float h, float l, float c) {
            open = o; high = h; low = l; close = c;
        }
    }

    // Кастомный View для отрисовки свечей на Canvas
    public static class CandleStickView extends View {
        private List<Candle> candles = new ArrayList<>();
        private Paint paintGreen = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Paint paintRed = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Paint paintGrid = new Paint(Paint.ANTI_ALIAS_FLAG);

        public CandleStickView(android.content.Context context) {
            super(context);
            paintGreen.setColor(Color.parseColor("#3B82F6")); // Сине-голубой для роста
            paintGreen.setStyle(Paint.Style.FILL);
            paintGreen.setStrokeWidth(3f);

            paintRed.setColor(Color.parseColor("#EF4444")); // Красный для падения
            paintRed.setStyle(Paint.Style.FILL);
            paintRed.setStrokeWidth(3f);

            paintGrid.setColor(Color.parseColor("#1E293B"));
            paintGrid.setStrokeWidth(1f);
        }

        public void updateCandles(List<Candle> newCandles) {
            this.candles = newCandles;
            invalidate(); // Перерисовка экрана
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (candles == null || candles.isEmpty()) return;

            int width = getWidth();
            int height = getHeight();

            // Рисуем сетку фона
            for (int i = 1; i < 5; i++) {
                float y = (height / 5f) * i;
                canvas.drawLine(0, y, width, y, paintGrid);
            }

            // Находим минимальную и максимальную цену для масштабирования
            float minPrice = Float.MAX_VALUE;
            float maxPrice = Float.MIN_VALUE;
            for (Candle c : candles) {
                if (c.low < minPrice) minPrice = c.low;
                if (c.high > maxPrice) maxPrice = c.high;
            }

            float priceRange = maxPrice - minPrice;
            if (priceRange == 0) priceRange = 1f;

            float candleWidth = (float) width / candles.size() - 4f;

            for (int i = 0; i < candles.size(); i++) {
                Candle c = candles.get(i);
                boolean isGrowing = c.close >= c.open;
                Paint paint = isGrowing ? paintGreen : paintRed;

                float x = i * (candleWidth + 4f) + 2f;

                // Переводим цены в координаты экрана Y
                float highY = height - ((c.high - minPrice) / priceRange) * (height - 20) - 10;
                float lowY = height - ((c.low - minPrice) / priceRange) * (height - 20) - 10;
                float openY = height - ((c.open - minPrice) / priceRange) * (height - 20) - 10;
                float closeY = height - ((c.close - minPrice) / priceRange) * (height - 20) - 10;

                // Рисуем фитиль (тень свечи)
                canvas.drawLine(x + candleWidth / 2, highY, x + candleWidth / 2, lowY, paint);

                // Рисуем тело свечи
                float topBody = Math.min(openY, closeY);
                float bottomBody = Math.max(openY, closeY);
                float bodyHeight = Math.max(bottomBody - topBody, 2f); // минимальная высота 2px

                canvas.drawRect(x, topBody, x + candleWidth, topBody + bodyHeight, paint);
            }
        }
    }
}
