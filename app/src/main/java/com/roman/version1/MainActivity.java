package com.roman.version1;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
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
    private float lastBearing = 0f;
    
    private GnssStatus.Callback gnssStatusCallback;
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
                    updateMapPosition(lastValidLocation, lastBearing);
                }
            }
        });

        // HTML/JS оболочка карты со стрелкой и динамическим автопанорамированием
        String mapHtml = "<!DOCTYPE html><html><head><meta charset=\"utf-8\" />" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />" +
                "<script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>" +
                "<style>" +
                "body, html { margin: 0; padding: 0; height: 100%; width: 100%; background: #030712; }" +
                "#map { width: 100%; height: 100%; }" +
                ".custom-arrow-icon { background: transparent !important; border: none !important; }" +
                "</style></head><body>" +
                "<div id=\"map\"></div><script>" +
                "var map = L.map('map', {zoomControl: false}).setView([48.3794, 31.1656], 6);" +
                "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '© OpenStreetMap' }).addTo(map);" +
                "var marker = null; var accuracyCircle = null;" +
                "function updateMarker(lat, lon, accuracy, bearing) {" +
                "  var latLng = [lat, lon];" +
                "  var arrowHtml = '<div style=\"transform: rotate(' + bearing + 'deg); width: 44px; height: 44px; display: flex; align-items: center; justify-content: center; filter: drop-shadow(0px 2px 4px rgba(0,0,0,0.6));\">' +" +
                "    '<svg width=\"36\" height=\"36\" viewBox=\"0 0 24 24\" fill=\"#38BDF8\" xmlns=\"http://www.w3.org/2000/svg\">' +" +
                "    '<path d=\"M12 2L3 19L12 15L21 19L12 2Z\" stroke=\"#ffffff\" stroke-width=\"2\" stroke-linejoin=\"round\"/>' +" +
                "    '</svg></div>';" +
                "  var icon = L.divIcon({" +
                "    className: 'custom-arrow-icon'," +
                "    html: arrowHtml," +
                "    iconSize: [44, 44]," +
                "    iconAnchor: [22, 22]" +
                "  });" +
                "  if (!marker) {" +
                "    marker = L.marker(latLng, {icon: icon, zIndexOffset: 1000}).addTo(map);" +
                "    accuracyCircle = L.circle(latLng, {radius: accuracy, color: '#38BDF8', fillColor: '#38BDF8', fillOpacity: 0.15, weight: 1}).addTo(map);" +
                "    map.setView(latLng, 16);" +
                "  } else {" +
                "    marker.setLatLng(latLng);" +
                "    marker.setIcon(icon);" +
                "    marker.setZIndexOffset(1000);" +
                "    accuracyCircle.setLatLng(latLng);" +
                "    accuracyCircle.setRadius(Math.max(accuracy, 10));" +
                "    var bounds = map.getBounds();" +
                "    var center = map.getCenter();" +
                "    var latSpan = bounds.getNorth() - bounds.getSouth();" +
                "    var lonSpan = bounds.getEast() - bounds.getWest();" +
                "    if (Math.abs(lat - center.lat) > latSpan * 0.35 || Math.abs(lon - center.lng) > lonSpan * 0.35) {" +
                "      map.panTo(latLng);" +
                "    }" +
                "  }" +
                "}" +
                "function centerMap(lat, lon) { map.setView([lat, lon], map.getZoom()); }" +
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
                updateMapPosition(lastValidLocation, lastBearing);
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

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    gnssStatusCallback = new GnssStatus.Callback() {
                        @Override
                        public void onStarted() {}

                        @Override
                        public void onStopped() {
                            runOnUiThread(() -> {
                                statusTextView.setText("⚠️ РЭБ / ПОТЕРЯ СИГНАЛА СПУТНИКОВ — Удержание точки");
                                statusTextView.setTextColor(Color.parseColor("#DC2626"));
                            });
                        }

                        @Override
                        public void onSatelliteStatusChanged(GnssStatus status) {
                            int satCount = 0;
                            for (int i = 0; i < status.getSatelliteCount(); i++) {
                                if (status.usedInFix(i)) satCount++;
                            }
                            if (satCount == 0 && lastValidLocation != null) {
                                runOnUiThread(() -> {
                                    statusTextView.setText("⚠️ РЭБ / НОЛЬ СПУТНИКОВ — Удержание точки");
                                    statusTextView.setTextColor(Color.parseColor("#DC2626"));
                                });
                            }
                        }
                    };
                    locationManager.registerGnssStatusCallback(gnssStatusCallback);
                }

                Location lastGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                Location lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (lastGPS != null) onLocationChanged(lastGPS);
                else if (lastNet != null) onLocationChanged(lastNet);
            }
        } catch (Exception e) {
            statusTextView.setText("❌ Ошибка GPS: " + e.getMessage());
        }
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

        // 🛡️ АНТИ-СПУФИНГ И АНТИ-ПРЕЖДЕВРЕМЕННЫЙ СКАЧОК (Фильтр РЭБ)
        if (lastValidLocation != null) {
            float distanceInMeters = location.distanceTo(lastValidLocation);
            long timeDeltaMs = location.getTime() - lastValidLocation.getTime();
            if (timeDeltaMs <= 0) timeDeltaMs = 1000;
            float calculatedSpeed = distanceInMeters / (timeDeltaMs / 1000f); // м/с

            // Если координаты сдвинулись быстрее чем 130 км/ч (~36 м/с) или телепортация > 250 метров за раз
            if (calculatedSpeed > 36.0f || distanceInMeters > 250f) {
                statusTextView.setText("⚠️ РЭБ / СПУФИНГ: Скачок координат отфильтрован!");
                statusTextView.setTextColor(Color.parseColor("#DC2626"));
                return; // Игнорируем фальшивые координаты РЭБ!
            }
        }

        lastValidLocation = location;
        
        if (location.hasBearing() && location.getSpeed() > 0.3f) {
            lastBearing = location.getBearing();
        }

        statusTextView.setText("🟢 СВЯЗЬ АКТИВНА (ОНЛАЙН КАРТА)");
        statusTextView.setTextColor(Color.parseColor("#10B981"));

        double lat = location.getLatitude();
        double lon = location.getLongitude();
        float acc = location.hasAccuracy() ? location.getAccuracy() : 20f;
        float speed = location.hasSpeed() ? location.getSpeed() * 3.6f : 0f;

        coordTextView.setText(String.format("Шир: %.5f | Дол: %.5f\nТочность: %.1fм | Скорость: %.1f км/ч", lat, lon, acc, speed));

        updateMapPosition(location, lastBearing);
    }

    private void updateMapPosition(Location loc, float bearing) {
        if (isMapLoaded && loc != null) {
            String js = String.format("updateMarker(%.6f, %.6f, %.1f, %.1f);", loc.getLatitude(), loc.getLongitude(), loc.hasAccuracy() ? loc.getAccuracy() : 20f, bearing);
            webView.evaluateJavascript(js, null);
        }
    }

    @Override public void onProviderEnabled(String provider) {}
    @Override public void onProviderDisabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            statusTextView.setText("⚠️ GPS ОТКЛЮЧЕН / ПОДАВЛЕН РЭБ");
            statusTextView.setTextColor(Color.parseColor("#DC2626"));
        }
    }
    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            try { 
                locationManager.removeUpdates(this); 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && gnssStatusCallback != null) {
                    locationManager.unregisterGnssStatusCallback(gnssStatusCallback);
                }
            } catch (Exception ignored) {}
        }
    }
}