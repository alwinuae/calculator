package com.countingapp.calculator;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvExpression, tvResult;
    private String expression = "";
    private boolean lastIsOperator = false;
    private boolean lastIsEquals = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvExpression = findViewById(R.id.tvExpression);
        tvResult = findViewById(R.id.tvResult);

        setupNumberButtons();
        setupOperatorButtons();
        setupSpecialButtons();
    }

    private void setupNumberButtons() {
        int[] numIds = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        };

        for (int id : numIds) {
            findViewById(id).setOnClickListener(v -> {
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
            String op = ((Button) v).getText().toString();

            // Convert display symbols to actual operators for evaluation
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
            expression = "";
            lastIsOperator = false;
            lastIsEquals = false;
            tvExpression.setText("");
            tvResult.setText("0");
        });

        // Decimal point
        findViewById(R.id.btnDot).setOnClickListener(v -> {
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
            expression = toggleSign(expression);
            updateDisplay();
        });

        // Percent
        findViewById(R.id.btnPercent).setOnClickListener(v -> {
            if (expression.isEmpty()) return;
            try {
                double val = evaluateExpression(expression);
                val = val / 100.0;
                expression = formatResult(val);
                updateDisplay();
            } catch (Exception ignored) {}
        });

        // Parentheses
        findViewById(R.id.btnParens).setOnClickListener(v -> {
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
            try {
                // Close any open parentheses
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
        // Try to show live result
        try {
            String expr = expression;
            if (lastIsOperator) {
                expr = expr.substring(0, expr.length() - 1);
            }
            // Close open parentheses for evaluation
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
        // Find the last number and toggle its sign
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

    // Simple expression evaluator supporting +, -, ×, ÷, and parentheses
    private double evaluateExpression(String expr) {
        // Replace display symbols with standard operators
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
                pos++; // skip '('
                double result = parseAddSub();
                if (pos < expr.length() && expr.charAt(pos) == ')') {
                    pos++; // skip ')'
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
}
