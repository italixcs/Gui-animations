package dev.guianimations.modernui;

import dev.guianimations.core.Easing;
import icyllis.modernui.animation.ObjectAnimator;
import icyllis.modernui.animation.ValueAnimator;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.drawable.GradientDrawable;
import icyllis.modernui.graphics.drawable.LayerDrawable;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.View;
import icyllis.modernui.widget.Button;

import java.util.function.BooleanSupplier;

/** Reusable button with native focus/input semantics and interruptible visual feedback. */
public final class UiButton extends Button {
    private final UiTheme theme;
    private final BooleanSupplier reducedMotion;
    private final boolean primary;
    private GradientDrawable shape;
    private ValueAnimator colorAnimator;
    private ObjectAnimator scaleXAnimator, scaleYAnimator;
    private int displayedColor;
    private GradientDrawable focusRing;

    public UiButton(Context context, String label, boolean primary, BooleanSupplier reducedMotion) {
        super(context);
        this.theme = UiTheme.DEFAULT;
        this.reducedMotion = reducedMotion;
        this.primary = primary;
        setText(label);
        theme.text(this, "body", false);
        setTextColor(theme.color(primary ? "textOnAccent" : "textPrimary"));
        setGravity(Gravity.CENTER);
        setMinHeight(dp(theme.metric("controlHeight")));
        setPadding(dp(14), dp(6), dp(14), dp(6));
        setFocusable(true);
        // ModernUI's host keeps touch mode enabled; explicit focus must opt in.
        setFocusableInTouchMode(true);
        shape = theme.panel(this, primary ? "accent" : "raised", "control", true);
        focusRing = theme.panel(this, "panel", "panel", false);
        GradientDrawable gap = theme.panel(this, "panel", "panel", false);
        LayerDrawable layers = new LayerDrawable(focusRing, gap, shape);
        layers.setLayerInset(1, dp(2), dp(2), dp(2), dp(2));
        layers.setLayerInset(2, dp(4), dp(4), dp(4), dp(4));
        displayedColor = theme.color(primary ? "accent" : "raised");
        setBackground(layers);
    }

    @Override protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (shape == null) return; // The superclass calls this while constructing the view.
        String role = !isEnabled() ? "raised" : primary
                ? isPressed() ? "accentPressed" : isHovered() ? "accentHover" : "accent"
                : isPressed() || isHovered() ? "panel" : "raised";
        int target = theme.color(role);
        if (colorAnimator != null) colorAnimator.cancel();
        if (reducedMotion.getAsBoolean()) {
            displayedColor = target;
            shape.setColor(target);
        } else {
            colorAnimator = ValueAnimator.ofArgb(displayedColor, target);
            colorAnimator.setDuration(theme.duration("hover"));
            colorAnimator.setInterpolator(value -> (float) Easing.CUBIC_OUT.apply(value));
            colorAnimator.addUpdateListener(animation -> {
                displayedColor = (int) animation.getAnimatedValue();
                shape.setColor(displayedColor);
            });
            colorAnimator.start();
        }
        focusRing.setColor(theme.color(isFocused() ? "focus" : "panel"));
        setTextColor(theme.color(!isEnabled() ? "textDisabled" : primary ? "textOnAccent" : "textPrimary"));
    }

    /** Apply a changed reduced-motion preference to transitions already running. */
    public void settleMotion() {
        if (colorAnimator != null) colorAnimator.end();
        if (scaleXAnimator != null) scaleXAnimator.cancel();
        if (scaleYAnimator != null) scaleYAnimator.cancel();
        setScaleX(1);
        setScaleY(1);
    }

    @Override public void setPressed(boolean pressed) {
        boolean changed = isPressed() != pressed;
        super.setPressed(pressed);
        if (theme == null || !changed) return;
        if (scaleXAnimator != null) scaleXAnimator.cancel();
        if (scaleYAnimator != null) scaleYAnimator.cancel();
        float target = pressed && !reducedMotion.getAsBoolean() ? 0.98f : 1f;
        if (reducedMotion.getAsBoolean()) { setScaleX(target); setScaleY(target); return; }
        long duration = theme.duration(pressed ? "press" : "release");
        scaleXAnimator = ObjectAnimator.ofFloat(this, View.SCALE_X, getScaleX(), target);
        scaleYAnimator = ObjectAnimator.ofFloat(this, View.SCALE_Y, getScaleY(), target);
        scaleXAnimator.setDuration(duration);
        scaleYAnimator.setDuration(duration);
        scaleXAnimator.setInterpolator(value -> (float) Easing.CUBIC_OUT.apply(value));
        scaleYAnimator.setInterpolator(value -> (float) Easing.CUBIC_OUT.apply(value));
        scaleXAnimator.start();
        scaleYAnimator.start();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        // Native View otherwise consumes the first release just to acquire touch-mode
        // focus. Acquire on press so the normal single-click dispatch remains intact.
        if (event.getAction() == MotionEvent.ACTION_DOWN && isEnabled()) requestFocus();
        return super.onTouchEvent(event);
    }

    @Override protected void onDetachedFromWindow() {
        if (colorAnimator != null) colorAnimator.cancel();
        if (scaleXAnimator != null) scaleXAnimator.cancel();
        if (scaleYAnimator != null) scaleYAnimator.cancel();
        super.onDetachedFromWindow();
    }
}
