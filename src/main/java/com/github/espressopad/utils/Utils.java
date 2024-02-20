package com.github.espressopad.utils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import com.github.espressopad.ui.EspressoPadMain;
import com.jthemedetecor.OsThemeDetector;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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

    public static Logger getLogger(Class<?> cls) {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder ple = new PatternLayoutEncoder();
        ple.setPattern("%green(%date) %highlight(%level) %boldBlue([%thread]) %cyan(%logger{10}) %blue([%file:%line]) %msg%n");
        ple.setContext(lc);
        ple.start();

        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setEncoder(ple);
        consoleAppender.setContext(lc);
        consoleAppender.start();

        Logger logger = (Logger) LoggerFactory.getLogger(cls);
        logger.addAppender(consoleAppender);
        logger.setLevel(Level.DEBUG);
        logger.setAdditive(true); /* set to true if root should log too */

        return logger;
    }

    public static List<String> getMonospaceFonts() {
        final Text th = new Text("1 l");
        final Text tk = new Text("MWX");

        List<String> fontFamilyList = Font.getFamilies();
        List<String> mFamilyList = new ArrayList<>();

        for (String fontFamilyName : fontFamilyList) {
            Font font = Font.font(fontFamilyName, FontWeight.NORMAL, FontPosture.REGULAR, 14.0d);
            th.setFont(font);
            tk.setFont(font);
            if (th.getLayoutBounds().getWidth() == tk.getLayoutBounds().getWidth())
                mFamilyList.add(fontFamilyName);
        }
        return mFamilyList;
    }
}
