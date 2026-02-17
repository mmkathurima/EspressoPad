package com.github.espressopad.utils;

import bibliothek.gui.dock.DefaultDockable;

import javax.swing.JComponent;
import javax.swing.filechooser.FileSystemView;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

public class Utilities {
    public static Font[] getMonospaceFonts() {
        FontRenderContext frc = new FontRenderContext(
                null,
                RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT,
                RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT
        );
        return Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts())
                .filter(font ->
                        font.getStringBounds("i", frc).getWidth() == font.getStringBounds("m", frc).getWidth())
                .toArray(Font[]::new);
    }

    public static Path validateDefaultDirectory() {
        Path path = Path.of(FileSystemView.getFileSystemView().getDefaultDirectory().getPath(), "EspressoPad");
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return path;
    }

    public static DefaultDockable createDockable(JComponent panel, String title) {
        DefaultDockable dockable = new DefaultDockable();
        dockable.setTitleText(title);
        panel.setOpaque(true);
        dockable.add(panel);
        return dockable;
    }

    public static Font deriveFont(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null)
            resultName = currentFont.getName();
        else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1'))
                resultName = fontName;
            else resultName = currentFont.getName();
        }
        Font font;
        if (style >= 0) {
            if (size >= 0) font = new Font(resultName, style, size);
            else font = new Font(resultName, style, currentFont.getSize());
        } else {
            if (size >= 0)
                font = new Font(resultName, currentFont.getStyle(), size);
            else font = new Font(resultName, currentFont.getStyle(), currentFont.getSize());
        }
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback;
        if (isMac)
            fontWithFallback = new Font(font.getFamily(), font.getStyle(), font.getSize());
        else fontWithFallback = new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        if (fontWithFallback instanceof FontUIResource)
            return fontWithFallback;
        return new FontUIResource(fontWithFallback);
    }
}
