package com.diktofon.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.media.MediaRecorder;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.view.ViewGroup;
import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {
    private static final String TAG = "AudioRecorder";
    private static final int REQUEST_PERMISSIONS_CODE = 200;
    
    private MediaRecorder mediaRecorder;
    private String filePath;
    private boolean isRecording = false;
    private Button btnRecord;
    private Button btnShare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Создаем интерфейс с двумя кнопками
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        layout.setPadding(50, 50, 50, 50);

        btnRecord = new Button(this);
        btnRecord.setText("Начать запись");
        btnRecord.setTextSize(18);

        btnShare = new Button(this);
        btnShare.setText("Поделиться последней записью");
        btnShare.setTextSize(18);
        btnShare.setEnabled(false); // Сначала файла нет, кнопка неактивна

        // Сохранение напрямую в публичную папку Downloads
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs();
        }
        filePath = new File(downloadsDir, "test_record.mp4").getAbsolutePath();

        btnRecord.setOnClickListener(v -> {
            if (!isRecording) {
                // Запрос разрешений на микрофон и память
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, REQUEST_PERMISSIONS_CODE);
                } else {
                    startRecording();
                }
            } else {
                stopRecording();
            }
        });

        btnShare.setOnClickListener(v -> shareAudioFile());

        layout.addView(btnRecord);
        layout.addView(btnShare);
        setContentView(layout);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                Toast.makeText(this, "Нужны разрешения для записи и сохранения!", Toast.LENGTH_LONG).show();
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
            btnShare.setEnabled(false);
            
            Toast.makeText(this, "Идет запись...", Toast.LENGTH_SHORT).show();
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
            isRecording = false;
            btnRecord.setText("Начать запись");
            btnShare.setEnabled(true); // Активируем кнопку отправки файла
            
            Toast.makeText(this, "Сохранено в папку Downloads: test_record.mp4", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Запись сохранена в: " + filePath);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при остановке записи: " + e.getMessage());
        } finally {
            mediaRecorder = null;
        }
    }

    private void shareAudioFile() {
        File file = new File(filePath);
        if (!file.exists()) {
            Toast.makeText(this, "Файл записи не найден!", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("audio/mp4");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        startActivity(Intent.createChooser(intent, "Поделиться записью через:"));
    }
}