package ru.vnuchok.app;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.CallLog;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.location.Location;
import android.location.LocationManager;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    // ---------- РИСОВАННЫЕ ЗНАЧКИ ----------
    static class Icon extends Drawable {
        String k; Paint f, s; int level = -1, bars = -1;
        Icon(String k, int col) {
            this.k = k;
            f = new Paint(Paint.ANTI_ALIAS_FLAG); f.setColor(col); f.setStyle(Paint.Style.FILL);
            s = new Paint(Paint.ANTI_ALIAS_FLAG); s.setColor(col); s.setStyle(Paint.Style.STROKE); s.setStrokeWidth(7f); s.setStrokeCap(Paint.Cap.ROUND); s.setStrokeJoin(Paint.Join.ROUND);
        }
        @Override public void draw(Canvas c) {
            Rect b = getBounds();
            c.save(); c.translate(b.left, b.top);
            c.scale(b.width() / 100f, b.height() / 100f);
            switch (k) {
                case "phone": c.rotate(-45, 50, 50); c.drawRoundRect(15, 40, 85, 62, 11, 11, f); c.drawRoundRect(10, 26, 32, 62, 10, 10, f); c.drawRoundRect(68, 26, 90, 62, 10, 10, f); break;
                case "mail": c.drawRoundRect(8, 22, 92, 78, 8, 8, s); c.drawLine(12, 28, 50, 56, s); c.drawLine(88, 28, 50, 56, s); break;
                case "dial": for (int i = 0; i < 9; i++) c.drawCircle(25 + (i % 3) * 25, 25 + (i / 3) * 25, 9, f); break;
                case "cam": c.drawRoundRect(6, 30, 94, 82, 10, 10, s); c.drawCircle(50, 56, 16, s); c.drawRoundRect(36, 16, 64, 32, 6, 6, f); break;
                case "album": c.drawRoundRect(8, 14, 92, 86, 8, 8, s); Path pa = new Path(); pa.moveTo(20, 72); pa.lineTo(42, 42); pa.lineTo(58, 62); pa.lineTo(70, 48); pa.lineTo(82, 72); pa.close(); c.drawPath(pa, f); c.drawCircle(68, 32, 8, f); break;
                case "memo": c.drawRoundRect(18, 8, 78, 92, 8, 8, s); c.drawLine(30, 30, 66, 30, s); c.drawLine(30, 46, 66, 46, s); c.drawLine(30, 62, 52, 62, s); c.save(); c.rotate(45, 72, 72); c.drawRoundRect(56, 66, 94, 78, 5, 5, f); c.restore(); break;
                case "alarm": c.drawCircle(50, 60, 28, s); c.drawLine(50, 60, 50, 44, s); c.drawLine(50, 60, 62, 64, s); c.drawRoundRect(12, 12, 34, 26, 6, 6, f); c.drawRoundRect(66, 12, 88, 26, 6, 6, f); c.drawLine(28, 84, 20, 94, s); c.drawLine(72, 84, 80, 94, s); break;
                case "clock": c.drawCircle(50, 50, 34, s); c.drawLine(50, 50, 50, 28, s); c.drawLine(50, 50, 66, 56, s); break;
                case "torch": Path ph = new Path(); ph.moveTo(30, 18); ph.lineTo(70, 18); ph.lineTo(62, 42); ph.lineTo(38, 42); ph.close(); c.drawPath(ph, f); c.drawRoundRect(38, 42, 62, 90, 6, 6, f); c.drawLine(50, 4, 50, 12, s); c.drawLine(28, 8, 34, 14, s); c.drawLine(72, 8, 66, 14, s); break;
                case "wrench": c.drawCircle(34, 34, 17, s); c.save(); c.rotate(45, 50, 50); c.drawRoundRect(42, 44, 92, 60, 8, 8, f); c.restore(); c.drawRoundRect(22, 28, 46, 40, 5, 5, f); break;
                case "mic": c.drawRoundRect(38, 6, 62, 50, 12, 12, f); c.drawArc(26, 28, 74, 76, 0, 180, false, s); c.drawLine(50, 76, 50, 90, s); c.drawLine(34, 92, 66, 92, s); break;
                case "sos": c.drawRect(40, 14, 60, 86, f); c.drawRect(14, 40, 86, 60, f); break;
                case "radio": c.drawRoundRect(8, 36, 92, 84, 8, 8, s); c.drawCircle(32, 60, 12, s); c.drawLine(58, 50, 82, 50, s); c.drawLine(58, 64, 82, 64, s); c.drawLine(68, 36, 88, 12, s); break;
                case "map": Path pm = new Path(); pm.moveTo(12, 26); pm.lineTo(38, 16); pm.lineTo(62, 26); pm.lineTo(88, 16); pm.lineTo(88, 74); pm.lineTo(62, 84); pm.lineTo(38, 74); pm.lineTo(12, 84); pm.close(); c.drawPath(pm, s); c.drawLine(38, 16, 38, 74, s); c.drawLine(62, 26, 62, 84, s); break;
                case "music": c.drawCircle(32, 76, 14, f); c.drawRect(44, 20, 50, 76, f); c.drawRoundRect(44, 12, 80, 30, 8, 8, f); break;
                case "weather": c.drawCircle(36, 36, 15, f); c.drawCircle(54, 62, 15, f); c.drawCircle(70, 66, 12, f); c.drawRoundRect(40, 62, 86, 78, 8, 8, f); break;
                case "web": c.drawCircle(50, 50, 36, s); c.drawOval(new RectF(32, 14, 68, 86), s); c.drawLine(14, 50, 86, 50, s); c.drawLine(20, 30, 80, 30, s); c.drawLine(20, 70, 80, 70, s); break;
                case "sound": Path sp = new Path(); sp.moveTo(14, 40); sp.lineTo(34, 40); sp.lineTo(54, 22); sp.lineTo(54, 78); sp.lineTo(34, 60); sp.lineTo(14, 60); sp.close(); c.drawPath(sp, f); c.drawArc(58, 30, 88, 70, -40, 80, false, s); break;
                case "silent": Path sq = new Path(); sq.moveTo(14, 40); sq.lineTo(34, 40); sq.lineTo(54, 22); sq.lineTo(54, 78); sq.lineTo(34, 60); sq.lineTo(14, 60); sq.close(); c.drawPath(sq, f); c.drawLine(66, 36, 90, 64, s); c.drawLine(90, 36, 66, 64, s); break;
                case "gear": c.drawCircle(50, 50, 20, s); c.drawCircle(50, 50, 7, f); for (int i = 0; i < 8; i++) { c.save(); c.rotate(i * 45f, 50, 50); c.drawRoundRect(45, 12, 55, 26, 4, 4, f); c.restore(); } break;
                case "batt": float lv = level < 0 ? 50 : level; s.setStrokeWidth(6); c.drawRoundRect(4, 26, 82, 74, 8, 8, s); c.drawRoundRect(85, 40, 96, 60, 3, 3, f); float fw = Math.max(5, 68 * lv / 100f); c.drawRoundRect(10, 32, 10 + fw, 68, 5, 5, f); break;
                case "sig": for (int i = 0; i < 4; i++) { float bh = 25 + i * 20; boolean on = bars < 0 || i < bars; c.drawRoundRect(10 + i * 24, 92 - bh, 26 + i * 24, 92, 4, 4, on ? f : s); } break;
                case "home": Path hh = new Path(); hh.moveTo(50, 14); hh.lineTo(88, 48); hh.lineTo(76, 48); hh.lineTo(76, 86); hh.lineTo(24, 86); hh.lineTo(24, 48); hh.lineTo(12, 48); hh.close(); c.drawPath(hh, s); break;
            }
            c.restore();
        }
        @Override public void setAlpha(int a) {}
        @Override public void setColorFilter(ColorFilter cf) {}
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    Icon ic(String k, String col, int size) { Icon i = new Icon(k, Color.parseColor(col)); i.setBounds(0, 0, size, size); return i; }

    Handler H = new Handler(Looper.getMainLooper());
    FrameLayout frame; ScrollView scroll;
    TextView caption, userSay, timeView, dateView, sbPopup, sbBatt, sbSig, sbNet;
    Button micBtn, msgTile, callTile;
    TextToSpeech tts; boolean ttsReady;
    SpeechRecognizer sr, cSr, compSr;
    boolean torchOn, battWarned, listening, pendingConfirm, compListen, charging;
    int lastPct = -1, sigBars = 3; String sigLabel = "";
    LinearLayout battOverlay, remOverlay, confirmOverlay;
    String lastSay = "", pendingNum = "", pendingLabel = "";
    BatteryReceiver battRec; BroadcastReceiver smsRec;
    Runnable clockRun, missRun, sigRun;
    SharedPreferences P;
    float FS = 1f, SC = 1f;
    Runnable sosTicker; volatile boolean sosActive;
    EditText composeEt;
    int composeIdx = -1;
    Runnable composeSend, composeCancel;
    Calendar selDate, calCur;
    int alarmMask = 0;
    String BG, CARD, TILE, TFG, ACC, DARK, EDGE, MUT, SBBG, SBFG;
    String[] tilePal = null;

    static final String DEF_CONTACTS = "Дочь Маша|+79000000001\nВнук Миша|+7900000002\nВнучка Оля|+7900000003\nСоседка Нина|+7900000004\nВрач Ирина|+7900000005";
    static final String[] DN = {"ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС"};
    static final String[] MN = {"ЯНВАРЬ", "ФЕВРАЛЬ", "МАРТ", "АПРЕЛЬ", "МАЙ", "ИЮНЬ", "ИЮЛЬ", "АВГУСТ", "СЕНТЯБРЬ", "ОКТЯБРЬ", "НОЯБРЬ", "ДЕКАБРЬ"};

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        P = getSharedPreferences("vnuchok", MODE_PRIVATE);
        FS = P.getFloat("fs", 1f);
        float dens = getResources().getDisplayMetrics().density;
        float wdp = getResources().getDisplayMetrics().widthPixels / dens;
        SC = Math.max(0.85f, Math.min(1.6f, wdp / 360f));
        applyTheme(P.getInt("theme", 0));
        requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.CALL_PHONE, android.Manifest.permission.SEND_SMS,
                android.Manifest.permission.READ_SMS, android.Manifest.permission.RECEIVE_SMS,
                android.Manifest.permission.READ_CALL_LOG,
                android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        frame = new FrameLayout(this);
        frame.setBackgroundColor(Color.parseColor(BG));
        scroll = new ScrollView(this);
        frame.addView(scroll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(frame);
        tts = new TextToSpeech(this, this);
        battRec = new BatteryReceiver();
        registerReceiver(battRec, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        smsRec = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) {
                H.post(() -> {
                    if (sbPopup != null) { sbPopup.setText("✉ НОВОЕ СООБЩЕНИЕ!"); sbPopup.setVisibility(View.VISIBLE); blink(sbPopup, true); H.postDelayed(() -> { sbPopup.setVisibility(View.GONE); blink(sbPopup, false); }, 10000); }
                    if (msgTile != null) blink(msgTile, true);
                    say("Пришло новое сообщение. Нажмите кнопку сообщения, чтобы прочитать.");
                });
            }
        };
        try { registerReceiver(smsRec, new IntentFilter("android.provider.Telephony.SMS_RECEIVED")); } catch (Exception e) {}
        missRun = new Runnable() { public void run() { checkMissed(); H.postDelayed(this, 20000); } };
        H.postDelayed(missRun, 10000);
        sigRun = new Runnable() { public void run() { pollSignal(); H.postDelayed(this, 15000); } };
        H.postDelayed(sigRun, 2000);
        showMain();
        if (getIntent() != null && getIntent().hasExtra("reminder")) showReminder(getIntent().getStringExtra("reminder"));
    }

    @Override public void onWindowFocusChanged(boolean has) {
        super.onWindowFocusChanged(has);
        if (has) hideSys();
    }

    void hideSys() {
        View d = getWindow().getDecorView();
        d.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @Override protected void onResume() { super.onResume(); hideSys(); }

    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        if (i != null && i.hasExtra("reminder")) showReminder(i.getStringExtra("reminder"));
    }

    @Override public void onInit(int s) {
        if (s == TextToSpeech.SUCCESS) {
            ttsReady = true; tts.setLanguage(new Locale("ru")); tts.setSpeechRate(P.getFloat("rate", 0.75f));
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                public void onStart(String id) {}
                public void onDone(String id) { if (pendingConfirm) H.post(() -> { pendingConfirm = false; startConfirmListen(); }); }
                public void onError(String id) { if (pendingConfirm) H.post(() -> { pendingConfirm = false; startConfirmListen(); }); }
            });
        }
    }

    void applyTheme(int t) {
        if (t == 1) { BG = "#EFE6D0"; CARD = "#F4EBD8"; TILE = "#6B7245"; TFG = "#F2E8D5"; ACC = "#6B7245"; DARK = "#3E2A1A"; EDGE = "#6B3F23"; MUT = "#7A6A50"; SBBG = "#5A6340"; SBFG = "#F2E8D5"; tilePal = new String[]{"#6B7245", "#6B4F35", "#B0592F"}; }
        else if (t == 2) { BG = "#101426"; CARD = "#1B2137"; TILE = "#232A45"; TFG = "#7FE7EE"; ACC = "#00B8C8"; DARK = "#E6F1FF"; EDGE = "#00E5FF"; MUT = "#8FA3C8"; SBBG = "#1B2137"; SBFG = "#E6F1FF"; tilePal = null; }
        else { BG = "#F4E3BD"; CARD = "#FFFDF4"; TILE = "#F9ECCA"; TFG = "#7A4A21"; ACC = "#D94F1E"; DARK = "#4A2C17"; EDGE = "#C98D4F"; MUT = "#8A6A3A"; SBBG = "#F4E3BD"; SBFG = "#4A2C17"; tilePal = null; }
    }

    String tileColor(int i) { return tilePal != null ? tilePal[i % tilePal.length] : TILE; }

    void say(String t) {
        lastSay = t;
        H.post(() -> { if (caption != null) caption.setText("ВНУЧОК: «" + t + "»"); });
        if (ttsReady && P.getBoolean("voice", true)) tts.speak(t, TextToSpeech.QUEUE_FLUSH, null, "v");
    }

    void speak(String t) { if (ttsReady && !t.isEmpty()) tts.speak(t, TextToSpeech.QUEUE_FLUSH, null, "s"); }

    int dp(int x) { return Math.round(x * getResources().getDisplayMetrics().density * SC); }
    int shade(int c, float f) { return Color.argb(255, Math.max(0, Math.min(255, (int) (Color.red(c) * f))), Math.max(0, Math.min(255, (int) (Color.green(c) * f))), Math.max(0, Math.min(255, (int) (Color.blue(c) * f)))); }

    TextView tv(String s, float size, String color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size * FS * SC); t.setTextColor(Color.parseColor(color));
        if (bold) t.getPaint().setFakeBoldText(true);
        t.setPadding(0, dp(3), 0, dp(3));
        return t;
    }

    void blink(View v, boolean on) {
        if (v == null) return;
        if (on) {
            v.animate().cancel();
            ObjectAnimator oa = ObjectAnimator.ofFloat(v, "alpha", 1f, 0.3f);
            oa.setDuration(600); oa.setRepeatMode(ValueAnimator.REVERSE); oa.setRepeatCount(ValueAnimator.INFINITE);
            oa.start(); v.setTag(oa);
        } else {
            Object o = v.getTag();
            if (o instanceof ObjectAnimator) ((ObjectAnimator) o).cancel();
            v.animate().alpha(1f);
        }
    }

    void clickFx(View v) {
        if (P.getBoolean("sound", true)) {
            try { ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 60); tg.startTone(ToneGenerator.TONE_PROP_ACK, 60); tg.release(); } catch (Exception e) {}
            v.playSoundEffect(android.view.SoundEffectConstants.CLICK);
        }
        try { Vibrator vb = (Vibrator) getSystemService(VIBRATOR_SERVICE); vb.vibrate(VibrationEffect.createOneShot(30, 120)); } catch (Exception e) {}
        v.animate().translationY(dp(4)).setDuration(60).withEndAction(() -> v.animate().translationY(0).setDuration(60));
    }

    LayerDrawable bg3(String bg) {
        int top = Color.parseColor(bg);
        GradientDrawable gb = new GradientDrawable(); gb.setColor(shade(top, 0.45f)); gb.setCornerRadius(dp(14)); gb.setStroke(dp(2), shade(top, 0.25f));
        GradientDrawable gt = new GradientDrawable(); gt.setColor(top); gt.setCornerRadius(dp(14)); gt.setStroke(dp(2), shade(top, 0.65f));
        LayerDrawable ld = new LayerDrawable(new Drawable[]{gb, gt});
        ld.setLayerInset(1, 0, 0, 0, dp(8));
        return ld;
    }

    Button big(String text, String bg, String fg, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text); b.setTextSize(20 * FS * SC); b.setTextColor(Color.parseColor(fg));
        b.setAllCaps(false); b.getPaint().setFakeBoldText(true);
        b.setBackground(bg3(bg));
        b.setPadding(dp(10), dp(14), dp(10), dp(20));
        b.setOnClickListener(v -> { clickFx(v); l.onClick(v); });
        return b;
    }

    Button bigI(String kind, String text, String bg, String fg, View.OnClickListener l) {
        Button b = big(text, bg, fg, l);
        b.setCompoundDrawables(ic(kind, fg, dp(34)), null, null, null);
        b.setCompoundDrawablePadding(dp(10));
        return b;
    }

    Button tileBtn(String kind, String label, String bg, String fg, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(15 * FS * SC); b.setTextColor(Color.parseColor(fg));
        b.setAllCaps(false); b.getPaint().setFakeBoldText(true);
        b.setBackground(bg3(bg));
        b.setCompoundDrawables(null, ic(kind, fg, dp(46)), null, null);
        b.setCompoundDrawablePadding(dp(6));
        b.setPadding(dp(4), dp(10), dp(4), dp(12));
        b.setOnClickListener(v -> { clickFx(v); l.onClick(v); });
        return b;
    }

    LinearLayout col() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(10), dp(16), dp(16));
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        return l;
    }

    LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setPadding(0, dp(4), 0, dp(4));
        return l;
    }

    void setScreen(LinearLayout c) {
        H.post(() -> { scroll.removeAllViews(); scroll.addView(c); scroll.scrollTo(0, 0); });
    }

    String curTime() {
        Calendar c = Calendar.getInstance();
        return String.format(Locale.getDefault(), "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    String curDate() {
        Calendar c = Calendar.getInstance();
        String[] days = {"ВОСКРЕСЕНЬЕ", "ПОНЕДЕЛЬНИК", "ВТОРНИК", "СРЕДА", "ЧЕТВЕРГ", "ПЯТНИЦА", "СУББОТА"};
        return days[c.get(Calendar.DAY_OF_WEEK) - 1] + ", " + c.get(Calendar.DAY_OF_MONTH) + " " + MN[c.get(Calendar.MONTH)];
    }

    void pollSignal() {
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (Build.VERSION.SDK_INT >= 30) sigBars = Math.max(0, tm.getSignalLevel() + 1);
            int nt = tm.getNetworkType();
            switch (nt) {
                case TelephonyManager.NETWORK_TYPE_GPRS: case TelephonyManager.NETWORK_TYPE_EDGE: case TelephonyManager.NETWORK_TYPE_CDMA: case TelephonyManager.NETWORK_TYPE_IDEN: sigLabel = "E"; break;
                case TelephonyManager.NETWORK_TYPE_UMTS: case TelephonyManager.NETWORK_TYPE_HSDPA: case TelephonyManager.NETWORK_TYPE_HSUPA: case TelephonyManager.NETWORK_TYPE_HSPA: case TelephonyManager.NETWORK_TYPE_EVDO_0: case TelephonyManager.NETWORK_TYPE_EVDO_A: sigLabel = "3G"; break;
                case TelephonyManager.NETWORK_TYPE_LTE: sigLabel = "4G"; break;
                case TelephonyManager.NETWORK_TYPE_NR: sigLabel = "5G"; break;
                default: sigLabel = ""; break;
            }
        } catch (Exception e) {}
        refreshStatus();
    }

    void refreshStatus() {
        H.post(() -> {
            if (sbBatt != null) {
                int pc = lastPct >= 0 ? lastPct : 0;
                String col = pc <= 20 ? "#C0392B" : SBFG;
                sbBatt.setTextColor(Color.parseColor(col));
                sbBatt.setText(pc + "%");
                Icon bi = new Icon("batt", Color.parseColor(col)); bi.level = pc; bi.setBounds(0, 0, dp(34), dp(34));
                sbBatt.setCompoundDrawables(bi, null, null, null);
                sbBatt.setCompoundDrawablePadding(dp(6));
                blink(sbBatt, charging || pc <= 15);
            }
            if (sbSig != null) {
                sbSig.setText(sigLabel.isEmpty() ? "сеть" : sigLabel);
                Icon si = new Icon("sig", Color.parseColor(SBFG)); si.bars = sigBars; si.setBounds(0, 0, dp(30), dp(30));
                sbSig.setCompoundDrawables(si, null, null, null);
                sbSig.setCompoundDrawablePadding(dp(6));
            }
            if (sbNet != null) {
                boolean net = false;
                try { ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE); net = cm.getActiveNetwork() != null; } catch (Exception e) {}
                sbSig.setVisibility(View.VISIBLE);
                sbNet.setText(net ? "ИНТЕРНЕТ" : "БЕЗ СЕТИ");
                Icon wi = new Icon("web", Color.parseColor(SBFG)); wi.setBounds(0, 0, dp(30), dp(30));
                sbNet.setCompoundDrawables(wi, null, null, null);
                sbNet.setCompoundDrawablePadding(dp(6));
            }
        });
    }

    void checkMissed() {
        try {
            long last = P.getLong("lastMissed", 0);
            Cursor cur = getContentResolver().query(CallLog.Calls.CONTENT_URI,
                    new String[]{CallLog.Calls.NUMBER, CallLog.Calls.DATE},
                    CallLog.Calls.TYPE + "=" + CallLog.Calls.MISSED_TYPE, null, CallLog.Calls.DATE + " DESC LIMIT 1");
            if (cur != null) {
                if (cur.moveToFirst()) {
                    long d = cur.getLong(1);
                    String num = cur.getString(0);
                    if (d > last && last > 0) {
                        H.post(() -> { if (sbPopup != null) { sbPopup.setText("✗ ПРОПУЩЕННЫЙ: " + nameForNumber(num)); sbPopup.setVisibility(View.VISIBLE); blink(sbPopup, true); H.postDelayed(() -> { sbPopup.setVisibility(View.GONE); blink(sbPopup, false); }, 10000); } if (callTile != null) blink(callTile, true); say("Пропущенный звонок: " + nameForNumber(num)); });
                    }
                    if (last == 0) P.edit().putLong("lastMissed", d).apply();
                }
                cur.close();
            }
        } catch (Exception e) {}
    }

    // ---------- ГЛАВНЫЙ ЭКРАН ----------
    void showMain() {
        applyTheme(P.getInt("theme", 0));
        frame.setBackgroundColor(Color.parseColor(BG));
        LinearLayout c = col();

        LinearLayout sbar = row();
        sbar.setBackgroundColor(Color.parseColor(SBBG));
        sbar.setPadding(dp(12), dp(8), dp(12), dp(8));
        sbBatt = tv("", 17, SBFG, true);
        sbSig = tv("", 17, SBFG, true);
        sbNet = tv("", 17, SBFG, true);
        TextView gear = tv("", 20, SBFG, true);
        gear.setCompoundDrawables(ic("gear", SBFG, dp(34)), null, null, null);
        sbSig.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        sbNet.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        gear.setOnLongClickListener(v -> { showSettings(); return true; });
        gear.setOnClickListener(v -> say("Шестерёнка — настройки для родных. Нажмите и держите палец."));
        sbar.addView(sbBatt); sbar.addView(sbSig); sbar.addView(sbNet); sbar.addView(gear);
        c.addView(sbar);
        sbPopup = tv("", 16, "#C0392B", true);
        sbPopup.setVisibility(View.GONE);
        c.addView(sbPopup);
        pollSignal();

        timeView = tv(curTime(), 54, DARK, true);
        timeView.setGravity(Gravity.CENTER);
        timeView.setOnClickListener(v -> openSystemCalendar());
        c.addView(timeView);
        dateView = tv(curDate(), 17, MUT, true);
        dateView.setGravity(Gravity.CENTER);
        dateView.setOnClickListener(v -> openSystemCalendar());
        c.addView(dateView);
        if (clockRun == null) clockRun = new Runnable() { public void run() { if (timeView != null) { timeView.setText(curTime()); dateView.setText(curDate()); } H.postDelayed(this, 20000); } };
        H.postDelayed(clockRun, 20000);

        micBtn = big("НАЖМИТЕ", ACC, "#FFFFFF", v -> startListenOnMain());
        setMicLabel("НАЖМИТЕ");
        GradientDrawable og = new GradientDrawable();
        og.setColor(Color.parseColor(ACC)); og.setShape(GradientDrawable.OVAL);
        og.setStroke(dp(7), Color.parseColor(CARD));
        micBtn.setBackground(og);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(dp(200), dp(200));
        mp.topMargin = dp(8); mp.bottomMargin = dp(6);
        micBtn.setLayoutParams(mp);
        c.addView(micBtn);

        LinearLayout capBox = new LinearLayout(this);
        capBox.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable cg = new GradientDrawable();
        cg.setColor(Color.parseColor(CARD)); cg.setCornerRadius(dp(12));
        cg.setStroke(dp(3), Color.parseColor(EDGE));
        capBox.setBackground(cg); capBox.setPadding(dp(14), dp(12), dp(14), dp(12));
        capBox.setGravity(Gravity.START);
        userSay = tv("ВЫ: (нажмите кнопку и говорите)", 16, MUT, true);
        capBox.addView(userSay);
        caption = tv("ВНУЧОК: «Здравствуйте! Нажмите кнопку и говорите.»", 20, DARK, true);
        capBox.addView(caption);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.topMargin = dp(6); cp.bottomMargin = dp(6);
        capBox.setLayoutParams(cp);
        c.addView(capBox);

        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(0, dp(140), 1f);
        hp.setMargins(dp(4), dp(3), dp(4), dp(3));
        callTile = tileBtn("phone", "ПОЗВОНИТЬ", tileColor(0), TFG, v -> showContacts(true));
        msgTile = tileBtn("mail", "СООБЩЕНИЯ", tileColor(1), TFG, v -> { blink(msgTile, false); showSmsList(); });
        Button t3 = tileBtn("dial", "НАБОР", tileColor(2), TFG, v -> showDial());
        LinearLayout r1 = row();
        callTile.setLayoutParams(hp); msgTile.setLayoutParams(hp); t3.setLayoutParams(hp);
        r1.addView(callTile); r1.addView(msgTile); r1.addView(t3);
        c.addView(r1);
        Button t6 = tileBtn("memo", "НАПОМИНАНИЯ", tileColor(0), TFG, v -> showReminders());
        Button t7 = tileBtn("alarm", "БУДИЛЬНИК", tileColor(1), TFG, v -> showAlarms());
        Button t8 = tileBtn("torch", "ФОНАРЬ", tileColor(2), TFG, v -> toggleTorch());
        LinearLayout r2 = row();
        t6.setLayoutParams(hp); t7.setLayoutParams(hp); t8.setLayoutParams(hp);
        r2.addView(t6); r2.addView(t7); r2.addView(t8);
        c.addView(r2);

        Button sos = bigI("sos", "SOS — ВЫЗВАТЬ ПОМОЩЬ (112)", "#C0392B", "#FFFFFF", v -> startSos());
        sos.setTextSize(26 * FS * SC);
        sos.setMinHeight(dp(120));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin = dp(8);
        sos.setLayoutParams(sp);
        c.addView(sos);

        Button extra = bigI("wrench", "ПРОЧЕЕ", TILE, TFG, v -> showExtra());
        extra.setTextSize(22 * FS * SC);
        LinearLayout.LayoutParams ep2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ep2.topMargin = dp(6);
        extra.setLayoutParams(ep2);
        c.addView(extra);

        if (P.getBoolean("showApps", true)) {
            c.addView(tv("ДРУГИЕ ПРИЛОЖЕНИЯ", 14, MUT, true));
            LinearLayout.LayoutParams hp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            hp2.setMargins(dp(3), dp(3), dp(3), dp(3));
            List<Object[]> apps = installedApps();
            LinearLayout ar = null;
            int n = 0;
            for (final Object[] a : apps) {
                if (n % 3 == 0) { ar = row(); c.addView(ar); }
                Button ab = new Button(this);
                ab.setText((String) a[0]);
                ab.setTextSize(11 * FS * SC); ab.setTextColor(Color.parseColor(TFG));
                ab.setBackground(bg3(TILE));
                ab.setMinHeight(dp(90));
                Drawable icn = (Drawable) a[1];
                if (icn != null) { icn.setBounds(0, 0, dp(40), dp(40)); ab.setCompoundDrawables(null, icn, null, null); }
                ab.setOnClickListener(v -> { clickFx(v); openPackage((String) a[2]); });
                ab.setLayoutParams(hp2);
                ar.addView(ab);
                n++;
            }
            if (n == 0) c.addView(tv("(нет других приложений)", 13, MUT, false));
        }
        setScreen(c);
    }

    void openSystemCalendar() {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse("content://com.android.calendar/time/" + System.currentTimeMillis()));
            startActivity(i);
        } catch (Exception e) {
            try { startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)); }
            catch (Exception e2) { say("Календарь не найден."); }
        }
    }

    void setMicLabel(String s) {
        if (micBtn == null) return;
        micBtn.setText(s);
        micBtn.setTextSize(20 * FS * SC);
        micBtn.setCompoundDrawables(null, ic("mic", "#FFFFFF", dp(70)), null, null);
        micBtn.setCompoundDrawablePadding(dp(6));
    }

    void startListenOnMain() {
        if (listening) { stopListening(); setMicLabel("НАЖМИТЕ"); return; }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { say("Телефон не умеет слушать."); return; }
        listening = true;
        setMicLabel("ГОВОРИТЕ");
        userSay.setText("ВЫ: (слушаю…)");
        sr = SpeechRecognizer.createSpeechRecognizer(this);
        Intent it = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        it.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        it.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
        it.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        sr.setRecognitionListener(new android.speech.RecognitionListener() {
            public void onReadyForSpeech(Bundle p) {}
            public void onBeginningOfSpeech() {}
            public void onRmsChanged(float v) {}
            public void onBufferReceived(byte[] b) {}
            public void onEndOfSpeech() { H.post(() -> { listening = false; setMicLabel("НАЖМИТЕ"); }); }
            public void onError(int e) { H.post(() -> { listening = false; setMicLabel("НАЖМИТЕ"); userSay.setText("ВЫ: (не расслышал, повторите)"); say("Не расслышал. Повторите, пожалуйста."); }); }
            public void onResults(Bundle r) {
                ArrayList<String> a = r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String t = (a != null && !a.isEmpty()) ? a.get(0) : "";
                H.post(() -> { listening = false; setMicLabel("НАЖМИТЕ"); if (!t.isEmpty()) { userSay.setText("ВЫ: «" + t + "»"); handleCommand(t); } else userSay.setText("ВЫ: (пусто)"); });
            }
            public void onPartialResults(Bundle r) {
                ArrayList<String> a = r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (a != null && !a.isEmpty()) H.post(() -> userSay.setText("ВЫ: «" + a.get(0) + "»…"));
            }
            public void onEvent(int i, Bundle b) {}
        });
        try { sr.startListening(it); } catch (Exception e) { say("Микрофон не открылся."); listening = false; setMicLabel("НАЖМИТЕ"); }
    }

    void stopListening() {
        listening = false;
        try { if (sr != null) { sr.stopListening(); sr.cancel(); sr.destroy(); } } catch (Exception e) {}
        sr = null;
    }

    void stopConfirmListen() {
        try { if (cSr != null) { cSr.stopListening(); cSr.cancel(); cSr.destroy(); } } catch (Exception e) {}
        cSr = null;
    }

    void stopCompListen() {
        compListen = false;
        try { if (compSr != null) { compSr.stopListening(); compSr.cancel(); compSr.destroy(); } } catch (Exception e) {}
        compSr = null;
    }

    void openPhotoalbum() {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setType("image/*");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            try { startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_GALLERY)); }
            catch (Exception e2) { say("Фотоальбом не найден."); }
        }
    }

    void showExtra() {
        LinearLayout c = col();
        c.addView(tv("ПРОЧЕЕ", 26, DARK, true));
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        boolean silent = am.getRingerMode() != AudioManager.RINGER_MODE_NORMAL;
        c.addView(bigI(silent ? "silent" : "sound", silent ? "ТИХИЙ РЕЖИМ: ВКЛ" : "ТИХИЙ РЕЖИМ: ВЫКЛ", TILE, TFG, v ->
                confirmDialog(silent ? "Включить звук?" : "Включить тихий режим?", () -> {
                    am.setRingerMode(silent ? AudioManager.RINGER_MODE_NORMAL : AudioManager.RINGER_MODE_SILENT);
                    say(silent ? "Звук включён." : "Тихий режим включён.");
                    showExtra();
                })));
        String[][] items = {
                {"radio", "📻 РАДИО".substring(2), "radio"}, {"map", "КАРТА", "maps"}, {"music", "МУЗЫКА", "music"},
                {"weather", "ПОГОДА", "weather"}, {"web", "ИНТЕРНЕТ", "web"},
                {"cam", "ФОТО", "cam"}, {"album", "ФОТОАЛЬБОМ", "album"}};
        for (final String[] it : items) {
            c.addView(bigI(it[0], it[1], TILE, TFG, v -> extraClick(it[2])));
        }
        c.addView(bigI("home", "НА ГЛАВНЫЙ", DARK, BG, v -> showMain()));
        setScreen(c);
    }

    void extraClick(String id) {
        switch (id) {
            case "radio": openAppKeyword("радио"); break;
            case "maps": try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0"))); } catch (Exception e) { openAppKeyword("карт"); } break;
            case "music": openAppKeyword("музык"); break;
            case "weather": openAppKeyword("погод"); break;
            case "web":
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))); }
                catch (Exception e) { openAppKeyword("браузер"); }
                break;
            case "cam": try { startActivity(new Intent(MediaStore.ACTION_IMAGE_CAPTURE)); say("Открыл фотоаппарат."); } catch (Exception e) { say("Камера не открылась."); } break;
            case "album": openPhotoalbum(); break;
        }
    }

    void openAppKeyword(String kw) {
        List<ResolveInfo> apps = getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        for (ResolveInfo ri : apps) {
            String label = ri.loadLabel(getPackageManager()).toString().toLowerCase();
            if (label.contains(kw)) {
                Intent li = getPackageManager().getLaunchIntentForPackage(ri.activityInfo.packageName);
                if (li != null) { startActivity(li); say("Открываю: " + ri.loadLabel(getPackageManager())); return; }
            }
        }
        say("Приложение не найдено.");
    }

    // ---------- ВЫБОР ДАТЫ (только для напоминаний) ----------
    void showCalendarPick() {
        if (calCur == null) calCur = Calendar.getInstance();
        renderCalendar();
    }

    void renderCalendar() {
        LinearLayout c = col();
        c.addView(tv("ВЫБОР ДАТЫ", 26, DARK, true));
        LinearLayout hr = row();
        Button prev = big("◀", TILE, TFG, v -> { calCur.add(Calendar.MONTH, -1); renderCalendar(); });
        TextView title = tv(MN[calCur.get(Calendar.MONTH)] + " " + calCur.get(Calendar.YEAR), 20, DARK, true);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        title.setGravity(Gravity.CENTER);
        Button next = big("▶", TILE, TFG, v -> { calCur.add(Calendar.MONTH, 1); renderCalendar(); });
        prev.setLayoutParams(new LinearLayout.LayoutParams(dp(90), ViewGroup.LayoutParams.WRAP_CONTENT));
        next.setLayoutParams(new LinearLayout.LayoutParams(dp(90), ViewGroup.LayoutParams.WRAP_CONTENT));
        hr.addView(prev); hr.addView(title); hr.addView(next);
        c.addView(hr);
        LinearLayout wr = row();
        for (String d : DN) {
            TextView w = tv(d, 15, MUT, true);
            w.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            w.setGravity(Gravity.CENTER);
            wr.addView(w);
        }
        c.addView(wr);
        Calendar first = (Calendar) calCur.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);
        int off = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        int dim = calCur.getActualMaximum(Calendar.DAY_OF_MONTH);
        Calendar today = Calendar.getInstance();
        LinearLayout dr = null;
        int cell = 0;
        for (int i = 0; i < off; i++) {
            if (cell % 7 == 0) { dr = row(); c.addView(dr); }
            TextView e = tv("", 16, DARK, false);
            e.setLayoutParams(new LinearLayout.LayoutParams(0, dp(52), 1f));
            dr.addView(e);
            cell++;
        }
        for (int d = 1; d <= dim; d++) {
            if (cell % 7 == 0) { dr = row(); c.addView(dr); }
            final int day = d;
            boolean isToday = day == today.get(Calendar.DAY_OF_MONTH) && calCur.get(Calendar.MONTH) == today.get(Calendar.MONTH) && calCur.get(Calendar.YEAR) == today.get(Calendar.YEAR);
            Button db = big(String.valueOf(day), isToday ? ACC : TILE, isToday ? "#FFFFFF" : TFG, v -> {
                selDate.set(Calendar.YEAR, calCur.get(Calendar.YEAR));
                selDate.set(Calendar.MONTH, calCur.get(Calendar.MONTH));
                selDate.set(Calendar.DAY_OF_MONTH, day);
                showRemEditor();
            });
            db.setTextSize(18 * FS * SC);
            db.setLayoutParams(new LinearLayout.LayoutParams(0, dp(52), 1f));
            dr.addView(db);
            cell++;
        }
        while (cell % 7 != 0) {
            if (dr == null) { dr = row(); c.addView(dr); }
            TextView e = tv("", 16, DARK, false);
            e.setLayoutParams(new LinearLayout.LayoutParams(0, dp(52), 1f));
            dr.addView(e);
            cell++;
        }
        c.addView(bigI("home", "НА ГЛАВНЫЙ", DARK, BG, v -> showMain()));
        setScreen(c);
    }

    // ---------- НАСТРОЙКИ ----------
    void showSettings() {
        LinearLayout c = col();
        c.addView(tv("⚙ НАСТРОЙКИ ОБОЛОЧКИ", 24, DARK, true));
        c.addView(tv("ТЕМА:", 16, DARK, true));
        LinearLayout rt = row();
        String[] thN = {"ТЁПЛАЯ", "ГАЗЕТА", "БИРЮЗА"};
        for (int i = 0; i < 3; i++) {
            final int ti = i;
            Button b = big(thN[i], P.getInt("theme", 0) == i ? ACC : TILE, P.getInt("theme", 0) == i ? "#FFFFFF" : TFG, x -> { P.edit().putInt("theme", ti).apply(); showMain(); say("Тема изменена."); });
            b.setTextSize(13 * FS * SC);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            b.setLayoutParams(lp);
            rt.addView(b);
        }
        c.addView(rt);
        c.addView(toggleRow("Другие приложения на главном", "showApps", true));
        c.addView(tv("Размер текста:", 16, DARK, true));
        LinearLayout r1 = row();
        String[] fsN = {"МЕЛКИЙ", "СРЕДНИЙ", "КРУПНЫЙ"};
        float[] fsV = {1f, 1.25f, 1.5f};
        for (int i = 0; i < 3; i++) {
            final float v = fsV[i];
            Button b = big(fsN[i], Math.abs(FS - v) < 0.01 ? ACC : TILE, Math.abs(FS - v) < 0.01 ? "#FFFFFF" : TFG, x -> { P.edit().putFloat("fs", v).apply(); FS = v; showSettings(); say("Размер текста изменён."); });
            b.setTextSize(13 * FS * SC);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            b.setLayoutParams(lp);
            r1.addView(b);
        }
        c.addView(r1);
        c.addView(tv("Скорость речи:", 16, DARK, true));
        LinearLayout r2 = row();
        String[] rn = {"МЕДЛЕННО", "ОБЫЧНО", "БЫСТРО"};
        float[] rv = {0.6f, 0.9f, 1.2f};
        for (int i = 0; i < 3; i++) {
            final float v = rv[i];
            Button b = big(rn[i], Math.abs(P.getFloat("rate", 0.75f) - v) < 0.05 ? ACC : TILE, Math.abs(P.getFloat("rate", 0.75f) - v) < 0.05 ? "#FFFFFF" : TFG, x -> { P.edit().putFloat("rate", v).apply(); if (ttsReady) tts.setSpeechRate(v); showSettings(); say("Скорость речи изменена."); });
            b.setTextSize(13 * FS * SC);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            b.setLayoutParams(lp);
            r2.addView(b);
        }
        c.addView(r2);
        c.addView(toggleRow("Ответы вслух", "voice", true));
        c.addView(toggleRow("Щелчки и вибрация кнопок", "sound", true));
        c.addView(bigI("gear", "СИСТЕМНЫЕ НАСТРОЙКИ ANDROID", TILE, TFG, v -> { try { startActivity(new Intent(Settings.ACTION_SETTINGS)); } catch (Exception e) { say("Не удалось открыть."); } }));
        c.addView(bigI("home", "НА ГЛАВНЫЙ", DARK, BG, v -> showMain()));
        setScreen(c);
    }

    LinearLayout toggleRow(String label, String key, boolean def) {
        LinearLayout r = row();
        TextView t = tv(label, 15, DARK, true);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(t);
        boolean on = P.getBoolean(key, def);
        Button b = big(on ? "ВКЛ" : "ВЫКЛ", on ? "#3FAE4C" : "#8A8A8A", "#FFFFFF", x -> { P.edit().putBoolean(key, !P.getBoolean(key, def)).apply(); showSettings(); });
        b.setTextSize(13 * FS * SC);
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(110), ViewGroup.LayoutParams.WRAP_CONTENT));
        r.addView(b);
        return r;
    }

    // ---------- КОМАНДЫ ----------
    void handleCommand(String raw) {
        String t = raw.toLowerCase().replace('ё', 'е');
        int ci = findContact(t);
        if (t.matches(".*(помогите|спасите|скорую|скорая|sos|112|плохо мне).*")) { startSos(); return; }
        if (t.contains("позвони") || t.contains("набери") || t.contains("перезвони")) {
            if (ci >= 0) confirmCall(contacts().get(ci)[0], contacts().get(ci)[1]);
            else { say("Кому звонить? Открываю список."); showContacts(true); } return; }
        if (t.contains("напиши") || t.contains("смс") || t.contains("письмо")) {
            if (ci >= 0) showCompose(ci); else { say("Кому написать? Открываю список."); showContacts(false); } return; }
        if (t.contains("прочитай сообщ") || t.contains("что написали") || t.contains("входящие")) { showSmsList(); return; }
        if (t.contains("фонарик") || t.contains("фонарь") || t.contains("свет")) { toggleTorch(); return; }
        if (t.contains("громче")) { vol(1); return; }
        if (t.contains("тише")) { vol(-1); return; }
        if (t.contains("фотоальбом") || t.contains("галере") || t.contains("покажи фото")) { openPhotoalbum(); return; }
        if (t.contains("сфотографиру") || t.contains("фото") || t.contains("сними")) {
            try { startActivity(new Intent(MediaStore.ACTION_IMAGE_CAPTURE)); say("Открыл фотоаппарат."); } catch (Exception e) { say("Камера не открылась."); } return; }
        if (t.contains("истори") || t.contains("кто звонил")) { showDial(); return; }
        if (t.contains("напомни") || t.contains("напоминан")) {
            if (t.contains("в ") || t.contains("через")) { setReminder(t); } else showReminders(); return; }
        if (t.contains("будильник")) { showAlarms(); return; }
        if (t.contains("настрой")) { showSettings(); return; }
        if (t.contains("набер") || t.contains("номер")) { showDial(); return; }
        if (t.contains("календар")) { openSystemCalendar(); return; }
        if (t.contains("который час") || t.contains("время")) { sayTime(); return; }
        if (t.contains("интернет") || t.contains("браузер")) { extraClick("web"); return; }
        if (t.contains("тихий режим") || t.contains("без звука")) { showExtra(); return; }
        if (t.contains("открой") || t.contains("запусти")) { openApp(t); return; }
        if (t.contains("умеешь") || t.contains("помощь")) { say("Я умею: звонить, писать и читать СМС, набирать номер, показывать историю и фотоальбом, включать фонарь, напоминать, будить и вызывать помощь."); return; }
        if (t.contains("повтори")) { say(lastSay); return; }
        say("Не понял. Скажите: позвони, напиши, напомни, фонарь или помощь.");
    }

    List<String[]> contacts() {
        List<String[]> out = new ArrayList<>();
        String raw = P.getString("contacts", DEF_CONTACTS);
        for (String line : raw.split("\n")) {
            String[] p = line.split("\\|");
            if (p.length == 2) out.add(p);
        }
        return out;
    }

    void saveContacts(List<String[]> cs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cs.size(); i++) { if (i > 0) sb.append("\n"); sb.append(cs.get(i)[0]).append("|").append(cs.get(i)[1]); }
        P.edit().putString("contacts", sb.toString()).apply();
    }

    boolean sim(String a, String b) {
        if (a.length() < 4 || b.length() < 4) return a.equals(b);
        return a.substring(0, 3).equals(b.substring(0, 3));
    }

    int findContact(String t) {
        List<String[]> cs = contacts();
        String[] tw = t.split("[^a-zа-яё0-9]+");
        for (int i = 0; i < cs.size(); i++) {
            String[] nw = cs.get(i)[0].toLowerCase().split("\\s+");
            for (String a : tw) for (String b : nw) if (a.length() >= 4 && sim(a, b)) return i;
        }
        return -1;
    }

    // ---------- КОНТАКТЫ ----------
    void showContacts(boolean callMode) {
        LinearLayout c = col();
        c.addView(tv(callMode ? "КОМУ ЗВОНИМ?" : "КОМУ ПИШЕМ?", 26, DARK, true));
        List<String[]> cs = contacts();
        for (int i = 0; i < cs.size(); i++) {
            final int idx = i;
            LinearLayout rw = row();
            Button nb = big(cs.get(i)[0], TILE, TFG, v -> { if (callMode) confirmCall(cs.get(idx)[0], cs.get(idx)[1]); else showCompose(idx); });
            nb.setTextSize(16 * FS * SC);
            nb.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            rw.addView(nb);
            Button eb = big("✏", "#E09F3E", "#FFFFFF", v -> editContact(idx));
            eb.setLayoutParams(new LinearLayout.LayoutParams(dp(70), ViewGroup.LayoutParams.WRAP_CONTENT));
            rw.addView(eb);
            Button db = big("✕", "#C0392B", "#FFFFFF", v -> confirmDialog("Удалить " + cs.get(idx)[0] + "?", () -> { List<String[]> x = contacts(); x.remove(idx); saveContacts(x); say("Удалил."); showContacts(callMode); }));
            db.setLayoutParams(new LinearLayout.LayoutParams(dp(70), ViewGroup.LayoutParams.WRAP_CONTENT));
            rw.addView(db);
            c.addView(rw);
        }
        c.addView(bigI("mail", "НОВЫЙ НОМЕР", "#3FAE4C", "#FFFFFF", v -> editContact(-1)));
        c.addView(bigI("home", "НА ГЛАВНЫЙ", DARK, BG, v -> showMain()));
        setScreen(c);
    }

    void editContact(int idx) {
        List<String[]> cs = contacts();
        LinearLayout c = col();
        c.addView(tv(idx < 0 ? "НОВЫЙ КОНТАКТ" : "ИЗМЕНИТЬ", 24, DARK, true));
        EditText en = new EditText(this); en.setTextSize(20 * FS * SC); en.setHint("Имя (например: Дочь Маша)");
        if (idx >= 0) en.setText(cs.get(idx)[0]);
        c.addView(en);
        EditText ep = new EditText(this); ep.setTextSize(20 * FS * SC); ep.setHint("Номер (например: +79121234567)");
        ep.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        if (idx >= 0) ep.setText(cs.get(idx)[1]);
        c.addView(ep);
        c.addView(big("💾 СОХРАНИТЬ", "#3FAE4C", "#FFFFFF", v -> {
            String n = en.getText().toString().trim(), p = ep.getText().toString().trim();
            if (n.isEmpty() || p.isEmpty()) { say("Заполните имя и номер."); return; }
            List<String[]> x = contacts();
            if (idx >= 0) x.set(idx, new String[]{n, p}); else x.add(new String[]{n, p});
            saveContacts(x); say("Сохранил: " + n); showContacts(true);
        }));
        c.addView(big("ОТМЕНА", TILE, TFG, v -> showContacts(true)));
        setScreen(c);
    }

    void confirmDialog(String title, Runnable yes) {
        confirmOverlay = new LinearLayout(this);
        confirmOverlay.setOrientation(LinearLayout.VERTICAL);
        confirmOverlay.setGravity(Gravity.CENTER);
        confirmOverlay.setBackgroundColor(Color.parseColor("#CC4A2C17"));
        LinearLayout card = col();
        card.setBackgroundColor(Color.parseColor(CARD));
        card.addView(tv(title, 24, DARK, true));
        card.addView(big("✅ ДА", "#3FAE4C", "#FFFFFF", v -> { removeConfirm(); yes.run(); }));
        card.addView(big("✋ ОТМЕНА", TILE, TFG, v -> removeConfirm()));
        confirmOverlay.addView(card);
        frame.addView(confirmOverlay);
    }

    void removeConfirm() {
        stopConfirmListen();
        pendingConfirm = false;
        if (confirmOverlay != null) { frame.removeView(confirmOverlay); confirmOverlay = null; }
    }

    void confirmCall(String label, String num) {
        pendingNum = num; pendingLabel = label;
        confirmOverlay = new LinearLayout(this);
        confirmOverlay.setOrientation(LinearLayout.VERTICAL);
        confirmOverlay.setGravity(Gravity.CENTER);
        confirmOverlay.setBackgroundColor(Color.parseColor("#CC4A2C17"));
        LinearLayout card = col();
        card.setBackgroundColor(Color.parseColor(CARD));
        card.addView(tv("Звоним: " + label + "?", 26, DARK, true));
        card.addView(tv("Скажите «да» или «нет»", 16, MUT, true));
        card.addView(big("✅ ДА, ЗВОНИ", "#3FAE4C", "#FFFFFF", v -> { removeConfirm(); callNumber(pendingNum, pendingLabel); }));
        card.addView(big("✋ ОТМЕНА", TILE, TFG, v -> { removeConfirm(); say("Отменил."); }));
        confirmOverlay.addView(card);
        frame.addView(confirmOverlay);
        say("Позвонить: " + label + "? Скажите да или нет.");
        pendingConfirm = true;
        H.postDelayed(() -> { if (pendingConfirm) { pendingConfirm = false; startConfirmListen(); } }, 4000);
    }

    void startConfirmListen() {
        if (confirmOverlay == null || cSr != null) return;
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return;
        cSr = SpeechRecognizer.createSpeechRecognizer(this);
        Intent it = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        it.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        it.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
        cSr.setRecognitionListener(new android.speech.RecognitionListener() {
            public void onReadyForSpeech(Bundle p) {}
            public void onBeginningOfSpeech() {}
            public void onRmsChanged(float v) {}
            public void onBufferReceived(byte[] b) {}
            public void onEndOfSpeech() {}
            public void onError(int e) { H.postDelayed(() -> { stopConfirmListen(); if (confirmOverlay != null) startConfirmListen(); }, 400); }
            public void onResults(Bundle r) {
                ArrayList<String> a = r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String t = (a != null && !a.isEmpty()) ? a.get(0).toLowerCase() : "";
                H.post(() -> {
                    if (confirmOverlay == null) return;
                    stopConfirmListen();
                    if (t.contains("нет") || t.contains("отмен") || t.contains("не надо")) { removeConfirm(); say("Отменил."); }
                    else if (t.contains("да") || t.contains("звони") || t.contains("конечно") || t.contains("ага")) { String n = pendingNum, l = pendingLabel; removeConfirm(); callNumber(n, l); }
                    else { say("Скажите да или нет."); H.postDelayed(() -> { if (confirmOverlay != null) startConfirmListen(); }, 1200); }
                });
            }
            public void onPartialResults(Bundle r) {}
            public void onEvent(int i, Bundle b) {}
        });
        try { cSr.startListening(it); } catch (Exception e) {}
    }

    void callNumber(String num, String label) {
        if (checkSelfPermission(android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            try { startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + num))); say("Звоню: " + label); return; } catch (Exception e) {}
        }
        try { startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + num))); } catch (Exception e) {}
        say("Нет разрешения на звонки. Нажмите кнопку вызова или разрешите звонки в настройках.");
    }

    // ---------- НАБОР + ИСТОРИЯ ----------
    void showDial() {
        LinearLayout c = col();
        LinearLayout tab = row();
        final boolean[] hist = {false};
        Button tb1 = bigI("dial", "НАБОР", ACC, "#FFFFFF", v -> { hist[0] = false; renderDialBody(c, hist); });
        Button tb2 = bigI("clock", "ИСТОРИЯ", TILE, TFG, v -> { hist[0] = true; renderDialBody(c, hist); });
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tp.setMargins(dp(3), 0, dp(3), 0);
        tb1.setLayoutParams(tp); tb2.setLayoutParams(tp);
        tab.addView(tb1); tab.addView(tb2);
        c.addView(tab);
        renderDialBody(c, hist);
        setScreen(c);
    }

    void renderDialBody(LinearLayout c, boolean[] hist) {
        for (int i = c.getChildCount() - 1; i >= 1; i--) c.removeViewAt(i);
        if (hist[0]) {
            c.addView(tv("ИСТОРИЯ ЗВОНКОВ", 22, DARK, true));
            try {
                Cursor cur = getContentResolver().query(CallLog.Calls.CONTENT_URI,
                        new String[]{CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE},
                        null, null, CallLog.Calls.DATE + " DESC LIMIT 15");
                if (cur != null) {
                    while (cur.moveToNext()) {
                        String num = cur.getString(0); long date = cur.getLong(1); int type = cur.getInt(2);
                        String arrow = type == CallLog.Calls.OUTGOING_TYPE ? "→" : type == CallLog.Calls.MISSED_TYPE ? "✗" : "←";
                        String color = type == CallLog.Calls.MISSED_TYPE ? "#C0392B" : TFG;
                        String name = nameForNumber(num);
                        Calendar cd = Calendar.getInstance(); cd.setTimeInMillis(date);
                        String when = String.format(Locale.getDefault(), "%02d:%02d %02d.%02d", cd.get(Calendar.HOUR_OF_DAY), cd.get(Calendar.MINUTE), cd.get(Calendar.DAY_OF_MONTH), cd.get(Calendar.MONTH) + 1);
                        final String fnum = num;
                        Button b = big(arrow + " " + name + "\n" + when, TILE, color, v -> { blink(callTile, false); P.edit().putLong("lastMissed", System.currentTimeMillis()).apply(); confirmCall(name, fnum); });
                        b.setTextSize(15 * FS * SC);
                        c.addView(b);
                    }
                    cur.close();
                }
            } catch (Exception e) { c.addView(tv("История недоступна", 16, "#C0392B", true)); }
        } else {
            TextView disp = tv("", 34, DARK, true);
            disp.setGravity(Gravity.CENTER);
            disp.setMinHeight(dp(70));
            c.addView(disp);
            View spacer = new View(this);
            spacer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(30)));
            c.addView(spacer);
            final StringBuilder cur = new StringBuilder();
            String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"};
            for (int r = 0; r < 4; r++) {
                LinearLayout rw = row();
                for (int k = 0; k < 3; k++) {
                    String key = keys[r * 3 + k];
                    Button b = big(key, TILE, TFG, v -> { cur.append(key); disp.setText(cur.toString()); });
                    b.setTextSize(26 * FS * SC);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    lp.setMargins(dp(4), dp(3), dp(4), dp(3));
                    b.setLayoutParams(lp);
                    rw.addView(b);
                }
                c.addView(rw);
            }
            LinearLayout rw = row();
            Button del = big("⌫", "#8A8A8A", "#FFFFFF", v -> { if (cur.length() > 0) cur.deleteCharAt(cur.length() - 1); disp.setText(cur.toString()); });
            Button call = bigI("phone", "ПОЗВОНИТЬ", "#3FAE4C", "#FFFFFF", v -> { if (cur.length() > 0) confirmCall(cur.toString(), cur.toString()); });
            del.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            call.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f));
            rw.addView(del); rw.addView(call);
            c.addView(rw);
        }
        c.addView(bigI("home", "НА ГЛАВНЫЙ", DARK, BG, v -> showMain()));
    }

    String nameForNumber(String num) {
        if (num == null) return "?";
        String clean = num.replaceAll("[^0-9]", "");
        for (String[] cc : contacts()) {
            String ccn = cc[1].replaceAll("[^0-9]", "");
            if (clean.length() >= 7 && ccn.length() >= 7 && clean.endsWith(ccn.substring(ccn.length() - 7))) return cc[0];
        }
        return num;
    }

    // ---------- СМС ----------
    void showSmsList() {
        LinearLayout c = col();
        c.addView(tv("ВХОДЯЩИЕ СООБЩЕНИЯ", 26, DARK, true));
        c.addView(bigI("mail", "НАПИСАТЬ НОВОЕ", "#3FAE4C", "#FFFFFF", v -> showContacts(false)));
        try {
            Cursor cur = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, "date DESC LIMIT 10");
            if (cur != null) {
                while (cur.moveToNext()) {
                    String addr = cur.getString(cur.getColumnIndexOrThrow("address"));
                    String body = cur.getString(cur.getColumnIndexOrThrow("body"));
                    String shortB = body.length() > 60 ? body.substring(0, 60) + "…" : body;
                    Button b = big("От: " + nameForNumber(addr) + "\n" + shortB, TILE, TFG, v -> say("Сообщение от " + nameForNumber(addr) + ": " + body));
                    b.setTextSize(15 * FS * SC);
                    c.addView(b);
                }
                cur.close();
            }
        } catch (Exception e) { c.addView(tv("СМС недоступны", 16, "#C0392B", true)); }
        c.addView(bigI("home", "НА ГЛАВНЫЙ", DARK, BG, v -> showMain()));
        setScreen(c);
    }

    void showCompose(int i) {
        List<String[]> cs = contacts();
        stopCompListen();
        composeIdx = i;
        LinearLayout c = col();
        c.addView(tv("ПИШЕМ: " + cs.get(i)[0], 24, DARK, true));
        composeEt = new EditText(this);
        composeEt.setTextSize(20 * FS * SC); composeEt.setMinLines(3);
        composeEt.setHint("Говорите текст — я запишу сам");
        c.addView(composeEt);
        c.addView(tv("Скажите «отправить» или «отмена»", 14, MUT, false));
        c.addView(bigI("sound", "ПРОЧИТАТЬ ВСЛУХ", TILE, TFG, v -> speak(composeEt.getText().toString())));
        composeSend = () -> {
            stopCompListen();
            sendSms(cs.get(composeIdx)[1], composeEt.getText().toString());
            say("Отправлено: " + cs.get(composeIdx)[0]);
            showMain();
        };
        composeCancel = () -> { stopCompListen(); showMain(); };
        c.addView(bigI("mail", "ОТПРАВИТЬ", "#3FAE4C", "#FFFFFF", v -> composeSend.run()));
        c.addView(big("ОТМЕНА", TILE, TFG, v -> composeCancel.run()));
        setScreen(c);
        say("Кому пишем: " + cs.get(i)[0] + ". Говорите текст, я запишу.");
        H.postDelayed(this::startCompListen, 800);
    }

    void startCompListen() {
        if (!compListen && compSr != null) return;
        if (compSr != null) return;
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return;
        compListen = true;
        compSr = SpeechRecognizer.createSpeechRecognizer(this);
        Intent it = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        it.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        it.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
        it.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        compSr.setRecognitionListener(new android.speech.RecognitionListener() {
            public void onReadyForSpeech(Bundle p) {}
            public void onBeginningOfSpeech() {}
            public void onRmsChanged(float v) {}
            public void onBufferReceived(byte[] b) {}
            public void onEndOfSpeech() {}
            public void onError(int e) { H.post(() -> { stopCompListen(); if (composeEt != null && compListenOrWanted()) H.postDelayed(() -> startCompListen(), 500); }); }
            public void onResults(Bundle r) {
                ArrayList<String> a = r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                final String t = (a != null && !a.isEmpty()) ? a.get(0) : "";
                H.post(() -> {
                    stopCompListen();
                    if (composeEt == null) return;
                    String tl = t.toLowerCase();
                    if (tl.contains("отправ")) { if (composeSend != null) composeSend.run(); return; }
                    if (tl.contains("отмен")) { if (composeCancel != null) composeCancel.run(); return; }
                    if (tl.contains("прочитай вслух")) speak(composeEt.getText().toString());
                    else if (!t.isEmpty()) {
                        String old = composeEt.getText().toString();
                        composeEt.setText(old.isEmpty() ? t : old + " " + t);
                        composeEt.setSelection(composeEt.getText().length());
                    }
                    H.postDelayed(() -> startCompListen(), 500);
                });
            }
            public void onPartialResults(Bundle r) {}
            public void onEvent(int i, Bundle b) {}
        });
        try { compSr.startListening(it); } catch (Exception e) { compSr = null; }
    }

    boolean compListenOrWanted() { return composeEt != null && compSendActive(); }
    boolean compSendActive() { return composeSend != null; }

    void sendSms(String num, String text) {
        try { SmsManager.getDefault().sendTextMessage(num, null, text, null, null); } catch (Exception e) { say("СМС не ушло."); }
    }

    // ---------- НАПОМИНАНИЯ ----------
    List<String[]> rems() {
        List<String[]> out = new ArrayList<>();
        String raw = P.getString("rems", "");
        if (raw.isEmpty()) return out;
        for (String line : raw.split("\n")) {
            String[] p = line.split("\\|");
            if (p.length >= 2) out.add(p);
        }
        return out;
    }

    void saveRems(List<String[]> r) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < r.size(); i++) { if (i > 0) sb.append("\n"); sb.append(String.join("|", r.get(i))); }
        P.edit().putString("rems", sb.toString()).apply();
    }

    int addReminder(long millis, String text) {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent in = new Intent(this, ReminderReceiver.class).putExtra("text", text);
        int code = (int) (millis % 1000000);
        PendingIntent pi = PendingIntent.getBroadcast(this, code, in, PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi);
        else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi);
        List<String[]> r = rems();
        r.add(new String[]{String.valueOf(millis), text, String.valueOf(code), "rem"});
        saveRems(r);
        return code;
    }

    void showRemList(LinearLayout c, String kind) {
        List<String[]> r = rems();
        long now = System.currentTimeMillis();
        boolean any = false;
        for (int i = r.size() - 1; i >= 0; i--) {
            String k = r.get(i).length > 3 ? r.get(i)[3] : "rem";
            if (!k.equals(kind)) continue;
            long ms = Long.parseLong(r.get(i)[0]);
            if (ms <= now) continue;
            any = true;
            Calendar cd = Calendar.getInstance(); cd.setTimeInMillis(ms);
            String when = String.format(Locale.getDefault(), "%02d:%02d %02d.%02d", cd.get(Calendar.HOUR_OF_DAY), cd.get(Calendar.MINUTE), cd.get(Calendar.DAY_OF_MONTH), cd.get(Calendar.MONTH) + 1);
            final int idx = i;
            final int code = r.get(i).length > 2 ? Integer.parseInt(r.get(i)[2]) : 0;
            LinearLayout rw = row();
            Button b = big(when + "\n" + r.get(i)[1] + "\n" + until(ms), TILE, TFG, v -> say("Напоминание: " + r.get(idx)[1] + " в " + when));
            b.setTextSize(14 * FS * SC);
            b.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            rw.addView(b);
            Button db = big("✕", "#C0392B", "#FFFFFF", v -> {
                AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                am.cancel(PendingIntent.getBroadcast(this, code, new Intent(this, ReminderReceiver.class), PendingIntent.FLAG_IMMUTABLE));
                List<String[]> x = rems(); x.remove(idx); saveRems(x); say("Убрал."); showReminders();
            });
            db.setLayoutParams(new LinearLayout.LayoutParams(dp(70), ViewGroup.LayoutParams.WRAP_CONTENT));
            rw.addView(db);
            c.addView(rw);
        }
        if (!any) c.addView(tv("(пока ничего нет)", 15, MUT, false));
    }

    void showReminders() {
        LinearLayout c = col();
        c.addView(tv("НАПОМИНАНИЯ", 26, DARK, true));
        c.addView(bigI("memo", "СОЗДАТЬ НАПОМИНАНИЕ", "#3FAE4C", "#FFFFFF", v -> { selDate = Calendar.getInstance(); showRemEditor(); }));
        c.addView(bigI("memo", "Через час: таблетки", TILE, TFG, v -> { addReminder(System.currentTimeMillis() + 3600000, "Пора принять таблетки!"); say("Напомню через час."); showReminders(); }));
        c.addView(bigI("memo", "Утром в 9:00: таблетки", TILE, TFG, v -> { addReminder(atTime(9, 0), "Пора принять таблетки!"); say("Напомню утром в девять."); showReminders(); }));
        c.addView(tv("Уже стоит:", 16, MUT, true));
        showRemList(c, "rem");
        c.addView(bigI("home", "НА ГЛАВНЫЙ", DARK, BG, v -> showMain()));
        setScreen(c);
    }

    void showRemEditor() {
        LinearLayout c = col();
        c.addView(tv("НОВОЕ НАПОМИНАНИЕ", 24, DARK, true));
        final EditText en = new EditText(this); en.setTextSize(20 * FS * SC); en.setHint("Название (например: таблетки)");
        c.addView(en);
        final NumberPicker nh = new NumberPicker(this); nh.setMinValue(0); nh.setMaxValue(23); nh.setValue(9);
        final NumberPicker nm = new NumberPicker(this); nm.setMinValue(0); nm.setMaxValue(55); nm.setValue(0);
        String[] mins = new String[60];
        for (int i = 0; i < 60; i += 5) mins[i] = String.format("%02d", i);
        for (int i = 0; i < 60; i++) if (mins[i] == null) mins[i] = "";
        nm.setDisplayedValues(mins);
        LinearLayout rw = row();
        rw.setGravity(Gravity.CENTER);
        nh.setLayoutParams(new LinearLayout.LayoutParams(dp(100), dp(150)));
        nm.setLayoutParams(new LinearLayout.LayoutParams(dp(100), dp(150)));
        rw.addView(nh); rw.addView(nm);
        c.addView(rw);
        c.addView(bigI("clock", "Дата: " + selDate.get(Calendar.DAY_OF_MONTH) + "." + (selDate.get(Calendar.MONTH) + 1) + "." + selDate.get(Calendar.YEAR), TILE, TFG, v -> showCalendarPick()));
        c.addView(big("💾 СОХРАНИТЬ", "#3FAE4C", "#FFFFFF", v -> {
            String name = en.getText().toString().trim();
            if (name.isEmpty()) name = "Напоминание";
            Calendar cc = (Calendar) selDate.clone();
            cc.set(Calendar.HOUR_OF_DAY, nh.getValue());
            cc.set(Calendar.MINUTE, nm.getValue() * 5);
            cc.set(Calendar.SECOND, 0);
            if (cc.before(Calendar.getInstance())) cc.add(Calendar.DAY_OF_YEAR, 1);
            addReminder(cc.getTimeInMillis(), name);
            say("Напоминание создано.");
            showReminders();
        }));
        c.addView(big("ОТМЕНА", TILE, TFG, v -> showReminders()));
        setScreen(c);
    }

    // ---------- БУДИЛЬНИК ----------
    long nextAlarm(int h, int m, int mask) {
        Calendar now = Calendar.getInstance();
        for (int d = 0; d < 8; d++) {
            Calendar c = (Calendar) now.clone();
            c.add(Calendar.DAY_OF_YEAR, d);
            c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, m); c.set(Calendar.SECOND, 0);
            if (c.before(now)) continue;
            int wd = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
            if (mask == 0 || (mask & (1 << wd)) != 0) return c.getTimeInMillis();
        }
        return 0;
    }

    String until(long ms) {
        long diff = ms - System.currentTimeMillis();
        if (diff < 0) return "";
        long d = diff / 86400000, h = (diff % 86400000) / 3600000, m = (diff % 3600000) / 60000;
        if (d > 0) return "через " + d + " д " + h + " ч " + m + " мин";
        if (h > 0) return "через " + h + " ч " + m + " мин";
        return "через " + m + " мин";
    }

    int addAlarm(int h, int m, int mask, String text) {
        long ms = nextAlarm(h, m, mask);
        if (ms == 0) return 0;
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent in = new Intent(this, ReminderReceiver.class).putExtra("text", text).putExtra("ah", h).putExtra("am", m).putExtra("mask", mask);
        int code = (int) (ms % 1000000);
        PendingIntent pi = PendingIntent.getBroadcast(this, code, in, PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi);
        else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, pi);
        List<String[]> r = rems();
        r.add(new String[]{String.valueOf(ms), text, String.valueOf(code), "alm", h + ":" + m + ":" + mask});
        saveRems(r);
        return code;
    }

    void showAlarms() {
        LinearLayout c = col();
        c.addView(tv("БУДИЛЬНИК", 26, DARK, true));
        final NumberPicker nh = new NumberPicker(this); nh.setMinValue(0); nh.setMaxValue(23); nh.setValue(7);
        final NumberPicker nm = new NumberPicker(this); nm.setMinValue(0); nm.setMaxValue(55); nm.setValue(0);
        String[] mins = new String[60];
        for (int i = 0; i < 60; i += 5) mins[i] = String.format("%02d", i);
        for (int i = 0; i < 60; i++) if (mins[i] == null) mins[i] = "";
        nm.setDisplayedValues(mins);
        LinearLayout rw = row();
        rw.setGravity(Gravity.CENTER);
        nh.setLayoutParams(new LinearLayout.LayoutParams(dp(100), dp(150)));
        nm.setLayoutParams(new LinearLayout.LayoutParams(dp(100), dp(150)));
        rw.addView(nh); rw.addView(nm);
        c.addView(rw);
        c.addView(tv("Дни недели:", 16, DARK, true));
        final LinearLayout days = row();
        for (int i = 0; i < 7; i++) {
            final int bit = i;
            Button db = big(DN[i], TILE, TFG, v -> {
                alarmMask ^= (1 << bit);
                v.setBackgroundColor(Color.parseColor((alarmMask & (1 << bit)) != 0 ? "#3FAE4C" : TILE));
            });
            db.setTextSize(13 * FS * SC);
            db.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            days.addView(db);
        }
        c.addView(days);
        c.addView(tv("(ничего не нажато = каждый день)", 13, MUT, false));
        c.addView(bigI("alarm", "ПОСТАВИТЬ БУДИЛЬНИК", "#3FAE4C", "#FFFFFF", v -> {
            int m = nm.getValue() * 5;
            long ms = nextAlarm(nh.getValue(), m, alarmMask);
            addAlarm(nh.getValue(), m, alarmMask, "Будильник! Пора вставать!");
            say("Будильник поставлен. " + until(ms));
            showAlarms();
        }));
        c.addView(tv("Уже стоит:", 16, MUT, true));
        List<String[]> r = rems();
        boolean any = false;
        for (int i = r.size() - 1; i >= 0; i--) {
            if (r.get(i).length < 4 || !r.get(i)[3].equals("alm")) continue;
            any = true;
            long ms = Long.parseLong(r.get(i)[0]);
            String[] p = r.get(i)[4].split(":");
            String dstr = p.length > 2 && !p[2].equals("0") ? daysStr(Integer.parseInt(p[2])) : "каждый день";
            final int idx = i;
            final int code = Integer.parseInt(r.get(i)[2]);
            LinearLayout rw2 = row();
            Button b = big(p[0] + ":" + p[1] + " • " + dstr + "\n" + until(ms), TILE, TFG, v -> say("Будильник в " + p[0] + ":" + p[1] + ", " + dstr + ". " + until(ms)));
            b.setTextSize(14 * FS * SC);
            b.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            rw2.addView(b);
            Button db = big("✕", "#C0392B", "#FFFFFF", v -> {
                AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                am.cancel(PendingIntent.getBroadcast(this, code, new Intent(this, ReminderReceiver.class), PendingIntent.FLAG_IMMUTABLE));
                List<String[]> x = rems(); x.remove(idx); saveRems(x); say("Выключил будильник."); showAlarms();
            });
            db.setLayoutParams(new LinearLayout.LayoutParams(dp(70), ViewGroup.LayoutParams.WRAP_CONTENT));
            rw2.addView(db);
            c.addView(rw2);
        }
        if (!any) c.addView(tv("(пока ничего нет)", 15, MUT, false));
        c.addView(bigI("home", "НА ГЛАВНЫЙ", DARK, BG, v -> showMain()));
        setScreen(c);
    }

    String daysStr(int mask) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) if ((mask & (1 << i)) != 0) sb.append(DN[i]).append(" ");
        return sb.toString().trim();
    }

    long atTime(int h, int m) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, m); c.set(Calendar.SECOND, 0);
        if (c.before(Calendar.getInstance())) c.add(Calendar.DAY_OF_YEAR, 1);
        return c.getTimeInMillis();
    }

    void setReminder(String t) {
        Matcher m = Pattern.compile("в\\s*(\\d{1,2})").matcher(t);
        if (!m.find()) { say("Скажите время, например: напомни в 9 утра."); return; }
        int h = Integer.parseInt(m.group(1));
        if ((t.contains("вечера") || t.contains("ночи")) && h < 12) h += 12;
        String text = "Пора принять таблетки!";
        Matcher tm = Pattern.compile("напомни\\s*(.*)").matcher(t);
        if (tm.find() && !tm.group(1).trim().isEmpty()) text = tm.group(1).trim();
        addReminder(atTime(h, 0), text);
        say("Запомнил. Напомню в " + h + " часов на весь экран.");
    }

    public static class ReminderReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context c, Intent i) {
            if (i.hasExtra("ah")) {
                int h = i.getIntExtra("ah", 7), m = i.getIntExtra("am", 0), mask = i.getIntExtra("mask", 0);
                Calendar now = Calendar.getInstance();
                for (int d = 0; d < 8; d++) {
                    Calendar cc = (Calendar) now.clone();
                    cc.add(Calendar.DAY_OF_YEAR, d);
                    cc.set(Calendar.HOUR_OF_DAY, h); cc.set(Calendar.MINUTE, m); cc.set(Calendar.SECOND, 0);
                    if (cc.before(now)) continue;
                    int wd = (cc.get(Calendar.DAY_OF_WEEK) + 5) % 7;
                    if (mask == 0 || (mask & (1 << wd)) != 0) {
                        AlarmManager am = (AlarmManager) c.getSystemService(ALARM_SERVICE);
                        Intent ni = new Intent(c, ReminderReceiver.class).putExtra("text", i.getStringExtra("text")).putExtra("ah", h).putExtra("am", m).putExtra("mask", mask);
                        int code = (int) (cc.getTimeInMillis() % 1000000);
                        PendingIntent pi = PendingIntent.getBroadcast(c, code, ni, PendingIntent.FLAG_IMMUTABLE);
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cc.getTimeInMillis(), pi);
                        break;
                    }
                }
            }
            Intent in = new Intent(c, MainActivity.class);
            in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            in.putExtra("reminder", i.getStringExtra("text"));
            c.startActivity(in);
        }
    }

    void showReminder(String text) {
        if (remOverlay != null) return;
        remOverlay = new LinearLayout(this);
        remOverlay.setOrientation(LinearLayout.VERTICAL);
        remOverlay.setGravity(Gravity.CENTER);
        remOverlay.setBackgroundColor(Color.parseColor(BG));
        remOverlay.setPadding(dp(30), dp(30), dp(30), dp(30));
        remOverlay.addView(tv("💊", 70, "#C0392B", false));
        TextView bt = tv(text.toUpperCase(), 36, "#C0392B", true);
        bt.setGravity(Gravity.CENTER);
        remOverlay.addView(bt);
        remOverlay.addView(big("✅ ПРИНЯЛ", "#3FAE4C", "#FFFFFF", v -> { frame.removeView(remOverlay); remOverlay = null; say("Молодец! Отметил."); }));
        remOverlay.addView(bigI("alarm", "НАПОМНИ ЧЕРЕЗ ЧАС", TILE, TFG, v -> {
            frame.removeView(remOverlay); remOverlay = null;
            addReminder(System.currentTimeMillis() + 3600000, text);
            say("Напомню ещё раз через час.");
        }));
        frame.addView(remOverlay);
        try { Vibrator vb = (Vibrator) getSystemService(VIBRATOR_SERVICE); vb.vibrate(VibrationEffect.createOneShot(800, 255)); } catch (Exception e) {}
        say("Внимание! " + text);
    }

    // ---------- SOS ----------
    void startSos() {
        LinearLayout c = col();
        c.addView(tv("ВЫЗЫВАЕМ ПОМОЩЬ!", 28, "#C0392B", true));
        TextView num = tv("5", 90, "#C0392B", true);
        c.addView(num);
        c.addView(tv("Если случайно — жмите ОТМЕНА", 18, DARK, true));
        c.addView(big("✋ ОТМЕНА", "#8A8A8A", "#FFFFFF", v -> cancelSos()));
        setScreen(c);
        say("Внимание! Вызываю помощь через пять секунд. Если случайно — отмена.");
        final int[] n = {5};
        sosActive = true;
        sosTicker = new Runnable() {
            public void run() {
                if (!sosActive) return;
                n[0]--;
                if (n[0] <= 0) { sosFire(); return; }
                num.setText(String.valueOf(n[0]));
                H.postDelayed(this, 1000);
            }
        };
        H.postDelayed(sosTicker, 1000);
    }

    void cancelSos() {
        sosActive = false;
        if (sosTicker != null) H.removeCallbacks(sosTicker);
        sosTicker = null;
        say("Отменили. Всё хорошо, помощь не вызывали.");
        showMain();
    }

    void sosFire() {
        sosActive = false;
        String loc = locText();
        String sms = "SOS! Нужна помощь срочно! " + loc;
        List<String[]> cs = contacts();
        for (int i = 0; i < 3 && i < cs.size(); i++) sendSms(cs.get(i)[1], sms);
        callNumber("112", "служба спасения");
        LinearLayout c = col();
        c.addView(tv("☎ ИДЁТ ЗВОНОК В 112…", 26, "#C0392B", true));
        c.addView(tv("СМС отправлено родным:", 18, DARK, true));
        c.addView(tv(sms, 16, DARK, true));
        c.addView(bigI("home", "НА ГЛАВНЫЙ", DARK, BG, v -> showMain()));
        setScreen(c);
        say("Вызываю сто двенадцать и отправляю сообщение родным!");
    }

    String locText() {
        try {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location l = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (l == null) l = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (l == null) return "Спутники не ловятся, точное место неизвестно.";
            return String.format(Locale.US, "Широта: %.6f Долгота: %.6f https://maps.google.com/?q=%.6f,%.6f", l.getLatitude(), l.getLongitude(), l.getLatitude(), l.getLongitude());
        } catch (Exception e) { return "Спутники не ловятся, точное место неизвестно."; }
    }

    // ---------- ПРИЛОЖЕНИЯ ----------
    List<Object[]> installedApps() {
        List<Object[]> out = new ArrayList<>();
        String myPkg = getPackageName();
        List<ResolveInfo> apps = getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        for (ResolveInfo ri : apps) {
            if (ri.activityInfo.packageName.equals(myPkg)) continue;
            String label = ri.loadLabel(getPackageManager()).toString();
            if (label.length() > 14) label = label.substring(0, 12) + "…";
            try { out.add(new Object[]{label, ri.loadIcon(getPackageManager()), ri.activityInfo.packageName}); }
            catch (Exception e) { out.add(new Object[]{label, null, ri.activityInfo.packageName}); }
        }
        Collections.sort(out, (a, b) -> ((String) a[0]).compareToIgnoreCase((String) b[0]));
        if (out.size() > 24) out = out.subList(0, 24);
        return out;
    }

    void openPackage(String pkg) {
        try {
            Intent li = getPackageManager().getLaunchIntentForPackage(pkg);
            if (li != null) startActivity(li); else say("Не удалось открыть.");
        } catch (Exception e) { say("Не удалось открыть."); }
    }

    // ---------- БАТАРЕЯ ----------
    class BatteryReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context c, Intent i) {
            int level = i.getIntExtra("level", -1), scale = i.getIntExtra("scale", -1);
            int plugged = i.getIntExtra("plugged", 0);
            charging = plugged != 0;
            if (level >= 0 && scale > 0) lastPct = level * 100 / scale;
            refreshStatus();
            if (level < 0 || scale <= 0) return;
            int pct = lastPct;
            if (plugged != 0) {
                if (battOverlay != null) { frame.removeView(battOverlay); battOverlay = null; say("Зарядка началась. Спасибо!"); }
                battWarned = false;
                return;
            }
            if (pct <= 5) { showBattOverlay(); return; }
            if (pct <= 15 && !battWarned) { battWarned = true; say("Батарея пятнадцать процентов. Поставьте телефон на зарядку, пожалуйста."); }
        }
    }

    void showBattOverlay() {
        if (battOverlay != null) return;
        battOverlay = new LinearLayout(this);
        battOverlay.setOrientation(LinearLayout.VERTICAL);
        battOverlay.setGravity(Gravity.CENTER);
        battOverlay.setBackgroundColor(Color.parseColor("#C0392B"));
        battOverlay.setPadding(dp(30), dp(30), dp(30), dp(30));
        battOverlay.addView(tv("🪫", 70, "#FFFFFF", false));
        TextView t = tv("ТЕЛЕФОН СЕЙЧАС ВЫКЛЮЧИТСЯ!\nПОСТАВЬТЕ НА ЗАРЯДКУ!", 32, "#FFFFFF", true);
        t.setGravity(Gravity.CENTER);
        battOverlay.addView(t);
        battOverlay.addView(tv("Это окно исчезнет само, когда начнётся зарядка", 16, "#FFE0D8", true));
        frame.addView(battOverlay);
        say("Внимание! Батарея почти села! Поставьте телефон на зарядку!");
    }

    // ---------- ПРОЧЕЕ ----------
    void toggleTorch() {
        try {
            CameraManager cm = (CameraManager) getSystemService(CAMERA_SERVICE);
            String[] ids = cm.getCameraIdList();
            torchOn = !torchOn;
            cm.setTorchMode(ids[0], torchOn);
            say(torchOn ? "Фонарь включил." : "Фонарь выключил.");
        } catch (Exception e) { say("Фонарь не включается."); }
    }

    void vol(int dir) {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir > 0 ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
        say(dir > 0 ? "Громче." : "Тише.");
    }

    void openApp(String t) {
        try {
            String token = t.replaceAll(".*(открой|запусти)\\s*", "").trim();
            if (token.isEmpty()) { say("Какое приложение открыть?"); return; }
            String[] words = token.split("\\s+");
            List<ResolveInfo> apps = getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
            for (ResolveInfo ri : apps) {
                String label = ri.loadLabel(getPackageManager()).toString().toLowerCase();
                for (String w : words) if (w.length() > 3 && label.contains(w)) {
                    Intent li = getPackageManager().getLaunchIntentForPackage(ri.activityInfo.packageName);
                    if (li != null) { startActivity(li); say("Открываю: " + ri.loadLabel(getPackageManager())); return; }
                }
            }
            say("Не нашёл такое приложение.");
        } catch (Exception e) { say("Не смог открыть."); }
    }

    void sayTime() {
        Calendar c = Calendar.getInstance();
        String[] days = {"воскресенье", "понедельник", "вторник", "среда", "четверг", "пятница", "суббота"};
        String[] mons = {"января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"};
        say("Сейчас " + c.get(Calendar.HOUR_OF_DAY) + " часов " + c.get(Calendar.MINUTE) + " минут. " + days[c.get(Calendar.DAY_OF_WEEK) - 1] + ", " + c.get(Calendar.DAY_OF_MONTH) + " " + mons[c.get(Calendar.MONTH)] + ".");
    }

    @Override public void onBackPressed() {
        if (confirmOverlay != null) { removeConfirm(); return; }
        showMain();
    }

    @Override protected void onDestroy() {
        try { unregisterReceiver(battRec); } catch (Exception e) {}
        try { unregisterReceiver(smsRec); } catch (Exception e) {}
        stopListening(); stopConfirmListen(); stopCompListen();
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }
            }
