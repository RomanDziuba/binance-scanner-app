package com.diktofon.app;

import android.app.Activity;
import android.os.Bundle;
import android.media.MediaRecorder;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.view.ViewGroup;
import android.graphics.Color;
import java.io.IOException;

public class MainActivity extends Activity {
    private static final String TAG = "AudioRecorder";
    private MediaRecorder mediaRecorder;
    private String filePath;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Создаем простую кнопку для проверки микрофона
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        layout.setPadding(50, 50, 50, 50);

        Button btnRecord = new Button(this);
        btnRecord.setText("Начать запись");
        btnRecord.setTextSize(20);
        
        // Путь для сохранения файла записи
        filePath = getExternalFilesDir(null).getAbsolutePath() + "/test_record.mp4";

        btnRecord.setOnClickListener(v -> {
            if (!isRecording) {
                startRecording();
                isRecording = true;
                btnRecord.setText("Остановить запись");
            } else {
                stopRecording();
                isRecording = false;
                btnRecord.setText("Начать запись");
            }
        });

        layout.addView(btnRecord);
        setContentView(layout);
    }

    private void startRecording() {
        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setOutputFile(filePath);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            
            mediaRecorder.prepare();
            mediaRecorder.start();
            Log.d(TAG, "Запись началась успешно: " + filePath);
        } catch (IOException e) {
            Log.e(TAG, "Ошибка подготовки MediaRecorder: " + e.getMessage());
            mediaRecorder = null;
        }
    }

    private void stopRecording() {
        if (mediaRecorder == null) {
            return;
        }

        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            Log.d(TAG, "Запись остановлена и сохранена в: " + filePath);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при остановке записи: " + e.getMessage());
        } finally {
            mediaRecorder = null;
        }
    }
}