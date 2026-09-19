package ru.vnuchok.app;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Calendar;
/* КАЛЕНДАРЬ: собственный выбор даты (сетка месяца в стиле оболочки) + системный календарь. */
public class VnCalendar {
    public interface OnDate { void on(Calendar c); }
    private static final String[] DN = {"MO", "TU", "WE", "TH", "FR", "SA", "SU"};
    private static final String[] MN = {"JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"};

    public static void openSystem() {
        try { VnUi.C.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.calendar/time/" + System.currentTimeMillis()))); }
        catch (Exception e) {
            try { VnUi.C.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)); }
            catch (Exception e2) { VnHome.say("Calendar not found."); }
        }
    }

    public static LinearLayout buildPicker(final Calendar cur, final OnDate onDate) {
        LinearLayout c = VnUi.col();
        LinearLayout hr = VnUi.row();
        Button prev = VnUi.big("◀", VnTheme.MUSTARD, VnTheme.BROWN, v -> { cur.add(Calendar.MONTH, -1); VnHome.router.show(buildPicker(cur, onDate), "Pick date"); });
        TextView title = VnUi.tv(MN[cur.get(Calendar.MONTH)] + " " + cur.get(Calendar.YEAR), 18, VnTheme.GREEN, true);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        title.setGravity(Gravity.CENTER);
        Button next = VnUi.big("▶", VnTheme.MUSTARD, VnTheme.BROWN, v -> { cur.add(Calendar.MONTH, 1); VnHome.router.show(buildPicker(cur, onDate), "Pick date"); });
        prev.setLayoutParams(new LinearLayout.LayoutParams(VnUi.dp(84), LinearLayout.LayoutParams.WRAP_CONTENT));
        next.setLayoutParams(new LinearLayout.LayoutParams(VnUi.dp(84), LinearLayout.LayoutParams.WRAP_CONTENT));
        hr.addView(prev); hr.addView(title); hr.addView(next);
        VnUi.addSpaced(c, hr, 8);
        LinearLayout wr = VnUi.row();
        for (String d : DN) {
            TextView w = VnUi.tv(d, 14, VnTheme.BROWN, true);
            w.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            w.setGravity(Gravity.CENTER);
            wr.addView(w);
        }
        c.addView(wr);
        Calendar first = (Calendar) cur.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);
        int off = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        int dim = cur.getActualMaximum(Calendar.DAY_OF_MONTH);
        Calendar today = Calendar.getInstance();
        LinearLayout dr = null; int cell = 0;
        for (int i = 0; i < off; i++) {
            if (cell % 7 == 0) { dr = VnUi.row(); c.addView(dr); }
            TextView e = VnUi.tv("", 15, VnTheme.GREEN, false);
            e.setLayoutParams(new LinearLayout.LayoutParams(0, VnUi.dp(48), 1f));
            dr.addView(e); cell++;
        }
        for (int d = 1; d <= dim; d++) {
            if (cell % 7 == 0) { dr = VnUi.row(); c.addView(dr); }
            final int day = d;
            boolean isToday = day == today.get(Calendar.DAY_OF_MONTH) && cur.get(Calendar.MONTH) == today.get(Calendar.MONTH) && cur.get(Calendar.YEAR) == today.get(Calendar.YEAR);
            Button db = VnUi.big(String.valueOf(day), isToday ? VnTheme.GREEN : VnTheme.MUSTARD, isToday ? VnTheme.CREAM : VnTheme.BROWN, v -> {
                Calendar out = (Calendar) cur.clone();
                out.set(Calendar.DAY_OF_MONTH, day);
                onDate.on(out);
            });
            db.setTextSize(16 * VnUi.FS * VnUi.SC);
            db.setLayoutParams(new LinearLayout.LayoutParams(0, VnUi.dp(48), 1f));
            dr.addView(db); cell++;
        }
        while (cell % 7 != 0) {
            if (dr == null) { dr = VnUi.row(); c.addView(dr); }
            TextView e = VnUi.tv("", 15, VnTheme.GREEN, false);
            e.setLayoutParams(new LinearLayout.LayoutParams(0, VnUi.dp(48), 1f));
            dr.addView(e); cell++;
        }
        return c;
    }
}
