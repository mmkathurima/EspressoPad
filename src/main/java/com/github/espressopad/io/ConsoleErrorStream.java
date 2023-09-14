package com.github.espressopad.io;

import com.github.espressopad.controllers.EspressoPadController;
import javafx.application.Platform;
import org.unbescape.html.HtmlEscape;

public class ConsoleErrorStream extends ConsoleOutputStream {
    public ConsoleErrorStream(EspressoPadController controller) {
        super(controller);
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
                        // Append a <br> element for newline characters
                        element.appendChild(document.createElement("br"));
                        break;
                    case '\n':
                        if (prev != null && prev != '\r')
                            element.appendChild(document.createElement("br"));
                        break;
                    case '\t':
                        // Replace tabs with four non-breaking spaces
                        element.append("&nbsp;".repeat(4));
                        break;
                    case '\b':
                        element.select("span.err:last-child").remove();
                        break;
                    case '\f':
                        element.appendChild(document.createElement("br"));
                        element.append("&nbsp;".repeat(4));
                        break;
                    default:
                        // Append other characters as text
                        element.append(String.format("<span class=\"err\">%c</span>", c));
                        break;
                }
                prev = c;
                engine.loadContent(HtmlEscape.unescapeHtml(document.outerHtml()), "text/html");
            }
        });
    }
}
