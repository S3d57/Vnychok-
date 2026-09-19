package ru.vnuchok.app;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Locale;
/* ЧАСЫ И ПОГОДА: пилюля даты, часы с двойным кольцом и полосами, погодная пилюля. */
public class VnClockWeather {
    public TextView timeView, dateView, weatherText;
    private Handler H = new Handler(Looper.getMainLooper());
    private Runnable clockRun;
    private static final String[] DN = {"Воскресенье", "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"};
    private static final String[] MN = {"ЯНВАРЬ", "ФЕВРАЛЬ", "МАРТ", "АПРЕЛЬ", "МАЙ", "ИЮНЬ", "ИЮЛЬ", "АВГУСТ", "СЕНТЯБРЬ", "ОКТЯБРЬ", "НОЯБРЬ", "ДЕКАБРЬ"};

    public static String curTime() {
        Calendar c = Calendar.getInstance();
        return String.format(Locale.getDefault(), "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }
    public static String curDate() {
        Calendar c = Calendar.getInstance();
        return DN[c.get(Calendar.DAY_OF_WEEK) - 1] + ", " + c.get(Calendar.DAY_OF_MONTH) + " " + MN[c.get(Calendar.MONTH)].toLowerCase();
    }

    public LinearLayout build() {
        LinearLayout col = VnUi.col();
        col.setPadding(0, 0, 0, 0);
        LinearLayout datePill = VnUi.row();
        datePill.setBackground(VnUi.pill(VnTheme.CREAM));
        datePill.setPadding(VnUi.dp(16), VnUi.dp(4), VnUi.dp(16), VnUi.dp(4));
        datePill.setGravity(Gravity.CENTER);
        dateView = VnUi.tv(curDate(), 13, VnTheme.GREEN, true);
        datePill.addView(dateView);
        col.addView(datePill);

        FrameLayout wrap = new FrameLayout(VnUi.C);
        LinearLayout clockPill = VnUi.row();
        clockPill.setBackground(VnUi.ringPill(VnTheme.GREEN, VnTheme.RUST));
        clockPill.setPadding(VnUi.dp(24), VnUi.dp(6), VnUi.dp(24), VnUi.dp(6));
        clockPill.setGravity(Gravity.CENTER);
        timeView = VnUi.tv(curTime(), 34, VnTheme.CREAM, true);
        clockPill.addView(timeView);
        wrap.addView(clockPill, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        View stripes = VnBg.stripes(VnUi.C);
        FrameLayout.LayoutParams stp = new FrameLayout.LayoutParams(VnUi.dp(100), VnUi.dp(70));
        stp.gravity = Gravity.END | Gravity.TOP;
        wrap.addView(stripes, stp);
        col.addView(wrap, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout weatherPill = VnUi.row();
        weatherPill.setBackground(VnUi.pill(VnTheme.CREAM));
        weatherPill.setPadding(VnUi.dp(14), VnUi.dp(4), VnUi.dp(14), VnUi.dp(4));
        weatherPill.setGravity(Gravity.CENTER);
        Drawable wic = VnIcons.icon("sun", VnTheme.MUSTARD);
        wic.setBounds(0, 0, VnUi.dp(26), VnUi.dp(26));
        TextView wicon = VnUi.tv("", 13, VnTheme.MUSTARD, true);
        wicon.setCompoundDrawables(wic, null, null, null);
        weatherText = VnUi.tv(" —", 13, VnTheme.GREEN, true);
        weatherPill.addView(wicon); weatherPill.addView(weatherText);
        col.addView(weatherPill);
        return col;
    }

    public void startClock() {
        if (clockRun == null) clockRun = new Runnable() { public void run() { if (timeView != null) timeView.setText(curTime()); H.postDelayed(this, 20000); } };
        H.postDelayed(clockRun, 20000);
    }

    public void fetchWeather() {
        new Thread(() -> {
            String label = "—";
            try {
                URL u = new URL("https://wttr.in/?format=%t+%C&lang=ru&0q");
                HttpURLConnection c = (HttpURLConnection) u.openConnection();
                c.setConnectTimeout(3000); c.setReadTimeout(4000);
                c.setRequestProperty("User-Agent", "curl/7.0");
                BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                String s = sb.toString().trim();
                if (!s.isEmpty()) label = s.length() > 40 ? s.substring(0, 40) : s;
            } catch (Exception e) { }
            final String f = label;
            H.post(() -> { if (weatherText != null) weatherText.setText(" " + f); });
        }).start();
    }
}
