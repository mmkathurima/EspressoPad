package com.github.espressopad.io;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.io.OutputStream;

public class ConsoleOutputStream extends OutputStream {
    private final TextArea textArea;

    public ConsoleOutputStream(TextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void write(int b) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                textArea.appendText(String.valueOf((char) b));
            }
        });
    }
}
