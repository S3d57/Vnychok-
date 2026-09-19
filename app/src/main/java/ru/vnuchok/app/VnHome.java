package ru.vnuchok.app;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
/* ГЛАВНЫЙ ЭКРАН: сцена фоном, пилюли поверх, голосовая карточка-морфинг, закреплённые плитки. */
public class VnHome {
    public interface Router { void show(View v, String title); void home(); void overlay(View v); void closeOverlay(); }
    public static Router router;
    public static VnHome inst;
    public interface Nav { void tile(String id); void sos(); void mic(); void close(); }

    public VnStatusBar sb = new VnStatusBar();
    public VnClockWeather cw = new VnClockWeather();
    public LinearLayout voiceCard, idleRow, dlgCol;
    public TextView userSay, caption;
    public Button msgTile, callTile;
    private Nav nav;

    public static void say(String t) {
        if (inst != null) inst.saySelf(t); else VnVoice.speak(t);
    }
    public void saySelf(String t) {
        if (caption != null) caption.setText(t);
        VnVoice.speak(t);
    }

    public static void toggleTorch() {
        try {
            CameraManager cm = (CameraManager) VnUi.C.getSystemService(Context.CAMERA_SERVICE);
            String[] ids = cm.getCameraIdList();
            boolean on = !VnUi.C.getSharedPreferences("vnuchok", Context.MODE_PRIVATE).getBoolean("torch", false);
            VnUi.C.getSharedPreferences("vnuchok", Context.MODE_PRIVATE).edit().putBoolean("torch", on).apply();
            cm.setTorchMode(ids[0], on);
            say(on ? "Flashlight on." : "Flashlight off.");
        } catch (Exception e) { say("Flashlight won't turn on."); }
    }

    public LinearLayout build(Nav nav) {
        this.nav = nav;
        inst = this;
        LinearLayout c = VnUi.col();
        c.setPadding(VnUi.dp(8), VnUi.dp(6), VnUi.dp(8), VnUi.dp(2));

        // scene as background of the top zone, pills on top of it
        FrameLayout topZone = new FrameLayout(VnUi.C);
        View scene = VnBg.scene(VnUi.C);
        topZone.addView(scene, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, VnUi.dp(300)));
        LinearLayout pillCol = VnUi.col();
        pillCol.setPadding(0, 0, 0, 0);
        pillCol.addView(sb.build(), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        pillCol.addView(cw.build(), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        topZone.addView(pillCol, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        c.addView(topZone, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // voice card edge to edge with morph
        voiceCard = new LinearLayout(VnUi.C);
        voiceCard.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable vcg = new GradientDrawable();
        vcg.setColor(VnTheme.MUSTARD); vcg.setCornerRadius(VnUi.dp(24)); vcg.setStroke(VnUi.dp(4), VnTheme.RUST);
        voiceCard.setBackground(vcg);
        voiceCard.setPadding(VnUi.dp(12), VnUi.dp(10), VnUi.dp(12), VnUi.dp(10));

        idleRow = VnUi.row();
        idleRow.setGravity(Gravity.CENTER_VERTICAL);
        idleRow.setPadding(0, 0, 0, 0);
        Button micBtn = new Button(VnUi.C);
        micBtn.setBackground(VnUi.pill(VnTheme.GREEN));
        micBtn.setTag(null);
        Drawable mic = VnIcons.icon("mic", VnTheme.CREAM);
        mic.setBounds(0, 0, VnUi.dp(34), VnUi.dp(34));
        micBtn.setCompoundDrawables(mic, null, null, null);
        micBtn.setLayoutParams(new LinearLayout.LayoutParams(VnUi.dp(64), VnUi.dp(64)));
        micBtn.setOnClickListener(v -> { VnUi.pressFx(v); nav.mic(); });
        LinearLayout tcol = VnUi.col();
        tcol.setPadding(VnUi.dp(10), 0, 0, 0);
        tcol.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        tcol.addView(VnUi.tv("Talk to me", 18, VnTheme.BROWN, true));
        tcol.addView(VnUi.tv("Press and speak, I'll help", 12, VnTheme.BROWN, false));
        idleRow.addView(micBtn);
        idleRow.addView(tcol, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        voiceCard.addView(idleRow);

        dlgCol = VnUi.col();
        dlgCol.setPadding(0, 0, 0, 0);
        dlgCol.setGravity(Gravity.START);
        dlgCol.setVisibility(View.GONE);
        LinearLayout drow = VnUi.row();
        drow.setGravity(Gravity.CENTER_VERTICAL);
        drow.setPadding(0, 0, 0, 0);
        Button dlgMic = new Button(VnUi.C);
        dlgMic.setBackground(VnUi.pill(VnTheme.GREEN));
        dlgMic.setTag(null);
        Drawable dmic = VnIcons.icon("mic", VnTheme.CREAM);
        dmic.setBounds(0, 0, VnUi.dp(28), VnUi.dp(28));
        dlgMic.setCompoundDrawables(dmic, null, null, null);
        dlgMic.setLayoutParams(new LinearLayout.LayoutParams(VnUi.dp(48), VnUi.dp(48)));
        dlgMic.setOnClickListener(v -> { VnUi.pressFx(v); nav.mic(); });
        userSay = VnUi.tv("YOU: …", 13, VnTheme.BROWN, true);
        userSay.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button dlgClose = new Button(VnUi.C);
        dlgClose.setBackground(VnUi.pill(VnTheme.CREAM));
        dlgClose.setTag(null);
        Drawable xic = VnIcons.icon("x", VnTheme.GREEN);
        xic.setBounds(0, 0, VnUi.dp(22), VnUi.dp(22));
        dlgClose.setCompoundDrawables(xic, null, null, null);
        dlgClose.setLayoutParams(new LinearLayout.LayoutParams(VnUi.dp(40), VnUi.dp(40)));
        dlgClose.setOnClickListener(v -> { VnUi.pressFx(v); nav.close(); });
        drow.addView(dlgMic); drow.addView(userSay); drow.addView(dlgClose);
        dlgCol.addView(drow);
        caption = VnUi.tv("GRANDSON: «Hello!»", 15, VnTheme.BROWN, true);
        dlgCol.addView(caption);
        voiceCard.addView(dlgCol);
        LinearLayout.LayoutParams vcp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        vcp.topMargin = VnUi.dp(6); vcp.bottomMargin = VnUi.dp(6);
        c.addView(voiceCard, vcp);

        // fixed tiles fill the rest, no scrolling
        LinearLayout tilesBox = VnUi.col();
        tilesBox.setPadding(0, 0, 0, 0);
        int[] cols = {VnTheme.GREEN, VnTheme.MUSTARD, VnTheme.RUST, VnTheme.BROWN, VnTheme.GREEN, VnTheme.MUSTARD};
        List<String> vis = visibleTiles();
        LinearLayout curRow = null; int inRow = 0;
        for (int i = 0; i < vis.size(); i++) {
            if (inRow == 0) {
                curRow = VnUi.row();
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
                rlp.bottomMargin = VnUi.dp(6);
                tilesBox.addView(curRow, rlp);
            }
            Button b = tileFor(vis.get(i), cols[i % cols.length]);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            lp.setMargins(VnUi.dp(3), 0, VnUi.dp(3), 0);
            b.setLayoutParams(lp);
            curRow.addView(b);
            inRow++; if (inRow == 3) inRow = 0;
        }
        c.addView(tilesBox, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        return c;
    }

    public void openDialog() {
        idleRow.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            idleRow.setVisibility(View.GONE);
            dlgCol.setVisibility(View.VISIBLE);
            dlgCol.setAlpha(0f);
            dlgCol.animate().alpha(1f).setDuration(250);
        });
    }
    public void closeDialog() {
        VnVoice.stopListen();
        dlgCol.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            dlgCol.setVisibility(View.GONE);
            idleRow.setVisibility(View.VISIBLE);
            idleRow.setAlpha(0f);
            idleRow.animate().alpha(1f).setDuration(250);
        });
    }

    public static List<String> visibleTiles() {
        SharedPreferences P = VnUi.C.getSharedPreferences("vnuchok", Context.MODE_PRIVATE);
        List<String> all = new ArrayList<>();
        for (String s : P.getString("tiles", "call,sms,apps,rem,alarm,torch").split(",")) if (!s.isEmpty()) all.add(s);
        String off = P.getString("tilesOff", "");
        List<String> vis = new ArrayList<>();
        for (String s : all) if (!off.contains(s)) vis.add(s);
        return vis;
    }

    private Button tileFor(String id, int color) {
        int fg = VnTheme.fgOn(color);
        switch (id) {
            case "call": callTile = VnUi.tile("phone", "Call", color, fg, v -> VnHome.router.show(VnCalls.buildContacts(true), "Who to call")); return callTile;
            case "sms": msgTile = VnUi.tile("mail", "Messages", color, fg, v -> { VnUi.blink(msgTile, false); VnHome.router.show(VnSms.buildList(), "Messages"); }); return msgTile;
            case "apps": return VnUi.tile("folder", "Apps", color, fg, v -> VnHome.router.show(VnApps.build(), "Apps"));
            case "rem": return VnUi.tile("memo", "Reminders", color, fg, v -> VnHome.router.show(VnRemind.build(), "Reminders"));
            case "alarm": return VnUi.tile("alarm", "Alarm", color, fg, v -> VnHome.router.show(VnAlarm.build(), "Alarm"));
            default: return VnUi.tile("torch", "Flashlight", color, fg, v -> toggleTorch());
        }
    }
}
