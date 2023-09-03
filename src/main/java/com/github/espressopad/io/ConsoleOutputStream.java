package com.github.espressopad.io;

import com.github.espressopad.EspressoPadController;
import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.unbescape.html.HtmlEscape;

import java.io.OutputStream;

public class ConsoleOutputStream extends OutputStream {
    protected final EspressoPadController controller;
    protected Document document;
    protected WebEngine engine;
    protected Element element;

    public ConsoleOutputStream(EspressoPadController controller) {
        this.controller = controller;
    }

    @Override
    public void write(int b) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                engine = controller.getOutput().getEngine();
                document = controller.getDocument();
                element = document.getElementById("output");

                char c = (char) b;
                switch (c) {
                    case '\r':
                    case '\n':
                        // Append a <br> element for newline characters
                        element.appendChild(document.createElement("br"));
                        break;
                    case '\t':
                        // Replace tabs with four non-breaking spaces
                        element.append("&nbsp;".repeat(4));
                        break;
                    case '\b':
                        element.html(element.html().substring(0, element.html().length() - 1));
                        break;
                    case '\f':
                        element.appendChild(document.createElement("br"));
                        element.append("&nbsp;".repeat(4));
                        break;
                    default:
                        // Append other characters as text
                        element.append(String.valueOf(c));
                        break;
                }
                engine.loadContent(HtmlEscape.unescapeHtml(document.outerHtml()), "text/html");
            }
        });
    }
}
