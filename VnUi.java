package ru.vnuchok.app;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
/* UI-КИРПИЧИКИ: dp, тексты, контейнеры, пилюли, кнопки, анимация нажатия, мигалки. */
public class VnUi {
    public static Context C;
    public static float FS = 1f, SC = 1f;
    public static void init(Activity a) { C = a; }
    public static int dp(int x) { return Math.round(x * C.getResources().getDisplayMetrics().density * SC); }

    public static class THolder { public String bg; public ObjectAnimator oa; }
    public static THolder holder(View v) {
        Object o = v.getTag();
        if (o instanceof THolder) return (THolder) o;
        THolder th = new THolder(); v.setTag(th); return th;
    }

    public static TextView tv(String s, float size, int color, boolean bold) {
        TextView t = new TextView(C);
        t.setText(s); t.setTextSize(size * FS * SC); t.setTextColor(color);
        if (bold) t.getPaint().setFakeBoldText(true);
        t.setPadding(0, dp(2), 0, dp(2));
        return t;
    }
    public static LinearLayout col() {
        LinearLayout l = new LinearLayout(C);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(12), dp(6), dp(12), dp(6));
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        l.setClipChildren(false); l.setClipToPadding(false);
        return l;
    }
    public static LinearLayout row() {
        LinearLayout l = new LinearLayout(C);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setPadding(0, dp(2), 0, dp(4));
        l.setClipChildren(false); l.setClipToPadding(false);
        return l;
    }
    public static void addSpaced(LinearLayout c, View v, int marginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(marginDp);
        c.addView(v, lp);
    }
    public static Drawable pill(int body) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.RECTANGLE);
        g.setCornerRadius(dp(60));
        g.setColor(body);
        return g;
    }
    public static Drawable ringPill(int body, int outer) {
        int r = dp(60);
        GradientDrawable g1 = new GradientDrawable(); g1.setCornerRadius(r); g1.setColor(body); g1.setStroke(dp(4), outer);
        GradientDrawable g2 = new GradientDrawable(); g2.setCornerRadius(r - dp(6)); g2.setColor(body); g2.setStroke(dp(3), VnTheme.CREAM);
        GradientDrawable g3 = new GradientDrawable(); g3.setCornerRadius(r - dp(11)); g3.setColor(body);
        LayerDrawable ld = new LayerDrawable(new Drawable[]{g1, g2, g3});
        ld.setLayerInset(1, dp(7), dp(7), dp(7), dp(7));
        ld.setLayerInset(2, dp(12), dp(12), dp(12), dp(12));
        return ld;
    }
    public static void pressFx(View v) {
        AudioManager am = (AudioManager) C.getSystemService(Context.AUDIO_SERVICE);
        boolean soundOn = soundEnabled() && am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
        if (soundOn) {
            try { ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 70); tg.startTone(ToneGenerator.TONE_PROP_ACK, 70); tg.release(); } catch (Exception e) {}
            v.playSoundEffect(android.view.SoundEffectConstants.CLICK);
        }
        try { Vibrator vb = (Vibrator) C.getSystemService(Context.VIBRATOR_SERVICE); vb.vibrate(VibrationEffect.createOneShot(35, 140)); } catch (Exception e) {}
        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(250).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(250));
        THolder th = holder(v);
        if (th.bg != null) {
            GradientDrawable base = new GradientDrawable(); base.setColor(Color.parseColor(th.bg)); base.setCornerRadius(dp(22));
            final GradientDrawable ring = new GradientDrawable(); ring.setColor(Color.TRANSPARENT); ring.setCornerRadius(dp(22)); ring.setStroke(dp(6), 0xFFFFC46B);
            LayerDrawable ld = new LayerDrawable(new Drawable[]{base, ring});
            v.setBackground(ld);
            if (Build.VERSION.SDK_INT >= 28) { v.setOutlineSpotShadowColor(0xFFFFC46B); v.setOutlineAmbientShadowColor(0xFFFFC46B); }
            v.setElevation(dp(14));
            ValueAnimator va = ValueAnimator.ofInt(255, 0); va.setDuration(500);
            va.addUpdateListener(a -> ring.setAlpha((Integer) a.getAnimatedValue()));
            va.start();
            final String bg = th.bg;
            v.postDelayed(() -> { v.setBackground(pill(Color.parseColor(bg))); v.setElevation(dp(5)); }, 500);
        }
    }
    public static boolean soundEnabled() { return C.getSharedPreferences("vnuchok", Context.MODE_PRIVATE).getBoolean("sound", true); }
    public static Button big(String text, int bgColor, int fgColor, View.OnClickListener l) {
        Button b = new Button(C);
        b.setText(text); b.setTextSize(18 * FS * SC); b.setTextColor(fgColor);
        b.setAllCaps(false); b.getPaint().setFakeBoldText(true);
        b.setBackground(pill(bgColor));
        THolder th = new THolder(); th.bg = VnTheme.hex(bgColor); b.setTag(th);
        b.setElevation(dp(5));
        b.setPadding(dp(14), dp(12), dp(14), dp(12));
        b.setOnClickListener(v -> { pressFx(v); l.onClick(v); });
        return b;
    }
    public static Button bigI(String kind, String text, int bgColor, int fgColor, View.OnClickListener l) {
        Button b = big(text, bgColor, fgColor, l);
        Drawable ic = VnIcons.icon(kind, fgColor); ic.setBounds(0, 0, dp(30), dp(30));
        b.setCompoundDrawables(ic, null, null, null);
        b.setCompoundDrawablePadding(dp(8));
        return b;
    }
    public static Button tile(String kind, String label, int bgColor, int fgColor, View.OnClickListener l) {
        Button b = new Button(C);
        b.setText(label); b.setTextSize(12 * FS * SC); b.setTextColor(fgColor);
        b.setAllCaps(false); b.getPaint().setFakeBoldText(true);
        b.setBackground(pill(bgColor));
        THolder th = new THolder(); th.bg = VnTheme.hex(bgColor); b.setTag(th);
        b.setElevation(dp(4));
        Drawable ic = VnIcons.icon(kind, fgColor); ic.setBounds(0, 0, dp(30), dp(30));
        b.setCompoundDrawables(null, ic, null, null);
        b.setCompoundDrawablePadding(dp(4));
        b.setPadding(dp(2), dp(6), dp(2), dp(8));
        b.setOnClickListener(v -> { pressFx(v); l.onClick(v); });
        return b;
    }
    public static void blink(View v, boolean on) {
        if (v == null) return;
        THolder th = holder(v);
        if (on) {
            v.animate().cancel();
            ObjectAnimator oa = ObjectAnimator.ofFloat(v, "alpha", 1f, 0.3f);
            oa.setDuration(600); oa.setRepeatMode(ValueAnimator.REVERSE); oa.setRepeatCount(ValueAnimator.INFINITE);
            oa.start(); th.oa = oa;
        } else {
            if (th.oa != null) th.oa.cancel();
            th.oa = null;
            v.animate().alpha(1f);
        }
    }
}
