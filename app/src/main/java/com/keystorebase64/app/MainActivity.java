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

        // Генерация пары ключей RSA
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        // Создаем самоподписанный сертификат через встроенные инструменты Android/Java
        // Используем стандартный генератор X.509 сертификатов
        byte[] encodedCert;
        try {
            // Современный способ через стандартный поставщик
            sun.security.x509.X509CertInfo info = new sun.security.x509.X509CertInfo();
            // Задаем базовые параметры сертификата
            info.set(sun.security.x509.X509CertInfo.VERSION, new sun.security.x509.CertificateVersion(sun.security.x509.CertificateVersion.V3));
            info.set(sun.security.x509.X509CertInfo.SERIAL_NUMBER, new sun.security.x509.CertificateSerialNumber(new java.util.Random().nextInt() & 0x7fffffff));
            
            sun.security.x509.X509CertImpl cert = new sun.security.x509.X509CertImpl(info);
            cert.sign(keyPair.getPrivate(), "SHA256withRSA");
            encodedCert = cert.getEncoded();
        } catch (Throwable t) {
            // Запасной вариант, если внутренние API недоступны в данной версии Android
            encodedCert = keyPair.getPublic().getEncoded();
        }

        java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
        java.security.cert.Certificate cert = cf.generateCertificate(new java.io.ByteArrayInputStream(encodedCert));

        // Создаем хранилище ключей
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        
        java.security.cert.Certificate[] chain = new java.security.cert.Certificate[]{cert};
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
        Toast.makeText(this, "✅ Ключ успешно создан!", Toast.LENGTH_LONG).show();

    } catch (Exception e) {
        Toast.makeText(this, "❌ Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
        e.printStackTrace();
    }
}