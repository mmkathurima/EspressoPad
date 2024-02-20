module com.github.espressopad {
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires jdk.jshell;
    requires java.desktop;
    requires org.fxmisc.flowless;
    requires org.fxmisc.richtext;
    requires org.fxmisc.undo;
    requires reactfx;
    requires maven.archeologist;
    requires kotlin.stdlib;
    requires kotlin.stdlib.common;
    requires jAstyle;
    requires unbescape;
    requires com.jthemedetector;
    requires org.jsoup;
    requires org.controlsfx.controls;
    requires com.github.javaparser.core;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;

    opens com.github.espressopad;
    exports com.github.espressopad;
    exports com.github.espressopad.editor;
    opens com.github.espressopad.editor;
    exports com.github.espressopad.io;
    opens com.github.espressopad.io;
    exports com.github.espressopad.xml;
    opens com.github.espressopad.xml;
    exports com.github.espressopad.controllers;
    opens com.github.espressopad.controllers;
    exports com.github.espressopad.ui;
    opens com.github.espressopad.ui;
}