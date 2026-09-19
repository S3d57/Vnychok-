package ru.vnuchok.app;
import android.content.Intent;
import android.net.Uri;
import android.widget.EditText;
import android.widget.LinearLayout;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
/* ЧИТАЛКА: читает книги голосом: файл книги или вставленный текст, пауза/стоп/продолжить. */
public class VnReader {
    private static List<String> sents;
    private static int idx = 0;
    private static boolean playing = false;
    private static String bookText = null;

    public static LinearLayout build() {
        LinearLayout c = VnUi.col();
        c.addView(VnUi.tv("BOOKS FOR THE VISUALLY IMPAIRED", 20, VnTheme.GREEN, true));
        c.addView(VnUi.tv("Vnuchok will read the book aloud", 14, VnTheme.BROWN, true));
        VnUi.addSpaced(c, VnUi.bigI("book", "OPEN BOOK FILE", 0xFF3FAE4C, VnTheme.CREAM, v -> {
            try {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.setType("text/*");
                i.addCategory(Intent.CATEGORY_OPENABLE);
                ((android.app.Activity) VnUi.C).startActivityForResult(i, 42);
            } catch (Exception e) { VnHome.say("Couldn't open file picker."); }
        }), 8);
        VnUi.addSpaced(c, VnUi.bigI("memo", "PASTE TEXT", VnTheme.MUSTARD, VnTheme.BROWN, v -> VnHome.router.show(buildPaste(), "Paste text")), 8);
        if (bookText != null) {
            String preview = bookText.length() > 160 ? bookText.substring(0, 160) + "…" : bookText;
            c.addView(VnUi.tv("Loaded: " + preview, 13, VnTheme.GREEN, false));
            VnUi.addSpaced(c, VnUi.bigI("sound", playing ? "PAUSE" : "READ ALOUD", VnTheme.MUSTARD, VnTheme.BROWN, v -> {
                if (playing) { playing = false; VnVoice.stopBook(); VnHome.say("Pause."); }
                else startBook();
            }), 8);
            VnUi.addSpaced(c, VnUi.bigI("x", "STOP AND FORGET", VnTheme.RUST, VnTheme.CREAM, v -> {
                playing = false; VnVoice.stopBook(); bookText = null; sents = null;
                VnHome.say("Stopped reading.");
                VnHome.router.show(build(), "Read aloud");
            }), 8);
        }
        return c;
    }

    public static LinearLayout buildPaste() {
        LinearLayout c = VnUi.col();
        final EditText et = new EditText(VnUi.C);
        et.setTextSize(16 * VnUi.FS * VnUi.SC); et.setMinLines(6);
        et.setHint("Paste or type text");
        c.addView(et);
        VnUi.addSpaced(c, VnUi.bigI("sound", "START READING", 0xFF3FAE4C, VnTheme.CREAM, v -> {
            bookText = et.getText().toString();
            prepare();
            startBook();
        }), 8);
        return c;
    }

    /* вызывается из MainActivity.onActivityResult при коде 42 */
    public static void load(Intent data) {
        if (data == null || data.getData() == null) return;
        final Uri uri = data.getData();
        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            try {
                InputStream in = VnUi.C.getContentResolver().openInputStream(uri);
                BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                String line;
                while ((line = br.readLine()) != null && sb.length() < 1500000) sb.append(line).append('\n');
                br.close();
            } catch (Exception e) { }
            final String txt = sb.toString();
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (txt.isEmpty()) { VnHome.say("File is empty or unreadable."); return; }
                bookText = txt;
                prepare();
                VnHome.say("Book loaded. Press read aloud.");
                VnHome.router.show(build(), "Read aloud");
            });
        }).start();
    }

    private static void prepare() {
        sents = new ArrayList<>();
        if (bookText == null) return;
        for (String p : bookText.split("(?<=[.!?…])\\s+|\\n+")) if (!p.trim().isEmpty()) sents.add(p.trim());
        idx = 0;
    }

    private static void startBook() {
        if (sents == null) prepare();
        if (sents == null || sents.isEmpty()) { VnHome.say("Text is empty."); return; }
        playing = true;
        VnVoice.bookDone = t -> {
            if (t == null) { playing = false; return; }
            if (playing) nextSent();
        };
        VnHome.say("Starting reading.");
        nextSent();
    }

    private static void nextSent() {
        if (!playing || sents == null) return;
        if (idx >= sents.size()) { playing = false; VnHome.say("Book read to the end."); VnHome.router.show(build(), "Read aloud"); return; }
        VnVoice.speakBook(sents.get(idx++));
    }
}
