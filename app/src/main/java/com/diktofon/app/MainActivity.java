package com.diktofon.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.graphics.*;
import android.view.*; // Укажите ваш пакет, если он отличается

import android.media.MediaRecorder;
import android.util.Log;
import java.io.IOException;

public class MainActivity {
    private static final String TAG = "AudioRecorder";
    private MediaRecorder mediaRecorder;
    private String filePath;

    // Конструктор
    public AudioRecorder(String filePath) {
        this.filePath = filePath;
    }

    // Начать запись
    public void startRecording() {
        if (mediaRecorder != null) {
            return;
        }

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(filePath);
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