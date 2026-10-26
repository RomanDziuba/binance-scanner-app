package com.smuleloading.app;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private EditText urlInput, logEditText;
    private TextView statusText, songInfoText;
    private Button mainActionButton, btnClear, btnPaste, btnInfo, btnBackToMain;
    private LinearLayout infoCard, mainContainer, infoContainer;
    private Handler handler = new Handler(Looper.getMainLooper());
    private static String cachedRc4Key = null;

    private enum State { IDLE, ANALYZING, READY_TO_DOWNLOAD, DOWNLOADED }
    private State currentState = State.IDLE;
    private String resolvedMediaUrl = null;
    private String mediaExtension = "mp4";
    private String songTitle = "";
    private String songArtist = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.parseColor("#000000"));
        root.setPadding(20, 20, 20, 20);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView notesHeader = new TextView(this);
        notesHeader.setText("♫ 🎶 🎧 🎸 🎵 🎤 🎹 🎶 ♫");
        notesHeader.setTextSize(16f);
        notesHeader.setGravity(Gravity.CENTER);
        notesHeader.setPadding(0, 10, 0, 5);
        container.addView(notesHeader);

        // Логотип
        TextView logoView = new TextView(this);
        logoView.setText("⚡ SMULE VIP MATRIX DOWNLOADER ⚡");
        logoView.setTextColor(Color.parseColor("#38BDF8"));
        logoView.setTextSize(16f);
        logoView.setTypeface(Typeface.DEFAULT_BOLD);
        logoView.setGravity(Gravity.CENTER);
        logoView.setPadding(0, 0, 0, 15);
        container.addView(logoView);

        // --- ГОЛОВНИЙ КОНТЕЙНЕР (MAIN CONTAINER) ---
        mainContainer = new LinearLayout(this);
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(25, 25, 25, 25);
        
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#0A0F1D"));
        cardBg.setCornerRadius(32f);
        cardBg.setStroke(2, Color.parseColor("#1E293B"));
        card.setBackground(cardBg);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        card.setLayoutParams(cardParams);

        // Кнопка вставки из буфера
        btnPaste = createPillButton("📋 ВСТАВИТЬ З БУФЕРУ / PASTE", "#334155", 12f);
        btnPaste.setOnClickListener(v -> pasteFromClipboard());
        card.addView(btnPaste);

        // Поле ввода ссылки
        urlInput = new EditText(this);
        urlInput.setHint("ВСТАВТЕ ПОСИЛАННЯ НА ТРЕК SMULE ТУТ... / PASTE SMULE TRACK LINK HERE...");
        urlInput.setHintTextColor(Color.parseColor("#475569"));
        urlInput.setTextSize(12f);
        urlInput.setTextColor(Color.parseColor("#F1F5F9"));
        urlInput.setBackgroundColor(Color.parseColor("#030712"));
        urlInput.setPadding(20, 20, 20, 20);
        
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        inputParams.setMargins(0, 12, 0, 15);
        urlInput.setLayoutParams(inputParams);
        card.addView(urlInput);

        infoCard = new LinearLayout(this);
        infoCard.setOrientation(LinearLayout.VERTICAL);
        infoCard.setPadding(15, 15, 15, 15);
        GradientDrawable infoBg = new GradientDrawable();
        infoBg.setColor(Color.parseColor("#111827"));
        infoBg.setCornerRadius(16f);
        infoCard.setBackground(infoBg);
        infoCard.setVisibility(LinearLayout.GONE);

        songInfoText = new TextView(this);
        songInfoText.setTextColor(Color.parseColor("#34D399"));
        songInfoText.setTextSize(12f);
        songInfoText.setTypeface(Typeface.DEFAULT_BOLD);
        infoCard.addView(songInfoText);

        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        infoParams.setMargins(0, 0, 0, 12);
        infoCard.setLayoutParams(infoParams);
        card.addView(infoCard);

        // Главная кнопка
        mainActionButton = createPillButton("🔍 ЗНАЙТИ ТА ПІДГОТУВАТИ / FIND & PREPARE", "#2563EB", 13f);
        mainActionButton.setOnClickListener(v -> handleMainButtonClick());
        card.addView(mainActionButton);

        // Кнопка Инфо / Донат
        btnInfo = createPillButton("ℹ️ ІНФОРМАЦІЯ ТА ДОНАТ / INFO & DONATE", "#7C3AED", 12f);
        btnInfo.setOnClickListener(v -> showInfoScreen());
        card.addView(btnInfo);

        // Кнопка очистки
        btnClear = createPillButton("❌ ОЧИСТИТИ / СКИДАТИ / CLEAR / RESET", "#7F1D1D", 12f);
        btnClear.setOnClickListener(v -> resetState());
        card.addView(btnClear);

        statusText = new TextView(this);
        statusText.setText("СТАТУС: ОЧІКУВАННЯ ПОСИЛАННЯ... / STATUS: WAITING FOR LINK...");
        statusText.setTextColor(Color.parseColor("#94A3B8"));
        statusText.setTextSize(11f);
        statusText.setPadding(12, 10, 12, 10);
        
        GradientDrawable statusBg = new GradientDrawable();
        statusBg.setColor(Color.parseColor("#030712"));
        statusBg.setCornerRadius(12f);
        statusText.setBackground(statusBg);
        
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        statusParams.setMargins(0, 10, 0, 10);
        statusText.setLayoutParams(statusParams);
        card.addView(statusText);

        logEditText = new EditText(this);
        logEditText.setHint("ТЕХНІЧНІ ЛОГ-ДАНІ... / TECHNICAL LOG DATA...");
        logEditText.setTextSize(9f);
        logEditText.setTypeface(Typeface.MONOSPACE);
        logEditText.setTextColor(Color.parseColor("#64748B"));
        logEditText.setHintTextColor(Color.parseColor("#334155"));
        logEditText.setBackgroundColor(Color.parseColor("#030712"));
        logEditText.setPadding(10, 10, 10, 10);
        logEditText.setVisibility(LinearLayout.GONE);
        card.addView(logEditText);

        mainContainer.addView(card);
        container.addView(mainContainer);

        // --- ІНФОРМАЦІЙНИЙ КОНТЕЙНЕР (INFO CONTAINER) ---
        infoContainer = new LinearLayout(this);
        infoContainer.setOrientation(LinearLayout.VERTICAL);
        infoContainer.setVisibility(View.GONE);

        LinearLayout infoCardBox = new LinearLayout(this);
        infoCardBox.setOrientation(LinearLayout.VERTICAL);
        infoCardBox.setPadding(25, 25, 25, 25);
        
        GradientDrawable infoBoxBg = new GradientDrawable();
        infoBoxBg.setColor(Color.parseColor("#0A0F1D"));
        infoBoxBg.setCornerRadius(32f);
        infoBoxBg.setStroke(2, Color.parseColor("#1E293B"));
        infoCardBox.setBackground(infoBoxBg);

        TextView infoTitle = new TextView(this);
        infoTitle.setText("ІНСТРУКЦІЯ ТА ПРО ПРОЄКТ\nGUIDE & ABOUT");
        infoTitle.setTextColor(Color.parseColor("#38BDF8"));
        infoTitle.setTextSize(14f);
        infoTitle.setTypeface(Typeface.DEFAULT_BOLD);
        infoTitle.setGravity(Gravity.CENTER);
        infoTitle.setPadding(0, 0, 0, 15);
        infoCardBox.addView(infoTitle);

        TextView infoText = new TextView(this);
        String guideText = 
            "UA:\n" +
            "1. Скопіюйте посилання на трек у додатку Smule.\n" +
            "2. Вставте посилання у поле введення на головному екрані.\n" +
            "3. Натисніть кнопку Знайти та підготувати.\n" +
            "4. Після успішного аналізу натисніть Завантажити трек.\n" +
            "Файл збережеться у папку Downloads.\n\n" +
            "EN:\n" +
            "1. Copy the track link in the Smule app.\n" +
            "2. Paste the link into the input field on the main screen.\n" +
            "3. Click Find & Prepare.\n" +
            "4. After successful analysis, click Download Track.\n" +
            "The file will be saved to your Downloads folder.\n\n" +
            "--- ПРО ПРОЄКТ / ABOUT PROJECT ---\n" +
            "UA: Моя особиста розробка, поширюється та працює безплатно і без реклами. Можете користуватися! Якщо вам подобається і є можливість — можна задонатити 😜. Я старався для вас!\n\n" +
            "EN: My personal development, distributed and works completely free and without ads. Enjoy using it! If you like it and have the opportunity, you can donate 😜. I worked hard for you!";
        
        infoText.setText(guideText);
        infoText.setTextColor(Color.parseColor("#E2E8F0"));
        infoText.setTextSize(11f);
        infoText.setPadding(10, 10, 10, 15);
        infoCardBox.addView(infoText);

        TextView donateHeader = new TextView(this);
        donateHeader.setText("РЕКВІЗИТИ ДЛЯ ПІДТРИМКИ / SUPPORT REQUISITES");
        donateHeader.setTextColor(Color.parseColor("#F59E0B"));
        donateHeader.setTextSize(13f);
        donateHeader.setTypeface(Typeface.DEFAULT_BOLD);
        donateHeader.setGravity(Gravity.CENTER);
        donateHeader.setPadding(0, 10, 0, 10);
        infoCardBox.addView(donateHeader);

        // Реквизиты с кнопками быстрого копирования
        addRequisiteRow(infoCardBox, "UAH (Гривня):", "4441111079514034", "📋 СКОПІЮВАТИ UAH / COPY UAH");
        addRequisiteRow(infoCardBox, "USD (Долар):", "4874100087586429", "📋 СКОПІЮВАТИ USD / COPY USD");
        addRequisiteRow(infoCardBox, "EUR (Євро):", "4441111095966903", "📋 СКОПІЮВАТИ EUR / COPY EUR");

        // Кнопка возврата в меню
        btnBackToMain = createPillButton("⬅️ ГОЛОВНЕ МЕНЮ / MAIN MENU", "#2563EB", 12f);
        btnBackToMain.setOnClickListener(v -> showMainScreen());
        
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        backParams.setMargins(0, 20, 0, 5);
        btnBackToMain.setLayoutParams(backParams);
        infoCardBox.addView(btnBackToMain);

        infoContainer.addView(infoCardBox);
        container.addView(infoContainer);

        TextView notesFooter = new TextView(this);
        notesFooter.setText("🎵 🎧 🎶 🎤 🎸 🎹 🎵");
        notesFooter.setTextSize(14f);
        notesFooter.setGravity(Gravity.CENTER);
        notesFooter.setPadding(0, 15, 0, 10);
        container.addView(notesFooter);

        // Фирменная подпись
        TextView footerAuthor = new TextView(this);
        footerAuthor.setText("CREATED BY DZIUBA ROMAN 2026");
        footerAuthor.setTextColor(Color.parseColor("#F59E0B"));
        footerAuthor.setTextSize(13f);
        footerAuthor.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        footerAuthor.setGravity(Gravity.CENTER);
        footerAuthor.setPadding(0, 5, 0, 15);
        container.addView(footerAuthor);

        scroll.addView(container);
        root.addView(scroll);
        setContentView(root);
    }

    private void addRequisiteRow(LinearLayout parent, String label, String number, String btnText) {
        TextView labelView = new TextView(this);
        labelView.setText(label + "\n" + number);
        labelView.setTextColor(Color.parseColor("#34D399"));
        labelView.setTextSize(12f);
        labelView.setTypeface(Typeface.MONOSPACE);
        labelView.setPadding(10, 8, 10, 5);
        parent.addView(labelView);

        Button copyBtn = createPillButton(btnText, "#047857", 11f);
        copyBtn.setOnClickListener(v -> copyToClipboard(label, number));
        parent.addView(copyBtn);
    }

    private void copyToClipboard(String label, String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText(label, text));
                Toast.makeText(this, "СКОПІЙОВАНО / COPIED: " + text, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "ПОМИЛКА КОПІЮВАННЯ / COPY ERROR", Toast.LENGTH_SHORT).show();
        }
    }

    private void showInfoScreen() {
        mainContainer.setVisibility(View.GONE);
        infoContainer.setVisibility(View.VISIBLE);
    }

    private void showMainScreen() {
        infoContainer.setVisibility(View.GONE);
        mainContainer.setVisibility(View.VISIBLE);
    }

    private Button createPillButton(String text, String colorHex, float textSizeSp) {
        Button btn = new Button(this);
        btn.setText(text.toUpperCase());
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(textSizeSp);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        
        GradientDrawable pillShape = new GradientDrawable();
        pillShape.setColor(Color.parseColor(colorHex));
        pillShape.setCornerRadius(100f);
        btn.setBackground(pillShape);
        
        btn.setPadding(20, 22, 20, 22);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 0, 8);
        btn.setLayoutParams(params);
        return btn;
    }

    private void pasteFromClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                ClipData clip = clipboard.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    CharSequence text = clip.getItemAt(0).getText();
                    if (text != null && text.length() > 0) {
                        urlInput.setText(text.toString());
                        Toast.makeText(this, "ПОСИЛАННЯ ВСТАВЛЕНО З БУФЕРУ! / LINK PASTED!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
            Toast.makeText(this, "В БУФЕРІ НЕМАЄ ТЕКСТУ! / NO TEXT IN CLIPBOARD!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "ПОМИЛКА ВСТАВКИ / PASTE ERROR", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleMainButtonClick() {
        if (currentState == State.IDLE || currentState == State.DOWNLOADED) {
            startAnalysis();
        } else if (currentState == State.READY_TO_DOWNLOAD) {
            downloadMediaFile();
        }
    }

    private void startAnalysis() {
        String urlStr = urlInput.getText().toString().trim();
        if (urlStr.isEmpty()) {
            Toast.makeText(this, "БУДЬ ЛАСКА, ВВЕДІТЬ ПОСИЛАННЯ НА ТРЕК! / PLEASE ENTER A LINK!", Toast.LENGTH_SHORT).show();
            return;
        }

        currentState = State.ANALYZING;
        mainActionButton.setEnabled(false);
        GradientDrawable bg = (GradientDrawable) mainActionButton.getBackground();
        bg.setColor(Color.parseColor("#475569"));
        mainActionButton.setText("⏳ АНАЛІЗ ТРЕКУ... / ANALYZING...");
        statusText.setText("СТАТУС: СКАНУВАННЯ СТОРІНКИ ТА РОЗШИФРУВАННЯ... / STATUS: SCANNING & DECRYPTING...");

        new Thread(() -> {
            try {
                String pageHtml = downloadString(urlStr);
                if (pageHtml == null) throw new Exception("НЕ ВДАЛОСЯ ЗАВАНТАЖИТИ СТОРІНКУ. / FAILED TO LOAD PAGE.");

                songTitle = extractMeta(pageHtml, "\"title\"\\s*:\\s*\"([^\"]+)\"");
                songArtist = extractMeta(pageHtml, "\"artist\"\\s*:\\s*\"([^\"]+)\"");
                if (songTitle.isEmpty()) songTitle = "НЕВІДОМИЙ ТРЕК / UNKNOWN TRACK";
                if (songArtist.isEmpty()) songArtist = "НЕВІДОМИЙ ВИКОНАВЕЦЬ / UNKNOWN ARTIST";

                String encryptedUrl = null;
                mediaExtension = "mp4";

                Matcher m1 = Pattern.compile("\"video_media_mp4_url\"\\s*:\\s*\"([^\"]+)\"").matcher(pageHtml);
                if (m1.find()) {
                    encryptedUrl = m1.group(1);
                    mediaExtension = "mp4";
                } else {
                    Matcher m2 = Pattern.compile("\"visualizer_media_url\"\\s*:\\s*\"([^\"]+)\"").matcher(pageHtml);
                    if (m2.find()) {
                        encryptedUrl = m2.group(1);
                        mediaExtension = "mp4";
                    } else {
                        Matcher m3 = Pattern.compile("\"media_url\"\\s*:\\s*\"([^\"]+)\"").matcher(pageHtml);
                        if (m3.find()) {
                            encryptedUrl = m3.group(1);
                            mediaExtension = "m4a";
                        }
                    }
                }

                if (encryptedUrl == null || encryptedUrl.isEmpty()) {
                    throw new Exception("МЕДІАТОК НЕ ЗНАЙДЕНО В ДАННИХ СТОРІНКИ. / MEDIA TOKEN NOT FOUND.");
                }

                if (cachedRc4Key == null) {
                    cachedRc4Key = fetchDynamicKey(pageHtml);
                }
                if (cachedRc4Key == null) {
                    cachedRc4Key = decodeBase64Custom("TT18WlV5TXVeLXFXYn1WTF5qSmR9TXYpOHklYlFXWGY+SUZCRGNKPiU0emcyQ2l8dGVsamBkVlpA");
                }

                resolvedMediaUrl = rc4Decrypt(encryptedUrl, cachedRc4Key);

                if (resolvedMediaUrl != null && resolvedMediaUrl.startsWith("http")) {
                    handler.post(() -> {
                        currentState = State.READY_TO_DOWNLOAD;
                        mainActionButton.setEnabled(true);
                        GradientDrawable successBg = (GradientDrawable) mainActionButton.getBackground();
                        successBg.setColor(Color.parseColor("#10B981"));
                        mainActionButton.setText(("💾 ЗАВАНТАЖИТИ ТРЕК (" + mediaExtension + ") / DOWNLOAD").toUpperCase());
                        
                        songInfoText.setText(("🎵 ТРЕК: " + songArtist + " — " + songTitle + " [" + mediaExtension + "]").toUpperCase());
                        infoCard.setVisibility(LinearLayout.VISIBLE);
                        statusText.setText("СТАТУС: ГОТОВО ДО ЗАВАНТАЖЕННЯ! / STATUS: READY TO DOWNLOAD!");
                        logEditText.setText(resolvedMediaUrl);
                    });
                } else {
                    throw new Exception("ПОМИЛКА РОЗШИФРУВАННЯ ПОТОКУ. / STREAM DECRYPTION ERROR.");
                }

            } catch (Exception e) {
                final String errMsg = e.getMessage() != null ? e.getMessage().toUpperCase() : "НЕВІДОМА ПОМИЛКА / UNKNOWN ERROR";
                handler.post(() -> {
                    currentState = State.IDLE;
                    mainActionButton.setEnabled(true);
                    GradientDrawable errorBg = (GradientDrawable) mainActionButton.getBackground();
                    errorBg.setColor(Color.parseColor("#2563EB"));
                    mainActionButton.setText("🔍 ЗНАЙТИ ТА ПІДГОТУВАТИ / FIND & PREPARE");
                    statusText.setText("СТАТУС: ПОМИЛКА — " + errMsg);
                });
            }
        }).start();
    }

    private void downloadMediaFile() {
        if (resolvedMediaUrl == null) return;

        try {
            String fileName = "Smule_" + songArtist.replaceAll("[^a-zA-Z0-9а-яА-ЯіІїЇєЄґҐ]", "_") + "_" + System.currentTimeMillis() + "." + mediaExtension;
            
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(resolvedMediaUrl));
            request.setTitle(songArtist + " - " + songTitle);
            request.setDescription("Скачування медіа зі Smule / Downloading media from Smule");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(this, "ЗАВАНТАЖЕННЯ РОЗПОЧАТО! / DOWNLOAD STARTED!", Toast.LENGTH_SHORT).show();
            }

            currentState = State.DOWNLOADED;
            GradientDrawable downloadedBg = (GradientDrawable) mainActionButton.getBackground();
            downloadedBg.setColor(Color.parseColor("#2563EB"));
            mainActionButton.setText("🚀 ЗАВАНТАЖИТИ ІНШИЙ / DOWNLOAD ANOTHER");
            statusText.setText("СТАТУС: ФАЙЛ НАДІСЛАНО ДО ЗАВАНТАЖЕНЬ! / FILE SENT TO DOWNLOADS!");

        } catch (Exception e) {
            Toast.makeText(this, "ПОМИЛКА ЗАВАНТАЖЕННЯ / DOWNLOAD ERROR", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetState() {
        currentState = State.IDLE;
        resolvedMediaUrl = null;
        urlInput.setText("");
        infoCard.setVisibility(LinearLayout.GONE);
        mainActionButton.setEnabled(true);
        GradientDrawable defaultBg = (GradientDrawable) mainActionButton.getBackground();
        defaultBg.setColor(Color.parseColor("#2563EB"));
        mainActionButton.setText("🔍 ЗНАЙТИ ТА ПІДГОТУВАТИ / FIND & PREPARE");
        statusText.setText("СТАТУС: СКИНУТО. ОЧІКУВАННЯ ПОСИЛАННЯ... / STATUS: RESET. WAITING...");

        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""));
            }
        } catch (Exception ignored) {}

        Toast.makeText(this, "БУФЕР ТА ПОЛЕ ОЧИЩЕНО! / CLEARED!", Toast.LENGTH_SHORT).show();
    }

    private String extractMeta(String html, String regex) {
        try {
            Matcher m = Pattern.compile(regex).matcher(html);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String fetchDynamicKey(String pageHtml) {
        try {
            Matcher coreMatcher = Pattern.compile("src=\"([^\"]*community_core-[a-f0-9]+\\.js)\"").matcher(pageHtml);
            String coreUrl;
            if (coreMatcher.find()) {
                coreUrl = coreMatcher.group(1);
                if (coreUrl.startsWith("//")) coreUrl = "https:" + coreUrl;
            } else {
                coreUrl = "https://w1-fa.cdn.smule.com/assets/community_core-f1e2b046e16d487330152631ceb1fdee.js";
            }

            String coreJsText = downloadString(coreUrl);
            if (coreJsText == null) return null;

            Matcher keyMatcher = Pattern.compile("var\\s+v\\s*=\\s*p\\(\"([^\"]+)\"\\)").matcher(coreJsText);
            if (keyMatcher.find()) {
                return decodeBase64Custom(keyMatcher.group(1));
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String decodeBase64Custom(String b64) {
        try {
            String fixed = b64.replace('-', '+').replace('_', '/');
            byte[] decodedBytes = Base64.decode(fixed, Base64.DEFAULT);
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return b64;
        }
    }

    private String rc4Decrypt(String encUrl, String key) {
        if (encUrl == null || !encUrl.startsWith("e:")) return encUrl;
        try {
            String b64 = encUrl.substring(2);
            byte[] cipherBytes = Base64.decode(b64, Base64.DEFAULT);
            int[] iArray = new int[cipherBytes.length];
            for (int k = 0; k < cipherBytes.length; k++) {
                iArray[k] = cipherBytes[k] & 0xFF;
            }

            int[] r = new int[256];
            for (int a = 0; a < 256; a++) r[a] = a;

            int s = 0;
            for (int a = 0; a < 256; a++) {
                s = (s + r[a] + key.charAt(a % key.length())) % 256;
                int temp = r[a]; r[a] = r[s]; r[s] = temp;
            }

            int a = 0; s = 0;
            StringBuilder decrypted = new StringBuilder();
            for (int l = 0; l < iArray.length; l++) {
                a = (a + 1) % 256;
                s = (s + r[a]) % 256;
                int temp = r[a]; r[a] = r[s]; r[s] = temp;
                int keyStreamByte = r[(r[a] + r[s]) % 256];
                decrypted.append((char) (iArray[l] ^ keyStreamByte));
            }

            return decrypted.toString();
        } catch (Exception e) {
            return "ПОМИЛКА RC4: " + e.getMessage();
        }
    }

    private String downloadString(String targetUrl) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(targetUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append("\n");
                br.close();
                return sb.toString();
            }
        } catch (Exception ignored) {}
        return null;
    }
}