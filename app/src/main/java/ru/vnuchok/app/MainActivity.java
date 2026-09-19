package ru.vnuchok.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* =====================================================================
   MAIN — тонкая сборка: жизненный цикл, маршрутизация экранов, статус-бар,
   батарея, занавес при старте, окно напоминания. Вся логика — в модулях Vn*.
   ===================================================================== */
public class MainActivity extends Activity {
    FrameLayout frame, midFrame, mainSlot;
    ScrollView scroll;
    LinearLayout rootLin, headerSlot, bottomSlot;
    LinearLayout overlayBox;
    VnHome home;
    BatteryReceiver battRec;
    Runnable sigRun;
    Handler H = new Handler(Looper.getMainLooper());
    String lastSay = "";
    SharedPreferences P;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        P = getSharedPreferences("vnuchok", MODE_PRIVATE);
        VnTheme.apply(P.getInt("theme", 0));
        VnUi.init(this);
        VnUi.FS = P.getFloat("fs", 1f);
        float wdp = getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density;
        VnUi.SC = Math.max(0.85f, Math.min(1.6f, wdp / 360f));
        VnVoice.init(this);
        VnVoice.voiceOn = P.getBoolean("voice", true);

        // --- каркас: шапка / середина (скролл+главный) / низ
        frame = new FrameLayout(this);
        frame.setBackground(VnBg.paper());
        rootLin = new LinearLayout(this);
        rootLin.setOrientation(LinearLayout.VERTICAL);
        headerSlot = new LinearLayout(this);
        headerSlot.setOrientation(LinearLayout.VERTICAL);
        headerSlot.setVisibility(View.GONE);
        rootLin.addView(headerSlot, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        midFrame = new FrameLayout(this);
        scroll = new ScrollView(this);
        scroll.setVisibility(View.GONE);
        midFrame.addView(scroll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mainSlot = new FrameLayout(this);
        midFrame.addView(mainSlot, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rootLin.addView(midFrame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        bottomSlot = new LinearLayout(this);
        bottomSlot.setOrientation(LinearLayout.VERTICAL);
        bottomSlot.setPadding(VnUi.dp(10), VnUi.dp(4), VnUi.dp(10), VnUi.dp(10));
        rootLin.addView(bottomSlot, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        frame.addView(rootLin, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(frame);

        // --- маршрутизация экранов
        VnHome.router = new VnHome.Router() {
            public void show(View v, String title) {
                H.post(() -> {
                    headerSlot.removeAllViews();
                    LinearLayout pillRow = VnUi.row();
                    pillRow.setPadding(VnUi.dp(12), VnUi.dp(8), VnUi.dp(12), VnUi.dp(2));
                    LinearLayout hpill = new LinearLayout(MainActivity.this);
                    hpill.setOrientation(LinearLayout.HORIZONTAL);
                    hpill.setGravity(Gravity.CENTER_VERTICAL);
                    hpill.setBackground(VnUi.pill(VnTheme.GREEN));
                    hpill.setPadding(VnUi.dp(10), VnUi.dp(6), VnUi.dp(18), VnUi.dp(6));
                    Button backB = new Button(MainActivity.this);
                    backB.setBackground(VnUi.pill(VnTheme.CREAM));
                    backB.setTag(null);
                    android.graphics.drawable.Drawable bic = VnIcons.icon("back", VnTheme.GREEN);
                    bic.setBounds(0, 0, VnUi.dp(26), VnUi.dp(26));
                    backB.setCompoundDrawables(bic, null, null, null);
                    backB.setLayoutParams(new LinearLayout.LayoutParams(VnUi.dp(44), VnUi.dp(44)));
                    backB.setOnClickListener(x -> VnHome.router.home());
                    TextView tt = VnUi.tv("  " + title, 20, VnTheme.CREAM, true);
                    hpill.addView(backB); hpill.addView(tt);
                    pillRow.addView(hpill, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    headerSlot.addView(pillRow);
                    headerSlot.setVisibility(View.VISIBLE);
                    bottomSlot.removeAllViews();
                    Button bb = VnUi.bigI("home", "BACK", VnTheme.GREEN, VnTheme.CREAM, x -> VnHome.router.home());
                    bb.setTextSize(18 * VnUi.FS * VnUi.SC);
                    bb.setMinHeight(VnUi.dp(56));
                    bottomSlot.addView(bb);
                    mainSlot.setVisibility(View.GONE);
                    scroll.removeAllViews();
                    scroll.addView(v);
                    scroll.setVisibility(View.VISIBLE);
                    scroll.scrollTo(0, 0);
                    v.setAlpha(0f); v.setTranslationY(VnUi.dp(10));
                    v.animate().alpha(1f).translationY(0f).setDuration(400);
                });
            }
            public void home() { showHome(); }
            public void overlay(View v) {
                H.post(() -> {
                    closeOverlayNow();
                    overlayBox = new LinearLayout(MainActivity.this);
                    overlayBox.setOrientation(LinearLayout.VERTICAL);
                    overlayBox.setGravity(Gravity.CENTER);
                    overlayBox.setBackgroundColor(0xCC4A2C17);
                    overlayBox.setPadding(VnUi.dp(20), VnUi.dp(20), VnUi.dp(20), VnUi.dp(20));
                    overlayBox.addView(v);
                    frame.addView(overlayBox, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                });
            }
            public void closeOverlay() { H.post(() -> { closeOverlayNow(); VnVoice.stopListen(); }); }
        };

        showHome();
        VnLock.play(frame);

        // --- батарея
        battRec = new BatteryReceiver();
        registerReceiver(battRec, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        // --- сеть живо
        if (home != null) home.sb.watchNet(this);
        // --- соты по таймеру
        sigRun = new Runnable() { public void run() { pollSignal(); H.postDelayed(this, 15000); } };
        H.postDelayed(sigRun, 2000);

        if (getIntent() != null && getIntent().hasExtra("reminder")) {
            VnHome.router.show(VnLock.buildReminder(getIntent().getStringExtra("reminder")), "Reminder");
        }
    }

    void closeOverlayNow() {
        if (overlayBox != null) { frame.removeView(overlayBox); overlayBox = null; }
    }

    // --- главный экран
    void showHome() {
        H.post(() -> {
            if (home != null) home.sb.unwatch(this);
            home = new VnHome();
            LinearLayout c = home.build(new VnHome.Nav() {
                public void tile(String id) { }
                public void sos() { VnHome.router.show(VnSos.build(), "Emergency call"); }
                public void mic() { home.openDialog(); startListen(); }
                public void close() { home.closeDialog(); }
            });
            headerSlot.removeAllViews();
            headerSlot.setVisibility(View.GONE);
            bottomSlot.removeAllViews();
            Button sosB = VnUi.bigI("sos", "SOS — CALL FOR HELP (112)", VnTheme.RUST, VnTheme.CREAM, v -> VnHome.router.show(VnSos.build(), "Emergency call"));
            sosB.setTextSize(18 * VnUi.FS * VnUi.SC);
            sosB.setMinHeight(VnUi.dp(56));
            bottomSlot.addView(sosB);
            scroll.setVisibility(View.GONE);
            mainSlot.removeAllViews();
            mainSlot.addView(c, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mainSlot.setVisibility(View.VISIBLE);
            c.setAlpha(0f);
            c.animate().alpha(1f).setDuration(400);
            home.sb.watchNet(this);
            home.sb.updateOperator();
            pollSignal();
            home.cw.startClock();
            home.cw.fetchWeather();
        });
    }

    void startListen() {
        VnVoice.listen(
                t -> { if (home != null && home.userSay != null) home.userSay.setText("YOU: «" + t + "»…"); },
                t -> {
                    if (t == null || t.isEmpty()) { if (home != null && home.userSay != null) home.userSay.setText("YOU: (empty)"); return; }
                    if (home != null && home.userSay != null) home.userSay.setText("YOU: «" + t + "»");
                    handleCommand(t);
                },
                e -> { if (home != null && home.userSay != null) home.userSay.setText("YOU: (couldn't hear, repeat)"); say("Couldn't hear. Repeat please."); });
    }

    void say(String t) {
        lastSay = t;
        if (home != null && home.caption != null) home.caption.setText(t);
        VnVoice.speak(t);
    }

    // --- команды голоса: маршрутизация по модулям
    void handleCommand(String raw) {
        String t = raw.toLowerCase().replace('ё', 'е');
        String[] fc = VnCalls.findContact(t);
        if (t.matches(".*(help|rescue|ambulance|sos|112|i feel bad).*")) { VnHome.router.show(VnSos.build(), "Emergency call"); return; }
        if (t.contains("call") || t.contains("dial") || t.contains("ring back")) {
            if (fc != null) VnCalls.confirmCall(fc[0], fc[1]);
            else { say("Who to call? Opening list."); VnHome.router.show(VnCalls.buildContacts(true), "Who to call"); } return; }
        if (t.contains("write") || t.contains("sms") || t.contains("letter")) {
            if (fc != null) VnHome.router.show(VnSms.buildCompose(fc[0], fc[1]), "Writing: " + fc[0]);
            else { say("Who to write? Opening list."); VnHome.router.show(VnCalls.buildContacts(false), "Who to write"); } return; }
        if (t.contains("read message") || t.contains("what did they write") || t.contains("inbox")) { VnHome.router.show(VnSms.buildList(), "Messages"); return; }
        if (t.contains("flashlight") || t.contains("torch") || t.contains("light")) { VnHome.toggleTorch(); return; }
        if (t.contains("louder")) { vol(1); return; }
        if (t.contains("quieter")) { vol(-1); return; }
        if (t.contains("photo album") || t.contains("gallery") || t.contains("show photos")) { try { Intent i = new Intent(Intent.ACTION_VIEW); i.setType("image/*"); startActivity(i); } catch (Exception e) { say("Photo album not found."); } return; }
        if (t.contains("take photo") || t.contains("photo") || t.contains("shoot")) { try { startActivity(new Intent(MediaStore.ACTION_IMAGE_CAPTURE)); say("Camera opened."); } catch (Exception e) { say("Camera didn't open."); } return; }
        if (t.contains("history") || t.contains("who called")) { VnHome.router.show(VnCalls.buildDial(), "Phone"); return; }
        if (t.contains("remind") || t.contains("reminder")) {
            Matcher m = Pattern.compile("at\\s*(\\d{1,2})").matcher(t);
            if (m.find()) {
                int h = Integer.parseInt(m.group(1));
                if ((t.contains("evening") || t.contains("night")) && h < 12) h += 12;
                String text = "Time to take pills!";
                Matcher tm = Pattern.compile("remind\\s*(.*)").matcher(t);
                if (tm.find() && !tm.group(1).trim().isEmpty()) text = tm.group(1).trim();
                VnRemind.schedule(VnRemind.atTime(h, 0), text, "rem", null);
                say("Noted. Will remind at " + h + " o'clock on full screen.");
            } else VnHome.router.show(VnRemind.build(), "Reminders");
            return; }
        if (t.contains("alarm")) { VnHome.router.show(VnAlarm.build(), "Alarm"); return; }
        if (t.contains("settings")) { VnHome.router.show(VnSettings.build(), "Settings"); return; }
        if (t.contains("calendar")) { VnCalendar.openSystem(); return; }
        if (t.contains("what time") || t.contains("time")) { sayTime(); return; }
        if (t.contains("internet") || t.contains("browser")) { try { startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com"))); } catch (Exception e) { say("Couldn't open."); } return; }
        if (t.contains("read") || t.contains("book")) { VnHome.router.show(VnReader.build(), "Read aloud"); return; }
        if (t.contains("vibrate") || t.contains("silent mode") || t.contains("no sound")) { toggleVibro(); return; }
        if (t.contains("open") || t.contains("launch")) { openApp(t); return; }
        if (t.contains("can you") || t.contains("help")) { say("I can: call, write and read SMS, dial, show history and photo album, turn on flashlight, remind, wake, read books and call for help."); return; }
        if (t.contains("repeat")) { say(lastSay); return; }
        say("Didn't understand. Say: call, write, remind, flashlight or help.");
    }

    void vol(int dir) {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir > 0 ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
        say(dir > 0 ? "Louder." : "Quieter.");
    }
    void toggleVibro() {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        boolean vib = am.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE;
        try {
            am.setRingerMode(vib ? AudioManager.RINGER_MODE_NORMAL : AudioManager.RINGER_MODE_VIBRATE);
            say(vib ? "Sound on." : "Vibrate mode on. Buttons now silent.");
        } catch (Exception e) {
            try { startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)); } catch (Exception e2) {}
            say("Need permission to control sound. Open: Apps, Settings, Permissions.");
        }
    }
    void openApp(String t) {
        try {
            String token = t.replaceAll(".*(open|launch)\\s*", "").trim();
            if (token.isEmpty()) { say("Which app to open?"); return; }
            String[] words = token.split("\\s+");
            java.util.List<android.content.pm.ResolveInfo> apps = getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
            for (android.content.pm.ResolveInfo ri : apps) {
                String label = ri.loadLabel(getPackageManager()).toString().toLowerCase();
                for (String w : words) if (w.length() > 3 && label.contains(w)) {
                    Intent li = getPackageManager().getLaunchIntentForPackage(ri.activityInfo.packageName);
                    if (li != null) { startActivity(li); say("Opening: " + ri.loadLabel(getPackageManager())); return; }
                }
            }
            say("Didn't find such app.");
        } catch (Exception e) { say("Couldn't open."); }
    }
    void sayTime() {
        Calendar c = Calendar.getInstance();
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        String[] mons = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        say("Now " + c.get(Calendar.HOUR_OF_DAY) + " hours " + c.get(Calendar.MINUTE) + " minutes. " + days[c.get(Calendar.DAY_OF_WEEK) - 1] + ", " + c.get(Calendar.DAY_OF_MONTH) + " " + mons[c.get(Calendar.MONTH)] + ".");
    }

    // --- статус: соты
    void pollSignal() {
        int bars = 3; String label = "";
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            try {
                Object ss = tm.getClass().getMethod("getSignalStrength").invoke(tm);
                if (ss != null) { Integer lv = (Integer) ss.getClass().getMethod("getLevel").invoke(ss); if (lv != null) bars = Math.max(1, Math.min(4, lv + 1)); }
            } catch (Exception e) {}
            int nt = tm.getNetworkType();
            switch (nt) {
                case TelephonyManager.NETWORK_TYPE_GPRS: case TelephonyManager.NETWORK_TYPE_EDGE: case TelephonyManager.NETWORK_TYPE_CDMA: case TelephonyManager.NETWORK_TYPE_IDEN: label = "E"; break;
                case TelephonyManager.NETWORK_TYPE_UMTS: case TelephonyManager.NETWORK_TYPE_HSDPA: case TelephonyManager.NETWORK_TYPE_HSUPA: case TelephonyManager.NETWORK_TYPE_HSPA: case TelephonyManager.NETWORK_TYPE_EVDO_0: case TelephonyManager.NETWORK_TYPE_EVDO_A: label = "3G"; break;
                case TelephonyManager.NETWORK_TYPE_LTE: label = "4G"; break;
                case TelephonyManager.NETWORK_TYPE_NR: label = "5G"; break;
                default: label = ""; break;
            }
        } catch (Exception e) {}
        if (home != null) home.sb.updateSignal(bars, label);
    }

    // --- батарея
    class BatteryReceiver extends BroadcastReceiver {
        boolean wasCharging = false, wasFull = false, warned = false;
        @Override public void onReceive(Context c, Intent i) {
            int level = i.getIntExtra("level", -1), scale = i.getIntExtra("scale", -1);
            int plugged = i.getIntExtra("plugged", 0);
            boolean nowCharging = plugged != 0;
            if (nowCharging && !wasCharging) say("Charging started.");
            if (!nowCharging && wasCharging) say("Phone unplugged.");
            wasCharging = nowCharging;
            int pct = (level >= 0 && scale > 0) ? level * 100 / scale : -1;
            if (pct >= 0 && home != null) H.post(() -> home.sb.updateBattery(pct, nowCharging));
            if (pct >= 99 && nowCharging && !wasFull) { wasFull = true; say("Battery fully charged. You can unplug."); }
            if (pct < 95) wasFull = false;
            if (pct < 0) return;
            if (plugged != 0) { warned = false; return; }
            if (pct <= 15 && !warned) { warned = true; say("Battery fifteen percent. Put phone on charge please."); }
            if (pct <= 5) showBattOverlay();
        }
    }
    void showBattOverlay() {
        H.post(() -> {
            LinearLayout ov = VnUi.col();
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setColor(VnTheme.RUST);
            ov.setBackground(gd);
            ov.setPadding(VnUi.dp(30), VnUi.dp(30), VnUi.dp(30), VnUi.dp(30));
            ov.addView(VnUi.tv("🪫", 60, VnTheme.CREAM, false));
            TextView t = VnUi.tv("PHONE WILL SHUT DOWN NOW!\nPUT ON CHARGE!", 28, VnTheme.CREAM, true);
            t.setGravity(Gravity.CENTER);
            ov.addView(t);
            ov.addView(VnUi.tv("This window closes by itself when charging starts", 14, 0xFFFFE0D8, true));
            frame.addView(ov, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            say("Attention! Battery almost dead! Put phone on charge!");
        });
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 42 && res == RESULT_OK) VnReader.load(data);
    }
    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        if (i != null && i.hasExtra("reminder")) VnHome.router.show(VnLock.buildReminder(i.getStringExtra("reminder")), "Reminder");
    }
    @Override public void onBackPressed() {
        if (overlayBox != null) { closeOverlayNow(); return; }
        showHome();
    }
    @Override protected void onDestroy() {
        try { unregisterReceiver(battRec); } catch (Exception e) {}
        if (home != null) home.sb.unwatch(this);
        VnVoice.stopListen();
        super.onDestroy();
    }
}
