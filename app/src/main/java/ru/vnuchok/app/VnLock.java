package ru.vnuchok.app;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
/* БЛОКИРОВКА И ПЕРВЫЙ ЗАПУСК: приветствие один раз, затем занавес из холмов с лесом (2 сек).
   Здесь же полноэкранное окно напоминания, когда срабатывает будильник. */
public class VnLock {
    private static SharedPreferences P() { return VnUi.C.getSharedPreferences("vnuchok", Context.MODE_PRIVATE); }

    public static void play(FrameLayout root) {
        if (!P().getBoolean("firstRun", false)) {
            LinearLayout ov = VnUi.col();
            ov.setBackground(VnBg.paper());
            FrameLayout fz = new FrameLayout(VnUi.C);
            View scene = VnBg.scene(VnUi.C);
            fz.addView(scene, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, VnUi.dp(260)));
            LinearLayout pills = VnUi.col();
            pills.setPadding(0, 0, 0, 0);
            pills.addView(VnUi.tv("Hello!", 30, VnTheme.GREEN, true));
            pills.addView(VnUi.tv("I'm Vnuchok — your phone assistant", 16, VnTheme.BROWN, true));
            fz.addView(pills, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
            ov.addView(fz, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            ov.addView(VnUi.tv("I speak, call, remind and protect.", 15, VnTheme.GREEN, false));
            VnUi.addSpaced(ov, VnUi.bigI("home", "START AND OPEN VNUCHOK", VnTheme.GREEN, VnTheme.CREAM, v -> {
                P().edit().putBoolean("firstRun", true).apply();
                root.removeView(ov);
                curtain(root);
            }), 12);
            root.addView(ov, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            return;
        }
        curtain(root);
    }

    private static void curtain(FrameLayout root) {
        final View cv = VnBg.curtain(VnUi.C);
        root.addView(cv, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        cv.post(() -> ((VnBg.CurtainView) cv).start(() -> root.removeView(cv)));
    }

    /* полноэкранное окно напоминания (будильник/таблетки) */
    public static LinearLayout buildReminder(String text) {
        LinearLayout ov = VnUi.col();
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(VnTheme.RUST);
        ov.setBackground(gd);
        ov.setPadding(VnUi.dp(30), VnUi.dp(30), VnUi.dp(30), VnUi.dp(30));
        ov.addView(VnUi.tv("💊", 60, VnTheme.CREAM, false));
        TextView bt = VnUi.tv(text.toUpperCase(), 30, VnTheme.CREAM, true);
        bt.setGravity(Gravity.CENTER);
        ov.addView(bt);
        VnUi.addSpaced(ov, VnUi.big("✅ ACCEPTED", 0xFF3FAE4C, VnTheme.CREAM, v -> { VnHome.router.home(); VnHome.say("Well done! Noted."); }), 10);
        VnUi.addSpaced(ov, VnUi.bigI("alarm", "REMIND IN AN HOUR", VnTheme.MUSTARD, VnTheme.BROWN, v -> {
            VnRemind.schedule(System.currentTimeMillis() + 3600000, text, "rem", null);
            VnHome.router.home();
            VnHome.say("Will remind again in an hour.");
        }), 10);
        return ov;
    }
}
