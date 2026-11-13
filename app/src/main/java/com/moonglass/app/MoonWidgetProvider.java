package com.moonglass.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

public class MoonWidgetProvider extends AppWidgetProvider {

    private static final int[] SLOTS = {R.id.m0, R.id.m1, R.id.m2, R.id.m3, R.id.m4};

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) mgr.updateAppWidget(id, build(ctx));
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        String a = intent.getAction();
        if (Intent.ACTION_DATE_CHANGED.equals(a) || Intent.ACTION_TIME_SET.equals(a)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(a)) {
            AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
            int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, MoonWidgetProvider.class));
            onUpdate(ctx, mgr, ids);
        }
    }

    static RemoteViews build(Context ctx) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_moon);
        double age = MoonMath.ageDays(System.currentTimeMillis());
        int idx = MoonMath.phaseIndex(age);

        for (int k = -2; k <= 2; k++) {
            int phase = ((idx + k) % 8 + 8) % 8;
            boolean current = (k == 0);
            // centre moon = true current phase; neighbours = canonical phases
            float frac = current ? (float) (age / MoonMath.SYNODIC) : phase / 8f;
            Bitmap bmp = MoonMath.moon(frac, current ? 160 : 112);
            int slot = SLOTS[k + 2];
            rv.setImageViewBitmap(slot, bmp);
            rv.setInt(slot, "setImageAlpha", current ? 255 : 100);
            rv.setContentDescription(slot, MoonMath.NAMES[phase]);
        }

        PendingIntent pi = PendingIntent.getActivity(ctx, 0,
                new Intent(ctx, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.root, pi);
        return rv;
    }
}
