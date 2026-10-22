package com.keystorebase64.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.widget.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.Certificate;

public class MainActivity extends Activity {

    private TextView tvStatus, tvBase64Result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#030712"));
        root.setPadding(30, 40, 30, 30);

        TextView title = new TextView(this);
        title.setText("🔑 Генератор ключа APK для GitHub");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(18f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 30);
        root.addView(title);

        Button btnGenerate = createButton("🛠 СГЕНИРИРОВАТЬ КЛЮЧ", Color.parseColor("#2563EB"));
        btnGenerate.setOnClickListener(v -> generateAndSaveKey());
        root.addView(btnGenerate);

        Button btnDelete = createButton("🗑 УДАЛИТЬ КЛЮЧ", Color.parseColor("#DC2626"));
        btnDelete.setOnClickListener(v -> deleteKeyFile());
        root.addView(btnDelete);

        tvStatus = new TextView(this);
        tvStatus.setTextColor(Color.parseColor("#10B981"));
        tvStatus.setTextSize(14f);
        tvStatus.setPadding(0, 30, 0, 15);
        updateStatusText();
        root.addView(tvStatus);

        TextView lblBase64 = new TextView(this);
        lblBase64.setText("Строка для GitHub Secrets (Base64):");
        lblBase64.setTextColor(Color.parseColor("#94A3B8"));
        lblBase64.setTextSize(13f);
        root.addView(lblBase64);

        tvBase64Result = new TextView(this);
        tvBase64Result.setTextColor(Color.parseColor("#F59E0B"));
        tvBase64Result.setTextSize(11f);
        tvBase64Result.setTypeface(Typeface.MONOSPACE);
        tvBase64Result.setBackgroundColor(Color.parseColor("#0F172A"));
        tvBase64Result.setPadding(15, 15, 15, 15);
        
        ScrollView scroll = new ScrollView(this);
        scroll.addView(tvBase64Result);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 300
        );
        scrollParams.setMargins(0, 10, 0, 20);
        scroll.setLayoutParams(scrollParams);
        root.addView(scroll);

        Button btnCopy = createButton("📋 СКОПИРОВАТЬ BASE64", Color.parseColor("#475569"));
        btnCopy.setOnClickListener(v -> {
            String text = tvBase64Result.getText().toString();
            if (!text.isEmpty() && !text.startsWith("Статус") && !text.startsWith("❌")) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Base64 Keystore", text);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "✅ Скопировано в буфер обмена!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "⚠️ Сначала сгенерируйте ключ!", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(btnCopy);

        setContentView(root);
    }

    private Button createButton(String text, int bgColor) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(14f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(20f);
        btn.setBackground(bg);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 120
        );
        params.setMargins(0, 10, 0, 10);
        btn.setLayoutParams(params);
        return btn;
    }

    private void generateAndSaveKey() {
        String alias = "my-release-key";
        String password = "password123";

        try {
            File keystoreFile = new File(getFilesDir(), "release.keystore");
            if (keystoreFile.exists()) keystoreFile.delete();

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();

            // Создаем хранилище BKS (Bouncy Castle KeyStore), которое является родным для Android 
            // и позволяет сохранять ключи без строгой проверки сертификатов X.509
            KeyStore keyStore = KeyStore.getInstance("BKS");
            keyStore.load(null, null);

            // Создаем простой класс-обертку сертификата, который возвращает публичный ключ
            Certificate cert = new Certificate("X.509") {
                @Override
                public byte[] getEncoded() { return keyPair.getPublic().getEncoded(); }
                @Override
                public void verify(PublicKey key) {}
                @Override
                public void verify(PublicKey key, String sigProvider) {}
                @Override
                public String toString() { return "[X.509 Certificate]"; }
                @Override
                public PublicKey getPublicKey() { return keyPair.getPublic(); }
            };

            keyStore.setKeyEntry(alias, keyPair.getPrivate(), password.toCharArray(), new Certificate[]{cert});

            FileOutputStream fos = new FileOutputStream(keystoreFile);
            keyStore.store(fos, password.toCharArray());
            fos.close();

            FileInputStream fis = new FileInputStream(keystoreFile);
            byte[] bytes = new byte[(int) keystoreFile.length()];
            fis.read(bytes);
            fis.close();
            String base64Str = Base64.encodeToString(bytes, Base64.NO_WRAP);

            tvBase64Result.setText(base64Str);
            updateStatusText();
            Toast.makeText(this, "✅ Успешно! Ключ создан", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "❌ Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void deleteKeyFile() {
        File keystoreFile = new File(getFilesDir(), "release.keystore");
        if (keystoreFile.exists()) {
            boolean deleted = keystoreFile.delete();
            if (deleted) {
                tvBase64Result.setText("");
                updateStatusText();
                Toast.makeText(this, "🗑 Ключ удален!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "⚠️ Не удалось удалить файл", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "ℹ️ Файл не найден", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateStatusText() {
        File keystoreFile = new File(getFilesDir(), "release.keystore");
        if (keystoreFile.exists()) {
            tvStatus.setText("Статус: Файл 'release.keystore' готов ✅");
            try {
                FileInputStream fis = new FileInputStream(keystoreFile);
                byte[] bytes = new byte[(int) keystoreFile.length()];
                fis.read(bytes);
                fis.close();
                tvBase64Result.setText(Base64.encodeToString(bytes, Base64.NO_WRAP));
            } catch (Exception ignored) {}
        } else {
            tvStatus.setText("Статус: Ключ еще не создан ⚠️");
        }
    }
}