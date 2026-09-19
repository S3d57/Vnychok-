package ru.vnuchok.app;

// --- СИСТЕМНЫЕ ИМПОРТЫ ---
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
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.CallLog;
import android.provider.ContactsContract;
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
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* =====================================================================
   ВНУЧОК — оболочка Android для пенсионеров и слабовидящих.
   Весь визуал живёт в Art.java. Этот файл — только логика и раскладка.
   Главный экран НЕ прокручивается: сцена фоном, поверх пилюли,
   голосовая карточка от края до края, плитки и SOS закреплены.
   ===================================================================== */
public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    /* --- ХОЛДЕР для мигалок и анимаций --- */
    static class THolder { String bg; ObjectAnimator oa; }

    /* --- КОНСТАНТЫ --- */
    static final String DEF_CONTACTS = "Дочь Маша|+79000000001\nВнук Миша|+7900000002\nВнучка Оля|+7900000003\nСоседка Нина|+7900000004\nВрач Ирина|+7900000005";
    static final String DEF_TILES = "call,sms,apps,rem,alarm,torch";
    static final String[] DN = {"ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС"};
    static final String[] MN = {"ЯНВАРЬ", "ФЕВРАЛЬ", "МАРТ", "АПРЕЛЬ", "МАЙ", "ИЮНЬ", "ИЮЛЬ", "АВГУСТ", "СЕНТЯБРЬ", "ОКТЯБРЬ", "НОЯБРЬ", "ДЕКАБРЬ"};
    static final String[] THN = {"РЕТРО", "ГОРЧИЦА", "НОЧЬ", "КАРАМЕЛЬ"};

    /* --- СОСТОЯНИЕ --- */
    Handler H = new Handler(Looper.getMainLooper());
    FrameLayout frame, midFrame, mainSlot; ScrollView scroll; LinearLayout rootLin, headerSlot, bottomSlot;
    TextView caption, userSay, timeView, sbPopup, sbBatt, sbSig, sbOper, sbNet, weatherText;
    Button micBtn, msgTile, callTile, dlgMic, dlgClose;
    LinearLayout voiceCard, idleRow, dlgCol;
    boolean dlgOpen = false;
    static boolean curtainDone = false;
    TextToSpeech tts; boolean ttsReady;
    SpeechRecognizer sr, cSr, compSr;
    boolean torchOn, battWarned, listening, pendingConfirm, charging, permAsked, composeActive;
    int lastPct = -1, sigBars = 3; String sigLabel = "";
    int contactShown = 30;
    String lastSmsStatus = "", weatherLabel = "—";
    String screenTitle = "";
    TextView smsStatusView;
    LinearLayout battOverlay, remOverlay, confirmOverlay;
    String lastSay = "", pendingNum = "", pendingLabel = "";
    BatteryReceiver battRec; BroadcastReceiver smsRec;
    Runnable clockRun, missRun, sigRun;
    SharedPreferences P;
    float FS = 1f, SC = 1f;
    Runnable sosTicker; volatile boolean sosActive;
    EditText composeEt;
    String composeName = "", composeNum = "";
    Runnable composeSend, composeCancel;
    Calendar selDate, calCur;
    int alarmMask = 0;
    String bookText = null; List<String> bookSents = null; int bookIdx = 0; boolean bookPlaying = false;

    /* =================================================================
       ЖИЗНЕННЫЙ ЦИКЛ
       ================================================================= */
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        P = getSharedPreferences("vnuchok", MODE_PRIVATE);
        FS = P.getFloat("fs", 1f);
        float dens = getResources().getDisplayMetrics().density;
        float wdp = getResources().getDisplayMetrics().widthPixels / dens;
        SC = Math.max(0.85f, Math.min(1.6f, wdp / 360f));

        // --- корень: шапка / середина / низ
        frame = new FrameLayout(this);
        frame.setBackground(Art.paper());
        rootLin = new LinearLayout(this);
        rootLin.setOrientation(LinearLayout.VERTICAL);

        headerSlot = new LinearLayout(this);
        headerSlot.setOrientation(LinearLayout.VERTICAL);
        headerSlot.setVisibility(View.GONE);
        rootLin.addView(headerSlot, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // --- середина: скролл для подэкранов + mainSlot для главного
        midFrame = new FrameLayout(this);
        scroll = new ScrollView(this);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.setVisibility(View.GONE);
        midFrame.addView(scroll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mainSlot = new FrameLayout(this);
        midFrame.addView(mainSlot, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rootLin.addView(midFrame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        bottomSlot = new LinearLayout(this);
        bottomSlot.setOrientation(LinearLayout.VERTICAL);
        bottomSlot.setPadding(dp(10), dp(4), dp(10), dp(10));
        rootLin.addView(bottomSlot, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        frame.addView(rootLin, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(frame);

        // --- занавес разблокировки: холмы с лесом разъезжаются за 2 сек
        if (!curtainDone) {
            curtainDone = true;
            final View curtain = Art.curtain(this);
            frame.addView(curtain, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            curtain.post(() -> ((Art.CurtainView) curtain).start(() -> frame.removeView(curtain)));
        }

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
        registerReceiver(new BroadcastReceiver() { public void onReceive(Context c, Intent i) { lastSmsStatus = "отправлено"; } }, new IntentFilter("ru.vnuchok.SMS_SENT"));
        registerReceiver(new BroadcastReceiver() { public void onReceive(Context c, Intent i) { lastSmsStatus = "доставлено"; } }, new IntentFilter("ru.vnuchok.SMS_DELIVERED"));
        missRun = new Runnable() { public void run() { checkMissed(); H.postDelayed(this, 20000); } };
        H.postDelayed(missRun, 10000);
        sigRun = new Runnable() { public void run() { pollSignal(); H.postDelayed(this, 15000); } };
        H.postDelayed(sigRun, 2000);
        askPerms();
        showMain();
        if (getIntent() != null && getIntent().hasExtra("reminder")) showReminder(getIntent().getStringExtra("reminder"));
    }

    void askPerms() {
        requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.CALL_PHONE, android.Manifest.permission.SEND_SMS,
                android.Manifest.permission.READ_SMS, android.Manifest.permission.RECEIVE_SMS,
                android.Manifest.permission.READ_CALL_LOG, android.Manifest.permission.READ_CONTACTS,
                android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
    }

    @Override protected void onResume() {
        super.onResume();
        hideSys();
        if (!permAsked) {
            permAsked = true;
            boolean miss = checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED;
            if (miss) askPerms();
        }
    }

    @Override public void onWindowFocusChanged(boolean has) {
        super.onWindowFocusChanged(has);
        if (has) hideSys();
    }

    void hideSys() {
        View d = getWindow().getDecorView();
        d.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        if (i != null && i.hasExtra("reminder")) showReminder(i.getStringExtra("reminder"));
    }

    @Override public void onInit(int s) {
        if (s == TextToSpeech.SUCCESS) {
            ttsReady = true; tts.setLanguage(new Locale("ru"));
            tts.setSpeechRate(P.getFloat("rate", 0.75f));
            tts.setPitch(P.getFloat("pitch", 1f));
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                public void onStart(String id) {}
                public void onDone(String id) {
                    if (pendingConfirm) H.post(() -> { pendingConfirm = false; startConfirmListen(); });
                    if ("book".equals(id) && bookPlaying) H.post(() -> speakBookNext());
                }
                public void onError(String id) {
                    if (pendingConfirm) H.post(() -> { pendingConfirm = false; startConfirmListen(); });
                    if ("book".equals(id)) bookPlaying = false;
                }
            });
        }
    }

    /* =================================================================
       ТЕМА
       ================================================================= */
    void applyTheme(int t) {
        switch (t) {
            case 1: Art.MUSTARD = 0xFFD9A02B; Art.GREEN = 0xFFB4531D; Art.SAND = 0xFFE8C15A; Art.CREAM = 0xFFFFFBF0; Art.PAPER = 0xFFF6EEDC; break;
            case 2: Art.GREEN = 0xFF2C3A30; Art.SAND = 0xFFE9E4D0; Art.CREAM = 0xFFE9E4D0; Art.PAPER = 0xFF141A16; Art.RUST = 0xFFB4562E; break;
            case 3: Art.GREEN = 0xFF7A4A21; Art.SAND = 0xFFFBF0DA; Art.CREAM = 0xFFFBEFD8; Art.PAPER = 0xFFF3E3C3; Art.RUST = 0xFFC05227; break;
            default: Art.GREEN = 0xFF3E5641; Art.CREAM = 0xFFF3ECD8; Art.PAPER = 0xFFEFE7D2; Art.RUST = 0xFFC05227; Art.MUSTARD = 0xFFD9A02B; break;
        }
    }

    /* =================================================================
       РЕЧЬ
       ================================================================= */
    void say(String t) {
        lastSay = t;
        H.post(() -> { if (caption != null) caption.setText(t); });
        if (ttsReady && P.getBoolean("voice", true)) tts.speak(t, TextToSpeech.QUEUE_FLUSH, null, "v");
    }

    void speak(String t) { if (ttsReady && !t.isEmpty()) tts.speak(t, TextToSpeech.QUEUE_FLUSH, null, "s"); }

    /* =================================================================
       УТИЛИТЫ UI
       ================================================================= */
    int dp(int x) { return Math.round(x * getResources().getDisplayMetrics().density * SC); }
    int shade(int c, float f) { return Color.argb(255, Math.max(0, Math.min(255, (int) (Color.red(c) * f))), Math.max(0, Math.min(255, (int) (Color.green(c) * f))), Math.max(0, Math.min(255, (int) (Color.blue(c) * f)))); }
    String hex(int c) { return "#" + Integer.toHexString(c & 0xFFFFFF | 0x1000000).substring(1); }

    TextView tv(String s, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size * FS * SC); t.setTextColor(color);
        if (bold) t.getPaint().setFakeBoldText(true);
        t.setPadding(0, dp(2), 0, dp(2));
        return t;
    }

    THolder holder(View v) {
        Object o = v.getTag();
        if (o instanceof THolder) return (THolder) o;
        THolder th = new THolder();
        v.setTag(th);
        return th;
    }

    void blink(View v, boolean on) {
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

    /* Добавление с отступом снизу (чтобы кнопки не слипались) */
    void addSpaced(LinearLayout c, View v, int marginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(marginDp);
        c.addView(v, lp);
    }

    /* Анимация нажатия: сжатие + ламповое свечение */
    void pressFx(View v) {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        boolean soundOn = P.getBoolean("sound", true) && am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
        if (soundOn) {
            try { ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 70); tg.startTone(ToneGenerator.TONE_PROP_ACK, 70); tg.release(); } catch (Exception e) {}
            v.playSoundEffect(android.view.SoundEffectConstants.CLICK);
        }
        try { Vibrator vb = (Vibrator) getSystemService(VIBRATOR_SERVICE); vb.vibrate(VibrationEffect.createOneShot(35, 140)); } catch (Exception e) {}
        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(250)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(250));
        THolder th = holder(v);
        String bg = th.bg;
        if (bg != null) {
            GradientDrawable base = new GradientDrawable(); base.setColor(Color.parseColor(bg)); base.setCornerRadius(dp(22));
            final GradientDrawable ring = new GradientDrawable();
            ring.setColor(Color.TRANSPARENT);
            ring.setCornerRadius(dp(22));
            ring.setStroke(dp(6), 0xFFFFC46B);
            android.graphics.drawable.LayerDrawable ld = new android.graphics.drawable.LayerDrawable(new Drawable[]{base, ring});
            v.setBackground(ld);
            if (Build.VERSION.SDK_INT >= 28) {
                v.setOutlineSpotShadowColor(0xFFFFC46B);
                v.setOutlineAmbientShadowColor(0xFFFFC46B);
            }
            v.setElevation(dp(14));
            ValueAnimator va = ValueAnimator.ofInt(255, 0);
            va.setDuration(500);
            va.addUpdateListener(a -> ring.setAlpha((Integer) a.getAnimatedValue()));
            va.start();
            H.postDelayed(() -> {
                GradientDrawable rb = new GradientDrawable(); rb.setColor(Color.parseColor(th.bg)); rb.setCornerRadius(dp(22)); v.setBackground(rb);
                v.setElevation(dp(5));
            }, 500);
        }
    }

    Button big(String text, int bgColor, int fgColor, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text); b.setTextSize(18 * FS * SC); b.setTextColor(fgColor);
        b.setAllCaps(false); b.getPaint().setFakeBoldText(true);
        b.setBackground(Art.pill(this, bgColor));
        THolder th = new THolder(); th.bg = hex(bgColor); b.setTag(th);
        b.setElevation(dp(5));
        b.setPadding(dp(14), dp(12), dp(14), dp(12));
        b.setOnClickListener(v -> { pressFx(v); l.onClick(v); });
        return b;
    }

    Button bigI(String kind, String text, int bgColor, int fgColor, View.OnClickListener l) {
        Button b = big(text, bgColor, fgColor, l);
        Drawable ic = Art.icon(kind, fgColor);
        ic.setBounds(0, 0, dp(30), dp(30));
        b.setCompoundDrawables(ic, null, null, null);
        b.setCompoundDrawablePadding(dp(8));
        return b;
    }

    /* Плитка главного экрана: компактная, иконка сверху, текст снизу */
    Button tileBtn(String kind, String label, int bgColor, int fgColor, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(12 * FS * SC); b.setTextColor(fgColor);
        b.setAllCaps(false); b.getPaint().setFakeBoldText(true);
        b.setBackground(Art.pill(this, bgColor));
        THolder th = new THolder(); th.bg = hex(bgColor); b.setTag(th);
        b.setElevation(dp(4));
        Drawable ic = Art.icon(kind, fgColor);
        ic.setBounds(0, 0, dp(30), dp(30));
        b.setCompoundDrawables(null, ic, null, null);
        b.setCompoundDrawablePadding(dp(4));
        b.setPadding(dp(2), dp(6), dp(2), dp(8));
        b.setOnClickListener(v -> { pressFx(v); l.onClick(v); });
        return b;
    }

    LinearLayout col() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(12), dp(6), dp(12), dp(6));
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        l.setClipChildren(false);
        l.setClipToPadding(false);
        return l;
    }

    LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setPadding(0, dp(2), 0, dp(4));
        l.setClipChildren(false);
        l.setClipToPadding(false);
        return l;
    }

    void setTitle(String t) { screenTitle = t; }

    /* =================================================================
       ПЕРЕКЛЮЧЕНИЕ ЭКРАНОВ
       ================================================================= */
    void setScreen(LinearLayout c, boolean isMain) { setScreenHeader(c, isMain, null); }

    void setMain(LinearLayout c) {
        H.post(() -> {
            headerSlot.removeAllViews();
            headerSlot.setVisibility(View.GONE);
            bottomSlot.removeAllViews();
            Button sosB = bigI("sos", "SOS — ВЫЗВАТЬ ПОМОЩЬ (112)", Art.RUST, Art.CREAM, v -> startSos());
            sosB.setTextSize(18 * FS * SC);
            sosB.setMinHeight(dp(56));
            bottomSlot.addView(sosB);
            scroll.setVisibility(View.GONE);
            mainSlot.removeAllViews();
            mainSlot.addView(c, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mainSlot.setVisibility(View.VISIBLE);
            c.setAlpha(0f);
            c.animate().alpha(1f).setDuration(400);
        });
    }

    void setScreenHeader(LinearLayout c, boolean isMain, View header) {
        if (isMain) { setMain(c); return; }
        H.post(() -> {
            headerSlot.removeAllViews();
            if (header != null) { headerSlot.setVisibility(View.VISIBLE); headerSlot.addView(header); }
            else {
                LinearLayout pillRow = row();
                pillRow.setPadding(dp(12), dp(8), dp(12), dp(2));
                LinearLayout hpill = new LinearLayout(this);
                hpill.setOrientation(LinearLayout.HORIZONTAL);
                hpill.setGravity(Gravity.CENTER_VERTICAL);
                hpill.setBackground(Art.pill(this, Art.GREEN));
                hpill.setPadding(dp(10), dp(6), dp(18), dp(6));
                Button backB = new Button(this);
                backB.setBackground(Art.pill(this, Art.CREAM));
                backB.setTag(null);
                Drawable bic = Art.icon("back", Art.GREEN);
                bic.setBounds(0, 0, dp(26), dp(26));
                backB.setCompoundDrawables(bic, null, null, null);
                backB.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
                backB.setOnClickListener(v -> showMain());
                TextView tt = tv("  " + screenTitle, 20, Art.CREAM, true);
                hpill.addView(backB); hpill.addView(tt);
                pillRow.addView(hpill, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                headerSlot.setVisibility(View.VISIBLE);
                headerSlot.addView(pillRow);
            }
            bottomSlot.removeAllViews();
            Button backB = bigI("home", "НАЗАД", Art.GREEN, Art.CREAM, v -> showMain());
            backB.setTextSize(18 * FS * SC);
            backB.setMinHeight(dp(56));
            bottomSlot.addView(backB);
            mainSlot.setVisibility(View.GONE);
            scroll.setVisibility(View.VISIBLE);
            scroll.removeAllViews();
            scroll.addView(c);
            scroll.scrollTo(0, 0);
            c.setAlpha(0f);
            c.setTranslationY(dp(10));
            c.animate().alpha(1f).translationY(0f).setDuration(400);
        });
    }

    /* =================================================================
       ВРЕМЯ/ДАТА/СТАТУС
       ================================================================= */
    String curTime() {
        Calendar c = Calendar.getInstance();
        return String.format(Locale.getDefault(), "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    String curDate() {
        Calendar c = Calendar.getInstance();
        String[] days = {"Воскресенье", "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота"};
        return days[c.get(Calendar.DAY_OF_WEEK) - 1] + ", " + c.get(Calendar.DAY_OF_MONTH) + " " + MN[c.get(Calendar.MONTH)].toLowerCase();
    }

    String nextReminderText() {
        long best = 0; String txt = null;
        for (String[] r : rems()) {
            long ms = Long.parseLong(r[0]);
            if (ms > System.currentTimeMillis() && (best == 0 || ms < best)) { best = ms; txt = r[1]; }
        }
        if (best == 0) return null;
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(best);
        Calendar now = Calendar.getInstance();
        String day;
        if (c.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) && c.get(Calendar.YEAR) == now.get(Calendar.YEAR)) day = "сегодня";
        else { now.add(Calendar.DAY_OF_YEAR, 1); if (c.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) && c.get(Calendar.YEAR) == now.get(Calendar.YEAR)) day = "завтра"; else day = c.get(Calendar.DAY_OF_MONTH) + "." + (c.get(Calendar.MONTH) + 1); }
        return day + " " + String.format(Locale.getDefault(), "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)) + " — " + txt;
    }

    void pollSignal() {
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            try {
                Object ss = tm.getClass().getMethod("getSignalStrength").invoke(tm);
                if (ss != null) {
                    Integer lv = (Integer) ss.getClass().getMethod("getLevel").invoke(ss);
                    if (lv != null) sigBars = Math.max(1, Math.min(4, lv + 1));
                }
            } catch (Exception e) { sigBars = 3; }
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
                int col = pc <= 20 ? 0xFFC0392B : Art.CREAM;
                sbBatt.setTextColor(col);
                sbBatt.setText(pc + "%");
                Art.Icon bi = Art.icon(charging ? "battc" : "batt", col);
                bi.level = pc; bi.setBounds(0, 0, dp(30), dp(30));
                sbBatt.setCompoundDrawables(bi, null, null, null);
                sbBatt.setCompoundDrawablePadding(dp(4));
                blink(sbBatt, charging || pc <= 15);
            }
            if (sbSig != null) {
                sbSig.setText(sigLabel.isEmpty() ? "" : " " + sigLabel);
                Art.Icon si = Art.icon("sig", Art.CREAM); si.bars = sigBars; si.setBounds(0, 0, dp(26), dp(26));
                sbSig.setCompoundDrawables(si, null, null, null);
            }
            if (sbOper != null) {
                String op = "";
                try { TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE); op = tm.getNetworkOperatorName(); } catch (Exception e) {}
                sbOper.setText(op == null || op.isEmpty() ? "ВНУЧОК" : op);
            }
            if (sbNet != null) {
                boolean net = false, wifi = false;
                try {
                    ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                    Network n = cm.getActiveNetwork();
                    if (n != null) { net = true; NetworkCapabilities nc = cm.getNetworkCapabilities(n); wifi = nc != null && nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI); }
                } catch (Exception e) {}
                sbNet.setText("");
                Art.Icon wi = Art.icon(wifi ? "wifi" : (net ? "sig" : "x"), Art.CREAM);
                if (!wifi && net) wi.bars = sigBars;
                wi.setBounds(0, 0, dp(26), dp(26));
                sbNet.setCompoundDrawables(wi, null, null, null);
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
                        H.post(() -> { if (sbPopup != null) { sbPopup.setText("✗ ПРОПУЩЕННЫЙ: " + nameForNumber(num)); sbPopup.setVisibility(View.VISIBLE); blink(sbPopup, true); H.postDelayed(() -> { sbPopup.setVisibility(View.GONE); blink(sbPopup, false); }, 10000); } if (callTile != null) blink(callTile, true); say("Пропущенный звонок: " + nameForNumber(num)); logCall("miss", num, nameForNumber(num)); });
                    }
                    if (last == 0) P.edit().putLong("lastMissed", d).apply();
                }
                cur.close();
            }
        } catch (Exception e) {}
    }

    /* =================================================================
       ГЛАВНЫЙ ЭКРАН (без прокрутки, всё на одном экране)
       ================================================================= */
    List<String> tilesOrder() {
        List<String> out = new ArrayList<>();
        for (String s : P.getString("tiles", DEF_TILES).split(",")) if (!s.isEmpty()) out.add(s);
        return out;
    }

    boolean tileOff(String id) {
        for (String s : P.getString("tilesOff", "").split(",")) if (s.equals(id)) return true;
        return false;
    }

    void showMain() {
        applyTheme(P.getInt("theme", 0));
        frame.setBackground(Art.paper());
        dlgOpen = false;

        LinearLayout c = col();
        c.setPadding(dp(8), dp(6), dp(8), dp(2));

        // --- верхняя зона: сцена фоном, поверх пилюли
        FrameLayout topZone = new FrameLayout(this);
        View scene = Art.scene(this, dp(300));
        topZone.addView(scene, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout pillCol = col();
        pillCol.setPadding(0, 0, 0, 0);

        LinearLayout sbar = row();
        sbar.setBackground(Art.pill(this, Art.GREEN));
        sbar.setPadding(dp(14), dp(6), dp(14), dp(6));
        sbar.setGravity(Gravity.CENTER_VERTICAL);
        sbBatt = tv("", 13, Art.CREAM, true);
        sbSig = tv("", 13, Art.CREAM, true);
        sbOper = tv("", 13, Art.CREAM, true);
        sbOper.setGravity(Gravity.CENTER);
        sbNet = tv("", 13, Art.CREAM, true);
        sbNet.setGravity(Gravity.END);
        sbOper.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        sbar.addView(sbBatt); sbar.addView(sbSig); sbar.addView(sbOper); sbar.addView(sbNet);
        pillCol.addView(sbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        sbPopup = tv("", 12, 0xFFC0392B, true);
        sbPopup.setVisibility(View.GONE);
        pillCol.addView(sbPopup);

        LinearLayout datePill = row();
        datePill.setBackground(Art.pill(this, Art.CREAM));
        datePill.setPadding(dp(16), dp(4), dp(16), dp(4));
        datePill.setGravity(Gravity.CENTER);
        TextView dateT = tv(curDate(), 13, Art.GREEN, true);
        dateT.setOnClickListener(v -> openSystemCalendar());
        datePill.addView(dateT);
        pillCol.addView(datePill);

        FrameLayout clockWrap = new FrameLayout(this);
        LinearLayout clockPill = row();
        clockPill.setBackground(Art.ringPill(this, Art.GREEN, Art.RUST));
        clockPill.setPadding(dp(24), dp(6), dp(24), dp(6));
        clockPill.setGravity(Gravity.CENTER);
        timeView = tv(curTime(), 34, Art.CREAM, true);
        timeView.setOnClickListener(v -> openSystemCalendar());
        clockPill.addView(timeView);
        clockWrap.addView(clockPill, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        View stripes = Art.stripes(this, dp(100), dp(70));
        FrameLayout.LayoutParams stp = new FrameLayout.LayoutParams(dp(100), dp(70));
        stp.gravity = Gravity.END | Gravity.TOP;
        clockWrap.addView(stripes, stp);
        pillCol.addView(clockWrap, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        if (clockRun == null) clockRun = new Runnable() { public void run() { if (timeView != null) timeView.setText(curTime()); H.postDelayed(this, 20000); } };
        H.postDelayed(clockRun, 20000);

        LinearLayout weatherPill = row();
        weatherPill.setBackground(Art.pill(this, Art.CREAM));
        weatherPill.setPadding(dp(14), dp(4), dp(14), dp(4));
        weatherPill.setGravity(Gravity.CENTER);
        Drawable wic = Art.icon("sun", Art.MUSTARD);
        wic.setBounds(0, 0, dp(26), dp(26));
        TextView wicon = tv("", 13, Art.MUSTARD, true);
        wicon.setCompoundDrawables(wic, null, null, null);
        weatherText = tv(" " + weatherLabel, 13, Art.GREEN, true);
        weatherPill.addView(wicon); weatherPill.addView(weatherText);
        pillCol.addView(weatherPill);
        fetchWeather();

        String nr = nextReminderText();
        if (nr != null) {
            LinearLayout remPill = row();
            remPill.setBackground(Art.pill(this, Art.CREAM));
            remPill.setPadding(dp(12), dp(4), dp(12), dp(4));
            remPill.setGravity(Gravity.CENTER_VERTICAL);
            Drawable ric = Art.icon("clock", Art.RUST);
            ric.setBounds(0, 0, dp(22), dp(22));
            TextView ri = tv("", 13, Art.MUSTARD, true);
            ri.setCompoundDrawables(ric, null, null, null);
            remPill.addView(ri);
            remPill.addView(tv(" " + nr, 12, Art.GREEN, true));
            pillCol.addView(remPill);
        }

        topZone.addView(pillCol, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        c.addView(topZone, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // --- голосовая карточка от края до края
        voiceCard = new LinearLayout(this);
        voiceCard.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable vcg = new GradientDrawable();
        vcg.setColor(Art.MUSTARD); vcg.setCornerRadius(dp(24)); vcg.setStroke(dp(4), Art.RUST);
        voiceCard.setBackground(vcg);
        voiceCard.setPadding(dp(12), dp(10), dp(12), dp(10));

        idleRow = row();
        idleRow.setGravity(Gravity.CENTER_VERTICAL);
        idleRow.setPadding(0, 0, 0, 0);
        micBtn = new Button(this);
        micBtn.setBackground(Art.pill(this, Art.GREEN));
        micBtn.setTag(null);
        Drawable mic = Art.icon("mic", Art.CREAM);
        mic.setBounds(0, 0, dp(34), dp(34));
        micBtn.setCompoundDrawables(mic, null, null, null);
        micBtn.setLayoutParams(new LinearLayout.LayoutParams(dp(64), dp(64)));
        micBtn.setOnClickListener(v -> { pressFx(v); openDialogAndListen(); });
        LinearLayout tcol = col();
        tcol.setPadding(dp(10), 0, 0, 0);
        tcol.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        tcol.addView(tv("Говорите со мной", 18, Art.BROWN, true));
        tcol.addView(tv("Нажмите и говорите, я помогу", 12, Art.BROWN, false));
        idleRow.addView(micBtn);
        idleRow.addView(tcol, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        voiceCard.addView(idleRow);

        dlgCol = col();
        dlgCol.setPadding(0, 0, 0, 0);
        dlgCol.setGravity(Gravity.START);
        dlgCol.setVisibility(View.GONE);
        LinearLayout drow = row();
        drow.setGravity(Gravity.CENTER_VERTICAL);
        drow.setPadding(0, 0, 0, 0);
        dlgMic = new Button(this);
        dlgMic.setBackground(Art.pill(this, Art.GREEN));
        dlgMic.setTag(null);
        Drawable dmic = Art.icon("mic", Art.CREAM);
        dmic.setBounds(0, 0, dp(28), dp(28));
        dlgMic.setCompoundDrawables(dmic, null, null, null);
        dlgMic.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));
        dlgMic.setOnClickListener(v -> { pressFx(v); startListenOnMain(); });
        userSay = tv("ВЫ: …", 13, Art.BROWN, true);
        userSay.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        dlgClose = new Button(this);
        dlgClose.setBackground(Art.pill(this, Art.CREAM));
        dlgClose.setTag(null);
        Drawable xic = Art.icon("x", Art.GREEN);
        xic.setBounds(0, 0, dp(22), dp(22));
        dlgClose.setCompoundDrawables(xic, null, null, null);
        dlgClose.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
        dlgClose.setOnClickListener(v -> collapseDialog());
        drow.addView(dlgMic); drow.addView(userSay); drow.addView(dlgClose);
        dlgCol.addView(drow);
        caption = tv("ВНУЧОК: «Здравствуйте!»", 15, Art.BROWN, true);
        dlgCol.addView(caption);
        voiceCard.addView(dlgCol);
        LinearLayout.LayoutParams vcp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vcp.topMargin = dp(6); vcp.bottomMargin = dp(6);
        c.addView(voiceCard, vcp);

        // --- плитки: закреплённая зона, заполняет остаток экрана
        LinearLayout tilesBox = col();
        tilesBox.setPadding(0, 0, 0, 0);
        int[] tileColors = {Art.GREEN, Art.MUSTARD, Art.RUST, Art.BROWN, Art.GREEN, Art.MUSTARD};
        List<String> order = tilesOrder();
        List<String> visible = new ArrayList<>();
        for (String id : order) if (!tileOff(id)) visible.add(id);
        LinearLayout curRow = null;
        int inRow = 0;
        for (int i = 0; i < visible.size(); i++) {
            if (inRow == 0) {
                curRow = row();
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
                rlp.bottomMargin = dp(6);
                tilesBox.addView(curRow, rlp);
            }
            String id = visible.get(i);
            Button b = tileFor(id, tileColors[i % tileColors.length]);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            b.setLayoutParams(lp);
            curRow.addView(b);
            inRow++;
            if (inRow == 3) inRow = 0;
        }
        c.addView(tilesBox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setMain(c);
        pollSignal();
    }

    Button tileFor(String id, int color) {
        int fg = color == Art.MUSTARD || color == Art.SAND ? Art.BROWN : Art.CREAM;
        switch (id) {
            case "call": callTile = tileBtn("phone", "Звонить", color, fg, v -> showContacts(true)); return callTile;
            case "sms": msgTile = tileBtn("mail", "Сообщения", color, fg, v -> { blink(msgTile, false); showSmsList(); }); return msgTile;
            case "apps": return tileBtn("folder", "Приложения", color, fg, v -> showAppsScreen());
            case "rem": return tileBtn("memo", "Напоминания", color, fg, v -> showReminders());
            case "alarm": return tileBtn("alarm", "Будильник", color, fg, v -> showAlarms());
            default: return tileBtn("torch", "Фонарик", color, fg, v -> toggleTorch());
        }
    }

    void openDialogAndListen() {
        if (dlgOpen) { startListenOnMain(); return; }
        dlgOpen = true;
        idleRow.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            idleRow.setVisibility(View.GONE);
            dlgCol.setVisibility(View.VISIBLE);
            dlgCol.setAlpha(0f);
            dlgCol.animate().alpha(1f).setDuration(250);
            startListenOnMain();
        });
    }

    void collapseDialog() {
        if (!dlgOpen) return;
        dlgOpen = false;
        stopListening();
        dlgCol.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            dlgCol.setVisibility(View.GONE);
            idleRow.setVisibility(View.VISIBLE);
            idleRow.setAlpha(0f);
            idleRow.animate().alpha(1f).setDuration(250);
        });
    }

    void fetchWeather() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        Network n = cm.getActiveNetwork();
        if (n == null) { weatherLabel = "—"; if (weatherText != null) weatherText.setText(" —"); return; }
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
            H.post(() -> { weatherLabel = f; if (weatherText != null) weatherText.setText(" " + f); });
        }).start();
    }

    /* =================================================================
       ГОЛОСОВОЕ СЛУШАНИЕ
       ================================================================= */
    void startListenOnMain() {
        if (listening) { stopListening(); return; }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { say("Телефон не умеет слушать."); return; }
        listening = true;
        if (userSay != null) userSay.setText("ВЫ: (слушаю…)");
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
            public void onEndOfSpeech() { H.post(() -> { listening = false; }); }
            public void onError(int e) { H.post(() -> { listening = false; if (userSay != null) userSay.setText("ВЫ: (не расслышал, повторите)"); say("Не расслышал. Повторите, пожалуйста."); }); }
            public void onResults(Bundle r) {
                ArrayList<String> a = r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String t = (a != null && !a.isEmpty()) ? a.get(0) : "";
                H.post(() -> { listening = false; if (!t.isEmpty()) { if (userSay != null) userSay.setText("ВЫ: «" + t + "»"); handleCommand(t); } else if (userSay != null) userSay.setText("ВЫ: (пусто)"); });
            }
            public void onPartialResults(Bundle r) {
                ArrayList<String> a = r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (a != null && !a.isEmpty()) H.post(() -> { if (userSay != null) userSay.setText("ВЫ: «" + a.get(0) + "»…"); });
            }
            public void onEvent(int i, Bundle b) {}
        });
        try { sr.startListening(it); } catch (Exception e) { say("Микрофон не открылся."); listening = false; }
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
        try { if (compSr != null) { compSr.stopListening(); compSr.cancel(); compSr.destroy(); } } catch (Exception e) {}
        compSr = null;
    }

    /* =================================================================
       ЭКРАН «ПРИЛОЖЕНИЯ»
       ================================================================= */
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

    void openCameraApp() {
        Intent li = null;
        try { li = getPackageManager().getLaunchIntentForPackage("com.sec.android.app.camera"); } catch (Exception e) {}
        if (li == null) {
            try {
                List<ResolveInfo> apps = getPackageManager().queryIntentActivities(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), 0);
                if (!apps.isEmpty()) li = getPackageManager().getLaunchIntentForPackage(apps.get(0).activityInfo.packageName);
            } catch (Exception e) {}
        }
        if (li == null) li = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try { startActivity(li); say("Открыл камеру."); } catch (Exception e) { say("Камера не открылась."); }
    }

    void showAppsScreen() {
        setTitle("Приложения");
        LinearLayout c = col();
        int rowH = Math.round(dp(110) * FS);
        LinearLayout.LayoutParams hp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        hp2.setMargins(dp(4), dp(4), dp(4), dp(4));
        String[][] funcs = {
                {"radio", "Радио"}, {"map", "Карта"}, {"music", "Музыка"}, {"weather", "Погода"},
                {"web", "Интернет"}, {"cam", "Фото"}, {"album", "Фотоальбом"}, {"book", "Чтение"},
                {"gear", "Настройки"}, {"memo", "Заметки голосом"}};
        int[] cols = {Art.GREEN, Art.MUSTARD, Art.RUST, Art.BROWN};
        LinearLayout fr = null;
        for (int i = 0; i < funcs.length; i++) {
            if (i % 2 == 0) { fr = row(); fr.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowH)); c.addView(fr); }
            final String id = funcs[i][0];
            int colr = cols[i % cols.length];
            int fg = colr == Art.MUSTARD || colr == Art.SAND ? Art.BROWN : Art.CREAM;
            Button b = tileBtn(id, funcs[i][1], colr, fg, v -> appFunc(id));
            b.setLayoutParams(hp2);
            fr.addView(b);
        }
        c.addView(tv("УСТАНОВЛЕННЫЕ ПРОГРАММЫ", 16, Art.GREEN, true));
        int rowH3 = Math.round(dp(90) * FS);
        LinearLayout.LayoutParams hp3 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        hp3.setMargins(dp(3), dp(3), dp(3), dp(3));
        List<Object[]> apps = installedApps();
        LinearLayout ar = null;
        int n = 0;
        for (final Object[] a : apps) {
            if (n % 3 == 0) { ar = row(); ar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowH3)); c.addView(ar); }
            Button ab = new Button(this);
            ab.setText((String) a[0]);
            ab.setTextSize(10 * FS * SC); ab.setTextColor(Art.BROWN);
            ab.setBackground(Art.pill(this, Art.MUSTARD));
            THolder th = new THolder(); th.bg = hex(Art.MUSTARD); ab.setTag(th);
            ab.setElevation(dp(4));
            Drawable icn = (Drawable) a[1];
            if (icn != null) { icn.setBounds(0, 0, dp(34), dp(34)); ab.setCompoundDrawables(null, icn, null, null); }
            ab.setOnClickListener(v -> { pressFx(v); openPackage((String) a[2]); });
            ab.setLayoutParams(hp3);
            ar.addView(ab);
            n++;
        }
        if (n == 0) c.addView(tv("(нет других приложений)", 14, Art.GREEN, true));
        setScreen(c, false);
    }

    void appFunc(String id) {
        switch (id) {
            case "radio": openAppKeyword("радио"); break;
            case "map": try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0"))); } catch (Exception e) { openAppKeyword("карт"); } break;
            case "music": openAppKeyword("музык"); break;
            case "weather": openAppKeyword("погод"); break;
            case "web": try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))); } catch (Exception e) { openAppKeyword("браузер"); } break;
            case "cam": openCameraApp(); break;
            case "album": openPhotoalbum(); break;
            case "book": showReading(); break;
            case "gear": showSettings(); break;
            case "memo": showComposeQuick(); break;
        }
    }

    void showComposeQuick() {
        setTitle("Заметка голосом");
        LinearLayout c = col();
        c.addView(tv("Говорите текст заметки, я запишу и озвучу", 15, Art.GREEN, true));
        final EditText et = new EditText(this);
        et.setTextSize(18 * FS * SC); et.setMinLines(4);
        c.addView(et);
        addSpaced(c, bigI("sound", "ПРОЧИТАТЬ ВСЛУХ", Art.MUSTARD, Art.BROWN, v -> speak(et.getText().toString())), 8);
        addSpaced(c, bigI("memo", "СОХРАНИТЬ В ЗАМЕТКИ", 0xFF3FAE4C, Art.CREAM, v -> {
            String old = P.getString("notes", "");
            P.edit().putString("notes", old + (old.isEmpty() ? "" : "\n---\n") + et.getText().toString()).apply();
            say("Заметка сохранена.");
            showAppsScreen();
        }), 8);
        setScreen(c, false);
    }

    /* =================================================================
       ЧТЕНИЕ ВСЛУХ
       ================================================================= */
    void showReading() {
        setTitle("Чтение вслух");
        LinearLayout c = col();
        c.addView(tv("КНИГИ ДЛЯ СЛАБОВИДЯЩИХ", 20, Art.GREEN, true));
        c.addView(tv("Внучок прочитает книгу голосом", 14, Art.BROWN, true));
        addSpaced(c, bigI("book", "ОТКРЫТЬ ФАЙЛ КНИГИ", 0xFF3FAE4C, Art.CREAM, v -> {
            try {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.setType("text/*");
                i.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(i, 42);
            } catch (Exception e) { say("Не удалось открыть выбор файла."); }
        }), 8);
        addSpaced(c, bigI("memo", "ВСТАВИТЬ ТЕКСТ", Art.MUSTARD, Art.BROWN, v -> showPasteText()), 8);
        if (bookText != null) {
            String preview = bookText.length() > 160 ? bookText.substring(0, 160) + "…" : bookText;
            c.addView(tv("Загружено: " + preview, 13, Art.GREEN, false));
            addSpaced(c, bigI("sound", bookPlaying ? "ПАУЗА" : "ЧИТАТЬ ВСЛУХ", Art.MUSTARD, Art.BROWN, v -> {
                if (bookPlaying) { bookPlaying = false; if (tts != null) tts.stop(); say("Пауза."); }
                else startBook();
            }), 8);
            addSpaced(c, bigI("x", "СТОП И ЗАБЫТЬ", Art.RUST, Art.CREAM, v -> { bookPlaying = false; bookText = null; bookSents = null; if (tts != null) tts.stop(); say("Остановил чтение."); showReading(); }), 8);
        }
        setScreen(c, false);
    }

    void showPasteText() {
        setTitle("Вставить текст");
        LinearLayout c = col();
        final EditText et = new EditText(this);
        et.setTextSize(16 * FS * SC); et.setMinLines(6);
        et.setHint("Вставьте или напишите текст");
        c.addView(et);
        addSpaced(c, bigI("sound", "НАЧАТЬ ЧТЕНИЕ", 0xFF3FAE4C, Art.CREAM, v -> {
            bookText = et.getText().toString();
            prepareBook();
            startBook();
        }), 8);
        setScreen(c, false);
    }

    void prepareBook() {
        bookSents = new ArrayList<>();
        if (bookText == null) return;
        String[] parts = bookText.split("(?<=[.!?…])\\s+|\\n+");
        for (String p : parts) if (!p.trim().isEmpty()) bookSents.add(p.trim());
        bookIdx = 0;
    }

    void startBook() {
        if (bookSents == null) prepareBook();
        if (bookSents == null || bookSents.isEmpty()) { say("Текст пуст."); return; }
        bookPlaying = true;
        say("Начинаю чтение.");
        speakBookNext();
    }

    void speakBookNext() {
        if (!bookPlaying || bookSents == null) return;
        if (bookIdx >= bookSents.size()) { bookPlaying = false; say("Книга прочитана до конца."); return; }
        String sent = bookSents.get(bookIdx++);
        if (ttsReady) tts.speak(sent, TextToSpeech.QUEUE_FLUSH, null, "book");
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 42 && res == RESULT_OK && data != null && data.getData() != null) {
            final Uri uri = data.getData();
            new Thread(() -> {
                StringBuilder sb = new StringBuilder();
                try {
                    InputStream in = getContentResolver().openInputStream(uri);
                    BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                    String line;
                    while ((line = br.readLine()) != null && sb.length() < 1500000) sb.append(line).append('\n');
                    br.close();
                } catch (Exception e) { }
                final String txt = sb.toString();
                H.post(() -> {
                    if (txt.isEmpty()) { say("Файл пустой или не читается."); return; }
                    bookText = txt;
                    prepareBook();
                    say("Книга загружена. Нажми читать вслух.");
                    showReading();
                });
            }).start();
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

    /* =================================================================
       НАСТРОЙКИ И РЕДАКТОР ЭКРАНА
       ================================================================= */
    void showSettings() {
        setTitle("Настройки");
        LinearLayout c = col();
        c.addView(tv("ТЕМА:", 15, Art.GREEN, true));
        LinearLayout rt = row();
        for (int i = 0; i < 4; i++) {
            final int ti = i;
            Button b = big(THN[i], P.getInt("theme", 0) == i ? Art.GREEN : Art.MUSTARD, P.getInt("theme", 0) == i ? Art.CREAM : Art.BROWN, x -> { P.edit().putInt("theme", ti).apply(); showMain(); say("Тема: " + THN[ti].toLowerCase() + "."); });
            b.setTextSize(11 * FS * SC);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            b.setLayoutParams(lp);
            rt.addView(b);
        }
        addSpaced(c, rt, 8);
        addSpaced(c, bigI("wrench", "РЕДАКТОР ГЛАВНОГО ЭКРАНА", Art.MUSTARD, Art.BROWN, v -> showEditor()), 8);
        addSpaced(c, toggleRow("Ответы вслух", "voice", true), 6);
        addSpaced(c, toggleRow("Щелчки и подсветка кнопок", "sound", true), 8);
        c.addView(tv("Размер текста:", 15, Art.GREEN, true));
        LinearLayout r1 = row();
        String[] fsN = {"МЕЛКИЙ", "СРЕДНИЙ", "КРУПНЫЙ"};
        float[] fsV = {1f, 1.25f, 1.5f};
        for (int i = 0; i < 3; i++) {
            final float v = fsV[i];
            Button b = big(fsN[i], Math.abs(FS - v) < 0.01 ? Art.GREEN : Art.MUSTARD, Math.abs(FS - v) < 0.01 ? Art.CREAM : Art.BROWN, x -> { P.edit().putFloat("fs", v).apply(); FS = v; showSettings(); say("Размер текста изменён."); });
            b.setTextSize(12 * FS * SC);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            b.setLayoutParams(lp);
            r1.addView(b);
        }
        addSpaced(c, r1, 8);
        c.addView(tv("Скорость речи:", 15, Art.GREEN, true));
        LinearLayout r2 = row();
        String[] rn = {"МЕДЛЕННО", "ОБЫЧНО", "БЫСТРО"};
        float[] rv = {0.6f, 0.9f, 1.2f};
        for (int i = 0; i < 3; i++) {
            final float v = rv[i];
            Button b = big(rn[i], Math.abs(P.getFloat("rate", 0.75f) - v) < 0.05 ? Art.GREEN : Art.MUSTARD, Math.abs(P.getFloat("rate", 0.75f) - v) < 0.05 ? Art.CREAM : Art.BROWN, x -> { P.edit().putFloat("rate", v).apply(); if (ttsReady) tts.setSpeechRate(v); showSettings(); say("Скорость речи изменена."); });
            b.setTextSize(12 * FS * SC);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            b.setLayoutParams(lp);
            r2.addView(b);
        }
        addSpaced(c, r2, 8);
        c.addView(tv("Тон голоса:", 15, Art.GREEN, true));
        LinearLayout r3 = row();
        String[] pn = {"НИЗКИЙ", "СРЕДНИЙ", "ВЫСОКИЙ"};
        float[] pv = {0.8f, 1f, 1.2f};
        for (int i = 0; i < 3; i++) {
            final float v = pv[i];
            Button b = big(pn[i], Math.abs(P.getFloat("pitch", 1f) - v) < 0.05 ? Art.GREEN : Art.MUSTARD, Math.abs(P.getFloat("pitch", 1f) - v) < 0.05 ? Art.CREAM : Art.BROWN, x -> { P.edit().putFloat("pitch", v).apply(); if (ttsReady) tts.setPitch(v); showSettings(); say("Тон голоса изменён."); });
            b.setTextSize(12 * FS * SC);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            b.setLayoutParams(lp);
            r3.addView(b);
        }
        addSpaced(c, r3, 8);
        addSpaced(c, bigI("mic", "ГОЛОСОВОЕ УПРАВЛЕНИЕ", Art.MUSTARD, Art.BROWN, v -> { try { startActivity(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)); } catch (Exception e) { say("Не удалось открыть."); } }), 6);
        addSpaced(c, bigI("sound", "СИНТЕЗ РЕЧИ (ДРУГИЕ ГОЛОСА)", Art.MUSTARD, Art.BROWN, v -> { try { startActivity(new Intent("com.android.settings.TTS_SETTINGS")); } catch (Exception e) { say("Не удалось открыть."); } }), 6);
        addSpaced(c, bigI("people", "СПЕЦИАЛЬНЫЕ ВОЗМОЖНОСТИ", Art.MUSTARD, Art.BROWN, v -> { try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); } catch (Exception e) { say("Не удалось открыть."); } }), 6);
        addSpaced(c, bigI("gear", "РАЗРЕШЕНИЯ ПРИЛОЖЕНИЯ", Art.MUSTARD, Art.BROWN, v -> { try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))); } catch (Exception e) { say("Не удалось открыть."); } }), 6);
        addSpaced(c, bigI("gear", "СИСТЕМНЫЕ НАСТРОЙКИ ANDROID", Art.MUSTARD, Art.BROWN, v -> { try { startActivity(new Intent(Settings.ACTION_SETTINGS)); } catch (Exception e) { say("Не удалось открыть."); } }), 6);
        addSpaced(c, bigI("clock", "О ПРИЛОЖЕНИИ", Art.MUSTARD, Art.BROWN, v -> showAbout()), 6);
        setScreen(c, false);
    }

    void showEditor() {
        setTitle("Редактор экрана");
        LinearLayout c = col();
        c.addView(tv("МАСШТАБ ПЛИТОК:", 15, Art.GREEN, true));
        LinearLayout rs = row();
        String[] sn = {"МЕЛКИЕ", "СРЕДНИЕ", "КРУПНЫЕ"};
        float[] sv = {0.85f, 1f, 1.2f};
        for (int i = 0; i < 3; i++) {
            final float v = sv[i];
            Button b = big(sn[i], Math.abs(P.getFloat("tileScale", 1f) - v) < 0.01 ? Art.GREEN : Art.MUSTARD, Math.abs(P.getFloat("tileScale", 1f) - v) < 0.01 ? Art.CREAM : Art.BROWN, x -> { P.edit().putFloat("tileScale", v).apply(); showEditor(); say("Масштаб изменён."); });
            b.setTextSize(11 * FS * SC);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            b.setLayoutParams(lp);
            rs.addView(b);
        }
        addSpaced(c, rs, 8);
        c.addView(tv("КНОПКИ ГЛАВНОГО ЭКРАНА:", 15, Art.GREEN, true));
        List<String> order = tilesOrder();
        String[][] meta = {
                {"call", "Звонить"}, {"sms", "Сообщения"}, {"apps", "Приложения"},
                {"rem", "Напоминания"}, {"alarm", "Будильник"}, {"torch", "Фонарик"}};
        for (int i = 0; i < order.size(); i++) {
            final int pos = i;
            final String id = order.get(i);
            String tmpName = id;
            for (String[] m : meta) if (m[0].equals(id)) tmpName = m[1];
            final String name = tmpName;
            LinearLayout rw = row();
            rw.setGravity(Gravity.CENTER_VERTICAL);
            Button nb = big(name, tileOff(id) ? 0xFF8A8A8A : Art.MUSTARD, tileOff(id) ? Art.CREAM : Art.BROWN, v -> {
                List<String> off = new ArrayList<>();
                for (String s : P.getString("tilesOff", "").split(",")) if (!s.isEmpty()) off.add(s);
                if (off.remove(id)) say(name + ": скрыта.");
                else { off.add(id); say(name + ": на экране."); }
                StringBuilder sb = new StringBuilder();
                for (int k = 0; k < off.size(); k++) { if (k > 0) sb.append(","); sb.append(off.get(k)); }
                P.edit().putString("tilesOff", sb.toString()).apply();
                showEditor();
            });
            nb.setTextSize(13 * FS * SC);
            nb.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            rw.addView(nb);
            Button up = big("↑", Art.MUSTARD, Art.BROWN, v -> { if (pos > 0) { List<String> o = tilesOrder(); String t = o.remove(pos); o.add(pos - 1, t); saveTiles(o); showEditor(); } });
            up.setLayoutParams(new LinearLayout.LayoutParams(dp(60), ViewGroup.LayoutParams.WRAP_CONTENT));
            rw.addView(up);
            Button dn = big("↓", Art.MUSTARD, Art.BROWN, v -> { if (pos < order.size() - 1) { List<String> o = tilesOrder(); String t = o.remove(pos); o.add(pos + 1, t); saveTiles(o); showEditor(); } });
            dn.setLayoutParams(new LinearLayout.LayoutParams(dp(60), ViewGroup.LayoutParams.WRAP_CONTENT));
            rw.addView(dn);
            addSpaced(c, rw, 6);
        }
        c.addView(tv("Серым помечены скрытые кнопки", 12, Art.BROWN, false));
        setScreen(c, false);
    }

    void saveTiles(List<String> o) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < o.size(); i++) { if (i > 0) sb.append(","); sb.append(o.get(i)); }
        P.edit().putString("tiles", sb.toString()).apply();
    }

    void showAbout() {
        setTitle("О приложении");
        LinearLayout c = col();
        c.addView(tv("ВНУЧОК", 30, Art.GREEN, true));
        c.addView(tv("Версия: 2.1", 18, Art.GREEN, true));
        c.addView(tv("Оболочка Android для пенсионеров", 15, Art.BROWN, true));
        c.addView(tv("Все данные хранятся только на телефоне", 13, Art.BROWN, true));
        setScreen(c, false);
    }

    LinearLayout toggleRow(String label, String key, boolean def) {
        LinearLayout r = row();
        TextView t = tv(label, 14, Art.GREEN, true);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(t);
        boolean on = P.getBoolean(key, def);
        Button b = big(on ? "ВКЛ" : "ВЫКЛ", on ? 0xFF3FAE4C : 0xFF8A8A8A, Art.CREAM, x -> { P.edit().putBoolean(key, !P.getBoolean(key, def)).apply(); showSettings(); });
        b.setTextSize(12 * FS * SC);
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(100), ViewGroup.LayoutParams.WRAP_CONTENT));
        r.addView(b);
        return r;
    }

    /* =================================================================
       ГОЛОСОВЫЕ КОМАНДЫ
       ================================================================= */
    void handleCommand(String raw) {
        String t = raw.toLowerCase().replace('ё', 'е');
        String[] fc = findContact(t);
        if (t.matches(".*(помогите|спасите|скорую|скорая|sos|112|плохо мне).*")) { startSos(); return; }
        if (t.contains("позвони") || t.contains("набери") || t.contains("перезвони")) {
            if (fc != null) confirmCall(fc[0], fc[1]);
            else { say("Кому звонить? Открываю список."); showContacts(true); } return; }
        if (t.contains("напиши") || t.contains("смс") || t.contains("письмо")) {
            if (fc != null) showCompose(fc[0], fc[1]); else { say("Кому написать? Открываю список."); showContacts(false); } return; }
        if (t.contains("прочитай сообщ") || t.contains("что написали") || t.contains("входящие")) { showSmsList(); return; }
        if (t.contains("фонарик") || t.contains("фонарь") || t.contains("свет")) { toggleTorch(); return; }
        if (t.contains("громче")) { vol(1); return; }
        if (t.contains("тише")) { vol(-1); return; }
        if (t.contains("фотоальбом") || t.contains("галере") || t.contains("покажи фото")) { openPhotoalbum(); return; }
        if (t.contains("сфотографиру") || t.contains("фото") || t.contains("сними")) { openCameraApp(); return; }
        if (t.contains("истори") || t.contains("кто звонил")) { showDial(); return; }
        if (t.contains("напомни") || t.contains("напоминан")) {
            if (t.contains("в ") || t.contains("через")) { setReminder(t); } else showReminders(); return; }
        if (t.contains("будильник")) { showAlarms(); return; }
        if (t.contains("настрой")) { showSettings(); return; }
        if (t.contains("набер") || t.contains("номер")) { showDial(); return; }
        if (t.contains("календар")) { openSystemCalendar(); return; }
        if (t.contains("который час") || t.contains("время")) { sayTime(); return; }
        if (t.contains("интернет") || t.contains("браузер")) { appFunc("web"); return; }
        if (t.contains("читай") || t.contains("книг")) { showReading(); return; }
        if (t.contains("вибро") || t.contains("тихий режим") || t.contains("без звука")) { toggleVibro(); return; }
        if (t.contains("открой") || t.contains("запусти")) { openApp(t); return; }
        if (t.contains("умеешь") || t.contains("помощь")) { say("Я умею: звонить, писать и читать СМС, набирать номер, показывать историю и фотоальбом, включать фонарь, напоминать, будить, читать книги и вызывать помощь."); return; }
        if (t.contains("повтори")) { say(lastSay); return; }
        say("Не понял. Скажите: позвони, напиши, напомни, фонарь или помощь.");
    }

    /* =================================================================
       КОНТАКТЫ
       ================================================================= */
    List<String[]> contacts() {
        List<String[]> out = new ArrayList<>();
        String raw = P.getString("contacts", DEF_CONTACTS);
        for (String line : raw.split("\n")) {
            String[] p = line.split("\\|");
            if (p.length == 2) out.add(p);
        }
        return out;
    }

    List<String> hidden() {
        List<String> out = new ArrayList<>();
        for (String s : P.getString("hidden", "").split("\n")) if (!s.isEmpty()) out.add(s);
        return out;
    }

    List<String[]> sysContacts() {
        List<String[]> out = new ArrayList<>();
        List<String> hid = hidden();
        List<String[]> cust = contacts();
        try {
            Cursor cur = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER},
                    null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIMIT 1000");
            if (cur != null) {
                while (cur.moveToNext()) {
                    String n = cur.getString(0);
                    String num = cur.getString(1);
                    if (n == null || num == null) continue;
                    n = n.trim(); num = num.replaceAll("[^0-9+]", "");
                    if (n.isEmpty() || num.isEmpty()) continue;
                    if (hid.contains(n + "|" + num)) continue;
                    boolean shadow = false;
                    for (String[] cc : cust) if (cc[0].equalsIgnoreCase(n)) shadow = true;
                    if (shadow) continue;
                    boolean dup = false;
                    for (String[] o : out) if (o[0].equals(n) && o[1].equals(num)) dup = true;
                    if (!dup) out.add(new String[]{n, num});
                }
                cur.close();
            }
        } catch (Exception e) {}
        return out;
    }

    List<String[]> allContacts() {
        List<String[]> all = contacts();
        all.addAll(sysContacts());
        return all;
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

    String[] findContact(String t) {
        List<String[]> cs = allContacts();
        String[] tw = t.split("[^a-zа-яё0-9]+");
        for (String[] cc : cs) {
            String[] nw = cc[0].toLowerCase().split("\\s+");
            for (String a : tw) for (String b : nw) if (a.length() >= 4 && sim(a, b)) return cc;
        }
        return null;
    }

    void showContacts(boolean callMode) {
        contactShown = 30;
        renderContacts(callMode);
    }

    void renderContacts(boolean callMode) {
        setTitle(callMode ? "Кому звоним" : "Кому пишем");
        LinearLayout c = col();
        List<String[]> cs = allContacts();
        int customN = contacts().size();
        int upTo = Math.min(contactShown, cs.size());
        for (int i = 0; i < upTo; i++) {
            final String nm = cs.get(i)[0], num = cs.get(i)[1];
            final boolean isCustom = i < customN;
            final int idx = i;
            LinearLayout rw = row();
            Button nb = bigI("people", nm, Art.MUSTARD, Art.BROWN, v -> { if (callMode) confirmCall(nm, num); else showCompose(nm, num); });
            nb.setTextSize(14 * FS * SC);
            nb.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            rw.addView(nb);
            Button eb = big("✏", Art.GREEN, Art.CREAM, v -> editContact(isCustom ? idx : -1, nm, num));
            eb.setLayoutParams(new LinearLayout.LayoutParams(dp(64), ViewGroup.LayoutParams.WRAP_CONTENT));
            rw.addView(eb);
            Button db = big("✕", Art.RUST, Art.CREAM, v -> confirmDialog("Удалить " + nm + " из списка?", () -> {
                if (isCustom) { List<String[]> x = contacts(); x.remove(idx); saveContacts(x); }
                else { P.edit().putString("hidden", P.getString("hidden", "") + (P.getString("hidden", "").isEmpty() ? "" : "\n") + nm + "|" + num).apply(); }
                say("Удалил: " + nm);
                renderContacts(callMode);
            }));
            db.setLayoutParams(new LinearLayout.LayoutParams(dp(64), ViewGroup.LayoutParams.WRAP_CONTENT));
            rw.addView(db);
            addSpaced(c, rw, 8);
        }
        if (upTo < cs.size()) {
            addSpaced(c, bigI("dial", "ПОКАЗАТЬ ЕЩЁ (" + (cs.size() - upTo) + ")", Art.MUSTARD, Art.BROWN, v -> { contactShown += 30; renderContacts(callMode); }), 8);
        }
        Button header = bigI("people", "НОВЫЙ НОМЕР", 0xFF3FAE4C, Art.CREAM, v -> editContact(-1, "", ""));
        header.setTextSize(16 * FS * SC);
        setScreenHeader(c, false, header);
    }

    void editContact(int idx, String preN, String preP) {
        setTitle(idx < 0 ? "Новый контакт" : "Изменить");
        List<String[]> cs = contacts();
        LinearLayout c = col();
        EditText en = new EditText(this); en.setTextSize(18 * FS * SC); en.setHint("Имя (например: Дочь Маша)");
        en.setText(preN);
        c.addView(en);
        EditText ep = new EditText(this); ep.setTextSize(18 * FS * SC); ep.setHint("Номер (например: +79121234567)");
        ep.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        ep.setText(preP);
        c.addView(ep);
        addSpaced(c, big("💾 СОХРАНИТЬ", 0xFF3FAE4C, Art.CREAM, v -> {
            String n = en.getText().toString().trim(), p = ep.getText().toString().trim();
            if (n.isEmpty() || p.isEmpty()) { say("Заполните имя и номер."); return; }
            List<String[]> x = contacts();
            if (idx >= 0 && idx < x.size()) x.set(idx, new String[]{n, p});
            else x.add(new String[]{n, p});
            saveContacts(x);
            say("Сохранил: " + n);
            showContacts(true);
        }), 8);
        addSpaced(c, big("ОТМЕНА", Art.MUSTARD, Art.BROWN, v -> showContacts(true)), 8);
        setScreen(c, false);
    }

    void confirmDialog(String title, Runnable yes) {
        confirmOverlay = new LinearLayout(this);
        confirmOverlay.setOrientation(LinearLayout.VERTICAL);
        confirmOverlay.setGravity(Gravity.CENTER);
        confirmOverlay.setBackgroundColor(Color.parseColor("#CC4A2C17"));
        LinearLayout card = col();
        card.setBackground(Art.pill(this, Art.CREAM));
        card.addView(tv(title, 20, Art.GREEN, true));
        addSpaced(card, big("✅ ДА", 0xFF3FAE4C, Art.CREAM, v -> { removeConfirm(); yes.run(); }), 8);
        addSpaced(card, big("✋ ОТМЕНА", Art.MUSTARD, Art.BROWN, v -> removeConfirm()), 8);
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
        card.setBackground(Art.pill(this, Art.CREAM));
        card.addView(tv("Звоним: " + label + "?", 22, Art.GREEN, true));
        card.addView(tv("Скажите «да» или «нет»", 14, Art.BROWN, true));
        addSpaced(card, big("✅ ДА, ЗВОНИ", 0xFF3FAE4C, Art.CREAM, v -> { removeConfirm(); callNumber(pendingNum, pendingLabel); }), 8);
        addSpaced(card, big("✋ ОТМЕНА", Art.MUSTARD, Art.BROWN, v -> { removeConfirm(); say("Отменил."); }), 8);
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
            try { startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + num))); logCall("out", num, label); say("Звоню: " + label); return; } catch (Exception e) {}
        }
        try { startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + num))); logCall("out", num, label); } catch (Exception e) {}
        say("Нет разрешения на звонки. Нажмите кнопку вызова или разрешите звонки в настройках.");
    }

    List<String[]> callJournal() {
        List<String[]> out = new ArrayList<>();
        for (String line : P.getString("callj", "").split("\n")) {
            String[] p = line.split("\\|");
            if (p.length == 4) out.add(p);
        }
        return out;
    }

    void logCall(String dir, String num, String name) {
        List<String[]> j = callJournal();
        j.add(new String[]{String.valueOf(System.currentTimeMillis()), dir, num, name});
        while (j.size() > 30) j.remove(0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < j.size(); i++) { if (i > 0) sb.append("\n"); sb.append(String.join("|", j.get(i))); }
        P.edit().putString("callj", sb.toString()).apply();
    }

    /* =================================================================
       ТЕЛЕФОН (набор + история)
       ================================================================= */
    void showDial() {
        setTitle("Телефон");
        LinearLayout c = col();
        LinearLayout tab = row();
        final boolean[] hist = {false};
        Button tb1 = bigI("dial", "НАБОР", Art.GREEN, Art.CREAM, v -> { hist[0] = false; renderDialBody(c, hist); });
        Button tb2 = bigI("clock", "ИСТОРИЯ", Art.MUSTARD, Art.BROWN, v -> { hist[0] = true; renderDialBody(c, hist); });
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tp.setMargins(dp(3), 0, dp(3), 0);
        tb1.setLayoutParams(tp); tb2.setLayoutParams(tp);
        tab.addView(tb1); tab.addView(tb2);
        addSpaced(c, tab, 8);
        renderDialBody(c, hist);
        setScreen(c, false);
    }

    void renderDialBody(LinearLayout c, boolean[] hist) {
        for (int i = c.getChildCount() - 1; i >= 1; i--) c.removeViewAt(i);
        if (hist[0]) {
            c.addView(tv("ИСТОРИЯ ЗВОНКОВ", 20, Art.GREEN, true));
            boolean any = false;
            try {
                Cursor cur = getContentResolver().query(CallLog.Calls.CONTENT_URI,
                        new String[]{CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE},
                        null, null, CallLog.Calls.DATE + " DESC LIMIT 15");
                if (cur != null) {
                    while (cur.moveToNext()) {
                        any = true;
                        String num = cur.getString(0); long date = cur.getLong(1); int type = cur.getInt(2);
                        String arrow = type == CallLog.Calls.OUTGOING_TYPE ? "→" : type == CallLog.Calls.MISSED_TYPE ? "✗" : "←";
                        int color = type == CallLog.Calls.MISSED_TYPE ? Art.RUST : Art.BROWN;
                        String name = nameForNumber(num);
                        Calendar cd = Calendar.getInstance(); cd.setTimeInMillis(date);
                        String when = String.format(Locale.getDefault(), "%02d:%02d %02d.%02d", cd.get(Calendar.HOUR_OF_DAY), cd.get(Calendar.MINUTE), cd.get(Calendar.DAY_OF_MONTH), cd.get(Calendar.MONTH) + 1);
                        final String fnum = num;
                        Button b = big(arrow + " " + name + "\n" + when, Art.MUSTARD, color, v -> { blink(callTile, false); P.edit().putLong("lastMissed", System.currentTimeMillis()).apply(); confirmCall(name, fnum); });
                        b.setTextSize(14 * FS * SC);
                        addSpaced(c, b, 8);
                    }
                    cur.close();
                }
            } catch (Exception e) {}
            if (!any) {
                List<String[]> j = callJournal();
                for (int i = j.size() - 1; i >= 0; i--) {
                    final String[] e = j.get(i);
                    String arrow = e[1].equals("out") ? "→" : e[1].equals("miss") ? "✗" : "←";
                    Calendar cd = Calendar.getInstance(); cd.setTimeInMillis(Long.parseLong(e[0]));
                    String when = String.format(Locale.getDefault(), "%02d:%02d %02d.%02d", cd.get(Calendar.HOUR_OF_DAY), cd.get(Calendar.MINUTE), cd.get(Calendar.DAY_OF_MONTH), cd.get(Calendar.MONTH) + 1);
                    addSpaced(c, big(arrow + " " + e[3] + "\n" + when, Art.MUSTARD, Art.BROWN, v -> confirmCall(e[3], e[2])), 8);
                    any = true;
                }
            }
            if (!any) c.addView(tv("Звонков пока нет.", 14, Art.BROWN, true));
            addSpaced(c, bigI("clock", "СИСТЕМНАЯ ИСТОРИЯ ЗВОНКОВ", Art.MUSTARD, Art.BROWN, v -> {
                try { Intent i = new Intent(Intent.ACTION_VIEW); i.setType(CallLog.Calls.CONTENT_TYPE); startActivity(i); }
                catch (Exception e) { say("Не удалось открыть системную историю."); }
            }), 8);
        } else {
            TextView disp = tv("", 30, Art.GREEN, true);
            disp.setGravity(Gravity.CENTER);
            disp.setMinHeight(dp(60));
            c.addView(disp);
            final StringBuilder cur = new StringBuilder();
            String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"};
            for (int r = 0; r < 4; r++) {
                LinearLayout rw = row();
                for (int k = 0; k < 3; k++) {
                    String key = keys[r * 3 + k];
                    Button b = big(key, Art.MUSTARD, Art.BROWN, v -> { cur.append(key); disp.setText(cur.toString()); });
                    b.setTextSize(24 * FS * SC);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    lp.setMargins(dp(4), dp(3), dp(4), dp(3));
                    b.setLayoutParams(lp);
                    rw.addView(b);
                }
                c.addView(rw);
            }
            LinearLayout rw = row();
            Button del = big("⌫", 0xFF8A8A8A, Art.CREAM, v -> { if (cur.length() > 0) cur.deleteCharAt(cur.length() - 1); disp.setText(cur.toString()); });
            Button call = bigI("phone", "ПОЗВОНИТЬ", 0xFF3FAE4C, Art.CREAM, v -> { if (cur.length() > 0) confirmCall(cur.toString(), cur.toString()); });
            del.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            call.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f));
            rw.addView(del); rw.addView(call);
            addSpaced(c, rw, 8);
            addSpaced(c, bigI("sos", "112 ЭКСТРЕННО", Art.RUST, Art.CREAM, v -> startSos()), 8);
        }
        setScreen(c, false);
    }

    String nameForNumber(String num) {
        if (num == null) return "?";
        String clean = num.replaceAll("[^0-9]", "");
        for (String[] cc : allContacts()) {
            String ccn = cc[1].replaceAll("[^0-9]", "");
            if (clean.length() >= 7 && ccn.length() >= 7 && clean.endsWith(ccn.substring(ccn.length() - 7))) return cc[0];
        }
        return num;
    }

    /* =================================================================
       СООБЩЕНИЯ
       ================================================================= */
    boolean smsHas(String key, String id) {
        for (String s : P.getString(key, "").split("\n")) if (s.equals(id)) return true;
        return false;
    }

    void smsAdd(String key, String id) {
        String cur = P.getString(key, "");
        P.edit().putString(key, cur.isEmpty() ? id : cur + "\n" + id).apply();
    }

    void showSmsList() {
        setTitle("Сообщения");
        LinearLayout c = col();
        addSpaced(c, bigI("mail", "НАПИСАТЬ НОВОЕ", 0xFF3FAE4C, Art.CREAM, v -> showContacts(false)), 10);
        boolean any = false;
        try {
            Cursor cur = getContentResolver().query(Uri.parse("content://sms/inbox"),
                    new String[]{"_id", "address", "body", "read"}, null, null, "date DESC LIMIT 20");
            if (cur != null) {
                while (cur.moveToNext()) {
                    String id = cur.getString(0);
                    if (smsHas("smsHidden", id)) continue;
                    any = true;
                    String addr = cur.getString(1);
                    String body = cur.getString(2);
                    boolean read = smsHas("smsRead", id) || "1".equals(cur.getString(3));
                    String shortB = body.length() > 70 ? body.substring(0, 70) + "…" : body;
                    final String fid = id, faddr = addr, fbody = body;
                    Button b = bigI("mail", (read ? "" : "● ") + "От: " + nameForNumber(addr) + "\n" + shortB + (read ? "\n(прочитано)" : ""), Art.MUSTARD, Art.BROWN, v -> showSmsDetail(fid, faddr, fbody));
                    b.setTextSize(14 * FS * SC);
                    if (read) b.setAlpha(0.75f);
                    addSpaced(c, b, 10);
                }
                cur.close();
            }
        } catch (Exception e) {}
        if (!any) c.addView(tv("Сообщений пока нет или нет доступа к СМС.", 14, Art.BROWN, true));
        setScreen(c, false);
    }

    void showSmsDetail(String id, String addr, String body) {
        setTitle("Сообщение");
        LinearLayout c = col();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable cg = new GradientDrawable();
        cg.setColor(Art.CREAM); cg.setCornerRadius(dp(18));
        cg.setStroke(dp(3), Art.RUST);
        box.setBackground(cg); box.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.setGravity(Gravity.START);
        box.addView(tv("От: " + nameForNumber(addr), 14, Art.BROWN, true));
        box.addView(tv(body, 18, Art.GREEN, true));
        addSpaced(c, box, 10);
        addSpaced(c, bigI("sound", "ПРОЧИТАТЬ ВСЛУХ", Art.MUSTARD, Art.BROWN, v -> speak(body)), 8);
        addSpaced(c, bigI("mail", "ОТВЕТИТЬ", 0xFF3FAE4C, Art.CREAM, v -> showCompose(nameForNumber(addr), addr)), 8);
        addSpaced(c, bigI("clock", "ПОМЕТИТЬ ПРОЧИТАННЫМ", Art.MUSTARD, Art.BROWN, v -> { smsAdd("smsRead", id); say("Пометил как прочитанное."); showSmsList(); }), 8);
        addSpaced(c, bigI("x", "УДАЛИТЬ", Art.RUST, Art.CREAM, v -> confirmDialog("Удалить сообщение?", () -> { smsAdd("smsHidden", id); say("Сообщение убрано из списка."); showSmsList(); })), 8);
        setScreen(c, false);
    }

    void showCompose(String name, String num) {
        stopCompListen();
        composeActive = true;
        composeName = name; composeNum = num;
        lastSmsStatus = "—";
        setTitle("Пишем: " + name);
        LinearLayout c = col();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable cg = new GradientDrawable();
        cg.setColor(Art.CREAM); cg.setCornerRadius(dp(18));
        cg.setStroke(dp(4), Art.RUST);
        box.setBackground(cg); box.setPadding(dp(14), dp(12), dp(14), dp(12));
        composeEt = new EditText(this);
        composeEt.setTextSize(20 * FS * SC); composeEt.setMinLines(4);
        composeEt.setBackgroundColor(Color.TRANSPARENT);
        composeEt.setTextColor(Art.GREEN);
        composeEt.setHint("Говорите текст — я запишу сам");
        composeEt.setHintTextColor(Art.BROWN);
        box.addView(composeEt);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.topMargin = dp(6); bp.bottomMargin = dp(6);
        box.setLayoutParams(bp);
        addSpaced(c, box, 6);
        smsStatusView = tv("Статус: —", 14, Art.BROWN, true);
        addSpaced(c, smsStatusView, 8);
        H.post(new Runnable() { public void run() { if (composeActive && smsStatusView != null) { smsStatusView.setText("Статус: " + lastSmsStatus); H.postDelayed(this, 1500); } } });
        addSpaced(c, bigI("sound", "ПРОЧИТАТЬ ВСЛУХ", Art.MUSTARD, Art.BROWN, v -> speak(composeEt.getText().toString())), 8);
        composeSend = () -> {
            composeActive = false;
            stopCompListen();
            sendSms(composeNum, composeEt.getText().toString());
            say("Отправлено: " + composeName);
            showMain();
        };
        composeCancel = () -> { composeActive = false; stopCompListen(); showMain(); };
        addSpaced(c, bigI("mail", "ОТПРАВИТЬ", 0xFF3FAE4C, Art.CREAM, v -> composeSend.run()), 8);
        addSpaced(c, big("ОТМЕНА", Art.MUSTARD, Art.BROWN, v -> composeCancel.run()), 8);
        setScreen(c, false);
        say("Кому пишем: " + name + ". Говорите текст, я запишу.");
        H.postDelayed(this::startCompListen, 800);
    }

    void startCompListen() {
        if (compSr != null) return;
        if (composeEt == null) return;
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return;
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
            public void onError(int e) { H.post(() -> { stopCompListen(); if (composeEt != null && composeActive) H.postDelayed(() -> startCompListen(), 500); }); }
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
                    if (composeActive) H.postDelayed(() -> startCompListen(), 500);
                });
            }
            public void onPartialResults(Bundle r) {}
            public void onEvent(int i, Bundle b) {}
        });
        try { compSr.startListening(it); } catch (Exception e) { compSr = null; }
    }

    void sendSms(String num, String text) {
        try {
            SmsManager sm = SmsManager.getDefault();
            PendingIntent si = PendingIntent.getBroadcast(this, 1001, new Intent("ru.vnuchok.SMS_SENT"), PendingIntent.FLAG_IMMUTABLE);
            PendingIntent di = PendingIntent.getBroadcast(this, 1002, new Intent("ru.vnuchok.SMS_DELIVERED"), PendingIntent.FLAG_IMMUTABLE);
            sm.sendTextMessage(num, null, text, si, di);
            lastSmsStatus = "отправлено…";
        } catch (Exception e) { lastSmsStatus = "ошибка отправки"; say("СМС не ушло."); }
    }

    /* =================================================================
       НАПОМИНАНИЯ И БУДИЛЬНИК (с отступами между кнопками)
       ================================================================= */
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
            Button b = big(when + "\n" + r.get(i)[1] + "\n" + until(ms), Art.MUSTARD, Art.BROWN, v -> say("Напоминание: " + r.get(idx)[1] + " в " + when));
            b.setTextSize(14 * FS * SC);
            b.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            rw.addView(b);
            Button db = big("✕", Art.RUST, Art.CREAM, v -> {
                AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                am.cancel(PendingIntent.getBroadcast(this, code, new Intent(this, ReminderReceiver.class), PendingIntent.FLAG_IMMUTABLE));
                List<String[]> x = rems(); x.remove(idx); saveRems(x); say("Убрал."); showReminders();
            });
            db.setLayoutParams(new LinearLayout.LayoutParams(dp(70), ViewGroup.LayoutParams.WRAP_CONTENT));
            rw.addView(db);
            addSpaced(c, rw, 10);
        }
        if (!any) c.addView(tv("(пока ничего нет)", 14, Art.BROWN, false));
    }

    void showReminders() {
        setTitle("Напоминания");
        LinearLayout c = col();
        addSpaced(c, bigI("memo", "СОЗДАТЬ НАПОМИНАНИЕ", 0xFF3FAE4C, Art.CREAM, v -> { selDate = Calendar.getInstance(); showRemEditor(); }), 10);
        addSpaced(c, bigI("memo", "Через час: таблетки", Art.MUSTARD, Art.BROWN, v -> { addReminder(System.currentTimeMillis() + 3600000, "Пора принять таблетки!"); say("Напомню через час."); showReminders(); }), 10);
        addSpaced(c, bigI("memo", "Утром в 9:00: таблетки", Art.MUSTARD, Art.BROWN, v -> { addReminder(atTime(9, 0), "Пора принять таблетки!"); say("Напомню утром в девять."); showReminders(); }), 10);
        c.addView(tv("Уже стоит:", 16, Art.BROWN, true));
        showRemList(c, "rem");
        setScreen(c, false);
    }

    void addTimeRow(LinearLayout c, String label, final int[] val, int mod, int step) {
        LinearLayout r = row();
        r.setGravity(Gravity.CENTER);
        TextView lt = tv(label, 16, Art.GREEN, true);
        lt.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(lt);
        Button minus = big("−", Art.MUSTARD, Art.BROWN, v -> { val[0] = (val[0] - step + mod) % mod; ((TextView) r.getChildAt(2)).setText(String.format(Locale.getDefault(), "%02d", val[0])); });
        minus.setTextSize(28 * FS * SC);
        minus.setLayoutParams(new LinearLayout.LayoutParams(dp(84), ViewGroup.LayoutParams.WRAP_CONTENT));
        final TextView disp = tv(String.format(Locale.getDefault(), "%02d", val[0]), 34, Art.GREEN, true);
        disp.setGravity(Gravity.CENTER);
        disp.setMinWidth(dp(110));
        Button plus = big("+", Art.MUSTARD, Art.BROWN, v -> { val[0] = (val[0] + step) % mod; ((TextView) r.getChildAt(2)).setText(String.format(Locale.getDefault(), "%02d", val[0])); });
        plus.setTextSize(28 * FS * SC);
        plus.setLayoutParams(new LinearLayout.LayoutParams(dp(84), ViewGroup.LayoutParams.WRAP_CONTENT));
        r.addView(minus); r.addView(disp); r.addView(plus);
        addSpaced(c, r, 8);
    }

    void showRemEditor() {
        setTitle("Новое напоминание");
        LinearLayout c = col();
        final EditText en = new EditText(this); en.setTextSize(20 * FS * SC); en.setHint("Название (например: таблетки)");
        c.addView(en);
        final int[] hv = {9};
        final int[] mv = {0};
        addTimeRow(c, "ЧАСЫ", hv, 24, 1);
        addTimeRow(c, "МИНУТЫ", mv, 60, 5);
        addSpaced(c, bigI("clock", "Дата: " + selDate.get(Calendar.DAY_OF_MONTH) + "." + (selDate.get(Calendar.MONTH) + 1) + "." + selDate.get(Calendar.YEAR), Art.MUSTARD, Art.BROWN, v -> showCalendarPick()), 8);
        addSpaced(c, big("💾 СОХРАНИТЬ", 0xFF3FAE4C, Art.CREAM, v -> {
            String name = en.getText().toString().trim();
            if (name.isEmpty()) name = "Напоминание";
            Calendar cc = (Calendar) selDate.clone();
            cc.set(Calendar.HOUR_OF_DAY, hv[0]);
            cc.set(Calendar.MINUTE, mv[0]);
            cc.set(Calendar.SECOND, 0);
            if (cc.before(Calendar.getInstance())) cc.add(Calendar.DAY_OF_YEAR, 1);
            addReminder(cc.getTimeInMillis(), name);
            say("Напоминание создано.");
            showReminders();
        }), 8);
        addSpaced(c, big("ОТМЕНА", Art.MUSTARD, Art.BROWN, v -> showReminders()), 8);
        setScreen(c, false);
    }

    void showCalendarPick() {
        if (calCur == null) calCur = Calendar.getInstance();
        renderCalendar();
    }

    void renderCalendar() {
        setTitle("Выбор даты");
        LinearLayout c = col();
        LinearLayout hr = row();
        Button prev = big("◀", Art.MUSTARD, Art.BROWN, v -> { calCur.add(Calendar.MONTH, -1); renderCalendar(); });
        TextView title = tv(MN[calCur.get(Calendar.MONTH)] + " " + calCur.get(Calendar.YEAR), 18, Art.GREEN, true);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        title.setGravity(Gravity.CENTER);
        Button next = big("▶", Art.MUSTARD, Art.BROWN, v -> { calCur.add(Calendar.MONTH, 1); renderCalendar(); });
        prev.setLayoutParams(new LinearLayout.LayoutParams(dp(84), ViewGroup.LayoutParams.WRAP_CONTENT));
        next.setLayoutParams(new LinearLayout.LayoutParams(dp(84), ViewGroup.LayoutParams.WRAP_CONTENT));
        hr.addView(prev); hr.addView(title); hr.addView(next);
        addSpaced(c, hr, 8);
        LinearLayout wr = row();
        for (String d : DN) {
            TextView w = tv(d, 14, Art.BROWN, true);
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
            TextView e = tv("", 15, Art.GREEN, false);
            e.setLayoutParams(new LinearLayout.LayoutParams(0, dp(48), 1f));
            dr.addView(e);
            cell++;
        }
        for (int d = 1; d <= dim; d++) {
            if (cell % 7 == 0) { dr = row(); c.addView(dr); }
            final int day = d;
            boolean isToday = day == today.get(Calendar.DAY_OF_MONTH) && calCur.get(Calendar.MONTH) == today.get(Calendar.MONTH) && calCur.get(Calendar.YEAR) == today.get(Calendar.YEAR);
            Button db = big(String.valueOf(day), isToday ? Art.GREEN : Art.MUSTARD, isToday ? Art.CREAM : Art.BROWN, v -> {
                selDate.set(Calendar.YEAR, calCur.get(Calendar.YEAR));
                selDate.set(Calendar.MONTH, calCur.get(Calendar.MONTH));
                selDate.set(Calendar.DAY_OF_MONTH, day);
                showRemEditor();
            });
            db.setTextSize(16 * FS * SC);
            db.setLayoutParams(new LinearLayout.LayoutParams(0, dp(48), 1f));
            dr.addView(db);
            cell++;
        }
        while (cell % 7 != 0) {
            if (dr == null) { dr = row(); c.addView(dr); }
            TextView e = tv("", 15, Art.GREEN, false);
            e.setLayoutParams(new LinearLayout.LayoutParams(0, dp(48), 1f));
            dr.addView(e);
            cell++;
        }
        setScreen(c, false);
    }

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
        setTitle("Будильник");
        LinearLayout c = col();
        final int[] hv = {7};
        final int[] mv = {0};
        addTimeRow(c, "ЧАСЫ", hv, 24, 1);
        addTimeRow(c, "МИНУТЫ", mv, 60, 5);
        c.addView(tv("Дни недели:", 16, Art.GREEN, true));
        final LinearLayout days = row();
        for (int i = 0; i < 7; i++) {
            final int bit = i;
            boolean on = (alarmMask & (1 << bit)) != 0;
            Button db = big(DN[i], on ? 0xFF3FAE4C : Art.MUSTARD, on ? Art.CREAM : Art.BROWN, v -> {
                alarmMask ^= (1 << bit);
                boolean nowOn = (alarmMask & (1 << bit)) != 0;
                int nb = nowOn ? 0xFF3FAE4C : Art.MUSTARD;
                v.setBackground(Art.pill(this, nb));
                holder(v).bg = hex(nb);
                ((Button) v).setTextColor(nowOn ? Art.CREAM : Art.BROWN);
            });
            db.setTextSize(14 * FS * SC);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(58), 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            db.setLayoutParams(lp);
            days.addView(db);
        }
        addSpaced(c, days, 6);
        c.addView(tv("(ничего не нажато = каждый день)", 13, Art.BROWN, false));
        addSpaced(c, bigI("alarm", "ПОСТАВИТЬ БУДИЛЬНИК", 0xFF3FAE4C, Art.CREAM, v -> {
            long ms = nextAlarm(hv[0], mv[0], alarmMask);
            addAlarm(hv[0], mv[0], alarmMask, "Будильник! Пора вставать!");
            say("Будильник поставлен. " + until(ms));
            showAlarms();
        }), 10);
        c.addView(tv("Уже стоит:", 16, Art.BROWN, true));
        showRemList(c, "alm");
        setScreen(c, false);
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
        remOverlay.setBackgroundColor(Art.RUST);
        remOverlay.setPadding(dp(30), dp(30), dp(30), dp(30));
        remOverlay.addView(tv("💊", 60, Art.CREAM, false));
        TextView bt = tv(text.toUpperCase(), 30, Art.CREAM, true);
        bt.setGravity(Gravity.CENTER);
        remOverlay.addView(bt);
        addSpaced(remOverlay, big("✅ ПРИНЯЛ", 0xFF3FAE4C, Art.CREAM, v -> { frame.removeView(remOverlay); remOverlay = null; say("Молодец! Отметил."); }), 10);
        addSpaced(remOverlay, bigI("alarm", "НАПОМНИ ЧЕРЕЗ ЧАС", Art.MUSTARD, Art.BROWN, v -> {
            frame.removeView(remOverlay); remOverlay = null;
            addReminder(System.currentTimeMillis() + 3600000, text);
            say("Напомню ещё раз через час.");
        }), 10);
        frame.addView(remOverlay);
        try { Vibrator vb = (Vibrator) getSystemService(VIBRATOR_SERVICE); vb.vibrate(VibrationEffect.createOneShot(800, 255)); } catch (Exception e) {}
        say("Внимание! " + text);
    }

    /* =================================================================
       SOS
       ================================================================= */
    void startSos() {
        if (checkSelfPermission(android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CALL_PHONE}, 77);
        }
        setTitle("Экстренный вызов");
        LinearLayout c = col();
        c.addView(tv("ВЫЗЫВАЕМ ПОМОЩЬ!", 26, Art.RUST, true));
        TextView num = tv("5", 80, Art.RUST, true);
        c.addView(num);
        c.addView(tv("Если случайно — жмите ОТМЕНА", 16, Art.GREEN, true));
        addSpaced(c, big("✋ ОТМЕНА", 0xFF8A8A8A, Art.CREAM, v -> cancelSos()), 8);
        setScreen(c, false);
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
        boolean called = false;
        if (checkSelfPermission(android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            try { startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:112"))); called = true; } catch (Exception e) {}
        }
        if (!called) {
            try { startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))); } catch (Exception e) {}
            say("Телефон не получил разрешение на прямой звонок. Нажмите зелёную кнопку вызова на экране набора.");
        }
        setTitle("Звонок в 112");
        LinearLayout c = col();
        c.addView(tv("☎ ИДЁТ ЗВОНОК В 112…", 24, Art.RUST, true));
        c.addView(tv("СМС отправлено родным:", 16, Art.GREEN, true));
        c.addView(tv(sms, 14, Art.GREEN, true));
        setScreen(c, false);
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

    /* =================================================================
       ПРИЛОЖЕНИЯ / ПРОЧЕЕ
       ================================================================= */
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
        return out;
    }

    void openPackage(String pkg) {
        try {
            Intent li = getPackageManager().getLaunchIntentForPackage(pkg);
            if (li != null) startActivity(li); else say("Не удалось открыть.");
        } catch (Exception e) { say("Не удалось открыть."); }
    }

    void toggleVibro() {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        boolean vib = am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE;
        try {
            am.setRingerMode(vib ? AudioManager.RINGER_MODE_NORMAL : AudioManager.RINGER_MODE_VIBRATE);
            say(vib ? "Звук включён." : "Виброрежим включён. Кнопки теперь без звука.");
        } catch (Exception e) {
            try { startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)); } catch (Exception e2) {}
            say("Нужно разрешить приложению управлять звуком. Откройте: Приложения, Настройки, Разрешения.");
        }
    }

    /* =================================================================
       БАТАРЕЯ
       ================================================================= */
    class BatteryReceiver extends BroadcastReceiver {
        boolean wasCharging = false, wasFull = false;
        @Override public void onReceive(Context c, Intent i) {
            int level = i.getIntExtra("level", -1), scale = i.getIntExtra("scale", -1);
            int plugged = i.getIntExtra("plugged", 0);
            boolean nowCharging = plugged != 0;
            if (nowCharging && !wasCharging) say("Зарядка началась.");
            if (!nowCharging && wasCharging) say("Телефон снят с зарядки.");
            wasCharging = nowCharging;
            charging = nowCharging;
            if (level >= 0 && scale > 0) lastPct = level * 100 / scale;
            if (lastPct >= 99 && nowCharging && !wasFull) { wasFull = true; say("Батарея полностью заряжена. Можно отключать от зарядки."); }
            if (lastPct < 95) wasFull = false;
            refreshStatus();
            if (level < 0 || scale <= 0) return;
            int pct = lastPct;
            if (plugged != 0) {
                if (battOverlay != null) { frame.removeView(battOverlay); battOverlay = null; }
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
        battOverlay.setBackgroundColor(Art.RUST);
        battOverlay.setPadding(dp(30), dp(30), dp(30), dp(30));
        battOverlay.addView(tv("🪫", 60, Art.CREAM, false));
        TextView t = tv("ТЕЛЕФОН СЕЙЧАС ВЫКЛЮЧИТСЯ!\nПОСТАВЬТЕ НА ЗАРЯДКУ!", 28, Art.CREAM, true);
        t.setGravity(Gravity.CENTER);
        battOverlay.addView(t);
        battOverlay.addView(tv("Это окно исчезнет само, когда начнётся зарядка", 14, 0xFFFFE0D8, true));
        frame.addView(battOverlay);
        say("Внимание! Батарея почти села! Поставьте телефон на зарядку!");
    }

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
