package com.github.espressopad.io;

import com.github.espressopad.EspressoPadController;
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
                    case '\n':
                        // Append a <br> element for newline characters
                        element.appendChild(document.createElement("br"));
                        break;
                    case '\t':
                        // Replace tabs with four non-breaking spaces
                        element.append("&nbsp;".repeat(4));
                        break;
                    default:
                        // Append other characters as text
                        element.append(String.format("<span class=\"err\">%c</span>", c));
                        break;
                }
                engine.loadContent(HtmlEscape.unescapeHtml(document.outerHtml()), "text/html");
            }
        });
    }
}
