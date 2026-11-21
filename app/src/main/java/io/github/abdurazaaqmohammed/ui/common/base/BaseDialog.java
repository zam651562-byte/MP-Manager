package io.github.abdurazaaqmohammed.ui.common.base;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import io.github.abdurazaaqmohammed.MPManager.R;

/**
 * Single place for dialog construction. Keeps MaterialAlertDialogBuilder
 * usage and view inflation consistent across features.
 */
public final class BaseDialog {

    private BaseDialog() {
    }

    public static EnhancedBuilder builder(@NonNull Context context) {
        return new EnhancedBuilder(context);
    }

    public static View inflate(@NonNull Context context, @LayoutRes int layout) {
        return LayoutInflater.from(context).inflate(layout, null, false);
    }

    /** Shared runtime styling for dialogs created through the original MP Manager builders. */
    public static void enhance(@Nullable AlertDialog dialog) {
        enhance(dialog, new DialogConfig());
    }

    public static void enhance(@Nullable AlertDialog dialog, @NonNull DialogConfig config) {
        if (dialog == null || dialog.getWindow() == null) return;
        Context context = dialog.getContext();
        Window window = dialog.getWindow();

        window.setBackgroundDrawable(new GradientDrawable());
        window.setWindowAnimations(R.style.MpDialogAnimations);
        window.setDimAmount(config.dimAlphaPercent != null
                ? clampPercent(config.dimAlphaPercent) / 100f : getDimAmount(context));

        WindowManager.LayoutParams attrs = window.getAttributes();
        attrs.width = calculateWidth(context, config);
        attrs.height = WindowManager.LayoutParams.WRAP_CONTENT;
        attrs.windowAnimations = R.style.MpDialogAnimations;
        attrs.dimAmount = config.dimAlphaPercent != null
                ? clampPercent(config.dimAlphaPercent) / 100f : getDimAmount(context);
        window.setAttributes(attrs);

        window.getDecorView().post(() -> applyCardAndDrag(context, dialog, window.getDecorView(), config));
    }

    private static int calculateWidth(Context context) {
        return calculateWidth(context, new DialogConfig());
    }

    private static int calculateWidth(Context context, DialogConfig config) {
        int width = context.getResources().getDisplayMetrics().widthPixels;
        int margin = dp(context, 20);
        int preferred = dp(context, config.widthDp != null ? clamp(config.widthDp, 280, 720) : getDialogWidth(context));
        int available = Math.max(dp(context, 280), width - margin * 2);
        return Math.min(preferred, available);
    }

    private static void applyCardAndDrag(Context context, AlertDialog dialog, View decor) {
        applyCardAndDrag(context, dialog, decor, new DialogConfig());
    }

    private static void applyCardAndDrag(Context context, AlertDialog dialog, View decor, DialogConfig config) {
        if (!dialog.isShowing()) return;
        float density = context.getResources().getDisplayMetrics().density;
        GradientDrawable card = new GradientDrawable();
        card.setShape(GradientDrawable.RECTANGLE);
        card.setColor(resolveSurface(context, config));
        card.setCornerRadius(dp(context, config.cornerRadiusDp != null
                ? clamp(config.cornerRadiusDp, 0, 32) : getCornerRadius(context)));
        card.setStroke(Math.max(1, (int) density),
                withAlpha(resolveBorder(context), config.borderAlphaPercent != null
                        ? clampPercent(config.borderAlphaPercent) : getBorderAlpha(context)));
        decor.setBackground(card);
        decor.setClipToOutline(true);
        decor.setElevation(dp(context, 8));

        TextView title = findTitle(decor);
        if (title == null) return;
        View dragHandle = title.getParent() instanceof View ? (View) title.getParent() : title;
        dragHandle.setOnTouchListener(new DragTouchListener(dialog, decor, config));
    }

    private static TextView findTitle(View root) {
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof TextView
                        && (child.getId() == androidx.appcompat.R.id.alertTitle
                        || child.getId() == com.google.android.material.R.id.alertTitle)) {
                    return (TextView) child;
                }
                TextView nested = findTitle(child);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static int resolveSurface(Context context) {
        return resolveSurface(context, new DialogConfig());
    }

    private static int resolveSurface(Context context, DialogConfig config) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, value, true);
        return withAlpha(value.data, config.alphaPercent != null
                ? clampPercent(config.alphaPercent) : getDialogAlpha(context));
    }

    private static int resolveBorder(Context context) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOutlineVariant, value, true);
        return value.resourceId != 0 ? context.getColor(value.resourceId) : value.data;
    }

    private static int withAlpha(int color, int alphaPercent) {
        int alpha = Math.max(0, Math.min(255, Math.round(255f * alphaPercent / 100f)));
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private static int dp(Context context, int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics());
    }

    private static int getDialogWidth(Context context) {
        int width = readInt(context, "mp_dialog_width_dp", 520);
        return Math.max(280, Math.min(720, width));
    }

    private static int getCornerRadius(Context context) {
        return Math.max(0, Math.min(32, readInt(context, "mp_dialog_corner_radius", 18)));
    }

    private static int getDialogAlpha(Context context) {
        return Math.max(40, Math.min(100, readInt(context, "mp_dialog_alpha", 100)));
    }

    private static int getBorderAlpha(Context context) {
        return Math.max(0, Math.min(100, readInt(context, "mp_dialog_border_alpha", 100)));
    }

    private static float getDimAmount(Context context) {
        int percent = Math.max(0, Math.min(100, readInt(context, "mp_dialog_dim_alpha", 55)));
        return percent / 100f;
    }

    private static int readInt(Context context, String key, int fallback) {
        try {
            return PreferenceManager.getDefaultSharedPreferences(context).getInt(key, fallback);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean isDragEnabled(Context context) {
        try {
            return PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean("mp_dialog_drag_enabled", true);
        } catch (Exception ignored) {
            return true;
        }
    }

    private static int clamp(int value, int low, int high) {
        return Math.max(low, Math.min(high, value));
    }

    private static int clampPercent(int value) {
        return clamp(value, 0, 100);
    }

    private static final class DragTouchListener implements View.OnTouchListener {
        private final AlertDialog dialog;
        private final View decor;
        private final DialogConfig config;
        private float downX;
        private float downY;
        private int startX;
        private int startY;
        private boolean dragging;

        DragTouchListener(AlertDialog dialog, View decor, DialogConfig config) {
            this.dialog = dialog;
            this.decor = decor;
            this.config = config;
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (config.dragEnabled != null ? !config.dragEnabled : !isDragEnabled(dialog.getContext())) return false;
            WindowManager.LayoutParams lp =
                    dialog.getWindow() == null ? null : dialog.getWindow().getAttributes();
            if (lp == null) return false;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getRawX();
                    downY = event.getRawY();
                    startX = lp.x;
                    startY = lp.y;
                    dragging = false;
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int dx = Math.round(event.getRawX() - downX);
                    int dy = Math.round(event.getRawY() - downY);
                    if (Math.abs(dx) > 3 || Math.abs(dy) > 3) dragging = true;
                    if (dragging) {
                        lp.x = startX + dx;
                        lp.y = startY + dy;
                        constrain(dialog.getContext(), lp);
                        dialog.getWindow().setAttributes(lp);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                    return dragging;
                default:
                    return false;
            }
        }

        private void constrain(Context context, WindowManager.LayoutParams lp) {
            int width = context.getResources().getDisplayMetrics().widthPixels;
            int height = context.getResources().getDisplayMetrics().heightPixels;
            int dialogWidth = decor.getWidth();
            int dialogHeight = decor.getHeight();
            int halfWidth = dialogWidth / 2;
            int halfHeight = dialogHeight / 2;
            lp.x = Math.max(-width / 2 + halfWidth, Math.min(width / 2 - halfWidth, lp.x));
            lp.y = Math.max(-height / 2 + halfHeight, Math.min(height / 2 - halfHeight, lp.y));
        }
    }

    /** Subclass keeps original chaining calls while automatically enhancing create/show. */
    public static class EnhancedBuilder extends MaterialAlertDialogBuilder {
        private final DialogConfig config = new DialogConfig();

        public EnhancedBuilder(@NonNull Context context) {
            super(context);
        }

        public EnhancedBuilder setDialogWidthDp(int widthDp) {
            config.widthDp = widthDp;
            return this;
        }

        public EnhancedBuilder setDialogCornerRadiusDp(int radiusDp) {
            config.cornerRadiusDp = radiusDp;
            return this;
        }

        public EnhancedBuilder setDialogAlpha(int alphaPercent) {
            config.alphaPercent = alphaPercent;
            return this;
        }

        public EnhancedBuilder setDimAlpha(int alphaPercent) {
            config.dimAlphaPercent = alphaPercent;
            return this;
        }

        public EnhancedBuilder setBorderAlpha(int alphaPercent) {
            config.borderAlphaPercent = alphaPercent;
            return this;
        }

        public EnhancedBuilder setDragEnabled(boolean enabled) {
            config.dragEnabled = enabled;
            return this;
        }

        @Override
        public AlertDialog create() {
            AlertDialog dialog = super.create();
            enhance(dialog, config);
            return dialog;
        }

        @Override
        public AlertDialog show() {
            AlertDialog dialog = super.show();
            enhance(dialog, config);
            return dialog;
        }
    }

    public static final class DialogConfig {
        private Integer widthDp;
        private Integer cornerRadiusDp;
        private Integer alphaPercent;
        private Integer dimAlphaPercent;
        private Integer borderAlphaPercent;
        private Boolean dragEnabled;
    }
}
