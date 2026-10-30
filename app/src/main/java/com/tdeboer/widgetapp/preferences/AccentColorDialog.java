package com.tdeboer.widgetapp.preferences;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.tdeboer.widgetapp.AppTheme;
import com.tdeboer.widgetapp.R;

public final class AccentColorDialog {
    private static final int DEFAULT_HUE = 240;
    private static final float CUSTOM_SATURATION = 0.75f;
    private static final float CUSTOM_BRIGHTNESS = 0.85f;
    private static final int[] SWATCHES = {
            0xFF4C51BF, 0xFF0B72E7, 0xFF00897B, 0xFF2E7D32,
            0xFFF9A825, 0xFFF4511E, 0xFFD81B60, 0xFF7B1FA2
    };

    public interface Callback {
        void onColorChosen(int color);
    }

    private AccentColorDialog() { }

    public static void show(Context context, int currentColor, Callback callback) {
        View content = LayoutInflater.from(context).inflate(R.layout.dialog_accent_color, null);
        View preview = content.findViewById(R.id.accent_preview);
        Slider hue = content.findViewById(R.id.accent_hue);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.accent_title)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.accent_use_custom, (d, which) -> callback.onColorChosen(colorForHue(hue.getValue())))
                .create();

        content.findViewById(R.id.accent_wallpaper).setOnClickListener(v -> {
            dialog.dismiss();
            callback.onColorChosen(AppTheme.ACCENT_WALLPAPER);
        });

        GridLayout swatches = content.findViewById(R.id.accent_swatches);
        String[] names = context.getResources().getStringArray(R.array.accent_swatch_names);
        int size = context.getResources().getDimensionPixelSize(R.dimen.touch_target);
        int margin = context.getResources().getDimensionPixelSize(R.dimen.space_small) / 2;
        for (int i = 0; i < SWATCHES.length; i++) {
            int color = SWATCHES[i];
            View swatch = new View(context);
            swatch.setBackground(circle(color));
            swatch.setContentDescription(names[i]);
            swatch.setOnClickListener(v -> {
                dialog.dismiss();
                callback.onColorChosen(color);
            });

            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, GridLayout.CENTER, 1f));
            params.width = size;
            params.height = size;
            params.setMargins(margin, margin, margin, margin);
            swatches.addView(swatch, params);
        }

        hue.setValue(startHue(currentColor));
        preview.setBackground(circle(colorForHue(hue.getValue())));
        hue.addOnChangeListener((slider, value, fromUser) -> preview.setBackground(circle(colorForHue(value))));

        dialog.show();
    }

    private static int startHue(int color) {
        if (color == AppTheme.ACCENT_WALLPAPER) {
            return DEFAULT_HUE;
        }
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        return Math.round(hsv[0]);
    }

    private static int colorForHue(float hue) {
        return Color.HSVToColor(new float[]{hue, CUSTOM_SATURATION, CUSTOM_BRIGHTNESS});
    }

    private static GradientDrawable circle(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }
}
