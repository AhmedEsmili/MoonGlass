package com.moonglass.app;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import java.util.Locale;
import java.util.Random;

/** Draws the whole widget (liquid-glass bar + minimal moons + labels) into one bitmap. */
public final class GlassRenderer {

    private GlassRenderer() {}

    private static int a(float alpha) {
        return Color.argb(Math.round(255f * Math.min(1f, Math.max(0f, alpha))), 255, 255, 255);
    }

    public static Bitmap render(float wDp, float hDp, float density, double age) {
        float d = Math.min(density, 3f);
        int W = Math.max(64, Math.round(wDp * d));
        int H = Math.max(32, Math.round(hDp * d));
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        float pad = 5f * d;
        RectF bar = new RectF(pad, pad, W - pad, H - pad);
        float bh = bar.height();
        float rad = bh * 0.42f;
        drawGlass(c, bar, rad, 1f, d, true);

        int idx = MoonMath.phaseIndex(age);
        float cx = W / 2f, cy = H / 2f;
        float step = Math.min(W * 0.135f, bh * 0.95f);
        float rn = bh * 0.125f;
        float rc = bh * 0.20f;

        // glass lens behind the current moon
        float lw = Math.max(step * 1.05f, rc * 2.5f);
        float lh = bh * 0.80f;
        RectF lens = new RectF(cx - lw / 2f, cy - lh / 2f, cx + lw / 2f, cy + lh / 2f);
        drawGlass(c, lens, Math.min(lw, lh) * 0.5f, 1.25f, d, false);

        for (int k = -2; k <= 2; k++) {
            int phase = ((idx + k) % 8 + 8) % 8;
            float x = cx + k * step;
            if (k == 0) drawMoon(c, x, cy, rc, (float) (age / MoonMath.SYNODIC), 255, d, true);
            else drawMoon(c, x, cy, rn, phase / 8f, 125, d, false);
        }

        // labels on both sides (skipped automatically when there is no room)
        float leftEdge = cx - 2 * step - rn - 8f * d;
        float rightEdge = cx + 2 * step + rn + 8f * d;
        float zoneL0 = bar.left + rad * 0.55f;
        float zoneR1 = bar.right - rad * 0.55f;
        String[] nm = MoonMath.NAMES[idx].split(" ", 2);
        twoLines(c, nm[0], nm.length > 1 ? nm[1] : "", zoneL0, leftEdge, cy, d, 8.5f, 12f);
        String pct = String.format(Locale.US, "%.0f%%", MoonMath.illumination(age) * 100);
        twoLines(c, "illuminated", pct, rightEdge, zoneR1, cy, d, 8f, 14f);
        return bmp;
    }

    /** Frosted, refractive-looking glass slab. k = strength multiplier. */
    private static void drawGlass(Canvas c, RectF r, float rad, float k, float d, boolean shadow) {
        Path p = new Path();
        p.addRoundRect(r, rad, rad, Path.Direction.CW);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        if (shadow) {
            c.save();
            c.clipOutPath(p);
            paint.setColor(0x47000000);
            paint.setMaskFilter(new BlurMaskFilter(6f * d, BlurMaskFilter.Blur.NORMAL));
            c.translate(0, 2.5f * d);
            c.drawPath(p, paint);
            c.restore();
            paint.setMaskFilter(null);
        }

        c.save();
        c.clipPath(p);

        // translucent body
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0, r.top, 0, r.bottom,
                a(0.26f * k), a(0.08f * k), Shader.TileMode.CLAMP));
        c.drawPath(p, paint);
        paint.setShader(null);

        // frost grain
        Random rnd = new Random(11);
        Paint g = new Paint();
        g.setStrokeWidth(Math.max(1f, d * 0.8f));
        int n = (int) (r.width() * r.height() / (14f * d * d));
        for (int i = 0; i < n; i++) {
            g.setColor(Color.argb(6 + rnd.nextInt(16), 255, 255, 255));
            c.drawPoint(r.left + rnd.nextFloat() * r.width(), r.top + rnd.nextFloat() * r.height(), g);
        }

        // top sheen
        RectF s = new RectF(r.left + rad * 0.55f, r.top + 1.5f * d, r.right - rad * 0.55f, r.top + r.height() * 0.5f);
        paint.setShader(new LinearGradient(0, s.top, 0, s.bottom, a(0.30f * k), a(0f), Shader.TileMode.CLAMP));
        c.drawRoundRect(s, s.height() / 2f, s.height() / 2f, paint);

        // bottom caustic
        RectF b = new RectF(r.left + rad * 0.8f, r.bottom - r.height() * 0.22f, r.right - rad * 0.8f, r.bottom - 1.5f * d);
        paint.setShader(new LinearGradient(0, b.bottom, 0, b.top, a(0.20f * k), a(0f), Shader.TileMode.CLAMP));
        c.drawRoundRect(b, b.height() / 2f, b.height() / 2f, paint);
        paint.setShader(null);

        // rim: faint full outline + soft glow + crisp specular on top-left & bottom-right
        float len = Math.max(r.height() * 2.4f, 1f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.2f * d);
        paint.setColor(a(0.28f * k));
        c.drawPath(p, paint);

        paint.setStrokeWidth(5f * d);
        paint.setMaskFilter(new BlurMaskFilter(2.5f * d, BlurMaskFilter.Blur.NORMAL));
        paint.setShader(new LinearGradient(r.left, r.top, r.left + len, r.top + len * 0.45f,
                a(0.55f * k), a(0f), Shader.TileMode.CLAMP));
        c.drawPath(p, paint);
        paint.setShader(new LinearGradient(r.right, r.bottom, r.right - len, r.bottom - len * 0.45f,
                a(0.45f * k), a(0f), Shader.TileMode.CLAMP));
        c.drawPath(p, paint);
        paint.setMaskFilter(null);

        paint.setStrokeWidth(1.8f * d);
        paint.setShader(new LinearGradient(r.left, r.top, r.left + len, r.top + len * 0.45f,
                a(0.95f * k), a(0f), Shader.TileMode.CLAMP));
        c.drawPath(p, paint);
        paint.setShader(new LinearGradient(r.right, r.bottom, r.right - len, r.bottom - len * 0.45f,
                a(0.80f * k), a(0f), Shader.TileMode.CLAMP));
        c.drawPath(p, paint);
        paint.setShader(null);

        c.restore();
    }

    /** Minimal flat moon: faint ring + warm-white lit area. frac 0=new, .5=full. */
    private static void drawMoon(Canvas c, float cx, float cy, float r, float frac,
                                 int litA, float d, boolean glow) {
        float af = litA / 255f;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        if (glow) {
            p.setColor(Color.argb(95, 255, 240, 200));
            p.setMaskFilter(new BlurMaskFilter(r * 0.9f, BlurMaskFilter.Blur.NORMAL));
            c.drawCircle(cx, cy, r * 1.15f, p);
            p.setMaskFilter(null);
        }

        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(Math.round(46f * af + 10f), 255, 255, 255));
        c.drawCircle(cx, cy, r, p);

        float sw = Math.max(1f, 0.9f * d);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(sw);
        p.setColor(Color.argb(Math.round(150f * af), 255, 255, 255));
        c.drawCircle(cx, cy, r - sw / 2f, p);

        double th = 2.0 * Math.PI * frac;
        float illum = (float) ((1.0 - Math.cos(th)) / 2.0);
        boolean waxing = frac < 0.5f;
        Path lit = new Path();
        if (illum > 0.995f) {
            lit.addCircle(cx, cy, r, Path.Direction.CW);
        } else if (illum >= 0.005f) {
            RectF oval = new RectF(cx - r, cy - r, cx + r, cy + r);
            Path half = new Path();
            half.arcTo(oval, waxing ? -90f : 90f, 180f);
            half.close();
            float rx = r * (float) Math.abs(Math.cos(th));
            Path ell = new Path();
            ell.addOval(new RectF(cx - rx, cy - r, cx + rx, cy + r), Path.Direction.CW);
            if (illum < 0.5f) half.op(ell, Path.Op.DIFFERENCE);
            else half.op(ell, Path.Op.UNION);
            lit = half;
        }
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(litA, 255, 246, 224));
        c.drawPath(lit, p);
    }

    private static void twoLines(Canvas c, String top, String bottom, float x0, float x1,
                                 float cy, float d, float topSp, float botSp) {
        float zone = x1 - x0;
        if (zone < 30f * d) return;
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setShadowLayer(2.5f * d, 0, 1f * d, 0x66000000);
        Typeface reg = Typeface.create("sans-serif", Typeface.NORMAL);
        Typeface med = Typeface.create("sans-serif-medium", Typeface.NORMAL);

        float ts = topSp * d, bs = botSp * d;
        tp.setTypeface(reg); tp.setTextSize(ts);
        float w1 = tp.measureText(top);
        tp.setTypeface(med); tp.setTextSize(bs);
        float w2 = tp.measureText(bottom);
        float m = Math.max(w1, w2);
        if (m > zone) {
            float f = zone / m;
            ts *= f;
            bs *= f;
        }
        if (bs < 7f * d) return;

        float mx = (x0 + x1) / 2f;
        float total = ts + 2f * d + bs;
        float topBase = cy - total / 2f + ts * 0.82f;
        float botBase = cy + total / 2f - bs * 0.2f;

        tp.setTypeface(reg); tp.setTextSize(ts);
        tp.setColor(Color.argb(170, 255, 255, 255));
        c.drawText(top, mx, topBase, tp);
        tp.setTypeface(med); tp.setTextSize(bs);
        tp.setColor(Color.argb(240, 255, 255, 255));
        c.drawText(bottom, mx, botBase, tp);
    }
}
