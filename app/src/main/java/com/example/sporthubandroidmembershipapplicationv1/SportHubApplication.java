package com.example.sporthubandroidmembershipapplicationv1;

import android.app.Activity;
import android.app.Application;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsSeekBar;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.core.graphics.ColorUtils;

import java.util.IdentityHashMap;
import java.util.Map;

/** Adds touch feedback to custom buttons without changing their click actions. */
public class SportHubApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            private final Map<Activity, RippleObserver> observers =
                    new IdentityHashMap<>();

            @Override
            public void onActivityCreated(Activity activity, Bundle state) {
                View root = activity.getWindow().getDecorView();
                RippleObserver observer = new RippleObserver(root);
                observers.put(activity, observer);
                root.getViewTreeObserver().addOnGlobalLayoutListener(observer);
            }

            @Override
            public void onActivityStarted(Activity activity) {
                RippleObserver observer = observers.get(activity);
                if (observer != null) {
                    observer.onGlobalLayout();
                }
            }

            @Override
            public void onActivityResumed(Activity activity) {
                RippleObserver observer = observers.get(activity);
                if (observer != null) {
                    observer.onGlobalLayout();
                }
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                RippleObserver observer = observers.remove(activity);
                if (observer != null) {
                    ViewTreeObserver tree = observer.root.getViewTreeObserver();
                    if (tree.isAlive()) {
                        tree.removeOnGlobalLayoutListener(observer);
                    }
                }
            }

            @Override
            public void onActivityPaused(Activity activity) { }

            @Override
            public void onActivityStopped(Activity activity) { }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
        });
    }

    private static final class RippleObserver
            implements ViewTreeObserver.OnGlobalLayoutListener {
        private final View root;

        private RippleObserver(View root) {
            this.root = root;
        }

        @Override
        public void onGlobalLayout() {
            applyRipples(root);
        }
    }

    private static void applyRipples(View view) {
        if (view.isClickable() || view.isLongClickable()) {
            addRippleIfNeeded(view);
        }

        // New fragment views and dynamically generated buttons are covered too.
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                applyRipples(group.getChildAt(index));
            }
        }
    }

    private static void addRippleIfNeeded(View view) {
        // Input controls keep their own selection and touch feedback.
        if (view instanceof EditText
                || view instanceof CompoundButton
                || view instanceof AbsSeekBar
                || view instanceof Spinner) {
            return;
        }

        Drawable foreground = view.getForeground();
        if (foreground instanceof ButtonRipple) {
            ((ButtonRipple) foreground).updateColor(rippleColor(view));
            return;
        }

        // Material/default buttons and the Rewards cards already have ripples.
        if (containsRipple(foreground) || containsRipple(view.getBackground())) {
            return;
        }

        // Foreground feedback preserves the original background, padding and tint.
        if (foreground != null) {
            view.setForeground(null);
        }
        view.setForeground(new ButtonRipple(
                rippleColor(view), foreground, createMask(view)
        ));
    }

    private static boolean containsRipple(Drawable drawable) {
        if (drawable == null) {
            return false;
        }
        if (drawable instanceof RippleDrawable) {
            return true;
        }
        if (drawable instanceof InsetDrawable) {
            return containsRipple(((InsetDrawable) drawable).getDrawable());
        }
        if (drawable instanceof LayerDrawable) {
            LayerDrawable layers = (LayerDrawable) drawable;
            for (int index = 0; index < layers.getNumberOfLayers(); index++) {
                if (containsRipple(layers.getDrawable(index))) {
                    return true;
                }
            }
        }
        Drawable current = drawable.getCurrent();
        return current != drawable && containsRipple(current);
    }

    private static Drawable createMask(View view) {
        Drawable background = view.getBackground();
        if (background != null
                && background.getConstantState() != null
                && background.getOpacity() != PixelFormat.TRANSPARENT) {
            // Clone the shape so rounded buttons retain rounded ripple edges.
            Drawable mask = background.getConstantState()
                    .newDrawable(view.getResources()).mutate();
            mask.setTint(Color.WHITE);
            return mask;
        }

        GradientDrawable mask = new GradientDrawable();
        mask.setColor(Color.WHITE);
        mask.setCornerRadius(
                8 * view.getResources().getDisplayMetrics().density
        );
        return mask;
    }

    private static int rippleColor(View view) {
        ColorStateList tint = view.getBackgroundTintList();
        Integer backgroundColor;

        // Keep unknown colours nullable. Mixing Integer and int in a conditional
        // expression would unbox a null colour and crash on custom CardViews.
        if (tint != null) {
            backgroundColor = tint.getColorForState(
                    view.getDrawableState(), tint.getDefaultColor()
            );
        } else {
            backgroundColor = drawableColor(
                    view.getBackground(), view.getDrawableState()
            );
        }

        boolean darkBackground = backgroundColor != null
                && Color.alpha(backgroundColor) >= 128
                && ColorUtils.calculateLuminance(backgroundColor) < 0.35;

        return darkBackground ? 0x38FFFFFF : 0x24000000;
    }

    private static Integer drawableColor(Drawable drawable, int[] state) {
        if (drawable instanceof ColorDrawable) {
            return ((ColorDrawable) drawable).getColor();
        }
        if (drawable instanceof GradientDrawable) {
            ColorStateList color = ((GradientDrawable) drawable).getColor();
            return color == null ? null
                    : color.getColorForState(state, color.getDefaultColor());
        }
        if (drawable instanceof InsetDrawable) {
            return drawableColor(((InsetDrawable) drawable).getDrawable(), state);
        }
        if (drawable != null && drawable.getCurrent() != drawable) {
            return drawableColor(drawable.getCurrent(), state);
        }
        return null;
    }

    private static final class ButtonRipple extends RippleDrawable {
        private int currentColor;

        private ButtonRipple(int color, Drawable content, Drawable mask) {
            super(ColorStateList.valueOf(color), content, mask);
            currentColor = color;
        }

        private void updateColor(int color) {
            // Navigation and category buttons change colour when selected.
            if (currentColor != color) {
                currentColor = color;
                setColor(ColorStateList.valueOf(color));
            }
        }
    }
}