package com.roman.version01;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

public class MainActivity extends Activity {

    private RadioGroup radioGroupMode;
    private EditText etUA, etIA, etValA, etUB, etIB, etValB, etUC, etIC, etValC;
    private TextView tvLabelValA, tvLabelValB, tvLabelValC;
    private Spinner spinnerRLCA, spinnerRLCB, spinnerRLCC;
    private TextView tvResults;
    private VectorCanvasView vectorView;

    private boolean isUpdating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#030712"));
        root.setPadding(12, 12, 12, 12);

        ScrollView scroll = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        // Заголовок
        TextView title = new TextView(this);
        title.setText("⚡ РЗА: Трехфазный Анализатор");
        title.setTextColor(Color.parseColor("#38BDF8"));
        title.setTextSize(15f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 10);
        container.addView(title);

        // Переключатель режимов
        TextView lblMode = new TextView(this);
        lblMode.setText("⚙️ Выберите режим ввода данных:");
        lblMode.setTextColor(Color.parseColor("#F1F5F9"));
        lblMode.setTextSize(12f);
        container.addView(lblMode);

        radioGroupMode = new RadioGroup(this);
        radioGroupMode.setOrientation(RadioGroup.HORIZONTAL);
        
        RadioButton rbRLC = new RadioButton(this);
        rbRLC.setId(101);
        rbRLC.setText("1. Прибор R / L / C");
        rbRLC.setTextColor(Color.parseColor("#FFFFFF"));
        rbRLC.setChecked(true);
        radioGroupMode.addView(rbRLC);

        RadioButton rbCircle = new RadioButton(this);
        rbCircle.setId(102);
        rbCircle.setText("2. Круговая (0-360°)");
        rbCircle.setTextColor(Color.parseColor("#FFFFFF"));
        radioGroupMode.addView(rbCircle);

        container.addView(radioGroupMode);

        // Карточка ФАЗА А (Желтый)
        LinearLayout cardA = createPhaseCard("🟡 Фаза A (Желтый | Опорная 0°)", "#F59E0B");
        addInputField(cardA, "Напряжение U_A (В):");
        etUA = addEditText("220"); cardA.addView(etUA);
        addInputField(cardA, "Ток I_A (А):");
        etIA = addEditText("10"); cardA.addView(etIA);
        tvLabelValA = addInputField(cardA, "Угол сдвига φ (°):");
        etValA = addEditText("30"); cardA.addView(etValA);
        spinnerRLCA = addRLCSpinner(cardA, 0);
        container.addView(cardA);

        // Карточка ФАЗА В (Зеленый)
        LinearLayout cardB = createPhaseCard("🟢 Фаза B (Зеленый | Опорная -120°)", "#10B981");
        addInputField(cardB, "Напряжение U_B (В):");
        etUB = addEditText("220"); cardB.addView(etUB);
        addInputField(cardB, "Ток I_B (А):");
        etIB = addEditText("10"); cardB.addView(etIB);
        tvLabelValB = addInputField(cardB, "Угол сдвига φ (°):");
        etValB = addEditText("30"); cardB.addView(etValB);
        spinnerRLCB = addRLCSpinner(cardB, 1);
        container.addView(cardB);

        // Карточка ФАЗА С (Красный)
        LinearLayout cardC = createPhaseCard("🔴 Фаза C (Красный | Опорная +120°)", "#EF4444");
        addInputField(cardC, "Напряжение U_C (В):");
        etUC = addEditText("220"); cardC.addView(etUC);
        addInputField(cardC, "Ток I_C (А):");
        etIC = addEditText("10"); cardC.addView(etIC);
        tvLabelValC = addInputField(cardC, "Угол сдвига φ (°):");
        etValC = addEditText("30"); cardC.addView(etValC);
        spinnerRLCC = addRLCSpinner(cardC, 2);
        container.addView(cardC);

        // Обработка переключения режимов
        radioGroupMode.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isRLC = (checkedId == 101);
            spinnerRLCA.setVisibility(isRLC ? View.VISIBLE : View.GONE);
            spinnerRLCB.setVisibility(isRLC ? View.VISIBLE : View.GONE);
            spinnerRLCC.setVisibility(isRLC ? View.VISIBLE : View.GONE);

            String labelText = isRLC ? "Угол сдвига φ (°):" : "Угол по круговой шкале (0-360°):";
            tvLabelValA.setText(labelText);
            tvLabelValB.setText(labelText);
            tvLabelValC.setText(labelText);

            calculateAndDraw();
        });

        // Кнопка расчета
        Button btnCalculate = new Button(this);
        btnCalculate.setText("🔄 Рассчитать все фазы");
        btnCalculate.setTextColor(Color.WHITE);
        btnCalculate.setBackgroundColor(Color.parseColor("#1D4ED8"));
        btnCalculate.setPadding(12, 12, 12, 12);
        btnCalculate.setOnClickListener(v -> calculateAndDraw());
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        btnParams.setMargins(0, 8, 0, 8);
        btnCalculate.setLayoutParams(btnParams);
        container.addView(btnCalculate);

        // Текстовый вывод результатов
        tvResults = new TextView(this);
        tvResults.setText("Сводные результаты расчета...");
        tvResults.setTextColor(Color.parseColor("#10B981"));
        tvResults.setTextSize(11f);
        tvResults.setPadding(12, 12, 12, 12);
        tvResults.setBackgroundColor(Color.parseColor("#0F172A"));
        container.addView(tvResults);

        // График диаграммы
        vectorView = new VectorCanvasView(this);
        LinearLayout.LayoutParams viewParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 750
        );
        viewParams.setMargins(0, 10, 0, 0);
        vectorView.setLayoutParams(viewParams);
        container.addView(vectorView);

        scroll.addView(container);
        root.addView(scroll);
        setContentView(root);

        setupDynamicListeners();
        calculateAndDraw();
    }

    private LinearLayout createPhaseCard(String title, String borderColorHex) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(14, 14, 14, 14);
        
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.parseColor("#0F172A"));
        bg.setCornerRadius(16f);
        bg.setStroke(2, Color.parseColor(borderColorHex));
        card.setBackground(bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 10);
        card.setLayoutParams(params);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(Color.parseColor(borderColorHex));
        tvTitle.setTextSize(13f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setPadding(0, 0, 0, 6);
        card.addView(tvTitle);

        return card;
    }

    private TextView addInputField(LinearLayout parent, String labelText) {
        TextView tv = new TextView(this);
        tv.setText(labelText);
        tv.setTextColor(Color.parseColor("#94A3B8"));
        tv.setTextSize(11f);
        tv.setPadding(0, 4, 0, 2);
        parent.addView(tv);
        return tv;
    }

    private EditText addEditText(String defaultVal) {
        EditText et = new EditText(this);
        et.setText(defaultVal);
        et.setTextColor(Color.WHITE);
        et.setTextSize(11f);
        et.setPadding(10, 8, 10, 8);
        et.setBackgroundColor(Color.parseColor("#1E293B"));
        
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        p.setMargins(0, 0, 0, 4);
        et.setLayoutParams(p);
        return et;
    }

    private Spinner addRLCSpinner(LinearLayout parent, int defaultIdx) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{
            "Характер L (Индуктивный / Ток отстает)",
            "Характер R (Активный / Совпадение)",
            "Характер C (Емкостной / Ток опережает)"
        }) {
            @Override
            public View getView(int position, View convertView, ViewGroup parentView) {
                View v = super.getView(position, convertView, parentView);
                ((TextView) v).setTextColor(Color.WHITE);
                ((TextView) v).setTextSize(11f);
                return v;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parentView) {
                View v = super.getView(position, convertView, parentView);
                ((TextView) v).setTextColor(Color.BLACK);
                ((TextView) v).setTextSize(11f);
                return v;
            }
        };
        spinner.setAdapter(adapter);
        spinner.setSelection(defaultIdx);
        
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        p.setMargins(0, 0, 0, 6);
        spinner.setLayoutParams(p);
        parent.addView(spinner);
        return spinner;
    }

    private void setupDynamicListeners() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (!isUpdating) {
                    calculateAndDraw();
                }
            }
        };

        etUA.addTextChangedListener(watcher);
        etIA.addTextChangedListener(watcher);
        etValA.addTextChangedListener(watcher);
        etUB.addTextChangedListener(watcher);
        etIB.addTextChangedListener(watcher);
        etValB.addTextChangedListener(watcher);
        etUC.addTextChangedListener(watcher);
        etIC.addTextChangedListener(watcher);
        etValC.addTextChangedListener(watcher);
    }

    private double parsePhaseData(EditText etVal, Spinner spinner, boolean isRLCMode, double[] outShift) {
        try {
            double val = Double.parseDouble(etVal.getText().toString());
            double shift = 0.0;
            if (isRLCMode) {
                int type = spinner.getSelectedItemPosition(); 
                if (type == 0) shift = Math.abs(val);       // L
                else if (type == 1) shift = 0.0;            // R
                else shift = -Math.abs(val);                 // C
            } else {
                double circle = val % 360;
                if (circle < 0) circle += 360;
                if (circle <= 1.0 || circle >= 359.0) shift = 0.0;
                else if (circle < 180.0) shift = circle;
                else shift = circle - 360.0;
            }
            outShift[0] = shift;
            return val;
        } catch (Exception e) {
            outShift[0] = 0.0;
            return 0.0;
        }
    }

    private void calculateAndDraw() {
        isUpdating = true;
        try {
            boolean isRLCMode = (radioGroupMode.getCheckedRadioButtonId() == 101);

            double uA = Double.parseDouble(etUA.getText().toString());
            double iA = Double.parseDouble(etIA.getText().toString());
            double[] shiftA = new double[1];
            parsePhaseData(etValA, spinnerRLCA, isRLCMode, shiftA);

            double uB = Double.parseDouble(etUB.getText().toString());
            double iB = Double.parseDouble(etIB.getText().toString());
            double[] shiftB = new double[1];
            parsePhaseData(etValB, spinnerRLCB, isRLCMode, shiftB);

            double uC = Double.parseDouble(etUC.getText().toString());
            double iC = Double.parseDouble(etIC.getText().toString());
            double[] shiftC = new double[1];
            parsePhaseData(etValC, spinnerRLCC, isRLCMode, shiftC);

            double pA = uA * iA * Math.cos(Math.toRadians(shiftA[0]));
            double pB = uB * iB * Math.cos(Math.toRadians(shiftB[0]));
            double pC = uC * iC * Math.cos(Math.toRadians(shiftC[0]));
            double pSum = pA + pB + pC;

            String report = String.format(
                "📊 СВОДНЫЙ АНАЛИЗ (Желтый-A, Зеленый-B, Красный-C):\n" +
                "• Фаза A (Желтый): U=%.1fV, I=%.1fA, φ=%.1f° | P = %.1f Вт\n" +
                "• Фаза B (Зеленый): U=%.1fV, I=%.1fA, φ=%.1f° | P = %.1f Вт\n" +
                "• Фаза C (Красный): U=%.1fV, I=%.1fA, φ=%.1f° | P = %.1f Вт\n" +
                "──────────────────────────────\n" +
                "⚡ Суммарная активная мощность P_sum = %.2f кВт",
                uA, iA, shiftA[0], pA,
                uB, iB, shiftB[0], pB,
                uC, iC, shiftC[0], pC,
                (pSum / 1000.0)
            );

            tvResults.setText(report);
            vectorView.updateData(0.0, shiftA[0], -120.0, shiftB[0], 120.0, shiftC[0]);

        } catch (Exception ignored) {
        } finally {
            isUpdating = false;
        }
    }

    // Компонент векторной диаграммы
    private static class VectorCanvasView extends View {
        private double vA = 0, iA = 30;
        private double vB = -120, iB = -90;
        private double vC = 120, iC = 150;
        
        private Paint paintUA, paintIA, paintUB, paintIB, paintUC, paintIC, paintGrid, paintText;

        public VectorCanvasView(Context context) {
            super(context);
            init();
        }

        private void init() {
            paintGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintGrid.setColor(Color.parseColor("#334155"));
            paintGrid.setStrokeWidth(2f);

            paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintText.setColor(Color.parseColor("#F8FAFC"));
            paintText.setTextSize(22f);

            // Фаза А — ЖЕЛТЫЙ
            paintUA = createVectorPaint(Color.parseColor("#F59E0B")); 
            paintIA = createVectorPaint(Color.parseColor("#FDE68A")); 

            // Фаза В — ЗЕЛЕНЫЙ
            paintUB = createVectorPaint(Color.parseColor("#10B981")); 
            paintIB = createVectorPaint(Color.parseColor("#A7F3D0")); 

            // Фаза С — КРАСНЫЙ
            paintUC = createVectorPaint(Color.parseColor("#EF4444")); 
            paintIC = createVectorPaint(Color.parseColor("#FCA5A5")); 
        }

        private Paint createVectorPaint(int color) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(color);
            p.setStrokeWidth(4f);
            p.setStyle(Paint.Style.STROKE);
            return p;
        }

        public void updateData(double voltageA, double shiftA, double voltageB, double shiftB, double voltageC, double shiftC) {
            this.vA = voltageA; this.iA = voltageA + shiftA;
            this.vB = voltageB; this.iB = voltageB + shiftB;
            this.vC = voltageC; this.iC = voltageC + shiftC;
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
            float radius = Math.min(cx, cy) - 45f;

            canvas.drawCircle(cx, cy, radius, paintGrid);
            // Исправленные аргументы drawLine (startX, startY, stopX, stopY, paint)
            canvas.drawLine(cx - radius - 10, cy, cx + radius + 10, cy, paintGrid);
            canvas.drawLine(cx, cy - radius - 10, cx, cy + radius + 10, paintGrid);

            drawVector(canvas, cx, cy, radius * 0.75f, vA, "U_A", paintUA);
            drawVector(canvas, cx, cy, radius * 0.55f, iA, "I_A", paintIA);

            drawVector(canvas, cx, cy, radius * 0.75f, vB, "U_B", paintUB);
            drawVector(canvas, cx, cy, radius * 0.55f, iB, "I_B", paintIB);

            drawVector(canvas, cx, cy, radius * 0.75f, vC, "U_C", paintUC);
            drawVector(canvas, cx, cy, radius * 0.55f, iC, "I_C", paintIC);
        }

        private void drawVector(Canvas canvas, float cx, float cy, float len, double angleDeg, String label, Paint paint) {
            double rad = Math.toRadians(-angleDeg);
            float endX = (float) (cx + len * Math.cos(rad));
            float endY = (float) (cy + len * Math.sin(rad));

            canvas.drawLine(cx, cy, endX, endY, paint);

            float angle = (float) Math.atan2(endY - cy, endX - cx);
            float arrowLen = 15f;
            float p1x = (float) (endX - arrowLen * Math.cos(angle - Math.PI / 6));
            float p1y = (float) (endY - arrowLen * Math.sin(angle - Math.PI / 6));
            float p2x = (float) (endX - arrowLen * Math.cos(angle + Math.PI / 6));
            float p2y = (float) (endY - arrowLen * Math.sin(angle + Math.PI / 6));

            Path path = new Path();
            path.moveTo(endX, endY);
            path.lineTo(p1x, p1y);
            path.lineTo(p2x, p2y);
            path.close();

            Paint fill = new Paint(paint);
            fill.setStyle(Paint.Style.FILL);
            canvas.drawPath(path, fill);

            canvas.drawText(label, endX + 6, endY + 4, paintText);
        }
    }
}