module com.example.jshelleditor {
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires javafx.controls;
    requires javafx.fxml;

    requires jdk.jshell;
    requires java.desktop;
    requires org.fxmisc.richtext;
    requires org.fxmisc.flowless;
    requires reactfx;
    requires javafx.web;
    requires erebus;
    requires aether.api;

    opens com.example.jshelleditor to javafx.fxml;
    exports com.example.jshelleditor;
    exports com.example.jshelleditor.artifacts;
    opens com.example.jshelleditor.artifacts to javafx.fxml;
    exports com.example.jshelleditor.streams;
    opens com.example.jshelleditor.streams to javafx.fxml;
}