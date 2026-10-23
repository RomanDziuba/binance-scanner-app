package com.diktofon.app;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Bundle;
import android.media.MediaRecorder;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.view.ViewGroup;
import java.io.IOException;

public class MainActivity extends Activity {
    private static final String TAG = "AudioRecorder";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    
    private MediaRecorder mediaRecorder;
    private String filePath;
    private boolean isRecording = false;
    private Button btnRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Создаем интерфейс с кнопкой
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        layout.setPadding(50, 50, 50, 50);

        btnRecord = new Button(this);
        btnRecord.setText("Начать запись");
        btnRecord.setTextSize(20);
        
        filePath = getExternalFilesDir(null).getAbsolutePath() + "/test_record.mp4";

        btnRecord.setOnClickListener(v -> {
            if (!isRecording) {
                // Проверяем разрешение перед стартом записи
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    // Если разрешения нет — запрашиваем у пользователя
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
                } else {
                    // Если разрешение уже есть — пишем звук
                    startRecording();
                }
            } else {
                stopRecording();
                isRecording = false;
                btnRecord.setText("Начать запись");
            }
        });

        layout.addView(btnRecord);
        setContentView(layout);
    }

    // Обработка ответа пользователя на системный запрос разрешения
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Пользователь нажал «Разрешить» — запускаем запись
                startRecording();
            } else {
                // Пользователь отказал
                Toast.makeText(this, "Нужно разрешение на микрофон для записи!", Toast.LENGTH_LONG).show();
            }
        }
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
            isRecording = true;
            btnRecord.setText("Остановить запись");
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
            Toast.makeText(this, "Запись сохранена!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при остановке записи: " + e.getMessage());
        } finally {
            mediaRecorder = null;
        }
    }
}