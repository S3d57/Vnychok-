package ru.vnuchok.app;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
/* СООБЩЕНИЯ: входящие список, карточка сообщения, ответ с диктовкой и статусом доставки. */
public class VnSms {
    private static SharedPreferences P() { return VnUi.C.getSharedPreferences("vnuchok", Context.MODE_PRIVATE); }
    private static boolean has(String key, String id) {
        for (String s : P().getString(key, "").split("\n")) if (s.equals(id)) return true;
        return false;
    }
    private static void add(String key, String id) {
        String cur = P().getString(key, "");
        P().edit().putString(key, cur.isEmpty() ? id : cur + "\n" + id).apply();
    }

    public static LinearLayout buildList() {
        LinearLayout c = VnUi.col();
        VnUi.addSpaced(c, VnUi.bigI("mail", "WRITE NEW", 0xFF3FAE4C, VnTheme.CREAM, v -> VnHome.router.show(VnCalls.buildContacts(false), "Who to write")), 10);
        boolean any = false;
        try {
            Cursor cur = VnUi.C.getContentResolver().query(Uri.parse("content://sms/inbox"),
                    new String[]{"_id", "address", "body", "read"}, null, null, "date DESC LIMIT 20");
            if (cur != null) {
                while (cur.moveToNext()) {
                    String id = cur.getString(0);
                    if (has("smsHidden", id)) continue;
                    any = true;
                    String addr = cur.getString(1);
                    String body = cur.getString(2);
                    boolean read = has("smsRead", id) || "1".equals(cur.getString(3));
                    String shortB = body.length() > 70 ? body.substring(0, 70) + "…" : body;
                    final String fid = id, faddr = addr, fbody = body;
                    Button b = VnUi.bigI("mail", (read ? "" : "● ") + "From: " + VnCalls.nameForNumber(addr) + "\n" + shortB + (read ? "\n(read)" : ""), VnTheme.MUSTARD, VnTheme.BROWN, v -> VnHome.router.show(buildDetail(fid, faddr, fbody), "Message"));
                    b.setTextSize(14 * VnUi.FS * VnUi.SC);
                    if (read) b.setAlpha(0.75f);
                    VnUi.addSpaced(c, b, 10);
                }
                cur.close();
            }
        } catch (Exception e) {}
        if (!any) c.addView(VnUi.tv("No messages yet or no SMS access.", 14, VnTheme.BROWN, true));
        return c;
    }

    public static LinearLayout buildDetail(String id, String addr, String body) {
        LinearLayout c = VnUi.col();
        LinearLayout box = VnUi.col();
        GradientDrawable cg = new GradientDrawable();
        cg.setColor(VnTheme.CREAM); cg.setCornerRadius(VnUi.dp(18)); cg.setStroke(VnUi.dp(3), VnTheme.RUST);
        box.setBackground(cg); box.setPadding(VnUi.dp(14), VnUi.dp(12), VnUi.dp(14), VnUi.dp(12));
        box.setGravity(android.view.Gravity.START);
        box.addView(VnUi.tv("From: " + VnCalls.nameForNumber(addr), 14, VnTheme.BROWN, true));
        box.addView(VnUi.tv(body, 18, VnTheme.GREEN, true));
        VnUi.addSpaced(c, box, 10);
        VnUi.addSpaced(c, VnUi.bigI("sound", "READ ALOUD", VnTheme.MUSTARD, VnTheme.BROWN, v -> VnVoice.speak(body)), 8);
        VnUi.addSpaced(c, VnUi.bigI("mail", "REPLY", 0xFF3FAE4C, VnTheme.CREAM, v -> VnHome.router.show(buildCompose(VnCalls.nameForNumber(addr), addr), "Writing: " + VnCalls.nameForNumber(addr))), 8);
        VnUi.addSpaced(c, VnUi.bigI("clock", "MARK AS READ", VnTheme.MUSTARD, VnTheme.BROWN, v -> { add("smsRead", id); VnHome.say("Marked as read."); VnHome.router.show(buildList(), "Messages"); }), 8);
        VnUi.addSpaced(c, VnUi.bigI("x", "DELETE", VnTheme.RUST, VnTheme.CREAM, v -> VnCalls.confirmDialog("Delete message?", () -> { add("smsHidden", id); VnHome.say("Message removed from list."); VnHome.router.show(buildList(), "Messages"); })), 8);
        return c;
    }

    public static LinearLayout buildCompose(String name, String num) {
        LinearLayout c = VnUi.col();
        LinearLayout box = VnUi.col();
        GradientDrawable cg = new GradientDrawable();
        cg.setColor(VnTheme.CREAM); cg.setCornerRadius(VnUi.dp(18)); cg.setStroke(VnUi.dp(4), VnTheme.RUST);
        box.setBackground(cg); box.setPadding(VnUi.dp(14), VnUi.dp(12), VnUi.dp(14), VnUi.dp(12));
        final EditText et = new EditText(VnUi.C);
        et.setTextSize(20 * VnUi.FS * VnUi.SC); et.setMinLines(4);
        et.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        et.setTextColor(VnTheme.GREEN);
        et.setHint("Speak the text — I'll write it down");
        et.setHintTextColor(VnTheme.BROWN);
        box.addView(et);
        VnUi.addSpaced(c, box, 6);
        final TextView status = VnUi.tv("Status: —", 14, VnTheme.BROWN, true);
        VnUi.addSpaced(c, status, 8);
        VnUi.addSpaced(c, VnUi.bigI("sound", "READ ALOUD", VnTheme.MUSTARD, VnTheme.BROWN, v -> VnVoice.speak(et.getText().toString())), 8);
        VnUi.addSpaced(c, VnUi.bigI("mail", "SEND", 0xFF3FAE4C, VnTheme.CREAM, v -> { VnVoice.stopListen(); send(num, et.getText().toString(), status); VnHome.say("Sent: " + name); VnHome.router.home(); }), 8);
        VnUi.addSpaced(c, VnUi.big("CANCEL", VnTheme.MUSTARD, VnTheme.BROWN, v -> { VnVoice.stopListen(); VnHome.router.home(); }), 8);
        VnHome.say("Writing to: " + name + ". Speak the text, I'll write it down.");
        dictate(et, num, status);
        return c;
    }
    private static void dictate(final EditText et, final String num, final TextView status) {
        VnVoice.listen(null, t -> {
            String tl = t.toLowerCase();
            if (tl.contains("send")) { send(num, et.getText().toString(), status); VnHome.router.home(); return; }
            if (tl.contains("cancel")) { VnHome.router.home(); return; }
            if (tl.contains("read aloud")) { VnVoice.speak(et.getText().toString()); }
            else if (!t.isEmpty()) {
                String old = et.getText().toString();
                et.setText(old.isEmpty() ? t : old + " " + t);
                et.setSelection(et.getText().length());
            }
            dictate(et, num, status);
        }, e -> dictate(et, num, status));
    }
    private static void send(String num, String text, TextView status) {
        try {
            SmsManager sm = SmsManager.getDefault();
            PendingIntent si = PendingIntent.getBroadcast(VnUi.C, 1001, new Intent("ru.vnuchok.SMS_SENT"), PendingIntent.FLAG_IMMUTABLE);
            PendingIntent di = PendingIntent.getBroadcast(VnUi.C, 1002, new Intent("ru.vnuchok.SMS_DELIVERED"), PendingIntent.FLAG_IMMUTABLE);
            sm.sendTextMessage(num, null, text, si, di);
            if (status != null) status.setText("Status: sent…");
        } catch (Exception e) { if (status != null) status.setText("Status: send error"); VnHome.say("SMS didn't go."); }
    }
}
