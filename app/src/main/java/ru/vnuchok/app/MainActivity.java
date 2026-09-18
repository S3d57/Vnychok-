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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.CallLog;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsManager;
import android.location.Location;
import android.location.LocationManager;
import android.util.Size;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    Handler H = new Handler(Looper.getMainLooper());
    FrameLayout frame; ScrollView scroll;
    TextView caption, timeView;
    TextToSpeech tts; boolean ttsReady;
    SpeechRecognizer sr, cSr;
    boolean torchOn, battWarned;
    LinearLayout battOverlay, remOverlay, confirmOverlay;
    String lastSay = "", pendingNum = "", pendingLabel = "";
    Runnable pendingYes = null;
    BatteryReceiver battRec;
    Runnable clockRun;
    SharedPreferences P;
    float FS = 1f;

    static final String DEF_CONTACTS = "Дочь Маша|+79000000001\nВнук Миша|+7900000002\nВнучка Оля|+7900000003\nСоседка Нина|+7900000004\nВрач Ирина|+7900000005";

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        P = getSharedPreferences("vnuchok", MODE_PRIVATE);
        FS = P.getFloat("fs", 1f);
        requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.CALL_PHONE, android.Manifest.permission.SEND_SMS,
                android.Manifest.permission.READ_SMS, android.Manifest.permission.READ_CALL_LOG,
                android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
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
        if (s == TextToSpeech.SUCCESS) { ttsReady = true; tts.setLanguage(new Locale("ru")); tts.setSpeechRate(P.getFloat("rate", 0.75f)); }
    }

    void say(String t) {
        lastSay = t;
        H.post(() -> { if (caption != null) caption.setText("«" + t + "»"); });
        if (ttsReady && P.getBoolean("voice", true)) tts.speak(t, TextToSpeech.QUEUE_FLUSH, null, "v");
    }

    void speak(String t) { if (ttsReady && !t.isEmpty()) tts.speak(t, TextToSpeech.QUEUE_FLUSH, null, "s"); }

    int dp(int x) { return Math.round(x * getResources().getDisplayMetrics().density); }
    int shade(int c, float f) { return Color.argb(255, (int) (Color.red(c) * f), (int) (Color.green(c) * f), (int) (Color.blue(c) * f)); }

    TextView tv(String s, float size, String color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size * FS); t.setTextColor(Color.parseColor(color));
        if (bold) t.getPaint().setFakeBoldText(true);
        t.setPadding(0, dp(4), 0, dp(4));
        return t;
    }

    void clickFx(View v) {
        if (P.getBoolean("sound", true)) v.playSoundEffect(android.view.SoundEffectConstants.CLICK);
        try { Vibrator vb = (Vibrator) getSystemService(VIBRATOR_SERVICE); vb.vibrate(VibrationEffect.createOneShot(35, 120)); } catch (Exception e) {}
        v.animate().translationY(dp(5)).setDuration(70).withEndAction(() -> v.animate().translationY(0).setDuration(70));
    }

    Button big(String text, String bg, String fg, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text); b.setTextSize(21 * FS); b.setTextColor(Color.parseColor(fg));
        b.setAllCaps(false); b.getPaint().setFakeBoldText(true);
        int top = Color.parseColor(bg);
        GradientDrawable gb = new GradientDrawable(); gb.setColor(shade(top, 0.55f)); gb.setCornerRadius(dp(16));
        GradientDrawable gt = new GradientDrawable(); gt.setColor(top); gt.setCornerRadius(dp(16));
        LayerDrawable ld = new LayerDrawable(new android.graphics.drawable.Drawable[]{gb, gt});
        ld.setLayerInset(1, 0, 0, 0, dp(6));
        b.setBackground(ld);
        b.setPadding(dp(10), dp(18), dp(10), dp(22));
        b.setOnClickListener(v -> { clickFx(v); l.onClick(v); });
        return b;
    }

    LinearLayout col() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(20), dp(20), dp(20), dp(20));
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        return l;
    }

    LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setPadding(0, dp(5), 0, dp(5));
        return l;
    }

    void setScreen(LinearLayout c) {
        H.post(() -> { scroll.removeAllViews(); scroll.addView(c); scroll.scrollTo(0, 0); });
    }

    String curTime() {
        Calendar c = Calendar.getInstance();
        return String.format(Locale.getDefault(), "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    // ---------- ГЛАВНЫЙ ЭКРАН ----------
    void showMain() {
        LinearLayout c = col();
        TextView gear = tv("⚙", 22, "#A97B3F", true);
        LinearLayout.LayoutParams gl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gl.gravity = Gravity.END;
        gear.setLayoutParams(gl);
        gear.setOnLongClickListener(v -> { showSettings(); return true; });
        gear.setOnClickListener(v -> say("Шестерёнка — настройки для родных. Нажмите и держите палец."));
        c.addView(gear);

        timeView = tv(curTime(), 46, "#4A2C17", true);
        timeView.setGravity(Gravity.CENTER);
        c.addView(timeView);
        if (clockRun == null) clockRun = new Runnable() { public void run() { if (timeView != null) timeView.setText(curTime()); H.postDelayed(this, 20000); } };
        H.postDelayed(clockRun, 20000);

        Button mic = big("🎙  ГОВОРИТЕ", "#D94F1E", "#FFFFFF", v -> showListen());
        GradientDrawable og = new GradientDrawable();
        og.setColor(Color.parseColor("#D94F1E")); og.setShape(GradientDrawable.OVAL);
        mic.setBackground(og);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(dp(200), dp(200));
        mp.topMargin = dp(8); mp.bottomMargin = dp(8);
        mic.setLayoutParams(mp);
        mic.setTextSize(26 * FS);
        c.addView(mic);
        c.addView(tv("Нажмите и скажите: «позвони дочке»", 16, "#8A6A3A", true));

        LinearLayout capBox = new LinearLayout(this);
        GradientDrawable cg = new GradientDrawable();
        cg.setColor(Color.parseColor("#FFFDF4")); cg.setCornerRadius(dp(12));
        cg.setStroke(dp(3), Color.parseColor("#C98D4F"));
        capBox.setBackground(cg); capBox.setPadding(dp(14), dp(12), dp(14), dp(12));
        capBox.addView(tv("ВНУЧОК ОТВЕЧАЕТ:", 13, "#A05A24", true));
        caption = tv("«Здравствуйте! Нажмите большую кнопку.»", 21, "#54300F", true);
        capBox.addView(caption);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.topMargin = dp(8); cp.bottomMargin = dp(8);
        capBox.setLayoutParams(cp);
        c.addView(capBox);

        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        hp.setMargins(dp(4), 0, dp(4), 0);
        String[][] tiles = {
                {"📞 ПОЗВОНИТЬ", "1"}, {"✉️ СООБЩЕНИЯ", "2"}, {"☎ НАБОР", "3"},
                {"🕐 ИСТОРИЯ", "4"}, {"📷 ФОТО", "5"}, {"🖼 ГАЛЕРЕЯ", "6"},
                {"⏰ НАПОМИНАНИЯ", "7"}, {"⏰ БУДИЛЬНИК", "8"}, {"🔦 СВЕТ", "9"}};
        for (int r = 0; r < 3; r++) {
            LinearLayout rw = row();
            for (int k = 0; k < 3; k++) {
                final String id = tiles[r * 3 + k][1];
                Button tb = big(tiles[r * 3 + k][0], "#F9ECCA", "#7A4A21", v -> tileClick(id));
                tb.setTextSize(15 * FS);
                tb.setLayoutParams(hp);
                rw.addView(tb);
            }
            c.addView(rw);
        }
        Button sos = big("🆘 SOS — ВЫЗВАТЬ ПОМОЩЬ (112)", "#C0392B", "#FFFFFF", v -> startSos());
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin = dp(8);
        sos.setLayoutParams(sp);
        c.addView(sos);
        c.addView(tv("Шестерёнка вверху — настройки для родных (долгое нажатие)", 12, "#8A6A3A", false));
        setScreen(c);
    }

    void tileClick(String id) {
        switch (id) {
            case "1": showContacts(true); break;
            case "2": showSmsList(); break;
            case "3": showDial(); break;
            case "4": showHistory(); break;
            case "5": try { startActivity(new Intent(MediaStore.ACTION_IMAGE_CAPTURE)); say("Открыл фотоаппарат."); } catch (Exception e) { say("Камера не открылась."); } break;
            case "6": showGallery(); break;
            case "7": showReminders(); break;
            case "8": showAlarms(); break;
            case "9": toggleTorch(); break;
        }
    }

    // ---------- НАСТРОЙКИ ----------
    void showSettings() {
        LinearLayout c = col();
        c.addView(tv("⚙ НАСТРОЙКИ (для родных)", 24, "#54300F", true));
        LinearLayout r1 = row();
        r1.addView(tv("Размер текста:", 17, "#54300F", true));
        String[] fsN = {"МЕЛКИЙ", "СРЕДНИЙ", "КРУПНЫЙ"};
        float[] fsV = {1f, 1.25f, 1.5f};
        for (int i = 0; i < 3; i++) {
            final float v = fsV[i];
            Button b = big(fsN[i], Math.abs(FS - v) < 0.01 ? "#D94F1E" : "#F9ECCA", Math.abs(FS - v) < 0.01 ? "#FFFFFF" : "#7A4A21", x -> { P.edit().putFloat("fs", v).apply(); FS = v; showSettings(); say("Размер текста изменён."); });
            b.setTextSize(14 * FS);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            b.setLayoutParams(lp);
            r1.addView(b);
        }
        c.addView(r1);
        LinearLayout r2 = row();
        r2.addView(tv("Речь:", 17, "#54300F", true));
        String[] rtN = {"МЕДЛЕННО", "ОБЫЧНО"};
        float[] rtV = {0.65f, 0.95f};
        for (int i = 0; i < 2; i++) {
            final float v = rtV[i];
            Button b = big(rtN[i], Math.abs(P.getFloat("rate", 0.75f) - v) < 0.01 ? "#D94F1E" : "#F9ECCA", Math.abs(P.getFloat("rate", 0.75f) - v) < 0.01 ? "#FFFFFF" : "#7A4A21", x -> { P.edit().putFloat("rate", v).apply(); if (ttsReady) tts.setSpeechRate(v); showSettings(); say("Скорость речи изменена."); });
            b.setTextSize(14 * FS);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            b.setLayoutParams(lp);
            r2.addView(b);
        }
        c.addView(r2);
        c.addView(toggleRow("Ответы вслух", "voice", true));
        c.addView(toggleRow("Щелчки кнопок", "sound", true));
        c.addView(big("⌂ НА ГЛАВНЫЙ", "#4A2C17", "#F3E2BA", v -> showMain()));
        setScreen(c);
    }

    LinearLayout toggleRow(String label, String key, boolean def) {
        LinearLayout r = row();
        r.addView(tv(label, 17, "#54300F", true));
        boolean on = P.getBoolean(key, def);
        Button b = big(on ? "ВКЛ" : "ВЫКЛ", on ? "#3FAE4C" : "#8A8A8A", "#FFFFFF", x -> { P.edit().putBoolean(key, !P.getBoolean(key, def)).apply(); showSettings(); });
        b.setTextSize(14 * FS);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(110), ViewGroup.LayoutParams.WRAP_CONTENT);
        b.setLayoutParams(lp);
        r.addView(b);
        return r;
    }

    // ---------- СЛУХ ----------
    void showListen() {
        LinearLayout c = col();
        c.addView(tv("СЛУШАЮ…", 32, "#54300F", true));
        TextView partial = tv("…", 22, "#7A4A21", true);
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

    void stopConfirmListen() {
        try { if (cSr != null) { cSr.stopListening(); cSr.cancel(); cSr.destroy(); } } catch (Exception e) {}
        cSr = null;
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
        if (t.contains("фонарик") || t.contains("свет")) { toggleTorch(); showMain(); return; }
        if (t.contains("громче")) { vol(1); showMain(); return; }
        if (t.contains("тише")) { vol(-1); showMain(); return; }
        if (t.contains("галере") || t.contains("покажи фото") || t.contains("фотографии")) { showGallery(); return; }
        if (t.contains("сфотографиру") || t.contains("фото") || t.contains("сними")) {
            try { startActivity(new Intent(MediaStore.ACTION_IMAGE_CAPTURE)); say("Открыл фотоаппарат."); } catch (Exception e) { say("Камера не открылась."); showMain(); } return; }
        if (t.contains("истори") || t.contains("кто звонил")) { showHistory(); return; }
        if (t.contains("напомни") || t.contains("напоминан")) { 
            if (t.contains("в ") || t.contains("через")) { setReminder(t); showMain(); } else showReminders(); return; }
        if (t.contains("будильник")) { showAlarms(); return; }
        if (t.contains("настрой")) { showSettings(); return; }
        if (t.contains("набер") || t.contains("номер")) { showDial(); return; }
        if (t.contains("который час") || t.contains("время") || t.contains("число")) { sayTime(); showMain(); return; }
        if (t.contains("открой") || t.contains("запусти")) { openApp(t); showMain(); return; }
        if (t.contains("умеешь") || t.contains("помощь")) { say("Я умею: звонить, писать и читать СМС, набирать номер, показывать историю и галерею, включать свет, напоминать, будить и вызывать помощь."); showMain(); return; }
        if (t.contains("повтори")) { say(lastSay); showMain(); return; }
        say("Не понял. Скажите: позвони, напиши, напомни, фонарик или помощь.");
        showMain();
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
        c.addView(tv(callMode ? "КОМУ ЗВОНИМ?" : "КОМУ ПИШЕМ?", 26, "#54300F", true));
        List<String[]> cs = contacts();
        for (int i = 0; i < cs.size(); i++) {
            final int idx = i;
            LinearLayout rw = row();
            Button nb = big(cs.get(i)[0], "#F9ECCA", "#7A4A21", v -> { if (callMode) confirmCall(cs.get(idx)[0], cs.get(idx)[1]); else showCompose(idx); });
            nb.setTextSize(17 * FS);
            nb.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            rw.addView(nb);
            Button eb = big("✏️", "#E09F3E", "#FFFFFF", v -> editContact(idx));
            eb.setLayoutParams(new LinearLayout.LayoutParams(dp(70), ViewGroup.LayoutParams.WRAP_CONTENT));
            rw.addView(eb);
            Button db = big("🗑", "#C0392B", "#FFFFFF", v -> confirmDialog("Удалить " + cs.get(idx)[0] + "?", () -> { List<String[]> x = contacts(); x.remove(idx); saveContacts(x); say("Удалил."); showContacts(callMode); }));
            db.setLayoutParams(new LinearLayout.LayoutParams(dp(70), ViewGroup.LayoutParams.WRAP_CONTENT));
            rw.addView(db);
            c.addView(rw);
        }
        c.addView(big("➕ НОВЫЙ НОМЕР", "#3FAE4C", "#FFFFFF", v -> editContact(-1)));
        c.addView(big("⌂ НА ГЛАВНЫЙ", "#4A2C17", "#F3E2BA", v -> showMain()));
        setScreen(c);
    }

    void editContact(int idx) {
        List<String[]> cs = contacts();
        LinearLayout c = col();
        c.addView(tv(idx < 0 ? "НОВЫЙ КОНТАКТ" : "ИЗМЕНИТЬ", 24, "#54300F", true));
        EditText en = new EditText(this); en.setTextSize(20 * FS); en.setHint("Имя (например: Дочь Маша)");
        if (idx >= 0) en.setText(cs.get(idx)[0]);
        c.addView(en);
        EditText ep = new EditText(this); ep.setTextSize(20 * FS); ep.setHint("Номер (например: +79121234567)");
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
        c.addView(big("ОТМЕНА", "#F9ECCA", "#7A4A21", v -> showContacts(true)));
        setScreen(c);
    }

    void confirmDialog(String title, Runnable yes) {
        confirmOverlay = new LinearLayout(this);
        confirmOverlay.setOrientation(LinearLayout.VERTICAL);
        confirmOverlay.setGravity(Gravity.CENTER);
        confirmOverlay.setBackgroundColor(Color.parseColor("#CC4A2C17"));
        LinearLayout card = col();
        card.setBackgroundColor(Color.parseColor("#F9EDD4"));
        card.addView(tv(title, 24, "#54300F", true));
        card.addView(big("✅ ДА", "#3FAE4C", "#FFFFFF", v -> { removeConfirm(); yes.run(); }));
        card.addView(big("✋ ОТМЕНА", "#F9ECCA", "#7A4A21", v -> { removeConfirm(); }));
        confirmOverlay.addView(card);
        frame.addView(confirmOverlay);
    }

    void removeConfirm() {
        stopConfirmListen();
        if (confirmOverlay != null) { frame.removeView(confirmOverlay); confirmOverlay = null; }
        pendingYes = null;
    }

    // ---------- ЗВОНОК С ГОЛОСОВЫМ ПОДТВЕРЖДЕНИЕМ ----------
    void confirmCall(String label, String num) {
        pendingNum = num; pendingLabel = label;
        confirmOverlay = new LinearLayout(this);
        confirmOverlay.setOrientation(LinearLayout.VERTICAL);
        confirmOverlay.setGravity(Gravity.CENTER);
        confirmOverlay.setBackgroundColor(Color.parseColor("#CC4A2C17"));
        LinearLayout card = col();
        card.setBackgroundColor(Color.parseColor("#F9EDD4"));
        card.addView(tv("Звоним: " + label + "?", 26, "#54300F", true));
        card.addView(tv("Скажите «да» или «нет»", 16, "#8A6A3A", true));
        card.addView(big("✅ ДА, ЗВОНИ", "#3FAE4C", "#FFFFFF", v -> { removeConfirm(); callNumber(pendingNum, pendingLabel); }));
        card.addView(big("✋ ОТМЕНА", "#F9ECCA", "#7A4A21", v -> { removeConfirm(); say("Отменил."); }));
        confirmOverlay.addView(card);
        frame.addView(confirmOverlay);
        say("Позвонить: " + label + "? Скажите да или нет.");
        startConfirmListen();
    }

    void startConfirmListen() {
        stopConfirmListen();
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
            public void onError(int e) {}
            public void onResults(Bundle r) {
                ArrayList<String> a = r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String t = (a != null && !a.isEmpty()) ? a.get(0).toLowerCase() : "";
                H.post(() -> {
                    if (confirmOverlay == null) return;
                    if (t.contains("нет") || t.contains("отмен") || t.contains("не надо")) { removeConfirm(); say("Отменил."); }
                    else if (t.contains("да") || t.contains("звони") || t.contains("конечно") || t.contains("ага")) { String n = pendingNum, l = pendingLabel; removeConfirm(); callNumber(n, l); }
                    else { say("Скажите да или нет."); startConfirmListen(); }
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
        requestPermissions(new String[]{android.Manifest.permission.CALL_PHONE}, 2);
        try { startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + num))); } catch (Exception e) {}
        say("Нет разрешения на звонки. Нажмите кнопку вызова на экране или разрешите звонки в настройках телефона.");
    }

    // ---------- НАБОР НОМЕРА ----------
    void showDial() {
        LinearLayout c = col();
        TextView disp = tv("", 34, "#4A2C17", true);
        disp.setGravity(Gravity.CENTER);
        disp.setMinHeight(dp(60));
        c.addView(disp);
        StringBuilder cur = new StringBuilder();
        String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"};
        for (int r = 0; r < 4; r++) {
            LinearLayout rw = row();
            for (int k = 0; k < 3; k++) {
                String key = keys[r * 3 + k];
                Button b = big(key, "#F9ECCA", "#7A4A21", v -> { cur.append(key); disp.setText(cur.toString()); });
                b.setTextSize(26 * FS);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                lp.setMargins(dp(4), 0, dp(4), 0);
                b.setLayoutParams(lp);
                rw.addView(b);
            }
            c.addView(rw);
        }
        LinearLayout rw = row();
        Button del = big("⌫", "#8A8A8A", "#FFFFFF", v -> { if (cur.length() > 0) cur.deleteCharAt(cur.length() - 1); disp.setText(cur.toString()); });
        Button call = big("📞 ПОЗВОНИТЬ", "#3FAE4C", "#FFFFFF", v -> { if (cur.length() > 0) confirmCall(cur.toString(), cur.toString()); });
        del.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        call.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f));
        rw.addView(del); rw.addView(call);
        c.addView(rw);
        c.addView(big("⌂ НА ГЛАВНЫЙ", "#4A2C17", "#F3E2BA", v -> showMain()));
        setScreen(c);
    }

    // ---------- ИСТОРИЯ ЗВОНКОВ ----------
    void showHistory() {
        LinearLayout c = col();
        c.addView(tv("ИСТОРИЯ ЗВОНКОВ", 26, "#54300F", true));
        try {
            Cursor cur = getContentResolver().query(CallLog.Calls.CONTENT_URI,
                    new String[]{CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE},
                    null, null, CallLog.Calls.DATE + " DESC LIMIT 15");
            if (cur != null) {
                while (cur.moveToNext()) {
                    String num = cur.getString(0);
                    long date = cur.getLong(1);
                    int type = cur.getInt(2);
                    String arrow = type == CallLog.Calls.OUTGOING_TYPE ? "→" : type == CallLog.Calls.MISSED_TYPE ? "✗" : "←";
                    String color = type == CallLog.Calls.MISSED_TYPE ? "#C0392B" : "#7A4A21";
                    String name = nameForNumber(num);
                    Calendar cd = Calendar.getInstance(); cd.setTimeInMillis(date);
                    String when = String.format(Locale.getDefault(), "%02d:%02d %02d.%02d", cd.get(Calendar.HOUR_OF_DAY), cd.get(Calendar.MINUTE), cd.get(Calendar.DAY_OF_MONTH), cd.get(Calendar.MONTH) + 1);
                    final String fnum = num;
                    Button b = big(arrow + " " + name + "\n" + when, "#F9ECCA", color, v -> confirmCall(name, fnum));
                    b.setTextSize(16 * FS);
                    c.addView(b);
                }
                cur.close();
            }
        } catch (Exception e) { c.addView(tv("История недоступна: разрешите доступ к звонкам", 16, "#C0392B", true)); }
        c.addView(big("⌂ НА ГЛАВНЫЙ", "#4A2C17", "#F3E2BA", v -> showMain()));
        setScreen(c);
    }

    String nameForNumber(String num) {
        for (String[] cc : contacts()) if (num != null && num.contains(cc[1].substring(Math.max(0, cc[1].length() - 7)))) return cc[0];
        return num == null ? "?" : num;
    }

    // ---------- СМС ----------
    void showSmsList() {
        LinearLayout c = col();
        c.addView(tv("ВХОДЯЩИЕ СООБЩЕНИЯ", 26, "#54300F", true));
        c.addView(big("✍️ НАПИСАТЬ НОВОЕ", "#3FAE4C", "#FFFFFF", v -> showContacts(false)));
        try {
            Cursor cur = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, "date DESC LIMIT 10");
            if (cur != null) {
                while (cur.moveToNext()) {
                    String addr = cur.getString(cur.getColumnIndexOrThrow("address"));
                    String body = cur.getString(cur.getColumnIndexOrThrow("body"));
                    String shortB = body.length() > 60 ? body.substring(0, 60) + "…" : body;
                    Button b = big("От: " + nameForNumber(addr) + "\n" + shortB, "#F9ECCA", "#7A4A21", v -> say("Сообщение от " + nameForNumber(addr) + ": " + body));
                    b.setTextSize(16 * FS);
                    c.addView(b);
                }
                cur.close();
            }
        } catch (Exception e) { c.addView(tv("СМС недоступны: разрешите доступ к сообщениям", 16, "#C0392B", true)); }
        c.addView(big("⌂ НА ГЛАВНЫЙ", "#4A2C17", "#F3E2BA", v -> showMain()));
        setScreen(c);
    }

    void showCompose(int i) {
        List<String[]> cs = contacts();
        LinearLayout c = col();
        c.addView(tv("ПИШЕМ: " + cs.get(i)[0], 24, "#54300F", true));
        EditText et = new EditText(this);
        et.setTextSize(20 * FS); et.setMinLines(3);
        et.setHint("Скажите текст или напишите здесь");
        c.addView(et);
        c.addView(big("🔊 ПРОЧИТАТЬ ВСЛУХ", "#F9ECCA", "#7A4A21", v -> speak(et.getText().toString())));
        c.addView(big("✅ ОТПРАВИТЬ", "#3FAE4C", "#FFFFFF", v -> {
            sendSms(cs.get(i)[1], et.getText().toString());
            say("Отправлено: " + cs.get(i)[0]);
            showMain();
        }));
        c.addView(big("ОТМЕНА", "#F9ECCA", "#7A4A21", v -> showMain()));
        setScreen(c);
        say("Кому пишем: " + cs.get(i)[0] + ". Говорите текст.");
    }

    void sendSms(String num, String text) {
        try { SmsManager.getDefault().sendTextMessage(num, null, text, null, null); } catch (Exception e) { say("СМС не ушло."); }
    }

    // ---------- ГАЛЕРЕЯ ----------
    void showGallery() {
        LinearLayout c = col();
        c.addView(tv("ГАЛЕРЕЯ", 26, "#54300F", true));
        c.addView(big("📷 СФОТОГРАФИРОВАТЬ", "#D94F1E", "#FFFFFF", v -> { try { startActivity(new Intent(MediaStore.ACTION_IMAGE_CAPTURE)); } catch (Exception e) {} }));
        try {
            Cursor cur = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED},
                    null, null, MediaStore.Images.Media.DATE_ADDED + " DESC LIMIT 12");
            if (cur != null) {
                LinearLayout rw = null;
                int n = 0;
                while (cur.moveToNext()) {
                    if (n % 3 == 0) { rw = row(); c.addView(rw); }
                    long id = cur.getLong(0);
                    Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
                    ImageButton ib = new ImageButton(this);
                    Bitmap th = null;
                    try { if (android.os.Build.VERSION.SDK_INT >= 29) th = getContentResolver().loadThumbnail(uri, new Size(dp(150), dp(150)), null); } catch (Exception e) {}
                    if (th != null) ib.setImageBitmap(th); else { ib.setBackgroundColor(Color.parseColor("#E9D0A0")); ib.setImageBitmap(null); }
                    ib.setScaleType(ImageButton.ScaleType.CENTER_CROP);
                    ib.setOnClickListener(v -> { try { Intent vi = new Intent(Intent.ACTION_VIEW, uri); vi.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); startActivity(vi); } catch (Exception e) {} });
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(110), 1f);
                    lp.setMargins(dp(3), dp(3), dp(3), dp(3));
                    ib.setLayoutParams(lp);
                    rw.addView(ib);
                    n++;
                }
                cur.close();
                if (n == 0) c.addView(tv("Фотографий пока нет", 18, "#8A6A3A", true));
            }
        } catch (Exception e) { c.addView(tv("Галерея недоступна", 16, "#C0392B", true)); }
        c.addView(big("⌂ НА ГЛАВНЫЙ", "#4A2C17", "#F3E2BA", v -> showMain()));
        setScreen(c);
    }

    // ---------- НАПОМИНАНИЯ И БУДИЛЬНИК ----------
    List<String[]> rems() {
        List<String[]> out = new ArrayList<>();
        for (String line : P.getString("rems", "").split("\n")) {
            String[] p = line.split("\\|");
            if (p.length == 2) out.add(p);
        }
        return out;
    }

    void saveRems(List<String[]> r) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < r.size(); i++) { if (i > 0) sb.append("\n"); sb.append(r.get(i)[0]).append("|").append(r.get(i)[1]); }
        P.edit().putString("rems", sb.toString()).apply();
    }

    void addReminder(long millis, String text) {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent in = new Intent(this, ReminderReceiver.class).putExtra("text", text);
        int code = (int) (millis % 100000);
        PendingIntent pi = PendingIntent.getBroadcast(this, code, in, PendingIntent.FLAG_IMMUTABLE);
        if (android.os.Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi);
        else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi);
        List<String[]> r = rems();
        r.add(new String[]{String.valueOf(millis), text});
        saveRems(r);
    }

    void showRemList(LinearLayout c) {
        List<String[]> r = rems();
        long now = System.currentTimeMillis();
        for (int i = r.size() - 1; i >= 0; i--) {
            long ms = Long.parseLong(r.get(i)[0]);
            if (ms <= now) continue;
            Calendar cd = Calendar.getInstance(); cd.setTimeInMillis(ms);
            String when = String.format(Locale.getDefault(), "%02d:%02d %02d.%02d", cd.get(Calendar.HOUR_OF_DAY), cd.get(Calendar.MINUTE), cd.get(Calendar.DAY_OF_MONTH), cd.get(Calendar.MONTH) + 1);
            final int idx = i;
            LinearLayout rw = row();
            Button b = big(when + " — " + r.get(i)[1], "#F9ECCA", "#7A4A21", v -> say("Напоминание: " + r.get(idx)[1] + " в " + when));
            b.setTextSize(15 * FS);
            b.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            rw.addView(b);
            Button db = big("✕", "#C0392B", "#FFFFFF", v -> { List<String[]> x = rems(); x.remove(idx); saveRems(x); say("Убрал напоминание."); showReminders(); });
            db.setLayoutParams(new LinearLayout.LayoutParams(dp(70), ViewGroup.LayoutParams.WRAP_CONTENT));
            rw.addView(db);
            c.addView(rw);
        }
    }

    void showReminders() {
        LinearLayout c = col();
        c.addView(tv("⏰ НАПОМИНАНИЯ", 26, "#54300F", true));
        c.addView(big("💊 Через час: таблетки", "#F9ECCA", "#7A4A21", v -> { addReminder(System.currentTimeMillis() + 3600000, "Пора принять таблетки!"); say("Напомню через час."); showReminders(); }));
        c.addView(big("💊 Утром в 9:00: таблетки", "#F9ECCA", "#7A4A21", v -> { addReminder(atTime(9, 0), "Пора принять таблетки!"); say("Напомню утром в девять."); showReminders(); }));
        c.addView(big("💊 Вечером в 21:00: таблетки", "#F9ECCA", "#7A4A21", v -> { addReminder(atTime(21, 0), "Пора принять таблетки!"); say("Напомню вечером в девять."); showReminders(); }));
        c.addView(tv("Уже стоит:", 16, "#8A6A3A", true));
        showRemList(c);
        c.addView(big("⌂ НА ГЛАВНЫЙ", "#4A2C17", "#F3E2BA", v -> showMain()));
        setScreen(c);
    }

    void showAlarms() {
        LinearLayout c = col();
        c.addView(tv("⏰ БУДИЛЬНИК", 26, "#54300F", true));
        NumberPicker nh = new NumberPicker(this); nh.setMinValue(0); nh.setMaxValue(23); nh.setValue(7);
        NumberPicker nm = new NumberPicker(this); nm.setMinValue(0); nm.setMaxValue(55); nm.setValue(0); nm.setWrapSelectorOrder(true);
        LinearLayout rw = row();
        rw.setGravity(Gravity.CENTER);
        nh.setLayoutParams(new LinearLayout.LayoutParams(dp(110), dp(160)));
        nm.setLayoutParams(new LinearLayout.LayoutParams(dp(110), dp(160)));
        rw.addView(nh); rw.addView(nm);
        c.addView(rw);
        c.addView(big("✅ ПОСТАВИТЬ БУДИЛЬНИК", "#3FAE4C", "#FFFFFF", v -> {
            addReminder(atTime(nh.getValue(), nm.getValue()), "Будильник! Пора вставать!");
            say("Будильник поставлен на " + nh.getValue() + " " + nm.getValue() + ".");
            showAlarms();
        }));
        c.addView(tv("Уже стоит:", 16, "#8A6A3A", true));
        showRemList(c);
        c.addView(big("⌂ НА ГЛАВНЫЙ", "#4A2C17", "#F3E2BA", v -> showMain()));
        setScreen(c);
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
        TextView bt = tv(text.toUpperCase(), 36, "#C0392B", true);
        bt.setGravity(Gravity.CENTER);
        remOverlay.addView(bt);
        remOverlay.addView(big("✅ ПРИНЯЛ", "#3FAE4C", "#FFFFFF", v -> { frame.removeView(remOverlay); remOverlay = null; say("Молодец! Отметил."); }));
        remOverlay.addView(big("⏰ НАПОМНИ ЧЕРЕЗ ЧАС", "#F9ECCA", "#7A4A21", v -> {
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
        List<String[]> cs = contacts();
        for (int i = 0; i < 3 && i < cs.size(); i++) sendSms(cs.get(i)[1], sms);
        callNumber("112", "служба спасения");
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

    // ---------- БАТАРЕЯ ----------
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

    @Override public void onBackPressed() {
        if (confirmOverlay != null) { removeConfirm(); return; }
    }

    @Override protected void onDestroy() {
        try { unregisterReceiver(battRec); } catch (Exception e) {}
        stopListening(); stopConfirmListen();
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }
}
