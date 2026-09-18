package ru.vnuchok.app;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsManager;
import android.location.Location;
import android.location.LocationManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.content.pm.ResolveInfo;
import android.provider.MediaStore;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    static final String[][] CONTACTS = {
            {"Дочь Маша", "+79000000001"},
            {"Внук Миша", "+7900000002"},
            {"Внучка Оля", "+7900000003"},
            {"Соседка Нина", "+7900000004"},
            {"Врач Ирина", "+7900000005"}};

    Handler H = new Handler(Looper.getMainLooper());
    FrameLayout frame; ScrollView scroll;
    TextView caption, timeView;
    TextToSpeech tts; boolean ttsReady;
    SpeechRecognizer sr;
    boolean torchOn, battWarned;
    LinearLayout battOverlay, remOverlay, confirmOverlay;
    String lastSay = "";
    BatteryReceiver battRec;
    Runnable clockRun;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.CALL_PHONE, android.Manifest.permission.SEND_SMS,
                android.Manifest.permission.READ_SMS, android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        frame = new FrameLayout(this);
        frame.setBackgroundColor(Color.parseColor("#F4E3BD"));
        scroll = new ScrollView(this);
        frame.addView(scroll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(frame);
        tts = new TextToSpeech(this, this);
        battRec = new BatteryReceiver();
        registerReceiver(battRec, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        showMain();
        if (getIntent() != null && getIntent().hasExtra("reminder")) showReminder(getIntent().getStringExtra("reminder"));
    }

    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        if (i != null && i.hasExtra("reminder")) showReminder(i.getStringExtra("reminder"));
    }

    @Override public void onInit(int s) {
        if (s == TextToSpeech.SUCCESS) { ttsReady = true; tts.setLanguage(new Locale("ru")); tts.setSpeechRate(0.75f); }
    }

    void say(String t) {
        lastSay = t;
        H.post(() -> { if (caption != null) caption.setText("«" + t + "»"); });
        if (ttsReady) tts.speak(t, TextToSpeech.QUEUE_FLUSH, null, "v");
    }

    int dp(int x) { return Math.round(x * getResources().getDisplayMetrics().density); }

    TextView tv(String s, float size, String color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(Color.parseColor(color));
        if (bold) t.getPaint().setFakeBoldText(true);
        t.setPadding(0, dp(4), 0, dp(4));
        return t;
    }

    Button big(String text, String bg, String fg, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text); b.setTextSize(22); b.setTextColor(Color.parseColor(fg));
        b.setAllCaps(false); b.getPaint().setFakeBoldText(true);
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.parseColor(bg)); g.setCornerRadius(dp(16));
        g.setStroke(dp(2), Color.parseColor("#8A6A3A"));
        b.setBackground(g); b.setPadding(dp(10), dp(18), dp(10), dp(18));
        b.setOnClickListener(l);
        return b;
    }

    LinearLayout col() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(24), dp(24), dp(24), dp(24));
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        return l;
    }

    LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setPadding(0, dp(6), 0, dp(6));
        return l;
    }

    void setScreen(LinearLayout c) {
        H.post(() -> { scroll.removeAllViews(); scroll.addView(c); });
    }

    String curTime() {
        Calendar c = Calendar.getInstance();
        return String.format(Locale.getDefault(), "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    void showMain() {
        LinearLayout c = col();
        timeView = tv(curTime(), 44, "#4A2C17", true);
        timeView.setGravity(Gravity.CENTER);
        c.addView(timeView);
        if (clockRun == null) clockRun = new Runnable() { public void run() { if (timeView != null) timeView.setText(curTime()); H.postDelayed(this, 20000); } };
        H.postDelayed(clockRun, 20000);

        Button mic = big("🎙  ГОВОРИТЕ", "#D94F1E", "#FFFFFF", v -> showListen());
        GradientDrawable og = new GradientDrawable();
        og.setColor(Color.parseColor("#D94F1E")); og.setShape(GradientDrawable.OVAL);
        mic.setBackground(og);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(dp(190), dp(190));
        mp.topMargin = dp(10); mp.bottomMargin = dp(10);
        mic.setLayoutParams(mp);
        mic.setTextSize(24);
        c.addView(mic);
        c.addView(tv("Нажмите и скажите: «позвони дочке»", 15, "#8A6A3A", true));

        LinearLayout capBox = new LinearLayout(this);
        GradientDrawable cg = new GradientDrawable();
        cg.setColor(Color.parseColor("#FFFDF4")); cg.setCornerRadius(dp(12));
        cg.setStroke(dp(2), Color.parseColor("#C98D4F"));
        capBox.setBackground(cg); capBox.setPadding(dp(12), dp(10), dp(12), dp(10));
        capBox.addView(tv("ВНУЧОК ОТВЕЧАЕТ:", 11, "#A05A24", true));
        caption = tv("«Здравствуйте! Нажмите большую кнопку.»", 17, "#54300F", true);
        capBox.addView(caption);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.topMargin = dp(8); cp.bottomMargin = dp(8);
        capBox.setLayoutParams(cp);
        c.addView(capBox);

        LinearLayout r1 = row();
        Button b1 = big("📞 ПОЗВОНИТЬ", "#F9ECCA", "#7A4A21", v -> showContacts(true));
        Button b2 = big("✉️ СООБЩЕНИЯ", "#F9ECCA", "#7A4A21", v -> showContacts(false));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        hp.setMargins(dp(4), 0, dp(4), 0);
        b1.setLayoutParams(hp); b2.setLayoutParams(hp);
        r1.addView(b1); r1.addView(b2); c.addView(r1);

        LinearLayout r2 = row();
        Button b3 = big("📷 ФОТО", "#F9ECCA", "#7A4A21", v -> { try { startActivity(new Intent(MediaStore.ACTION_IMAGE_CAPTURE)); say("Открыл фотоаппарат."); } catch (Exception e) { say("Камера не открылась."); } });
        Button b4 = big("🔦 СВЕТ", "#F9ECCA", "#7A4A21", v -> toggleTorch());
        b3.setLayoutParams(hp); b4.setLayoutParams(hp);
        r2.addView(b3); r2.addView(b4); c.addView(r2);

        Button sos = big("🆘 SOS — ВЫЗВАТЬ ПОМОЩЬ (112)", "#C0392B", "#FFFFFF", v -> startSos());
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin = dp(8);
        sos.setLayoutParams(sp);
        c.addView(sos);
        setScreen(c);
    }

    // ---------- СЛУХ (встроенный распознаватель телефона) ----------
    void showListen() {
        LinearLayout c = col();
        c.addView(tv("СЛУШАЮ…", 30, "#54300F", true));
        TextView partial = tv("…", 20, "#7A4A21", true);
        partial.setGravity(Gravity.CENTER);
        c.addView(partial);
        c.addView(big("✅ ГОТОВО", "#D94F1E", "#FFFFFF", v -> { try { if (sr != null) sr.stopListening(); } catch (Exception e) {} }));
        c.addView(big("ОТМЕНА", "#F9ECCA", "#7A4A21", v -> { stopListening(); showMain(); }));
        setScreen(c);
        startListening(partial);
    }

    void startListening(TextView pv) {
        stopListening();
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { say("Телефон не умеет слушать. Попросите родных обновить сервисы Google."); showMain(); return; }
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
            public void onEndOfSpeech() {}
            public void onError(int e) { H.post(() -> { stopListening(); say("Не расслышал. Повторите, пожалуйста."); showMain(); }); }
            public void onResults(Bundle r) {
                ArrayList<String> a = r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String t = (a != null && !a.isEmpty()) ? a.get(0) : "";
                H.post(() -> { stopListening(); if (!t.isEmpty()) handleCommand(t); else { say("Не расслышал. Повторите, пожалуйста."); showMain(); } });
            }
            public void onPartialResults(Bundle r) {
                ArrayList<String> a = r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (a != null && !a.isEmpty()) H.post(() -> pv.setText(a.get(0)));
            }
            public void onEvent(int i, Bundle b) {}
        });
        try { sr.startListening(it); } catch (Exception e) { say("Микрофон не открылся."); showMain(); }
    }

    void stopListening() {
        try { if (sr != null) { sr.stopListening(); sr.cancel(); sr.destroy(); } } catch (Exception e) {}
        sr = null;
    }

    // ---------- КОМАНДЫ ----------
    void handleCommand(String raw) {
        String t = raw.toLowerCase().replace('ё', 'е');
        int ci = findContact(t);
        if (t.matches(".*(помогите|спасите|скорую|скорая|sos|112|плохо мне).*")) { startSos(); return; }
        if (t.contains("позвони") || t.contains("набери") || t.contains("перезвони")) {
            if (ci >= 0) confirmCall(ci); else { say("Кому звонить? Открываю список."); showContacts(true); } return; }
        if (t.contains("напиши") || t.contains("смс") || t.contains("письмо")) {
            if (ci >= 0) showCompose(ci); else { say("Кому написать? Открываю список."); showContacts(false); } return; }
        if (t.contains("прочитай сообщ") || t.contains("что написали")) { readLastSms(); showMain(); return; }
        if (t.contains("фонарик") || t.contains("свет")) { toggleTorch(); showMain(); return; }
        if (t.contains("громче")) { vol(1); showMain(); return; }
        if (t.contains("тише")) { vol(-1); showMain(); return; }
        if (t.contains("сфотографиру") || t.contains("фото") || t.contains("сними")) {
            try { startActivity(new Intent(MediaStore.ACTION_IMAGE_CAPTURE)); say("Открыл фотоаппарат."); } catch (Exception e) { say("Камера не открылась."); showMain(); } return; }
        if (t.contains("напомни") || t.contains("будильник")) { setReminder(t); showMain(); return; }
        if (t.contains("который час") || t.contains("время") || t.contains("число")) { sayTime(); showMain(); return; }
        if (t.contains("открой") || t.contains("запусти")) { openApp(t); showMain(); return; }
        if (t.contains("умеешь") || t.contains("помощь")) { say("Я умею: звонить, писать СМС, читать сообщения вслух, включать свет, фотографировать, напоминать и вызывать помощь."); showMain(); return; }
        if (t.contains("повтори")) { say(lastSay); showMain(); return; }
        say("Не понял. Скажите: позвони, напиши, напомни, фонарик или помощь.");
        showMain();
    }

    int findContact(String t) {
        String[] keys = {"маш|доч", "миш|внук", "ол|внуч", "нин|сосед", "ирин|врач"};
        for (int i = 0; i < keys.length; i++) if (t.matches(".*(" + keys[i] + ").*")) return i;
        return -1;
    }

    void showContacts(boolean callMode) {
        LinearLayout c = col();
        c.addView(tv(callMode ? "КОМУ ЗВОНИМ?" : "КОМУ ПИШЕМ?", 26, "#54300F", true));
        for (int i = 0; i < CONTACTS.length; i++) {
            final int idx = i;
            c.addView(big(" " + CONTACTS[i][0], "#F9ECCA", "#7A4A21", v -> {
                if (callMode) confirmCall(idx); else showCompose(idx);
            }));
        }
        c.addView(big("⌂ НА ГЛАВНЫЙ", "#4A2C17", "#F3E2BA", v -> showMain()));
        setScreen(c);
    }

    void confirmCall(int i) {
        confirmOverlay = new LinearLayout(this);
        confirmOverlay.setOrientation(LinearLayout.VERTICAL);
        confirmOverlay.setGravity(Gravity.CENTER);
        confirmOverlay.setBackgroundColor(Color.parseColor("#CC4A2C17"));
        LinearLayout card = col();
        card.setBackgroundColor(Color.parseColor("#F9EDD4"));
        card.addView(tv("Звоним: " + CONTACTS[i][0] + "?", 26, "#54300F", true));
        card.addView(big("✅ ДА, ЗВОНИ", "#3FAE4C", "#FFFFFF", v -> {
            removeConfirm();
            try { startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + CONTACTS[i][1]))); say("Звоню: " + CONTACTS[i][0]); }
            catch (Exception e) { say("Не смог позвонить."); }
        }));
        card.addView(big("✋ ОТМЕНА", "#F9ECCA", "#7A4A21", v -> { removeConfirm(); say("Отменил."); }));
        confirmOverlay.addView(card);
        frame.addView(confirmOverlay);
        say("Позвонить: " + CONTACTS[i][0] + "?");
    }

    void removeConfirm() { if (confirmOverlay != null) { frame.removeView(confirmOverlay); confirmOverlay = null; } }

    void showCompose(int i) {
        LinearLayout c = col();
        c.addView(tv("ПИШЕМ: " + CONTACTS[i][0], 24, "#54300F", true));
        EditText et = new EditText(this);
        et.setTextSize(20); et.setMinLines(3);
        et.setHint("Скажите текст или напишите здесь");
        c.addView(et);
        c.addView(big("🔊 ПРОЧИТАТЬ ВСЛУХ", "#F9ECCA", "#7A4A21", v -> speak(et.getText().toString())));
        c.addView(big("✅ ОТПРАВИТЬ", "#3FAE4C", "#FFFFFF", v -> {
            sendSms(CONTACTS[i][1], et.getText().toString());
            say("Отправлено: " + CONTACTS[i][0]);
            showMain();
        }));
        c.addView(big("ОТМЕНА", "#F9ECCA", "#7A4A21", v -> showMain()));
        setScreen(c);
        say("Кому пишем: " + CONTACTS[i][0] + ". Говорите текст.");
    }

    void speak(String t) { if (ttsReady && !t.isEmpty()) tts.speak(t, TextToSpeech.QUEUE_FLUSH, null, "s"); }

    void sendSms(String num, String text) {
        try { SmsManager.getDefault().sendTextMessage(num, null, text, null, null); } catch (Exception e) { say("СМС не ушло."); }
    }

    void readLastSms() {
        try {
            android.database.Cursor cur = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, "date DESC");
            if (cur != null && cur.moveToFirst()) {
                String addr = cur.getString(cur.getColumnIndexOrThrow("address"));
                String body = cur.getString(cur.getColumnIndexOrThrow("body"));
                cur.close();
                say("Сообщение от " + addr + ": " + body);
            } else { say("Новых сообщений нет."); if (cur != null) cur.close(); }
        } catch (Exception e) { say("Не смог прочитать сообщения."); }
    }

    void toggleTorch() {
        try {
            CameraManager cm = (CameraManager) getSystemService(CAMERA_SERVICE);
            String[] ids = cm.getCameraIdList();
            torchOn = !torchOn;
            cm.setTorchMode(ids[0], torchOn);
            say(torchOn ? "Фонарик включил." : "Фонарик выключил.");
        } catch (Exception e) { say("Фонарик не включается."); }
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
                    if (li != null) { startActivity(li); say("Открываю: " + label); return; }
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

    void setReminder(String t) {
        Matcher m = Pattern.compile("в\\s*(\\d{1,2})").matcher(t);
        if (!m.find()) { say("Скажите время, например: напомни в 9 утра."); return; }
        int h = Integer.parseInt(m.group(1));
        if ((t.contains("вечера") || t.contains("ночи")) && h < 12) h += 12;
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0);
        if (c.before(Calendar.getInstance())) c.add(Calendar.DAY_OF_YEAR, 1);
        String text = "Пора принять таблетки!";
        Matcher tm = Pattern.compile("напомни\\s*(.*)").matcher(t);
        if (tm.find() && !tm.group(1).trim().isEmpty()) text = tm.group(1).trim();
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent in = new Intent(this, ReminderReceiver.class).putExtra("text", text);
        PendingIntent pi = PendingIntent.getBroadcast(this, 42, in, PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
        else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);
        say("Запомнил. Напомню в " + h + " часов на весь экран.");
    }

    public static class ReminderReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context c, Intent i) {
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
        remOverlay.setBackgroundColor(Color.parseColor("#F4E3BD"));
        remOverlay.setPadding(dp(30), dp(30), dp(30), dp(30));
        remOverlay.addView(tv("💊", 70, "#C0392B", false));
        TextView bt = tv(text.toUpperCase(), 34, "#C0392B", true);
        bt.setGravity(Gravity.CENTER);
        remOverlay.addView(bt);
        remOverlay.addView(big("✅ ПРИНЯЛ", "#3FAE4C", "#FFFFFF", v -> { frame.removeView(remOverlay); remOverlay = null; say("Молодец! Отметил."); }));
        remOverlay.addView(big("⏰ НАПОМНИ ЧЕРЕЗ ЧАС", "#F9ECCA", "#7A4A21", v -> {
            frame.removeView(remOverlay); remOverlay = null;
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent in = new Intent(this, ReminderReceiver.class).putExtra("text", text);
            PendingIntent pi = PendingIntent.getBroadcast(this, 43, in, PendingIntent.FLAG_IMMUTABLE);
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 3600000, pi);
            say("Напомню ещё раз через час.");
        }));
        frame.addView(remOverlay);
        Vibrator vb = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        try { vb.vibrate(VibrationEffect.createOneShot(800, 255)); } catch (Exception e) {}
        say("Внимание! " + text);
    }

    void startSos() {
        LinearLayout c = col();
        c.addView(tv("ВЫЗЫВАЕМ ПОМОЩЬ!", 28, "#C0392B", true));
        TextView num = tv("5", 90, "#C0392B", true);
        c.addView(num);
        c.addView(tv("Если случайно — жмите ОТМЕНА", 18, "#54300F", true));
        c.addView(big("✋ ОТМЕНА", "#8A8A8A", "#FFFFFF", v -> { H.removeCallbacksAndMessages("sos"); say("Отменили. Всё хорошо."); showMain(); }));
        setScreen(c);
        say("Внимание! Вызываю помощь. Если случайно — отмена.");
        final int[] n = {5};
        H.postDelayed(new Runnable() {
            public void run() {
                n[0]--;
                if (n[0] <= 0) { sosFire(); return; }
                num.setText(String.valueOf(n[0]));
                H.postDelayed(this, 1000);
            }
        }, 1000);
    }

    void sosFire() {
        String loc = locText();
        String sms = "SOS! Нужна помощь срочно! " + loc;
        for (int i = 0; i < 3 && i < CONTACTS.length; i++) sendSms(CONTACTS[i][1], sms);
        try { startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:112"))); } catch (Exception e) {}
        LinearLayout c = col();
        c.addView(tv("☎ ИДЁТ ЗВОНОК В 112…", 26, "#C0392B", true));
        c.addView(tv("СМС отправлено родным:", 18, "#54300F", true));
        c.addView(tv(sms, 16, "#54300F", true));
        c.addView(big("⌂ НА ГЛАВНЫЙ", "#4A2C17", "#F3E2BA", v -> showMain()));
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

    class BatteryReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context c, Intent i) {
            int level = i.getIntExtra("level", -1), scale = i.getIntExtra("scale", -1);
            int plugged = i.getIntExtra("plugged", 0);
            if (level < 0 || scale <= 0) return;
            int pct = level * 100 / scale;
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
        TextView t = tv("ТЕЛЕФОН СЕЙЧАС ВЫКЛЮЧИТСЯ!\nПОСТАВЬТЕ НА ЗАРЯДКУ!", 30, "#FFFFFF", true);
        t.setGravity(Gravity.CENTER);
        battOverlay.addView(t);
        battOverlay.addView(tv("Это окно исчезнет само, когда начнётся зарядка", 15, "#FFE0D8", true));
        frame.addView(battOverlay);
        say("Внимание! Батарея почти села! Поставьте телефон на зарядку!");
    }

    @Override public void onBackPressed() {
        if (confirmOverlay != null) { removeConfirm(); return; }
    }

    @Override protected void onDestroy() {
        try { unregisterReceiver(battRec); } catch (Exception e) {}
        stopListening();
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }
}
