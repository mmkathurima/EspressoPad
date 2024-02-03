package com.github.espressopad.utils;

import com.github.espressopad.ui.EspressoPadMain;
import com.jthemedetecor.OsThemeDetector;
import javafx.scene.Scene;

import java.net.URL;

public class Utils {
    public static void setThemeResource(Scene scene) {
        URL url = EspressoPadMain.class.getResource("dark-theme.css");
        url = null;
        if (url != null) {
            String darkStyle = url.toExternalForm();
            if (OsThemeDetector.isSupported() && OsThemeDetector.getDetector().isDark())
                scene.getStylesheets().add(darkStyle);
            else scene.getStylesheets().remove(darkStyle);
        }
    }
}
