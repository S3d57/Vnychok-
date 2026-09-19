package ru.vnuchok.app;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
/* НАПОМИНАНИЯ: создание (название, время, дата своим календарём), пресеты, список.
   Здесь же общее хранилище и планировщик, которым пользуется будильник. */
public class VnRemind {
    private static SharedPreferences P() { return VnUi.C.getSharedPreferences("vnuchok", Context.MODE_PRIVATE); }

    public static List<String[]> all() {
        List<String[]> out = new ArrayList<>();
        for (String line : P().getString("rems", "").split("\n")) {
            String[] p = line.split("\\|");
            if (p.length >= 4) out.add(p);
        }
        return out;
    }
    public static void save(List<String[]> r) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < r.size(); i++) { if (i > 0) sb.append("\n"); sb.append(String.join("|", r.get(i))); }
        P().edit().putString("rems", sb.toString()).apply();
    }
    public static String until(long ms) {
        long diff = ms - System.currentTimeMillis();
        if (diff < 0) return "";
        long d = diff / 86400000, h = (diff % 86400000) / 3600000, m = (diff % 3600000) / 60000;
        if (d > 0) return "in " + d + " d " + h + " h " + m + " min";
        if (h > 0) return "in " + h + " h " + m + " min";
        return "in " + m + " min";
    }
    public static int schedule(long millis, String text, String kind, String extra) {
        AlarmManager am = (AlarmManager) VnUi.C.getSystemService(Context.ALARM_SERVICE);
        Intent in = new Intent(VnUi.C, VnAlarm.Receiver.class);
        in.putExtra("text", text);
        if (extra != null) {
            String[] p = extra.split(":");
            if (p.length == 3) { in.putExtra("ah", Integer.parseInt(p[0])); in.putExtra("am", Integer.parseInt(p[1])); in.putExtra("mask", Integer.parseInt(p[2])); }
        }
        int code = (int) (millis % 1000000);
        PendingIntent pi = PendingIntent.getBroadcast(VnUi.C, code, in, PendingIntent.FLAG_IMMUTABLE);
        if (android.os.Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi);
        else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi);
        List<String[]> r = all();
        r.add(new String[]{String.valueOf(millis), text, String.valueOf(code), kind, extra == null ? "" : extra});
        save(r);
        return code;
    }
    public static void cancelByCode(int code) {
        AlarmManager am = (AlarmManager) VnUi.C.getSystemService(Context.ALARM_SERVICE);
        am.cancel(PendingIntent.getBroadcast(VnUi.C, code, new Intent(VnUi.C, VnAlarm.Receiver.class), PendingIntent.FLAG_IMMUTABLE));
    }
    static long atTime(int h, int m) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, m); c.set(Calendar.SECOND, 0);
        if (c.before(Calendar.getInstance())) c.add(Calendar.DAY_OF_YEAR, 1);
        return c.getTimeInMillis();
    }

    public static LinearLayout build() {
        LinearLayout c = VnUi.col();
        VnUi.addSpaced(c, VnUi.bigI("memo", "CREATE REMINDER", 0xFF3FAE4C, VnTheme.CREAM, v -> VnHome.router.show(buildEditor(Calendar.getInstance(), ""), "New reminder")), 10);
        VnUi.addSpaced(c, VnUi.bigI("memo", "In an hour: pills", VnTheme.MUSTARD, VnTheme.BROWN, v -> { schedule(System.currentTimeMillis() + 3600000, "Time to take pills!", "rem", null); VnHome.say("Will remind in an hour."); VnHome.router.show(build(), "Reminders"); }), 10);
        VnUi.addSpaced(c, VnUi.bigI("memo", "Morning at 9:00: pills", VnTheme.MUSTARD, VnTheme.BROWN, v -> { schedule(atTime(9, 0), "Time to take pills!", "rem", null); VnHome.say("Will remind at nine in the morning."); VnHome.router.show(build(), "Reminders"); }), 10);
        c.addView(VnUi.tv("Already set:", 16, VnTheme.BROWN, true));
        addList(c, "rem", "Reminders");
        return c;
    }

    static void addList(LinearLayout c, String kind, String backTitle) {
        List<String[]> r = all();
        long now = System.currentTimeMillis();
        boolean any = false;
        for (int i = r.size() - 1; i >= 0; i--) {
            if (!r.get(i)[3].equals(kind)) continue;
            long ms = Long.parseLong(r.get(i)[0]);
            if (ms <= now) continue;
            any = true;
            Calendar cd = Calendar.getInstance(); cd.setTimeInMillis(ms);
            String when = String.format(Locale.getDefault(), "%02d:%02d %02d.%02d", cd.get(Calendar.HOUR_OF_DAY), cd.get(Calendar.MINUTE), cd.get(Calendar.DAY_OF_MONTH), cd.get(Calendar.MONTH) + 1);
            final int code = Integer.parseInt(r.get(i)[2]);
            final int idx = i;
            LinearLayout rw = VnUi.row();
            Button b = VnUi.big(when + "\n" + r.get(i)[1] + "\n" + until(ms), VnTheme.MUSTARD, VnTheme.BROWN, v -> VnHome.say("Reminder: " + r.get(idx)[1] + " at " + when));
            b.setTextSize(14 * VnUi.FS * VnUi.SC);
            b.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            rw.addView(b);
            Button db = VnUi.big("✕", VnTheme.RUST, VnTheme.CREAM, v -> { cancelByCode(code); List<String[]> x = all(); x.remove(idx); save(x); VnHome.say("Removed."); VnHome.router.show(kind.equals("rem") ? build() : VnAlarm.build(), backTitle); });
            db.setLayoutParams(new LinearLayout.LayoutParams(VnUi.dp(70), LinearLayout.LayoutParams.WRAP_CONTENT));
            rw.addView(db);
            VnUi.addSpaced(c, rw, 10);
        }
        if (!any) c.addView(VnUi.tv("(nothing yet)", 14, VnTheme.BROWN, false));
    }

    public static LinearLayout buildEditor(final Calendar sel, String preName) {
        LinearLayout c = VnUi.col();
        final EditText en = new EditText(VnUi.C); en.setTextSize(20 * VnUi.FS * VnUi.SC); en.setHint("Name (e.g. pills)");
        en.setText(preName);
        c.addView(en);
        final int[] hv = {9}; final int[] mv = {0};
        addTimeRow(c, "HOURS", hv, 24, 1);
        addTimeRow(c, "MINUTES", mv, 60, 5);
        VnUi.addSpaced(c, VnUi.bigI("clock", "Date: " + sel.get(Calendar.DAY_OF_MONTH) + "." + (sel.get(Calendar.MONTH) + 1) + "." + sel.get(Calendar.YEAR), VnTheme.MUSTARD, VnTheme.BROWN, v -> VnHome.router.show(VnCalendar.buildPicker(sel, out -> VnHome.router.show(buildEditor(out, en.getText().toString()), "New reminder")), "Pick date")), 8);
        VnUi.addSpaced(c, VnUi.big("💾 SAVE", 0xFF3FAE4C, VnTheme.CREAM, v -> {
            String name = en.getText().toString().trim();
            if (name.isEmpty()) name = "Reminder";
            Calendar cc = (Calendar) sel.clone();
            cc.set(Calendar.HOUR_OF_DAY, hv[0]); cc.set(Calendar.MINUTE, mv[0]); cc.set(Calendar.SECOND, 0);
            if (cc.before(Calendar.getInstance())) cc.add(Calendar.DAY_OF_YEAR, 1);
            schedule(cc.getTimeInMillis(), name, "rem", null);
            VnHome.say("Reminder created.");
            VnHome.router.show(build(), "Reminders");
        }), 8);
        VnUi.addSpaced(c, VnUi.big("CANCEL", VnTheme.MUSTARD, VnTheme.BROWN, v -> VnHome.router.show(build(), "Reminders")), 8);
        return c;
    }

    static void addTimeRow(LinearLayout c, String label, final int[] val, int mod, int step) {
        LinearLayout r = VnUi.row();
        r.setGravity(Gravity.CENTER);
        TextView lt = VnUi.tv(label, 16, VnTheme.GREEN, true);
        lt.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(lt);
        Button minus = VnUi.big("−", VnTheme.MUSTARD, VnTheme.BROWN, v -> { val[0] = (val[0] - step + mod) % mod; ((TextView) r.getChildAt(2)).setText(String.format(Locale.getDefault(), "%02d", val[0])); });
        minus.setTextSize(28 * VnUi.FS * VnUi.SC);
        minus.setLayoutParams(new LinearLayout.LayoutParams(VnUi.dp(84), LinearLayout.LayoutParams.WRAP_CONTENT));
        final TextView disp = VnUi.tv(String.format(Locale.getDefault(), "%02d", val[0]), 34, VnTheme.GREEN, true);
        disp.setGravity(Gravity.CENTER);
        disp.setMinWidth(VnUi.dp(110));
        Button plus = VnUi.big("+", VnTheme.MUSTARD, VnTheme.BROWN, v -> { val[0] = (val[0] + step) % mod; ((TextView) r.getChildAt(2)).setText(String.format(Locale.getDefault(), "%02d", val[0])); });
        plus.setTextSize(28 * VnUi.FS * VnUi.SC);
        plus.setLayoutParams(new LinearLayout.LayoutParams(VnUi.dp(84), LinearLayout.LayoutParams.WRAP_CONTENT));
        r.addView(minus); r.addView(disp); r.addView(plus);
        VnUi.addSpaced(c, r, 8);
    }
}
