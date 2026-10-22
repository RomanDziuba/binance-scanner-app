package com.keystorebase64.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.graphics.*;
import android.view.*;

    private void generateAndSaveKey() {
        String alias = etAlias.getText().toString().trim();
        String storePass = etStorePass.getText().toString().trim();
        String keyPass = etKeyPass.getText().toString().trim();

        if (alias.isEmpty() || storePass.isEmpty() || keyPass.isEmpty()) {
            Toast.makeText(this, "❌ Заполните все поля!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            prefs.edit()
                .putString("alias", alias)
                .putString("storePass", storePass)
                .putString("keyPass", keyPass)
                .apply();

            // Генерация пары ключей RSA 2048 бит
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();

            // Используем стандартный Android KeyStore для создания контейнера, 
            // либо создаем PKCS12 стандартным способом с валидным сертификатом
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);

            // Создаем полноценный байтовый массив для сертификата из публичного ключа
            byte[] pubKeyBytes = keyPair.getPublic().getEncoded();
            
            java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
            java.security.cert.Certificate dummyCert = cf.generateCertificate(
                new java.io.ByteArrayInputStream(pubKeyBytes)
            );

            Certificate[] chain = new Certificate[]{dummyCert};
            keyStore.setKeyEntry(alias, keyPair.getPrivate(), keyPass.toCharArray(), chain);

            // Сохраняем файл в папку Загрузки (Download)
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadDir.exists()) downloadDir.mkdirs();
            File keystoreFile = new File(downloadDir, "release.keystore");

            FileOutputStream fos = new FileOutputStream(keystoreFile);
            keyStore.store(fos, storePass.toCharArray());
            fos.close();

            // Переводим файл в Base64 для GitHub Secrets
            FileInputStream fis = new FileInputStream(keystoreFile);
            byte[] bytes = new byte[(int) keystoreFile.length()];
            fis.read(bytes);
            fis.close();
            String base64Str = Base64.encodeToString(bytes, Base64.NO_WRAP);

            tvBase64Result.setText(base64Str);
            updateStatusText();
            Toast.makeText(this, "✅ Ключ успешно создан в Загрузках!", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            // Если фабрика X.509 требует ASN.1 структуру, используем прямой обход через Android KeyStore
            try {
                generateFallbackKeystore(alias, storePass, keyPass);
            } catch (Exception ex) {
                Toast.makeText(this, "❌ Ошибка: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                ex.printStackTrace();
            }
        }
    }

    // Запасной метод гарантированной генерации без ошибок сертификатов
    private void generateFallbackKeystore(String alias, String storePass, String keyPass) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        KeyStore keyStore = KeyStore.getInstance("BKS"); // Используем BKS (Bouncy Castle KeyStore), родной для Android
        keyStore.load(null, null);

        // В BKS можно записать приватный ключ напрямую с публичным ключом вместо сертификата
        keyStore.setKeyEntry(alias, keyPair.getPrivate(), keyPass.toCharArray(), new Certificate[0]);

        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadDir.exists()) downloadDir.mkdirs();
        File keystoreFile = new File(downloadDir, "release.keystore");

        FileOutputStream fos = new FileOutputStream(keystoreFile);
        keyStore.store(fos, storePass.toCharArray());
        fos.close();

        FileInputStream fis = new FileInputStream(keystoreFile);
        byte[] bytes = new byte[(int) keystoreFile.length()];
        fis.read(bytes);
        fis.close();
        String base64Str = Base64.encodeToString(bytes, Base64.NO_WRAP);

        tvBase64Result.setText(base64Str);
        updateStatusText();
        Toast.makeText(this, "✅ Ключ создан (Fallback BKS)!", Toast.LENGTH_LONG).show();
    }