package com.smuledrv.app;

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

    private static String cachedRc4Key = null;

    /**
     * Главный метод: получает ссылку на трек Smule и возвращает расшифрованную прямую ссылку на MP4
     */
    public static String getDirectVideoUrl(String trackUrl) {
        try {
            // Шаг 1. Загружаем страницу трека
            String html = httpGet(trackUrl);
            if (html == null) return null;

            // Шаг 2. Ищем зашифрованную ссылку video_media_mp4_url в DataStore
            Matcher mp4Matcher = Pattern.compile("\"video_media_mp4_url\"\\s*:\\s*\"(e:[^\"]+)\"").matcher(html);
            if (!mp4Matcher.find()) {
                // Если нет видео, попробуем поискать аудио media_url
                mp4Matcher = Pattern.compile("\"media_url\"\\s*:\\s*\"(e:[^\"]+)\"").matcher(html);
                if (!mp4Matcher.find()) return null;
            }
            String encryptedUrl = mp4Matcher.group(1);

            // Шаг 3. Добываем актуальный RC4 ключ 'v' из скриптов страницы или core-движка
            String rc4Key = getOrFetchRc4Key(html);
            if (rc4Key == null) return null;

            // Шаг 4. Расшифровываем ссылку алгоритмом RC4
            return rc4Decrypt(encryptedUrl, rc4Key);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getOrFetchRc4Key(String pageHtml) {
        if (cachedRc4Key != null) return cachedRc4Key;

        try {
            // Ищем шаблон var v=p("...") прямо в HTML или подгружаем community_core.js
            Matcher keyMatcher = Pattern.compile("var\\s+v\\s*=\\s*p\\(\"([^\"]+)\"\\)").matcher(pageHtml);
            String b64Key = null;

            if (keyMatcher.find()) {
                b64Key = keyMatcher.group(1);
            } else {
                // Если во фрейме нет, ищем ссылку на community_core.js и качаем его
                Matcher coreJsMatcher = Pattern.compile("src=\"([^\"]*community_core-[^\"]+\\.js)\"").matcher(pageHtml);
                if (coreJsMatcher.find()) {
                    String coreUrl = coreJsMatcher.group(1);
                    if (coreUrl.startsWith("//")) coreUrl = "https:" + coreUrl;
                    String coreJsText = httpGet(coreUrl);
                    
                    Matcher coreKeyMatcher = Pattern.compile("var\\s+v\\s*=\\s*p\\(\"([^\"]+)\"\\)").matcher(coreJsText);
                    if (coreKeyMatcher.find()) {
                        b64Key = coreKeyMatcher.group(1);
                    }
                }
            }

            if (b64Key != null) {
                // Функция p() у Smule — это обычный Base64.decode
                byte[] decodedBytes = Base64.decode(b64Key, Base64.DEFAULT);
                cachedRc4Key = new String(decodedBytes, StandardCharsets.UTF_8);
                return cachedRc4Key;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String rc4Decrypt(String encUrl, String key) {
        if (!encUrl.startsWith("e:")) return encUrl;
        try {
            String b64Data = encUrl.substring(2);
            byte[] cipherBytes = Base64.decode(b64Data, Base64.DEFAULT);
            int[] iArray = new int[cipherBytes.length];
            for (int k = 0; k < cipherBytes.length; k++) {
                iArray[k] = cipherBytes[k] & 0xFF;
            }

            // RC4 KSA
            int[] r = new int[256];
            for (int a = 0; a < 256; a++) r[a] = a;

            int s = 0;
            for (int a = 0; a < 256; a++) {
                s = (s + r[a] + key.charAt(a % key.length())) % 256;
                int temp = r[a]; r[a] = r[s]; r[s] = temp;
            }

            // RC4 PRGA & Decryption
            int a = 0;
            s = 0;
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

    private static String httpGet(String targetUrl) {
        try {
            URL url = new URL(targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}