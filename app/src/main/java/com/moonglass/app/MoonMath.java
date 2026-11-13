package com.moonglass.app;

import android.graphics.Bitmap;

/** Moon phase maths + procedural moon bitmap renderer. */
public final class MoonMath {
    public static final double SYNODIC = 29.530588853;
    // Known new moon: 2000-01-06 18:14 UTC
    private static final long REF_NEW_MOON_MS = 947182440000L;

    public static final String[] NAMES = {
            "New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous",
            "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent"
    };

    private MoonMath() {}

    /** Days since last new moon (0..29.53). */
    public static double ageDays(long nowMs) {
        double a = ((nowMs - REF_NEW_MOON_MS) / 86400000.0) % SYNODIC;
        return a < 0 ? a + SYNODIC : a;
    }

    /** Nearest of the 8 named phases (0..7). */
    public static int phaseIndex(double age) {
        return (int) Math.round(age / SYNODIC * 8.0) % 8;
    }

    /** Illuminated fraction 0..1. */
    public static double illumination(double age) {
        return (1.0 - Math.cos(2.0 * Math.PI * age / SYNODIC)) / 2.0;
    }

    private static float smooth(float e0, float e1, float x) {
        float t = Math.max(0f, Math.min(1f, (x - e0) / (e1 - e0)));
        return t * t * (3f - 2f * t);
    }

    /**
     * Renders a lit sphere. frac: 0 = new, 0.25 = first quarter, 0.5 = full, 0.75 = last quarter.
     * Waxing phases are lit on the right (northern hemisphere view).
     */
    public static Bitmap moon(float frac, int size) {
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        int[] px = new int[size * size];
        double th = 2.0 * Math.PI * frac;
        float sx = (float) Math.sin(th);
        float sz = (float) -Math.cos(th);
        float c = size / 2f;
        float rad = c - 1.5f;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float nx = (x + 0.5f - c) / rad;
                float ny = (y + 0.5f - c) / rad;
                float d2 = nx * nx + ny * ny;
                float dist = (float) Math.sqrt(d2);
                float cov = Math.min(1f, Math.max(0f, (1f - dist) * rad + 0.5f));
                if (cov <= 0f) continue;

                float nz = (float) Math.sqrt(Math.max(0f, 1f - d2));
                float lit = nx * sx + nz * sz;
                float t = smooth(-0.04f, 0.10f, lit);
                float limb = 0.62f + 0.38f * nz;

                // soft "maria" texture
                float tex = 1f - 0.06f * (float) (Math.sin(nx * 9 + ny * 4) * Math.cos(ny * 11 - nx * 3)
                        + Math.sin(nx * 21) * Math.sin(ny * 17)) * 0.5f;

                float r = 46f + (252f * limb * tex - 46f) * t;
                float g = 56f + (246f * limb * tex - 56f) * t;
                float b = 84f + (230f * limb * tex - 84f) * t;

                int a = (int) (cov * 255f);
                px[y * size + x] = (a << 24)
                        | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
            }
        }
        bmp.setPixels(px, 0, size, 0, 0, size, size);
        return bmp;
    }

    private static int clamp(float v) {
        return Math.max(0, Math.min(255, (int) v));
    }
}
