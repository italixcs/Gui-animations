package dev.guianimations.modernui;

import icyllis.modernui.R;
import icyllis.modernui.core.Context;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.fragment.OnBackPressedCallback;
import icyllis.modernui.fragment.OnBackPressedDispatcher;
import icyllis.modernui.graphics.drawable.GradientDrawable;
import icyllis.modernui.graphics.drawable.ColorDrawable;
import icyllis.modernui.graphics.drawable.StateListDrawable;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.KeyEvent;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.MeasureSpec;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.ScrollView;
import icyllis.modernui.widget.SeekBar;
import icyllis.modernui.widget.Switch;
import icyllis.modernui.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static icyllis.modernui.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static icyllis.modernui.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * Reusable reference screen. The host owns Minecraft lifecycle and disk I/O;
 * this Fragment owns ModernUI views, their focus, and cancellable motion.
 */
public final class ShowcaseFragment extends Fragment {
    private final UiTheme theme = UiTheme.DEFAULT;
    private final Function<DemoSettings, CompletableFuture<Void>> save;
    private final Runnable closeHost;
    private final OnBackPressedDispatcher backDispatcher;
    private DemoSettings settings;
    private FrameLayout scene;
    private LinearLayout shell;
    private LinearLayout content;
    private LinearLayout body;
    private LinearLayout navigation;
    private ScrollView scroll;
    private TextView status;
    private TextView headerEyebrow, headerTitle, headerSubtitle;
    private LinearLayout header, footer;
    private Boolean compactLayout;
    private TextView toast;
    private UiMotion screenMotion, contentMotion, popupMotion, toastMotion;
    private ModalLayer modal;
    private View previousFocus;
    private UiButton resetButton;
    private OnBackPressedCallback backCallback;
    private final List<UiButton> navigationButtons = new ArrayList<>();
    private String section = "General";
    private boolean destroyed, closing, saving;
    private Runnable toastDismiss;
    private int generation;

    public ShowcaseFragment(DemoSettings initial, Function<DemoSettings, CompletableFuture<Void>> save,
                            Runnable closeHost, OnBackPressedDispatcher backDispatcher) {
        this.settings = initial;
        this.save = save;
        this.closeHost = closeHost;
        this.backDispatcher = backDispatcher;
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, DataSet savedState) {
        destroyed = false;
        generation++;
        closing = false;
        saving = false;
        Context context = requireContext();
        scene = new FrameLayout(context);
        scene.setBackground(new ColorDrawable(theme.color("background")));
        scene.setPadding(scene.dp(16), scene.dp(16), scene.dp(16), scene.dp(16));

        FrameLayout bounds = new CappedPanel(context);
        FrameLayout.LayoutParams boundsParams = new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        boundsParams.gravity = Gravity.CENTER;
        scene.addView(bounds, boundsParams);

        shell = vertical(context);
        shell.setBackground(theme.panel(shell, "panel", "panel", true));
        bounds.addView(shell, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        buildHeader(context);

        body = new ResponsiveBody(context);
        navigation = vertical(context);
        navigation.setPadding(body.dp(12), body.dp(12), body.dp(12), body.dp(12));
        navigation.setBackground(new ColorDrawable(theme.color("background")));
        for (String name : List.of("General", "Style", "Motion")) {
            UiButton item = button(context, name, false, () -> selectSection(name));
            item.setTooltipText("Open " + name.toLowerCase() + " settings");
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            params.bottomMargin = body.dp(8);
            navigation.addView(item, params);
            navigationButtons.add(item);
        }
        body.addView(navigation);
        scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        content = vertical(context);
        content.setPadding(content.dp(20), content.dp(16), content.dp(20), content.dp(16));
        scroll.addView(content, new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        body.addView(scroll);
        shell.addView(body, new LinearLayout.LayoutParams(MATCH_PARENT, 0, 1));

        buildFooter(context);
        fillSection();
        screenMotion = new UiMotion(shell);
        shell.post(() -> {
            if (!destroyed) screenMotion.enter(theme.duration("screenOpen"), shell.dp(8), settings.reducedMotion());
        });
        return scene;
    }

    @Override public void onViewCreated(View view, DataSet state) {
        super.onViewCreated(view, state);
        backCallback = new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (modal != null) dismissModal(false);
                else requestClose();
            }
        };
        backDispatcher.addCallback(getViewLifecycleOwner(), backCallback);
    }

    private void buildHeader(Context context) {
        header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(header.dp(20), header.dp(16), header.dp(20), header.dp(16));
        LinearLayout copy = vertical(context);
        TextView eyebrow = label(context, "GUI / ANIMATIONS", "caption", true);
        headerEyebrow = eyebrow;
        eyebrow.setTextColor(theme.color("accent"));
        copy.addView(eyebrow);
        TextView title = label(context, "A calmer kind of Minecraft.", "title", false);
        headerTitle = title;
        copy.addView(title);
        headerSubtitle = label(context, "Clean type. Soft edges. Every interaction considered.", "body", true);
        copy.addView(headerSubtitle);
        header.addView(copy, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1));
        UiButton close = button(context, "Close", false, this::requestClose);
        close.setTooltipText("Close this screen · Escape");
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        closeParams.leftMargin = header.dp(12);
        header.addView(close, closeParams);
        shell.addView(header, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        divider(shell);
    }

    private void buildFooter(Context context) {
        divider(shell);
        footer = new LinearLayout(context);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(footer.dp(20), footer.dp(12), footer.dp(20), footer.dp(12));
        status = label(context, "Changes apply to this showcase.", "caption", true);
        footer.addView(status, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1));
        UiButton reset = button(context, "Reset", false, this::showResetDialog);
        resetButton = reset;
        reset.setTooltipText("Restore showcase settings to their defaults");
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        resetParams.leftMargin = footer.dp(8);
        footer.addView(reset, resetParams);
        UiButton save = button(context, "Save changes", true, this::saveSettings);
        save.setTooltipText("Save these settings locally");
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        saveParams.leftMargin = footer.dp(8);
        footer.addView(save, saveParams);
        shell.addView(footer);
    }

    private void selectSection(String name) {
        if (section.equals(name) || closing || modal != null) return;
        section = name;
        fillSection();
        scroll.scrollTo(0, 0);
        if (contentMotion != null) contentMotion.cancel();
        contentMotion = new UiMotion(content);
        contentMotion.enter(theme.duration("tab"), 0, settings.reducedMotion());
    }

    private void fillSection() {
        content.removeAllViews();
        for (int i = 0; i < navigationButtons.size(); i++) {
            UiButton item = navigationButtons.get(i);
            item.setText((List.of("General", "Style", "Motion").get(i).equals(section) ? "• " : "")
                    + List.of("General", "Style", "Motion").get(i));
        }
        switch (section) {
            case "Style" -> appearance();
            case "Motion" -> motion();
            default -> general();
        }
    }

    private void general() {
        heading("Made for the way you play", "A small collection of real, reusable interface components.");
        toggleRow("Notifications", "Show a toast after saving your preferences.", settings.notifications(), value ->
                settings = new DemoSettings(value, settings.reducedMotion(), settings.animationStrength()));
        toggleRow("Reduced motion", "Keep state feedback and remove spatial animations.", settings.reducedMotion(), this::setReducedMotion);
        strengthRow();
        TextView hint = label(requireContext(), "Tip: use Tab to move through controls, Enter or Space to activate, and Escape to go back.", "caption", true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        lp.topMargin = content.dp(12);
        content.addView(hint, lp);
    }

    private void appearance() {
        heading("Clarity at every scale", "A shared theme keeps every screen in your client consistent.");
        LinearLayout preview = card();
        preview.addView(label(requireContext(), "Inter · Regular", "section", false));
        preview.addView(label(requireContext(), "The quick brown fox jumps over the lazy dog.", "body", false));
        preview.addView(label(requireContext(), "Aa  0123456789  —  Clean, proportional text", "caption", true));
        UiButton sample = button(requireContext(), "Preview notification", true, () -> showToast("Everything feels connected.", false));
        LinearLayout.LayoutParams sampleParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        sampleParams.topMargin = preview.dp(16);
        preview.addView(sample, sampleParams);
        content.addView(preview, cardParams());
        TextView detail = label(requireContext(), "This screen uses the bundled Inter font, anti-aliased ModernUI shapes, and shared design tokens. It respects host density instead of scaling the interface twice.", "body", true);
        content.addView(detail, cardParams());
    }

    private void motion() {
        heading("Motion with a purpose", "Fast feedback, calm transitions, and a comfortable alternative.");
        toggleRow("Reduced motion", "Apply changes immediately without slides or springs.", settings.reducedMotion(), this::setReducedMotion);
        strengthRow();
        UiButton preview = button(requireContext(), "Replay entrance", true, () -> {
            if (contentMotion != null) contentMotion.cancel();
            contentMotion = new UiMotion(content);
            contentMotion.enter(theme.duration("screenOpen"), content.dp(8) * settings.animationStrength() / 100f, settings.reducedMotion());
        });
        content.addView(preview, cardParams());
        content.addView(label(requireContext(), "Animations follow elapsed time rather than game ticks. Repeated input retargets the current visual state, and closing a screen cancels its pending work.", "body", true), cardParams());
    }

    private void heading(String title, String description) {
        content.addView(label(requireContext(), title, "section", false));
        TextView detail = label(requireContext(), description, "body", true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        params.topMargin = content.dp(4);
        params.bottomMargin = content.dp(16);
        content.addView(detail, params);
    }

    private void toggleRow(String title, String description, boolean checked, java.util.function.Consumer<Boolean> changed) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(row.dp(14), row.dp(12), row.dp(14), row.dp(12));
        row.setBackground(theme.panel(row, "raised", "control", false));
        LinearLayout copy = vertical(requireContext());
        copy.addView(label(requireContext(), title, "body", false));
        copy.addView(label(requireContext(), description, "caption", true));
        row.addView(copy, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1));
        Switch control = new Switch(requireContext()) {
            @Override public void setChecked(boolean value) {
                super.setChecked(value);
                if (settings.reducedMotion()) jumpDrawablesToCurrentState();
            }
        };
        control.setContentDescription(title);
        control.setTooltipText(description);
        control.setChecked(checked);
        control.setFocusable(true);
        control.setFocusableInTouchMode(true);
        focusOutline(control);
        control.setThumbTintList(new ColorStateList(new int[][] {new int[]{R.attr.state_checked}, new int[]{}},
                new int[]{theme.color("textOnAccent"), theme.color("textPrimary")}));
        control.setTrackTintList(new ColorStateList(new int[][] {new int[]{R.attr.state_checked}, new int[]{}},
                new int[]{theme.color("accent"), theme.color("border")}));
        control.setOnCheckedChangeListener((button, value) -> { changed.accept(value); markChanged(); });
        LinearLayout.LayoutParams controlParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        controlParams.leftMargin = row.dp(12);
        row.addView(control, controlParams);
        content.addView(row, cardParams());
    }

    private void strengthRow() {
        LinearLayout row = card();
        TextView value = label(requireContext(), "Animation distance   " + settings.animationStrength() + "%", "body", false);
        row.addView(value);
        row.addView(label(requireContext(), "Adjust the travel distance in the motion preview.", "caption", true));
        SeekBar slider = new SeekBar(requireContext());
        slider.setMax(100);
        slider.setProgress(settings.animationStrength());
        slider.setContentDescription("Animation distance");
        slider.setFocusableInTouchMode(true);
        focusOutline(slider);
        slider.setTooltipText("Use Left and Right arrows to adjust");
        slider.setProgressTintList(ColorStateList.valueOf(theme.color("accent")));
        GradientDrawable thumb = new GradientDrawable();
        thumb.setShape(GradientDrawable.CIRCLE);
        thumb.setUseLevelForShape(false);
        thumb.setSize(slider.dp(16), slider.dp(16));
        thumb.setColor(theme.color("accent"));
        thumb.setStroke(slider.dp(2), theme.color("textOnAccent"));
        slider.setThumb(thumb);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                settings = new DemoSettings(settings.notifications(), settings.reducedMotion(), progress);
                value.setText("Animation distance   " + progress + "%");
                markChanged();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, row.dp(32));
        params.topMargin = row.dp(8);
        row.addView(slider, params);
        content.addView(row, cardParams());
    }

    private void setReducedMotion(boolean value) {
        settings = new DemoSettings(settings.notifications(), value, settings.animationStrength());
        if (value) {
            if (screenMotion != null) screenMotion.finish();
            if (contentMotion != null) contentMotion.finish();
            if (popupMotion != null) popupMotion.finish();
            if (toastMotion != null) toastMotion.finish();
            settleControls(shell);
        }
    }

    private void settleControls(View view) {
        if (view instanceof UiButton button) button.settleMotion();
        if (view instanceof Switch control) control.jumpDrawablesToCurrentState();
        if (view instanceof ViewGroup group) {
            for (int index = 0; index < group.getChildCount(); index++) settleControls(group.getChildAt(index));
        }
    }

    private void markChanged() {
        status.setText("Unsaved changes");
        status.setTextColor(theme.color("textSecondary"));
    }

    private void saveSettings() {
        if (saving || closing || destroyed) return;
        saving = true;
        DemoSettings snapshot = settings;
        int saveGeneration = generation;
        FrameLayout saveScene = scene;
        status.setText("Saving…");
        save.apply(snapshot).whenComplete((unused, failure) -> saveScene.post(() -> {
            if (destroyed || generation != saveGeneration) return;
            saving = false;
            if (failure != null) {
                status.setText("Could not save. Check the game log.");
                status.setTextColor(theme.color("danger"));
                showToast("Saving failed. Your changes are still here.", true);
            } else {
                status.setText(snapshot.equals(settings) ? "Saved locally" : "Saved · newer changes pending");
                status.setTextColor(theme.color("success"));
                if (settings.notifications()) showToast("Preferences saved. You're all set.", false);
            }
        }));
    }

    private void showResetDialog() {
        if (modal != null || closing) return;
        previousFocus = resetButton;
        body.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        shell.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        modal = new ModalLayer(requireContext());
        modal.setBackground(new ColorDrawable(0xb0000000));
        modal.setClickable(true);
        modal.setPadding(modal.dp(16), modal.dp(16), modal.dp(16), modal.dp(16));
        LinearLayout dialog = card();
        dialog.setBackground(theme.panel(dialog, "panel", "popup", true));
        dialog.setPadding(dialog.dp(22), dialog.dp(20), dialog.dp(22), dialog.dp(20));
        dialog.addView(label(requireContext(), "Start fresh?", "section", false));
        dialog.addView(label(requireContext(), "Restore this showcase's default preferences. Save afterwards to keep them.", "body", true));
        LinearLayout actions = new LinearLayout(requireContext());
        actions.setGravity(Gravity.RIGHT);
        UiButton cancel = button(requireContext(), "Keep changes", false, () -> dismissModal(false));
        UiButton confirm = button(requireContext(), "Reset defaults", true, () -> dismissModal(true));
        modal.focusTargets = List.of(cancel, confirm);
        actions.addView(cancel);
        LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        confirmParams.leftMargin = dialog.dp(8);
        actions.addView(confirm, confirmParams);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        actionsParams.topMargin = dialog.dp(20);
        dialog.addView(actions, actionsParams);
        FrameLayout.LayoutParams dialogParams = new FrameLayout.LayoutParams(modal.dp(330), WRAP_CONTENT);
        dialogParams.gravity = Gravity.CENTER;
        modal.addView(dialog, dialogParams);
        scene.addView(modal, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        popupMotion = new UiMotion(dialog);
        popupMotion.enter(theme.duration("popupOpen"), dialog.dp(4), settings.reducedMotion());
        cancel.requestFocus();
    }

    private void dismissModal(boolean reset) {
        if (modal == null || closing) return;
        ModalLayer current = modal;
        if (current.dismissing) return;
        current.dismissing = true;
        for (UiButton item : current.focusTargets) item.setEnabled(false);
        popupMotion.exit(theme.duration("popupClose"), current.dp(4), settings.reducedMotion(), () -> {
            if (destroyed || modal != current) return;
            scene.removeView(current);
            modal = null;
            shell.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            body.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            if (reset) { settings = DemoSettings.defaults(); fillSection(); markChanged(); }
            if (previousFocus != null && previousFocus.isAttachedToWindow()) previousFocus.requestFocus();
        });
    }

    private void showToast(String message, boolean error) {
        if (destroyed || closing) return;
        if (toastDismiss != null) scene.removeCallbacks(toastDismiss);
        if (toastMotion != null) toastMotion.cancel();
        if (toast != null) scene.removeView(toast);
        toast = label(requireContext(), message, "body", false);
        toast.setTextColor(theme.color(error ? "danger" : "success"));
        toast.setPadding(toast.dp(16), toast.dp(12), toast.dp(16), toast.dp(12));
        toast.setBackground(theme.panel(toast, error ? "dangerSurface" : "successSurface", "popup", true));
        toast.setMaxWidth(toast.dp(theme.metric("toastMaxWidth")));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.bottomMargin = toast.dp(8);
        scene.addView(toast, params);
        toastMotion = new UiMotion(toast);
        toastMotion.enter(theme.duration("toastOpen"), toast.dp(8), settings.reducedMotion());
        TextView current = toast;
        toastDismiss = () -> {
            if (destroyed || toast != current) return;
            toastMotion.exit(theme.duration("toastClose"), current.dp(4), settings.reducedMotion(), () -> {
                if (!destroyed && toast == current) { scene.removeView(current); toast = null; }
            });
        };
        scene.postDelayed(toastDismiss, 4000);
    }

    private void requestClose() {
        if (closing || destroyed) return;
        closing = true;
        shell.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        screenMotion.exit(theme.duration("screenClose"), shell.dp(4), settings.reducedMotion(), closeHost);
    }

    @Override public void onDestroyView() {
        destroyed = true;
        if (screenMotion != null) screenMotion.cancel();
        if (contentMotion != null) contentMotion.cancel();
        if (popupMotion != null) popupMotion.cancel();
        if (toastMotion != null) toastMotion.cancel();
        if (toastDismiss != null) scene.removeCallbacks(toastDismiss);
        if (backCallback != null) backCallback.remove();
        navigationButtons.clear();
        modal = null;
        toast = null;
        previousFocus = null;
        super.onDestroyView();
    }

    private LinearLayout vertical(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private TextView label(Context context, String text, String size, boolean secondary) {
        TextView view = new TextView(context);
        view.setText(text);
        theme.text(view, size, secondary);
        return view;
    }

    private UiButton button(Context context, String text, boolean primary, Runnable action) {
        UiButton button = new UiButton(context, text, primary, () -> settings.reducedMotion());
        button.setOnClickListener(view -> { if (!closing && !destroyed) action.run(); });
        return button;
    }

    private LinearLayout card() {
        LinearLayout card = vertical(requireContext());
        card.setPadding(card.dp(14), card.dp(12), card.dp(14), card.dp(12));
        card.setBackground(theme.panel(card, "raised", "control", false));
        return card;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        params.bottomMargin = content.dp(10);
        return params;
    }

    private void divider(LinearLayout parent) {
        View divider = new View(requireContext());
        divider.setBackground(new ColorDrawable(theme.color("raised")));
        parent.addView(divider, new LinearLayout.LayoutParams(MATCH_PARENT, parent.dp(1)));
    }

    private void focusOutline(View control) {
        control.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN && view.isEnabled()) view.requestFocus();
            return false;
        });
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(0x00000000);
        focused.setCornerRadius(control.dp(theme.radius("control")));
        focused.setStroke(control.dp(theme.metric("focusWidth")), theme.color("focus"));
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {R.attr.state_focused}, focused);
        states.addState(new int[] {}, new ColorDrawable(0x00000000));
        control.setBackground(states);
        control.setPadding(control.dp(6), control.dp(6), control.dp(6), control.dp(6));
    }

    private final class CappedPanel extends FrameLayout {
        CappedPanel(Context context) { super(context); }
        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int width = Math.min(MeasureSpec.getSize(widthSpec), dp(theme.metric("maxContentWidth")));
            int height = Math.min(MeasureSpec.getSize(heightSpec), dp(680));
            boolean compact = width < dp(480) || height < dp(360);
            if (compactLayout == null || compactLayout != compact) {
                compactLayout = compact;
                headerEyebrow.setVisibility(compact ? GONE : VISIBLE);
                headerSubtitle.setVisibility(compact ? GONE : VISIBLE);
                headerTitle.setText(compact ? "Gui-animations" : "A calmer kind of Minecraft.");
                theme.text(headerTitle, compact ? "section" : "title", false);
                header.setPadding(dp(compact ? 12 : 20), dp(compact ? 8 : 16), dp(compact ? 12 : 20), dp(compact ? 8 : 16));
                footer.setPadding(dp(compact ? 12 : 20), dp(compact ? 8 : 12), dp(compact ? 12 : 20), dp(compact ? 8 : 12));
                status.setVisibility(compact ? GONE : VISIBLE);
                navigation.setPadding(dp(compact ? 8 : 12), dp(compact ? 6 : 12), dp(compact ? 8 : 12), dp(compact ? 6 : 12));
            }
            super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        }
    }

    private final class ResponsiveBody extends LinearLayout {
        private int lastOrientation = -1;
        ResponsiveBody(Context context) { super(context); }
        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int orientation = MeasureSpec.getSize(widthSpec) < dp(theme.metric("sidebarBreakpoint")) ? VERTICAL : HORIZONTAL;
            if (orientation != lastOrientation) {
                lastOrientation = orientation;
                setOrientation(orientation);
                navigation.setOrientation(orientation == VERTICAL ? HORIZONTAL : VERTICAL);
                navigation.setLayoutParams(new LayoutParams(orientation == VERTICAL ? MATCH_PARENT : dp(theme.metric("sidebarWidth")),
                        orientation == VERTICAL ? WRAP_CONTENT : MATCH_PARENT));
                for (UiButton item : navigationButtons) {
                    LayoutParams params = new LayoutParams(orientation == VERTICAL ? 0 : MATCH_PARENT, WRAP_CONTENT,
                            orientation == VERTICAL ? 1 : 0);
                    params.rightMargin = orientation == VERTICAL ? dp(6) : 0;
                    params.bottomMargin = orientation == VERTICAL ? 0 : dp(8);
                    item.setLayoutParams(params);
                }
                scroll.setLayoutParams(new LayoutParams(orientation == VERTICAL ? MATCH_PARENT : 0,
                        orientation == VERTICAL ? 0 : MATCH_PARENT, 1));
            }
            super.onMeasure(widthSpec, heightSpec);
        }
    }

    private final class ModalLayer extends FrameLayout {
        private List<UiButton> focusTargets = List.of();
        private boolean dismissing;
        ModalLayer(Context context) { super(context); }
        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            if (getChildCount() > 0) {
                View dialog = getChildAt(0);
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) dialog.getLayoutParams();
                params.width = Math.max(0, Math.min(dp(330), MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight()));
                dialog.setLayoutParams(params);
            }
            super.onMeasure(widthSpec, heightSpec);
        }
        @Override public boolean dispatchKeyEvent(KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.KEY_TAB) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && !dismissing) {
                    int current = focusTargets.get(0).hasFocus() ? 0 : 1;
                    focusTargets.get(Math.floorMod(current + (event.isShiftPressed() ? -1 : 1), focusTargets.size())).requestFocus();
                }
                return true;
            }
            return super.dispatchKeyEvent(event);
        }
    }
}
