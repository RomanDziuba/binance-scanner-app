package com.gps.version1;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

// THEME: @android:style/Theme.Material.NoActionBar
public class MainActivity extends Activity implements LocationListener {

    private WebView webView;
    private LocationManager locationManager;
    private TextView statusTextView, coordTextView;
    private Location lastValidLocation = null;
    private long lastFixTimestamp = 0;
    
    private Handler watchdogHandler = new Handler(Looper.getMainLooper());
    private Runnable watchdogRunnable;
    private boolean isMapLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Держим экран включенным для постоянного контроля
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        checkAndRequestPermissions();

        // Корневой макет
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#030712"));

        // Верхняя панель статуса
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.VERTICAL);
        topBar.setBackgroundColor(Color.parseColor("#0F172A"));
        topBar.setPadding(20, 20, 20, 20);

        TextView title = new TextView(this);
        title.setText("🗺 ONLINE MAP + REB SHIELD");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(15f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        topBar.addView(title);

        statusTextView = new TextView(this);
        statusTextView.setText("⏳ Загрузка карты через интернет...");
        statusTextView.setTextColor(Color.parseColor("#FBBF24"));
        statusTextView.setTextSize(12f);
        statusTextView.setPadding(0, 4, 0, 2);
        topBar.addView(statusTextView);

        coordTextView = new TextView(this);
        coordTextView.setText("Шир: --- | Дол: --- | Точность: --- м");
        coordTextView.setTextColor(Color.parseColor("#E2E8F0"));
        coordTextView.setTextSize(11f);
        topBar.addView(coordTextView);

        root.addView(topBar);

        // WebView с интерактивной онлайн-картой OpenStreetMap / Leaflet
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadsImagesAutomatically(true);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                isMapLoaded = true;
                if (lastValidLocation != null) {
                    updateMapPosition(lastValidLocation);
                }
            }
        });

        // HTML/JS оболочка карты
        String mapHtml = "<!DOCTYPE html><html><head><meta charset=\"utf-8\" />" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />" +
                "<script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>" +
                "<style>body, html { margin: 0; padding: 0; height: 100%; width: 100%; background: #030712; }" +
                "#map { width: 100%; height: 100%; }</style></head><body>" +
                "<div id=\"map\"></div><script>" +
                "var map = L.map('map', {zoomControl: false}).setView([48.3794, 31.1656], 6);" +
                "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '© OpenStreetMap' }).addTo(map);" +
                "var marker = null; var accuracyCircle = null;" +
                "function updateMarker(lat, lon, accuracy) {" +
                "  var latLng = [lat, lon];" +
                "  if (!marker) {" +
                "    marker = L.marker(latLng).addTo(map);" +
                "    accuracyCircle = L.circle(latLng, {radius: accuracy, color: '#38BDF8', fillOpacity: 0.2}).addTo(map);" +
                "    map.setView(latLng, 16);" +
                "  } else {" +
                "    marker.setLatLng(latLng);" +
                "    accuracyCircle.setLatLng(latLng);" +
                "    accuracyCircle.setRadius(Math.max(accuracy, 10));" +
                "  }" +
                "}" +
                "function centerMap(lat, lon) { map.setView([lat, lon], 16); }" +
                "</script></body></html>";

        webView.loadDataWithBaseURL("https://example.com", mapHtml, "text/html", "UTF-8", null);

        LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f
        );
        webView.setLayoutParams(webParams);
        root.addView(webView);

        // Нижняя панель управления
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setBackgroundColor(Color.parseColor("#0F172A"));
        bottomBar.setPadding(16, 16, 16, 16);
        bottomBar.setGravity(Gravity.CENTER);

        Button btnCenter = new Button(this);
        btnCenter.setText("🎯 НАЙТИ МЕНЯ");
        btnCenter.setTextColor(Color.WHITE);
        btnCenter.setBackgroundColor(Color.parseColor("#1D4ED8"));
        btnCenter.setTextSize(11f);
        btnCenter.setPadding(16, 14, 16, 14);
        btnCenter.setOnClickListener(v -> {
            if (lastValidLocation != null) {
                updateMapPosition(lastValidLocation);
                webView.evaluateJavascript("centerMap(" + lastValidLocation.getLatitude() + ", " + lastValidLocation.getLongitude() + ");", null);
                Toast.makeText(this, "Центрирование по последней точке", Toast.LENGTH_SHORT).show();
            } else {
                forceReconnectGPS();
                Toast.makeText(this, "🔄 Запрос свежих координат...", Toast.LENGTH_SHORT).show();
            }
        });
        bottomBar.addView(btnCenter);

        Button btnReconnect = new Button(this);
        btnReconnect.setText("🔄 ПЕРЕПОДКЛЮЧИТЬ GPS");
        btnReconnect.setTextColor(Color.WHITE);
        btnReconnect.setBackgroundColor(Color.parseColor("#334155"));
        btnReconnect.setTextSize(11f);
        btnReconnect.setPadding(16, 14, 16, 14);
        btnReconnect.setOnClickListener(v -> forceReconnectGPS());
        bottomBar.addView(btnReconnect);

        root.addView(bottomBar);
        setContentView(root);

        initLocationManager();
        startWatchdog();
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            String[] perms = {
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            };
            List<String> needed = new ArrayList<>();
            for (String p : perms) {
                if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) {
                    needed.add(p);
                }
            }
            if (!needed.isEmpty()) {
                requestPermissions(needed.toArray(new String[0]), 100);
            }
        }
    }

    private void initLocationManager() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            if (locationManager != null && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0.0f, this);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 0.0f, this);

                Location lastGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                Location lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (lastGPS != null) onLocationChanged(lastGPS);
                else if (lastNet != null) onLocationChanged(lastNet);
            }
        } catch (Exception e) {
            statusTextView.setText("❌ Ошибка GPS: " + e.getMessage());
        }
    }

    private void startWatchdog() {
        watchdogRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - lastFixTimestamp;
                if (lastFixTimestamp > 0 && elapsed > 4000) {
                    statusTextView.setText("⚠️ РЭБ / СИГНАЛ ПОДАВЛЕН (" + (elapsed / 1000) + "с) — Удержание точки");
                    statusTextView.setTextColor(Color.parseColor("#DC2626"));

                    try {
                        if (locationManager != null && ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            locationManager.removeUpdates(MainActivity.this);
                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0.0f, MainActivity.this);
                            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 0.0f, MainActivity.this);
                        }
                    } catch (Exception ignored) {}
                }
                watchdogHandler.postDelayed(this, 2000);
            }
        };
        watchdogHandler.postDelayed(watchdogRunnable, 2000);
    }

    private void forceReconnectGPS() {
        try {
            if (locationManager != null && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.removeUpdates(this);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0.0f, this);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0.0f, this);

                Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (loc != null) {
                    onLocationChanged(loc);
                    Toast.makeText(this, "🔄 GPS перезапущен!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "⚠️ Глушение РЭБ. Ожидание сигнала...", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) return;
        lastValidLocation = location;
        lastFixTimestamp = System.currentTimeMillis();

        statusTextView.setText("🟢 СВЯЗЬ АКТИВНА (ОНЛАЙН КАРТА)");
        statusTextView.setTextColor(Color.parseColor("#10B981"));

        double lat = location.getLatitude();
        double lon = location.getLongitude();
        float acc = location.hasAccuracy() ? location.getAccuracy() : 20f;
        float speed = location.hasSpeed() ? location.getSpeed() * 3.6f : 0f;

        coordTextView.setText(String.format("Шир: %.5f | Дол: %.5f\nТочность: %.1fм | Скорость: %.1f км/ч", lat, lon, acc, speed));

        updateMapPosition(location);
    }

    private void updateMapPosition(Location loc) {
        if (isMapLoaded && loc != null) {
            String js = String.format("updateMarker(%.6f, %.6f, %.1f);", loc.getLatitude(), loc.getLongitude(), loc.hasAccuracy() ? loc.getAccuracy() : 20f);
            webView.evaluateJavascript(js, null);
        }
    }

    @Override public void onProviderEnabled(String provider) {}
    @Override public void onProviderDisabled(String provider) {}
    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            try { locationManager.removeUpdates(this); } catch (Exception ignored) {}
        }
        if (watchdogHandler != null && watchdogRunnable != null) {
            watchdogHandler.removeCallbacks(watchdogRunnable);
        }
    }
}