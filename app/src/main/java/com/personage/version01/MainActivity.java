package com.personage.version01;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import android.graphics.*;

public class MainActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 101;
    
    private KimView kimView;
    private TextView statusTextView, clockTextView;
    private java.io.File storageDir;
    
    private Handler animHandler = new Handler(Looper.getMainLooper());
    private Runnable animRunnable;
    private float globalTime = 0f;
    private android.media.ToneGenerator toneGen;

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
            toneGen = new android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Автоматическое создание папки KimAssets во внешнем хранилище
        storageDir = new java.io.File(getExternalFilesDir(null), "KimAssets");
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

        // --- КНОПКА ВЫБОРА ФОТО ---
        Button btnPickImage = new Button(this);
        btnPickImage.setText("📁 ВЫБРАТЬ ФОТО ИЗ ПАМЯТИ");
        btnPickImage.setTextColor(Color.WHITE);
        btnPickImage.setTextSize(11);
        btnPickImage.setTypeface(Typeface.DEFAULT_BOLD);
        
        android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
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
        
        android.graphics.drawable.GradientDrawable clockBg = new android.graphics.drawable.GradientDrawable();
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
        
        android.graphics.drawable.GradientDrawable statusBg = new android.graphics.drawable.GradientDrawable();
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
        android.content.Intent intent = new android.content.Intent("android.intent.action.OPEN_DOCUMENT");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            android.net.Uri uri = data.getData();
            if (uri != null) {
                try {
                    java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
                    Bitmap selectedBitmap = BitmapFactory.decodeStream(inputStream);
                    if (inputStream != null) inputStream.close();

                    if (selectedBitmap != null) {
                        java.io.File outputFile = new java.io.File(storageDir, "kim.png");
                        java.io.FileOutputStream fos = new java.io.FileOutputStream(outputFile);
                        
                        selectedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        fos.flush();
                        fos.close();

                        if (kimView != null) {
                            kimView.reloadImage();
                        }
                        Toast.makeText(this, "Фото конвертировано в PNG и загружено!", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Ошибка обработки файла!", Toast.LENGTH_SHORT).show();
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
                String timeOnly = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
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
        private java.io.File dir;
        private float breathOffset = 0f;
        private float jumpOffset = 0f;
        private float mouthOpenRatio = 0f;
        private boolean isTalking = false;

        public KimView(Context context, java.io.File storageDir) {
            super(context);
            this.dir = storageDir;
            reloadImage();
        }

        public void reloadImage() {
            try {
                java.io.File imgFile = new java.io.File(dir, "kim.png");
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
                toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP_BEEP, 300);
            }
            
            java.util.Random random = new java.util.Random();
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
                canvas.drawText("чтобы выбрать фото через пикер!", width / 2f, height / 2f + 20, paint);
            }
        }
    }
}