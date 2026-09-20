package org.jd.infestusfrontier.ui;

/** Accessible palette shared by organ screens; colors are opaque ARGB. */
public final class UiTheme {
    public static final int PANEL_BACKGROUND = 0xFF171018;
    public static final int PANEL_EDGE = 0xFF704459;
    public static final int PANEL_INSET = 0xFF291923;
    public static final int TEXT = 0xFFF7E9C8;
    public static final int MUTED_TEXT = 0xFFCDBBA8;
    public static final int FOCUSED_BORDER = 0xFFFFD75E;
    public static final int TOOLTIP_BACKGROUND = 0xFF09070A;
    public static final int TOOLTIP_TEXT = 0xFFFFFFFF;

    public static double contrastRatio(int foreground, int background) {
        double light = luminance(foreground);
        double dark = luminance(background);
        return (Math.max(light, dark) + 0.05) / (Math.min(light, dark) + 0.05);
    }

    private static double luminance(int color) {
        double red = channel((color >>> 16) & 0xff);
        double green = channel((color >>> 8) & 0xff);
        double blue = channel(color & 0xff);
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
    }

    private static double channel(int channel) {
        double value = channel / 255.0;
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private UiTheme() {}
}
