module com.example.jshelleditor {
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires javafx.controls;
    requires javafx.fxml;

    requires jdk.jshell;
    requires java.desktop;
    requires org.fxmisc.flowless;
    requires org.fxmisc.richtext;
    requires org.fxmisc.undo;
    requires reactfx;
    requires javafx.web;
    requires maven.archeologist;
    requires kotlin.stdlib;
    requires kotlin.stdlib.common;
    requires jAstyle;

    opens com.example.jshelleditor to javafx.fxml;
    exports com.example.jshelleditor;
    exports com.example.jshelleditor.artifacts;
    opens com.example.jshelleditor.artifacts to javafx.fxml;
    exports com.example.jshelleditor.io;
    opens com.example.jshelleditor.io to javafx.fxml;
    exports com.example.jshelleditor.editor;
    opens com.example.jshelleditor.editor to javafx.fxml;
}