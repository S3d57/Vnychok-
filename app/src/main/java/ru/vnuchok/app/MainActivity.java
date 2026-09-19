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
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    static class THolder { String bg; ObjectAnimator oa; }

    static class Icon extends Drawable {
        String k; Paint f, s; int level = -1, bars = -1;
        Icon(String k, int col) {
            this.k = k;
            f = new Paint(Paint.ANTI_ALIAS_FLAG); f.setColor(col); f.setStyle(Paint.Style.FILL);
            s = new Paint(Paint.ANTI_ALIAS_FLAG); s.setColor(col); s.setStyle(Paint.Style.STROKE); s.setStrokeWidth(8f); s.setStrokeCap(Paint.Cap.ROUND); s.setStrokeJoin(Paint.Join.ROUND);
        }
        @Override public void draw(Canvas c) {
            Rect b = getBounds();
            c.save(); c.translate(b.left, b.top);
            c.scale(b.width() / 100f, b.height() / 100f);
            switch (k) {
                case "phone":
                    s.setStrokeWidth(16f);
                    c.drawArc(18, 22, 82, 86, 180, 180, false, s);
                    c.drawCircle(18, 54, 12, f);
                    c.drawCircle(82, 54, 12, f);
                    break;
                case "mail":
                    c.drawRoundRect(8, 20, 92, 80, 12, 12, s);
                    c.drawLine(14, 28, 50, 56, s);
                    c.drawLine(86, 28, 50, 56, s);
                    break;
                case "dial":
                    for (int i = 0; i < 9; i++) c.drawCircle(26 + (i % 3) * 24, 26 + (i / 3) * 24, 9, f);
                    break;
                case "cam":
                    c.drawRoundRect(6, 28, 94, 82, 14, 14, s);
                    c.drawCircle(50, 55, 15, s);
                    c.drawCircle(50, 55, 5, f);
                    c.drawRoundRect(36, 16, 60, 30, 6, 6, f);
                    c.drawCircle(80, 40, 4, f);
                    break;
                case "album":
                    c.drawRoundRect(8, 14, 92, 86, 12, 12, s);
                    Path pa = new Path(); pa.moveTo(20, 74); pa.lineTo(40, 46); pa.lineTo(56, 64); pa.lineTo(68, 50); pa.lineTo(82, 74); pa.close(); c.drawPath(pa, f);
                    c.drawCircle(68, 32, 8, f);
                    break;
                case "memo":
                    c.drawRoundRect(16, 8, 76, 92, 10, 10, s);
                    Path fold = new Path(); fold.moveTo(76, 8); fold.lineTo(76, 26); fold.lineTo(58, 8); fold.close(); c.drawPath(fold, f);
                    c.drawLine(28, 34, 64, 34, s);
                    c.drawLine(28, 50, 64, 50, s);
                    c.drawLine(28, 66, 50, 66, s);
                    c.save(); c.rotate(45, 74, 74); c.drawRoundRect(58, 68, 96, 80, 6, 6, f); Path tip = new Path(); tip.moveTo(58, 68); tip.lineTo(50, 74); tip.lineTo(58, 80); tip.close(); c.drawPath(tip, f); c.restore();
                    break;
                case "alarm":
                    c.drawCircle(50, 58, 26, s);
                    c.drawLine(50, 58, 50, 42, s);
                    c.drawLine(50, 58, 62, 62, s);
                    c.drawCircle(50, 58, 4, f);
                    s.setStrokeWidth(9f);
                    c.drawArc(14, 16, 40, 42, 180, 140, false, s);
                    c.drawArc(60, 16, 86, 42, 320, 140, false, s);
                    c.drawLine(30, 80, 22, 92, s);
                    c.drawLine(70, 80, 78, 92, s);
                    break;
                case "clock":
                    c.drawCircle(50, 50, 32, s);
                    c.drawLine(50, 50, 50, 30, s);
                    c.drawLine(50, 50, 64, 56, s);
                    c.drawCircle(50, 50, 4, f);
                    break;
                case "torch":
                    Path ph = new Path(); ph.moveTo(32, 20); ph.lineTo(68, 20); ph.lineTo(60, 44); ph.lineTo(40, 44); ph.close(); c.drawPath(ph, f);
                    c.drawRoundRect(40, 44, 60, 90, 8, 8, f);
                    c.drawLine(50, 4, 50, 12, s);
                    c.drawLine(26, 8, 32, 14, s);
                    c.drawLine(74, 8, 68, 14, s);
                    c.drawLine(44, 56, 56, 56, s);
                    break;
                case "wrench":
                    s.setStrokeWidth(11f);
                    c.drawArc(16, 16, 48, 48, 60, 240, false, s);
                    c.save(); c.rotate(45, 50, 50); c.drawRoundRect(40, 44, 92, 58, 7, 7, f); c.restore();
                    c.drawCircle(82, 50, 5, f);
                    break;
                case "mic":
                    c.drawRoundRect(38, 6, 62, 48, 12, 12, f);
                    c.drawArc(26, 26, 74, 74, 0, 180, false, s);
                    c.drawLine(50, 74, 50, 86, s);
                    c.drawRoundRect(34, 86, 66, 94, 4, 4, f);
                    break;
                case "sos":
                    c.drawRoundRect(40, 14, 60, 86, 8, 8, f);
                    c.drawRoundRect(14, 40, 86, 60, 8, 8, f);
                    break;
                case "radio":
                    c.drawRoundRect(8, 34, 92, 84, 12, 12, s);
                    c.drawCircle(32, 59, 12, s);
                    c.drawCircle(32, 59, 4, f);
                    c.drawLine(56, 50, 82, 50, s);
                    c.drawLine(56, 62, 82, 62, s);
                    c.drawLine(56, 74, 74, 74, s);
                    c.drawLine(66, 34, 88, 10, s);
                    break;
                case "map":
                    Path pm = new Path(); pm.moveTo(12, 24); pm.lineTo(38, 14); pm.lineTo(62, 24); pm.lineTo(88, 14); pm.lineTo(88, 76); pm.lineTo(62, 86); pm.lineTo(38, 76); pm.lineTo(12, 86); pm.close(); c.drawPath(pm, s);
                    c.drawLine(38, 14, 38, 76, s);
                    c.drawLine(62, 24, 62, 86, s);
                    c.drawCircle(50, 44, 7, f);
                    break;
                case "music":
                    c.drawCircle(30, 76, 13, f);
                    c.drawCircle(66, 70, 13, f);
                    c.drawRect(41, 26, 47, 76, f);
                    c.drawRect(77, 20, 83, 70, f);
                    c.drawRoundRect(41, 14, 83, 30, 8, 8, f);
                    break;
                case "weather":
                    c.drawCircle(36, 34, 14, f);
                    for (int i = 0; i < 8; i++) { c.save(); c.rotate(i * 45f, 36, 34); c.drawLine(36, 12, 36, 6, s); c.restore(); }
                    c.drawCircle(56, 62, 14, f);
                    c.drawCircle(72, 66, 11, f);
                    c.drawRoundRect(42, 62, 86, 77, 8, 8, f);
                    break;
                case "web":
                    c.drawCircle(50, 50, 34, s);
                    c.drawOval(new RectF(34, 16, 66, 84), s);
                    c.drawLine(16, 50, 84, 50, s);
                    c.drawArc(20, 26, 80, 74, 200, 140, false, s);
                    c.drawArc(20, 26, 80, 74, 20, 140, false, s);
                    break;
                case "wifi":
                    s.setStrokeWidth(9f);
                    c.drawArc(14, 26, 86, 98, 210, 120, false, s);
                    c.drawArc(28, 40, 72, 84, 210, 120, false, s);
                    c.drawArc(40, 52, 60, 72, 210, 120, false, s);
                    c.drawCircle(50, 76, 7, f);
                    break;
                case "folder":
                    c.drawRoundRect(10, 24, 90, 82, 10, 10, f);
                    c.drawRoundRect(10, 16, 44, 34, 8, 8, f);
                    c.drawRoundRect(16, 44, 84, 76, 8, 8, s);
                    break;
                case "sound":
                    Path sp = new Path(); sp.moveTo(14, 40); sp.lineTo(32, 40); sp.lineTo(52, 22); sp.lineTo(52, 78); sp.lineTo(32, 60); sp.lineTo(14, 60); sp.close(); c.drawPath(sp, f);
                    c.drawArc(58, 32, 84, 68, -45, 90, false, s);
                    c.drawArc(66, 24, 96, 76, -45, 90, false, s);
                    break;
                case "silent":
                    Path sq = new Path(); sq.moveTo(14, 40); sq.lineTo(32, 40); sq.lineTo(52, 22); sq.lineTo(52, 78); sq.lineTo(32, 60); sq.lineTo(14, 60); sq.close(); c.drawPath(sq, f);
                    c.drawLine(64, 36, 90, 64, s);
                    c.drawLine(90, 36, 64, 64, s);
                    break;
                case "gear":
                    for (int i = 0; i < 8; i++) { c.save(); c.rotate(i * 45f, 50, 50); c.drawRoundRect(43, 10, 57, 26, 5, 5, f); c.restore(); }
                    s.setStrokeWidth(13f);
                    c.drawCircle(50, 50, 22, s);
                    c.drawCircle(50, 50, 7, f);
                    break;
                case "batt": float lv = level < 0 ? 50 : level; s.setStrokeWidth(7); c.drawRoundRect(4, 26, 82, 74, 12, 12, s); c.drawRoundRect(86, 40, 96, 60, 4, 4, f); float fw = Math.max(6, 66 * lv / 100f); c.drawRoundRect(11, 33, 11 + fw, 67, 6, 6, f); break;
                case "battc": s.setStrokeWidth(7); c.drawRoundRect(4, 26, 82, 74, 12, 12, s); c.drawRoundRect(86, 40, 96, 60, 4, 4, f); Path bo = new Path(); bo.moveTo(50, 30); bo.lineTo(34, 52); bo.lineTo(44, 52); bo.lineTo(38, 70); bo.lineTo(58, 46); bo.lineTo(47, 46); bo.close(); c.drawPath(bo, f); break;
                case "sig": for (int i = 0; i < 4; i++) { float bh = 25 + i * 20; boolean on = bars < 0 || i < bars; c.drawRoundRect(10 + i * 24, 92 - bh, 26 + i * 24, 92, 5, 5, on ? f : s); } break;
                case "home":
                    Path hh = new Path(); hh.moveTo(50, 12); hh.lineTo(90, 48); hh.lineTo(78, 48); hh.lineTo(78, 88); hh.lineTo(58, 88); hh.lineTo(58, 62); hh.lineTo(42, 62); hh.lineTo(42, 88); hh.lineTo(22, 88); hh.lineTo(22, 48); hh.lineTo(10, 48); hh.close(); c.drawPath(hh, f);
                    break;
                case "people":
                    c.drawCircle(38, 30, 13, f);
                    c.drawRoundRect(18, 48, 58, 86, 16, 16, f);
                    c.drawCircle(68, 36, 10, f);
                    c.drawRoundRect(52, 52, 86, 86, 13, 13, f);
                    break;
                case "back":
                    s.setStrokeWidth(10f);
                    c.drawLine(72, 50, 32, 50, s);
                    c.drawLine(46, 34, 30, 50, s);
                    c.drawLine(46, 66, 30, 50, s);
                    break;
                case "book":
                    Path bl = new Path(); bl.moveTo(50, 22); bl.quadTo(30, 12, 12, 20); bl.lineTo(12, 78); bl.quadTo(30, 70, 50, 80); bl.close(); c.drawPath(bl, s);
                    Path br = new Path(); br.moveTo(50, 22); br.quadTo(70, 12, 88, 20); br.lineTo(88, 78); br.quadTo(70, 70, 50, 80); br.close(); c.drawPath(br, s);
                    c.drawLine(50, 22, 50, 80, s);
                    c.drawLine(22, 34, 40, 30, s);
                    c.drawLine(22, 48, 40, 44, s);
                    c.drawLine(60, 30, 78, 34, s);
                    c.drawLine(60, 44, 78, 48, s);
                    break;
                case "x":
                    s.setStrokeWidth(10f);
                    c.drawLine(30, 30, 70, 70, s);
                    c.drawLine(70, 30, 30, 70, s);
                    break;
                case "gallery":
                    c.drawRoundRect(6, 16, 94, 84, 10, 10, s);
                    c.drawCircle(28, 38, 10, f);
                    Path mp = new Path(); mp.moveTo(16, 74); mp.lineTo(36, 50); mp.lineTo(50, 64); mp.lineTo(64, 48); mp.lineTo(84, 74); mp.close(); c.drawPath(mp, f);
                    break;
            }
            c.restore();
        }
        @Override public void setAlpha(int a) {}
        @Override public void setColorFilter(ColorFilter cf) {}
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    Icon ic(String k, String col, int size) { Icon i = new Icon(k, Color.parseColor(col)); i.setBounds(0, 0, size, size); return i; }

    Handler H = new Handler(Looper.getMainLooper());
    FrameLayout frame; ScrollView scroll; LinearLayout rootLin, headerSlot, bottomSlot;
    TextView caption, userSay, timeView, sbPopup, sbBatt, sbOper, sbNet, weatherView, tempView;
    Button micBtn, msgTile, callTile, appsTile, dlgMic, dlgClose;
    LinearLayout dlgCard, micArea;
    boolean dlgOpen = false;
    TextToSpeech tts; boolean ttsReady;
    SpeechRecognizer sr, cSr, compSr;
    boolean torchOn, battWarned, listening, pendingConfirm, charging, permAsked, composeActive;
    int lastPct = -1, sigBars = 3; String sigLabel = "";
    int contactShown = 30;
    String lastSmsStatus = "";
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
    
    // Retro theme colors
    String BG, CARD, TILE, TFG, ACC, DARK, EDGE, MUT, SBBG, SBFG, RUST, MUSTARD, BROWN, OLIVE, CREAM;
    String[] tilePal = null;

    static final String DEF_CONTACTS = "Дочь Маша|+79000000001\nВнук Миша|+7900000002\nВнучка Оля|+7900000003\nСоседка Нина|+7900000004\nВрач Ирина|+7900000005";
    static final String DEF_TILES = "call,sms,apps,rem,alarm,torch";
    static final String[] DN = {"ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС"};
    static final String[] MN = {"ЯНВАРЯ", "ФЕВРАЛЯ", "МАРТА", "АПРЕЛЯ", "МАЯ", "ИЮНЯ", "ИЮЛЯ", "АВГУСТА", "СЕНТЯБРЯ", "ОКТЯБРЯ", "НОЯБРЯ", "ДЕКАБРЯ"};
    static final String[] THN = {"РЕТРО", "ГОРЧИЦА", "НОЧЬ", "КАРАМЕЛЬ"};

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        P = getSharedPreferences("vnuchok", MODE_PRIVATE);
        FS = P.getFloat("fs", 1f);
        float dens = getResources().getDisplayMetrics().density;
        float wdp = getResources().getDisplayMetrics().widthPixels / dens;
        SC = Math.max(0.85f, Math.min(1.6f, wdp / 360f));
        applyTheme(P.getInt("theme", 0));
        askPerms();
        frame = new FrameLayout(this);
        frame.setBackgroundColor(Color.parseColor(BG));
        rootLin = new LinearLayout(this);
        rootLin.setOrientation(LinearLayout.VERTICAL);
        headerSlot = new LinearLayout(this);
        headerSlot.setOrientation(LinearLayout.VERTICAL);
        headerSlot.setVisibility(View.GONE);
        rootLin.addView(headerSlot, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        scroll = new ScrollView(this);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(Color.parseColor(BG));
        rootLin.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        bottomSlot = new LinearLayout(this);
        bottomSlot.setOrientation(LinearLayout.VERTICAL);
        bottomSlot.setPadding(dp(10), dp(4), dp(10), dp(10));
        rootLin.addView(bottomSlot, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        frame.addView(rootLin, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
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
        registerReceiver(new BroadcastReceiver() { public void onReceive(Context c, Intent i) { lastSmsStatus = "отправлено"; } }, new IntentFilter("ru.vnuchok.SMS_SENT"));
        registerReceiver(new BroadcastReceiver() { public void onReceive(Context c, Intent i) { lastSmsStatus = "доставлено"; } }, new IntentFilter("ru.vnuchok.SMS_DELIVERED"));
        missRun = new Runnable() { public void run() { checkMissed(); H.postDelayed(this, 20000); } };
        H.postDelayed(missRun, 10000);
        sigRun = new Runnable() { public void run() { pollSignal(); H.postDelayed(this, 15000); } };
        H.postDelayed(sigRun, 2000);
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

    void applyTheme(int t) {
        switch (t) {
            case 1: // Mustard retro
                BG = "#F5E6C8"; CARD = "#FFF8DC"; TILE = "#D4A574"; TFG = "#3E2723"; ACC = "#2E7D32"; 
                DARK = "#3E2723"; EDGE = "#8D6E63"; MUT = "#8D7B68"; SBBG = "#5D4037"; SBFG = "#FFF8DC"; 
                RUST = "#BF360C"; MUSTARD = "#FFB300"; BROWN = "#6D4C41"; OLIVE = "#558B2F"; CREAM = "#FFF8E1";
                tilePal = new String[]{"#D4A574", "#BF360C", "#6D4C41", "#2E7D32"}; 
                break;
            case 2: // Night
                BG = "#1A1F1C"; CARD = "#263238"; TILE = "#37474F"; TFG = "#ECEFF1"; ACC = "#66BB6A"; 
                DARK = "#FFFFFF"; EDGE = "#66BB6A"; MUT = "#B0BEC5"; SBBG = "#37474F"; SBFG = "#ECEFF1"; 
                RUST = "#D84315"; MUSTARD = "#FFB300"; BROWN = "#5D4037"; OLIVE = "#558B2F"; CREAM = "#263238";
                tilePal = new String[]{"#37474F", "#FFB300", "#D84315", "#5D4037"}; 
                break;
            case 3: // Caramel
                BG = "#EFEBE9"; CARD = "#D7CCC8"; TILE = "#8D6E63"; TFG = "#3E2723"; ACC = "#5D4037"; 
                DARK = "#3E2723"; EDGE = "#6D4C41"; MUT = "#A1887F"; SBBG = "#6D4C41"; SBFG = "#EFEBE9"; 
                RUST = "#BF360C"; MUSTARD = "#FFB300"; BROWN = "#6D4C41"; OLIVE = "#558B2F"; CREAM = "#D7CCC8";
                tilePal = new String[]{"#8D6E63", "#FFB300", "#BF360C", "#6D4C41"}; 
                break;
            default: // Retro green
                BG = "#F1F8E9"; CARD = "#DCEDC8"; TILE = "#558B2F"; TFG = "#1B5E20"; ACC = "#33691E"; 
                DARK = "#1B5E20"; EDGE = "#689F38"; MUT = "#689F38"; SBBG = "#558B2F"; SBFG = "#F1F8E9"; 
                RUST = "#BF360C"; MUSTARD = "#FFB300"; BROWN = "#6D4C41"; OLIVE = "#558B2F"; CREAM = "#F1F8E9";
                tilePal = new String[]{"#558B2F", "#FFB300", "#BF360C", "#6D4C41"}; 
                break;
        }
    }

    String tileColor(int i) { return tilePal != null ? tilePal[i % tilePal.length] : TILE; }

    void say(String t) {
        lastSay = t;
        H.post(() -> { if (caption != null) caption.setText(t); });
        if (ttsReady && P.getBoolean("voice", true)) tts.speak(t, TextToSpeech.QUEUE_FLUSH, null, "v");
    }

    void speak(String t) { if (ttsReady && !t.isEmpty()) tts.speak(t, TextToSpeech.QUEUE_FLUSH, null, "s"); }

    int dp(int x) { return Math.round(x * getResources().getDisplayMetrics().density * SC); }
    int shade(int c, float f) { return Color.argb(255, Math.max(0, Math.min(255, (int) (Color.red(c) * f))), Math.max(0, Math.min(255, (int) (Color.green(c) * f))), Math.max(0, Math.min(255, (int) (Color.blue(c) * f)))); }

    GradientDrawable gd(String bg) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.parseColor(bg));
        g.setCornerRadius(dp(20));
        g.setStroke(dp(3), shade(Color.parseColor(bg), 0.6f));
        return g;
    }

    GradientDrawable gdRetro(String bg, String stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.parseColor(bg));
        g.setCornerRadius(dp(24));
        g.setStroke(dp(4), Color.parseColor(stroke));
        return g;
    }

    GradientDrawable pill(String bg) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.parseColor(bg));
        g.setCornerRadius(dp(60));
        return g;
    }

    GradientDrawable pillStroke(String bg, String stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.parseColor(bg));
        g.setCornerRadius(dp(60));
        g.setStroke(dp(3), Color.parseColor(stroke));
        return g;
    }

    TextView tv(String s, float size, String color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size * FS * SC); t.setTextColor(Color.parseColor(color));
        if (bold) t.getPaint().setFakeBoldText(true);
        t.setPadding(0, dp(3), 0, dp(3));
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
            GradientDrawable base = gd(bg);
            final GradientDrawable ring = new GradientDrawable();
            ring.setColor(Color.TRANSPARENT);
            ring.setCornerRadius(dp(22));
            ring.setStroke(dp(6), 0xFFFFC46B);
            LayerDrawable ld = new LayerDrawable(new Drawable[]{base, ring});
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
                v.setBackground(gd(th.bg));
                v.setElevation(dp(5));
            }, 500);
        }
    }

    Button big(String text, String bg, String fg, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text); b.setTextSize(20 * FS * SC); b.setTextColor(Color.parseColor(fg));
        b.setAllCaps(false); b.getPaint().setFakeBoldText(true);
        b.setBackground(gd(bg));
        THolder th = new THolder(); th.bg = bg; b.setTag(th);
        b.setElevation(dp(5));
        b.setPadding(dp(10), dp(14), dp(10), dp(16));
        b.setOnClickListener(v -> { pressFx(v); l.onClick(v); });
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
        b.setTextSize(14 * FS * SC); b.setTextColor(Color.parseColor(fg));
        b.setAllCaps(false); b.getPaint().setFakeBoldText(true);
        b.setBackground(gdRetro(bg, shade(Color.parseColor(bg), 0.5f)));
        THolder th = new THolder(); th.bg = bg; b.setTag(th);
        b.setElevation(dp(6));
        b.setCompoundDrawables(null, ic(kind, fg, dp(48)), null, null);
        b.setCompoundDrawablePadding(dp(8));
        b.setPadding(dp(6), dp(12), dp(6), dp(14));
        b.setOnClickListener(v -> { pressFx(v); l.onClick(v); });
        return b;
    }

    LinearLayout col() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(14), dp(8), dp(14), dp(8));
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        l.setClipChildren(false);
        l.setClipToPadding(false);
        l.setBackgroundColor(Color.parseColor(BG));
        return l;
    }

    LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setPadding(0, dp(3), 0, dp(6));
        l.setClipChildren(false);
        l.setClipToPadding(false);
        return l;
    }

    void setTitle(String t) { screenTitle = t; }

    void setScreen(LinearLayout c, boolean isMain) { setScreenHeader(c, isMain, null); }

    void setScreenHeader(LinearLayout c, boolean isMain, View header) {
        H.post(() -> {
            headerSlot.removeAllViews();
            if (header != null) { headerSlot.setVisibility(View.VISIBLE); headerSlot.addView(header); }
            else if (!isMain) {
                LinearLayout pillRow = row();
                pillRow.setPadding(dp(12), dp(10), dp(12), dp(4));
                LinearLayout hpill = new LinearLayout(this);
                hpill.setOrientation(LinearLayout.HORIZONTAL);
                hpill.setGravity(Gravity.CENTER_VERTICAL);
                hpill.setBackground(pillStroke(SBBG, SBFG));
                hpill.setPadding(dp(8), dp(6), dp(20), dp(6));
                Button backB = new Button(this);
                backB.setBackground(pill(CARD));
                backB.setTag(null);
                backB.setCompoundDrawables(ic("back", DARK, dp(30)), null, null, null);
                backB.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));
                backB.setOnClickListener(v -> showMain());
                TextView tt = tv("  " + screenTitle, 22, SBFG, true);
                hpill.addView(backB); hpill.addView(tt);
                pillRow.addView(hpill, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                headerSlot.setVisibility(View.VISIBLE);
                headerSlot.addView(pillRow);
            } else headerSlot.setVisibility(View.GONE);
            bottomSlot.removeAllViews();
            if (isMain) {
                Button sosB = bigI("sos", "SOS — ВЫЗВАТЬ ПОМОЩЬ (112)", RUST, "#FFFFFF", v -> startSos());
                sosB.setTextSize(20 * FS * SC);
                sosB.setMinHeight(dp(64));
                bottomSlot.addView(sosB);
            } else {
                Button backB = bigI("home", "НАЗАД", SBBG, SBFG, v -> showMain());
                backB.setTextSize(20 * FS * SC);
                backB.setMinHeight(dp(60));
                bottomSlot.addView(backB);
            }
            scroll.removeAllViews();
            scroll.addView(c);
            scroll.scrollTo(0, 0);
            c.setAlpha(0f);
            c.setTranslationY(dp(10));
            c.animate().alpha(1f).translationY(0f).setDuration(500);
        });
    }

    String curTime() {
        Calendar c = Calendar.getInstance();
        return String.format(Locale.getDefault(), "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    String curDate() {
        Calendar c = Calendar.getInstance();
        String[] days = {"ВОСКРЕСЕНЬЕ", "ПОНЕДЕЛЬНИК", "ВТОРНИК", "СРЕДА", "ЧЕТВЕРГ", "ПЯТНИЦА", "СУББОТА"};
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
                String col = pc <= 20 ? "#C0392B" : SBFG;
                sbBatt.setTextColor(Color.parseColor(col));
                sbBatt.setText(pc + "%");
                Icon bi = new Icon(charging ? "battc" : "batt", Color.parseColor(col)); bi.level = pc; bi.setBounds(0, 0, dp(34), dp(34));
                sbBatt.setCompoundDrawables(bi, null, null, null);
                sbBatt.setCompoundDrawablePadding(dp(6));
                blink(sbBatt, charging || pc <= 15);
            }
            if (sbOper != null) {
                String op = "";
                try { TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE); op = tm.getNetworkOperatorName(); } catch (Exception e) {}
                sbOper.setText((op == null || op.isEmpty() ? "ВНУЧОК" : op) + (sigLabel.isEmpty() ? "" : " " + sigLabel));
            }
            if (sbNet != null) {
                boolean net = false, wifi = false;
                try {
                    ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                    Network n = cm.getActiveNetwork();
                    if (n != null) { net = true; NetworkCapabilities nc = cm.getNetworkCapabilities(n); wifi = nc != null && nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI); }
                } catch (Exception e) {}
                sbNet.setText(wifi ? "" : (net ? "" : ""));
                Icon wi = new Icon(wifi ? "wifi" : (net ? "sig" : "x"), Color.parseColor(SBFG));
                if (!wifi && net) wi.bars = sigBars;
                wi.setBounds(0, 0, dp(30), dp(30));
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
        frame.setBackgroundColor(Color.parseColor(BG));
        dlgOpen = false;

        LinearLayout mainContainer = col();
        mainContainer.setPadding(dp(12), dp(8), dp(12), dp(12));

        // Status bar
        LinearLayout sbar = row();
        sbar.setBackground(pill(SBBG));
        sbar.setPadding(dp(16), dp(10), dp(16), dp(10));
        sbar.setGravity(Gravity.CENTER_VERTICAL);
        sbBatt = tv("", 14, SBFG, true);
        sbOper = tv("", 14, SBFG, true);
        sbOper.setGravity(Gravity.CENTER);
        sbNet = tv("", 14, SBFG, true);
        sbNet.setGravity(Gravity.END);
        sbOper.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        sbNet.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        sbar.addView(sbBatt); sbar.addView(sbOper); sbar.addView(sbNet);
        mainContainer.addView(sbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        sbPopup = tv("", 14, "#C0392B", true);
        sbPopup.setVisibility(View.GONE);
        sbPopup.setPadding(dp(12), dp(6), dp(12), dp(6));
        sbPopup.setBackground(gdRetro(CREAM, RUST));
        mainContainer.addView(sbPopup);

        // Date pill
        LinearLayout datePill = new LinearLayout(this);
        datePill.setOrientation(LinearLayout.HORIZONTAL);
        datePill.setBackground(pillStroke(CARD, EDGE));
        datePill.setPadding(dp(20), dp(10), dp(20), dp(10));
        datePill.setGravity(Gravity.CENTER);
        TextView dateT = tv(curDate(), 14, DARK, true);
        dateT.setOnClickListener(v -> openSystemCalendar());
        datePill.addView(dateT);
        mainContainer.addView(datePill, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Large time display
        LinearLayout timeCard = new LinearLayout(this);
        timeCard.setOrientation(LinearLayout.VERTICAL);
        timeCard.setBackground(gdRetro(CARD, EDGE));
        timeCard.setPadding(dp(24), dp(20), dp(24), dp(20));
        timeCard.setGravity(Gravity.CENTER);
        timeView = tv(curTime(), 56, DARK, true);
        timeView.setOnClickListener(v -> openSystemCalendar());
        timeCard.addView(timeView);
        mainContainer.addView(timeCard, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(110)));

        if (clockRun == null) clockRun = new Runnable() { public void run() { if (timeView != null) timeView.setText(curTime()); H.postDelayed(this, 20000); } };
        H.postDelayed(clockRun, 20000);

        // Weather widget
        LinearLayout weatherCard = new LinearLayout(this);
        weatherCard.setOrientation(LinearLayout.HORIZONTAL);
        weatherCard.setBackground(gdRetro(CREAM, EDGE));
        weatherCard.setPadding(dp(20), dp(14), dp(20), dp(14));
        weatherCard.setGravity(Gravity.CENTER_VERTICAL);
        Icon sunIcon = new Icon("weather", Color.parseColor(MUSTARD));
        sunIcon.setBounds(0, 0, dp(48), dp(48));
        TextView weatherIcon = new TextView(this);
        weatherIcon.setCompoundDrawables(sunIcon, null, null, null);
        tempView = tv("+18°", 28, DARK, true);
        weatherView = tv("Ясно", 18, MUT, true);
        weatherCard.addView(weatherIcon);
        weatherCard.addView(tempView);
        weatherCard.addView(tv("  ", 18, DARK, false));
        weatherCard.addView(weatherView);
        mainContainer.addView(weatherCard, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Voice assistant button
        micArea = new LinearLayout(this);
        micArea.setOrientation(LinearLayout.VERTICAL);
        micArea.setGravity(Gravity.CENTER_HORIZONTAL);
        micArea.setClipChildren(false);
        micArea.setPadding(0, dp(12), 0, dp(12));

        LinearLayout voiceCard = new LinearLayout(this);
        voiceCard.setOrientation(LinearLayout.HORIZONTAL);
        voiceCard.setBackground(gdRetro(MUSTARD, RUST));
        voiceCard.setPadding(dp(20), dp(18), dp(20), dp(18));
        voiceCard.setGravity(Gravity.CENTER_VERTICAL);

        micBtn = new Button(this);
        micBtn.setBackground(pill(ACC));
        micBtn.setTag(null);
        micBtn.setCompoundDrawables(ic("mic", "#FFFFFF", dp(56)), null, null, null);
        micBtn.setLayoutParams(new LinearLayout.LayoutParams(dp(72), dp(72)));
        micBtn.setOnClickListener(v -> { pressFx(v); openDialogAndListen(); });

        LinearLayout voiceText = new LinearLayout(this);
        voiceText.setOrientation(LinearLayout.VERTICAL);
        voiceText.setPadding(dp(16), 0, 0, 0);
        TextView voiceTitle = tv("Говорите со мной", 20, DARK, true);
        TextView voiceSub = tv("Нажмите и говорите, я помогу", 14, "#5D4037", false);
        voiceText.addView(voiceTitle);
        voiceText.addView(voiceSub);

        voiceCard.addView(micBtn);
        voiceCard.addView(voiceText);
        micArea.addView(voiceCard);

        // Dialog card (hidden by default)
        dlgCard = new LinearLayout(this);
        dlgCard.setOrientation(LinearLayout.VERTICAL);
        dlgCard.setBackground(gdRetro(MUSTARD, RUST));
        dlgCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        dlgCard.setVisibility(View.GONE);
        LinearLayout drow = row();
        drow.setGravity(Gravity.CENTER_VERTICAL);
        dlgMic = new Button(this);
        dlgMic.setBackground(pill(ACC));
        dlgMic.setTag(null);
        dlgMic.setCompoundDrawables(ic("mic", "#FFFFFF", dp(34)), null, null, null);
        dlgMic.setLayoutParams(new LinearLayout.LayoutParams(dp(56), dp(56)));
        dlgMic.setOnClickListener(v -> { pressFx(v); startListenOnMain(); });
        TextView dtitle = tv(" Говорите со мной", 17, DARK, true);
        dtitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        dlgClose = new Button(this);
        dlgClose.setBackground(pill(CARD));
        dlgClose.setTag(null);
        dlgClose.setCompoundDrawables(ic("x", DARK, dp(26)), null, null, null);
        dlgClose.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
        dlgClose.setOnClickListener(v -> collapseDialog());
        drow.addView(dlgMic); drow.addView(dtitle); drow.addView(dlgClose);
        dlgCard.addView(drow);
        userSay = tv("ВЫ: …", 14, "#5D4037", true);
        dlgCard.addView(userSay);
        caption = tv("ВНУЧОК: «Здравствуйте! Нажмите кнопку и говорите.»", 16, DARK, true);
        dlgCard.addView(caption);
        micArea.addView(dlgCard, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mainContainer.addView(micArea);

        // App tiles grid
        LinearLayout tilesContainer = new LinearLayout(this);
        tilesContainer.setOrientation(LinearLayout.VERTICAL);
        tilesContainer.setPadding(0, dp(8), 0, dp(8));

        float tScale = P.getFloat("tileScale", 1f);
        int tileH = Math.round(dp(120) * FS * tScale);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        hp.setMargins(dp(5), dp(5), dp(5), dp(5));
        
        List<String> order = tilesOrder();
        List<String> visible = new ArrayList<>();
        for (String id : order) if (!tileOff(id)) visible.add(id);
        
        LinearLayout curRow = null;
        int inRow = 0;
        for (int i = 0; i < visible.size(); i++) {
            if (inRow == 0) { 
                curRow = row(); 
                curRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, tileH)); 
                tilesContainer.addView(curRow); 
            }
            String id = visible.get(i);
            Button b = tileFor(id, tileColor(i));
            b.setLayoutParams(hp);
            curRow.addView(b);
            inRow++;
            if (inRow == 3) inRow = 0;
        }
        
        mainContainer.addView(tilesContainer);

        // Main menu button
        LinearLayout menuBar = new LinearLayout(this);
        menuBar.setOrientation(LinearLayout.HORIZONTAL);
        menuBar.setGravity(Gravity.CENTER);
        menuBar.setPadding(0, dp(12), 0, dp(8));
        
        Button menuBtn = bigI("home", "Главное меню", SBBG, SBFG, v -> {});
        menuBtn.setTextSize(16 * FS * SC);
        menuBtn.setPadding(dp(24), dp(14), dp(24), dp(14));
        menuBar.addView(menuBtn);
        
        mainContainer.addView(menuBar);

        setScreenHeader(mainContainer, true, null);
        pollSignal();
    }

    Button tileFor(String id, String color) {
        switch (id) {
            case "call": callTile = tileBtn("phone", "Звонить", color, TFG, v -> showContacts(true)); return callTile;
            case "sms": msgTile = tileBtn("mail", "Сообщения", color, TFG, v -> { blink(msgTile, false); showSmsList(); }); return msgTile;
            case "apps": appsTile = tileBtn("dial", "Приложения", color, TFG, v -> showAppsScreen()); return appsTile;
            case "rem": return tileBtn("memo", "Напоминания", color, TFG, v -> showReminders());
            case "alarm": return tileBtn("alarm", "Будильник", color, TFG, v -> showAlarms());
            case "torch": return tileBtn("torch", "Фонарик", color, TFG, v -> toggleTorch());
            case "cam": return tileBtn("cam", "Фото", color, TFG, v -> openCameraApp());
            case "album": return tileBtn("gallery", "Галерея", color, TFG, v -> openPhotoalbum());
            case "music": return tileBtn("music", "Музыка", color, TFG, v -> appFunc("music"));
            default: return tileBtn("gear", "Настройки", color, TFG, v -> showSettings());
        }
    }

    void openDialogAndListen() {
        if (dlgOpen) { startListenOnMain(); return; }
        dlgOpen = true;
        micBtn.animate().scaleX(0.5f).scaleY(0.5f).alpha(0f).setDuration(200).withEndAction(() -> {
            micBtn.setVisibility(View.GONE);
            dlgCard.setVisibility(View.VISIBLE);
            dlgCard.setPivotX(dlgCard.getWidth() / 2f);
            dlgCard.setPivotY(0f);
            dlgCard.setScaleX(0.5f); dlgCard.setScaleY(0.5f); dlgCard.setAlpha(0f);
            dlgCard.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(350);
            startListenOnMain();
        });
    }

    void collapseDialog() {
        if (!dlgOpen) return;
        dlgOpen = false;
        stopListening();
        dlgCard.animate().scaleX(0.5f).scaleY(0.5f).alpha(0f).setDuration(200).withEndAction(() -> {
            dlgCard.setVisibility(View.GONE);
            micBtn.setVisibility(View.VISIBLE);
            micBtn.setScaleX(0.5f); micBtn.setScaleY(0.5f); micBtn.setAlpha(0f);
            micBtn.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(300);
        });
    }

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
        int rowH = Math.round(dp(120) * FS);
        LinearLayout.LayoutParams hp2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        hp2.setMargins(dp(4), dp(4), dp(4), dp(4));
        String[][] funcs = {
                {"radio", "Радио"}, {"map", "Карта"}, {"music", "Музыка"}, {"weather", "Погода"},
                {"web", "Интернет"}, {"cam", "Фото"}, {"album", "Фотоальбом"}, {"book", "Чтение"},
                {"gear", "Настройки"}, {"memo", "Заметки голосом"}};
        LinearLayout fr = null;
        for (int i = 0; i < funcs.length; i++) {
            if (i % 2 == 0) { fr = row(); fr.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowH)); c.addView(fr); }
            final String id = funcs[i][0];
            Button b = tileBtn(id, funcs[i][1], tileColor(i), TFG, v -> appFunc(id));
            b.setLayoutParams(hp2);
            fr.addView(b);
        }
        c.addView(tv("УСТАНОВЛЕННЫЕ ПРОГРАММЫ", 18, DARK, true));
        int rowH3 = Math.round(dp(100) * FS);
        LinearLayout.LayoutParams hp3 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        hp3.setMargins(dp(3), dp(3), dp(3), dp(3));
        List<Object[]> apps = installedApps();
        LinearLayout ar = null;
        int n = 0;
        for (final Object[] a : apps) {
            if (n % 3 == 0) { ar = row(); ar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowH3)); c.addView(ar); }
            Button ab = new Button(this);
            ab.setText((String) a[0]);
            ab.setTextSize(11 * FS * SC); ab.setTextColor(Color.parseColor(TFG));
            ab.setBackground(gd(TILE));
            THolder th = new THolder(); th.bg = TILE; ab.setTag(th);
            ab.setElevation(dp(4));
            Drawable icn = (Drawable) a[1];
            if (icn != null) { icn.setBounds(0, 0, dp(38), dp(38)); ab.setCompoundDrawables(null, icn, null, null); }
            ab.setOnClickListener(v -> { pressFx(v); openPackage((String) a[2]); });
            ab.setLayoutParams(hp3);
            ar.addView(ab);
            n++;
        }
        if (n == 0) c.addView(tv("(нет других приложений)", 15, MUT, true));
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
        c.addView(tv("Говорите текст заметки, я запишу и озвучу", 16, DARK, true));
        final EditText et = new EditText(this);
        et.setTextSize(20 * FS * SC); et.setMinLines(4);
        c.addView(et);
        c.addView(bigI("sound", "ПРОЧИТАТЬ ВСЛУХ", TILE, TFG, v -> speak(et.getText().toString())));
        c.addView(bigI("memo", "СОХРАНИТЬ В ЗАМЕТКИ", "#3FAE4C", "#FFFFFF", v -> {
            String old = P.getString("notes", "");
            P.edit().putString("notes", old + (old.isEmpty() ? "" : "\n---\n") + et.getText().toString()).apply();
            say("Заметка сохранена.");
            showAppsScreen();
        }));
        setScreen(c, false);
    }

    void showReading() {
        setTitle("Чтение вслух");
        LinearLayout c = col();
        c.addView(tv("КНИГИ ДЛЯ СЛАБОВИДЯЩИХ", 22, DARK, true));
        c.addView(tv("Внучок прочитает книгу голосом", 15, MUT, true));
        c.addView(bigI("book", "ОТКРЫТЬ ФАЙЛ КНИГИ", "#3FAE4C", "#FFFFFF", v -> {
            try {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.setType("text/*");
                i.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(i, 42);
            } catch (Exception e) { say("Не удалось открыть выбор файла."); }
        }));
        c.addView(bigI("memo", "ВСТАВИТЬ ТЕКСТ", TILE, TFG, v -> showPasteText()));
        if (bookText != null) {
            String preview = bookText.length() > 160 ? bookText.substring(0, 160) + "…" : bookText;
            c.addView(tv("Загружено: " + preview, 14, DARK, false));
            c.addView(bigI("sound", bookPlaying ? "ПАУЗА" : "ЧИТАТЬ ВСЛУХ", MUSTARD, DARK, v -> {
                if (bookPlaying) { bookPlaying = false; if (tts != null) tts.stop(); say("Пауза."); }
                else startBook();
            }));
            c.addView(bigI("x", "СТОП И ЗАБЫТЬ", RUST, "#FFFFFF", v -> { bookPlaying = false; bookText = null; bookSents = null; if (tts != null) tts.stop(); say("Остановил чтение."); showReading(); }));
        }
        setScreen(c, false);
    }

    void showPasteText() {
        setTitle("Вставить текст");
        LinearLayout c = col();
        final EditText et = new EditText(this);
        et.setTextSize(18 * FS * SC); et.setMinLines(6);
        et.setHint("Вставьте или напишите текст");
        c.addView(et);
        c.addView(bigI("sound", "НАЧАТЬ ЧТЕНИЕ", "#3FAE4C", "#FFFFFF", v -> {
            bookText = et.getText().toString();
            prepareBook();
            startBook();
        }));
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

    void showSettings() {
        setTitle("Настройки");
        LinearLayout c = col();
        c.addView(tv("ТЕМА:", 16, DARK, true));
        LinearLayout rt = row();
        for (int i = 0; i < 4; i++) {
            final int ti = i;
            Button b = big(THN[i], P.getInt("theme", 0) == i ? ACC : TILE, P.getInt("theme", 0) == i ? "#FFFFFF" : TFG, x -> { P.edit().putInt("theme", ti).apply(); showMain(); say("Тема: " + THN[ti].toLowerCase() + "."); });
            b.setTextSize(12 * FS * SC);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            b.setLayoutParams(lp);
            rt.addView(b);
        }
        c.addView(rt);
        c.addView(bigI("wrench", "РЕДАКТОР ГЛАВНОГО ЭКРАНА", MUSTARD, DARK, v -> showEditor()));
        c.addView(toggleRow("Ответы вслух", "voice", true));
        c.addView(toggleRow("Щелчки и подсветка кнопок", "sound", true));
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
        c.addView(tv("Тон голоса:", 16, DARK, true));
        LinearLayout r3 = row();
        String[] pn = {"НИЗКИЙ", "СРЕДНИЙ", "ВЫСОКИЙ"};
        float[] pv = {0.8f, 1f, 1.2f};
        for (int i = 0; i < 3; i++) {
            final float v = pv[i];
            Button b = big(pn[i], Math.abs(P.getFloat("pitch", 1f) - v) < 0.05 ? ACC : TILE, Math.abs(P.getFloat("pitch", 1f) - v) < 0.05 ? "#FFFFFF" : TFG, x -> { P.edit().putFloat("pitch", v).apply(); if (ttsReady) tts.setPitch(v); showSettings(); say("Тон голоса изменён."); });
            b.setTextSize(13 * FS * SC);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            b.setLayoutParams(lp);
            r3.addView(b);
        }
        c.addView(r3);
        c.addView(bigI("mic", "ГОЛОСОВОЕ УПРАВЛЕНИЕ", TILE, TFG, v -> { try { startActivity(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)); } catch (Exception e) { say("Не удалось открыть."); } }));
        c.addView(bigI("sound", "СИНТЕЗ РЕЧИ (ДРУГИЕ ГОЛОСА)", TILE, TFG, v -> { try { startActivity(new Intent("com.android.settings.TTS_SETTINGS")); } catch (Exception e) { say("Не удалось открыть."); } }));
        c.addView(bigI("people", "СПЕЦИАЛЬНЫЕ ВОЗМОЖНОСТИ", TILE, TFG, v -> { try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); } catch (Exception e) { say("Не удалось открыть."); } }));
        c.addView(bigI("gear", "РАЗРЕШЕНИЯ ПРИЛОЖЕНИЯ", TILE, TFG, v -> { try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))); } catch (Exception e) { say("Не удалось открыть."); } }));
        c.addView(bigI("gear", "СИСТЕМНЫЕ НАСТРОЙКИ ANDROID", TILE, TFG, v -> { try { startActivity(new Intent(Settings.ACTION_SETTINGS)); } catch (Exception e) { say("Не удалось открыть."); } }));
        c.addView(bigI("clock", "О ПРИЛОЖЕНИИ", TILE, TFG, v -> showAbout()));
        setScreen(c, false);
    }

    void showEditor() {
        setTitle("Редактор экрана");
        LinearLayout c = col();
        c.addView(tv("МАСШТАБ ПЛИТОК:", 16, DARK, true));
        LinearLayout rs = row();
        String[] sn = {"МЕЛКИЕ", "СРЕДНИЕ", "КРУПНЫЕ"};
        float[] sv = {0.85f, 1f, 1.2f};
        for (int i = 0; i < 3; i++) {
            final float v = sv[i];
            Button b = big(sn[i], Math.abs(P.getFloat("tileScale", 1f) - v) < 0.01 ? ACC : TILE, Math.abs(P.getFloat("tileScale", 1f) - v) < 0.01 ? "#FFFFFF" : TFG, x -> { P.edit().putFloat("tileScale", v).apply(); showEditor(); say("Масштаб изменён."); });
            b.setTextSize(12 * FS * SC);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            b.setLayoutParams(lp);
            rs.addView(b);
        }
        c.addView(rs);
        c.addView(tv("КНОПКИ ГЛАВНОГО ЭКРАНА:", 16, DARK, true));
        List<String> order = tilesOrder();
        String[][] meta = {
                {"call", "Звонить"}, {"sms", "Сообщения"}, {"apps", "Приложения"},
                {"rem", "Напоминания"}, {"alarm", "Будильник"}, {"torch", "Фонарик"}};
        for (int i = 0; i < order.size(); i++) {
            final int pos = i;
            final String id = order.get(i);
            String name = id;
            for (String[] m : meta) if (m[0].equals(id)) name = m[1];
            LinearLayout rw = row();
            rw.setGravity(Gravity.CENTER_VERTICAL);
            Button nb = big(name, tileOff(id) ? "#8A8A8A" : TILE, TFG, v -> {
                List<String> off = new ArrayList<>();
                for (String s : P.getString("tilesOff", "").split(",")) if (!s.isEmpty()) off.add(s);
                if (off.remove(id)) say(name + ": скрыта.");
                else { off.add(id); say(name + ": на экране."); }
                StringBuilder sb = new StringBuilder();
                for (int k = 0; k < off.size(); k++) { if (k > 0) sb.append(","); sb.append(off.get(k)); }
                P.edit().putString("tilesOff", sb.toString()).apply();
                showEditor();
            });
            nb.setTextSize(14 * FS * SC);
            nb.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            rw.addView(nb);
            Button up = big("↑", TILE, TFG, v -> { if (pos > 0) { List<String> o = tilesOrder(); String t = o.remove(pos); o.add(pos - 1, t); saveTiles(o); showEditor(); } });
            up.setLayoutParams(new LinearLayout.LayoutParams(dp(64), ViewGroup.LayoutParams.WRAP_CONTENT));
            rw.addView(up);
            Button dn = big("↓", TILE, TFG, v -> { if (pos < order.size() - 1) { List<String> o = tilesOrder(); String t = o.remove(pos); o.add(pos + 1, t); saveTiles(o); showEditor(); } });
            dn.setLayoutParams(new LinearLayout.LayoutParams(dp(64), ViewGroup.LayoutParams.WRAP_CONTENT));
            rw.addView(dn);
            c.addView(rw);
        }
        c.addView(tv("Серым помечены скрытые кнопки", 13, MUT, false));
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
        c.addView(tv("ВНУЧОК", 34, ACC, true));
        c.addView(tv("Версия: 2.0", 20, DARK, true));
        c.addView(tv("Оболочка Android для пенсионеров", 16, MUT, true));
        c.addView(tv("Все данные хранятся только на телефоне", 14, MUT, true));
        setScreen(c, false);
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
            Button nb = bigI("people", nm, TILE, TFG, v -> { if (callMode) confirmCall(nm, num); else showCompose(nm, num); });
            nb.setTextSize(15 * FS * SC);
            nb.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            rw.addView(nb);
            Button eb = big("✏", MUSTARD, DARK, v -> editContact(isCustom ? idx : -1, nm, num));
            eb.setLayoutParams(new LinearLayout.LayoutParams(dp(70), ViewGroup.LayoutParams.WRAP_CONTENT));
            rw.addView(eb);
            Button db = big("✕", RUST, "#FFFFFF", v -> confirmDialog("Удалить " + nm + " из списка?", () -> {
                if (isCustom) { List<String[]> x = contacts(); x.remove(idx); saveContacts(x); }
                else { P.edit().putString("hidden", P.getString("hidden", "") + (P.getString("hidden", "").isEmpty() ? "" : "\n") + nm + "|" + num).apply(); }
                say("Удалил: " + nm);
                renderContacts(callMode);
            }));
            db.setLayoutParams(new LinearLayout.LayoutParams(dp(70), ViewGroup.LayoutParams.WRAP_CONTENT));
            rw.addView(db);
            c.addView(rw);
        }
        if (upTo < cs.size()) {
            c.addView(bigI("dial", "ПОКАЗАТЬ ЕЩЁ (" + (cs.size() - upTo) + ")", TILE, TFG, v -> { contactShown += 30; renderContacts(callMode); }));
        }
        Button header = bigI("people", "НОВЫЙ НОМЕР", "#3FAE4C", "#FFFFFF", v -> editContact(-1, "", ""));
        header.setTextSize(18 * FS * SC);
        setScreenHeader(c, false, header);
    }

    void editContact(int idx, String preN, String preP) {
        setTitle(idx < 0 ? "Новый контакт" : "Изменить");
        List<String[]> cs = contacts();
        LinearLayout c = col();
        EditText en = new EditText(this); en.setTextSize(20 * FS * SC); en.setHint("Имя (например: Дочь Маша)");
        en.setText(preN);
        c.addView(en);
        EditText ep = new EditText(this); ep.setTextSize(20 * FS * SC); ep.setHint("Номер (например: +79121234567)");
        ep.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        ep.setText(preP);
        c.addView(ep);
        c.addView(big("💾 СОХРАНИТЬ", "#3FAE4C", "#FFFFFF", v -> {
            String n = en.getText().toString().trim(), p = ep.getText().toString().trim();
            if (n.isEmpty() || p.isEmpty()) { say("Заполните имя и номер."); return; }
            List<String[]> x = contacts();
            if (idx >= 0 && idx < x.size()) x.set(idx, new String[]{n, p});
            else x.add(new String[]{n, p});
            saveContacts(x);
            say("Сохранил: " + n);
            showContacts(true);
        }));
        c.addView(big("ОТМЕНА", TILE, TFG, v -> showContacts(true)));
        setScreen(c, false);
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

    void showDial() {
        setTitle("Телефон");
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
        setScreen(c, false);
    }

    void renderDialBody(LinearLayout c, boolean[] hist) {
        for (int i = c.getChildCount() - 1; i >= 1; i--) c.removeViewAt(i);
        if (hist[0]) {
            c.addView(tv("ИСТОРИЯ ЗВОНКОВ", 24, DARK, true));
            boolean any = false;
            try {
                Cursor cur = getContentResolver().query(CallLog.Calls.CONTENT_URI,
                        new String[]{CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE},
                        null, null, CallLog.Calls.DATE + " DESC LIMIT 15");
                if (cur != null) {
                    while (cur.moveToNext()) {
                        any = true;
                        String num = cur.getString(0); long date = cur.getLong(1); int type = cur.getInt(2);
                        String arrow = type == CallLog.Calls.OUTGOING_TYPE ? "→" : type == CallLog.Calls.MISSED_TYPE ? "" : "←";
                        String color = type == CallLog.Calls.MISSED_TYPE ? RUST : TFG;
                        String name = nameForNumber(num);
                        Calendar cd = Calendar.getInstance(); cd.setTimeInMillis(date);
                        String when = String.format(Locale.getDefault(), "%02d:%02d %02d.%02d", cd.get(Calendar.HOUR_OF_DAY), cd.get(Calendar.MINUTE), cd.get(Calendar.DAY_OF_MONTH), cd.get(Calendar.MONTH) + 1);
                        final String fnum = num;
                        Button b = big(arrow + " " + name + "\n" + when, TILE, color, v -> { blink(callTile, false); P.edit().putLong("lastMissed", System.currentTimeMillis()).apply(); confirmCall(name, fnum); });
                        b.setTextSize(16 * FS * SC);
                        c.addView(b);
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
                    c.addView(big(arrow + " " + e[3] + "\n" + when, TILE, TFG, v -> confirmCall(e[3], e[2])));
                    any = true;
                }
            }
            if (!any) c.addView(tv("Звонков пока нет.", 16, MUT, true));
            c.addView(bigI("clock", "СИСТЕМНАЯ ИСТОРИЯ ЗВОНКОВ", TILE, TFG, v -> {
                try { Intent i = new Intent(Intent.ACTION_VIEW); i.setType(CallLog.Calls.CONTENT_TYPE); startActivity(i); }
                catch (Exception e) { say("Не удалось открыть системную историю."); }
            }));
        } else {
            TextView disp = tv("", 34, DARK, true);
            disp.setGravity(Gravity.CENTER);
            disp.setMinHeight(dp(70));
            c.addView(disp);
            View spacer = new View(this);
            spacer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(60)));
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
            c.addView(bigI("sos", "112 ЭКСТРЕННО", RUST, "#FFFFFF", v -> startSos()));
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
        c.addView(bigI("mail", "НАПИСАТЬ НОВОЕ", "#3FAE4C", "#FFFFFF", v -> showContacts(false)));
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
                    Button b = bigI("mail", (read ? "" : "● ") + "От: " + nameForNumber(addr) + "\n" + shortB + (read ? "\n(прочитано)" : ""), TILE, TFG, v -> showSmsDetail(fid, faddr, fbody));
                    b.setTextSize(15 * FS * SC);
                    if (read) b.setAlpha(0.75f);
                    c.addView(b);
                }
                cur.close();
            }
        } catch (Exception e) {}
        if (!any) c.addView(tv("Сообщений пока нет или нет доступа к СМС.", 16, MUT, true));
        setScreen(c, false);
    }

    void showSmsDetail(String id, String addr, String body) {
        setTitle("Сообщение");
        LinearLayout c = col();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable cg = new GradientDrawable();
        cg.setColor(Color.parseColor(CARD)); cg.setCornerRadius(dp(18));
        cg.setStroke(dp(3), Color.parseColor(EDGE));
        box.setBackground(cg); box.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.setGravity(Gravity.START);
        box.addView(tv("От: " + nameForNumber(addr), 16, MUT, true));
        box.addView(tv(body, 20, DARK, true));
        c.addView(box);
        c.addView(bigI("sound", "ПРОЧИТАТЬ ВСЛУХ", TILE, TFG, v -> speak(body)));
        c.addView(bigI("mail", "ОТВЕТИТЬ", "#3FAE4C", "#FFFFFF", v -> showCompose(nameForNumber(addr), addr)));
        c.addView(bigI("clock", "ПОМЕТИТЬ ПРОЧИТАННЫМ", TILE, TFG, v -> { smsAdd("smsRead", id); say("Пометил как прочитанное."); showSmsList(); }));
        c.addView(bigI("x", "УДАЛИТЬ", RUST, "#FFFFFF", v -> confirmDialog("Удалить сообщение?", () -> { smsAdd("smsHidden", id); say("Сообщение убрано из списка."); showSmsList(); })));
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
        cg.setColor(Color.parseColor(CARD)); cg.setCornerRadius(dp(18));
        cg.setStroke(dp(4), Color.parseColor(EDGE));
        box.setBackground(cg); box.setPadding(dp(14), dp(12), dp(14), dp(12));
        composeEt = new EditText(this);
        composeEt.setTextSize(22 * FS * SC); composeEt.setMinLines(4);
        composeEt.setBackgroundColor(Color.TRANSPARENT);
        composeEt.setTextColor(Color.parseColor(DARK));
        composeEt.setHint("Говорите текст — я запишу сам");
        composeEt.setHintTextColor(Color.parseColor(MUT));
        box.addView(composeEt);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.topMargin = dp(6); bp.bottomMargin = dp(6);
        box.setLayoutParams(bp);
        c.addView(box);
        smsStatusView = tv("Статус: —", 15, MUT, true);
        c.addView(smsStatusView);
        H.post(new Runnable() { public void run() { if (composeActive && smsStatusView != null) { smsStatusView.setText("Статус: " + lastSmsStatus); H.postDelayed(this, 1500); } } });
        c.addView(bigI("sound", "ПРОЧИТАТЬ ВСЛУХ", TILE, TFG, v -> speak(composeEt.getText().toString())));
        composeSend = () -> {
            composeActive = false;
            stopCompListen();
            sendSms(composeNum, composeEt.getText().toString());
            say("Отправлено: " + composeName);
            showMain();
        };
        composeCancel = () -> { composeActive = false; stopCompListen(); showMain(); };
        c.addView(bigI("mail", "ОТПРАВИТЬ", "#3FAE4C", "#FFFFFF", v -> composeSend.run()));
        c.addView(big("ОТМЕНА", TILE, TFG, v -> composeCancel.run()));
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
            b.setTextSize(16 * FS * SC);
            b.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            rw.addView(b);
            Button db = big("✕", RUST, "#FFFFFF", v -> {
                AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                am.cancel(PendingIntent.getBroadcast(this, code, new Intent(this, ReminderReceiver.class), PendingIntent.FLAG_IMMUTABLE));
                List<String[]> x = rems(); x.remove(idx); saveRems(x); say("Убрал."); showReminders();
            });
            db.setLayoutParams(new LinearLayout.LayoutParams(dp(80), ViewGroup.LayoutParams.WRAP_CONTENT));
            rw.addView(db);
            c.addView(rw);
        }
        if (!any) c.addView(tv("(пока ничего нет)", 16, MUT, false));
    }

    void showReminders() {
        setTitle("Напоминания");
        LinearLayout c = col();
        c.addView(bigI("memo", "СОЗДАТЬ НАПОМИНАНИЕ", "#3FAE4C", "#FFFFFF", v -> { selDate = Calendar.getInstance(); showRemEditor(); }));
        c.addView(bigI("memo", "Через час: таблетки", TILE, TFG, v -> { addReminder(System.currentTimeMillis() + 3600000, "Пора принять таблетки!"); say("Напомню через час."); showReminders(); }));
        c.addView(bigI("memo", "Утром в 9:00: таблетки", TILE, TFG, v -> { addReminder(atTime(9, 0), "Пора принять таблетки!"); say("Напомню утром в девять."); showReminders(); }));
        c.addView(tv("Уже стоит:", 18, MUT, true));
        showRemList(c, "rem");
        setScreen(c, false);
    }

    void addTimeRow(LinearLayout c, String label, final int[] val, int mod, int step) {
        LinearLayout r = row();
        r.setGravity(Gravity.CENTER);
        TextView lt = tv(label, 18, DARK, true);
        lt.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(lt);
        Button minus = big("−", TILE, TFG, v -> { val[0] = (val[0] - step + mod) % mod; ((TextView) r.getChildAt(2)).setText(String.format(Locale.getDefault(), "%02d", val[0])); });
        minus.setTextSize(32 * FS * SC);
        minus.setLayoutParams(new LinearLayout.LayoutParams(dp(90), ViewGroup.LayoutParams.WRAP_CONTENT));
        final TextView disp = tv(String.format(Locale.getDefault(), "%02d", val[0]), 40, DARK, true);
        disp.setGravity(Gravity.CENTER);
        disp.setMinWidth(dp(120));
        Button plus = big("+", TILE, TFG, v -> { val[0] = (val[0] + step) % mod; ((TextView) r.getChildAt(2)).setText(String.format(Locale.getDefault(), "%02d", val[0])); });
        plus.setTextSize(32 * FS * SC);
        plus.setLayoutParams(new LinearLayout.LayoutParams(dp(90), ViewGroup.LayoutParams.WRAP_CONTENT));
        r.addView(minus); r.addView(disp); r.addView(plus);
        c.addView(r);
    }

    void showRemEditor() {
        setTitle("Новое напоминание");
        LinearLayout c = col();
        final EditText en = new EditText(this); en.setTextSize(22 * FS * SC); en.setHint("Название (например: таблетки)");
        c.addView(en);
        final int[] hv = {9};
        final int[] mv = {0};
        addTimeRow(c, "ЧАСЫ", hv, 24, 1);
        addTimeRow(c, "МИНУТЫ", mv, 60, 5);
        c.addView(bigI("clock", "Дата: " + selDate.get(Calendar.DAY_OF_MONTH) + "." + (selDate.get(Calendar.MONTH) + 1) + "." + selDate.get(Calendar.YEAR), TILE, TFG, v -> showCalendarPick()));
        c.addView(big("💾 СОХРАНИТЬ", "#3FAE4C", "#FFFFFF", v -> {
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
        }));
        c.addView(big("ОТМЕНА", TILE, TFG, v -> showReminders()));
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
        c.addView(tv("Дни недели:", 18, DARK, true));
        final LinearLayout days = row();
        for (int i = 0; i < 7; i++) {
            final int bit = i;
            boolean on = (alarmMask & (1 << bit)) != 0;
            Button db = big(DN[i], on ? "#3FAE4C" : TILE, on ? "#FFFFFF" : TFG, v -> {
                alarmMask ^= (1 << bit);
                boolean nowOn = (alarmMask & (1 << bit)) != 0;
                String nb = nowOn ? "#3FAE4C" : TILE;
                v.setBackground(gd(nb));
                holder(v).bg = nb;
                ((Button) v).setTextColor(Color.parseColor(nowOn ? "#FFFFFF" : TFG));
            });
            db.setTextSize(16 * FS * SC);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(64), 1f);
            lp.setMargins(dp(2), 0, dp(2), 0);
            db.setLayoutParams(lp);
            days.addView(db);
        }
        c.addView(days);
        c.addView(tv("(ничего не нажато = каждый день)", 14, MUT, false));
        c.addView(bigI("alarm", "ПОСТАВИТЬ БУДИЛЬНИК", "#3FAE4C", "#FFFFFF", v -> {
            long ms = nextAlarm(hv[0], mv[0], alarmMask);
            addAlarm(hv[0], mv[0], alarmMask, "Будильник! Пора вставать!");
            say("Будильник поставлен. " + until(ms));
            showAlarms();
        }));
        c.addView(tv("Уже стоит:", 18, MUT, true));
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
        remOverlay.setBackgroundColor(Color.parseColor(BG));
        remOverlay.setPadding(dp(30), dp(30), dp(30), dp(30));
        remOverlay.addView(tv("💊", 70, RUST, false));
        TextView bt = tv(text.toUpperCase(), 36, RUST, true);
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

    void startSos() {
        if (checkSelfPermission(android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CALL_PHONE}, 77);
        }
        setTitle("Экстренный вызов");
        LinearLayout c = col();
        c.addView(tv("ВЫЗЫВАЕМ ПОМОЩЬ!", 28, RUST, true));
        TextView num = tv("5", 90, RUST, true);
        c.addView(num);
        c.addView(tv("Если случайно — жмите ОТМЕНА", 18, DARK, true));
        c.addView(big("✋ ОТМЕНА", "#8A8A8A", "#FFFFFF", v -> cancelSos()));
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
        c.addView(tv("☎ ИДЁТ ЗВОНОК В 112…", 26, RUST, true));
        c.addView(tv("СМС отправлено родным:", 18, DARK, true));
        c.addView(tv(sms, 16, DARK, true));
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
        battOverlay.setBackgroundColor(Color.parseColor(RUST));
        battOverlay.setPadding(dp(30), dp(30), dp(30), dp(30));
        battOverlay.addView(tv("🪫", 70, "#FFFFFF", false));
        TextView t = tv("ТЕЛЕФОН СЕЙЧАС ВЫКЛЮЧИТСЯ!\nПОСТАВЬТЕ НА ЗАРЯДКУ!", 32, "#FFFFFF", true);
        t.setGravity(Gravity.CENTER);
        battOverlay.addView(t);
        battOverlay.addView(tv("Это окно исчезнет само, когда начнётся зарядка", 16, "#FFE0D8", true));
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
