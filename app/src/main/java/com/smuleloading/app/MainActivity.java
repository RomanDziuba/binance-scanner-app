package com.smuleloading.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.graphics.*;
import android.view.*;

import android.util.Base64;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity {

    // Кэшируем ключ в памяти на время сессии работы приложения
    private static String cachedRc4Key = null;

    /**
     * Главный метод: принимает ссылку на трек Смул, находит ключ, расшифровывает и возвращает MP4
     */
    public static String getDirectVideoUrl(String trackPageUrl) {
        try {
            // Шаг 1. Сканируем страницу трека и получаем HTML + ссылку на community_core.js
            String pageHtml = downloadString(trackPageUrl);
            if (pageHtml == null) return null;

            // Извлекаем зашифрованную ссылку video_media_mp4_url из window.DataStore
            Matcher mp4Matcher = Pattern.compile("\"video_media_mp4_url\"\\s*:\\s*\"([^\"]+)\"").matcher(pageHtml);
            if (!mp4Matcher.find()) return null;
            String encryptedUrl = mp4Matcher.group(1);

            // Шаг 2. Получаем актуальный ключ v (если еще не кэширован или обновился)
            if (cachedRc4Key == null) {
                cachedRc4Key = fetchDynamicKey(pageHtml);
            }
            if (cachedRc4Key == null) {
                // Фоллбэк на твой текущий ключ на случай сбоя сети
                cachedRc4Key = decodeBase64Custom("TT18WlV5TXVeLXFXYn1WTF5qSmR9TXYpOHklYlFXWGY+SUZCRGNKPiU0emcyQ2l8dGVsamBkVlpA");
            }

            // Шаг 3. Расшифровываем через RC4
            return rc4Decrypt(encryptedUrl, cachedRc4Key);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Автоматически находит и скачивает актуальный community_core.js, вытаскивая оттуда ключ v
     */
    private static String fetchDynamicKey(String pageHtml) {
        try {
            // Ищем ссылку на community_core скрипт
            Matcher coreMatcher = Pattern.compile("src=\"([^\"]*community_core-[a-f0-9]+\\.js)\"").matcher(pageHtml);
            String coreUrl = null;
            if (coreMatcher.find()) {
                coreUrl = coreMatcher.group(1);
                if (coreUrl.startsWith("//")) coreUrl = "https:" + coreUrl;
            } else {
                coreUrl = "https://w1-fa.cdn.smule.com/assets/community_core-f1e2b046e16d487330152631ceb1fdee.js"; // Дефолт
            }

            String coreJsText = downloadString(coreUrl);
            if (coreJsText == null) return null;

            // Ищем строку: var v=p("...")
            Matcher keyMatcher = Pattern.compile("var\\s+v\\s*=\\s*p\\(\"([^\"]+)\"\\)").matcher(coreJsText);
            if (keyMatcher.find()) {
                String b64KeyPayload = keyMatcher.group(1);
                return decodeBase64Custom(b64KeyPayload);
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Вспомогательный метод декодирования Base64 с учетом возможных символов URL-safe (- и _)
     */
    private static String decodeBase64Custom(String b64) {
        try {
            String fixed = b64.replace('-', '+').replace('_', '/');
            byte[] decodedBytes = Base64.decode(fixed, Base64.DEFAULT);
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return b64;
        }
    }

    /**
     * Точная реализация RC4 дешифратора Smule в Java
     */
    private static String rc4Decrypt(String encUrl, String key) {
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

            String result = decrypted.toString();
            return result.startsWith("http") ? result : encUrl;
        } catch (Exception e) {
            return encUrl;
        }
    }

    private static String downloadString(String targetUrl) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(targetUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");
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