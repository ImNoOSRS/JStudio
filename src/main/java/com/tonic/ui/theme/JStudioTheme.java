package com.tonic.ui.theme;

import java.awt.Color;
import java.awt.Font;

/**
 * JStudio theme facade.
 * Delegates to ThemeManager for current theme colors.
 * Provides static accessor methods for backward compatibility.
 */
public class JStudioTheme {

    private JStudioTheme() {
    }

    public static Color getBgPrimary() {
        return ThemeManager.getInstance().getCurrentTheme().getBgPrimary();
    }

    public static Color getBgSecondary() {
        return ThemeManager.getInstance().getCurrentTheme().getBgSecondary();
    }

    public static Color getBgTertiary() {
        return ThemeManager.getInstance().getCurrentTheme().getBgTertiary();
    }

    public static Color getBgSurface() {
        return ThemeManager.getInstance().getCurrentTheme().getBgSurface();
    }

    public static Color getTextPrimary() {
        return ThemeManager.getInstance().getCurrentTheme().getTextPrimary();
    }

    public static Color getTextSecondary() {
        return ThemeManager.getInstance().getCurrentTheme().getTextSecondary();
    }

    public static Color getTextDisabled() {
        return ThemeManager.getInstance().getCurrentTheme().getTextDisabled();
    }

    public static Color getAccent() {
        return ThemeManager.getInstance().getCurrentTheme().getAccent();
    }

    public static Color getAccentSecondary() {
        return ThemeManager.getInstance().getCurrentTheme().getAccentSecondary();
    }

    public static Color getSuccess() {
        return ThemeManager.getInstance().getCurrentTheme().getSuccess();
    }

    public static Color getWarning() {
        return ThemeManager.getInstance().getCurrentTheme().getWarning();
    }

    public static Color getError() {
        return ThemeManager.getInstance().getCurrentTheme().getError();
    }

    public static Color getInfo() {
        return ThemeManager.getInstance().getCurrentTheme().getInfo();
    }

    public static Color getSelection() {
        return ThemeManager.getInstance().getCurrentTheme().getSelection();
    }

    public static Color getHover() {
        return ThemeManager.getInstance().getCurrentTheme().getHover();
    }

    public static Color getLineHighlight() {
        return ThemeManager.getInstance().getCurrentTheme().getLineHighlight();
    }

    public static Color getBorder() {
        return ThemeManager.getInstance().getCurrentTheme().getBorder();
    }

    public static Color getBorderFocus() {
        return ThemeManager.getInstance().getCurrentTheme().getBorderFocus();
    }

    public static void apply() {
        ThemeManager.getInstance().applyTheme();
    }

    public static Font getCodeFont(int size) {
        return ThemeManager.getInstance().getCurrentTheme().getCodeFont(size);
    }

    public static Font getUIFont(int size) {
        return ThemeManager.getInstance().getCurrentTheme().getUIFont(size);
    }
}
