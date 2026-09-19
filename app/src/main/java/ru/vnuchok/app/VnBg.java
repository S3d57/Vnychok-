package ru.vnuchok.app;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;
/* ФОН: бумага с зерном, сцена (солнце+холмы+лес), радужные полосы, занавес разблокировки. */
public class VnBg {
    public static Drawable paper() {
        return new Drawable() {
            private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override public void draw(Canvas c) {
                Rect b = getBounds();
                p.setStyle(Paint.Style.FILL);
                p.setColor(VnTheme.PAPER);
                c.drawRect(b, p);
                p.setColor(0x0D6B4A2F);
                for (int y = b.top; y < b.bottom; y += 7) c.drawLine(b.left, y, b.right, y, p);
                p.setShader(new RadialGradient(b.centerX(), b.centerY(), Math.max(b.width(), b.height()) * 0.75f, new int[]{0x006B4A2F, 0x266B4A2F}, null, Shader.TileMode.CLAMP));
                c.drawRect(b, p);
                p.setShader(null);
            }
            @Override public void setAlpha(int a) {}
            @Override public void setColorFilter(ColorFilter cf) {}
            @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
        };
    }
    static void drawScene(Canvas c, int w, int h, Paint p) {
        float cx = w * 0.16f, cy = h * 0.62f;
        int[] cols = {VnTheme.RUST, VnTheme.ORANGE, VnTheme.MUSTARD, VnTheme.SAND};
        float[] rr = {h * 0.58f, h * 0.46f, h * 0.34f, h * 0.22f};
        for (int i = 0; i < 4; i++) { p.setColor(cols[i]); c.drawCircle(cx, cy, rr[i], p); }
        Path h1 = new Path(); h1.moveTo(0, h * 0.52f); h1.quadTo(w * 0.22f, h * 0.34f, w * 0.48f, h * 0.50f); h1.quadTo(w * 0.74f, h * 0.66f, w, h * 0.44f); h1.lineTo(w, h); h1.lineTo(0, h); h1.close(); p.setColor(VnTheme.GREEN); c.drawPath(h1, p);
        Path h2 = new Path(); h2.moveTo(0, h * 0.68f); h2.quadTo(w * 0.3f, h * 0.54f, w * 0.6f, h * 0.68f); h2.quadTo(w * 0.82f, h * 0.8f, w, h * 0.64f); h2.lineTo(w, h); h2.lineTo(0, h); h2.close(); p.setColor(VnTheme.MOSS); c.drawPath(h2, p);
        Path h3 = new Path(); h3.moveTo(0, h * 0.86f); h3.quadTo(w * 0.4f, h * 0.74f, w * 0.75f, h * 0.87f); h3.quadTo(w * 0.9f, h * 0.92f, w, h * 0.84f); h3.lineTo(w, h); h3.lineTo(0, h); h3.close(); p.setColor(VnTheme.OLIVE); c.drawPath(h3, p);
        float[] xs = {0.60f, 0.70f, 0.80f, 0.90f};
        float[] ss = {0.8f, 1.0f, 0.9f, 0.7f};
        for (int i = 0; i < 4; i++) tree(c, xs[i] * w, h * 0.50f, h * 0.22f * ss[i], p);
    }
    private static void tree(Canvas c, float x, float baseY, float size, Paint p) {
        p.setColor(VnTheme.PINE);
        for (int t = 0; t < 3; t++) {
            float ty = baseY - size * 0.33f * t;
            float tw = size * (0.42f - t * 0.10f);
            Path tr = new Path(); tr.moveTo(x, ty - size * 0.42f); tr.lineTo(x - tw, ty); tr.lineTo(x + tw, ty); tr.close();
            c.drawPath(tr, p);
        }
        c.drawRect(x - size * 0.06f, baseY - size * 0.06f, x + size * 0.06f, baseY + size * 0.10f, p);
    }
    public static View scene(Context ctx) {
        return new View(ctx) {
            private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            { p.setStyle(Paint.Style.FILL); }
            @Override protected void onDraw(Canvas c) { drawScene(c, getWidth(), getHeight(), p); }
        };
    }
    public static View stripes(Context ctx) {
        return new View(ctx) {
            private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            { p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); }
            @Override protected void onDraw(Canvas c) {
                int w = getWidth(), h = getHeight();
                p.setStrokeWidth(h * 0.16f);
                int[] cols = {VnTheme.RUST, VnTheme.MUSTARD, VnTheme.ORANGE};
                for (int i = 0; i < 3; i++) { p.setColor(cols[i]); float r = h * (1.15f - i * 0.28f); c.drawArc(w - r, -r * 0.55f, w + r * 0.2f, r * 0.9f, 25, 65, false, p); }
            }
        };
    }
    /* ЗАНАВЕС: холмы с лесом разъезжаются в стороны за 2 секунды */
    public static class CurtainView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float open = 0f;
        public CurtainView(Context c) { super(c); p.setStyle(Paint.Style.FILL); }
        public void start(final Runnable onDone) {
            android.animation.ValueAnimator va = android.animation.ValueAnimator.ofFloat(0f, 1f);
            va.setDuration(2000);
            va.addUpdateListener(a -> { open = (Float) a.getAnimatedValue(); invalidate(); });
            va.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator a) { if (onDone != null) onDone.run(); }
            });
            va.start();
        }
        @Override protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight();
            if (w == 0 || h == 0) return;
            c.drawColor(VnTheme.PAPER);
            int shift = (int) (open * w);
            c.save(); c.clipRect(0, 0, w / 2, h); c.translate(-shift, 0); drawScene(c, w, h, p); c.restore();
            c.save(); c.clipRect(w / 2, 0, w, h); c.translate(shift, 0); drawScene(c, w, h, p); c.restore();
        }
    }
    public static View curtain(Context c) { return new CurtainView(c); }
}
