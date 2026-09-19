package ru.vnuchok.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;

/* =====================================================================
   Art.java — весь визуальный слой оболочки «ВНУЧОК» в стиле ретро-постера
   70-х: бумага с зерном, солнце над холмами, еловый лес, радужные полосы,
   пилюли с двойным кольцом и единый набор векторных иконок v2.
   Логика приложения сюда не заходит: только отрисовка.
   ===================================================================== */
public class Art {

    /* Цвета ретро-палитры, чтобы все экраны брали их из одного места */
    public static int RUST = 0xFFC05227;     // ржавый
    public static int ORANGE = 0xFFD9772F;   // оранжевый
    public static int MUSTARD = 0xFFD9A02B;  // горчичный
    public static int SAND = 0xFFE8C15A;     // песочный
    public static int PINE = 0xFF2E4034;     // хвойный тёмный
    public static int GREEN = 0xFF3E5641;    // зелёный корпус
    public static int MOSS = 0xFF5A7A4F;     // мох
    public static int OLIVE = 0xFF7A9A5F;    // олива
    public static int CREAM = 0xFFF3ECD8;    // кремовый текст
    public static int PAPER = 0xFFEFE7D2;    // бумага
    public static int BROWN = 0xFF6B4A2F;    // коричневый

    /* -----------------------------------------------------------------
       ИКОНКИ v2: единая сетка 100x100, скруглённые колпачки линий,
       одинаковая оптическая толщина, заполненные акценты.
       ----------------------------------------------------------------- */
    public static class Icon extends Drawable {
        private final String k;
        private final Paint f = new Paint(Paint.ANTI_ALIAS_FLAG); // заливка
        private final Paint s = new Paint(Paint.ANTI_ALIAS_FLAG); // контур
        public int level = -1; // для батарейки: процент
        public int bars = -1;  // для связи: число палок

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
                case "phone": { // телефонная трубка под 45°, как классика
                    c.save(); c.rotate(-45, 50, 50);
                    c.drawRoundRect(24, 40, 76, 60, 10, 10, f);
                    c.drawRoundRect(14, 26, 34, 60, 10, 10, f);
                    c.drawRoundRect(66, 26, 86, 60, 10, 10, f);
                    c.restore();
                    break; }
                case "mail": { // конверт с заполненным клапаном
                    c.drawRoundRect(8, 22, 92, 78, 10, 10, s);
                    Path flap = new Path();
                    flap.moveTo(12, 26); flap.lineTo(50, 54); flap.lineTo(88, 26);
                    flap.lineTo(88, 34); flap.lineTo(50, 62); flap.lineTo(12, 34); flap.close();
                    c.drawPath(flap, f);
                    break; }
                case "dial": { // сетка набора 3x3
                    for (int i = 0; i < 9; i++) c.drawCircle(26 + (i % 3) * 24, 26 + (i / 3) * 24, 8.5f, f);
                    break; }
                case "cam": { // фотоаппарат: корпус, объектив кольцом, вспышка
                    c.drawRoundRect(6, 30, 94, 80, 12, 12, s);
                    c.drawCircle(50, 55, 14, s);
                    c.drawCircle(50, 55, 5, f);
                    c.drawRoundRect(34, 18, 58, 30, 6, 6, f);
                    c.drawCircle(80, 42, 4, f);
                    break; }
                case "album": { // рамка с пейзажем: мягкие горы и солнце
                    c.drawRoundRect(8, 14, 92, 86, 12, 12, s);
                    Path m = new Path();
                    m.moveTo(18, 74);
                    m.quadTo(30, 50, 40, 62);
                    m.quadTo(50, 72, 58, 56);
                    m.quadTo(66, 44, 74, 60);
                    m.lineTo(82, 74); m.close();
                    c.drawPath(m, f);
                    c.drawCircle(70, 32, 7, f);
                    break; }
                case "memo": { // лист с загнутым углом, строками и карандашом
                    c.drawRoundRect(16, 8, 78, 92, 10, 10, s);
                    Path fold = new Path(); fold.moveTo(78, 8); fold.lineTo(78, 26); fold.lineTo(60, 8); fold.close();
                    c.drawPath(fold, f);
                    c.drawLine(28, 36, 66, 36, s);
                    c.drawLine(28, 52, 66, 52, s);
                    c.drawLine(28, 68, 52, 68, s);
                    c.save(); c.rotate(45, 76, 76);
                    c.drawRoundRect(60, 70, 96, 82, 6, 6, f);
                    Path tip = new Path(); tip.moveTo(60, 70); tip.lineTo(52, 76); tip.lineTo(60, 82); tip.close();
                    c.drawPath(tip, f);
                    c.restore();
                    break; }
                case "alarm": { // будильник: корпус, стрелки, chuông-кнопки, ножки
                    c.drawCircle(30, 26, 9, f);
                    c.drawCircle(70, 26, 9, f);
                    c.drawCircle(50, 56, 26, s);
                    c.drawLine(50, 56, 50, 40, s);
                    c.drawLine(50, 56, 62, 60, s);
                    c.drawCircle(50, 56, 4, f);
                    c.drawLine(32, 78, 24, 90, s);
                    c.drawLine(68, 78, 76, 90, s);
                    break; }
                case "clock": { // часы без будильника
                    c.drawCircle(50, 50, 32, s);
                    c.drawLine(50, 50, 50, 30, s);
                    c.drawLine(50, 50, 64, 56, s);
                    c.drawCircle(50, 50, 4, f);
                    break; }
                case "torch": { // фонарик: голова-трапеция, корпус, лучи
                    Path head = new Path();
                    head.moveTo(32, 22); head.lineTo(68, 22); head.lineTo(60, 44); head.lineTo(40, 44); head.close();
                    c.drawPath(head, f);
                    c.drawRoundRect(40, 44, 60, 88, 9, 9, f);
                    c.drawLine(50, 6, 50, 14, s);
                    c.drawLine(28, 10, 34, 16, s);
                    c.drawLine(72, 10, 66, 16, s);
                    break; }
                case "wrench": { // гаечный ключ с открытым зевом
                    s.setStrokeWidth(11f);
                    c.drawArc(16, 16, 48, 48, 60, 240, false, s);
                    c.save(); c.rotate(45, 50, 50);
                    c.drawRoundRect(40, 44, 90, 58, 7, 7, f);
                    c.restore();
                    break; }
                case "mic": { // микрофон: капсула, дуга-держатель, ножка, база
                    c.drawRoundRect(38, 6, 62, 48, 12, 12, f);
                    c.drawArc(26, 26, 74, 74, 0, 180, false, s);
                    c.drawLine(50, 74, 50, 86, s);
                    c.drawRoundRect(34, 86, 66, 94, 4, 4, f);
                    break; }
                case "sos": { // медицинский крест со скруглениями
                    c.drawRoundRect(40, 14, 60, 86, 8, 8, f);
                    c.drawRoundRect(14, 40, 86, 60, 8, 8, f);
                    break; }
                case "radio": { // радиоприёмник: динамик, шкала, антенна
                    c.drawRoundRect(8, 34, 92, 84, 12, 12, s);
                    c.drawCircle(30, 59, 12, s);
                    c.drawCircle(30, 59, 4, f);
                    c.drawLine(54, 50, 82, 50, s);
                    c.drawLine(54, 62, 82, 62, s);
                    c.drawLine(54, 74, 72, 74, s);
                    c.drawLine(64, 34, 88, 10, s);
                    break; }
                case "map": { // сложенная карта с меткой
                    Path p = new Path();
                    p.moveTo(12, 24); p.lineTo(38, 14); p.lineTo(62, 24); p.lineTo(88, 14);
                    p.lineTo(88, 76); p.lineTo(62, 86); p.lineTo(38, 76); p.lineTo(12, 86); p.close();
                    c.drawPath(p, s);
                    c.drawLine(38, 14, 38, 76, s);
                    c.drawLine(62, 24, 62, 86, s);
                    c.drawCircle(50, 46, 7, f);
                    break; }
                case "music": { // двойная нота с перекладиной
                    c.drawCircle(30, 76, 12, f);
                    c.drawCircle(66, 70, 12, f);
                    c.drawRect(40, 26, 46, 76, f);
                    c.drawRect(76, 20, 82, 70, f);
                    c.drawRoundRect(40, 14, 82, 28, 7, 7, f);
                    break; }
                case "weather": { // солнце за облаком
                    c.drawCircle(38, 34, 13, f);
                    for (int i = 0; i < 8; i++) {
                        c.save(); c.rotate(i * 45f, 38, 34);
                        c.drawLine(38, 14, 38, 8, s);
                        c.restore();
                    }
                    c.drawCircle(56, 62, 14, f);
                    c.drawCircle(72, 66, 11, f);
                    c.drawRoundRect(42, 62, 86, 77, 8, 8, f);
                    break; }
                case "sun": { // солнце для погодной пилюли
                    c.drawCircle(50, 50, 16, f);
                    for (int i = 0; i < 8; i++) {
                        c.save(); c.rotate(i * 45f, 50, 50);
                        c.drawLine(50, 26, 50, 16, s);
                        c.restore();
                    }
                    break; }
                case "web": { // глобус с меридианом и параллелями
                    c.drawCircle(50, 50, 34, s);
                    c.drawOval(new RectF(34, 16, 66, 84), s);
                    c.drawLine(16, 50, 84, 50, s);
                    c.drawArc(22, 28, 78, 72, 200, 140, false, s);
                    c.drawArc(22, 28, 78, 72, 20, 140, false, s);
                    break; }
                case "wifi": { // вай-фай: три дуги и точка
                    s.setStrokeWidth(9f);
                    c.drawArc(14, 26, 86, 98, 210, 120, false, s);
                    c.drawArc(28, 40, 72, 84, 210, 120, false, s);
                    c.drawArc(40, 52, 60, 72, 210, 120, false, s);
                    c.drawCircle(50, 76, 7, f);
                    break; }
                case "folder": { // папка с карманом
                    c.drawRoundRect(10, 24, 90, 82, 10, 10, f);
                    c.drawRoundRect(10, 16, 44, 34, 8, 8, f);
                    c.drawRoundRect(18, 44, 82, 74, 8, 8, s);
                    break; }
                case "sound": { // динамик с двумя волнами
                    Path p = new Path();
                    p.moveTo(14, 40); p.lineTo(32, 40); p.lineTo(52, 22);
                    p.lineTo(52, 78); p.lineTo(32, 60); p.lineTo(14, 60); p.close();
                    c.drawPath(p, f);
                    c.drawArc(58, 32, 84, 68, -45, 90, false, s);
                    c.drawArc(68, 22, 98, 78, -45, 90, false, s);
                    break; }
                case "silent": { // перечёркнутый динамик
                    Path p = new Path();
                    p.moveTo(14, 40); p.lineTo(32, 40); p.lineTo(52, 22);
                    p.lineTo(52, 78); p.lineTo(32, 60); p.lineTo(14, 60); p.close();
                    c.drawPath(p, f);
                    c.drawLine(64, 36, 90, 64, s);
                    c.drawLine(90, 36, 64, 64, s);
                    break; }
                case "gear": { // шестерня: толстое кольцо, зубцы, ось
                    for (int i = 0; i < 8; i++) {
                        c.save(); c.rotate(i * 45f, 50, 50);
                        c.drawRoundRect(43, 10, 57, 26, 5, 5, f);
                        c.restore();
                    }
                    s.setStrokeWidth(13f);
                    c.drawCircle(50, 50, 22, s);
                    c.drawCircle(50, 50, 7, f);
                    break; }
                case "batt": { // батарейка с уровнем заряда
                    float lv = level < 0 ? 50 : level;
                    s.setStrokeWidth(7);
                    c.drawRoundRect(4, 26, 82, 74, 12, 12, s);
                    c.drawRoundRect(86, 40, 96, 60, 4, 4, f);
                    float fw = Math.max(6, 66 * lv / 100f);
                    c.drawRoundRect(11, 33, 11 + fw, 67, 6, 6, f);
                    break; }
                case "battc": { // батарейка с молнией (зарядка)
                    s.setStrokeWidth(7);
                    c.drawRoundRect(4, 26, 82, 74, 12, 12, s);
                    c.drawRoundRect(86, 40, 96, 60, 4, 4, f);
                    Path bo = new Path();
                    bo.moveTo(50, 30); bo.lineTo(34, 52); bo.lineTo(44, 52);
                    bo.lineTo(38, 70); bo.lineTo(58, 46); bo.lineTo(47, 46); bo.close();
                    c.drawPath(bo, f);
                    break; }
                case "sig": { // палки связи
                    for (int i = 0; i < 4; i++) {
                        float bh = 25 + i * 20;
                        boolean on = bars < 0 || i < bars;
                        c.drawRoundRect(10 + i * 24, 92 - bh, 26 + i * 24, 92, 5, 5, on ? f : s);
                    }
                    break; }
                case "home": { // домик с дверью
                    Path p = new Path();
                    p.moveTo(50, 12); p.lineTo(90, 48); p.lineTo(78, 48); p.lineTo(78, 88);
                    p.lineTo(58, 88); p.lineTo(58, 62); p.lineTo(42, 62); p.lineTo(42, 88);
                    p.lineTo(22, 88); p.lineTo(22, 48); p.lineTo(10, 48); p.close();
                    c.drawPath(p, f);
                    break; }
                case "people": { // два человека
                    c.drawCircle(38, 30, 13, f);
                    c.drawRoundRect(18, 48, 58, 86, 16, 16, f);
                    c.drawCircle(68, 36, 10, f);
                    c.drawRoundRect(52, 52, 86, 86, 13, 13, f);
                    break; }
                case "back": { // стрелка назад
                    s.setStrokeWidth(10f);
                    c.drawLine(72, 50, 32, 50, s);
                    c.drawLine(46, 34, 30, 50, s);
                    c.drawLine(46, 66, 30, 50, s);
                    break; }
                case "book": { // раскрытая книга
                    Path bl = new Path();
                    bl.moveTo(50, 22); bl.quadTo(30, 12, 12, 20); bl.lineTo(12, 78);
                    bl.quadTo(30, 70, 50, 80); bl.close();
                    c.drawPath(bl, s);
                    Path br = new Path();
                    br.moveTo(50, 22); br.quadTo(70, 12, 88, 20); br.lineTo(88, 78);
                    br.quadTo(70, 70, 50, 80); br.close();
                    c.drawPath(br, s);
                    c.drawLine(50, 22, 50, 80, s);
                    c.drawLine(22, 34, 40, 30, s);
                    c.drawLine(22, 48, 40, 44, s);
                    c.drawLine(60, 30, 78, 34, s);
                    c.drawLine(60, 44, 78, 48, s);
                    break; }
                case "x": { // крестик закрытия
                    s.setStrokeWidth(10f);
                    c.drawLine(30, 30, 70, 70, s);
                    c.drawLine(70, 30, 30, 70, s);
                    break; }
            }
            c.restore();
        }
        @Override public void setAlpha(int a) {}
        @Override public void setColorFilter(ColorFilter cf) {}
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    public static Icon icon(String k, int color) { return new Icon(k, color); }

    /* -----------------------------------------------------------------
       БУМАГА: кремовый фон с горизонтальным зерном и мягкой виньеткой
       ----------------------------------------------------------------- */
    public static Drawable paper() {
        return new Drawable() {
            private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override public void draw(Canvas c) {
                Rect b = getBounds();
                p.setStyle(Paint.Style.FILL);
                p.setColor(PAPER);
                c.drawRect(b, p);
                // зерно: тонкие горизонтальные линии
                p.setColor(0x0D6B4A2F);
                for (int y = b.top; y < b.bottom; y += 7) c.drawLine(b.left, y, b.right, y, p);
                // виньетка по краям
                p.setShader(new RadialGradient(b.centerX(), b.centerY(),
                        Math.max(b.width(), b.height()) * 0.75f,
                        new int[]{0x006B4A2F, 0x266B4A2F}, null, Shader.TileMode.CLAMP));
                c.drawRect(b, p);
                p.setShader(null);
            }
            @Override public void setAlpha(int a) {}
            @Override public void setColorFilter(ColorFilter cf) {}
            @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        };
    }

    /* -----------------------------------------------------------------
       ПИЛЮЛЯ С ДВОЙНЫМ КОЛЬЦОМ (как часы в референсе):
       внешнее цветное кольцо, кремовое кольцо, тело
       ----------------------------------------------------------------- */
    public static Drawable ringPill(Context ctx, int body, int outer) {
        float d = ctx.getResources().getDisplayMetrics().density;
        int r = Math.round(60 * d);
        GradientDrawable g1 = new GradientDrawable();
        g1.setShape(GradientDrawable.RECTANGLE); g1.setCornerRadius(r);
        g1.setColor(body); g1.setStroke(Math.round(4 * d), outer);
        GradientDrawable g2 = new GradientDrawable();
        g2.setShape(GradientDrawable.RECTANGLE); g2.setCornerRadius(r - Math.round(6 * d));
        g2.setColor(body); g2.setStroke(Math.round(3 * d), CREAM);
        GradientDrawable g3 = new GradientDrawable();
        g3.setShape(GradientDrawable.RECTANGLE); g3.setCornerRadius(r - Math.round(11 * d));
        g3.setColor(body);
        int i1 = Math.round(7 * d), i2 = Math.round(12 * d);
        LayerDrawable ld = new LayerDrawable(new Drawable[]{g1, g2, g3});
        ld.setLayerInset(1, i1, i1, i1, i1);
        ld.setLayerInset(2, i2, i2, i2, i2);
        return ld;
    }

    /* Обычная пилюля без колец (статус-бар, дата) */
    public static Drawable pill(Context ctx, int body) {
        float d = ctx.getResources().getDisplayMetrics().density;
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.RECTANGLE);
        g.setCornerRadius(Math.round(60 * d));
        g.setColor(body);
        return g;
    }

    /* -----------------------------------------------------------------
       РАДУЖНЫЕ ПОЛОСЫ в углу пилюли (фирменный штрих референса)
       ----------------------------------------------------------------- */
    public static View stripes(Context ctx, int wPx, int hPx) {
        View v = new View(ctx) {
            private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            { p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); }
            @Override protected void onDraw(Canvas c) {
                int w = getWidth(), h = getHeight();
                float sw = h * 0.16f;
                p.setStrokeWidth(sw);
                int[] cols = {RUST, MUSTARD, ORANGE};
                for (int i = 0; i < 3; i++) {
                    p.setColor(cols[i]);
                    float r = h * (1.15f - i * 0.28f);
                    c.drawArc(w - r, -r * 0.55f, w + r * 0.2f, r * 0.9f, 25, 65, false, p);
                }
            }
        };
        v.getLayoutParams();
        return v;
    }

    /* -----------------------------------------------------------------
       СЦЕНА-ИЛЛЮСТРАЦИЯ шапки: солнце кольцами, три холма, еловый лес
       ----------------------------------------------------------------- */
    public static View scene(Context ctx, int hPx) {
        View v = new View(ctx) {
            private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            { p.setStyle(Paint.Style.FILL); }
            @Override protected void onDraw(Canvas c) {
                int w = getWidth(), h = getHeight();
                // солнце: концентрические круги у левого края (видны дуги)
                float cx = w * 0.13f, cy = h * 0.98f;
                int[] cols = {RUST, ORANGE, MUSTARD, SAND};
                float[] rr = {h * 0.92f, h * 0.74f, h * 0.56f, h * 0.38f};
                for (int i = 0; i < 4; i++) { p.setColor(cols[i]); c.drawCircle(cx, cy, rr[i], p); }
                // холмы тремя волнами
                Path h1 = new Path();
                h1.moveTo(0, h * 0.60f);
                h1.quadTo(w * 0.22f, h * 0.40f, w * 0.48f, h * 0.58f);
                h1.quadTo(w * 0.74f, h * 0.76f, w, h * 0.52f);
                h1.lineTo(w, h); h1.lineTo(0, h); h1.close();
                p.setColor(GREEN); c.drawPath(h1, p);
                Path h2 = new Path();
                h2.moveTo(0, h * 0.78f);
                h2.quadTo(w * 0.3f, h * 0.62f, w * 0.6f, h * 0.78f);
                h2.quadTo(w * 0.82f, h * 0.9f, w, h * 0.74f);
                h2.lineTo(w, h); h2.lineTo(0, h); h2.close();
                p.setColor(MOSS); c.drawPath(h2, p);
                Path h3 = new Path();
                h3.moveTo(0, h * 0.94f);
                h3.quadTo(w * 0.4f, h * 0.82f, w * 0.75f, h * 0.95f);
                h3.quadTo(w * 0.9f, h * 0.99f, w, h * 0.92f);
                h3.lineTo(w, h); h3.lineTo(0, h); h3.close();
                p.setColor(OLIVE); c.drawPath(h3, p);
                // еловый лес справа на первом холме
                float[] xs = {0.62f, 0.72f, 0.82f, 0.92f};
                float[] ss = {0.8f, 1.0f, 0.9f, 0.7f};
                for (int i = 0; i < 4; i++) tree(c, xs[i] * w, h * 0.58f, h * 0.30f * ss[i]);
            }
            private void tree(Canvas c, float x, float baseY, float size) {
                p.setColor(PINE);
                for (int t = 0; t < 3; t++) {
                    float ty = baseY - size * 0.33f * t;
                    float tw = size * (0.42f - t * 0.10f);
                    Path tr = new Path();
                    tr.moveTo(x, ty - size * 0.42f);
                    tr.lineTo(x - tw, ty);
                    tr.lineTo(x + tw, ty);
                    tr.close();
                    c.drawPath(tr, p);
                }
                c.drawRect(x - size * 0.06f, baseY - size * 0.06f, x + size * 0.06f, baseY + size * 0.10f, p);
            }
        };
        return v;
    }
}
