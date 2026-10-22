package dev.guianimations.modernui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import icyllis.modernui.graphics.drawable.GradientDrawable;
import icyllis.modernui.graphics.text.FontFamily;
import icyllis.modernui.text.Typeface;
import icyllis.modernui.view.View;
import icyllis.modernui.widget.TextView;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Shared tokens and packaged Inter font. Lengths map to ModernUI dp; text maps to sp. */
public final class UiTheme {
    public static final UiTheme DEFAULT = load();
    private final JsonObject tokens;
    private final Typeface typeface;

    private UiTheme(JsonObject tokens, Typeface typeface) {
        this.tokens = tokens;
        this.typeface = typeface;
    }

    private static UiTheme load() {
        try (InputStream input = resource("tokens.json");
             InputStream font = resource("fonts/Inter-Regular.ttf")) {
            JsonObject tokens = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            if (tokens.get("schemaVersion").getAsInt() != 1) {
                throw new IllegalStateException("Unsupported Gui-animations token schema");
            }
            return new UiTheme(tokens, Typeface.createTypeface(FontFamily.createFamily(font, false)));
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load Gui-animations tokens or bundled Inter font", exception);
        }
    }

    private static InputStream resource(String path) {
        return Objects.requireNonNull(UiTheme.class.getResourceAsStream("/assets/guianimations/" + path),
                "Missing packaged UI resource: " + path);
    }

    public int color(String role) {
        String value = tokens.getAsJsonObject("colors").get(role).getAsString();
        return (int) (Long.parseLong(value.substring(1), 16) | (value.length() == 7 ? 0xff000000L : 0L));
    }

    public int metric(String role) { return tokens.getAsJsonObject("metrics").get(role).getAsInt(); }
    public float radius(String role) { return tokens.getAsJsonObject("radii").get(role).getAsFloat(); }
    public long duration(String role) { return tokens.getAsJsonObject("motion").getAsJsonObject(role).get("durationMs").getAsLong(); }

    public void text(TextView view, String size, boolean secondary) {
        view.setTypeface(typeface);
        view.setTextSize(tokens.getAsJsonObject("typography").getAsJsonObject("sizes").get(size).getAsFloat());
        view.setTextColor(color(secondary ? "textSecondary" : "textPrimary"));
    }

    public GradientDrawable panel(View view, String color, String radius, boolean border) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color(color));
        drawable.setCornerRadius(view.dp(this.radius(radius)));
        if (border) drawable.setStroke(view.dp(metric("borderWidth")), color("border"));
        return drawable;
    }
}
