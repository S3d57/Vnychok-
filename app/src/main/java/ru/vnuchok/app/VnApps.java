package ru.vnuchok.app;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/* ПРИЛОЖЕНИЯ: сетка функций оболочки (радио…настройки) + установленные программы. */
public class VnApps {
    public static LinearLayout build() {
        LinearLayout c = VnUi.col();
        int rowH = Math.round(VnUi.dp(110) * VnUi.FS);
        LinearLayout.LayoutParams hp2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        hp2.setMargins(VnUi.dp(4), VnUi.dp(4), VnUi.dp(4), VnUi.dp(4));
        String[][] funcs = {
                {"radio", "Radio"}, {"map", "Map"}, {"music", "Music"}, {"weather", "Weather"},
                {"web", "Internet"}, {"cam", "Photo"}, {"album", "Photo album"}, {"book", "Reading"},
                {"calc", "Calculator"}, {"gear", "Settings"}};
        int[] cols = {VnTheme.GREEN, VnTheme.MUSTARD, VnTheme.RUST, VnTheme.BROWN};
        LinearLayout fr = null;
        for (int i = 0; i < funcs.length; i++) {
            if (i % 2 == 0) { fr = VnUi.row(); fr.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, rowH)); c.addView(fr); }
            final String id = funcs[i][0];
            int colr = cols[i % cols.length];
            Button b = VnUi.tile(id, funcs[i][1], colr, VnTheme.fgOn(colr), v -> open(id));
            b.setLayoutParams(hp2);
            fr.addView(b);
        }
        c.addView(VnUi.tv("INSTALLED APPS", 16, VnTheme.GREEN, true));
        int rowH3 = Math.round(VnUi.dp(90) * VnUi.FS);
        LinearLayout.LayoutParams hp3 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        hp3.setMargins(VnUi.dp(3), VnUi.dp(3), VnUi.dp(3), VnUi.dp(3));
        List<Object[]> apps = installed();
        LinearLayout ar = null; int n = 0;
        for (final Object[] a : apps) {
            if (n % 3 == 0) { ar = VnUi.row(); ar.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, rowH3)); c.addView(ar); }
            Button ab = new Button(VnUi.C);
            ab.setText((String) a[0]);
            ab.setTextSize(10 * VnUi.FS * VnUi.SC); ab.setTextColor(VnTheme.BROWN);
            ab.setBackground(VnUi.pill(VnTheme.MUSTARD));
            VnUi.THolder th = new VnUi.THolder(); th.bg = VnTheme.hex(VnTheme.MUSTARD); ab.setTag(th);
            ab.setElevation(VnUi.dp(4));
            Drawable icn = (Drawable) a[1];
            if (icn != null) { icn.setBounds(0, 0, VnUi.dp(34), VnUi.dp(34)); ab.setCompoundDrawables(null, icn, null, null); }
            ab.setOnClickListener(v -> { VnUi.pressFx(v); openPackage((String) a[2]); });
            ab.setLayoutParams(hp3);
            ar.addView(ab);
            n++;
        }
        if (n == 0) c.addView(VnUi.tv("(no other apps)", 14, VnTheme.GREEN, true));
        return c;
    }

    static void open(String id) {
        switch (id) {
            case "radio": keyword("radio"); break;
            case "map": try { VnUi.C.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0"))); } catch (Exception e) { keyword("map"); } break;
            case "music": keyword("music"); break;
            case "weather": keyword("weather"); break;
            case "web": try { VnUi.C.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))); } catch (Exception e) { keyword("browser"); } break;
            case "cam": try { VnUi.C.startActivity(new Intent(MediaStore.ACTION_IMAGE_CAPTURE)); VnHome.say("Camera opened."); } catch (Exception e) { VnHome.say("Camera didn't open."); } break;
            case "album": try { Intent i = new Intent(Intent.ACTION_VIEW); i.setType("image/*"); VnUi.C.startActivity(i); } catch (Exception e) { VnHome.say("Photo album not found."); } break;
            case "book": VnHome.router.show(VnReader.build(), "Read aloud"); break;
            case "calc": VnHome.router.show(VnCalc.build(), "Calculator"); break;
            case "gear": VnHome.router.show(VnSettings.build(), "Settings"); break;
        }
    }

    static void keyword(String kw) {
        List<ResolveInfo> apps = VnUi.C.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        for (ResolveInfo ri : apps) {
            String label = ri.loadLabel(VnUi.C.getPackageManager()).toString().toLowerCase();
            if (label.contains(kw)) {
                Intent li = VnUi.C.getPackageManager().getLaunchIntentForPackage(ri.activityInfo.packageName);
                if (li != null) { VnUi.C.startActivity(li); VnHome.say("Opening: " + ri.loadLabel(VnUi.C.getPackageManager())); return; }
            }
        }
        VnHome.say("App not found.");
    }

    static List<Object[]> installed() {
        List<Object[]> out = new ArrayList<>();
        String my = VnUi.C.getPackageName();
        List<ResolveInfo> apps = VnUi.C.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        for (ResolveInfo ri : apps) {
            if (ri.activityInfo.packageName.equals(my)) continue;
            String label = ri.loadLabel(VnUi.C.getPackageManager()).toString();
            if (label.length() > 14) label = label.substring(0, 12) + "…";
            try { out.add(new Object[]{label, ri.loadIcon(VnUi.C.getPackageManager()), ri.activityInfo.packageName}); }
            catch (Exception e) { out.add(new Object[]{label, null, ri.activityInfo.packageName}); }
        }
        Collections.sort(out, (a, b) -> ((String) a[0]).compareToIgnoreCase((String) b[0]));
        return out;
    }

    static void openPackage(String pkg) {
        try { Intent li = VnUi.C.getPackageManager().getLaunchIntentForPackage(pkg); if (li != null) VnUi.C.startActivity(li); else VnHome.say("Couldn't open."); }
        catch (Exception e) { VnHome.say("Couldn't open."); }
    }
}
