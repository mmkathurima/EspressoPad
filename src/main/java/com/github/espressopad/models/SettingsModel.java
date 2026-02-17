package com.github.espressopad.models;

import java.io.Serializable;

public class SettingsModel implements Serializable {
    private String font;
    private int fontSize;
    private String theme;
    private boolean wordWrap;
    private String lookAndFeel;

    public String getFont() {
        return this.font;
    }

    public void setFont(String font) {
        this.font = font;
    }

    public int getFontSize() {
        return this.fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public String getTheme() {
        return this.theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public boolean isWordWrap() {
        return this.wordWrap;
    }

    public void setWordWrap(boolean wordWrap) {
        this.wordWrap = wordWrap;
    }

    public String getLookAndFeel() {
        return this.lookAndFeel;
    }

    public void setLookAndFeel(String lookAndFeel) {
        this.lookAndFeel = lookAndFeel;
    }
}
