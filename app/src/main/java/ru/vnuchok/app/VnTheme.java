package ru.vnuchok.app;
/* ТЕМА: единая палитра. Цвета изменяемые, чтобы темы менялись на лету. */
public class VnTheme {
    public static int RUST = 0xFFC05227, ORANGE = 0xFFD9772F, MUSTARD = 0xFFD9A02B, SAND = 0xFFE8C15A,
            PINE = 0xFF2E4034, GREEN = 0xFF3E5641, MOSS = 0xFF5A7A4F, OLIVE = 0xFF7A9A5F,
            CREAM = 0xFFF3ECD8, PAPER = 0xFFEFE7D2, BROWN = 0xFF6B4A2F,
            OK = 0xFF3FAE4C, GRAY = 0xFF8A8A8A, ALERT = 0xFFC0392B;
    public static final String[] NAMES = {"РЕТРО", "ГОРЧИЦА", "НОЧЬ", "КАРАМЕЛЬ"};

    public static void apply(int t) {
        switch (t) {
            case 1: GREEN = 0xFFB4531D; CREAM = 0xFFFFFBF0; PAPER = 0xFFF6EEDC; BROWN = 0xFF4A2C17; break;
            case 2: GREEN = 0xFF2C3A30; CREAM = 0xFFE9E4D0; PAPER = 0xFF141A16; RUST = 0xFFB4562E; MUSTARD = 0xFFC99A2E; BROWN = 0xFFD8D2C0; SAND = 0xFFE9E4D0; break;
            case 3: GREEN = 0xFF7A4A21; CREAM = 0xFFFBEFD8; PAPER = 0xFFF3E3C3; BROWN = 0xFF4A2C10; break;
            default: GREEN = 0xFF3E5641; CREAM = 0xFFF3ECD8; PAPER = 0xFFEFE7D2; RUST = 0xFFC05227; MUSTARD = 0xFFD9A02B; BROWN = 0xFF6B4A2F; SAND = 0xFFE8C15A; break;
        }
    }
    public static String hex(int c) { return "#" + Integer.toHexString(c & 0xFFFFFF | 0x1000000).substring(1); }
    public static int shade(int c, float f) { return android.graphics.Color.argb(255, cl((int) (android.graphics.Color.red(c) * f)), cl((int) (android.graphics.Color.green(c) * f)), cl((int) (android.graphics.Color.blue(c) * f))); }
    private static int cl(int v) { return Math.max(0, Math.min(255, v)); }
    public static int fgOn(int bg) { return (bg == MUSTARD || bg == SAND) ? BROWN : CREAM; }
}
