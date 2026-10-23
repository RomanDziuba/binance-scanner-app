package com.diktofon.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.graphics.*;
import android.view.*;

import android.media.MediaRecorder;
import android.util.Log;
import java.io.IOException;
import java.io.File;

public class MainActivity {
    private static final String TAG = "AudioRecorder";
    private MediaRecorder mediaRecorder;
    private String filePath;

    // Конструктор принимает путь, куда сохранить файл (например, .3gp или .mp4)
    public AudioRecorder(String filePath) {
        this.filePath = filePath;
    }

    // Начать запись
    public void startRecording() {
        if (mediaRecorder != null) {
            return;
        }

        mediaRecorder = new MediaRecorder();
        // 1. Источник звука — микрофон
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        // 2. Формат контейнера (выходной файл)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        // 3. Путь сохранения
        mediaRecorder.setOutputFile(filePath);
        // 4. Кодек для аудио
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            Log.d(TAG, "Запись началась успешно: " + filePath);
        } catch (IOException e) {
            Log.e(TAG, "Ошибка подготовки MediaRecorder: " + e.getMessage());
        }
    }

    // Остановить запись
    public void stopRecording() {
        if (mediaRecorder == null) {
            return;
        }

        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            Log.d(TAG, "Запись остановлена и сохранена.");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при остановке записи: " + e.getMessage());
        }
    }
}