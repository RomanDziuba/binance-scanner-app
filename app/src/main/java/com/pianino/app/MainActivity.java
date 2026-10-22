package com.pianino.app;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;

public class MainActivity extends Activity {

    private static final int SAMPLE_RATE = 22050;
    
    // 10 популярных инструментов (разная гармоническая окраска звука)
    private final String[] INSTRUMENTS = {
        "🎹 Акустическое Пианино",
        "🎛 Аналоговый Синтезатор",
        "🎼 Духовой Орган",
        "🪵 Деревянная Маримба",
        "🔔 Яркий Ксилофон",
        "🪈 Мелодичная Флейта",
        "🎻 Смычковая Скрипка",
        "🎸 Электрогитара",
        "🎸 Глубокий Бас",
        "🔊 Чистый Синус (Тон)"
    };
    
    private int currentInstrumentIndex = 0;
    private Button btnInstrument;

    // Частоты нот первой октавы (Гц)
    private final double[] NOTE_FREQS = {
        261.63, // До (C4)
        277.18, // До# (C#4)
        293.66, // Ре (D4)
        311.13, // Ре# (D#4)
        329.63, // Ми (E4)
        349.23, // Фа (F4)
        369.99, // Фа# (F#4)
        392.00, // Соль (G4)
        415.30, // Соль# (G#4)
        440.00, // Ля (A4)
        466.16, // Ля# (A#4)
        493.88, // Си (B4)
        523.25  // До2 (C5)
    };

    private final String[] NOTE_NAMES = {
        "До", "До#", "Ре", "Ре#", "Ми", "Фа", "Фа#", "Соль", "Соль#", "Ля", "Ля#", "Си", "До²"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Разрешаем автоповорот экрана под любое положение телефона
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#090D16"));
        rootLayout.setPadding(15, 15, 15, 15);

        // Шапка с выбором инструментов
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER);
        topBar.setPadding(0, 0, 0, 15);

        btnInstrument = new Button(this);
        btnInstrument.setText(INSTRUMENTS[currentInstrumentIndex]);
        btnInstrument.setTextColor(Color.WHITE);
        btnInstrument.setTextSize(14f);
        btnInstrument.setTypeface(Typeface.DEFAULT_BOLD);
        
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#2563EB"));
        btnBg.setCornerRadius(20f);
        btnInstrument.setBackground(btnBg);
        
        btnInstrument.setOnClickListener(v -> {
            currentInstrumentIndex = (currentInstrumentIndex + 1) % INSTRUMENTS.length;
            btnInstrument.setText(INSTRUMENTS[currentInstrumentIndex]);
        });

        topBar.addView(btnInstrument);
        rootLayout.addView(topBar);

        // Панель пианино
        HorizontalScrollView scrollKeys = new HorizontalScrollView(this);
        LinearLayout keysLayout = new LinearLayout(this);
        keysLayout.setOrientation(LinearLayout.HORIZONTAL);
        keysLayout.setGravity(Gravity.BOTTOM);

        for (int i = 0; i < NOTE_FREQS.length; i++) {
            final double freq = NOTE_FREQS[i];
            final String name = NOTE_NAMES[i];
            final boolean isBlackKey = name.contains("#");

            Button keyButton = new Button(this);
            keyButton.setText(name);
            keyButton.setTypeface(Typeface.DEFAULT_BOLD);

            GradientDrawable keyBg = new GradientDrawable();
            if (isBlackKey) {
                keyBg.setColor(Color.parseColor("#1E293B"));
                keyButton.setTextColor(Color.parseColor("#38BDF8"));
                keyBg.setStroke(2, Color.parseColor("#0F172A"));
            } else {
                keyBg.setColor(Color.WHITE);
                keyButton.setTextColor(Color.BLACK);
                keyBg.setStroke(3, Color.parseColor("#94A3B8"));
            }
            keyBg.setCornerRadius(12f);
            keyButton.setBackground(keyBg);

            // Большие клавиши
            int width = isBlackKey ? 130 : 170;
            int height = isBlackKey ? 320 : 480;

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
            params.setMargins(4, 0, 4, 0);
            keyButton.setLayoutParams(params);

            keyButton.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    playNoteSound(freq);
                    keyBg.setColor(Color.parseColor("#F59E0B"));
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    keyBg.setColor(isBlackKey ? Color.parseColor("#1E293B") : Color.WHITE);
                    return true;
                }
                return false;
            });

            keysLayout.addView(keyButton);
        }

        scrollKeys.addView(keysLayout);
        rootLayout.addView(scrollKeys);

        setContentView(rootLayout);
    }

    // Генерация звуковой волны инструмента в реальном времени
    private void playNoteSound(double frequency) {
        new Thread(() -> {
            try {
                int durationMs = 600; // Длительность звучания ноты
                int numSamples = (int) ((durationMs / 1000.0) * SAMPLE_RATE);
                short[] sample = new short[numSamples];

                for (int i = 0; i < numSamples; i++) {
                    double t = (double) i / SAMPLE_RATE;
                    double wave = 0;

                    // Окраска звука в зависимости от инструмента
                    switch (currentInstrumentIndex) {
                        case 0: // Пианино (Основной тон + гармоники)
                            wave = Math.sin(2 * Math.PI * frequency * t) 
                                 + 0.5 * Math.sin(4 * Math.PI * frequency * t)
                                 + 0.2 * Math.sin(6 * Math.PI * frequency * t);
                            break;
                        case 1: // Синтезатор (Прямоугольная волна)
                            wave = Math.sin(2 * Math.PI * frequency * t) >= 0 ? 0.8 : -0.8;
                            break;
                        case 2: // Орган (Богатый набор гармоник)
                            wave = Math.sin(2 * Math.PI * frequency * t) 
                                 + Math.sin(4 * Math.PI * frequency * t) 
                                 + 0.5 * Math.sin(8 * Math.PI * frequency * t);
                            break;
                        case 3: // Маримба (Быстро затухающий деревянный тон)
                            wave = Math.sin(2 * Math.PI * frequency * t) * Math.exp(-i / 1500.0);
                            break;
                        case 4: // Ксилофон (Звонкий колокольчик)
                            wave = Math.sin(2 * Math.PI * frequency * t * 2) * Math.exp(-i / 800.0);
                            break;
                        case 5: // Флейта (Мягкий тон)
                            wave = Math.sin(2 * Math.PI * frequency * t) + 0.1 * Math.sin(6 * Math.PI * frequency * t);
                            break;
                        case 6: // Скрипка (Пилообразная волна)
                            wave = 0.6 * (2 * (t * frequency - Math.floor(0.5 + t * frequency)));
                            break;
                        case 7: // Электрогитара
                            wave = Math.tanh(2.5 * Math.sin(2 * Math.PI * frequency * t));
                            break;
                        case 8: // Бас (Низкая октава)
                            wave = Math.sin(Math.PI * frequency * t);
                            break;
                        case 9: // Чистый Синус
                        default:
                            wave = Math.sin(2 * Math.PI * frequency * t);
                            break;
                    }

                    // Плавно убираем щелчки в конце звука (Envelope)
                    double envelope = Math.min(1.0, (numSamples - i) / (double) (numSamples * 0.2));
                    sample[i] = (short) (wave * envelope * Short.MAX_VALUE * 0.6);
                }

                int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

                AudioTrack audioTrack = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build())
                        .setBufferSizeInBytes(Math.max(bufferSize, sample.length * 2))
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build();

                audioTrack.write(sample, 0, sample.length);
                audioTrack.play();

                // Автоматически освобождаем ресурсы
                Thread.sleep(durationMs);
                audioTrack.release();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}