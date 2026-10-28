package com.eqsquantumshift.version01;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 101;
    
    private KimView kimView;
    private TextView statusTextView, clockTextView;
    private File storageDir;
    
    private Handler animHandler = new Handler(Looper.getMainLooper());
    private Runnable animRunnable;
    private float globalTime = 0f;
    private ToneGenerator toneGen;

    private String[] kimQuotes = {
        "💬 «What's the sitch? (Какая ситуация?)»",
        "💬 «Call me, beep me, if you wanna reach me!»",
        "💬 «Ready for action! (Готова к миссии!)»",
        "💬 «So not the cash! (Совсем не вовремя!)»",
        "💬 «This is a totally normal, normal day.»"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Автоматическое создание папки для работы с файлами
        storageDir = new File(getExternalFilesDir(null), "KimAssets");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        // Корневой макет
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#040711"));
        rootLayout.setPadding(14, 14, 14, 14);
        rootLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        // --- ШАПКА ---
        TextView titleView = new TextView(this);
        titleView.setText("🔥 КИМ ПЯТЬ-СПЛЮС: МИССИЯ АКТИВНА");
        titleView.setTextColor(Color.parseColor("#38BDF8"));
        titleView.setTextSize(12);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER);
        titleView.setPadding(0, 0, 0, 6);
        rootLayout.addView(titleView);

        // --- КНОПКА ВЫБОРА ФОТО ИЗ ПАМЯТИ ТЕЛЕФОНА ---
        Button btnPickImage = new Button(this);
        btnPickImage.setText("📁 ВЫБРАТЬ ФОТО С ТЕЛЕФОНА");
        btnPickImage.setTextColor(Color.WHITE);
        btnPickImage.setTextSize(11);
        btnPickImage.setTypeface(Typeface.DEFAULT_BOLD);
        
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor("#0284C7"));
        btnBg.setCornerRadius(10f);
        btnBg.setStroke(2, Color.parseColor("#38BDF8"));
        btnPickImage.setBackground(btnBg);
        
        btnPickImage.setOnClickListener(v -> openFileSelector());
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 0, 0, 8);
        btnPickImage.setLayoutParams(btnParams);
        rootLayout.addView(btnPickImage);

        // --- КВАНТОВЫЕ ЧАСЫ ---
        clockTextView = new TextView(this);
        clockTextView.setTextColor(Color.parseColor("#34D399"));
        clockTextView.setTextSize(11);
        clockTextView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        clockTextView.setGravity(Gravity.CENTER);
        clockTextView.setPadding(6, 4, 6, 4);
        
        GradientDrawable clockBg = new GradientDrawable();
        clockBg.setColors(new int[]{Color.parseColor("#064E3B"), Color.parseColor("#022C22")});
        clockBg.setCornerRadius(10f);
        clockBg.setStroke(2, Color.parseColor("#10B981"));
        clockTextView.setBackground(clockBg);
        
        LinearLayout.LayoutParams clockParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clockParams.setMargins(0, 0, 0, 8);
        clockTextView.setLayoutParams(clockParams);
        rootLayout.addView(clockTextView);
        startClockAnimation();

        // --- ВИДЖЕТ С ПЕРСОНАЖЕМ ---
        kimView = new KimView(this, storageDir);
        LinearLayout.LayoutParams charParams = new LinearLayout.LayoutParams(700, 900);
        charParams.setMargins(0, 4, 0, 8);
        kimView.setLayoutParams(charParams);
        rootLayout.addView(kimView);

        // --- СТАТУСНАЯ ПАНЕЛЬ ---
        statusTextView = new TextView(this);
        statusTextView.setText(kimQuotes[0]);
        statusTextView.setTextColor(Color.parseColor("#34D399"));
        statusTextView.setTextSize(12);
        statusTextView.setTypeface(Typeface.DEFAULT_BOLD);
        statusTextView.setGravity(Gravity.CENTER);
        statusTextView.setPadding(10, 10, 10, 10);
        
        GradientDrawable statusBg = new GradientDrawable();
        statusBg.setColor(Color.parseColor("#1E1B4B"));
        statusBg.setCornerRadius(12f);
        statusBg.setStroke(2, Color.parseColor("#6366F1"));
        statusTextView.setBackground(statusBg);
        
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        statusTextView.setLayoutParams(statusParams);
        rootLayout.addView(statusTextView);

        setContentView(rootLayout);
        startGlobalAnimationLoop();
    }

    private void openFileSelector() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    Bitmap selectedBitmap = BitmapFactory.decodeStream(inputStream);
                    if (inputStream != null) inputStream.close();

                    if (selectedBitmap != null) {
                        File outputFile = new File(storageDir, "kim.png");
                        FileOutputStream fos = new FileOutputStream(outputFile);
                        selectedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        fos.flush();
                        fos.close();

                        kimView.reloadImage();
                        Toast.makeText(this, "Фото загружено и конвертировано в PNG!", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Ошибка загрузки файла!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void startGlobalAnimationLoop() {
        animRunnable = new Runnable() {
            @Override
            public void run() {
                globalTime += 0.1f;
                if (kimView != null) {
                    kimView.updateAnimation(globalTime);
                }
                animHandler.postDelayed(this, 33);
            }
        };
        animHandler.post(animRunnable);
    }

    private void startClockAnimation() {
        Handler clockHandler = new Handler(Looper.getMainLooper());
        Runnable clockRunnable = new Runnable() {
            @Override
            public void run() {
                String timeOnly = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                clockTextView.setText("💠 [ " + timeOnly + " ] 💠 СИСТЕМА ГОТОВА");
                clockHandler.postDelayed(this, 1000);
            }
        };
        clockHandler.post(clockRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (animHandler != null && animRunnable != null) {
            animHandler.removeCallbacks(animRunnable);
        }
        if (toneGen != null) {
            toneGen.release();
        }
    }

    private class KimView extends View {
        private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Bitmap kimBitmap;
        private File dir;
        private float breathOffset = 0f;
        private float jumpOffset = 0f;
        private float mouthOpenRatio = 0f;
        private boolean isTalking = false;

        public KimView(Context context, File storageDir) {
            super(context);
            this.dir = storageDir;
            reloadImage();
        }

        public void reloadImage() {
            try {
                File imgFile = new File(dir, "kim.png");
                if (imgFile.exists()) {
                    kimBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                } else {
                    kimBitmap = null;
                }
            } catch (Exception e) {
                kimBitmap = null;
            }
            invalidate();
        }

        public void updateAnimation(float time) {
            breathOffset = (float) Math.sin(time * 2.5f) * 6f;

            if (isTalking) {
                mouthOpenRatio = (float)(Math.sin(time * 8.0f) * 0.5f + 0.5f);
            } else {
                mouthOpenRatio = 0f;
            }
            invalidate();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                triggerTalkingSequence();
                return true;
            }
            return super.onTouchEvent(event);
        }

        private void triggerTalkingSequence() {
            if (isTalking) return;
            isTalking = true;
            
            if (toneGen != null) {
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP_BEEP, 300);
            }
            
            Random random = new Random();
            statusTextView.setText(kimQuotes[random.nextInt(kimQuotes.length)]);
            statusTextView.setTextColor(Color.parseColor("#F59E0B"));

            jumpOffset = -40f;
            postDelayed(() -> jumpOffset = 0f, 200);

            postDelayed(() -> {
                isTalking = false;
                statusTextView.setTextColor(Color.parseColor("#34D399"));
            }, 2000);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float width = getWidth();
            float height = getHeight();

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(60, 56, 189, 248));
            canvas.drawCircle(width / 2f, height / 2f + 50, 190, paint);

            if (kimBitmap != null) {
                float destWidth = width * 0.92f;
                float destHeight = height * 0.92f;
                float left = (width - destWidth) / 2f;
                float top = (height - destHeight) / 2f + breathOffset + jumpOffset;

                RectF destRect = new RectF(left, top, left + destWidth, top + destHeight);
                canvas.drawBitmap(kimBitmap, null, destRect, paint);
                
                float cx = width / 2f;
                float mouthY = top + 0.185f * destHeight;

                paint.setColor(Color.parseColor("#E6735E"));
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3f);
                float lipstickLineWidth = 0.02f * destWidth;
                canvas.drawLine(cx - lipstickLineWidth/2, mouthY, cx + lipstickLineWidth/2, mouthY, paint);

                if (mouthOpenRatio > 0.01f) {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(Color.parseColor("#5D1F1A"));
                    float maxMouthHeight = 0.012f * destHeight;
                    float currentMouthHeight = maxMouthHeight * mouthOpenRatio;
                    
                    RectF mouthRect = new RectF(cx - lipstickLineWidth/2, mouthY + 1f, cx + lipstickLineWidth/2, mouthY + 1f + currentMouthHeight);
                    canvas.drawOval(mouthRect, paint);
                }
                
            } else {
                paint.setColor(Color.parseColor("#38BDF8"));
                paint.setTextSize(24f);
                paint.setTypeface(Typeface.DEFAULT_BOLD);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("Нажми кнопку сверху,", width / 2f, height / 2f - 15, paint);
                canvas.drawText("чтобы выбрать фото из памяти!", width / 2f, height / 2f + 20, paint);
            }
        }
    }
}