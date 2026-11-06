package com.roman.version01;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class MainActivity extends Activity {

    private EditText etVoltage, etCurrent, etAngle, etResistance, etInductance;
    private TextView tvResults;
    private VectorCanvasView vectorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#030712"));
        root.setPadding(20, 20, 20, 20);

        ScrollView scroll = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        // Заголовок
        TextView title = new TextView(this);
        title.setText("⚡ РЗА Анализатор и Векторная Диаграмма");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(16f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 15);
        container.addView(title);

        // Поля ввода
        etVoltage = createInput("Напряжение U (В), например: 220", "220");
        container.addView(etVoltage);

        etCurrent = createInput("Ток I (А), например: 10", "10");
        container.addView(etCurrent);

        etAngle = createInput("Угол φ (° от -360 до 360), например: 30", "30");
        container.addView(etAngle);

        etResistance = createInput("Активное сопротивление R (Ом, опционально)", "");
        container.addView(etResistance);

        etInductance = createInput("Индуктивность L (мГн, опционально)", "");
        container.addView(etInductance);

        // Кнопка расчета
        Button btnCalculate = new Button(this);
        btnCalculate.setText("🔄 Рассчитать и построить диаграмму");
        btnCalculate.setTextColor(Color.WHITE);
        btnCalculate.setBackgroundColor(Color.parseColor("#1D4ED8"));
        btnCalculate.setPadding(15, 15, 15, 15);
        btnCalculate.setOnClickListener(v -> performCalculation());
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnParams.setMargins(0, 10, 0, 15);
        btnCalculate.setLayoutParams(btnParams);
        container.addView(btnCalculate);

        // Текстовый блок результатов
        tvResults = new TextView(this);
        tvResults.setText("Результаты расчета появятся здесь...");
        tvResults.setTextColor(Color.parseColor("#10B981"));
        tvResults.setTextSize(12f);
        tvResults.setPadding(15, 15, 15, 15);
        tvResults.setBackgroundColor(Color.parseColor("#0F172A"));
        container.addView(tvResults);

        // Графическое поле для векторной диаграммы
        vectorView = new VectorCanvasView(this);
        LinearLayout.LayoutParams viewParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 700
        );
        viewParams.setMargins(0, 15, 0, 0);
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
        et.setTextSize(12f);
        et.setPadding(12, 12, 12, 12);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        p.setMargins(0, 0, 0, 8);
        et.setLayoutParams(p);
        return et;
    }

    private void performCalculation() {
        try {
            double u = Double.parseDouble(etVoltage.getText().toString());
            double i = Double.parseDouble(etCurrent.getText().toString());
            double phi = Double.parseDouble(etAngle.getText().toString());

            String rStr = etResistance.getText().toString();
            String lStr = etInductance.getText().toString();

            // Если заданы R и L, пересчитываем угол через сопротивления (частота f = 50 Гц)
            if (!rStr.isEmpty() && !lStr.isEmpty()) {
                double r = Double.parseDouble(rStr);
                double lMh = Double.parseDouble(lStr);
                double omega = 2 * Math.PI * 50; // 50 Гц
                double xl = omega * (lMh / 1000.0); // Ом
                if (r > 0) {
                    phi = Math.toDegrees(Math.atan(xl / r));
                }
            }

            // Нормализуем угол в диапазон [-180, 180] для удобства анализа
            double normPhi = phi % 360;
            if (normPhi > 180) normPhi -= 360;
            if (normPhi < -180) normPhi += 360;

            double phiRad = Math.toRadians(normPhi);
            double cosPhi = Math.cos(phiRad);
            double sinPhi = Math.sin(phiRad);

            double p = u * i * cosPhi; // Активная мощность (Вт)
            double q = u * i * sinPhi; // Реактивная мощность (ВАр)
            double s = Math.sqrt(p * p + q * q); // Полная мощность (ВА)

            // Определение характера нагрузки
            String character;
            if (Math.abs(normPhi) < 2.0) {
                character = "Чисто активная нагрузка";
            } else if (normPhi > 0) {
                character = "Индуктивно-активная (ток отстает от напряжения)";
            } else {
                character = "Емкостно-активная (ток опережает напряжение)";
            }

            String direction = (p >= 0) ? "Потребление мощности (нагрузка)" : "Генерация мощности (источник)";

            String report = String.format(
                "📊 АНАЛИЗ РЕЖИМА РЗА:\n" +
                "• Напряжение U: %.1f В\n" +
                "• Ток I: %.1f А\n" +
                "• Угол φ: %.2f° (норм: %.2f°)\n" +
                "• Коэффициент мощности (cos φ): %.3f\n" +
                "• Активная мощность P: %.2f Вт (%.3f кВт)\n" +
                "• Реактивная мощность Q: %.2f ВАр\n" +
                "• Полная мощность S: %.2f ВА\n" +
                "• Характер: %s\n" +
                "• Направление: %s",
                u, i, phi, normPhi, cosPhi, p, (p / 1000.0), q, s, character, direction
            );

            tvResults.setText(report);

            // Передаем данные в отрисовку векторов
            vectorView.updateVectors(normPhi, u, i);

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка в данных: проверьте числа", Toast.LENGTH_SHORT).show();
        }
    }

    // Кастомный компонент для отрисовки векторной диаграммы
    private static class VectorCanvasView extends View {
        private double angleDeg = 30;
        private double voltage = 220;
        private double current = 10;
        
        private Paint paintU, paintI, paintGrid, paintText;

        public VectorCanvasView(Context context) {
            super(context);
            init();
        }

        private void init() {
            paintU = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintU.setColor(Color.parseColor("#EF4444")); // Красный для U
            paintU.setStrokeWidth(6f);
            paintU.setStyle(Paint.Style.STROKE);

            paintI = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintI.setColor(Color.parseColor("#38BDF8")); // Голубой для I
            paintI.setStrokeWidth(6f);
            paintI.setStyle(Paint.Style.STROKE);

            paintGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintGrid.setColor(Color.parseColor("#334155"));
            paintGrid.setStrokeWidth(2f);

            paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintText.setColor(Color.parseColor("#F8FAFC"));
            paintText.setTextSize(28f);
        }

        public void updateVectors(double angle, double u, double i) {
            this.angleDeg = angle;
            this.voltage = u;
            this.current = i;
            invalidate(); // Перерисовка холста
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(Color.parseColor("#090D16"));

            int width = getWidth();
            int height = getHeight();
            float cx = width / 2f;
            float cy = height / 2f;
            float radius = Math.min(cx, cy) - 60f;

            // Осевые линии (сетка)
            canvas.drawCircle(cx, cy, radius, paintGrid);
            canvas.drawLine(cx - radius - 20, cy, cx + radius + 20, cy, paintGrid);
            canvas.drawLine(cx, cy - radius - 20, cx, cy + radius + 20, paintGrid);

            // Вектор напряжения U всегда направлен по горизонтали вправо (0 градусов)
            float endUX = cx + radius * 0.85f;
            float endUY = cy;
            drawArrow(canvas, cx, cy, endUX, endUY, paintU);
            canvas.drawText("U (0°)", endUX + 10, endUY - 10, paintText);

            // Вектор тока I сдвинут на угол angleDeg
            // В электротехнике положительный угол (индуктивность) означает отставание тока (по часовой стрелке в экранных координатах, либо против — зависит от принятого построения; здесь откладываем математически: угол минус для отставания или плюс)
            double rad = Math.toRadians(-angleDeg); // инверсия для экрана Android (где оси Y направлены вниз)
            float endIX = (float) (cx + radius * 0.75f * Math.cos(rad));
            float endIY = (float) (cy + radius * 0.75f * Math.sin(rad));
            
            drawArrow(canvas, cx, cy, endIX, endIY, paintI);
            canvas.drawText(String.format("I (%.1f°)", angleDeg), endIX + 10, endIY + 10, paintText);
        }

        private void drawArrow(Canvas canvas, float startX, float startY, float endX, float endY, Paint paint) {
            canvas.drawLine(startX, startY, endX, endY, paint);
            // Рисуем наконечник стрелки
            float angle = (float) Math.atan2(endY - startY, endX - startX);
            float arrowLen = 25f;
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