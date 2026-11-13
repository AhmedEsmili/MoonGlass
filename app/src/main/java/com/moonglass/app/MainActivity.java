package com.moonglass.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity {

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(6), 0, dp(6));
        return t;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        double age = MoonMath.ageDays(System.currentTimeMillis());
        int idx = MoonMath.phaseIndex(age);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF22386B, 0xFF0B1226}));

        ImageView iv = new ImageView(this);
        iv.setImageBitmap(MoonMath.moon((float) (age / MoonMath.SYNODIC), 512));
        root.addView(iv, new LinearLayout.LayoutParams(dp(220), dp(220)));

        root.addView(text(MoonMath.NAMES[idx], 28, Color.WHITE));
        root.addView(text(String.format(Locale.US, "%.0f%% illuminated  •  day %.1f of 29.5",
                MoonMath.illumination(age) * 100, age), 15, 0xFFB8C4E6));
        root.addView(text("Add the widget: long-press your home screen → Widgets → MoonGlass (4×1).",
                13, 0xFF8895BC));

        setContentView(root);
    }
}
