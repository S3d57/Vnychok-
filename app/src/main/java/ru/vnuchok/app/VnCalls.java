package ru.vnuchok.app;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
/* ЗВОНИЛКА: контакты, набор, история, звонок с голосовым подтверждением. */
public class VnCalls {
    public interface Run { void run(); }
    private static SharedPreferences P() { return VnUi.C.getSharedPreferences("vnuchok", Context.MODE_PRIVATE); }
    static final String DEF = "Daughter Masha|+79000000001\nGrandson Misha|+7900000002\nGranddaughter Olya|+7900000003\nNeighbor Nina|+7900000004\nDoctor Irina|+7900000005";

    public static List<String[]> contacts() {
        List<String[]> out = new ArrayList<>();
        for (String line : P().getString("contacts", DEF).split("\n")) {
            String[] p = line.split("\\|");
            if (p.length == 2) out.add(p);
        }
        return out;
    }
    public static List<String> hidden() {
        List<String> out = new ArrayList<>();
        for (String s : P().getString("hidden", "").split("\n")) if (!s.isEmpty()) out.add(s);
        return out;
    }
    public static List<String[]> sysContacts() {
        List<String[]> out = new ArrayList<>();
        List<String> hid = hidden();
        List<String[]> cust = contacts();
        try {
            Cursor cur = VnUi.C.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER},
                    null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIMIT 1000");
            if (cur != null) {
                while (cur.moveToNext()) {
                    String n = cur.getString(0); String num = cur.getString(1);
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
    public static List<String[]> allContacts() {
        List<String[]> all = contacts();
        all.addAll(sysContacts());
        return all;
    }
    public static void saveContacts(List<String[]> cs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cs.size(); i++) { if (i > 0) sb.append("\n"); sb.append(cs.get(i)[0]).append("|").append(cs.get(i)[1]); }
        P().edit().putString("contacts", sb.toString()).apply();
    }
    public static boolean sim(String a, String b) {
        if (a.length() < 4 || b.length() < 4) return a.equals(b);
        return a.substring(0, 3).equals(b.substring(0, 3));
    }
    public static String[] findContact(String t) {
        List<String[]> cs = allContacts();
        String[] tw = t.split("[^a-zа-яё0-9]+");
        for (String[] cc : cs) {
            String[] nw = cc[0].toLowerCase().split("\\s+");
            for (String a : tw) for (String b : nw) if (a.length() >= 4 && sim(a, b)) return cc;
        }
        return null;
    }
    public static String nameForNumber(String num) {
        if (num == null) return "?";
        String clean = num.replaceAll("[^0-9]", "");
        for (String[] cc : allContacts()) {
            String ccn = cc[1].replaceAll("[^0-9]", "");
            if (clean.length() >= 7 && ccn.length() >= 7 && clean.endsWith(ccn.substring(ccn.length() - 7))) return cc[0];
        }
        return num;
    }

    public static LinearLayout buildContacts(boolean callMode) {
        LinearLayout c = VnUi.col();
        List<String[]> cs = allContacts();
        int customN = contacts().size();
        int upTo = Math.min(30, cs.size());
        for (int i = 0; i < upTo; i++) {
            final String nm = cs.get(i)[0], num = cs.get(i)[1];
            final boolean isCustom = i < customN;
            final int idx = i;
            LinearLayout rw = VnUi.row();
            Button nb = VnUi.bigI("people", nm, VnTheme.MUSTARD, VnTheme.BROWN, v -> {
                if (callMode) confirmCall(nm, num);
                else VnHome.router.show(VnSms.buildCompose(nm, num), "Writing: " + nm);
            });
            nb.setTextSize(14 * VnUi.FS * VnUi.SC);
            nb.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            rw.addView(nb);
            Button eb = VnUi.big("✏", VnTheme.GREEN, VnTheme.CREAM, v -> VnHome.router.show(buildEdit(isCustom ? idx : -1, nm, num), isCustom ? "Edit" : "New contact"));
            eb.setLayoutParams(new LinearLayout.LayoutParams(VnUi.dp(64), LinearLayout.LayoutParams.WRAP_CONTENT));
            rw.addView(eb);
            Button db = VnUi.big("✕", VnTheme.RUST, VnTheme.CREAM, v -> confirmDialog("Remove " + nm + " from list?", () -> {
                if (isCustom) { List<String[]> x = contacts(); x.remove(idx); saveContacts(x); }
                else { String h = P().getString("hidden", ""); P().edit().putString("hidden", h + (h.isEmpty() ? "" : "\n") + nm + "|" + num).apply(); }
                VnHome.say("Removed: " + nm);
                VnHome.router.show(buildContacts(callMode), callMode ? "Who to call" : "Who to write");
            }));
            db.setLayoutParams(new LinearLayout.LayoutParams(VnUi.dp(64), LinearLayout.LayoutParams.WRAP_CONTENT));
            rw.addView(db);
            VnUi.addSpaced(c, rw, 8);
        }
        Button header = VnUi.bigI("people", "NEW NUMBER", 0xFF3FAE4C, VnTheme.CREAM, v -> VnHome.router.show(buildEdit(-1, "", ""), "New contact"));
        header.setTextSize(16 * VnUi.FS * VnUi.SC);
        c.addView(header, 0);
        return c;
    }

    public static LinearLayout buildEdit(int idx, String preN, String preP) {
        LinearLayout c = VnUi.col();
        EditText en = new EditText(VnUi.C); en.setTextSize(18 * VnUi.FS * VnUi.SC); en.setHint("Name (e.g. Daughter Masha)");
        en.setText(preN);
        c.addView(en);
        EditText ep = new EditText(VnUi.C); ep.setTextSize(18 * VnUi.FS * VnUi.SC); ep.setHint("Number (e.g. +79121234567)");
        ep.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        ep.setText(preP);
        c.addView(ep);
        VnUi.addSpaced(c, VnUi.big("💾 SAVE", 0xFF3FAE4C, VnTheme.CREAM, v -> {
            String n = en.getText().toString().trim(), p = ep.getText().toString().trim();
            if (n.isEmpty() || p.isEmpty()) { VnHome.say("Fill in name and number."); return; }
            List<String[]> x = contacts();
            if (idx >= 0 && idx < x.size()) x.set(idx, new String[]{n, p});
            else x.add(new String[]{n, p});
            saveContacts(x);
            VnHome.say("Saved: " + n);
            VnHome.router.show(buildContacts(true), "Who to call");
        }), 8);
        VnUi.addSpaced(c, VnUi.big("CANCEL", VnTheme.MUSTARD, VnTheme.BROWN, v -> VnHome.router.show(buildContacts(true), "Who to call")), 8);
        return c;
    }

    public static void confirmDialog(String title, Run yes) {
        LinearLayout ov = VnUi.col();
        ov.setBackground(VnUi.pill(VnTheme.CREAM));
        ov.addView(VnUi.tv(title, 20, VnTheme.GREEN, true));
        VnUi.addSpaced(ov, VnUi.big("✅ YES", 0xFF3FAE4C, VnTheme.CREAM, v -> { VnHome.router.closeOverlay(); yes.run(); }), 8);
        VnUi.addSpaced(ov, VnUi.big("✋ CANCEL", VnTheme.MUSTARD, VnTheme.BROWN, v -> VnHome.router.closeOverlay()), 8);
        VnHome.router.overlay(ov);
    }

    public static void confirmCall(String label, String num) {
        LinearLayout ov = VnUi.col();
        ov.setBackground(VnUi.pill(VnTheme.CREAM));
        ov.addView(VnUi.tv("Call: " + label + "?", 22, VnTheme.GREEN, true));
        ov.addView(VnUi.tv("Say «yes» or «no»", 14, VnTheme.BROWN, true));
        VnUi.addSpaced(ov, VnUi.big("✅ YES, CALL", 0xFF3FAE4C, VnTheme.CREAM, v -> { VnHome.router.closeOverlay(); callNumber(num, label); }), 8);
        VnUi.addSpaced(ov, VnUi.big("✋ CANCEL", VnTheme.MUSTARD, VnTheme.BROWN, v -> { VnHome.router.closeOverlay(); VnHome.say("Cancelled."); }), 8);
        VnHome.router.overlay(ov);
        VnHome.say("Call: " + label + "? Say yes or no.");
        startConfirmListen(num, label);
    }
    private static void startConfirmListen(final String num, final String label) {
        VnVoice.listen(null, t -> {
            String s = t.toLowerCase();
            if (s.contains("no") || s.contains("cancel") || s.contains("don't")) { VnHome.router.closeOverlay(); VnHome.say("Cancelled."); }
            else if (s.contains("yes") || s.contains("call") || s.contains("sure") || s.contains("uh-huh")) { VnHome.router.closeOverlay(); callNumber(num, label); }
            else { VnHome.say("Say yes or no."); startConfirmListen(num, label); }
        }, e -> startConfirmListen(num, label));
    }

    public static void callNumber(String num, String label) {
        if (VnUi.C.checkSelfPermission(android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try { VnUi.C.startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + num))); logCall("out", num, label); VnHome.say("Calling: " + label); return; } catch (Exception e) {}
        }
        try { VnUi.C.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + num))); logCall("out", num, label); } catch (Exception e) {}
        VnHome.say("No permission to call. Press the call button or allow calls in settings.");
    }

    public static List<String[]> callJournal() {
        List<String[]> out = new ArrayList<>();
        for (String line : P().getString("callj", "").split("\n")) {
            String[] p = line.split("\\|");
            if (p.length == 4) out.add(p);
        }
        return out;
    }
    public static void logCall(String dir, String num, String name) {
        List<String[]> j = callJournal();
        j.add(new String[]{String.valueOf(System.currentTimeMillis()), dir, num, name});
        while (j.size() > 30) j.remove(0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < j.size(); i++) { if (i > 0) sb.append("\n"); sb.append(String.join("|", j.get(i))); }
        P().edit().putString("callj", sb.toString()).apply();
    }

    public static LinearLayout buildDial() {
        LinearLayout c = VnUi.col();
        LinearLayout tab = VnUi.row();
        final boolean[] hist = {false};
        Button tb1 = VnUi.bigI("dial", "DIAL", VnTheme.GREEN, VnTheme.CREAM, v -> { hist[0] = false; rebuildDial(c, hist); });
        Button tb2 = VnUi.bigI("clock", "HISTORY", VnTheme.MUSTARD, VnTheme.BROWN, v -> { hist[0] = true; rebuildDial(c, hist); });
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tp.setMargins(VnUi.dp(3), 0, VnUi.dp(3), 0);
        tb1.setLayoutParams(tp); tb2.setLayoutParams(tp);
        tab.addView(tb1); tab.addView(tb2);
        VnUi.addSpaced(c, tab, 8);
        rebuildDial(c, hist);
        return c;
    }
    private static void rebuildDial(LinearLayout c, boolean[] hist) {
        for (int i = c.getChildCount() - 1; i >= 1; i--) c.removeViewAt(i);
        if (hist[0]) {
            c.addView(VnUi.tv("CALL HISTORY", 20, VnTheme.GREEN, true));
            boolean any = false;
            try {
                Cursor cur = VnUi.C.getContentResolver().query(CallLog.Calls.CONTENT_URI,
                        new String[]{CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE},
                        null, null, CallLog.Calls.DATE + " DESC LIMIT 15");
                if (cur != null) {
                    while (cur.moveToNext()) {
                        any = true;
                        String num = cur.getString(0); long date = cur.getLong(1); int type = cur.getInt(2);
                        String arrow = type == CallLog.Calls.OUTGOING_TYPE ? "→" : type == CallLog.Calls.MISSED_TYPE ? "✗" : "←";
                        int color = type == CallLog.Calls.MISSED_TYPE ? VnTheme.RUST : VnTheme.BROWN;
                        String name = nameForNumber(num);
                        Calendar cd = Calendar.getInstance(); cd.setTimeInMillis(date);
                        String when = String.format(Locale.getDefault(), "%02d:%02d %02d.%02d", cd.get(Calendar.HOUR_OF_DAY), cd.get(Calendar.MINUTE), cd.get(Calendar.DAY_OF_MONTH), cd.get(Calendar.MONTH) + 1);
                        final String fnum = num;
                        VnUi.addSpaced(c, VnUi.big(arrow + " " + name + "\n" + when, VnTheme.MUSTARD, color, v -> confirmCall(name, fnum)), 8);
                    }
                    cur.close();
                }
            } catch (Exception e) {}
            if (!any) {
                for (int i = callJournal().size() - 1; i >= 0; i--) {
                    final String[] e = callJournal().get(i);
                    String arrow = e[1].equals("out") ? "→" : e[1].equals("miss") ? "✗" : "←";
                    Calendar cd = Calendar.getInstance(); cd.setTimeInMillis(Long.parseLong(e[0]));
                    String when = String.format(Locale.getDefault(), "%02d:%02d %02d.%02d", cd.get(Calendar.HOUR_OF_DAY), cd.get(Calendar.MINUTE), cd.get(Calendar.DAY_OF_MONTH), cd.get(Calendar.MONTH) + 1);
                    VnUi.addSpaced(c, VnUi.big(arrow + " " + e[3] + "\n" + when, VnTheme.MUSTARD, VnTheme.BROWN, v -> confirmCall(e[3], e[2])), 8);
                    any = true;
                }
            }
            if (!any) c.addView(VnUi.tv("No calls yet.", 14, VnTheme.BROWN, true));
            VnUi.addSpaced(c, VnUi.bigI("clock", "SYSTEM CALL HISTORY", VnTheme.MUSTARD, VnTheme.BROWN, v -> {
                try { Intent i = new Intent(Intent.ACTION_VIEW); i.setType(CallLog.Calls.CONTENT_TYPE); VnUi.C.startActivity(i); }
                catch (Exception e) { VnHome.say("Couldn't open system history."); }
            }), 8);
        } else {
            final android.widget.TextView disp = VnUi.tv("", 30, VnTheme.GREEN, true);
            disp.setGravity(Gravity.CENTER);
            disp.setMinHeight(VnUi.dp(60));
            c.addView(disp);
            final StringBuilder cur = new StringBuilder();
            String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"};
            for (int r = 0; r < 4; r++) {
                LinearLayout rw = VnUi.row();
                for (int k = 0; k < 3; k++) {
                    String key = keys[r * 3 + k];
                    Button b = VnUi.big(key, VnTheme.MUSTARD, VnTheme.BROWN, v -> { cur.append(key); disp.setText(cur.toString()); });
                    b.setTextSize(24 * VnUi.FS * VnUi.SC);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                    lp.setMargins(VnUi.dp(4), VnUi.dp(3), VnUi.dp(4), VnUi.dp(3));
                    b.setLayoutParams(lp);
                    rw.addView(b);
                }
                c.addView(rw);
            }
            LinearLayout rw = VnUi.row();
            Button del = VnUi.big("⌫", VnTheme.GRAY, VnTheme.CREAM, v -> { if (cur.length() > 0) cur.deleteCharAt(cur.length() - 1); disp.setText(cur.toString()); });
            Button call = VnUi.bigI("phone", "CALL", 0xFF3FAE4C, VnTheme.CREAM, v -> { if (cur.length() > 0) confirmCall(cur.toString(), cur.toString()); });
            del.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            call.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));
            rw.addView(del); rw.addView(call);
            VnUi.addSpaced(c, rw, 8);
            VnUi.addSpaced(c, VnUi.bigI("sos", "112 EMERGENCY", VnTheme.RUST, VnTheme.CREAM, v -> VnHome.router.show(VnSos.build(), "Emergency call")), 8);
        }
    }
}
