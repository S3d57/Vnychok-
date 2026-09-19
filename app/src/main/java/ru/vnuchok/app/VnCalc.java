package ru.vnuchok.app;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;
/* КАЛЬКУЛЯТОР: крупные кнопки, четыре действия, равно, сброс,Backspace. */
public class VnCalc {
    public static LinearLayout build() {
        LinearLayout c = VnUi.col();
        final TextView disp = VnUi.tv("0", 40, VnTheme.GREEN, true);
        disp.setGravity(Gravity.END);
        disp.setMinHeight(VnUi.dp(70));
        c.addView(disp, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        final StringBuilder cur = new StringBuilder();
        final double[] acc = {0};
        final char[] op = {0};
        final boolean[] fresh = {true};

        String[] keys = {"7", "8", "9", "÷", "4", "5", "6", "×", "1", "2", "3", "−", "0", ".", "=", "+"};
        for (int r = 0; r < 4; r++) {
            LinearLayout rw = VnUi.row();
            for (int k = 0; k < 4; k++) {
                final String key = keys[r * 4 + k];
                Button b = VnUi.big(key, VnTheme.MUSTARD, VnTheme.BROWN, v -> {
                    if (key.equals("=")) {
                        if (op[0] != 0) { acc[0] = apply(acc[0], parse(cur), op[0]); op[0] = 0; }
                        else acc[0] = parse(cur);
                        cur.setLength(0); cur.append(fmt(acc[0]));
                        fresh[0] = true;
                    } else if (key.equals("+") || key.equals("−") || key.equals("×") || key.equals("÷")) {
                        if (op[0] != 0 && !fresh[0]) { acc[0] = apply(acc[0], parse(cur), op[0]); }
                        else if (fresh[0] && cur.length() == 0) { acc[0] = 0; }
                        else acc[0] = parse(cur);
                        op[0] = key.charAt(0);
                        cur.setLength(0);
                        fresh[0] = true;
                    } else {
                        if (fresh[0]) { cur.setLength(0); fresh[0] = false; }
                        if (!(key.equals(".") && cur.toString().contains("."))) cur.append(key);
                    }
                    disp.setText(cur.length() == 0 ? "0" : cur.toString());
                });
                b.setTextSize(26 * VnUi.FS * VnUi.SC);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                lp.setMargins(VnUi.dp(3), VnUi.dp(3), VnUi.dp(3), VnUi.dp(3));
                b.setLayoutParams(lp);
                rw.addView(b);
            }
            c.addView(rw);
        }
        LinearLayout rw = VnUi.row();
        Button clr = VnUi.big("C", VnTheme.RUST, VnTheme.CREAM, v -> { cur.setLength(0); acc[0] = 0; op[0] = 0; fresh[0] = true; disp.setText("0"); });
        Button bsk = VnUi.big("⌫", VnTheme.GRAY, VnTheme.CREAM, v -> { if (cur.length() > 0) cur.deleteCharAt(cur.length() - 1); disp.setText(cur.length() == 0 ? "0" : cur.toString()); });
        clr.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        bsk.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        rw.addView(clr); rw.addView(bsk);
        c.addView(rw);
        return c;
    }

    private static double parse(StringBuilder sb) {
        try { return Double.parseDouble(sb.toString()); } catch (Exception e) { return 0; }
    }
    private static String fmt(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
        return String.valueOf(d);
    }
    private static double apply(double a, double b, char op) {
        switch (op) {
            case '+': return a + b;
            case '−': return a - b;
            case '×': return a * b;
            case '÷': return b == 0 ? 0 : a / b;
            default: return b;
        }
    }
}
