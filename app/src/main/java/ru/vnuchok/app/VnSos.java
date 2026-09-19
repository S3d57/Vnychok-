package ru.vnuchok.app;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import java.util.Locale;
/* SOS: отсчёт 5 секунд с отменой, затем звонок 112 и СМС родным с координатами. */
public class VnSos {
    private static Handler H = new Handler(Looper.getMainLooper());
    private static Runnable ticker;
    private static volatile boolean active;

    public static LinearLayout build() {
        LinearLayout c = VnUi.col();
        c.addView(VnUi.tv("CALLING FOR HELP!", 26, VnTheme.RUST, true));
        final TextView num = VnUi.tv("5", 80, VnTheme.RUST, true);
        c.addView(num);
        c.addView(VnUi.tv("If by accident — press CANCEL", 16, VnTheme.GREEN, true));
        VnUi.addSpaced(c, VnUi.big("✋ CANCEL", VnTheme.GRAY, VnTheme.CREAM, v -> cancel()), 8);
        VnHome.say("Attention! Calling for help in five seconds. If by accident — cancel.");
        active = true;
        final int[] n = {5};
        ticker = new Runnable() { public void run() {
            if (!active) return;
            n[0]--;
            if (n[0] <= 0) { fire(); return; }
            num.setText(String.valueOf(n[0]));
            H.postDelayed(this, 1000);
        } };
        H.postDelayed(ticker, 1000);
        return c;
    }

    public static void cancel() {
        active = false;
        if (ticker != null) H.removeCallbacks(ticker);
        VnHome.say("Cancelled. All good, help not called.");
        VnHome.router.home();
    }

    private static void fire() {
        active = false;
        String loc = loc();
        String sms = "SOS! Urgent help needed! " + loc;
        List<String[]> cs = VnCalls.contacts();
        for (int i = 0; i < 3 && i < cs.size(); i++) sendSms(cs.get(i)[1], sms);
        boolean called = false;
        if (VnUi.C.checkSelfPermission(android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try { VnUi.C.startActivity(new Intent(Intent.ACTION_CALL, Uri.parse("tel:112"))); called = true; } catch (Exception e) {}
        }
        if (!called) {
            try { VnUi.C.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))); } catch (Exception e) {}
            VnHome.say("Phone didn't get permission for direct call. Press the green call button.");
        }
        LinearLayout c = VnUi.col();
        c.addView(VnUi.tv("☎ CALLING 112…", 24, VnTheme.RUST, true));
        c.addView(VnUi.tv("SMS sent to relatives:", 16, VnTheme.GREEN, true));
        c.addView(VnUi.tv(sms, 14, VnTheme.GREEN, true));
        VnHome.router.show(c, "Call to 112");
        VnHome.say("Calling one-twelve and sending message to relatives!");
    }

    private static void sendSms(String num, String text) {
        try { SmsManager.getDefault().sendTextMessage(num, null, text, null, null); } catch (Exception e) {}
    }

    private static String loc() {
        try {
            android.location.LocationManager lm = (android.location.LocationManager) VnUi.C.getSystemService(Context.LOCATION_SERVICE);
            android.location.Location l = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
            if (l == null) l = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
            if (l == null) return "Can't catch satellites, exact location unknown.";
            return String.format(Locale.US, "Lat: %.6f Lon: %.6f https://maps.google.com/?q=%.6f,%.6f", l.getLatitude(), l.getLongitude(), l.getLatitude(), l.getLongitude());
        } catch (Exception e) { return "Can't catch satellites, exact location unknown."; }
    }
}
