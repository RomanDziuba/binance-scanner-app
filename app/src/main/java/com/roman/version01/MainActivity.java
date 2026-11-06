package com.roman.version01;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class MainActivity extends Activity {

    private Spinner spinnerPhase, spinnerRLC;
    private RadioGroup radioGroupDeviceType;
    private LinearLayout layoutDeviceRL, layoutDeviceCircle;
    private EditText etVoltage, etCurrent, etAngleWithRL, etAngleCircle;
    private TextView tvResults;
    private VectorCanvasView vectorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#030712"));
        root.setPadding(16, 16, 16, 16);

        ScrollView scroll = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        // Заголовок
        TextView title = new TextView(this);
        title.setText("⚡ РЗА: Анализатор фаз (R/L прибор vs Круговая шкала 0-360°)");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(14f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 12);
        container.addView(title);

        // Выбор фазы (А, В, С)
        TextView lblPhase = new TextView(this);
        lblPhase.setText("📌 Измеряемая фаза:");
        lblPhase.setTextColor(Color.parseColor("#94A3B8"));
        lblPhase.setTextSize(12f);
        container.addView(lblPhase);

        spinnerPhase = new Spinner(this);
        spinnerPhase.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{
            "Фаза A (опорная 0°)", "Фаза B (опорная -120°)", "Фаза C (опорная +120°)"
        }));
        container.addView(spinnerPhase);

        // Выбор типа прибора / метода ввода
        TextView lblDevice = new TextView(this);
        lblDevice.setText("⚙️ Тип прибора / метод отображения:");
        lblDevice.setTextColor(Color.parseColor("#94A3B8"));
        lblDevice.setTextSize(12f);
        lblDevice.setPadding(0, 10, 0, 4);
        container.addView(lblDevice);

        radioGroupDeviceType = new RadioGroup(this);
        radioGroupDeviceType.setOrientation(RadioGroup.HORIZONTAL);
        
        RadioButton rbRL = new RadioButton(this);
        rbRL.setText("1. Прибор с R / L / C");
        rbRL.setTextColor(Color.WHITE);
        rbRL.setChecked(true);
        radioGroupDeviceType.addView(rbRL);

        RadioButton rbCircle = new RadioButton(this);
        rbCircle.setText("2. Круговая шкала (0-360°)");
        rbCircle.setTextColor(Color.WHITE);
        radioGroupDeviceType.addView(rbCircle);

        container.addView(radioGroupDeviceType);

        // Общие параметры U и I
        etVoltage = createInput("Напряжение U фазное (В), например: 220", "220");
        container.addView(etVoltage);

        etCurrent = createInput("Ток I фазный (А), например: 10", "10");
        container.addView(etCurrent);

        // --- ВАРИАНТ 1: Прибор выдает градусы + индекс R / L / C ---
        layoutDeviceRL = new LinearLayout(this);
        layoutDeviceRL.setOrientation(LinearLayout.VERTICAL);

        etAngleWithRL = createInput("Угол сдвига φ от прибора (градусы), например: 30", "30");
        layoutDeviceRL.addView(etAngleWithRL);

        TextView lblRLC = new TextView(this);
        lblRLC.setText("Индикатор характера с прибора:");
        lblRLC.setTextColor(Color.parseColor("#94A3B8"));
        lblRLC.setTextSize(11f);
        layoutDeviceRL.addView(lblRLC);

        spinnerRLC = new Spinner(this);
        spinnerRLC.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{
            "L (Индуктивный характер / Ток отстает)",
            "R (Активный характер / Совпадение фаз)",
            "C (Емкостной характер / Ток опережает)"
        }));
        layoutDeviceRL.addView(spinnerRLC);
        container.addView(layoutDeviceRL);

        // --- ВАРИАНТ 2: Прибор с круговой шкалой 0 - 360 градусов ---
        layoutDeviceCircle = new LinearLayout(this);
        layoutDeviceCircle.setOrientation(LinearLayout.VERTICAL);
        layoutDeviceCircle.setVisibility(View.GONE);

        etAngleCircle = createInput("Угол по круговой шкале (0° - 360°), например: 330", "330");
        layoutDeviceCircle.addView(etAngleCircle);
        container.addView(layoutDeviceCircle);

        // Переключение видимости блоков ввода
        radioGroupDeviceType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == rbRL.getId()) {
                layoutDeviceRL.setVisibility(View.VISIBLE);
                layoutDeviceCircle.setVisibility(View.GONE);
            } else {
                layoutDeviceRL.setVisibility(View.GONE);
                layoutDeviceCircle.setVisibility(View.VISIBLE);
            }
        });

        // Кнопка расчета
        Button btnCalculate = new Button(this);
        btnCalculate.setText("🔄 Рассчитать и построить векторную диаграмму");
        btnCalculate.setTextColor(Color.WHITE);
        btnCalculate.setBackgroundColor(Color.parseColor("#1D4ED8"));
        btnCalculate.setPadding(12, 12, 12, 12);
        btnCalculate.setOnClickListener(v -> performCalculation());
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnParams.setMargins(0, 10, 0, 12);
        btnCalculate.setLayoutParams(btnParams);
        container.addView(btnCalculate);

        // Блок результатов
        tvResults = new TextView(this);
        tvResults.setText("Результаты анализа появятся здесь...");
        tvResults.setTextColor(Color.parseColor("#10B981"));
        tvResults.setTextSize(11f);
        tvResults.setPadding(12, 12, 12, 12);
        tvResults.setBackgroundColor(Color.parseColor("#0F172A"));
        container.addView(tvResults);

        // Холст для векторной диаграммы
        vectorView = new VectorCanvasView(this);
        LinearLayout.LayoutParams viewParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 650
        );
        viewParams.setMargins(0, 12, 0, 0);
        vectorView.setLayoutParams(viewParams);
        container.addView(vectorView);

        scroll.addView(container);
        root.addView(scroll);
        setContentView(root);
    }

    private EditText createInput(String hint, String defaultVal) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setText(defaultVal);
        et.setTextColor(Color.WHITE);
        et.setHintTextColor(Color.parseColor("#64748B"));
        et.setTextSize(11f);
        et.setPadding(10, 10, 10, 10);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        p.setMargins(0, 0, 0, 6);
        et.setLayoutParams(p);
        return et;
    }

    private void performCalculation() {
        try {
            int phaseIdx = spinnerPhase.getSelectedItemPosition();
            double baseVoltageAngle = 0.0;
            String phaseName = "Фаза A";
            if (phaseIdx == 1) {
                baseVoltageAngle = -120.0;
                phaseName = "Фаза B";
            } else if (phaseIdx == 2) {
                baseVoltageAngle = 120.0;
                phaseName = "Фаза C";
            }

            double u = Double.parseDouble(etVoltage.getText().toString());
            double i = Double.parseDouble(etCurrent.getText().toString());
            
            double calculatedShiftAngle = 0.0; // Итоговый сдвиг тока относительно напряжения
            String character = "";
            boolean isRLMode = (radioGroupDeviceType.getCheckedRadioButtonId() == radioGroupDeviceType.getChildAt(0).getId());

            if (isRLMode) {
                // РЕЖИМ 1: Прибор с R/L/C и градусами
                double rawAngle = Double.parseDouble(etAngleWithRL.getText().toString());
                int rlcType = spinnerRLC.getSelectedItemPosition(); // 0-L, 1-R, 2-C

                if (rlcType == 0) {
                    // L: Индуктивный (ток отстает от напряжения, угол положительный)
                    calculatedShiftAngle = Math.abs(rawAngle);
                    character = "Индуктивный характер (L): ток отстает от напряжения";
                } else if (rlcType == 1) {
                    // R: Активный
                    calculatedShiftAngle = 0.0;
                    character = "Активный характер (R): ток совпадает по фазе с напряжением";
                } else {
                    // C: Емкостной (ток опережает напряжение, угол отрицательный)
                    calculatedShiftAngle = -Math.abs(rawAngle);
                    character = "Емкостной характер (C): ток опережает напряжение";
                }
            } else {
                // РЕЖИМ 2: Прибор с круговой шкалой 0° - 360°
                double circleAngle = Double.parseDouble(etAngleCircle.getText().toString());
                circleAngle = circleAngle % 360;
                if (circleAngle < 0) circleAngle += 360;

                // Переводим круговую шкалу 0-360 в сдвиг относительно U в диапазон [-180, 180]
                // Обычно в круговых фазометрах 0-180 — отставание (индуктив.), 180-360 — опережение (емкост.)
                if (circleAngle <= 2.0 || circleAngle >= 358.0) {
                    calculatedShiftAngle = 0.0;
                    character = "Чисто активная нагрузка (R)";
                } else if (circleAngle < 180.0) {
                    calculatedShiftAngle = circleAngle; // Ток отстает (L)
                    character = "Индуктивный характер (L), сдвиг: " + circleAngle + "°";
                } else {
                    calculatedShiftAngle = circleAngle - 360.0; // Ток опережает (C)
                    character = "Емкостной характер (C), сдвиг: " + calculatedShiftAngle + "°";
                }
            }

            double rad = Math.toRadians(calculatedShiftAngle);
            double cosPhi = Math.cos(rad);
            double sinPhi = Math.sin(rad);

            double p = u * i * cosPhi;
            double q = u * i * sinPhi;
            double s = Math.sqrt(p * p + q * q);

            String report = String.format(
                "📊 АНАЛИЗ ДЛЯ [%s]:\n" +
                "• Опорное напряжение: %.1f°\n" +
                "• U = %.1f В | I = %.1f А\n" +
                "• Угол сдвига (φ): %.2f°\n" +
                "• Характер нагрузки: %s\n" +
                "• Коэффициент мощности (cos φ): %.3f\n" +
                "• Активная мощность P: %.2f Вт\n" +
                "• Реактивная мощность Q: %.2f ВАр\n" +
                "• Полная мощность S: %.2f ВА",
                phaseName, baseVoltageAngle, u, i, calculatedShiftAngle, character, cosPhi, p, q, s
            );

            tvResults.setText(report);
            vectorView.updateVectors(baseVoltageAngle, calculatedShiftAngle, phaseName);

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка ввода данных. Проверьте числа.", Toast.LENGTH_SHORT).show();
        }
    }

    // Компонент векторной диаграммы
    private static class VectorCanvasView extends View {
        private double voltageAngle = 0.0;
        private double currentShift = 30.0;
        private String phaseTitle = "Фаза A";
        
        private Paint paintU, paintI, paintGrid, paintText;

        public VectorCanvasView(Context context) {
            super(context);
            init();
        }

        private void init() {
            paintU = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintU.setColor(Color.parseColor("#EF4444")); // Красный - Напряжение U
            paintU.setStrokeWidth(5f);
            paintU.setStyle(Paint.Style.STROKE);

            paintI = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintI.setColor(Color.parseColor("#38BDF8")); // Голубой - Ток I
            paintI.setStrokeWidth(5f);
            paintI.setStyle(Paint.Style.STROKE);

            paintGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintGrid.setColor(Color.parseColor("#334155"));
            paintGrid.setStrokeWidth(2f);

            paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintText.setColor(Color.parseColor("#F8FAFC"));
            paintText.setTextSize(24f);
        }

        public void updateVectors(double vAngle, double cShift, String title) {
            this.voltageAngle = vAngle;
            this.currentShift = cShift;
            this.phaseTitle = title;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(Color.parseColor("#090D16"));

            int width = getWidth();
            int height = getHeight();
            float cx = width / 2f;
            float cy = height / 2f;
            float radius = Math.min(cx, cy) - 50f;

            canvas.drawCircle(cx, cy, radius, paintGrid);
            canvas.drawLine(cx - radius - 15, cy, cx + radius + 15, cy, paintGrid);
            canvas.drawLine(cx, cy - radius - 15, cx, cy + radius + 15, paintGrid);

            // Вектор напряжения U
            double uRad = Math.toRadians(-voltageAngle);
            float endUX = (float) (cx + radius * 0.8f * Math.cos(uRad));
            float endUY = (float) (cy + radius * 0.8f * Math.sin(uRad));
            drawArrow(canvas, cx, cy, endUX, endUY, paintU);
            canvas.drawText("U_" + phaseTitle.replace("Фаза ", ""), endUX + 10, endUY - 10, paintText);

            // Вектор тока I (сдвиг относительно U)
            double totalCurrentAngle = voltageAngle + currentShift;
            double iRad = Math.toRadians(-totalCurrentAngle);
            float endIX = (float) (cx + radius * 0.65f * Math.cos(iRad));
            float endIY = (float) (cy + radius * 0.65f * Math.sin(iRad));
            drawArrow(canvas, cx, cy, endIX, endIY, paintI);
            canvas.drawText(String.format("I (φ=%.1f°)", currentShift), endIX + 10, endIY + 10, paintText);
        }

        private void drawArrow(Canvas canvas, float startX, float startY, float endX, float endY, Paint paint) {
            canvas.drawLine(startX, startY, endX, endY, paint);
            float angle = (float) Math.atan2(endY - startY, endX - startX);
            float arrowLen = 20f;
            float p1x = (float) (endX - arrowLen * Math.cos(angle - Math.PI / 6));
            float p1y = (float) (endY - arrowLen * Math.sin(angle - Math.PI / 6));
            float p2x = (float) (endX - arrowLen * Math.cos(angle + Math.PI / 6));
            float p2y = (float) (endY - arrowLen * Math.sin(angle + Math.PI / 6));

            Path path = new Path();
            path.moveTo(endX, endY);
            path.lineTo(p1x, p1y);
            path.lineTo(p2x, p2y);
            path.close();

            Paint fillPaint = new Paint(paint);
            fillPaint.setStyle(Paint.Style.FILL);
            canvas.drawPath(path, fillPaint);
        }
    }
}