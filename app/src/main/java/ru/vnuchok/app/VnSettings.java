package ru.vnuchok.app;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;
/* НАСТРОЙКИ: темы, редактор главного экрана, текст и речь, доступность, системные ссылки. */
public class VnSettings {
    private static SharedPreferences P() { return VnUi.C.getSharedPreferences("vnuchok", Context.MODE_PRIVATE); }

    public static LinearLayout build() {
        LinearLayout c = VnUi.col();
        c.addView(VnUi.tv("THEME:", 15, VnTheme.GREEN, true));
        LinearLayout rt = VnUi.row();
        int cur = P().getInt("theme", 0);
        for (int i = 0; i < 4; i++) {
            final int ti = i;
            Button b = VnUi.big(VnTheme.NAMES[i], cur == i ? VnTheme.GREEN : VnTheme.MUSTARD, cur == i ? VnTheme.CREAM : VnTheme.BROWN, v -> {
                P().edit().putInt("theme", ti).apply();
                VnTheme.apply(ti);
                VnHome.router.home();
                VnHome.say("Theme: " + VnTheme.NAMES[ti].toLowerCase() + ".");
            });
            b.setTextSize(11 * VnUi.FS * VnUi.SC);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(VnUi.dp(2), 0, VnUi.dp(2), 0);
            b.setLayoutParams(lp);
            rt.addView(b);
        }
        VnUi.addSpaced(c, rt, 8);
        VnUi.addSpaced(c, VnUi.bigI("wrench", "HOME SCREEN EDITOR", VnTheme.MUSTARD, VnTheme.BROWN, v -> VnHome.router.show(buildEditor(), "Screen editor")), 8);
        VnUi.addSpaced(c, toggle("Voice responses", "voice", true), 6);
        VnUi.addSpaced(c, toggle("Button click and highlight", "sound", true), 8);

        c.addView(VnUi.tv("Text size:", 15, VnTheme.GREEN, true));
        LinearLayout r1 = VnUi.row();
        String[] fsN = {"SMALL", "MEDIUM", "LARGE"};
        float[] fsV = {1f, 1.25f, 1.5f};
        for (int i = 0; i < 3; i++) {
            final float v = fsV[i];
            Button b = VnUi.big(fsN[i], Math.abs(VnUi.FS - v) < 0.01 ? VnTheme.GREEN : VnTheme.MUSTARD, Math.abs(VnUi.FS - v) < 0.01 ? VnTheme.CREAM : VnTheme.BROWN, x -> { P().edit().putFloat("fs", v).apply(); VnUi.FS = v; VnHome.router.show(build(), "Settings"); VnHome.say("Text size changed."); });
            b.setTextSize(12 * VnUi.FS * VnUi.SC);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(VnUi.dp(3), 0, VnUi.dp(3), 0);
            b.setLayoutParams(lp);
            r1.addView(b);
        }
        VnUi.addSpaced(c, r1, 8);

        c.addView(VnUi.tv("Speech rate:", 15, VnTheme.GREEN, true));
        LinearLayout r2 = VnUi.row();
        String[] rn = {"SLOW", "NORMAL", "FAST"};
        float[] rv = {0.6f, 0.9f, 1.2f};
        for (int i = 0; i < 3; i++) {
            final float v = rv[i];
            Button b = VnUi.big(rn[i], VnTheme.MUSTARD, VnTheme.BROWN, x -> { P().edit().putFloat("rate", v).apply(); VnVoice.setRate(v); VnHome.say("Speech rate changed."); });
            b.setTextSize(12 * VnUi.FS * VnUi.SC);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(VnUi.dp(3), 0, VnUi.dp(3), 0);
            b.setLayoutParams(lp);
            r2.addView(b);
        }
        VnUi.addSpaced(c, r2, 8);

        c.addView(VnUi.tv("Voice pitch:", 15, VnTheme.GREEN, true));
        LinearLayout r3 = VnUi.row();
        String[] pn = {"LOW", "MEDIUM", "HIGH"};
        float[] pv = {0.8f, 1f, 1.2f};
        for (int i = 0; i < 3; i++) {
            final float v = pv[i];
            Button b = VnUi.big(pn[i], VnTheme.MUSTARD, VnTheme.BROWN, x -> { P().edit().putFloat("pitch", v).apply(); VnVoice.setPitch(v); VnHome.say("Voice pitch changed."); });
            b.setTextSize(12 * VnUi.FS * VnUi.SC);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(VnUi.dp(3), 0, VnUi.dp(3), 0);
            b.setLayoutParams(lp);
            r3.addView(b);
        }
        VnUi.addSpaced(c, r3, 8);

        c.addView(VnUi.tv("VOICE AND ACCESS:", 15, VnTheme.GREEN, true));
        VnUi.addSpaced(c, VnUi.bigI("mic", "VOICE CONTROL", VnTheme.MUSTARD, VnTheme.BROWN, v -> sys(Settings.ACTION_VOICE_INPUT_SETTINGS)), 6);
        VnUi.addSpaced(c, VnUi.bigI("sound", "SPEECH SYNTHESIS (OTHER VOICES)", VnTheme.MUSTARD, VnTheme.BROWN, v -> sys("com.android.settings.TTS_SETTINGS")), 6);
        VnUi.addSpaced(c, VnUi.bigI("people", "ACCESSIBILITY", VnTheme.MUSTARD, VnTheme.BROWN, v -> sys(Settings.ACTION_ACCESSIBILITY_SETTINGS)), 6);
        VnUi.addSpaced(c, VnUi.bigI("gear", "APP PERMISSIONS", VnTheme.MUSTARD, VnTheme.BROWN, v -> { try { VnUi.C.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + VnUi.C.getPackageName()))); } catch (Exception e) { VnHome.say("Couldn't open."); } }), 6);
        VnUi.addSpaced(c, VnUi.bigI("gear", "ANDROID SYSTEM SETTINGS", VnTheme.MUSTARD, VnTheme.BROWN, v -> sys(Settings.ACTION_SETTINGS)), 6);
        VnUi.addSpaced(c, VnUi.bigI("clock", "ABOUT APP", VnTheme.MUSTARD, VnTheme.BROWN, v -> VnHome.router.show(buildAbout(), "About app")), 6);
        return c;
    }

    private static void sys(String action) {
        try { VnUi.C.startActivity(new Intent(action)); } catch (Exception e) { VnHome.say("Couldn't open."); }
    }

    private static LinearLayout toggle(String label, String key, boolean def) {
        LinearLayout r = VnUi.row();
        android.widget.TextView t = VnUi.tv(label, 14, VnTheme.GREEN, true);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(t);
        boolean on = P().getBoolean(key, def);
        Button b = VnUi.big(on ? "ON" : "OFF", on ? 0xFF3FAE4C : VnTheme.GRAY, VnTheme.CREAM, x -> {
            P().edit().putBoolean(key, !P().getBoolean(key, def)).apply();
            if (key.equals("voice")) VnVoice.voiceOn = !P().getBoolean(key, def);
            if (key.equals("sound") && VnHome.inst != null) VnHome.say(!P().getBoolean(key, def) ? "Click on." : "Click off.");
            VnHome.router.show(build(), "Settings");
        });
        b.setTextSize(12 * VnUi.FS * VnUi.SC);
        b.setLayoutParams(new LinearLayout.LayoutParams(VnUi.dp(100), LinearLayout.LayoutParams.WRAP_CONTENT));
        r.addView(b);
        return r;
    }

    public static LinearLayout buildAbout() {
        LinearLayout c = VnUi.col();
        c.addView(VnUi.tv("VNUCHOK", 30, VnTheme.GREEN, true));
        c.addView(VnUi.tv("Version: 3.0 modular", 18, VnTheme.GREEN, true));
        c.addView(VnUi.tv("Android shell for pensioners and visually impaired", 15, VnTheme.BROWN, true));
        c.addView(VnUi.tv("All data stored only on the phone", 13, VnTheme.BROWN, true));
        return c;
    }

    /* --- редактор главного экрана: масштаб плиток, порядок, скрытие --- */
    public static LinearLayout buildEditor() {
        LinearLayout c = VnUi.col();
        c.addView(VnUi.tv("TILE SCALE:", 15, VnTheme.GREEN, true));
        LinearLayout rs = VnUi.row();
        String[] sn = {"SMALL", "MEDIUM", "LARGE"};
        float[] sv = {0.85f, 1f, 1.2f};
        float cur = P().getFloat("tileScale", 1f);
        for (int i = 0; i < 3; i++) {
            final float v = sv[i];
            Button b = VnUi.big(sn[i], Math.abs(cur - v) < 0.01 ? VnTheme.GREEN : VnTheme.MUSTARD, Math.abs(cur - v) < 0.01 ? VnTheme.CREAM : VnTheme.BROWN, x -> { P().edit().putFloat("tileScale", v).apply(); VnHome.router.show(buildEditor(), "Screen editor"); VnHome.say("Scale changed."); });
            b.setTextSize(11 * VnUi.FS * VnUi.SC);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(VnUi.dp(2), 0, VnUi.dp(2), 0);
            b.setLayoutParams(lp);
            rs.addView(b);
        }
        VnUi.addSpaced(c, rs, 8);
        c.addView(VnUi.tv("HOME BUTTONS:", 15, VnTheme.GREEN, true));
        final List<String> order = order();
        String off = P().getString("tilesOff", "");
        for (int i = 0; i < order.size(); i++) {
            final int pos = i;
            final String id = order.get(i);
            final String nm = name(id);
            boolean hidden = off.contains(id);
            LinearLayout rw = VnUi.row();
            Button nb = VnUi.big(nm, hidden ? VnTheme.GRAY : VnTheme.MUSTARD, hidden ? VnTheme.CREAM : VnTheme.BROWN, v -> {
                List<String> list = new ArrayList<>();
                for (String s : P().getString("tilesOff", "").split(",")) if (!s.isEmpty()) list.add(s);
                if (list.remove(id)) VnHome.say(nm + ": hidden.");
                else { list.add(id); VnHome.say(nm + ": on screen."); }
                StringBuilder sb = new StringBuilder();
                for (int k = 0; k < list.size(); k++) { if (k > 0) sb.append(","); sb.append(list.get(k)); }
                P().edit().putString("tilesOff", sb.toString()).apply();
                VnHome.router.show(buildEditor(), "Screen editor");
            });
            nb.setTextSize(13 * VnUi.FS * VnUi.SC);
            nb.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            rw.addView(nb);
            Button up = VnUi.big("↑", VnTheme.MUSTARD, VnTheme.BROWN, v -> { if (pos > 0) { List<String> o = order(); String t = o.remove(pos); o.add(pos - 1, t); saveOrder(o); VnHome.router.show(buildEditor(), "Screen editor"); } });
            up.setLayoutParams(new LinearLayout.LayoutParams(VnUi.dp(60), LinearLayout.LayoutParams.WRAP_CONTENT));
            rw.addView(up);
            Button dn = VnUi.big("↓", VnTheme.MUSTARD, VnTheme.BROWN, v -> { if (pos < order.size() - 1) { List<String> o = order(); String t = o.remove(pos); o.add(pos + 1, t); saveOrder(o); VnHome.router.show(buildEditor(), "Screen editor"); } });
            dn.setLayoutParams(new LinearLayout.LayoutParams(VnUi.dp(60), LinearLayout.LayoutParams.WRAP_CONTENT));
            rw.addView(dn);
            VnUi.addSpaced(c, rw, 6);
        }
        c.addView(VnUi.tv("Gray = hidden button", 12, VnTheme.BROWN, false));
        return c;
    }

    static List<String> order() {
        List<String> out = new ArrayList<>();
        for (String s : P().getString("tiles", "call,sms,apps,rem,alarm,torch").split(",")) if (!s.isEmpty()) out.add(s);
        return out;
    }
    static void saveOrder(List<String> o) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < o.size(); i++) { if (i > 0) sb.append(","); sb.append(o.get(i)); }
        P().edit().putString("tiles", sb.toString()).apply();
    }
    static String name(String id) {
        switch (id) {
            case "call": return "Call";
            case "sms": return "Messages";
            case "apps": return "Apps";
            case "rem": return "Reminders";
            case "alarm": return "Alarm";
            default: return "Flashlight";
        }
    }
}
