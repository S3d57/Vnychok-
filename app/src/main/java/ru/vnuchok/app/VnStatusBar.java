package ru.vnuchok.app;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
/* СТАТУС-БАР: батарея / соты / оператор / wifi. Живо реагирует на смену сети. */
public class VnStatusBar {
    public TextView batt, sig, oper, net;
    public interface OnNet { void onNet(boolean wifi, boolean net); }
    public OnNet onNet;
    private ConnectivityManager.NetworkCallback cb;
    private Handler H = new Handler(Looper.getMainLooper());

    public LinearLayout build() {
        LinearLayout r = VnUi.row();
        r.setBackground(VnUi.pill(VnTheme.GREEN));
        r.setPadding(VnUi.dp(14), VnUi.dp(6), VnUi.dp(14), VnUi.dp(6));
        r.setGravity(Gravity.CENTER_VERTICAL);
        batt = VnUi.tv("", 13, VnTheme.CREAM, true);
        sig = VnUi.tv("", 13, VnTheme.CREAM, true);
        oper = VnUi.tv("", 13, VnTheme.CREAM, true);
        oper.setGravity(Gravity.CENTER);
        net = VnUi.tv("", 13, VnTheme.CREAM, true);
        net.setGravity(Gravity.END);
        oper.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(batt); r.addView(sig); r.addView(oper); r.addView(net);
        return r;
    }
    public void updateBattery(int pct, boolean charging) {
        int col = pct <= 20 ? 0xFFC0392B : VnTheme.CREAM;
        batt.setTextColor(col);
        batt.setText(pct + "%");
        VnIcons.Icon bi = VnIcons.icon(charging ? "battc" : "batt", col);
        bi.level = pct; bi.setBounds(0, 0, VnUi.dp(30), VnUi.dp(30));
        batt.setCompoundDrawables(bi, null, null, null);
        batt.setCompoundDrawablePadding(VnUi.dp(4));
        VnUi.blink(batt, charging || pct <= 15);
    }
    public void updateSignal(int bars, String label) {
        sig.setText(label.isEmpty() ? "" : " " + label);
        VnIcons.Icon si = VnIcons.icon("sig", VnTheme.CREAM);
        si.bars = bars; si.setBounds(0, 0, VnUi.dp(26), VnUi.dp(26));
        sig.setCompoundDrawables(si, null, null, null);
    }
    public void updateOperator() {
        String op = "";
        try { TelephonyManager tm = (TelephonyManager) VnUi.C.getSystemService(Context.TELEPHONY_SERVICE); op = tm.getNetworkOperatorName(); } catch (Exception e) {}
        oper.setText(op == null || op.isEmpty() ? "ВНУЧОК" : op);
    }
    public void updateNet(boolean wifi, boolean netOn) {
        net.setText("");
        VnIcons.Icon wi = VnIcons.icon(wifi ? "wifi" : (netOn ? "sig" : "x"), VnTheme.CREAM);
        if (!wifi && netOn) wi.bars = 3;
        wi.setBounds(0, 0, VnUi.dp(26), VnUi.dp(26));
        net.setCompoundDrawables(wi, null, null, null);
    }
    public void watchNet(final Context c) {
        try {
            final ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
            cb = new ConnectivityManager.NetworkCallback() {
                @Override public void onAvailable(Network n) { push(cm); }
                @Override public void onLost(Network n) { push(cm); }
                @Override public void onCapabilitiesChanged(Network n, NetworkCapabilities cap) { push(cm); }
            };
            cm.registerDefaultNetworkCallback(cb);
        } catch (Exception e) {}
    }
    private void push(final ConnectivityManager cm) {
        final boolean[] r = probe(cm);
        H.post(() -> { updateNet(r[0], r[1]); if (onNet != null) onNet.onNet(r[0], r[1]); });
    }
    public static boolean[] probe(ConnectivityManager cm) {
        boolean net = false, wifi = false;
        try {
            Network n = cm.getActiveNetwork();
            if (n != null) { net = true; NetworkCapabilities nc = cm.getNetworkCapabilities(n); wifi = nc != null && nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI); }
        } catch (Exception e) {}
        return new boolean[]{wifi, net};
    }
    public void unwatch(Context c) {
        try { if (cb != null) ((ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE)).unregisterNetworkCallback(cb); } catch (Exception e) {}
        cb = null;
    }
}
