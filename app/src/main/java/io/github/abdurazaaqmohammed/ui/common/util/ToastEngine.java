package io.github.abdurazaaqmohammed.ui.common.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.preference.PreferenceManager;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayDeque;

/**
 * Single toast queue used by the original MP Manager toast calls.
 * It preserves {@link Toast} duration constants and the normal makeText API.
 */
public final class ToastEngine {
    public static final int LENGTH_SHORT = Toast.LENGTH_SHORT;
    public static final int LENGTH_LONG = Toast.LENGTH_LONG;

    private static final ArrayDeque<EnhancedToast> QUEUE = new ArrayDeque<>();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static EnhancedToast current;

    private ToastEngine() {
    }

    public enum Style {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }

    public static Toast makeText(Context context, CharSequence text, int duration) {
        return makeText(context, text, duration, Style.INFO);
    }

    public static Toast makeText(Context context, @StringRes int text, int duration) {
        CharSequence resolved = context == null ? "" : context.getText(text);
        return makeText(context, resolved, duration, Style.INFO);
    }

    public static Toast makeText(Context context, CharSequence text, int duration, Style style) {
        Context safeContext = context instanceof Activity
                ? context
                : (context != null ? context.getApplicationContext() : null);
        return makeToast(safeContext, text, duration, style);
    }

    public static Toast success(Context context, CharSequence text) {
        return makeText(context, text, LENGTH_SHORT, Style.SUCCESS);
    }

    public static Toast warning(Context context, CharSequence text) {
        return makeText(context, text, LENGTH_SHORT, Style.WARNING);
    }

    public static Toast error(Context context, CharSequence text) {
        return makeText(context, text, LENGTH_LONG, Style.ERROR);
    }

    private static Toast makeToast(Context context, CharSequence text, int duration, Style style) {
        if (context == null) return Toast.makeText(context, text == null ? "" : text, duration);
        return new EnhancedToast(context, text == null ? "" : text, duration, style);
    }

    private static final class EnhancedToast extends Toast {
        private final Style style;
        private final CharSequence text;
        private final int duration;
        private boolean queued;

        EnhancedToast(Context context, CharSequence text, int duration, Style style) {
            super(context);
            this.text = text;
            this.style = style;
            this.duration = duration;
            setDuration(duration);
            buildView(context);
        }

        private void buildView(Context context) {
        try {
            float density = context.getResources().getDisplayMetrics().density;
            int padding = (int) (16 * density);
            MaterialCardView card = new MaterialCardView(context);
            card.setRadius(dp(context, readInt(context, "mp_toast_corner_radius", 16)));
            card.setCardElevation(dp(context, 6));
            card.setMaxCardElevation(dp(context, 10));
            card.setCardBackgroundColor(styleColor(context, style));
            card.setAlpha(readInt(context, "mp_toast_alpha", 96) / 100f);
            card.setUseCompatPadding(false);

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(padding, (int) (12 * density), padding, (int) (12 * density));

            TextView icon = new TextView(context);
            icon.setText(iconFor(style));
            icon.setTextSize(16);
            icon.setTextColor(Color.WHITE);
            icon.setPadding(0, 0, (int) (10 * density), 0);

            TextView message = new TextView(context);
            message.setText(text);
            message.setTextColor(Color.WHITE);
            message.setTextSize(14);
            message.setMaxLines(8);
            message.setLineSpacing(2 * density, 1f);
            message.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            message.setTextDirection(isRtl(text) ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LOCALE);
            message.setTextAlignment(isRtl(text) ? View.TEXT_ALIGNMENT_VIEW_START : View.TEXT_ALIGNMENT_VIEW_START);

            row.addView(icon);
            row.addView(message);
            card.addView(row);
            setView(card);
            setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, dp(context, 28));
            setMargin(0.04f, 0.02f);
        } catch (Exception ignored) {
        }
        }

        @Override
        public void show() {
            synchronized (QUEUE) {
                if (queued) return;
                queued = true;
                if (QUEUE.size() >= 3) QUEUE.removeFirst();
                QUEUE.addLast(this);
            }
            MAIN.post(() -> enqueueAndShow(this));
        }

        @Override
        public void cancel() {
            synchronized (QUEUE) {
                if (current == this) current = null;
            }
            superCancel();
        }

        void superShow() {
            super.show();
        }

        void superCancel() {
            super.cancel();
        }
    }

    private static boolean isRtl(CharSequence text) {
        if (text == null) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c) && Character.getDirectionality(c) == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                    || Character.isLetter(c) && Character.getDirectionality(c) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
                return true;
            }
        }
        return false;
    }

    private static String iconFor(Style style) {
        switch (style) {
            case SUCCESS: return "\u2713";
            case WARNING: return "\u26A0";
            case ERROR: return "\u2715";
            default: return "\u2139";
        }
    }

    private static int styleColor(Context context, Style style) {
        switch (style) {
            case SUCCESS: return 0xE62E7D32;
            case WARNING: return 0xE6ED6C02;
            case ERROR: return 0xE6C62828;
            default: return 0xE6383A40;
        }
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static int readInt(Context context, String key, int fallback) {
        try {
            return PreferenceManager.getDefaultSharedPreferences(context).getInt(key, fallback);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static void enqueueAndShow(EnhancedToast toast) {
        MAIN.post(() -> {
            synchronized (QUEUE) {
                if (current != null) return;
                current = toast;
            }
            toast.setDuration(toast.duration);
            toast.superShow();
            View view = toast.getView();
            if (view != null) {
                view.setAlpha(0f);
                view.setTranslationY(dp(view.getContext(), -8));
                view.animate().alpha(1f).translationY(0f).setDuration(180).start();
            }
            long visible = toast.duration == Toast.LENGTH_LONG ? 3800L : 2200L;
            MAIN.postDelayed(() -> hideCurrent(toast), visible);
        });
    }

    private static void hideCurrent(EnhancedToast toast) {
        MAIN.post(() -> {
            synchronized (QUEUE) {
                if (current != toast) return;
                QUEUE.remove(toast);
            }
            View view = toast.getView();
            if (view != null) {
                view.animate().alpha(0f).translationY(dp(view.getContext(), -6))
                        .setDuration(160).withEndAction(() -> {
                            toast.superCancel();
                            synchronized (QUEUE) {
                                if (current == toast) current = null;
                            }
                            MAIN.post(() -> {
                                synchronized (QUEUE) {
                                    if (current != null) return;
                                    if (!QUEUE.isEmpty()) {
                                        EnhancedToast next = QUEUE.peekFirst();
                                        if (next != null) QUEUE.remove(next);
                                        enqueueAndShow(next);
                                    }
                                }
                            });
                        }).start();
            } else {
                toast.superCancel();
                synchronized (QUEUE) {
                    current = null;
                }
                EnhancedToast next;
                synchronized (QUEUE) {
                    next = QUEUE.isEmpty() ? null : QUEUE.removeFirst();
                }
                if (next != null) enqueueAndShow(next);
            }
        });
    }
}
