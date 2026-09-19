package ru.vnuchok.app;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Calendar;
/* БУДИЛЬНИК: время кнопками +/−, дни недели, «сработает через», список с удалением. */
public class VnAlarm {
    private static int mask = 0;
    private static final String[] DN = {"MO", "TU", "WE", "TH", "FR", "SA", "SU"};

    public static LinearLayout build() {
        LinearLayout c = VnUi.col();
        final int[] hv = {7}; final int[] mv = {0};
        addTimeRow(c, "HOURS", hv, 24, 1);
        addTimeRow(c, "MINUTES", mv, 60, 5);
        c.addView(VnUi.tv("Days of week:", 16, VnTheme.GREEN, true));
        LinearLayout days = VnUi.row();
        for (int i = 0; i < 7; i++) {
            final int bit = i;
            boolean on = (mask & (1 << bit)) != 0;
            Button db = VnUi.big(DN[i], on ? 0xFF3FAE4C : VnTheme.MUSTARD, on ? VnTheme.CREAM : VnTheme.BROWN, v -> {
                mask ^= (1 << bit);
                boolean nowOn = (mask & (1 << bit)) != 0;
                int col = nowOn ? 0xFF3FAE4C : VnTheme.MUSTARD;
                v.setBackground(VnUi.pill(col));
                VnUi.holder(v).bg = VnTheme.hex(col);
                ((Button) v).setTextColor(nowOn ? VnTheme.CREAM : VnTheme.BROWN);
            });
            db.setTextSize(14 * VnUi.FS * VnUi.SC);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, VnUi.dp(58), 1f);
            lp.setMargins(VnUi.dp(2), 0, VnUi.dp(2), 0);
            db.setLayoutParams(lp);
            days.addView(db);
        }
        VnUi.addSpaced(c, days, 6);
        c.addView(VnUi.tv("(nothing pressed = every day)", 13, VnTheme.BROWN, false));
        VnUi.addSpaced(c, VnUi.bigI("alarm", "SET ALARM", 0xFF3FAE4C, VnTheme.CREAM, v -> {
            long ms = nextAlarm(hv[0], mv[0], mask);
            String extra = hv[0] + ":" + mv[0] + ":" + mask;
            VnRemind.schedule(ms, "Alarm! Time to get up!", "alm", extra);
            VnHome.say("Alarm set. " + VnRemind.until(ms));
            VnHome.router.show(build(), "Alarm");
        }), 10);
        c.addView(VnUi.tv("Already set:", 16, VnTheme.BROWN, true));
        VnRemind.addList(c, "alm", "Alarm");
        return c;
    }

    public static long nextAlarm(int h, int m, int mk) {
        Calendar now = Calendar.getInstance();
        for (int d = 0; d < 8; d++) {
            Calendar c = (Calendar) now.clone();
            c.add(Calendar.DAY_OF_YEAR, d);
            c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, m); c.set(Calendar.SECOND, 0);
            if (c.before(now)) continue;
            int wd = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
            if (mk == 0 || (mk & (1 << wd)) != 0) return c.getTimeInMillis();
        }
        return 0;
    }

    static void addTimeRow(LinearLayout c, String label, final int[] val, int mod, int step) {
        LinearLayout r = VnUi.row();
        r.setGravity(Gravity.CENTER);
        TextView lt = VnUi.tv(label, 16, VnTheme.GREEN, true);
        lt.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(lt);
        Button minus = VnUi.big("−", VnTheme.MUSTARD, VnTheme.BROWN, v -> { val[0] = (val[0] - step + mod) % mod; ((TextView) r.getChildAt(2)).setText(String.format(java.util.Locale.getDefault(), "%02d", val[0])); });
        minus.setTextSize(28 * VnUi.FS * VnUi.SC);
        minus.setLayoutParams(new LinearLayout.LayoutParams(VnUi.dp(84), LinearLayout.LayoutParams.WRAP_CONTENT));
        final TextView disp = VnUi.tv(String.format(java.util.Locale.getDefault(), "%02d", val[0]), 34, VnTheme.GREEN, true);
        disp.setGravity(Gravity.CENTER);
        disp.setMinWidth(VnUi.dp(110));
        Button plus = VnUi.big("+", VnTheme.MUSTARD, VnTheme.BROWN, v -> { val[0] = (val[0] + step) % mod; ((TextView) r.getChildAt(2)).setText(String.format(java.util.Locale.getDefault(), "%02d", val[0])); });
        plus.setTextSize(28 * VnUi.FS * VnUi.SC);
        plus.setLayoutParams(new LinearLayout.LayoutParams(VnUi.dp(84), LinearLayout.LayoutParams.WRAP_CONTENT));
        r.addView(minus); r.addView(disp); r.addView(plus);
        VnUi.addSpaced(c, r, 8);
    }

    /* Приёмник: повторяющиеся будильники переставляет сам и будит главный экран */
    public static class Receiver extends BroadcastReceiver {
        @Override public void onReceive(Context c, Intent i) {
            String text = i.getStringExtra("text");
            if (i.hasExtra("ah")) {
                int h = i.getIntExtra("ah", 7), m = i.getIntExtra("am", 0), mk = i.getIntExtra("mask", 0);
                long next = nextAlarm(h, m, mk);
                if (next > 0) {
                    android.app.AlarmManager am = (android.app.AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
                    Intent ni = new Intent(c, Receiver.class);
                    ni.putExtra("text", text); ni.putExtra("ah", h); ni.putExtra("am", m); ni.putExtra("mask", mk);
                    int code = (int) (next % 1000000);
                    android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(c, code, ni, android.app.PendingIntent.FLAG_IMMUTABLE);
                    am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, next, pi);
                }
            }
            Intent in = new Intent(c, MainActivity.class);
            in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            in.putExtra("reminder", text);
            c.startActivity(in);
        }
    }
}
