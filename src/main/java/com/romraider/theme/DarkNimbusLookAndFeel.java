/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 */

package com.romraider.theme;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.Painter;
import javax.swing.UIDefaults;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.DimensionUIResource;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

/**
 * Dark Nimbus palette for the local macOS RomRaider bundle.
 *
 * Nimbus is part of the Java runtime, so the application does not need a
 * third-party look-and-feel dependency.
 */
public final class DarkNimbusLookAndFeel extends NimbusLookAndFeel {
    private static final long serialVersionUID = 1L;

    private static final ColorUIResource BACKGROUND = color(31, 38, 42);
    private static final ColorUIResource ALTERNATE_BACKGROUND =
            color(37, 44, 47);
    private static final ColorUIResource SURFACE = color(37, 44, 47);
    private static final ColorUIResource CONTROL = color(58, 65, 68);
    private static final ColorUIResource CONTROL_HOVER = color(71, 79, 82);
    private static final ColorUIResource CONTROL_PRESSED = color(47, 54, 57);
    private static final ColorUIResource BORDER = color(89, 97, 100);
    private static final ColorUIResource TEXT = color(220, 227, 231);
    private static final ColorUIResource DISABLED_TEXT = color(146, 151, 155);
    private static final ColorUIResource SELECTION = color(62, 142, 246);
    private static final ColorUIResource FOCUS = color(83, 155, 248);

    @Override
    public String getName() {
        return "RomRaiderHD Dark";
    }

    @Override
    public String getID() {
        return "RomRaiderHDDark";
    }

    @Override
    public String getDescription() {
        return "Dark Nimbus look and feel for RomRaiderHD";
    }

    @Override
    public UIDefaults getDefaults() {
        UIDefaults defaults = super.getDefaults();

        put(defaults, BACKGROUND,
                "nimbusLightBackground",
                "Table.background",
                "TextArea.background",
                "TextField.background",
                "FormattedTextField.background",
                "PasswordField.background",
                "EditorPane.background",
                "TextPane.background",
                "Tree.background",
                "List.background",
                "Viewport.background");

        put(defaults, SURFACE,
                "control",
                "info",
                "Panel.background",
                "OptionPane.background",
                "ScrollPane.background",
                "TabbedPane.background",
                "SplitPane.background",
                "ToolBar.background",
                "MenuBar.background",
                "PopupMenu.background",
                "Menu.background",
                "MenuItem.background",
                "CheckBoxMenuItem.background",
                "RadioButtonMenuItem.background",
                "ToolTip.background",
                "window");

        defaults.put("DesktopPane.background", BACKGROUND);

        put(defaults, CONTROL,
                "nimbusBase",
                "nimbusBlueGrey",
                "Button.background",
                "ToggleButton.background",
                "ComboBox.background",
                "Spinner.background",
                "TableHeader.background");

        put(defaults, BORDER,
                "nimbusBorder",
                "Table.gridColor",
                "Separator.foreground",
                "controlShadow",
                "controlDkShadow");

        put(defaults, TEXT,
                "text",
                "Label.foreground",
                "Button.foreground",
                "ToggleButton.foreground",
                "CheckBox.foreground",
                "RadioButton.foreground",
                "ComboBox.foreground",
                "ComboBox:\"ComboBox.renderer\".foreground",
                "List.foreground",
                "Table.foreground",
                "TableHeader.foreground",
                "Tree.foreground",
                "TextArea.foreground",
                "TextField.foreground",
                "FormattedTextField.foreground",
                "PasswordField.foreground",
                "EditorPane.foreground",
                "TextPane.foreground",
                "Menu.foreground",
                "MenuItem.foreground",
                "CheckBoxMenuItem.foreground",
                "RadioButtonMenuItem.foreground",
                "Menu[Enabled].textForeground",
                "Menu[Selected].textForeground",
                "MenuBar:Menu[Enabled].textForeground",
                "MenuBar:Menu[Selected].textForeground",
                "MenuItem[Enabled].textForeground",
                "MenuItem[MouseOver].textForeground",
                "CheckBoxMenuItem[Enabled].textForeground",
                "CheckBoxMenuItem[MouseOver].textForeground",
                "RadioButtonMenuItem[Enabled].textForeground",
                "RadioButtonMenuItem[MouseOver].textForeground",
                "TabbedPane.foreground",
                "TabbedPane:TabbedPaneTab[Enabled].textForeground",
                "TabbedPane:TabbedPaneTab[Enabled+MouseOver].textForeground",
                "TabbedPane:TabbedPaneTab[Enabled+Pressed].textForeground",
                "TitledBorder.titleColor",
                "ToolTip.foreground",
                "controlText",
                "infoText",
                "menuText",
                "textText",
                "windowText");

        put(defaults, DISABLED_TEXT,
                "nimbusDisabledText",
                "Label.disabledForeground",
                "Button.disabledText",
                "MenuItem.disabledForeground",
                "Menu[Disabled].textForeground",
                "MenuBar:Menu[Disabled].textForeground",
                "MenuItem[Disabled].textForeground",
                "CheckBoxMenuItem[Disabled].textForeground",
                "RadioButtonMenuItem[Disabled].textForeground",
                "TabbedPane:TabbedPaneTab[Disabled].textForeground");

        put(defaults, SELECTION,
                "nimbusSelectionBackground",
                "List.selectionBackground",
                "Table.selectionBackground",
                "Tree.selectionBackground",
                "ComboBox.selectionBackground",
                "Menu.selectionBackground",
                "MenuItem.selectionBackground",
                "CheckBoxMenuItem.selectionBackground",
                "RadioButtonMenuItem.selectionBackground",
                "TextArea.selectionBackground",
                "TextField.selectionBackground",
                "FormattedTextField.selectionBackground",
                "PasswordField.selectionBackground",
                "EditorPane.selectionBackground",
                "TextPane.selectionBackground");

        put(defaults, color(255, 255, 255),
                "nimbusSelectedText",
                "List.selectionForeground",
                "Table.selectionForeground",
                "Tree.selectionForeground",
                "ComboBox.selectionForeground",
                "ComboBox:\"ComboBox.renderer\"[Selected].textForeground",
                "ComboBox:\"ComboBox.listRenderer\"[Selected].textForeground",
                "Menu.selectionForeground",
                "MenuItem.selectionForeground",
                "CheckBoxMenuItem.selectionForeground",
                "RadioButtonMenuItem.selectionForeground",
                "TabbedPane:TabbedPaneTab[Selected].textForeground",
                "TabbedPane:TabbedPaneTab[Focused+Selected].textForeground",
                "TabbedPane:TabbedPaneTab[MouseOver+Selected].textForeground",
                "TabbedPane:TabbedPaneTab[Pressed+Selected].textForeground",
                "TabbedPane:TabbedPaneTab"
                        + "[Focused+MouseOver+Selected].textForeground",
                "TabbedPane:TabbedPaneTab"
                        + "[Focused+Pressed+Selected].textForeground");

        put(defaults, FOCUS,
                "nimbusFocus",
                "nimbusInfoBlue",
                "Tree.selectionBorderColor");
        put(defaults, color(217, 180, 74),
                "nimbusOrange", "nimbusAlertYellow");
        put(defaults, color(224, 122, 135), "nimbusRed");
        put(defaults, color(123, 201, 154), "nimbusGreen");

        defaults.put("Table.alternateRowColor", ALTERNATE_BACKGROUND);
        defaults.put("Tree.rendererFillBackground", Boolean.TRUE);
        if (Boolean.getBoolean(
                RomRaiderBootstrap.AQUA_INTERNAL_FRAMES_PROPERTY)) {
            defaults.put(
                    "InternalFrameUI",
                    "com.apple.laf.AquaInternalFrameUI");
        }
        defaults.put("ComboBox:\"ComboBox.renderer\".background", CONTROL);
        defaults.put(
                "ComboBox:\"ComboBox.renderer\"[Selected].background",
                CONTROL);
        defaults.put(
                "ComboBox:\"ComboBox.listRenderer\"[Selected].background",
                SELECTION);

        defaults.put("ScrollBar.thumbHeight", Integer.valueOf(12));
        defaults.put(
                "ScrollBar.buttonSize", new DimensionUIResource(0, 0));
        defaults.put("ScrollBar.incrementButtonGap", Integer.valueOf(0));
        defaults.put("ScrollBar.decrementButtonGap", Integer.valueOf(0));
        defaults.put(
                "ScrollBar.minimumThumbSize",
                new DimensionUIResource(24, 24));
        defaults.put(
                "ScrollBar.maximumThumbSize",
                new DimensionUIResource(4096, 4096));

        paint(defaults, BACKGROUND,
                "DesktopPane[Enabled].backgroundPainter");
        paint(defaults, SURFACE,
                "MenuBar[Enabled].backgroundPainter",
                "PopupMenu[Enabled].backgroundPainter",
                "PopupMenu[Disabled].backgroundPainter");
        paint(defaults, SELECTION,
                "MenuBar:Menu[Selected].backgroundPainter",
                "Menu[Enabled+Selected].backgroundPainter",
                "MenuItem[MouseOver].backgroundPainter",
                "CheckBoxMenuItem[MouseOver].backgroundPainter",
                "CheckBoxMenuItem[MouseOver+Selected].backgroundPainter",
                "RadioButtonMenuItem[MouseOver].backgroundPainter",
                "RadioButtonMenuItem[MouseOver+Selected].backgroundPainter");
        paint(defaults, BORDER,
                "PopupMenuSeparator[Enabled].backgroundPainter");

        paint(defaults, SURFACE,
                "TabbedPane:TabbedPaneTabArea[Enabled].backgroundPainter",
                "TabbedPane:TabbedPaneTabArea"
                        + "[Enabled+MouseOver].backgroundPainter",
                "TabbedPane:TabbedPaneTabArea"
                        + "[Enabled+Pressed].backgroundPainter",
                "TabbedPane:TabbedPaneTabArea[Disabled].backgroundPainter");
        paintRounded(defaults, CONTROL, BORDER,
                "TabbedPane:TabbedPaneTab[Enabled].backgroundPainter");
        paintRounded(defaults, CONTROL_HOVER, BORDER,
                "TabbedPane:TabbedPaneTab"
                        + "[Enabled+MouseOver].backgroundPainter");
        paintRounded(defaults, CONTROL_PRESSED, BORDER,
                "TabbedPane:TabbedPaneTab"
                        + "[Enabled+Pressed].backgroundPainter");
        paintRounded(defaults, SELECTION, FOCUS,
                "TabbedPane:TabbedPaneTab[Selected].backgroundPainter",
                "TabbedPane:TabbedPaneTab"
                        + "[Focused+Selected].backgroundPainter",
                "TabbedPane:TabbedPaneTab"
                        + "[MouseOver+Selected].backgroundPainter",
                "TabbedPane:TabbedPaneTab"
                        + "[Pressed+Selected].backgroundPainter",
                "TabbedPane:TabbedPaneTab"
                        + "[Focused+MouseOver+Selected].backgroundPainter",
                "TabbedPane:TabbedPaneTab"
                        + "[Focused+Pressed+Selected].backgroundPainter");
        paintRounded(defaults, SURFACE, BORDER,
                "TabbedPane:TabbedPaneTab[Disabled].backgroundPainter",
                "TabbedPane:TabbedPaneTab"
                        + "[Disabled+Selected].backgroundPainter");

        paintRounded(defaults, CONTROL, BORDER,
                "Button[Enabled].backgroundPainter");
        paintRounded(defaults, CONTROL, FOCUS,
                "Button[Default].backgroundPainter",
                "Button[Default+Focused].backgroundPainter",
                "Button[Focused].backgroundPainter");
        paintRounded(defaults, CONTROL_HOVER, BORDER,
                "Button[MouseOver].backgroundPainter",
                "Button[Default+MouseOver].backgroundPainter");
        paintRounded(defaults, CONTROL_HOVER, FOCUS,
                "Button[Focused+MouseOver].backgroundPainter",
                "Button[Default+Focused+MouseOver].backgroundPainter");
        paintRounded(defaults, CONTROL_PRESSED, BORDER,
                "Button[Pressed].backgroundPainter",
                "Button[Default+Pressed].backgroundPainter");
        paintRounded(defaults, CONTROL_PRESSED, FOCUS,
                "Button[Focused+Pressed].backgroundPainter",
                "Button[Default+Focused+Pressed].backgroundPainter");
        paintRounded(defaults, SURFACE, BORDER,
                "Button[Disabled].backgroundPainter");

        paintRounded(defaults, CONTROL_HOVER, BORDER,
                "ToolBar:Button[MouseOver].backgroundPainter");
        paintRounded(defaults, CONTROL_HOVER, FOCUS,
                "ToolBar:Button[Focused].backgroundPainter",
                "ToolBar:Button[Focused+MouseOver].backgroundPainter");
        paintRounded(defaults, CONTROL_PRESSED, BORDER,
                "ToolBar:Button[Pressed].backgroundPainter");
        paintRounded(defaults, CONTROL_PRESSED, FOCUS,
                "ToolBar:Button[Focused+Pressed].backgroundPainter");

        paintRounded(defaults, CONTROL, BORDER,
                "ComboBox[Enabled].backgroundPainter",
                "ComboBox[Editable+Enabled].backgroundPainter");
        paintRounded(defaults, CONTROL_HOVER, BORDER,
                "ComboBox[MouseOver].backgroundPainter",
                "ComboBox[Focused+MouseOver].backgroundPainter",
                "ComboBox[Editable+MouseOver].backgroundPainter");
        paintRounded(defaults, CONTROL_PRESSED, BORDER,
                "ComboBox[Pressed].backgroundPainter",
                "ComboBox[Focused+Pressed].backgroundPainter",
                "ComboBox[Enabled+Selected].backgroundPainter",
                "ComboBox[Editable+Pressed].backgroundPainter");
        paintRounded(defaults, CONTROL, FOCUS,
                "ComboBox[Focused].backgroundPainter",
                "ComboBox[Editable+Focused].backgroundPainter");
        paintRounded(defaults, SURFACE, BORDER,
                "ComboBox[Disabled].backgroundPainter",
                "ComboBox[Disabled+Pressed].backgroundPainter",
                "ComboBox[Disabled+Editable].backgroundPainter");

        paintArrow(defaults, TEXT,
                "ArrowButton[Enabled].foregroundPainter",
                "ComboBox:\"ComboBox.arrowButton\"[Enabled].foregroundPainter",
                "ComboBox:\"ComboBox.arrowButton\"[MouseOver].foregroundPainter",
                "ComboBox:\"ComboBox.arrowButton\"[Pressed].foregroundPainter",
                "ComboBox:\"ComboBox.arrowButton\"[Selected].foregroundPainter");
        paintArrow(defaults, DISABLED_TEXT,
                "ArrowButton[Disabled].foregroundPainter",
                "ComboBox:\"ComboBox.arrowButton\"[Disabled].foregroundPainter");

        paintRoundedFill(defaults, CONTROL,
                "TextField[Enabled].backgroundPainter",
                "TextField[Selected].backgroundPainter",
                "FormattedTextField[Enabled].backgroundPainter",
                "FormattedTextField[Selected].backgroundPainter",
                "PasswordField[Enabled].backgroundPainter",
                "PasswordField[Selected].backgroundPainter");
        paintRoundedFill(defaults, SURFACE,
                "TextField[Disabled].backgroundPainter",
                "FormattedTextField[Disabled].backgroundPainter",
                "PasswordField[Disabled].backgroundPainter");
        paintRoundedBorder(defaults, BORDER,
                "TextField[Enabled].borderPainter",
                "TextField[Disabled].borderPainter",
                "FormattedTextField[Enabled].borderPainter",
                "FormattedTextField[Disabled].borderPainter",
                "PasswordField[Enabled].borderPainter",
                "PasswordField[Disabled].borderPainter");
        paintRoundedBorder(defaults, FOCUS,
                "TextField[Focused].borderPainter",
                "FormattedTextField[Focused].borderPainter",
                "PasswordField[Focused].borderPainter");

        paint(defaults, BACKGROUND,
                "ScrollBar:ScrollBarTrack[Enabled].backgroundPainter",
                "ScrollBar:ScrollBarTrack[Disabled].backgroundPainter");
        paintInsetRounded(defaults, CONTROL, BORDER,
                "ScrollBar:ScrollBarThumb[Enabled].backgroundPainter");
        paintInsetRounded(defaults, CONTROL_HOVER, BORDER,
                "ScrollBar:ScrollBarThumb[MouseOver].backgroundPainter");
        paintInsetRounded(defaults, FOCUS, FOCUS,
                "ScrollBar:ScrollBarThumb[Pressed].backgroundPainter");

        return defaults;
    }

    private static ColorUIResource color(int red, int green, int blue) {
        return new ColorUIResource(new Color(red, green, blue));
    }

    private static void put(UIDefaults defaults, ColorUIResource value,
            String... keys) {
        for (String key : keys) {
            defaults.put(key, value);
        }
    }

    private static void paint(UIDefaults defaults, ColorUIResource color,
            String... keys) {
        Painter<JComponent> painter = new SolidColorPainter(color);
        for (String key : keys) {
            defaults.put(key, painter);
        }
    }

    private static void paintRounded(UIDefaults defaults,
            ColorUIResource fill, ColorUIResource border, String... keys) {
        Painter<JComponent> painter =
                new RoundedColorPainter(fill, border, 0);
        for (String key : keys) {
            defaults.put(key, painter);
        }
    }

    private static void paintInsetRounded(UIDefaults defaults,
            ColorUIResource fill, ColorUIResource border, String... keys) {
        Painter<JComponent> painter =
                new RoundedColorPainter(fill, border, 2);
        for (String key : keys) {
            defaults.put(key, painter);
        }
    }

    private static void paintRoundedFill(UIDefaults defaults,
            ColorUIResource fill, String... keys) {
        Painter<JComponent> painter = new RoundedFillPainter(fill);
        for (String key : keys) {
            defaults.put(key, painter);
        }
    }

    private static void paintRoundedBorder(UIDefaults defaults,
            ColorUIResource border, String... keys) {
        Painter<JComponent> painter = new RoundedBorderPainter(border);
        for (String key : keys) {
            defaults.put(key, painter);
        }
    }

    private static void paintArrow(UIDefaults defaults,
            ColorUIResource color, String... keys) {
        Painter<JComponent> painter = new ArrowPainter(color);
        for (String key : keys) {
            defaults.put(key, painter);
        }
    }

    private static final class SolidColorPainter
            implements Painter<JComponent> {
        private final Color color;

        private SolidColorPainter(Color color) {
            this.color = color;
        }

        @Override
        public void paint(Graphics2D graphics, JComponent component,
                int width, int height) {
            graphics.setColor(color);
            graphics.fillRect(0, 0, width, height);
        }
    }

    private static final class RoundedColorPainter
            implements Painter<JComponent> {
        private final Color fill;
        private final Color border;
        private final int inset;

        private RoundedColorPainter(Color fill, Color border, int inset) {
            this.fill = fill;
            this.border = border;
            this.inset = inset;
        }

        @Override
        public void paint(Graphics2D graphics, JComponent component,
                int width, int height) {
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int outerWidth = Math.max(0, width - inset * 2);
            int outerHeight = Math.max(0, height - inset * 2);
            int arc = Math.min(10, Math.min(outerWidth, outerHeight));

            graphics.setColor(border);
            graphics.fillRoundRect(
                    inset, inset, outerWidth, outerHeight, arc, arc);

            int innerInset = inset + 1;
            int innerWidth = Math.max(0, width - innerInset * 2);
            int innerHeight = Math.max(0, height - innerInset * 2);
            graphics.setColor(fill);
            graphics.fillRoundRect(
                    innerInset,
                    innerInset,
                    innerWidth,
                    innerHeight,
                    Math.max(0, arc - 2),
                    Math.max(0, arc - 2));
        }
    }

    private static final class RoundedFillPainter
            implements Painter<JComponent> {
        private final Color fill;

        private RoundedFillPainter(Color fill) {
            this.fill = fill;
        }

        @Override
        public void paint(Graphics2D graphics, JComponent component,
                int width, int height) {
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = Math.min(10, Math.min(width, height));
            graphics.setColor(fill);
            graphics.fillRoundRect(0, 0, width, height, arc, arc);
        }
    }

    private static final class RoundedBorderPainter
            implements Painter<JComponent> {
        private final Color border;

        private RoundedBorderPainter(Color border) {
            this.border = border;
        }

        @Override
        public void paint(Graphics2D graphics, JComponent component,
                int width, int height) {
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = Math.min(10, Math.min(width, height));
            graphics.setColor(border);
            graphics.drawRoundRect(
                    0, 0, Math.max(0, width - 1),
                    Math.max(0, height - 1), arc, arc);
        }
    }

    private static final class ArrowPainter
            implements Painter<JComponent> {
        private final Color color;

        private ArrowPainter(Color color) {
            this.color = color;
        }

        @Override
        public void paint(Graphics2D graphics, JComponent component,
                int width, int height) {
            int radius = Math.max(2, Math.min(width, height) / 5);
            int centerX = width / 2;
            int centerY = height / 2;
            Polygon triangle = new Polygon(
                    new int[]{
                        centerX + radius / 2,
                        centerX + radius / 2,
                        centerX - radius
                    },
                    new int[]{
                        centerY - radius,
                        centerY + radius,
                        centerY
                    },
                    3);
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(color);
            graphics.fillPolygon(triangle);
        }
    }
}
