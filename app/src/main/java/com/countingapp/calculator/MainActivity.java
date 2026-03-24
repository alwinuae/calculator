package com.countingapp.calculator;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvExpression, tvResult;
    private String expression = "";
    private boolean lastIsOperator = false;
    private boolean lastIsEquals = false;

    private Vibrator vibrator;
    private SoundPool soundPool;
    private int soundClick;
    private int soundEquals;
    private int soundClear;
    private boolean soundsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvExpression = findViewById(R.id.tvExpression);
        tvResult = findViewById(R.id.tvResult);

        initVibrator();
        initSoundPool();

        setupNumberButtons();
        setupOperatorButtons();
        setupSpecialButtons();
    }

    private void initVibrator() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vm.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    private void initSoundPool() {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(attrs)
                .build();
        soundPool.setOnLoadCompleteListener((sp, id, status) -> {
            if (status == 0) soundsLoaded = true;
        });
        soundClick = soundPool.load(this, R.raw.click, 1);
        soundEquals = soundPool.load(this, R.raw.equals_sound, 1);
        soundClear = soundPool.load(this, R.raw.clear, 1);
    }

    private void performHaptic(int type) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        switch (type) {
            case 0: // light tap for numbers
                vibrator.vibrate(VibrationEffect.createOneShot(20, 80));
                break;
            case 1: // medium tap for operators
                vibrator.vibrate(VibrationEffect.createOneShot(30, 120));
                break;
            case 2: // strong tap for equals
                vibrator.vibrate(VibrationEffect.createOneShot(40, 180));
                break;
            case 3: // double tap for clear
                long[] pattern = {0, 25, 50, 25};
                int[] amplitudes = {0, 120, 0, 120};
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1));
                break;
        }
    }

    private void playSound(int soundId) {
        if (soundsLoaded) {
            soundPool.play(soundId, 0.3f, 0.3f, 1, 0, 1.0f);
        }
    }

    private void onButtonPress(int hapticType, int soundId) {
        performHaptic(hapticType);
        playSound(soundId);
    }

    private void setupNumberButtons() {
        int[] numIds = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        };

        for (int id : numIds) {
            findViewById(id).setOnClickListener(v -> {
                onButtonPress(0, soundClick);
                if (lastIsEquals) {
                    expression = "";
                    lastIsEquals = false;
                }
                String num = ((Button) v).getText().toString();
                expression += num;
                lastIsOperator = false;
                updateDisplay();
            });
        }
    }

    private void setupOperatorButtons() {
        View.OnClickListener opListener = v -> {
            if (expression.isEmpty()) return;
            onButtonPress(1, soundClick);
            String op = ((Button) v).getText().toString();

            if (lastIsOperator) {
                expression = expression.substring(0, expression.length() - 1);
            }

            lastIsEquals = false;
            expression += op;
            lastIsOperator = true;
            updateDisplay();
        };

        findViewById(R.id.btnPlus).setOnClickListener(opListener);
        findViewById(R.id.btnMinus).setOnClickListener(opListener);
        findViewById(R.id.btnMultiply).setOnClickListener(opListener);
        findViewById(R.id.btnDivide).setOnClickListener(opListener);
    }

    private void setupSpecialButtons() {
        // Clear
        findViewById(R.id.btnClear).setOnClickListener(v -> {
            onButtonPress(3, soundClear);
            expression = "";
            lastIsOperator = false;
            lastIsEquals = false;
            tvExpression.setText("");
            tvResult.setText("0");
        });

        // Decimal point
        findViewById(R.id.btnDot).setOnClickListener(v -> {
            onButtonPress(0, soundClick);
            if (lastIsEquals) {
                expression = "0";
                lastIsEquals = false;
            }
            String lastNumber = getLastNumber();
            if (!lastNumber.contains(".")) {
                if (lastNumber.isEmpty() || lastIsOperator) {
                    expression += "0";
                }
                expression += ".";
                lastIsOperator = false;
                updateDisplay();
            }
        });

        // Plus/Minus toggle
        findViewById(R.id.btnPlusMinus).setOnClickListener(v -> {
            if (expression.isEmpty()) return;
            onButtonPress(1, soundClick);
            expression = toggleSign(expression);
            updateDisplay();
        });

        // Percent
        findViewById(R.id.btnPercent).setOnClickListener(v -> {
            if (expression.isEmpty()) return;
            onButtonPress(1, soundClick);
            try {
                double val = evaluateExpression(expression);
                val = val / 100.0;
                expression = formatResult(val);
                updateDisplay();
            } catch (Exception ignored) {}
        });

        // Parentheses
        findViewById(R.id.btnParens).setOnClickListener(v -> {
            onButtonPress(0, soundClick);
            if (lastIsEquals) {
                expression = "";
                lastIsEquals = false;
            }
            int openCount = countChar(expression, '(');
            int closeCount = countChar(expression, ')');
            if (expression.isEmpty() || lastIsOperator || expression.endsWith("(")) {
                expression += "(";
            } else if (openCount > closeCount) {
                expression += ")";
            } else {
                expression += "(";
            }
            lastIsOperator = false;
            updateDisplay();
        });

        // Equals
        findViewById(R.id.btnEquals).setOnClickListener(v -> {
            if (expression.isEmpty()) return;
            onButtonPress(2, soundEquals);
            try {
                int openCount = countChar(expression, '(');
                int closeCount = countChar(expression, ')');
                String expr = expression;
                for (int i = 0; i < openCount - closeCount; i++) {
                    expr += ")";
                }

                double result = evaluateExpression(expr);
                tvExpression.setText(expression + " =");
                tvResult.setText(formatResult(result));
                expression = formatResult(result);
                lastIsOperator = false;
                lastIsEquals = true;
            } catch (Exception e) {
                tvResult.setText("Error");
            }
        });
    }

    private void updateDisplay() {
        tvExpression.setText(expression);
        try {
            String expr = expression;
            if (lastIsOperator) {
                expr = expr.substring(0, expr.length() - 1);
            }
            int openCount = countChar(expr, '(');
            int closeCount = countChar(expr, ')');
            for (int i = 0; i < openCount - closeCount; i++) {
                expr += ")";
            }
            if (!expr.isEmpty()) {
                double result = evaluateExpression(expr);
                tvResult.setText(formatResult(result));
            }
        } catch (Exception ignored) {}
    }

    private String getLastNumber() {
        StringBuilder num = new StringBuilder();
        for (int i = expression.length() - 1; i >= 0; i--) {
            char c = expression.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                num.insert(0, c);
            } else {
                break;
            }
        }
        return num.toString();
    }

    private String toggleSign(String expr) {
        int i = expr.length() - 1;
        while (i >= 0 && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
            i--;
        }
        if (i >= 0 && expr.charAt(i) == '-' && (i == 0 || isOperator(expr.charAt(i - 1)))) {
            return expr.substring(0, i) + expr.substring(i + 1);
        } else {
            return expr.substring(0, i + 1) + "-" + expr.substring(i + 1);
        }
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '×' || c == '÷' || c == '(';
    }

    private int countChar(String s, char c) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) count++;
        }
        return count;
    }

    private String formatResult(double val) {
        if (val == (long) val && !Double.isInfinite(val)) {
            return String.valueOf((long) val);
        }
        return String.valueOf(val);
    }

    private double evaluateExpression(String expr) {
        expr = expr.replace("×", "*").replace("÷", "/");
        return new ExpressionParser(expr).parse();
    }

    private static class ExpressionParser {
        private final String expr;
        private int pos = 0;

        ExpressionParser(String expr) {
            this.expr = expr.replaceAll("\\s", "");
        }

        double parse() {
            double result = parseAddSub();
            if (pos < expr.length()) throw new RuntimeException("Unexpected: " + expr.charAt(pos));
            return result;
        }

        private double parseAddSub() {
            double result = parseMulDiv();
            while (pos < expr.length()) {
                char op = expr.charAt(pos);
                if (op == '+' || op == '-') {
                    pos++;
                    double right = parseMulDiv();
                    result = (op == '+') ? result + right : result - right;
                } else {
                    break;
                }
            }
            return result;
        }

        private double parseMulDiv() {
            double result = parseUnary();
            while (pos < expr.length()) {
                char op = expr.charAt(pos);
                if (op == '*' || op == '/') {
                    pos++;
                    double right = parseUnary();
                    result = (op == '*') ? result * right : result / right;
                } else {
                    break;
                }
            }
            return result;
        }

        private double parseUnary() {
            if (pos < expr.length() && expr.charAt(pos) == '-') {
                pos++;
                return -parsePrimary();
            }
            if (pos < expr.length() && expr.charAt(pos) == '+') {
                pos++;
            }
            return parsePrimary();
        }

        private double parsePrimary() {
            if (pos < expr.length() && expr.charAt(pos) == '(') {
                pos++;
                double result = parseAddSub();
                if (pos < expr.length() && expr.charAt(pos) == ')') {
                    pos++;
                }
                return result;
            }

            int start = pos;
            while (pos < expr.length() && (Character.isDigit(expr.charAt(pos)) || expr.charAt(pos) == '.')) {
                pos++;
            }
            if (start == pos) throw new RuntimeException("Unexpected end of expression");
            return Double.parseDouble(expr.substring(start, pos));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}
