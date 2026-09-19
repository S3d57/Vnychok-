package ru.vnuchok.app;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
/* ЗНАЧКИ: единая сетка 100x100, скруглённые колпачки, стиль ретро-референса. */
public class VnIcons {
    public static class Icon extends Drawable {
        private final String k;
        private final Paint f = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint s = new Paint(Paint.ANTI_ALIAS_FLAG);
        public int level = -1, bars = -1;
        Icon(String k, int col) {
            this.k = k;
            f.setColor(col); f.setStyle(Paint.Style.FILL);
            s.setColor(col); s.setStyle(Paint.Style.STROKE);
            s.setStrokeWidth(8f); s.setStrokeCap(Paint.Cap.ROUND); s.setStrokeJoin(Paint.Join.ROUND);
        }
        @Override public void draw(Canvas c) {
            Rect b = getBounds();
            c.save(); c.translate(b.left, b.top);
            c.scale(b.width() / 100f, b.height() / 100f);
            switch (k) {
                case "phone": { c.save(); c.rotate(-45, 50, 50); c.drawRoundRect(24, 40, 76, 60, 10, 10, f); c.drawRoundRect(14, 26, 34, 60, 10, 10, f); c.drawRoundRect(66, 26, 86, 60, 10, 10, f); c.restore(); break; }
                case "mail": { c.drawRoundRect(8, 22, 92, 78, 10, 10, s); Path fl = new Path(); fl.moveTo(12, 26); fl.lineTo(50, 54); fl.lineTo(88, 26); fl.lineTo(88, 34); fl.lineTo(50, 62); fl.lineTo(12, 34); fl.close(); c.drawPath(fl, f); break; }
                case "dial": { for (int i = 0; i < 9; i++) c.drawCircle(26 + (i % 3) * 24, 26 + (i / 3) * 24, 8.5f, f); break; }
                case "cam": { c.drawRoundRect(6, 30, 94, 80, 12, 12, s); c.drawCircle(50, 55, 14, s); c.drawCircle(50, 55, 5, f); c.drawRoundRect(34, 18, 58, 30, 6, 6, f); c.drawCircle(80, 42, 4, f); break; }
                case "album": { c.drawRoundRect(8, 14, 92, 86, 12, 12, s); Path m = new Path(); m.moveTo(18, 74); m.quadTo(30, 50, 40, 62); m.quadTo(50, 72, 58, 56); m.quadTo(66, 44, 74, 60); m.lineTo(82, 74); m.close(); c.drawPath(m, f); c.drawCircle(70, 32, 7, f); break; }
                case "memo": { c.drawRoundRect(14, 10, 86, 90, 12, 12, s); c.drawLine(30, 34, 70, 34, s); c.drawLine(30, 50, 70, 50, s); c.drawLine(30, 66, 58, 66, s); break; }
                case "alarm": { c.drawCircle(50, 50, 30, s); c.drawLine(50, 50, 50, 32, s); c.drawLine(50, 50, 63, 55, s); c.drawCircle(50, 50, 4, f); break; }
                case "clock": { c.drawCircle(50, 50, 32, s); c.drawLine(50, 50, 50, 30, s); c.drawLine(50, 50, 64, 56, s); c.drawCircle(50, 50, 4, f); break; }
                case "torch": { Path h = new Path(); h.moveTo(32, 22); h.lineTo(68, 22); h.lineTo(60, 44); h.lineTo(40, 44); h.close(); c.drawPath(h, f); c.drawRoundRect(40, 44, 60, 88, 9, 9, f); c.drawLine(50, 6, 50, 14, s); c.drawLine(28, 10, 34, 16, s); c.drawLine(72, 10, 66, 16, s); break; }
                case "wrench": { s.setStrokeWidth(11f); c.drawArc(16, 16, 48, 48, 60, 240, false, s); c.save(); c.rotate(45, 50, 50); c.drawRoundRect(40, 44, 90, 58, 7, 7, f); c.restore(); break; }
                case "mic": { c.drawRoundRect(38, 6, 62, 48, 12, 12, f); c.drawArc(26, 26, 74, 74, 0, 180, false, s); c.drawLine(50, 74, 50, 86, s); c.drawRoundRect(34, 86, 66, 94, 4, 4, f); break; }
                case "sos": { c.drawRoundRect(40, 14, 60, 86, 8, 8, f); c.drawRoundRect(14, 40, 86, 60, 8, 8, f); break; }
                case "radio": { c.drawRoundRect(8, 34, 92, 84, 12, 12, s); c.drawCircle(30, 59, 12, s); c.drawCircle(30, 59, 4, f); c.drawLine(54, 50, 82, 50, s); c.drawLine(54, 62, 82, 62, s); c.drawLine(54, 74, 72, 74, s); c.drawLine(64, 34, 88, 10, s); break; }
                case "map": { Path p = new Path(); p.moveTo(12, 24); p.lineTo(38, 14); p.lineTo(62, 24); p.lineTo(88, 14); p.lineTo(88, 76); p.lineTo(62, 86); p.lineTo(38, 76); p.lineTo(12, 86); p.close(); c.drawPath(p, s); c.drawLine(38, 14, 38, 76, s); c.drawLine(62, 24, 62, 86, s); c.drawCircle(50, 46, 7, f); break; }
                case "music": { c.drawCircle(38, 74, 14, f); c.drawRect(50, 22, 56, 74, f); Path fl = new Path(); fl.moveTo(56, 22); fl.quadTo(80, 30, 72, 54); fl.quadTo(70, 38, 56, 34); fl.close(); c.drawPath(fl, f); break; }
                case "weather": { c.drawCircle(38, 34, 13, f); for (int i = 0; i < 8; i++) { c.save(); c.rotate(i * 45f, 38, 34); c.drawLine(38, 14, 38, 8, s); c.restore(); } c.drawCircle(56, 62, 14, f); c.drawCircle(72, 66, 11, f); c.drawRoundRect(42, 62, 86, 77, 8, 8, f); break; }
                case "sun": { c.drawCircle(50, 50, 16, f); for (int i = 0; i < 8; i++) { c.save(); c.rotate(i * 45f, 50, 50); c.drawLine(50, 26, 50, 16, s); c.restore(); } break; }
                case "web": { c.drawCircle(50, 50, 34, s); c.drawOval(new RectF(34, 16, 66, 84), s); c.drawLine(16, 50, 84, 50, s); c.drawArc(22, 28, 78, 72, 200, 140, false, s); c.drawArc(22, 28, 78, 72, 20, 140, false, s); break; }
                case "wifi": { s.setStrokeWidth(9f); c.drawArc(14, 26, 86, 98, 210, 120, false, s); c.drawArc(28, 40, 72, 84, 210, 120, false, s); c.drawArc(40, 52, 60, 72, 210, 120, false, s); c.drawCircle(50, 76, 7, f); break; }
                case "folder": { c.drawRoundRect(10, 24, 90, 82, 10, 10, f); c.drawRoundRect(10, 16, 44, 34, 8, 8, f); c.drawRoundRect(18, 44, 82, 74, 8, 8, s); break; }
                case "sound": { Path p = new Path(); p.moveTo(14, 40); p.lineTo(32, 40); p.lineTo(52, 22); p.lineTo(52, 78); p.lineTo(32, 60); p.lineTo(14, 60); p.close(); c.drawPath(p, f); c.drawArc(58, 32, 84, 68, -45, 90, false, s); c.drawArc(68, 22, 98, 78, -45, 90, false, s); break; }
                case "silent": { Path p = new Path(); p.moveTo(14, 40); p.lineTo(32, 40); p.lineTo(52, 22); p.lineTo(52, 78); p.lineTo(32, 60); p.lineTo(14, 60); p.close(); c.drawPath(p, f); c.drawLine(64, 36, 90, 64, s); c.drawLine(90, 36, 64, 64, s); break; }
                case "gear": { for (int i = 0; i < 8; i++) { c.save(); c.rotate(i * 45f, 50, 50); c.drawRoundRect(43, 10, 57, 26, 5, 5, f); c.restore(); } s.setStrokeWidth(13f); c.drawCircle(50, 50, 22, s); c.drawCircle(50, 50, 7, f); break; }
                case "batt": { float lv = level < 0 ? 50 : level; s.setStrokeWidth(7); c.drawRoundRect(4, 26, 82, 74, 12, 12, s); c.drawRoundRect(86, 40, 96, 60, 4, 4, f); float fw = Math.max(6, 66 * lv / 100f); c.drawRoundRect(11, 33, 11 + fw, 67, 6, 6, f); break; }
                case "battc": { s.setStrokeWidth(7); c.drawRoundRect(4, 26, 82, 74, 12, 12, s); c.drawRoundRect(86, 40, 96, 60, 4, 4, f); Path bo = new Path(); bo.moveTo(50, 30); bo.lineTo(34, 52); bo.lineTo(44, 52); bo.lineTo(38, 70); bo.lineTo(58, 46); bo.lineTo(47, 46); bo.close(); c.drawPath(bo, f); break; }
                case "sig": { for (int i = 0; i < 4; i++) { float bh = 25 + i * 20; boolean on = bars < 0 || i < bars; c.drawRoundRect(10 + i * 24, 92 - bh, 26 + i * 24, 92, 5, 5, on ? f : s); } break; }
                case "home": { Path p = new Path(); p.moveTo(50, 12); p.lineTo(90, 48); p.lineTo(78, 48); p.lineTo(78, 88); p.lineTo(58, 88); p.lineTo(58, 62); p.lineTo(42, 62); p.lineTo(42, 88); p.lineTo(22, 88); p.lineTo(22, 48); p.lineTo(10, 48); p.close(); c.drawPath(p, f); break; }
                case "people": { c.drawCircle(38, 30, 13, f); c.drawRoundRect(18, 48, 58, 86, 16, 16, f); c.drawCircle(68, 36, 10, f); c.drawRoundRect(52, 52, 86, 86, 13, 13, f); break; }
                case "back": { s.setStrokeWidth(10f); c.drawLine(72, 50, 32, 50, s); c.drawLine(46, 34, 30, 50, s); c.drawLine(46, 66, 30, 50, s); break; }
                case "book": { Path bl = new Path(); bl.moveTo(50, 22); bl.quadTo(30, 12, 12, 20); bl.lineTo(12, 78); bl.quadTo(30, 70, 50, 80); bl.close(); c.drawPath(bl, s); Path br = new Path(); br.moveTo(50, 22); br.quadTo(70, 12, 88, 20); br.lineTo(88, 78); br.quadTo(70, 70, 50, 80); br.close(); c.drawPath(br, s); c.drawLine(50, 22, 50, 80, s); c.drawLine(22, 34, 40, 30, s); c.drawLine(22, 48, 40, 44, s); c.drawLine(60, 30, 78, 34, s); c.drawLine(60, 44, 78, 48, s); break; }
                case "calc": { c.drawRoundRect(18, 8, 82, 92, 10, 10, s); c.drawLine(30, 26, 70, 26, s); for (int i = 0; i < 9; i++) c.drawCircle(32 + (i % 3) * 18, 44 + (i / 3) * 16, 5, f); break; }
                case "x": { s.setStrokeWidth(10f); c.drawLine(30, 30, 70, 70, s); c.drawLine(70, 30, 30, 70, s); break; }
            }
            c.restore();
        }
        @Override public void setAlpha(int a) {}
        @Override public void setColorFilter(ColorFilter cf) {}
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
    public static Icon icon(String k, int color) { return new Icon(k, color); }
}
