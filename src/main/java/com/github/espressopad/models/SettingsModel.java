package com.github.espressopad.models;

import java.awt.*;
import java.io.Serializable;

public class SettingsModel implements Serializable {
    private Font font;
    private String theme;
    private boolean wordWrap;
    private String lookAndFeel;

    public Font getFont() {
        return this.font;
    }

    public void setFont(Font font) {
        this.font = font;
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
